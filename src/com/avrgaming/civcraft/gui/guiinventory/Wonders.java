package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.config.ConfigConstructInfo.ConstructType;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItem;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.interactive.BuildCallback;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class Wonders extends GuiInventory {

	public Wonders(Player player, String arg) throws CivException {
		super(player, player, arg);
		Boolean isTutorial = Boolean.parseBoolean(arg);
		if (!isTutorial) {
			if (getResident().getTown() == null)
				isTutorial = true;
			else {
				Town town = getResident().getTown();
				if (getResident().getSelectedTown() != null) {
					try {
						getResident().getSelectedTown().validateResidentSelect(getResident());
					} catch (CivException e) {
						CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid", getResident().getSelectedTown().getName(), getResident().getTown().getName()));
						getResident().setSelectedTown(getResident().getTown());
						town = getResident().getTown();
					}
					town = getResident().getSelectedTown();
					this.setTown(town);
				}
				if (!town.GM.isMayorOrAssistant(getResident()) && !town.getCiv().GM.isLeader(getResident())) {
					isTutorial = true;
				}
			}
		}
		if (isTutorial) this.setPlayer(null);
		this.setTitle(CivSettings.localize.localizedString("resident_structuresGuiHeading") + (getTown() != null ? " " + getTown().getName() : " Tutorial"));

		double rate = 1.0;
		if (!isTutorial) {
			rate -= getTown().getBuffManager().getEffectiveDouble("buff_rush");
			rate -= getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
			rate -= getTown().getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");
		}
		GuiItem gi;
		for (ConfigConstructInfo info : CivSettings.constructs.values()) {
			if (info.type != ConstructType.Wonder) continue;
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
				ConfigConstructInfo structure = CivSettings.constructs.get(info.require_structure);
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
		GuiInventory.closeInventory(getPlayer());
		try {
			String buildId = strings[0];
			ConfigConstructInfo sinfo = CivSettings.constructs.get(buildId);
			if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildId);
			getResident().setPendingCallback(new BuildCallback(getPlayer(), sinfo, getTown()));
		} catch (CivException e) {
			CivMessage.sendError(getPlayer(), e.getMessage());
		}
	}

}
