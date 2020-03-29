package com.avrgaming.civcraft.loregui;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.global.perks.Perk;

public class BuildWithPersonalTemplate implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);

		ConfigBuildableInfo info = resident.pendingBuildableInfo;
		try {
			/* get the template name from the perk's CustomTemplate component. */
			String perk_id = LoreGuiItem.getActionData(stack, "perk");
			Perk perk = Perk.staticPerks.get(perk_id);
			Template tpl = Template.getTemplate(Template.getTemplateFilePath(player.getLocation(), resident.pendingBuildableInfo,
					perk.getComponent("CustomPersonalTemplate").getString("theme")));
			Location centerLoc = BuildableStatic.repositionCenterStatic(player.getLocation(), info.templateYShift, tpl);
			TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, resident.pendingCallback), 0);
			resident.desiredTemplate = tpl;
			player.closeInventory();
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

}
