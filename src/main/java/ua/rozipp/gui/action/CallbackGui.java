package ua.rozipp.gui.action;

import java.util.ArrayDeque;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiAction;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.CallbackInterface;

public class CallbackGui extends GuiAction {

	@Override
	public void performAction(Player player, ItemStack stack) {
		if (player == null) return;
		String callbackData = GuiItem.getActionData(stack, "data");

		CallbackInterface callback = CivGlobal.getResident(player).getPendingCallback();
		if (callback == null) {
			ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(player.getUniqueId());
			gis.pop().execute(callbackData, player.getUniqueId().toString());
		} else
			callback.execute(callbackData, player.getUniqueId().toString());
	}
}
