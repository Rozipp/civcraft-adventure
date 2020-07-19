
package com.avrgaming.civcraft.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;

public class CloseInventory
implements GuiItemAction {
    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        GuiInventory.closeInventory((Player) event.getWhoClicked());
    }
}

