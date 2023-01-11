package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.components.SignSelectionActionInterface;
import com.avrgaming.civcraft.components.SignSelectionComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigStableHorse;
import com.avrgaming.civcraft.config.ConfigStableItem;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.*;
import com.avrgaming.gpl.HorseModifier;
import com.avrgaming.gpl.HorseModifier.HorseType;
import com.avrgaming.gpl.HorseModifier.HorseVariant;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_12_R1.util.HashTreeSet;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Stable extends Structure {

	public static Integer FEE_MIN = 5;
	public static Integer FEE_MAX = 100;
	private final HashMap<Integer, SignSelectionComponent> signSelectors = new HashMap<>();
	private BlockCoord horseSpawnCoord;
	private BlockCoord muleSpawnCoord;

	public HashTreeSet<ChunkCoord> chunks = new HashTreeSet<>();
	public static Map<ChunkCoord, Stable> stableChunks = new ConcurrentHashMap<>();

	public Stable(String id, Town town) {
		super(id, town);
	}

	public void bindStableChunks() {
		for (BlockCoord bcoord : this.constructBlocks.keySet()) {
			ChunkCoord coord = new ChunkCoord(bcoord);
			this.chunks.add(coord);
			stableChunks.put(coord, this);
		}
	}

	public void unbindStableChunks() {
		for (ChunkCoord coord : this.chunks) {
			stableChunks.remove(coord);
		}
		this.chunks.clear();
	}

	@Override
	public void onComplete() {
		bindStableChunks();
	}

	@Override
	public void onLoad() {
		bindStableChunks();
	}

	@Override
	public void delete() {
		super.delete();
		unbindStableChunks();
	}

	public void loadSettings() {
		super.loadSettings();

		SignSelectionComponent horseVender = new SignSelectionComponent();
		SignSelectionComponent muleVender = new SignSelectionComponent();
		SignSelectionComponent itemVender = new SignSelectionComponent();

		signSelectors.put(0, horseVender);
		signSelectors.put(1, muleVender);
		signSelectors.put(2, itemVender);

		class buyHorseAction implements SignSelectionActionInterface {
			final int horse_id;
			final double cost;

			public buyHorseAction(int horse_id, double cost) {
				this.horse_id = horse_id;
				this.cost = cost;
			}

			@Override
			public void process(Player player) {
				ConfigStableHorse horse = CivSettings.horses.get(horse_id);
				if (horse == null) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("stable_unknownHorse"));
					return;
				}
				if (horse_id >= 5 && !getCivOwner().hasTechnologys("tech_military_science")) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("stable_missingTech_MilitaryScience"));
					return;
				}

				Resident resident = CivGlobal.getResident(player);

				double paid;
				if (resident.getTown() != getTownOwner()) {
					if (!resident.getTreasury().hasEnough(getItemCost(cost))) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", (getItemCost(cost) + " " + CivSettings.CURRENCY_NAME)));
						return;
					}

					resident.getTreasury().withdraw(getItemCost(cost));
					getTownOwner().depositTaxed(getFeeToTown(cost));
					CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_taxes_paid", getFeeToTown(cost), CivSettings.CURRENCY_NAME));
					paid = getItemCost(cost);
				} else {
					if (!resident.getTreasury().hasEnough(cost)) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", (cost + " " + CivSettings.CURRENCY_NAME)));
						return;
					}

					resident.getTreasury().withdraw(cost);
					paid = cost;
				}

				HorseModifier mod;
				if (!horse.mule) {
					mod = HorseModifier.spawn(horseSpawnCoord.getLocation());
					mod.setType(HorseType.NORMAL);
					mod.setTamed(true);
					mod.setSaddled(true);
				} else {
					mod = HorseModifier.spawn(muleSpawnCoord.getLocation());
					mod.setType(HorseType.MULE);
				}

				mod.setVariant(HorseVariant.valueOf(horse.variant));
				HorseModifier.setHorseSpeed(mod.getHorse(), horse.speed);
				((Horse) mod.getHorse()).setJumpStrength(horse.jump);
				mod.getHorse().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(horse.health);
				mod.getHorse().setHealth(horse.health);
				((Horse) mod.getHorse()).setOwner(player);
				mod.getHorse().setCustomName(horse.name);
				mod.getHorse().setCustomNameVisible(true);

				CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_stable_buySuccess", paid, CivSettings.CURRENCY_NAME));
			}
		}

		class buyItemAction implements SignSelectionActionInterface {

			final int item_id;
			final double cost;

			public buyItemAction(int item_id, double cost) {
				this.item_id = item_id;
				this.cost = cost;
			}

			@Override
			public void process(Player player) {

				Resident resident = CivGlobal.getResident(player);
				if ((item_id >= 417 && item_id <= 419) && !getCivOwner().hasTechnologys("tech_military_science")) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("stable_missingTech_MilitaryScience"));
					return;
				}

				double paid;
				if (resident.getTown() != getTownOwner()) {
					if (!resident.getTreasury().hasEnough(getItemCost(cost))) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", (getItemCost(cost) + " " + CivSettings.CURRENCY_NAME)));
						return;
					}

					resident.getTreasury().withdraw(getItemCost(cost));
					CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_taxes_paid", getFeeToTown(cost), CivSettings.CURRENCY_NAME));
					paid = getItemCost(cost);
				} else {
					if (!resident.getTreasury().hasEnough(cost)) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", (cost + " " + CivSettings.CURRENCY_NAME)));
						return;
					}

					resident.getTreasury().withdraw(cost);
					paid = cost;
				}

				HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(ItemManager.createItemStack(item_id, 1));
				if (leftovers.size() > 0) {
					for (ItemStack stack : leftovers.values()) {
						player.getWorld().dropItem(player.getLocation(), stack);
					}
				}

				CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_stable_buySuccess", paid, CivSettings.CURRENCY_NAME));
			}

		}

		for (ConfigStableItem item : CivSettings.stableItems) {
			SignSelectionComponent comp = signSelectors.get(item.store_id);
			if (comp == null) {
				continue;
			}
			if (item.item_id == 0) {
				comp.addItem(
						new String[] { CivColor.LightGreen + item.name, CivSettings.localize.localizedString("stable_sign_buyFor"), "" + item.cost, CivSettings.localize.localizedString("Fee:") + this.getNonMemberFeeComponent().getFeeString() },
						new buyHorseAction(item.horse_id, item.cost));
			} else {
				comp.addItem(
						new String[] { CivColor.LightGreen + item.name, CivSettings.localize.localizedString("stable_sign_buyFor"), "" + item.cost, CivSettings.localize.localizedString("Fee:") + this.getNonMemberFeeComponent().getFeeString() },
						new buyItemAction(item.item_id, item.cost));
			}
		}
	}

	private double getItemCost(double cost) {
		return cost + getFeeToTown(cost);
	}

	private double getFeeToTown(double cost) {
		return cost * this.getNonMemberFeeComponent().getFeeRate();
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		SignSelectionComponent signSelection = signSelectors.get(Integer.valueOf(sign.getAction()));
		if (signSelection == null) {
			CivLog.warning("No sign seletor component for with id:" + sign.getAction());
			return;
		}

		switch (sign.getType()) {
		case "prev":
			signSelection.processPrev();
			break;
		case "next":
			signSelection.processNext();
			break;
		case "item":
			signSelection.processAction(player);
			break;
		}
	}

	@Override
	public void updateSignText() {
		for (SignSelectionComponent comp : signSelectors.values()) {
			comp.setMessageAllItems(3, CivSettings.localize.localizedString("Fee:") + " " + this.getNonMemberFeeComponent().getFeeString());
		}
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		ConstructSign structSign;
		int selectorIndex;
		SignSelectionComponent signComp;

		switch (sb.command) {
		case "/prev":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("stable_sign_previousUnit"));
			structSign.setDirection(sb.getData());
			structSign.setAction(sb.keyvalues.get("id"));
			structSign.setType("prev");
			structSign.update();
			this.addConstructSign(structSign);
			CivGlobal.addConstructSign(structSign);
			break;
		case "/item":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new ConstructSign(absCoord, this);
			structSign.setText("");
			structSign.setDirection(sb.getData());
			structSign.setAction(sb.keyvalues.get("id"));
			structSign.setType("item");
			structSign.update();

			this.addConstructSign(structSign);
			CivGlobal.addConstructSign(structSign);

			selectorIndex = Integer.parseInt(sb.keyvalues.get("id"));
			signComp = signSelectors.get(selectorIndex);
			if (signComp != null) {
				signComp.setActionSignCoord(absCoord);
				signComp.updateActionSign();
			} else {
				CivLog.warning("No sign selector found for id:" + selectorIndex);
			}

			break;
		case "/next":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("stable_sign_nextUnit"));
			structSign.setDirection(sb.getData());
			structSign.setType("next");
			structSign.setAction(sb.keyvalues.get("id"));
			structSign.update();
			this.addConstructSign(structSign);
			CivGlobal.addConstructSign(structSign);

			break;
		case "/horsespawn":
			this.horseSpawnCoord = absCoord;
			break;
		case "/mulespawn":
			this.muleSpawnCoord = absCoord;
			break;
		}
	}

	public void setNonResidentFee(double d) {
		this.getNonMemberFeeComponent().setFeeRate(d);
	}

}
