/************************************************************************* AVRGAMING LLC __________________
 *
 * [2013] AVRGAMING LLC All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveRepairItem;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.components.RepairCost;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Barracks extends Structure {

	private static final long SAVE_INTERVAL = 60 * 1000;

	private int index = 0;
	private ConstructSign unitNameSign;

	private ConfigUnit trainingUnit = null;
	private double currentHammers = 0.0;

	private TreeMap<Integer, ConstructSign> progresBar = new TreeMap<Integer, ConstructSign>();
	private Date lastSave = null;

	public Barracks(String id, Town town) throws CivException {
		super(id, town);
	}

	public Barracks(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	private String getUnitSignText(int index) throws IndexOutOfBoundsException {
		ArrayList<ConfigUnit> unitList = getTown().getAvailableUnits();

		if (unitList.size() == 0) {
			return "\n" + CivColor.LightGray + CivSettings.localize.localizedString("Nothing") + "\n" + CivColor.LightGray + CivSettings.localize.localizedString("Available");
		}

		ConfigUnit unit = unitList.get(index);
		String out = "\n";
		double coinCost = unit.cost;

		out += CivColor.LightPurple + unit.name + "\n";
		out += CivColor.Yellow + coinCost + "\n";
		out += CivColor.Yellow + CivSettings.CURRENCY_NAME;

		return out;
	}

	private void changeIndex(int newIndex) {
		if (this.unitNameSign != null) {
			try {
				this.unitNameSign.setText(getUnitSignText(newIndex));
				index = newIndex;
			} catch (IndexOutOfBoundsException e) {
				// index = 0;
				// this.unitNameSign.setText(getUnitSignText(index));
			}
			this.unitNameSign.update();
		} else {
			CivLog.warning("Could not find unit name sign for barracks:" + this.getId() + " at " + this.getCorner());
		}
	}

	private void train(Resident whoClicked) throws CivException {
		if (!getTown().GM.isMayorOrAssistant(whoClicked)) {
			throw new CivException(CivSettings.localize.localizedString("barracks_actionNoPerms"));
		}

		ArrayList<ConfigUnit> unitList = getTown().getAvailableUnits();

		ConfigUnit unit = unitList.get(index);
		if (unit == null) throw new CivException(CivSettings.localize.localizedString("barracks_unknownUnit"));
		// TODO Добавить проверку на количество юнитов if (unit.limit != 0 && unit.limit < getTown().getUnitTypeCount(unit.id)) throw new
		// CivException(CivSettings.localize.localizedString("var_barracks_atLimit", unit.name));
		if (!unit.isAvailable(getTown())) throw new CivException(CivSettings.localize.localizedString("barracks_unavailable"));
		if (this.trainingUnit != null) throw new CivException(CivSettings.localize.localizedString("var_barracks_inProgress", this.trainingUnit.name));
		double coinCost = unit.cost;
		if (!getTown().getTreasury().hasEnough(coinCost)) throw new CivException(CivSettings.localize.localizedString("var_barracks_tooPoor", unit.name, coinCost, CivSettings.CURRENCY_NAME));

		getTown().getTreasury().withdraw(coinCost);

		this.currentHammers = 0.0;
		this.trainingUnit = unit;
		CivMessage.sendTown(getTown(), CivSettings.localize.localizedString("var_barracks_begin", unit.name));
		this.onSecondUpdate();
		this.onTechUpdate();
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;

		switch (sign.getAction()) {
		case "prev":
			changeIndex((index - 1));
			break;
		case "next":
			changeIndex((index + 1));
			break;
		case "train":
			if (resident.hasTown()) {
				try {
					train(resident);
				} catch (CivException e) {
					CivMessage.send(player, CivColor.Rose + e.getMessage());
				}
			}
			break;
		case "repair_item":
			repairItem(player, resident, event);
			break;
		}
	}

	private void repairItem(Player player, Resident resident, PlayerInteractEvent event) {
		try {
			ItemStack inHand = player.getInventory().getItemInMainHand();
			if (inHand == null || inHand.getType().equals(Material.AIR)) {
				throw new CivException(CivSettings.localize.localizedString("barracks_repair_noItem"));
			}

			if (Enchantments.hasEnchantment(inHand, CustomEnchantment.NoRepair)) {
				throw new CivException(CivSettings.localize.localizedString("barracks_repair_noRepairEnch"));
			}

			if (inHand.getType().getMaxDurability() == 0) {
				throw new CivException(CivSettings.localize.localizedString("barracks_repair_invalidItem"));
			}

			if (inHand.getDurability() == 0) {
				throw new CivException(CivSettings.localize.localizedString("barracks_repair_atFull"));
			}

			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(inHand);
			if (craftMat == null) {
				throw new CivException(CivSettings.localize.localizedString("barracks_repair_irreperable"));
			}

			try {
				double totalCost;
				if (craftMat.hasComponent("RepairCost")) {
					RepairCost repairCost = (RepairCost) craftMat.getComponent("RepairCost");
					totalCost = repairCost.getDouble("value");
				} else {
					double baseTierRepair = CivSettings.getDouble(CivSettings.structureConfig, "barracks.base_tier_repair");
					double tierDamp = CivSettings.getDouble(CivSettings.structureConfig, "barracks.tier_damp");
					double tierCost = Math.pow((craftMat.getConfigMaterial().tier), tierDamp);
					double fromTier = Math.pow(baseTierRepair, tierCost);
					totalCost = Math.round(fromTier + 0);
				}

				InteractiveRepairItem repairItem = new InteractiveRepairItem(totalCost, player.getName(), craftMat);
				repairItem.displayMessage();
				resident.setInteractiveMode(repairItem);
				return;

			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("internalException"));
			}

		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
			event.setCancelled(true);
		}
	}

	public static void repairItemInHand(double cost, String playerName, CraftableCustomMaterial craftMat) {
		Player player;

		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}

		Resident resident = CivGlobal.getResident(player);

		if (!resident.getTreasury().hasEnough(cost)) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_barracks_repair_TooPoor", cost, CivSettings.CURRENCY_NAME));
			return;
		}

		CraftableCustomMaterial craftMatInHand = CraftableCustomMaterial.getCraftableCustomMaterial(player.getInventory().getItemInMainHand());

		if (!craftMatInHand.getConfigId().equals(craftMat.getConfigId())) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("barracks_repair_DifferentItem"));
			return;
		}

		resident.getTreasury().withdraw(cost);
		player.getInventory().getItemInMainHand().setDurability((short) 0);

		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_barracks_repair_Success", craftMat.getName(), cost, CivSettings.CURRENCY_NAME));

	}

	@Override
	public void onTechUpdate() {
		ConstructSign unitNameSign = this.unitNameSign;
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				unitNameSign.setText(getUnitSignText(index));
				unitNameSign.update();
			}
		});
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		ConstructSign structSign;

		switch (sb.command) {
		case "/prev":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_previousUnit"));
			structSign.setDirection(sb.getData());
			structSign.setAction("prev");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/unitname":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new ConstructSign(absCoord, this);
			structSign.setText(getUnitSignText(0));
			structSign.setDirection(sb.getData());
			structSign.setAction("info");
			structSign.update();

			this.unitNameSign = structSign;

			this.addConstructSign(structSign);
			break;
		case "/next":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_nextUnit"));
			structSign.setDirection(sb.getData());
			structSign.setAction("next");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/train":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_train"));
			structSign.setDirection(sb.getData());
			structSign.setAction("train");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		case "/progress":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new ConstructSign(absCoord, this);
			structSign.setText("");
			structSign.setDirection(sb.getData());
			structSign.setAction("");
			structSign.update();
			this.addConstructSign(structSign);
			this.progresBar.put(Integer.valueOf(sb.keyvalues.get("id")), structSign);

			break;
		case "/repair":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_repairItem"));
			structSign.setDirection(sb.getData());
			structSign.setAction("repair_item");
			structSign.update();
			this.addConstructSign(structSign);
			break;

		}
	}

	public void onPostBuild() {
		ArrayList<ConstructChest> chests = this.getAllChestsById("0");
		if (chests.size() == 0) return;
		Chest chest = (Chest) chests.get(0).getCoord().getBlock().getState();
		getTown().unitInventory.chest = chest;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public ConfigUnit getTrainingUnit() {
		return trainingUnit;
	}

	public void createUnit(ConfigUnit unit) {

		// Find the chest inventory
		ArrayList<ConstructChest> chests = this.getAllChestsById("0");
		if (chests.size() == 0) {
			return;
		}

		try {
			UnitStatic.spawn(this.getTown(), unit.id);

			CivMessage.sendTown(this.getTown(), CivSettings.localize.localizedString("var_barracks_completedTraining", unit.name));
			this.trainingUnit = null;
			this.currentHammers = 0.0;

			CivGlobal.getSessionDatabase().delete_all(getSessionKey());

		} catch (CivException e) {
			this.trainingUnit = null;
			this.currentHammers = 0.0;
			e.getCause().getMessage();
			e.printStackTrace();
			CivMessage.sendTown(getTown(), CivColor.Rose + e.getMessage());
		}

	}

	public void updateProgressBar() {
		double percentageDone = 0.0;

		percentageDone = this.currentHammers / this.trainingUnit.hammer_cost;
		int size = this.progresBar.size();
		int textCount = (int) (size * 16 * percentageDone);
		int textIndex = 0;

		for (int i = 0; i < size; i++) {
			ConstructSign structSign = this.progresBar.get(i);
			String[] text = new String[4];
			text[0] = "";
			text[1] = "";
			text[2] = "";
			text[3] = "";
			for (int j = 0; j < 16; j++) {
				if (textIndex == 0) {
					text[2] += "[";
				} else if (textIndex == ((size * 15) + 3)) {
					text[2] += "]";
				} else if (textIndex < textCount) {
					text[2] += "=";
				} else {
					text[2] += "_";
				}

				textIndex++;
			}

			if (i == (size / 2)) {
				text[1] = CivColor.LightGreen + this.trainingUnit.name;
			}

			structSign.setText(text);
			structSign.update();
		}

	}

	public String getSessionKey() {
		return this.getTown().getName() + ":" + "barracks" + ":" + this.getId();
	}

	public void saveProgress() {
		Barracks barracks = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				if (barracks.getTrainingUnit() != null) {
					String key = getSessionKey();
					String value = barracks.getTrainingUnit().id + ":" + barracks.currentHammers;
					ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);

					if (entries.size() > 0) {
						SessionEntry entry = entries.get(0);
						CivGlobal.getSessionDatabase().update(entry.request_id, key, value);

						/* delete any bad extra entries. */
						for (int i = 1; i < entries.size(); i++) {
							SessionEntry bad_entry = entries.get(i);
							CivGlobal.getSessionDatabase().delete(bad_entry.request_id, key);
						}
					} else {
						barracks.sessionAdd(key, value);
					}
					lastSave = new Date();
				}
			}
		}, 0);

	}

	@Override
	public void onUnload() {
		saveProgress();
	}

	@Override
	public void onLoad() {
		String key = getSessionKey();
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);

		if (entries.size() > 0) {
			SessionEntry entry = entries.get(0);
			String[] values = entry.value.split(":");

			this.trainingUnit = UnitStatic.configUnits.get(values[0]);

			if (trainingUnit == null) {
				CivLog.error("Couldn't find in-progress unit id:" + values[0] + " for town " + this.getTown().getName());
				return;
			}

			this.currentHammers = Double.valueOf(values[1]);

			/* delete any bad extra entries. */
			for (int i = 1; i < entries.size(); i++) {
				SessionEntry bad_entry = entries.get(i);
				CivGlobal.getSessionDatabase().delete(bad_entry.request_id, key);
			}
		}
	}

	public void onSecondUpdate() {
		if (this.trainingUnit != null) {
			// Hammers are per hour, this runs per min. We need to adjust the hammers we add.
			double addedHammers = (getTown().SM.getAttrHammer().total / 60) / 60;
			this.currentHammers += addedHammers;

			this.updateProgressBar();
			Date now = new Date();

			if (lastSave == null || ((lastSave.getTime() + SAVE_INTERVAL) < now.getTime())) {
				this.saveProgress();
			}

			if (this.currentHammers >= this.trainingUnit.hammer_cost) {
				this.currentHammers = this.trainingUnit.hammer_cost;
				this.createUnit(this.trainingUnit);
			}
		}
	}

	public void addHammers(double hammers) {
		if (this.trainingUnit != null) {
			this.currentHammers += hammers;
			this.updateProgressBar();
			Date now = new Date();
			if (this.lastSave == null || this.lastSave.getTime() + 60000L < now.getTime()) {
				this.saveProgress();
			}
			if (this.currentHammers >= this.trainingUnit.hammer_cost) {
				this.currentHammers = this.trainingUnit.hammer_cost;
				this.createUnit(this.trainingUnit);
			}
		}
	}

}
