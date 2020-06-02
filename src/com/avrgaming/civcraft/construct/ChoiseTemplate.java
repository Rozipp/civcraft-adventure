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
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.PerkComponent;

public class ChoiseTemplate implements CallbackInterface {

	private Player player;
	private Resident resident;
	private Construct construct;
	private CallbackInterface beginCallback;
	
	public ChoiseTemplate(Player player, Construct construct, CallbackInterface beginCallback) {
		this.player = player;
		this.resident = CivGlobal.getResident(player);
		this.construct = construct;
		this.beginCallback = beginCallback;
		resident.clearInteractiveMode();
		resident.undoPreview();
		Set<Perk> perkList;
		if (construct.getTown() != null)
			perkList = construct.getTown().getTemplatePerks(resident, construct.getInfo());
		else {
			perkList = new HashSet<>();
			for (Perk perk : Perk.staticPerks.values()) {
				PerkComponent pc = perk.getComponent("CustomTemplate");
				if (pc == null) continue;
				String template = pc.getString("template");
				if (template !=null && template.equalsIgnoreCase(construct.getInfo().template_name)) {
					perkList.add(perk);
				}
			}
		}
		if (perkList.size() == 0) {
			CivLog.debug("perkList.size() == 0");
			this.execute();
			return;
		}

		/* Store the pending buildable. */
		resident.pendingCallback = this;

		/* Build an inventory full of templates to select. */
		Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9);
		ItemStack infoRec = LoreGuiItem.build(CivSettings.localize.localizedString("buildable_lore_default") + " " + construct.getDisplayName(), ItemManager.getMaterialId(Material.WRITTEN_BOOK), 0,
				CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
		infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
		infoRec = LoreGuiItem.setActionData(infoRec, "perk", "");
		infoRec.getItemMeta().addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		inv.addItem(infoRec);

		for (Perk perk : perkList) {
			infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data, CivColor.Gold + "<Click To Build>", CivColor.Gray + "Provided by: " + CivColor.LightBlue + perk.provider);
			// for camp
			// infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data, CivColor.Gold +
			// CivSettings.localize.localizedString("loreGui_template_clickToBuild"),
			// CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_providedBy") + " " + CivColor.LightBlue +
			// CivSettings.localize.localizedString("loreGui_template_Yourself"));

			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
			infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getConfigId());
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
		try {
			if (strings.length > 0) {
				String theme = strings[0];
				Template old_tpl = construct.getTemplate();
				String tplPath;
				tplPath = Template.getTemplateFilePath(construct.getInfo().template_name, old_tpl.getDirection(), theme);
				Template tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				construct.setTemplate(tpl);
			}
			BuildableStatic.buildPlayerPreview(player, construct);
			CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
			resident.pendingCallback = beginCallback;
			TaskMaster.asyncTask(new StructureValidator(player, construct, beginCallback), 0);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

}
