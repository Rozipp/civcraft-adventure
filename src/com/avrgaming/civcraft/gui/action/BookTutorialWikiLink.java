
package com.avrgaming.civcraft.gui.action;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.main.CivMessage;


public class BookTutorialWikiLink
implements GuiItemAction {
    @Override
    public void performAction(InventoryClickEvent event, ItemStack stack) {
        CivMessage.send((Object)event.getWhoClicked(), "§a" + CivSettings.localize.localizedString("cmd_wiki_wikiLink", "http://wiki.minetexas.com/index.php/Civcraft_Wiki"));
    }
}

