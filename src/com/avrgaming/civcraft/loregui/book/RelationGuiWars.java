
package com.avrgaming.civcraft.loregui.book;

import java.text.SimpleDateFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.loregui.GuiAction;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;

import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.util.ItemManager;

public class RelationGuiWars implements GuiAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Civilization civ = CivGlobal.getCiv(LoreGuiItem.getActionData(stack, "civilization"));
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		ItemStack itemStack = null;
		Inventory inventory = Bukkit.getServer().createInventory((InventoryHolder) event.getWhoClicked(), 54, CivSettings.localize.localizedString("resident_relationsGui_war"));
		for (Relation relation : civ.getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Relation.Status.WAR) {
				itemStack = LoreGuiItem.build("", ItemManager.getMaterialId(Material.REDSTONE_BLOCK), 0, (Object) ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_relationToString", relation.toString()),
						"§6" + CivSettings.localize.localizedString("relation_creationDate", sdf.format(relation.getCreatedDate())));
			}
			if (itemStack == null) continue;
			inventory.addItem(itemStack);
		}
		ItemStack backButton = LoreGuiItem.build(CivSettings.localize.localizedString("bookReborn_back"), ItemManager.getMaterialId(Material.MAP), 0,
				CivSettings.localize.localizedString("bookReborn_backTo", BookRelationsGui.inventory.getName()));
		backButton = LoreGuiItem.setAction(backButton, "OpenInventory");
		backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
		backButton = LoreGuiItem.setActionData(backButton, "invName", BookRelationsGui.inventory.getName());
		inventory.setItem(53, backButton);
		LoreGuiItemListener.guiInventories.put(inventory.getName(), inventory);
		((Player) event.getWhoClicked()).openInventory(inventory);
	}
}
