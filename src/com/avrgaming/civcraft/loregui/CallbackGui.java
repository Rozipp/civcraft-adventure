package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CallbackInterface;

public class CallbackGui implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		if (player == null) return;
		
		Resident resident = CivGlobal.getResident(player);
		CallbackInterface callback = resident.getPendingCallback();
		if (callback == null) {
			CivMessage.sendError(player, "Запрос уже не действителен");
			player.closeInventory();
			return;
		}

		String callbackData = LoreGuiItem.getActionData(stack, "data");
		callback.execute(player.getName(), callbackData);
	}
}
