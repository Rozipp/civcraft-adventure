package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItems;

public class CraftingHelpCategory extends GuiInventory {

	public CraftingHelpCategory(Player player, String categoryId) {
		super(player, categoryId);
		/* Build a new GUI Inventory. */
		ConfigMaterialCategory cat = ConfigMaterialCategory.getCategory(categoryId);
		this.setTitle(cat.title + " " + CivSettings.localize.localizedString("tutorial_lore_recipes"));
		for (ConfigMaterial mat : cat.materials.values()) {
			ItemStack stack = Recipe.getInfoBookForItem(mat.id);
			if (stack == null) continue;
			this.addGuiItem(GuiItems.newGuiItem(stack).setOpenInventory("Recipe", mat.id));
		}
//		saveStaticGuiInventory(buildId(getClass().getSimpleName(), getArg()));
	}

}
