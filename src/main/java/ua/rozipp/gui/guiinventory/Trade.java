package ua.rozipp.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class Trade extends GuiInventory {

	public Trade(Player player, String arg) throws CivException {
		super(player, player, arg);
		Resident tradeResident = CivGlobal.getResident(arg);
		if (tradeResident == null) throw new CivException("Resident " + arg + " not found");
		this.setRow(5);
		this.setTitle(this.getName() + " : " + tradeResident.getName());

		/* Set up top and bottom layer with buttons. */

		/* Top part which is for the other resident. */
		GuiItem signStack = GuiItem.newGuiItem(ItemManager.createItemStack(CivData.WOOL, (short) CivData.DATA_WOOL_WHITE, 1));
		this.addGuiItem(8, GuiItem.newGuiItem(ItemManager.createItemStack(CivData.WOOL, (short) CivData.DATA_WOOL_RED, 1))//
				.setTitle(tradeResident.getName() + " Confirm")//
				.setLore(CivColor.LightGreen + CivSettings.localize.localizedString("var_resident_tradeWait1", CivColor.LightBlue + tradeResident.getName()), //
						CivColor.LightGray + " " + CivSettings.localize.localizedString("resident_tradeWait2")));
		this.addGuiItem(7, GuiItem.newGuiItem()//
				.setTitle(CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"))//
				.setMaterial(Material.NETHER_BRICK_ITEM)//
				.setLore(CivColor.Yellow + "0 " + CivSettings.CURRENCY_NAME));
		for (int i = 0; i < 7; i++) {
			this.addGuiItem(i, signStack);
		}

		int start = 4 * 9;
		this.addGuiItem(start, GuiItem.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("resident_tradeRemove") + " " + CivSettings.CURRENCY_NAME)//
				.setMaterial(Material.NETHER_BRICK_ITEM)//
				.setLore(CivColor.Gold + CivSettings.localize.localizedString("resident_tradeRemove100") + " " + CivSettings.CURRENCY_NAME, //
						CivColor.Gold + CivSettings.localize.localizedString("resident_tradeRemove1000") + " " + CivSettings.CURRENCY_NAME));
		this.addGuiItem(start + 1, GuiItem.newGuiItem()//
				.setTitle(CivSettings.localize.localizedString("resident_tradeAdd") + " " + CivSettings.CURRENCY_NAME)//
				.setMaterial(Material.GOLD_INGOT)//
				.setLore(CivColor.Gold + CivSettings.localize.localizedString("resident_tradeAdd100") + " " + CivSettings.CURRENCY_NAME, //
						CivColor.Gold + CivSettings.localize.localizedString("resident_tradeAdd1000") + " " + CivSettings.CURRENCY_NAME));
		this.addGuiItem(start + 7, GuiItem.newGuiItem()//
				.setTitle(CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"))//
				.setMaterial(Material.NETHER_BRICK_ITEM)//
				.setLore(CivColor.Yellow + "0 " + CivSettings.CURRENCY_NAME));
		this.addGuiItem(start + 8, GuiItem.newGuiItem(ItemManager.createItemStack(CivData.WOOL, (short) CivData.DATA_WOOL_RED, 1))//
				.setTitle(CivSettings.localize.localizedString("resident_tradeYourConfirm"))//
				.setLore(CivColor.Gold + CivSettings.localize.localizedString("resident_tradeClicktoConfirm")));
		for (int i = start + 2; i < (start + 8); i++) {
			this.addGuiItem(i, signStack);
		}

		/* Set up middle divider. */
		start = 2 * 9;
		for (int i = start; i < (9 + start); i++) {
			this.addGuiItem(i, signStack);
		}
	}

}
