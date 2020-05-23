
package com.avrgaming.civcraft.loregui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.loregui.GuiAction;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class WondersGui implements GuiAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident res = CivGlobal.getResident(player);
		Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54, CivSettings.localize.localizedString("resident_structuresGuiHeading"));
		Town town = res.getSelectedTown();
		if (town == null) town = res.getTown();
		if (town == null) return;
		double rate = 1.0;
		rate -= town.getBuffManager().getEffectiveDouble("buff_rush");
		rate -= town.getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
		rate -= town.getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");

		for (ConfigBuildableInfo info : CivSettings.wonders.values()) {
			double cost = info.cost;
			if (res.getCiv().getCapitol() != null && res.getCiv().getCapitol().getBuffManager().hasBuff("level10_architectorTown")) {
				cost *= 0.9;
			}
			double hammer_cost = Math.round(info.hammer_cost * rate);

			String money_requ = "§b" + CivSettings.localize.localizedString("money_requ", cost);
			String hammers_requ = "§a" + CivSettings.localize.localizedString("hammers_requ", hammer_cost);
			String ppoints = "§d" + CivSettings.localize.localizedString("ppoints", info.points);

			ItemStack is;
			if (!town.hasTechnology(info.require_tech)) {
				ConfigTech tech = CivSettings.techs.get(info.require_tech);
				is = LoreGuiItem.build(info.displayName, Material.GOLD_INGOT, //
						money_requ, hammers_requ, ppoints, //
						CivColor.Red + CivSettings.localize.localizedString("req") + tech.name, //
						"§3" + CivSettings.localize.localizedString("clicktoresearch"));
				is = LoreGuiItem.setAction(is, "ResearchGui");
				is = LoreGuiItem.setActionData(is, "info", tech.name);
				inv.addItem(is);
				continue;
			}
			if (!town.hasUpgrade(info.require_upgrade)) {
				ConfigTownUpgrade upgrade = CivSettings.townUpgrades.get(info.require_upgrade);
				is = LoreGuiItem.build(info.displayName, Material.BLAZE_ROD, //
						money_requ, hammers_requ, ppoints, //
						CivColor.Red + CivSettings.localize.localizedString("req") + upgrade.name, //
						"§3" + CivSettings.localize.localizedString("clicktoresearch"));
				is = LoreGuiItem.setAction(is, "UpgradeGuiBuy");
				is = LoreGuiItem.setActionData(is, "info", upgrade.name);
				inv.addItem(is);
				continue;
			}
			if (!town.hasStructure(info.require_structure)) {
				ConfigBuildableInfo structure = CivSettings.structures.get(info.require_structure);
				is = LoreGuiItem.build(info.displayName, Material.EMERALD, //
						money_requ, hammers_requ, ppoints, //
						CivColor.Red + CivSettings.localize.localizedString("requ") + structure.displayName, //
						"§3" + CivSettings.localize.localizedString("clicktobuild"));
				is = LoreGuiItem.setAction(is, "WonderGuiBuild");
				is = LoreGuiItem.setActionData(is, "info", structure.displayName);
				inv.addItem(is);
				continue;
			}
			if (!info.isAvailable(res.getTown())) {
				CivLog.debug("DIAMOND");
				is = LoreGuiItem.build(info.displayName, Material.DIAMOND, //
						money_requ, hammers_requ, ppoints, //
						CivSettings.localize.localizedString("town_buildwonder_errorNotAvailable"));
				CivLog.debug(is.toString());
				inv.addItem(is);
				continue;
			}
			if (!Wonder.isWonderAvailable(info.id)) {
				is = LoreGuiItem.build(info.displayName, Material.DIAMOND_SWORD, //
						money_requ, hammers_requ, ppoints, //
						"§c" + CivSettings.localize.localizedString("town_buildwonder_errorBuiltElsewhere"));
				inv.addItem(is);
				continue;
			}
			is = LoreGuiItem.build(info.displayName, Material.DIAMOND_BLOCK, //
					"§6" + CivSettings.localize.localizedString("clicktobuild"), //
					money_requ, hammers_requ, ppoints);
			is = LoreGuiItem.setAction(is, "WonderGuiBuild");
			is = LoreGuiItem.setActionData(is, "info", info.displayName);
			inv.addItem(is);
			continue;
 		}
		CivLog.debug("size = " + inv.getSize());
		ItemStack backButton = LoreGuiItem.build("Back", ItemManager.getMaterialId(Material.MAP), 0, CivSettings.localize.localizedString("bookReborn_backTo", inv.getName()));
		backButton = LoreGuiItem.setAction(backButton, "OpenInventory");
		backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
		backButton = LoreGuiItem.setActionData(backButton, "invName", inv.getName());
		inv.setItem(53, backButton);
		LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
		player.openInventory(inv);
	}
}
