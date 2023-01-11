
package ua.rozipp.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import ua.rozipp.gui.GuiAction;
import com.avrgaming.civcraft.main.CivMessage;


public class BookLinksAction extends GuiAction {
    @Override
    public void performAction(Player player, ItemStack stack) {
        CivMessage.send(player, "§a" + CivSettings.localize.localizedString("cmd_wiki_wikiLink", "http://wiki.minetexas.com/index.php/Civcraft_Wiki"));
        try {
			String url = CivSettings.getStringBase("dynmap_url");
			if (!url.isEmpty()) {
		        CivMessage.send(player, "§2" + CivSettings.localize.localizedString("cmd_map_dynmapLink", url));
			}
		} catch (InvalidConfiguration e) {
	        CivMessage.send(player, "§2" + CivSettings.localize.localizedString("cmd_map_dynmapLink", "None"));
		}
    }
}
