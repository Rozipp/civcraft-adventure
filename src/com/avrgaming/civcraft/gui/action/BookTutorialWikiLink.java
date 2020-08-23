
package com.avrgaming.civcraft.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.main.CivMessage;


public class BookTutorialWikiLink
implements GuiItemAction {
    @Override
    public void performAction(Player player, ItemStack stack) {
        CivMessage.send(player, "§a" + CivSettings.localize.localizedString("cmd_wiki_wikiLink", "http://wiki.minetexas.com/index.php/Civcraft_Wiki"));
    }
}

