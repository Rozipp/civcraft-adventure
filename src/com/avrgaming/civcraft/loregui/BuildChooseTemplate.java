package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.BuildCallback;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class BuildChooseTemplate implements GuiItemAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		GuiInventory.closeInventory(player);
		Resident resident = CivGlobal.getResident(player);
		Town town = resident.getTown();
		if (resident != null && resident.getTown() != null) {
			if (resident.getSelectedTown() != null) {
				try {
					resident.getSelectedTown().validateResidentSelect(resident);
				} catch (CivException e) {
					CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid", resident.getSelectedTown().getName(), resident.getTown().getName()));
					resident.setSelectedTown(resident.getTown());
					town = resident.getTown();
				}
				town = resident.getSelectedTown();
			}
		}
		try {
			String buildName = GuiItems.getActionData(stack, "info");
			ConfigBuildableInfo sinfo = CivSettings.structures.get(buildName);
			if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildName);
			new BuildCallback(player, sinfo, town);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}
}
