package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;

public class SelectTemplate implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		player.closeInventory();
		Resident resident = CivGlobal.getResident(player);
		String theme = LoreGuiItem.getActionData(stack, "theme");
		resident.pendingCallback.execute(theme);
	}
}
