package ua.rozipp.gui.guiinventory;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigNewspaper;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class NewsPaper extends GuiInventory {

	public NewsPaper(Player player, String arg) throws CivException {
		super(player, null, arg);
		this.setTitle(CivColor.LightGreenBold + CivSettings.localize.localizedString("bookReborn_news_heading"));
		for (int i = 0; i < 27; ++i) {
			Random rand = CivCraft.civRandom;
			this.addGuiItem(i, GuiItem.newGuiItem(ItemManager.createItemStack(ItemManager.getMaterialId(Material.STAINED_GLASS_PANE), (short) rand.nextInt(15), 1)).setTitle(""));
		}
		for (ConfigNewspaper news : CivSettings.newspapers.values()) {
			boolean useAllLines;
			try {
				double version = Double.parseDouble(news.version);
				useAllLines = version <= 2.2;
			} catch (NumberFormatException twoFourFive) {
				useAllLines = false;
			}
			this.addGuiItem(news.guiData, useAllLines //
					? GuiItem.newGuiItem(ItemManager.createItemStack(news.item, news.iData.shortValue(), 1))//
							.setTitle(CivColor.WhiteBold + news.headline + " " + CivColor.WhiteBold + news.lineotd)//
							.setLore(CivColor.LightGrayItalic + news.date, //
									CivColor.LightGreenBold + "Aura:", "§f" + news.line1, "§f" + news.line2, "§f" + news.line3, //
									"§bAlcor:", "§f" + news.line4, "§f" + news.line5, "§f" + news.line6, //
									CivColor.LightPurpleBold + "Orion:", "§f" + news.line7, "§f" + news.line8, "§f" + news.line9, //
									CivColor.GoldBold + "Tauri:", "§f" + news.line10, "§f" + news.line11, "§f" + news.line12, //
									"Version: " + news.version)
					: GuiItem.newGuiItem(ItemManager.createItemStack(news.item, news.iData.shortValue(), 1))//
							.setTitle(CivColor.WhiteBold + news.headline + " " + CivColor.WhiteBold + news.lineotd)//
							.setLore(CivColor.LightGrayItalic + news.date, //
									CivColor.LightGreenBold + "Orion:", "§f" + news.line7, "§f" + news.line8, "§f" + news.line9, //
									CivColor.LightPurpleBold + "Tauri:", "§f" + news.line10, "§f" + news.line11, "§f" + news.line12, //
									"Version: " + news.version));
		}
	}

}
