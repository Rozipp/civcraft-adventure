package com.avrgaming.civcraft.gui.action;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CallbackInterface;

public class CallbackGui implements GuiItemAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		if (!GuiInventory.isGuiInventory(event.getClickedInventory())) return;
		Player player = (Player) event.getWhoClicked();
		if (player == null) return;
		String callbackData = GuiItems.getActionData(stack, "data");

		try {
			ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(player.getUniqueId());
			gis.pop().execute(callbackData);
		} catch (NoSuchElementException e) {
			Resident resident = CivGlobal.getResident(player);
			CallbackInterface callback = resident.getPendingCallback();
			if (callback == null) {
				CivMessage.sendError(player, "Запрос уже не действителен");
				GuiInventory.closeInventory(player);
				return;
			}
			callback.execute(callbackData);
		}

	}
}
