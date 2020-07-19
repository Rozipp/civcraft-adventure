package com.avrgaming.civcraft.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public interface GuiItemAction {
	public void performAction(InventoryClickEvent event, ItemStack stack);
}
