package com.avrgaming.civcraft.loreguiinventory;

import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigIngredient;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItem;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

import gpl.AttributeUtil;

public class Recipe extends GuiInventory {

	public static final int START_OFFSET = GuiItems.INV_ROW_COUNT + 3;

	public Recipe(Player player, String mid) {
		super(player, mid);
		CraftableCustomMaterial craftMat = CustomMaterial.getCraftableCustomMaterial(mid);
		if (craftMat == null || craftMat.getConfigMaterial().ingredients == null) return;

		this.setTitle(craftMat.getName() + " " + CivSettings.localize.localizedString("loreGui_recipes_guiHeading"));
		if (craftMat.isShaped()) {
			int offset = START_OFFSET;
			for (String line : craftMat.getConfigMaterial().shape) {
				for (int i = 0; i < line.toCharArray().length; i++) {
					ConfigIngredient ingred = null;
					for (ConfigIngredient in : craftMat.getConfigMaterial().ingredients.values()) {
						if (in.letter.equalsIgnoreCase(String.valueOf(line.toCharArray()[i]))) {
							ingred = in;
							break;
						}
					}
					if (ingred != null) this.addGuiItem(i + offset, getIngredItem(ingred));
				}
				offset += GuiItems.INV_ROW_COUNT;
			}
		} else {
			int x = 0;
			int offset = START_OFFSET;
			for (ConfigIngredient ingred : craftMat.getConfigMaterial().ingredients.values()) {
				if (ingred != null) {
					for (int i = 0; i < ingred.count; i++) {
						this.addGuiItem(x + offset, getIngredItem(ingred));
						x++;
						if (x >= 3) {
							x = 0;
							offset += GuiItems.INV_ROW_COUNT;
						}
					}
				}
			}
		}

		buildCraftTableBorder(this);
		buildInfoBar(craftMat, this, player);
		this.addLastItem();
		saveStaticGuiInventory();
	}

	public GuiItem getIngredItem(ConfigIngredient ingred) {
		String name;
		GuiItem entryStack;
		if (ingred.custom_id == null) {
			name = ItemManager.getMaterialData(ingred.type_id, ingred.data).toString();
			entryStack = GuiItems.newGuiItem(ItemManager.createItemStack(ingred.type_id, (short) ingred.data, 1))//
					.setTitle(name).setLore("Vanilla Item");
		} else {
			CraftableCustomMaterial cmat = CraftableCustomMaterial.getCraftableCustomMaterial(ingred.custom_id);
			name = cmat.getName();
			entryStack = GuiItems.newGuiItem(CraftableCustomMaterial.spawn(cmat));
			if (cmat.isCraftable()) {
				entryStack.addLore(CivSettings.localize.localizedString("loreGui_recipes_clickForRecipe"))//
						.setOpenInventory("Recipe", cmat.getId());
			} else
				entryStack.addLore(CivSettings.localize.localizedString("loreGui_recipes_notCraftable"));
		}
		return entryStack;
	}

	public void buildCraftTableBorder(GuiInventory recInv) {
		int offset = 2;
		GuiItem item = GuiItems.newGuiItem().setTitle("Craft Table Border").setMaterial(Material.WORKBENCH);
		for (int y = 0; y <= 4; y++) {
			for (int x = 0; x <= 4; x++) {
				if (x == 0 || x == 4 || y == 0 || y == 4) {
					recInv.addGuiItem(offset + (y * GuiItems.INV_ROW_COUNT) + x, item);
				}
			}
		}
	}

	public void buildInfoBar(CraftableCustomMaterial craftMat, GuiInventory recInv, Player player) {
		int offset = 0;
		GuiItem stack = null;

		if (craftMat.getConfigMaterial().required_tech != null) {
			Resident resident = CivGlobal.getResident(player);
			ConfigTech tech = CivSettings.techs.get(craftMat.getConfigMaterial().required_tech);
			if (tech != null) {
				stack = (resident.hasTown() && resident.getCiv().hasTechnologys(craftMat.getConfigMaterial().required_tech)) ? //
						GuiItems.newGuiItem().setTitle(CivSettings.localize.localizedString("loreGui_recipes_requiredTech")).setMaterial(Material.EMERALD_BLOCK).setLore(tech.name) : //
						GuiItems.newGuiItem().setTitle(CivSettings.localize.localizedString("loreGui_recipes_requiredTech")).setMaterial(Material.REDSTONE_BLOCK).setLore(tech.name);
				recInv.addGuiItem(offset + 0, stack);
			}
		}

		stack = (craftMat.isShaped()) ? //
				GuiItems.newGuiItem().setTitle(CivSettings.localize.localizedString("loreGui_recipes_shaped")).setMaterial(Material.HOPPER) : //
				GuiItems.newGuiItem().setTitle(CivSettings.localize.localizedString("loreGui_recipes_unshaped")).setMaterial(Material.COAL);
		offset += GuiItems.INV_ROW_COUNT;
		recInv.addGuiItem(offset + 0, stack);
	}

	public static ItemStack getInfoBookForItem(String matID) {
		CraftableCustomMaterial loreMat = CraftableCustomMaterial.getCraftableCustomMaterial(matID);
		ItemStack stack = CustomMaterial.spawn(loreMat);

		if (!loreMat.isCraftable()) return null;

		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.removeAll(); /* Remove all attribute modifiers to prevent them from displaying */
		LinkedList<String> lore = new LinkedList<String>();

		lore.add("" + ChatColor.RESET + ChatColor.BOLD + ChatColor.GOLD + CivSettings.localize.localizedString("tutorial_clickForRecipe"));

		attrs.setLore(lore);
		stack = attrs.getStack();
		return stack;
	}

}
