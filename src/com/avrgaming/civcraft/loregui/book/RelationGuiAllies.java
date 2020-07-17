
package com.avrgaming.civcraft.loregui.book;

import java.text.SimpleDateFormat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;

import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;

public class RelationGuiAllies implements GuiItemAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			Civilization civ = CivGlobal.getCivFromName(GuiItems.getActionData(stack, "civilization"));
			SimpleDateFormat sdf = CivGlobal.dateFormat;
			GuiInventory inventory = new GuiInventory(player, CivSettings.localize.localizedString("resident_relationsGui_ally"), BookRelationsGui.inventory.getName());
			for (Relation relation : civ.getDiplomacyManager().getRelations()) {
				if (relation.getStatus() == Relation.Status.ALLY) {
					inventory.addGuiItem(GuiItems.newGuiItem()//
							.setMaterial(Material.EMERALD_BLOCK)//
							.setLore(ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_relationToString", relation.toString()), //
									"§6" + CivSettings.localize.localizedString("relation_creationDate", sdf.format(relation.getCreatedDate()))));
				}
			}
			inventory.addBackItem(CivSettings.localize.localizedString("bookReborn_back"), CivSettings.localize.localizedString("bookReborn_backTo", BookRelationsGui.inventory.getName()));
			inventory.openInventory(player);
		}
	}
}
