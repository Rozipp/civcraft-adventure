package com.avrgaming.civcraft.gui.action;


import gpl.AttributeUtil;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiItemAction;


public class SpawnItem implements GuiItemAction {

	@SuppressWarnings("deprecation")
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.removeCivCraftProperty("GUI");
		attrs.removeCivCraftProperty("GUI_ACTION");

		ItemStack is = attrs.getStack();
		if (event.getClick().equals(ClickType.SHIFT_LEFT) ||
			event.getClick().equals(ClickType.SHIFT_RIGHT)) {
			is.setAmount(is.getMaxStackSize());
		}
		
		event.setCursor(is);
	}

}
