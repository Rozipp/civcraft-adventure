
package ua.rozipp.gui.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import ua.rozipp.gui.GuiAction;

public class BookShowDonateMenu extends GuiAction {
    @Override
    public void performAction(Player player, ItemStack stack) {
        Bukkit.dispatchCommand(player, (String)"buy");
    }
}

