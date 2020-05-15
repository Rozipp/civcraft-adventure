package com.avrgaming.civcraft.loregui;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.threading.TaskMaster;

public class BuildWithDefaultPersonalTemplate implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);
		ConfigBuildableInfo info = resident.pendingBuildableInfo;

		try {
			String path = Template.getTemplateFilePath(info.template_name, Template.getDirection(player.getLocation()), null);
			Template tpl = Template.getTemplate(path);
			Location centerLoc = BuildableStatic.repositionCenterStatic(player.getLocation(), info.templateYShift, tpl);
			TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, resident.pendingCallback), 0);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
		player.closeInventory();
	}

}
