
package com.avrgaming.civcraft.loregui.space;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;

public class BookCivSpace implements GuiItemAction {
	public static GuiInventory guiInventory;

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		guiInventory = new GuiInventory(player, 1, CivSettings.localize.localizedString("bookReborn_civSpaceHeading"), event.getInventory().getName());

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

		guiInventory.addGuiItem(8, GuiItems.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("loreGui_recipes_back"))//
				.setMaterial(Material.MAP)//
				.setLore(CivSettings.localize.localizedString("bookReborn_backToDashBoard"))//
				.setAction("OpenInventory")//
				.setActionData("invType", "showGuiInv").setActionData("invName", Book.guiInventory.getName()));

		guiInventory.openInventory(player);
	}
}
