
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
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class BookCivSpaceFuture implements GuiItemAction {
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
			if (civ.getCurrentMission() >= 8) {
				throw new CivException(CivSettings.localize.localizedString("var_spaceshuttle_end", CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) 7)).name));
			}
			int current = civ.getCurrentMission();
			if (current == 7 && civ.getMissionActive()) {
				throw new CivException(CivSettings.localize.localizedString("var_spaceshuttle_end", CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) 7)).name));
			}
			if (civ.getMissionActive()) {
				++current;
			}

			guiInventory = new GuiInventory(player, null, null)//
					.setRow(1)//
					.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceFutureHeading"));

			for (int i = current; i <= 7; ++i) {
				ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(i);
				guiInventory.addGuiItem(GuiItems.newGuiItem(ItemManager.createItemStack(ItemManager.getMaterialId(Material.GLASS), (short) CivCraft.civRandom.nextInt(15), 1))//
						.setTitle(CivColor.Red + configSpaceMissions.name)//
						.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
						.setAction("CivSpaceComponents")//
						.setActionData("i", String.valueOf(i))//
						.setActionData("b", "false"));
			}
			guiInventory.openInventory(player);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}
}
