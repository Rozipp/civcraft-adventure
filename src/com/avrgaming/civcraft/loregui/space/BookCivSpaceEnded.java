
package com.avrgaming.civcraft.loregui.space;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigSpaceMissions;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

public class BookCivSpaceEnded implements GuiItemAction {
	public static GuiInventory guiInventory;

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident interactor = CivGlobal.getResident(player);
		if (interactor.getCiv() == null) {
			CivMessage.sendError((Object) player, CivSettings.localize.localizedString("var_bookcivspacegui_noCiv"));
			return;
		}
		Civilization civ = interactor.getCiv();
		if (!civ.GM.isLeader(interactor)) {
			CivMessage.sendError((Object) player, CivSettings.localize.localizedString("var_bookcivspacegui_noLeader", civ.getName()));
			return;
		}
		int ended = civ.getCurrentMission();
		guiInventory = new GuiInventory(player, 1, CivSettings.localize.localizedString("bookReborn_civSpaceEndedHeading"), event.getInventory().getName());
		for (int i = 1; i < ended; ++i) {
			ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(i);
			guiInventory.addGuiItem(GuiItems.newGuiItem()//
					.setTitle("§a" + configSpaceMissions.name).setStack(ItemManager.createItemStack(ItemManager.getMaterialId(Material.STAINED_GLASS_PANE), CivCraft.civRandom.nextInt(15)))//
					.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
					.setAction("CivSpaceComponents")//
					.setActionData("i", String.valueOf(i))//
					.setActionData("b", "true"));
		}
		guiInventory.addBackItem(CivSettings.localize.localizedString("loreGui_recipes_back"));
		guiInventory.openInventory(player);
	}
}
