package com.avrgaming.civcraft.construct;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.PerkComponent;

public class ChoiseTemplateDbg implements CallbackInterface {

	private Player player;
	private Resident resident;
	private ConfigBuildableInfo info;
	private CallbackInterface beginCallback;

	public ChoiseTemplateDbg(Player player, ConfigBuildableInfo info, CallbackInterface beginCallback) {
		this.player = player;
		this.resident = CivGlobal.getResident(player);
		this.beginCallback = beginCallback;
		this.info = info;
		resident.clearInteractiveMode();
		resident.undoPreview();
		Set<Perk> perkList;
		perkList = new HashSet<>();
		for (Perk perk : Perk.staticPerks.values()) {
			PerkComponent pc = perk.getComponent("CustomTemplate");
			if (pc == null) continue;
			String template = pc.getString("template");
			if (template != null && template.equalsIgnoreCase(info.template_name)) {
				perkList.add(perk);
			}
		}
		if (perkList.size() == 0) {
			this.execute("default");
			return;
		}

		/* Store the pending buildable. */
		resident.pendingCallback = this;

		/* Build an inventory full of templates to select. */
		Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9);
		ItemStack infoRec = LoreGuiItem.build(CivSettings.localize.localizedString("buildable_lore_default") + " " + info.displayName, ItemManager.getMaterialId(Material.WRITTEN_BOOK), 0,
				CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
		infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
		infoRec = LoreGuiItem.setActionData(infoRec, "perk", "default");
		infoRec.getItemMeta().addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		inv.addItem(infoRec);

		for (Perk perk : perkList) {
			infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data, CivColor.Gold + "<Click To Build>", CivColor.Gray + "Provided by: " + CivColor.LightBlue + perk.provider);

			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
			infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getConfigId());
			infoRec.getItemMeta().addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			inv.addItem(infoRec);
		}
		player.openInventory(inv);
	}

	@Override
	public void execute(String... strings) {
		try {
			if (strings.length > 0) {
				String theme = strings[0];
				String tplPath = Template.getTemplateFilePath(player.getLocation(), info , theme);
				Template tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				beginCallback.execute(theme);
			}
			
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

}
