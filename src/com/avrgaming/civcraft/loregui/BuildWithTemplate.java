package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.global.perks.Perk;

public class BuildWithTemplate implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		player.closeInventory();

		Resident resident = CivGlobal.getResident(player);

		String perk_id = LoreGuiItem.getActionData(stack, "perk");
		if (perk_id != null && !perk_id.isEmpty()) {
			Perk perk = Perk.staticPerks.get(perk_id);
			if (perk == null) {
				CivLog.error(perk_id + " " + CivSettings.localize.localizedString("loreGui_perkActivationFailed"));
				return;
			}

			String theme = perk.getComponent("CustomTemplate").getString("theme");
			if (theme != null && !theme.isEmpty()) {
				resident.pendingCallback.execute(theme);
				return;
			}
		}
		resident.pendingCallback.execute();
	}
}
