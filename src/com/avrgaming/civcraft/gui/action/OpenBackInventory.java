package com.avrgaming.civcraft.gui.action;

import java.util.ArrayDeque;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;

public class OpenBackInventory implements GuiItemAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(player.getUniqueId());
		gis.pop();
		
		if (!gis.isEmpty()) {
			player.openInventory(gis.getFirst().getInventory());
			GuiInventory.setInventoryStack(player.getUniqueId(), gis);
		} else
			player.closeInventory();
	}

}