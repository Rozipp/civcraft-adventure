package com.avrgaming.civcraft.command.oldcommands;

import org.bukkit.command.*;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;

public class WikiCommand implements CommandExecutor
{
    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        if (args.length < 1) {
            CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_wiki_wikiLink", "https://wiki.furnex.ru/index.php?title=Введение"));
        }
        else {
            final StringBuilder combineArgs = new StringBuilder();
            for (int i = 0; i < args.length; ++i) {
                combineArgs.append(args[i]).append((i == args.length - 1) ? "" : " ");
            }
            CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_wiki_wikiLinkAddon", "https://wiki.furnex.ru/index.php?title=Введение" + combineArgs.toString().replace(" ", "_"), combineArgs.toString()));
        }
        return true;
    }
}
