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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.tasks.NotificationTask;
import com.avrgaming.civcraft.units.EquipmentElement;
import com.avrgaming.civcraft.units.UnitMaterial;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;

public class Blacksmith extends Structure {

	private static final long COOLDOWN = 5;
	//private static final double BASE_CHANCE = 0.8;
	public static int SMELT_TIME_SECONDS = 3600 * 3;
	public static double YIELD_RATE = 1.25;

	private Date lastUse = new Date();
	private NonMemberFeeComponent nonMemberFeeComponent;
	public static HashMap<BlockCoord, Blacksmith> blacksmithAnvils = new HashMap<BlockCoord, Blacksmith>();

	public Blacksmith(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onSave();
	}

	public Blacksmith(ResultSet rs) throws SQLException, CivException {
		super(rs);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onLoad();
	}

	public double getNonResidentFee() {
		return nonMemberFeeComponent.getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
	}

	private String getNonResidentFeeString() {
		return CivSettings.localize.localizedString("Fee:") + " " + ((int) (this.nonMemberFeeComponent.getFeeRate() * 100) + "%").toString();
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "factory";
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
		int special_id = Integer.valueOf(sign.getAction());
		Date now = new Date();
		long diff = now.getTime() - lastUse.getTime();
		diff /= 1000;
		if (diff < Blacksmith.COOLDOWN) throw new CivException(CivSettings.localize.localizedString("var_blacksmith_onCooldown", (Blacksmith.COOLDOWN - diff)));
		lastUse = now;
		switch (special_id) {
			case 0 :
				this.deposit_forge(player);
				break;
			case 1 :
				double cost = CivSettings.getDoubleStructure("blacksmith.forge_cost");
				this.perform_forge(player, cost);
				break;
			case 2 :
				this.depositSmelt(player, player.getInventory().getItemInMainHand());
				break;
			case 3 :
				this.withdrawSmelt(player);
				break;
		}
	}

	@Override
	public void updateSignText() {
		double cost = CivSettings.getDoubleStructure("blacksmith.forge_cost");
		for (ConstructSign sign : getSigns()) {
			int special_id = Integer.valueOf(sign.getAction());
			switch (special_id) {
				case 0 :
					sign.setText(CivSettings.localize.localizedString("rightClickDisabled"));
					break;
				case 1 :
					sign.setText("Одень амуницию"//CivSettings.localize.localizedString("blacksmith_sign_forgeCost")
							+ "\n " + cost + CivSettings.CURRENCY_NAME + "\n" + getNonResidentFeeString());
					break;
				case 2 :
					sign.setText(CivSettings.localize.localizedString("blacksmith_sign_depositOre"));
					break;
				case 3 :
					sign.setText(CivSettings.localize.localizedString("blacksmith_sign_withdrawOre"));
					break;
			}
			sign.update();
		}
	}

	public String getkey(Player player, Structure struct, String tag) {
		return player.getUniqueId().toString() + "_" + struct.getConfigId() + "_" + struct.getCorner().toString() + "_" + tag;
	}

	public void saveItem(ItemStack item, String key) {
		String value = "" + ItemManager.getTypeId(item) + ":";
		for (Enchantment e : item.getEnchantments().keySet()) {
			value += ItemManager.getEnchantmentId(e) + "," + item.getEnchantmentLevel(e);
			value += ":";
		}
		sessionAdd(key, value);
	}

	public static boolean canSmelt(int blockid) {
		switch (blockid) {
			case CivData.GOLD_ORE :
			case CivData.IRON_ORE :
				return true;
		}
		return false;
	}

	/* Converts the ore id's into the ingot id's */
	public static int convertType(int blockid) {
		switch (blockid) {
			case CivData.GOLD_ORE :
				return CivData.GOLD_INGOT;
			case CivData.IRON_ORE :
				return CivData.IRON_INGOT;
		}
		return -1;
	}

