
package com.avrgaming.civcraft.loregui.book;

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
import com.avrgaming.civcraft.loregui.OpenInventoryTask;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;

import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class BookWondersGui
implements GuiAction {
    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Player player = (Player)event.getWhoClicked();
        Resident res = CivGlobal.getResident(player);
        Inventory inv = Bukkit.getServer().createInventory((InventoryHolder)player, 54, CivSettings.localize.localizedString("resident_wonersGuiHeading"));
        CivSettings.wonders.values().stream().map(info -> {
            ItemStack is;
            double cost = info.cost;
            if (res.getCiv().getCapitol() != null && res.getCiv().getCapitol().getBuffManager().hasBuff("level10_architectorTown")) {
                cost *= 0.9;
            }
            if (!res.getTown().hasTechnology(info.require_tech)) {
                ConfigTech tech = CivSettings.techs.get(info.require_tech);
                is = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.GOLD_INGOT), 0, "§b" + CivSettings.localize.localizedString("money_requ", cost), "§a" + CivSettings.localize.localizedString("hammers_requ", info.hammer_cost), "§d" + CivSettings.localize.localizedString("ppoints", info.points), CivColor.Red + CivSettings.localize.localizedString("req") + tech.name, "§3" + CivSettings.localize.localizedString("clicktoresearch"));
                is = LoreGuiItem.setAction(is, "ResearchGui");
                is = LoreGuiItem.setActionData(is, "info", tech.name);
            } else if (!res.getSelectedTown().hasUpgrade(info.require_upgrade)) {
                ConfigTownUpgrade upgrade = CivSettings.townUpgrades.get(info.require_upgrade);
                is = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.BLAZE_ROD), 0, "§b" + CivSettings.localize.localizedString("money_requ", cost), "§a" + CivSettings.localize.localizedString("hammers_requ", info.hammer_cost), "§d" + CivSettings.localize.localizedString("ppoints", info.points), CivColor.Red + CivSettings.localize.localizedString("req") + upgrade.name, "§3" + CivSettings.localize.localizedString("tutorial_lore_clicktoView"));
                is = LoreGuiItem.setAction(is, "UpgradeGuiBuy");
                is = LoreGuiItem.setActionData(is, "info", upgrade.name);
            } else if (!res.getSelectedTown().hasStructure(info.require_structure)) {
                ConfigBuildableInfo structure = CivSettings.structures.get(info.require_structure);
                is = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.EMERALD), 0, "§b" + CivSettings.localize.localizedString("money_requ", cost), "§a" + CivSettings.localize.localizedString("hammers_requ", info.hammer_cost), "§d" + CivSettings.localize.localizedString("ppoints", info.points), CivColor.Red + CivSettings.localize.localizedString("requ") + structure.displayName, "§3" + CivSettings.localize.localizedString("clicktobuild"));
                is = LoreGuiItem.setAction(is, "WonderGuiBuild");
                is = LoreGuiItem.setActionData(is, "info", structure.displayName);
            } else if (!info.isAvailable(res.getTown())) {
                is = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.DIAMOND), 0, "§b" + CivSettings.localize.localizedString("money_requ", cost), "§a" + CivSettings.localize.localizedString("hammers_requ", info.hammer_cost), "§d" + CivSettings.localize.localizedString("ppoints", info.points), CivSettings.localize.localizedString("town_buildwonder_errorNotAvailable"));
            } else if (!Wonder.isWonderAvailable(info.id)) {
                is = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.DIAMOND_SWORD), 0, "§b" + CivSettings.localize.localizedString("money_requ", cost), "§a" + CivSettings.localize.localizedString("hammers_requ", info.hammer_cost), "§d" + CivSettings.localize.localizedString("ppoints", info.points), "§c"+CivSettings.localize.localizedString("town_buildwonder_errorBuiltElsewhere"));
            } else {
                is = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.DIAMOND_BLOCK), 0, "§6" + CivSettings.localize.localizedString("clicktobuild"), "§b" + CivSettings.localize.localizedString("money_requ", cost), "§a" + CivSettings.localize.localizedString("hammers_requ", info.hammer_cost), "§d" + CivSettings.localize.localizedString("ppoints", info.points));
                is = LoreGuiItem.setAction(is, "WonderGuiBuild");
                is = LoreGuiItem.setActionData(is, "info", info.displayName);
            }
            return is;
        }
        ).forEachOrdered(is -> {
            inv.addItem(new ItemStack[]{is});
        }
        );
        ItemStack backButton = LoreGuiItem.build(CivSettings.localize.localizedString("bookReborn_back"), ItemManager.getMaterialId(Material.MAP), 0, CivSettings.localize.localizedString("bookReborn_backTo", BookStructuresGui.inv.getName()));
        backButton = LoreGuiItem.setAction(backButton, "OpenInventory");
        backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
        backButton = LoreGuiItem.setActionData(backButton, "invName", BookStructuresGui.inv.getName());
        inv.setItem(53, backButton);
        LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
        TaskMaster.syncTask(new OpenInventoryTask(player, inv));
    }
}

