package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class BuildTemplateDbg implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		player.closeInventory();
		Resident resident = CivGlobal.getResident(player);
		try {
			String buildName = LoreGuiItem.getActionData(stack, "info");
			ConfigBuildableInfo sinfo = CivSettings.structures.get(buildName);
			if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildName);
			resident.pendingCallback.execute(buildName);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}
}
