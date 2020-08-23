package com.avrgaming.civcraft.gui.action;

import java.util.ArrayDeque;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.CallbackInterface;

public class CallbackGui implements GuiItemAction {

	@Override
	public void performAction(Player player, ItemStack stack) {
		if (player == null) return;
		String callbackData = GuiItems.getActionData(stack, "data");

		CallbackInterface callback = CivGlobal.getResident(player).getPendingCallback();
		if (callback == null) {
			ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(player.getUniqueId());
			gis.pop().execute(callbackData, player.getUniqueId().toString());
		} else
			callback.execute(callbackData, player.getUniqueId().toString());
	}
}
