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

public class ShowTemplateType implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		// TODO Auto-generated method stub
		Resident resident = CivGlobal.getResident((Player)event.getWhoClicked());
		String perk_id = LoreGuiItem.getActionData(stack, "perk");
		Perk perk = resident.perks.get(perk_id);
		if (perk != null) {
			if (perk.getIdent().startsWith("template_arctic"))
			{
				GuiPage.showTemplatePerks(resident, "arctic");
			}
			else if (perk.getIdent().startsWith("template_atlantean"))
			{
				GuiPage.showTemplatePerks(resident, "atlantean");
			}
			else if (perk.getIdent().startsWith("template_aztec"))
			{
				GuiPage.showTemplatePerks(resident, "aztec");
			}
			else if (perk.getIdent().startsWith("template_cultist"))
			{
				GuiPage.showTemplatePerks(resident, "cultist");
			}
			else if (perk.getIdent().startsWith("template_egyptian"))
			{
				GuiPage.showTemplatePerks(resident, "egyptian");
			}
			else if (perk.getIdent().startsWith("template_elven"))
			{
				GuiPage.showTemplatePerks(resident, "elven");
			}
			else if (perk.getIdent().startsWith("template_roman"))
			{
				GuiPage.showTemplatePerks(resident, "roman");
			}
			else if (perk.getIdent().startsWith("template_hell"))
			{
				GuiPage.showTemplatePerks(resident, "hell");
			}
			else if (perk.getIdent().startsWith("template_medieval"))
			{
				GuiPage.showTemplatePerks(resident, "medieval");
			}
		} else {
			CivLog.error(perk_id+" "+CivSettings.localize.localizedString("loreGui_perkActivationFailed"));
		}
	}

}
