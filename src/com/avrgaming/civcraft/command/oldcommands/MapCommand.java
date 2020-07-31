package com.avrgaming.civcraft.command.oldcommands;

import org.bukkit.command.*;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;

public class MapCommand implements CommandExecutor
{
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        if (args.length < 1) {
            CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_map_mapLink", "http://95.216.74.3:5551"));
        }
        else {
            final StringBuilder combineArgs = new StringBuilder();
            for (int i = 0; i < args.length; ++i) {
                combineArgs.append(args[i]).append((i == args.length - 1) ? "" : " ");
            }
            CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_map_mapLinkAddon", "http://95.216.74.3:5551" + combineArgs.toString().replace(" ", "_"), combineArgs.toString()));
        }
        return true;
    }
}
