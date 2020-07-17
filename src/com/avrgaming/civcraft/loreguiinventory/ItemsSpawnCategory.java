package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItems;

public class ItemsSpawnCategory extends GuiInventory {

	public ItemsSpawnCategory(Player player, String catId) {
		super(player, catId);
		/* Build a new GUI Inventory. */
		ConfigMaterialCategory cat = ConfigMaterialCategory.getCategory(catId);
		this.setTitle(cat.title + " Spawn");//
		for (ConfigMaterial mat : cat.materials.values()) {
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(mat.id);
			if (craftMat == null) continue;
			this.addGuiItem(GuiItems.newGuiItem(CustomMaterial.spawn(craftMat)).setAction("SpawnItem"));
		}
		saveStaticGuiInventory();
	}

}
