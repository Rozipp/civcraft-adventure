
package com.avrgaming.civcraft.gui.action.book;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class CivSpaceProgress implements GuiItemAction {
	@Override
	public void performAction(Player player, ItemStack stack) {
		try {
			Resident interactor = CivGlobal.getResident(player);
			Civilization civ = interactor.getCiv();
			if (!civ.getMissionActive()) {
				CivMessage.sendError((Object) player, CivSettings.localize.localizedString("var_spaceshuttle_noProgress"));
				return;
			}
			GuiInventory guiInventory = new GuiInventory(player, null, null)//
					.setRow(1)//
					.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceProgressHeading"));
			String[] split = civ.getMissionProgress().split(":");
			String missionName = CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) civ.getCurrentMission())).name;
			double beakers = Math.round(Double.parseDouble(split[0]));
			double hammers = Math.round(Double.parseDouble(split[1]));
			int percentageCompleteBeakers = (int) ((double) Math.round(Double.parseDouble(split[0])) / Double.parseDouble(CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) civ.getCurrentMission())).require_beakers)
					* 100.0);
			int percentageCompleteHammers = (int) ((double) Math.round(Double.parseDouble(split[1])) / Double.parseDouble(CivSettings.spacemissions_levels.get((Object) Integer.valueOf((int) civ.getCurrentMission())).require_hammers)
					* 100.0);
			guiInventory.addGuiItem(0, GuiItems.newGuiItem()//
					.setTitle("§b" + missionName)//
					.setMaterial(Material.DIAMOND_SWORD)//
					.setLore("§6" + CivSettings.localize.localizedString("beakers") + " " + beakers + CivColor.Red + "(" + percentageCompleteBeakers + "%)", //
							"§d" + CivSettings.localize.localizedString("hammers") + " " + hammers + CivColor.Red + "(" + percentageCompleteHammers + "%)"));
			guiInventory.openInventory(player);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}
}
