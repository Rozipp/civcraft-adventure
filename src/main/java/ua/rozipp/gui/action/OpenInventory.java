package ua.rozipp.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiAction;
import ua.rozipp.gui.GuiItem;

public class OpenInventory extends GuiAction {

	@Override
	public void performAction(Player player, ItemStack stack) {
		GuiInventory.openGuiInventory(player, GuiItem.getActionData(stack, "className"), GuiItem.getActionData(stack, "arg"));
	}

}
