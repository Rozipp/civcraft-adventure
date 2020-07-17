
package com.avrgaming.civcraft.loregui.space;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.civ.CivSpaceCommand;
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
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class CivSpaceFuture implements GuiItemAction {
	public static GuiInventory guiInventory;

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident interactor = CivGlobal.getResident(player);
		Civilization civ = interactor.getCiv();
		if (civ.getCurrentMission() >= 8) {
			CivMessage.sendError((Object) player, CivSettings.localize.localizedString("var_spaceshuttle_end", CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) 7)).name));
			return;
		}
		int current = civ.getCurrentMission();
		if (current == 7 && civ.getMissionActive()) {
			CivMessage.sendError((Object) player, CivSettings.localize.localizedString("var_spaceshuttle_end", CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) 7)).name));
			return;
		}
		if (civ.getMissionActive()) ++current;
		guiInventory = new GuiInventory(player, 1, CivSettings.localize.localizedString("bookReborn_civSpaceFutureHeading"), CivSpaceCommand.guiInventory.getName());
		for (int i = current; i <= 7; ++i) {
			ConfigSpaceMissions configSpaceMissions = CivSettings.spacemissions_levels.get(i);
			guiInventory.addGuiItem(GuiItems.newGuiItem(ItemManager.createItemStack(ItemManager.getMaterialId(Material.GLASS), (short) CivCraft.civRandom.nextInt(15), 1))//
					.setTitle(CivColor.Red + configSpaceMissions.name)//
					.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
					.setAction("CivSpaceComponentsNormal")//
					.setActionData("i", String.valueOf(i))//
					.setActionData("b", "false"));
		}
		guiInventory.addBackItem();
		guiInventory.openInventory(player);
	}
}
