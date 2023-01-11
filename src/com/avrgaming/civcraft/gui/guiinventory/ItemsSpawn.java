package com.avrgaming.civcraft.gui.guiinventory;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.threading.tasks.DelayMoveInventoryItem;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemsSpawn extends GuiInventory {

	public ItemsSpawn(Player player, String arg) throws CivException {
		super(player, null, arg);
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
	}

	private void createCategory() {
		/* Build a new GUI Inventory. */
		ConfigMaterialCategory cat = ConfigMaterialCategory.getCategory(getArg());
		this.setTitle("Spawn items from category " + cat.title);//
		for (ConfigMaterial mat : cat.materials.values()) {
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(mat.id);
			if (craftMat == null) continue;
			this.addGuiItem(GuiItems.newGuiItem(CustomMaterial.spawn(craftMat,64)).setAction("SpawnItem"));
		}
	}

	@Override
	public boolean onItemToInventory(Cancellable event, Player player, Inventory inv, ItemStack stack) {
		stack.setAmount(0);
		player.updateInventory();
		return false;
	}

	@Override
	public boolean onItemFromInventory(Cancellable event, Player player, Inventory inv, ItemStack stack) {
		if (!GuiItems.isGUIItem(stack)) {
			DelayMoveInventoryItem.beginTaskRespawn(event, player, inv, stack, inv.first(stack));
			return false;
		} else {
			String action = GuiItems.getAction(stack);
			if (action != null) {
				if (action.equals("SpawnItem")){
					DelayMoveInventoryItem.beginTaskRespawn(event, player, inv, stack, inv.first(stack));
					CustomMaterial cMat = CustomMaterial.getCustomMaterial(stack);
					stack.setItemMeta(CustomMaterial.spawn(cMat).getItemMeta());
					return false;
				}
				GuiItems.processAction(action, stack, player);
				return true;
			}
		}
		return true;
	}
}
