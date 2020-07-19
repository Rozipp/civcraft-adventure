package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.util.CivColor;

public class ItemsSpawn extends GuiInventory {

	public ItemsSpawn(Player player, String arg) {
		super(player, arg);
		if (arg == null)
			createPerent();
		else
			createCategory();
	}

	private void createPerent() {
		this.setTitle(CivSettings.localize.localizedString("adcmd_itemsHeader"));

		/* Build the Category Inventory. */
		for (ConfigMaterialCategory cat : ConfigMaterialCategory.getCategories()) {
			Material identifier = cat.title.contains("Fish") ? Material.RAW_FISH //
					: cat.title.contains("Gear") ? Material.IRON_SWORD //
							: cat.title.contains("Materials") ? Material.WOOD_STEP //
									: cat.title.contains("Tools") ? Material.IRON_SPADE : //
											Material.WRITTEN_BOOK;
			this.addGuiItem(0, GuiItems.newGuiItem()//
					.setMaterial(identifier)//
					.setTitle(cat.title)//
					.setLore(CivColor.LightBlue + cat.materials.size() + " Items", CivColor.Gold + "<Click To Open>")//
					.setOpenInventory("ItemsSpawn", cat.id));
		}
		saveStaticGuiInventory();
	}

	private void createCategory() {
		/* Build a new GUI Inventory. */
		ConfigMaterialCategory cat = ConfigMaterialCategory.getCategory(getArg());
		this.setTitle("Spawn items from category " + cat.title);//
		for (ConfigMaterial mat : cat.materials.values()) {
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(mat.id);
			if (craftMat == null) continue;
			this.addGuiItem(GuiItems.newGuiItem(CustomMaterial.spawn(craftMat)).setAction("SpawnItem"));
		}
		saveStaticGuiInventory();
	}
}
