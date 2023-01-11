
package com.avrgaming.civcraft.gui.action.book;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigSpaceMissions;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

public class BookCivSpaceEnded implements GuiItemAction {
	public static GuiInventory guiInventory;

	@Override
	public void performAction(Player player, ItemStack stack) {
		try {
			Resident interactor = CivGlobal.getResident(player);
			if (interactor.getCiv() == null) {
				throw new CivException(CivSettings.localize.localizedString("var_bookcivspacegui_noCiv"));
			}
			Civilization civ = interactor.getCiv();
			if (!civ.GM.isLeader(interactor)) {
				throw new CivException(CivSettings.localize.localizedString("var_bookcivspacegui_noLeader", civ.getName()));
			}
			int ended = civ.getCurrentMission();

			guiInventory = new GuiInventory(player, null, null)//
					.setRow(1)//
					.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceEndedHeading"));
			for (int i = 1; i < ended; ++i) {
				ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(i);
				guiInventory.addGuiItem(GuiItems.newGuiItem()//
						.setTitle("§a" + configSpaceMissions.name).setStack(ItemManager.createItemStack(ItemManager.getMaterialId(Material.STAINED_GLASS_PANE), CivCraft.civRandom.nextInt(15)))//
						.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
						.setAction("CivSpaceComponents")//
						.setActionData("i", String.valueOf(i))//
						.setActionData("b", "true"));
			}
			guiInventory.openInventory(player);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}
}
