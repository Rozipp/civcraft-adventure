package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItem;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class Structure extends GuiInventory {

	public Structure(Player player, String arg) {
		super(player, arg);
		Boolean isTutorial = Boolean.parseBoolean(arg);
		if (!isTutorial) {
			Town town = getResident().getSelectedTown();
			if (town == null) town = getResident().getTown();
			if (town == null) return;
			this.setTown(town);
		}

		if (!getTown().GM.isMayorOrAssistant(getResident()) && !getTown().getCiv().GM.isLeader(getResident())) isTutorial = true;

		this.setTitle(CivSettings.localize.localizedString("resident_structuresGuiHeading") + " " + getTown().getName());

		double rate = 1.0;
		rate -= getTown().getBuffManager().getEffectiveDouble("buff_rush");
		rate -= getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
		rate -= getTown().getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");

		for (ConfigBuildableInfo info : CivSettings.structures.values()) {
			double hammerCost = Math.round(info.hammer_cost * rate);
			GuiItem gi = GuiItems.newGuiItem(ItemManager.createItemStack(info.gui_item_id, (short) 0, 1))//
					.setTitle(info.displayName)//
					.setLore("§b" + CivSettings.localize.localizedString("money_requ", Double.parseDouble(String.valueOf(info.cost))), //
							"§a" + CivSettings.localize.localizedString("hammers_requ", hammerCost), //
							"§d" + CivSettings.localize.localizedString("upkeep_day", info.upkeep)); //
			if (!getTown().getCiv().hasTechnologys(info.require_tech)) {
				ConfigTech tech = CivSettings.techs.get(info.require_tech);
				gi.setMaterial(Material.REDSTONE)//
						.addLore(CivColor.Red + CivSettings.localize.localizedString("req") + tech.name, //
								"§3" + CivSettings.localize.localizedString("clicktoresearch"), //
								"§d" + CivSettings.localize.localizedString("era_this", tech.era))//
						.setOpenInventory("TechPage", isTutorial.toString());
				this.addGuiItem(gi);
				continue;
			}
			ConfigBuildableInfo str = CivSettings.structures.get(info.require_structure);
			if (str != null) {
				gi.setMaterial(Material.BEDROCK).setLore(CivColor.Red + CivSettings.localize.localizedString("requ") + str.displayName, //
						"§3" + CivSettings.localize.localizedString("clicktobuild"));
				this.addGuiItem(gi);
				continue;
			}
			if (info.isAvailable(getTown())) {
				gi.addLore("§6" + CivSettings.localize.localizedString("clicktobuild")); //
				if (!isTutorial) gi.setAction("BuildChooseTemplate").setActionData("info", info.id);

				continue;
			}
			this.addGuiItem(info.gui_slot, gi);
		}
		this.addGuiItem(52, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("4udesa"))//
				.setMaterial(Material.DIAMOND_BLOCK)//
				.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
				.setOpenInventory("Wonders", isTutorial.toString()));
		this.addLastItem(CivSettings.localize.localizedString("bookReborn_backToDashBoard"));
	}

}
