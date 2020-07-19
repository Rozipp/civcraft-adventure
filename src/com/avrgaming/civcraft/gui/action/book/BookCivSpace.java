
package com.avrgaming.civcraft.gui.action.book;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;

public class BookCivSpace implements GuiItemAction {
	public static GuiInventory guiInventory;

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		guiInventory = new GuiInventory(player, null)//
				.setRow(1)//
				.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceHeading"));

		guiInventory.addGuiItem(0, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceProgressHeading"))//
				.setMaterial(Material.MAP)//
				.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
				.setAction("BookCivSpaceProgress"));

		guiInventory.addGuiItem(1, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceEndedHeading"))//
				.setMaterial(Material.MAP)//
				.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
				.setAction("BookCivSpaceEnded"));

		guiInventory.addGuiItem(2, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceFutureHeading"))//
				.setMaterial(Material.MAP)//
				.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
				.setAction("BookCivSpaceFuture"));

		guiInventory.openInventory();
	}
}
