package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;

public class OpenInventory implements GuiItemAction  {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) throws CivException {
		Player player = (Player) event.getWhoClicked();
		
		String className = GuiItems.getActionData(stack, "className");
		String data = GuiItems.getActionData(stack, "arg");
		
		GuiInventory.getGuiInventory(player, className, data).openInventory();
	}

}