	/* Deposit forge will take the current item in the player's hand and deposit its information into the sessionDB. It will store the item's id, data, and
	 * damage. */
	public void deposit_forge(Player player) throws CivException {
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("rightClickDisabled"));
	}

	public void perform_forge(Player player, double cost) throws CivException {
		if (!CivGlobal.getResident(player).getTreasury().hasEnough(cost)) {
			throw new CivException("§c" + CivSettings.localize.localizedString("blacksmith_forge_notEnough",
					"§6" + (cost - CivGlobal.getResident(player).getTreasury().getBalance())));
		}

		ItemStack stack = player.getInventory().getItemInMainHand();
		String umid = ItemManager.getUMid(stack);

		Resident resident = CivGlobal.getResident(player);
		if (!resident.isUnitActive()) throw new CivException("Сперва активируйте юнита");
		UnitObject uo = CivGlobal.getUnitObject(resident.getUnitObjectId());
		if (uo == null) throw new CivException("Юнит сломан. Ишибка базы данних. Обратитесь к администрации");

		UnitMaterial um = uo.getUnit();
		String equip = "";
		int new_tir = -1;
		for (String s : EquipmentElement.allEquipments) {
			EquipmentElement eE = um.equipmentElemens.get(s);
			for (int i = 0; i <= 4; i++) {
				if (eE.getMatTir(i).equalsIgnoreCase(umid)) {
					equip = s;
					new_tir = i;
					break;
				}
				if (new_tir != -1) break;
			}
		}
		if (new_tir == -1) throw new CivException("Этот предмет нельзя одеть на вашего юнита");
		int old_tir = uo.getComponent(equip);
		if (new_tir == old_tir) throw new CivException("На Вас надета такая же аммуниция");

		String old_mat = (old_tir == 0) ? "" : um.getAmuntMatTir(equip, old_tir);
		CivGlobal.getResident(player).getTreasury().withdraw(cost);

		player.getInventory().setItemInMainHand(ItemManager.createItemStack(old_mat, 1));
		uo.setComponent(equip, new_tir);
		uo.rebuildUnitItem(player);
		UnitStatic.updateUnitForPlaeyr(player);
		CivMessage.sendSuccess(player, "Аммуниция одета удачно");
		return;
	}

	/* Take the itemstack in hand and deposit it into the session DB. */
	@SuppressWarnings("deprecation")
	public void depositSmelt(Player player, ItemStack itemsInHand) throws CivException {

		// Make sure that the item is a valid smelt type.
		if (!Blacksmith.canSmelt(itemsInHand.getTypeId())) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_onlyOres"));
		}

		// Only members can use the smelter
		Resident res = CivGlobal.getResident(player.getName());
		if (!res.hasTown() || this.getTown() != res.getTown()) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_notMember"));
		}

		String value = convertType(itemsInHand.getTypeId()) + ":" + (itemsInHand.getAmount() * Blacksmith.YIELD_RATE);
		String key = getkey(player, this, "smelt");

		// Store entry in session DB
		sessionAdd(key, value);

		// Take ore away from player.
		player.getInventory().removeItem(itemsInHand);
		//BukkitTools.sch
		// Schedule a message to notify the player when the smelting is finished.
		BukkitObjects.scheduleAsyncDelayedTask(new NotificationTask(player.getName(), CivColor.LightGreen + CivSettings.localize
				.localizedString("var_blacksmith_smelt_asyncNotify", itemsInHand.getAmount(), CivData.getDisplayName(itemsInHand.getTypeId()))),
				TimeTools.toTicks(SMELT_TIME_SECONDS));

		CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_blacksmith_smelt_depositSuccess", itemsInHand.getAmount(),
				CivData.getDisplayName(itemsInHand.getTypeId())));

		player.updateInventory();
	}

	/* Queries the sessionDB for entries for this player When entries are found, their inserted time is compared to the current time, if they have been in long
	 * enough each itemstack is sent to the players inventory.
	 * 
	 * For each itemstack ready to withdraw try to place it in the players inventory. If there is not enough space, take the leftovers and place them back in
	 * the sessionDB. If there are no leftovers, delete the sessionDB entry. */
	@SuppressWarnings("deprecation")
	public void withdrawSmelt(Player player) throws CivException {

		String key = getkey(player, this, "smelt");
		ArrayList<SessionEntry> entries = null;

		// Only members can use the smelter
		Resident res = CivGlobal.getResident(player.getName());
		if (!res.hasTown() || this.getTown() != res.getTown()) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_notMember"));
		}

		entries = CivGlobal.getSessionDatabase().lookup(key);

		if (entries == null || entries.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_nothingInSmelter"));
		}

		Inventory inv = player.getInventory();
		HashMap<Integer, ItemStack> leftovers = null;

		for (SessionEntry se : entries) {
			String split[] = se.value.split(":");
			int itemId = Integer.valueOf(split[0]);
			double amount = Double.valueOf(split[1]);
			long now = System.currentTimeMillis();
			int secondsBetween = CivGlobal.getSecondsBetween(se.time, now);

			// First determine the time between two events.
			if (secondsBetween < Blacksmith.SMELT_TIME_SECONDS) {
				DecimalFormat df1 = new DecimalFormat("0.##");

				double timeLeft = ((double) Blacksmith.SMELT_TIME_SECONDS - (double) secondsBetween) / (double) 60;
				//Date finish = new Date(now+(secondsBetween*1000));
				CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_blacksmith_smelt_inProgress1", amount,
						CivData.getDisplayName(itemId), df1.format(timeLeft)));
				continue;
			}

			ItemStack stack = new ItemStack(itemId, (int) amount, (short) 0);
			if (stack != null) leftovers = inv.addItem(stack);

			// If this stack was successfully withdrawn, delete it from the DB.
			if (leftovers.size() == 0) {
				CivGlobal.getSessionDatabase().delete(se.request_id, se.key);
				CivMessage.send(player, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess", amount, CivData.getDisplayName(itemId)));

				break;
			} else {
				// We do not have space in our inventory, inform the player.
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("blacksmith_smelt_notEnoughInvenSpace"));

				// If the leftover size is the same as the size we are trying to withdraw, do nothing.
				int leftoverAmount = CivGlobal.getLeftoverSize(leftovers);

				if (leftoverAmount == amount) {
					continue;
				}

				if (leftoverAmount == 0) {
					//just in case we somehow get an entry with 0 items in it.
					CivGlobal.getSessionDatabase().delete(se.request_id, se.key);
				} else {
					// Some of the items were deposited into the players inventory but the sessionDB 
					// still has the full amount stored, update the db to only contain the leftovers.
					String newValue = itemId + ":" + leftoverAmount;
					CivGlobal.getSessionDatabase().update(se.request_id, se.key, newValue);
				}
			}

			// only withdraw one item at a time.
			break;
		}

		player.updateInventory();
	}

	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
	}
}
