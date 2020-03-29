package com.avrgaming.civcraft.loregui;

import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.PerkComponent;

public class BuildWithTemplate implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);

		String perk_id = LoreGuiItem.getActionData(stack, "perk");

		try {
			String filepath = null;
			if (perk_id != null) {
				/* Use a template defined by a perk. */
				Perk perk = Perk.staticPerks.get(perk_id);
				if (perk == null) {
					CivLog.error(perk_id + " " + CivSettings.localize.localizedString("loreGui_perkActivationFailed"));
					player.closeInventory();
					return;
				}

				/* get the template name from the perk's CustomTemplate component. */
				PerkComponent perkComp = perk.getComponent("CustomTemplate");
				filepath = Template.getTemplateFilePath(player.getLocation(), resident.pendingBuildable.getInfo(), perkComp.getString("theme"));

			} else {
				/* Use the default template. */
				filepath = Template.getTemplateFilePath(player.getLocation(), resident.pendingBuildable.getInfo(), null);
			}
			resident.pendingBuildable.setTemplate(Template.getTemplate(filepath));
			BuildableStatic.buildPlayerPreview(player, player.getLocation(), resident.pendingBuildable);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		} catch (IOException e) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("internalIOException"));
			e.printStackTrace();
		}
		player.closeInventory();
	}

}
