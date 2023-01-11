
package ua.rozipp.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiAction;

public class CloseInventory extends GuiAction {
    @Override
    public void performAction(Player player, ItemStack stack) {
        GuiInventory.closeInventory(player);
    }
}

