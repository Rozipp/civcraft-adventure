package com.avrgaming.civcraft.lorestorage;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;

public interface GuiItemAction {
	public void performAction(InventoryClickEvent event, ItemStack stack) throws CivException;
}
