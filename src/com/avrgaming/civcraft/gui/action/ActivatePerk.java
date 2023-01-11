package com.avrgaming.civcraft.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.global.perks.Perk;

public class ActivatePerk implements GuiItemAction {

	@Override
	public void performAction(Player player, ItemStack stack) {
		Resident resident = CivGlobal.getResident(player);
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
