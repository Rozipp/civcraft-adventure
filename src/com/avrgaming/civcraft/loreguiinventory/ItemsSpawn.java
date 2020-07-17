package com.avrgaming.civcraft.loreguiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItems;
import com.avrgaming.civcraft.util.CivColor;

public class ItemsSpawn extends GuiInventory {

	public ItemsSpawn(Player player, String arg) {
		super(player, arg);
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
					.setOpenInventory("ItemsSpawnCategory", cat.id));
		}
		saveStaticGuiInventory();
	}

}
