
package com.avrgaming.civcraft.gui.action.book;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigSpaceMissions;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;

import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

public class CivSpaceEnded implements GuiItemAction {
	public static GuiInventory guiInventory;

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident interactor = CivGlobal.getResident(player);
		Civilization civ = interactor.getCiv();
		int ended = civ.getCurrentMission();
		guiInventory = new GuiInventory(player,null)//
				.setRow(1)//
				.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceEndedHeading"));
		for (int i = 1; i < ended; ++i) {
			ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(i);
			guiInventory.addGuiItem(GuiItems.newGuiItem(ItemManager.createItemStack(ItemManager.getMaterialId(Material.STAINED_GLASS_PANE), (short) CivCraft.civRandom.nextInt(15), 1))//
					.setTitle("§a" + configSpaceMissions.name)//
					.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
					.setAction("CivSpaceComponentsNormal")//
					.setActionData("i", String.valueOf(i))//
					.setActionData("b", "true"));
		}
		guiInventory.openInventory();
	}
}
