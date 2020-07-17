package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.global.perks.Perk;

public class PerkPage extends GuiInventory {

	public PerkPage(Player player, String arg) {
		super(player, arg);
		this.setTitle(CivSettings.localize.localizedString("resident_perksGuiHeading"));
		for (Perk p : getResident().perks.values()) {
			if (p.getConfigId().startsWith("temp")) {
				this.addGuiItem(GuiItems.newGuiItem(ItemManager.createItemStack(p.configPerk.type_id, p.configPerk.data.shortValue(), 1))//
						.setTitle(p.configPerk.display_name)//
						.setLore("§b" + CivSettings.localize.localizedString("resident_perksGuiClickToView"), //
								"§b" + CivSettings.localize.localizedString("resident_perksGuiTheseTemplates"))//
						.setAction("ShowTemplateType")//
						.setActionData("perk", p.configPerk.id));
			} else
				if (p.getConfigId().startsWith("perk")) {
					this.addGuiItem(GuiItems.newGuiItem(ItemManager.createItemStack(p.configPerk.type_id, p.configPerk.data.shortValue(), 1))//
							.setTitle(p.getDisplayName())//
							.setLore("§6" + CivSettings.localize.localizedString("resident_perksGui_clickToActivate"), "Неограниченное использование")//
							.setAction("ActivatePerk")//
							.setActionData("perk", p.configPerk.id));
				}
			return;
		}
	}

}
