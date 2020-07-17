package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.util.CivColor;

public class CraftingHelp extends GuiInventory {

	public CraftingHelp(Player player, String arg) {
		super(player, arg);
		this.setTitle(CivSettings.localize.localizedString("tutorial_customRecipesHeading"));

		/* Build the Category Inventory. */
		for (ConfigMaterialCategory cat : ConfigMaterialCategory.getCategories()) {
			if (cat.craftableCount == 0) continue;
			Material identifier = cat.title.contains("Fish") ? Material.RAW_FISH //
					: cat.title.contains("Gear") ? Material.IRON_SWORD //
							: cat.title.contains("Materials") ? Material.WOOD_STEP //
									: cat.title.contains("Tools") ? Material.IRON_SPADE : //
											Material.WRITTEN_BOOK;
			this.addGuiItem(GuiItems.newGuiItem()//
					.setTitle(cat.title)//
					.setMaterial(identifier)//
					.addLore(CivColor.LightBlue + cat.materials.size() + " " + CivSettings.localize.localizedString("tutorial_lore_items")) //
					.addLore(CivColor.Gold + CivSettings.localize.localizedString("tutorial_lore_clickToOpen"))//
					.setOpenInventory("CraftingHelpCategory", cat.id));
		}
		saveStaticGuiInventory();
	}

}
