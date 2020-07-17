package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.global.perks.Perk;

public class ActivatePerk implements GuiItemAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player)event.getWhoClicked();
		Resident resident = CivGlobal.getResident((Player)event.getWhoClicked());
		String perk_id = GuiItems.getActionData(stack, "perk");
		Perk perk = resident.perks.get(perk_id);
		if (perk != null) {
				perk.onActivate(resident);
		} else {
			CivLog.error(perk_id+" "+CivSettings.localize.localizedString("loreGui_perkActivationFailed"));
		}
		GuiInventory.closeInventory(player);		
	}
}
