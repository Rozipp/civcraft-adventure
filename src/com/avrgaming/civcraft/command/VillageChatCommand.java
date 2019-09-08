package com.avrgaming.civcraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class VillageChatCommand implements CommandExecutor
{
    public boolean onCommand(final CommandSender sender, final Command cmd, final String commandLabel, final String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        final Player player = (Player)sender;
        final Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_civchat_notResident"));
            return false;
        }
        if (args.length == 0) {
            resident.setVillageChat(!resident.isVillageChat());
            resident.setCivChat(false);
            resident.setTownChat(false);
            resident.save();
            CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_villagechat_modeSet") + " " + resident.isVillageChat());
            return true;
        }
        if (resident.getVillage() == null) {
            CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_villagechat_error"));
            return false;
        }
        if (CivGlobal.isChatDisAllowed(player)) {
            return false;
        }
        final StringBuilder fullArgs = new StringBuilder();
        for (final String arg : args) {
            fullArgs.append(arg).append(" ");
        }
        CivMessage.sendVillageChat(resident.getVillage(), resident, "<%s> %s", fullArgs.toString());
        return true;
    }
}
