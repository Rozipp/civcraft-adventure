package com.avrgaming.civcraft.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Utils {

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static String getClearInventoryName(Inventory inv){
        String clearName = ChatColor.translateAlternateColorCodes('&', inv.getName());
        clearName = ChatColor.stripColor(clearName);
        return clearName;
    }

    public static String getClearItemName(ItemStack itemStack){
        String clearName = ChatColor.translateAlternateColorCodes('&', itemStack.getItemMeta().getDisplayName());
        clearName = ChatColor.stripColor(clearName);
        return clearName;
    }

    public static String materialTransform(String materialName){
        switch (materialName){
            case "STONE":
                return "Камень";
            case "WOOL":
                return "Шерсть";
            default:
                return "Неизвестный предмет";
        }
    }
}