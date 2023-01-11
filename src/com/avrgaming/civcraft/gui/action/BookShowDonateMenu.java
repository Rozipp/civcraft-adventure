
package com.avrgaming.civcraft.gui.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiItemAction;

public class BookShowDonateMenu
implements GuiItemAction {
    @Override
    public void performAction(Player player, ItemStack stack) {
        Bukkit.dispatchCommand(player, (String)"buy");
    }
}

