package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItem;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.interactive.BuildCallback;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.CivColor;

public class Wonders extends GuiInventory {

	public Wonders(Player player, String arg) throws CivException {
		super(player, arg);
		Boolean isTutorial = Boolean.parseBoolean(arg);
		if (!isTutorial) {
			Town town = getResident().getSelectedTown();
			if (town == null) town = getResident().getTown();
			if (town == null) return;
			this.setTown(town);
		}
		this.setTitle(CivSettings.localize.localizedString("resident_structuresGuiHeading"));

		double rate = 1.0;
		if (!isTutorial) {
			rate -= getTown().getBuffManager().getEffectiveDouble("buff_rush");
			rate -= getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
			rate -= getTown().getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");
		}
		GuiItem gi;
		for (ConfigBuildableInfo info : CivSettings.wonders.values()) {
			double cost = info.cost;
			double hammer_cost = Math.round(info.hammer_cost * rate);

			String money_requ = "§b" + CivSettings.localize.localizedString("money_requ", cost);
			String hammers_requ = "§a" + CivSettings.localize.localizedString("hammers_requ", hammer_cost);
			String ppoints = "§d" + CivSettings.localize.localizedString("ppoints", info.points);

			gi = GuiItems.newGuiItem()//
					.setTitle(info.displayName)//
					.setLore(money_requ, hammers_requ, ppoints);

			if (!getTown().hasTechnology(info.require_tech)) {
				ConfigTech tech = CivSettings.techs.get(info.require_tech);
				gi.setMaterial(Material.GOLD_INGOT)//
						.addLore(CivColor.Red + CivSettings.localize.localizedString("req") + tech.name, //
								"§3" + CivSettings.localize.localizedString("clicktoresearch"))//
						.setOpenInventory("TechPage", isTutorial.toString());
				this.addGuiItem(gi);
				continue;
			}
			if (!getTown().hasUpgrade(info.require_upgrade)) {
				ConfigTownUpgrade upgrade = CivSettings.townUpgrades.get(info.require_upgrade);
				gi.setMaterial(Material.BLAZE_ROD)//
						.setLore(money_requ, hammers_requ, ppoints, //
								CivColor.Red + CivSettings.localize.localizedString("req") + upgrade.name, //
								"§3" + CivSettings.localize.localizedString("clicktoresearch"))//
						.setOpenInventory("UpgradeBuy", isTutorial.toString());
				this.addGuiItem(gi);
				continue;
			}
			if (!getTown().BM.hasStructure(info.require_structure)) {
				ConfigBuildableInfo structure = CivSettings.structures.get(info.require_structure);
				this.addGuiItem(GuiItems.newGuiItem()//
						.setMaterial(Material.EMERALD) //
						.addLore(CivColor.Red + CivSettings.localize.localizedString("requ") + structure.displayName, //
								"§3" + CivSettings.localize.localizedString("clicktobuild"))//
						.setAction("StructureGuiBuild")//
						.setActionData("info", structure.displayName));
				continue;
			}
			if (!info.isAvailable(getTown())) {
				this.addGuiItem(GuiItems.newGuiItem()//
						.setMaterial(Material.DIAMOND) //
						.addLore(CivSettings.localize.localizedString("town_buildwonder_errorNotAvailable")));
				continue;
			}
			if (!Wonder.isWonderAvailable(info.id)) {
				this.addGuiItem(GuiItems.newGuiItem()//
						.setTitle(info.displayName)//
						.setMaterial(Material.DIAMOND_SWORD) //
						.addLore("§c" + CivSettings.localize.localizedString("town_buildwonder_errorBuiltElsewhere")));
				continue;
			}
			this.addGuiItem(GuiItems.newGuiItem()//
					.setMaterial(Material.DIAMOND_BLOCK) //
					.addLore("§6" + CivSettings.localize.localizedString("clicktobuild")) //
					.setCallbackGui(info.id));
			continue;
		}
	}

	@Override
	public void execute(String... strings) {
		try {
			GuiInventory.closeInventory(getPlayer());
			try {
				String buildId = strings[0];
				ConfigBuildableInfo sinfo = CivSettings.wonders.get(buildId);
				if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildId);
				getResident().setPendingCallback(new BuildCallback(getPlayer(), sinfo, getTown()));
			} catch (CivException e) {
				CivMessage.sendError(getPlayer(), e.getMessage());
			}
		} catch (CivException e1) {
			// TODO Автоматически созданный блок catch
			e1.printStackTrace();
		}
	}

}
