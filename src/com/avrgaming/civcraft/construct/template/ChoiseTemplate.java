package com.avrgaming.civcraft.construct.template;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class ChoiseTemplate implements CallbackInterface {

	private Resident resident;
	private CallbackInterface beginCallback;
	
	public ChoiseTemplate(Player player, ConfigBuildableInfo sinfo, CallbackInterface beginCallback) {
		this.resident = CivGlobal.getResident(player);
		this.beginCallback = beginCallback;
		resident.clearInteractiveMode();
		resident.undoPreview();

		/* Store the pending buildable. */
		resident.pendingCallback = this;

		/* Build an inventory full of templates to select. */
		Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9);
		String constructName = sinfo.template_name;
		for (ConfigTheme theme : ConfigTheme.getConfigThemeForConstruct(constructName)) {
			ItemStack infoRec = LoreGuiItem.build(theme.display_name, theme.item_id, theme.data, CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
			// for camp
			// infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data, CivColor.Gold +
			// CivSettings.localize.localizedString("loreGui_template_clickToBuild"),
			// CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_providedBy") + " " + CivColor.LightBlue +
			// CivSettings.localize.localizedString("loreGui_template_Yourself"));

			infoRec = LoreGuiItem.setAction(infoRec, "SelectTemplate");
			infoRec = LoreGuiItem.setActionData(infoRec, "theme", theme.simple_name);
			infoRec.getItemMeta().addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			inv.addItem(infoRec);
		}
		// for (Perk perk : personalUnboundPerks) {
		// infoRec = LoreGuiItem.build(perk.getDisplayName(), CivData.BEDROCK, perk.configPerk.data, CivColor.Gold +
		// CivSettings.localize.localizedString("loreGui_template_clickToBuild"),
		// CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound"), CivColor.Gray +
		// CivSettings.localize.localizedString("loreGui_template_unbound2"),
		// CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound3"), CivColor.Gray +
		// CivSettings.localize.localizedString("loreGui_template_unbound4"),
		// CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound5"));
		// infoRec = LoreGuiItem.setAction(infoRec, "ActivatePerk");
		// infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getConfigId());
		// inv.addItem(infoRec);
		// }
		/* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
		player.openInventory(inv);
	}

	@Override
	public void execute(String... strings) {
		if (strings.length > 0) {
			beginCallback.execute(strings[0]);
		}
	}

}
