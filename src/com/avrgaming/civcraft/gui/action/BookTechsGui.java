
package com.avrgaming.civcraft.gui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;

public class BookTechsGui implements GuiItemAction {
	@Override
	public void performAction(Player player, ItemStack stack) {
		Resident whoClicked = CivGlobal.getResident(player);
		if (whoClicked.getTown() == null) {
			CivMessage.send((Object) player, "§c" + CivSettings.localize.localizedString("res_gui_noTown"));
			GuiInventory.closeInventory(player);
			return;
		}
		Civilization civ = whoClicked.getCiv();
		if (!civ.GM.isLeaderOrAdviser(whoClicked)) {
			CivMessage.send((Object) player, "§c" + CivSettings.localize.localizedString("cmd_NeedHigherCivRank"));
			GuiInventory.closeInventory(player);
			return;
		}
		CivMessage.send(player, "Сейчас в разработке"); // TODO BookTechsGui
		GuiInventory.closeInventory(player);
	}
}
