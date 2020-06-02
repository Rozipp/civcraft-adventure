package com.avrgaming.civcraft.loregui;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loregui.book.BookShowPerkGui;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.global.perks.Perk;

public class GuiPage {

	public static void showTechPage(Resident resident) throws CivException {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		Civilization civ = resident.getCiv();
		if (civ == null) {
			throw new CivException(CivColor.Red + CivSettings.localize.localizedString("cmd_getSenderCivNoCiv"));
		}
		final int type = ItemManager.getMaterialId(Material.EMERALD_BLOCK);
		final ArrayList<ConfigTech> techs = ConfigTech.getAvailableTechs(civ);
		final Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54,
				CivSettings.localize.localizedString("resident_techsGuiHeading"));
		for (final ConfigTech tech : techs) {
			final String techh = tech.name;
			ItemStack itemStack = LoreGuiItem.build(tech.name, type, 0, "§6" + CivSettings.localize.localizedString("clicktoresearch"),
					"§b" + CivSettings.localize.localizedString("money_req", tech.getAdjustedTechCost(civ)),
					"§a" + CivSettings.localize.localizedString("bealers_req", tech.getAdjustedBeakerCost(civ)),
					"§d" + CivSettings.localize.localizedString("era_this", tech.era));
			itemStack = LoreGuiItem.setAction(itemStack, "ResearchGui");
			itemStack = LoreGuiItem.setActionData(itemStack, "info", techh);
			inv.addItem(itemStack);
		}
		player.openInventory(inv);
	}

	public static void showRelationPage(Resident resident) throws CivException {
		if (resident.getCiv() == null) {
			throw new CivException(CivColor.Red + CivSettings.localize.localizedString("cmd_getSenderCivNoCiv"));
		}
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		final Inventory inventory = Bukkit.getServer().createInventory((InventoryHolder) player, 9,
				CivSettings.localize.localizedString("resident_relationsGuiHeading"));
		ItemStack relation = LoreGuiItem.build(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_ally"),
				ItemManager.getMaterialId(Material.EMERALD_BLOCK), 0, ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_allyInfo"),
				"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"));
		relation = LoreGuiItem.setAction(relation, "RelationAllies");
		relation = LoreGuiItem.setActionData(relation, "civilization", resident.getCiv().getName());
		inventory.addItem(relation);
		relation = LoreGuiItem.build(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_peace"),
				ItemManager.getMaterialId(Material.LAPIS_BLOCK), 0, ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_peaceInfo"),
				"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"));
		relation = LoreGuiItem.setAction(relation, "RelationPeaces");
		relation = LoreGuiItem.setActionData(relation, "civilization", resident.getCiv().getName());
		inventory.addItem(relation);
		relation = LoreGuiItem.build(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_hostile"),
				ItemManager.getMaterialId(Material.GOLD_BLOCK), 0, ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_hostileInfo"),
				"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"));
		relation = LoreGuiItem.setAction(relation, "RelationHostiles");
		relation = LoreGuiItem.setActionData(relation, "civilization", resident.getCiv().getName());
		inventory.addItem(relation);
		relation = LoreGuiItem.build(CivColor.LightGreenBold + CivSettings.localize.localizedString("resident_relationsGui_war"),
				ItemManager.getMaterialId(Material.REDSTONE_BLOCK), 0, ChatColor.RESET + CivSettings.localize.localizedString("resident_relationsGui_warInfo"),
				"§6" + CivSettings.localize.localizedString("bookReborn_clickToView"));
		relation = LoreGuiItem.setAction(relation, "RelationWars");
		relation = LoreGuiItem.setActionData(relation, "civilization", resident.getCiv().getName());
		inventory.addItem(relation);
		player.openInventory(inventory);
	}

	public static void showStructPage(Resident resident) throws CivException {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		Civilization civ = resident.getCiv();
		Town town = resident.getSelectedTown();
		if (town == null) town = resident.getTown();
		if (town == null) {
			throw new CivException(CivColor.Red + CivSettings.localize.localizedString("cmd_notPartOfTown"));
		}
		Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54, CivSettings.localize.localizedString("resident_structuresGuiHeading"));
		double rate = 1.0;
		rate -= town.getBuffManager().getEffectiveDouble("buff_rush");
		rate -= town.getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
		rate -= town.getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");
		for (final ConfigBuildableInfo info : CivSettings.structures.values()) {
			final int type = ItemManager.getMaterialId(Material.EMERALD_BLOCK);
			final double hammerCost = Math.round(info.hammer_cost * rate);
			ItemStack itemStack;
			String money_requ = "§b" + CivSettings.localize.localizedString("money_requ", Double.parseDouble(String.valueOf(info.cost)));
			String hammers_requ = "§a" + CivSettings.localize.localizedString("hammers_requ", hammerCost);
			String upkeep_day = "§d" + CivSettings.localize.localizedString("upkeep_day", info.upkeep);
			if (town.getMayorGroup() == null || town.getAssistantGroup() == null || civ.getLeaderGroup() == null) {
				itemStack = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.REDSTONE_BLOCK), 0, money_requ, hammers_requ, upkeep_day,
						"§c" + CivSettings.localize.localizedString("belongtown"));
				inv.addItem(itemStack);
				continue;
			}
			if (!resident.getCiv().hasTechnology(info.require_tech)) {
				final ConfigTech tech = CivSettings.techs.get(info.require_tech);
				final String techh = tech.name;
				itemStack = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.REDSTONE), 0, money_requ, hammers_requ, upkeep_day,
						"§c" + CivSettings.localize.localizedString("req") + tech.name, "§3" + CivSettings.localize.localizedString("clicktoresearch"),
						"§d" + CivSettings.localize.localizedString("era_this", tech.era));
				itemStack = LoreGuiItem.setAction(itemStack, "ResearchGui");
				itemStack = LoreGuiItem.setActionData(itemStack, "info", techh);
				inv.addItem(itemStack);
				continue;
			}
			if (!town.getMayorGroup().hasMember(resident) && !town.getAssistantGroup().hasMember(resident) && !civ.getLeaderGroup().hasMember(resident)) {
				itemStack = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.REDSTONE_BLOCK), 0, money_requ, hammers_requ, upkeep_day,
						"§c" + CivSettings.localize.localizedString("belongtown"));
				inv.addItem(itemStack);
				continue;
			}
			if (info.isAvailable(town)) {
					itemStack = LoreGuiItem.build(info.displayName, type, 0, "§6" + CivSettings.localize.localizedString("clicktobuild"), money_requ,
							hammers_requ, upkeep_day);
					itemStack = LoreGuiItem.setAction(itemStack, "BuildChooseTemplate");
					itemStack = LoreGuiItem.setActionData(itemStack, "info", info.id);
					inv.addItem(itemStack);
					continue;
			} else {
				final ConfigBuildableInfo str = CivSettings.structures.get(info.require_structure);
				if (str != null) {
					final String req_build = str.displayName;
					itemStack = LoreGuiItem.build(info.displayName, ItemManager.getMaterialId(Material.BEDROCK), 0,
							"§c" + CivSettings.localize.localizedString("requ") + str.displayName, money_requ, hammers_requ, upkeep_day,
							"§3" + CivSettings.localize.localizedString("clicktobuild"));
					itemStack = LoreGuiItem.setAction(itemStack, "WonderGuiBuild");
					itemStack = LoreGuiItem.setActionData(itemStack, "info", req_build);
					inv.addItem(itemStack);
					continue;
				} else {
					continue;
				}
			}
		}
		ItemStack is = LoreGuiItem.build("§e" + CivSettings.localize.localizedString("4udesa"), ItemManager.getMaterialId(Material.DIAMOND_BLOCK), 0,
				"§6" + CivSettings.localize.localizedString("click_to_view"));
		is = LoreGuiItem.setAction(is, "WondersGui");
		inv.setItem(53, is);
		LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
		player.openInventory(inv);
	}

	public static void showUpgradePage(Resident resident) throws CivException {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		Town town = resident.getSelectedTown();
		if (town == null) town = resident.getTown();
		if (town == null) {
			throw new CivException(CivColor.Red + CivSettings.localize.localizedString("cmd_notPartOfTown"));
		}
		final Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54,
				CivSettings.localize.localizedString("resident_upgradesGuiHeading"));
		for (final ConfigTownUpgrade upgrade : ConfigTownUpgrade.getAllUpgrades(town)) {
			double cost = upgrade.cost;
			if (town.getCiv().getGovernment().id.equalsIgnoreCase("gov_theocracy")) {
				cost *= 0.9;
			}
			ItemStack is = null;
			if (upgrade.isAvailable(town)) {
				is = LoreGuiItem.build(upgrade.name, ItemManager.getMaterialId(Material.EMERALD_BLOCK), 0,
						"§b" + CivSettings.localize.localizedString("money_requ", Math.round(cost)),
						"§6" + CivSettings.localize.localizedString("tutorial_lore_clicktoView"));
				is = LoreGuiItem.setAction(is, "UpgradeGuiBuy");
				is = LoreGuiItem.setActionData(is, "info", upgrade.name);
			} else
				if (!town.hasStructure(upgrade.require_structure)) {
					final ConfigBuildableInfo structure = CivSettings.structures.get(upgrade.require_structure);
					is = LoreGuiItem.build(upgrade.name, ItemManager.getMaterialId(Material.EMERALD), 0,
							"§b" + CivSettings.localize.localizedString("money_requ", Math.round(cost)),
							"§c" + CivSettings.localize.localizedString("requ") + structure.displayName,
							"§3" + CivSettings.localize.localizedString("clicktobuild"));
					is = LoreGuiItem.setAction(is, "WonderGuiBuild");
					is = LoreGuiItem.setActionData(is, "info", structure.displayName);
				} else
					if (!town.hasUpgrade(upgrade.require_upgrade)) {
						final ConfigTownUpgrade upgrade2 = CivSettings.getUpgradeById(upgrade.require_upgrade);
						is = LoreGuiItem.build(upgrade.name, ItemManager.getMaterialId(Material.GLOWSTONE_DUST), 0,
								"§b" + CivSettings.localize.localizedString("money_requ", Math.round(cost)),
								"§c" + CivSettings.localize.localizedString("requ") + upgrade2.name,
								"§3" + CivSettings.localize.localizedString("clicktobuild"));
						is = LoreGuiItem.setAction(is, "UpgradeGuiBuy");
						is = LoreGuiItem.setActionData(is, "info", upgrade2.name);
					}
			if (is != null) {
				inv.addItem(is);
			}
		}
		player.openInventory(inv);
	}

	@SuppressWarnings("null")
	public void showPerkPage(Resident resident) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		final Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54,
				CivSettings.localize.localizedString("resident_perksGuiHeading"));
		resident.perks.values().stream().forEachOrdered(p -> {
			if (p.getConfigId().startsWith("temp")) {
				ItemStack stack;
				ItemStack stack2;
				ItemStack stack3;
				final Inventory inventory = null;
				stack = LoreGuiItem.build(p.configPerk.display_name, p.configPerk.type_id, p.configPerk.data,
						"§b" + CivSettings.localize.localizedString("resident_perksGuiClickToView"),
						"§b" + CivSettings.localize.localizedString("resident_perksGuiTheseTemplates"));
				stack2 = LoreGuiItem.setAction(stack, "ShowTemplateType");
				stack3 = LoreGuiItem.setActionData(stack2, "perk", p.configPerk.id);
				inventory.addItem(new ItemStack[]{stack3});
			} else
				if (p.getConfigId().startsWith("perk")) {
					final Inventory inventory = null;
					ItemStack stack4;
					ItemStack stack5;
					ItemStack stack6;
					stack4 = LoreGuiItem.build(p.getDisplayName(), p.configPerk.type_id, p.configPerk.data,
							"§6" + CivSettings.localize.localizedString("resident_perksGui_clickToActivate"),
							"\u041d\u0435\u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0435\u043d\u043d\u043e\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u0435");
					stack5 = LoreGuiItem.setAction(stack4, "ActivatePerk");
					stack6 = LoreGuiItem.setActionData(stack5, "perk", p.configPerk.id);
					inventory.addItem(new ItemStack[]{stack6});
				}
			return;
		});
		player.openInventory(inv);
	}

	public static void showPerkPage(Resident resident, int pageNumber) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}

		Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9, CivSettings.localize.localizedString("resident_perksGuiHeading"));

		for (Object obj : resident.perks.values()) {
			Perk p = (Perk) obj;

			if (p.getConfigId().startsWith("temp")) {
				ItemStack stack = LoreGuiItem.build(p.configPerk.display_name, p.configPerk.type_id, p.configPerk.data,
						CivColor.LightBlue + CivSettings.localize.localizedString("resident_perksGuiClickToView"),
						CivColor.LightBlue + CivSettings.localize.localizedString("resident_perksGuiTheseTemplates"));
				stack = LoreGuiItem.setAction(stack, "ShowTemplateType");
				stack = LoreGuiItem.setActionData(stack, "perk", p.configPerk.id);

				inv.addItem(stack);
			} else
				if (p.getConfigId().startsWith("perk")) {
					ItemStack stack = LoreGuiItem.build(p.getDisplayName(), p.configPerk.type_id, p.configPerk.data,
							CivColor.Gold + CivSettings.localize.localizedString("resident_perksGui_clickToActivate"), "Unlimted Uses");
					stack = LoreGuiItem.setAction(stack, "ActivatePerk");
					stack = LoreGuiItem.setActionData(stack, "perk", p.configPerk.id);

					inv.addItem(stack);

				}
		}
		player.openInventory(inv);
	}

	public static void showTemplatePerks(Resident resident, String name) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}

		Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9,
				CivSettings.localize.localizedString("resident_perksGui_templatesHeading") + " " + name);

		for (Object obj : resident.perks.values()) {
			Perk p = (Perk) obj;
			if (p.getConfigId().contains("tpl_" + name)) {
				ItemStack stack = LoreGuiItem.build(p.configPerk.display_name, p.configPerk.type_id, p.configPerk.data,
						CivColor.Gold + CivSettings.localize.localizedString("resident_perksGui_clickToActivate"), CivColor.LightBlue + "Count: " + p.count);
				stack = LoreGuiItem.setAction(stack, "ActivatePerk");
				stack = LoreGuiItem.setActionData(stack, "perk", p.configPerk.id);

				inv.addItem(stack);
			}
		}

		player.openInventory(inv);
	}

	public void templatePerksGui(Resident resident, String name) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		final Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54,
				CivSettings.localize.localizedString("resident_perksGui_templatesHeading") + " " + name);
		resident.perks.values().stream().filter(p -> p.getConfigId().contains("tpl_" + name)).map(p -> {
			ItemStack stack2 = LoreGuiItem.build(p.configPerk.display_name, p.configPerk.type_id, p.configPerk.data,
					"§6" + CivSettings.localize.localizedString("resident_perksGui_clickToActivate"), "§b\u041a\u043e\u043b-\u0432\u043e: " + p.count);
			ItemStack stack3 = LoreGuiItem.setAction(stack2, "ActivatePerk");
			ItemStack stack4 = LoreGuiItem.setActionData(stack3, "perk", p.configPerk.id);
			return stack4;
		}).forEachOrdered(stack -> inv.addItem(new ItemStack[]{stack}));
		ItemStack backButton = LoreGuiItem.build(CivSettings.localize.localizedString("bookReborn_back"), ItemManager.getMaterialId(Material.MAP), 0,
				CivSettings.localize.localizedString("bookReborn_backTo", BookShowPerkGui.inv.getName()));
		backButton = LoreGuiItem.setAction(backButton, "OpenInventory");
		backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
		backButton = LoreGuiItem.setActionData(backButton, "invName", BookShowPerkGui.inv.getName());
		inv.setItem(53, backButton);
		LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
		player.openInventory(inv);
	}
	
}
