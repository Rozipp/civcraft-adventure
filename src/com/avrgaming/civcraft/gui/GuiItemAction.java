package com.avrgaming.civcraft.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public interface GuiItemAction {
	public void performAction(Player player, ItemStack stack);
}
