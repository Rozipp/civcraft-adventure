package com.avrgaming.civcraft.gui.action;

import java.util.ArrayDeque;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;

public class OpenBackInventory implements GuiItemAction {

	@Override
	public void performAction(Player player, ItemStack stack) {
		ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(player.getUniqueId());
		gis.pop();
		
		if (!gis.isEmpty()) {
			player.openInventory(gis.getFirst().getInventory());
			GuiInventory.setInventoryStack(player.getUniqueId(), gis);
		} else
			player.closeInventory();
	}

}
