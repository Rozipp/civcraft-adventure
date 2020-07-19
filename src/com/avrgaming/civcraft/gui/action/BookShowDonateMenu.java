
package com.avrgaming.civcraft.gui.action;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiItemAction;

public class BookShowDonateMenu
implements GuiItemAction {
    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        Bukkit.dispatchCommand((CommandSender)event.getWhoClicked(), (String)"buy");
    }
}

