package com.avrgaming.civcraft.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;

public class OpenInventory implements GuiItemAction {

	@Override
	public void performAction(Player player, ItemStack stack) {
		GuiInventory.openGuiInventory(player, GuiItems.getActionData(stack, "className"), GuiItems.getActionData(stack, "arg"));
	}

}
