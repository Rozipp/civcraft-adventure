package com.avrgaming.civcraft.command;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.permission.PermissionGroup;

public class EnderChestCommand extends CommandBase
{
    @Override
    public void init() {
        this.command = "/enderchest";
    }
    
    @Override
    public void doDefaultAction() throws CivException {
        final Player sender = this.getPlayer();
        if (!sender.getWorld().getName().equalsIgnoreCase("world")) {
            throw new CivException("§c" + CivSettings.localize.localizedString("cmd_enderchest_inArena"));
        }
        final Inventory enderChest = sender.getEnderChest();
        sender.openInventory(enderChest);
    }
    
    @Override
    public void showHelp() {
        this.showBasicHelp();
    }
    
    @Override
    public void permissionCheck() throws CivException {
        if (!this.sender.hasPermission("civcraft.enderchest") && !this.sender.isOp() && !this.sender.hasPermission("civcraft.ec") && !PermissionGroup.hasGroup(this.sender.getName(), "ultra") && !PermissionGroup.hasGroup(this.sender.getName(), "deluxe")) {
            throw new CivException("§c" + CivSettings.localize.localizedString("cmd_enderchest_NoPermissions"));
        }
    }
}
