package com.avrgaming.civcraft.loreguiinventory;

import java.text.SimpleDateFormat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class ResidentPage extends GuiInventory {

	public ResidentPage(Player player, String arg) {
		super(player, arg);
		SimpleDateFormat sdf = CivGlobal.dateFormat;

		this.setRow(1).setTitle(CivSettings.localize.localizedString("bookReborn_resInfoHeading"));
		this.addGuiItem(0, GuiItems.newGuiItem(ItemManager.createItemStack(ItemManager.getMaterialId(Material.SKULL_ITEM), (short) 3, 1))//
				.setTitle(CivSettings.localize.localizedString("bookReborn_infoMenu_name"))//
				.setLore(CivColor.LightGray + "Player: " + getResident().getName(), //
						"§6" + CivSettings.CURRENCY_NAME + ": " + "§a" + getResident().getTreasury().getBalance(), //
						"§2" + CivSettings.localize.localizedString("cmd_res_showRegistrationDate", //
								new StringBuilder().append("§a").append(sdf.format(getResident().getRegistered())).toString()), //
						"§b" + CivSettings.localize.localizedString("Civilization") + " " + getResident().getCivName(), //
						"§d" + CivSettings.localize.localizedString("Town") + " " + getResident().getTownName(), //
						CivColor.Red + CivSettings.localize.localizedString("Camp") + getResident().getCampName()));
		this.addLastItem(CivSettings.localize.localizedString("bookReborn_backToDashBoard"));
	}

}
