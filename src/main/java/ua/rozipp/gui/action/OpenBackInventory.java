package ua.rozipp.gui.action;

import java.util.ArrayDeque;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiAction;

public class OpenBackInventory extends GuiAction {

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
