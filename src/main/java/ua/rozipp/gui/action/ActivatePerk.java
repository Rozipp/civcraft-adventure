package ua.rozipp.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiAction;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.global.perks.Perk;
import ua.rozipp.gui.GuiItem;

public class ActivatePerk extends GuiAction {

	@Override
	public void performAction(Player player, ItemStack stack) {
		Resident resident = CivGlobal.getResident(player);
		String perk_id = GuiItem.getActionData(stack, "perk");
		Perk perk = resident.perks.get(perk_id);
		if (perk != null) {
				perk.onActivate(resident);
		} else {
			CivLog.error(perk_id+" "+CivSettings.localize.localizedString("loreGui_perkActivationFailed"));
		}
		GuiInventory.closeInventory(player);		
	}
}
