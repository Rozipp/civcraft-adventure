
package com.avrgaming.civcraft.loregui.book;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigSpaceRocket;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.loregui.space.BookCivSpaceEnded;
import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.lorestorage.GuiItemAction;
import com.avrgaming.civcraft.lorestorage.GuiItems;

import com.avrgaming.civcraft.util.ItemManager;

public class CivSpaceComponents implements GuiItemAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		GuiInventory guiInventory = new GuiInventory(player, 1, CivSettings.localize.localizedString("bookReborn_civSpaceComponentsHeading"));
		int i = Integer.valueOf(GuiItems.getActionData(stack, "i"));
		boolean fromEnded = Boolean.valueOf(GuiItems.getActionData(stack, "b"));
		ConfigSpaceRocket configSpaceRocket = CivSettings.spaceRocket_name.get(i);
		for (String craftMatID : configSpaceRocket.components.split(":")) {
			int count = Integer.parseInt(craftMatID.replaceAll("[^\\d]", ""));
			String craftMat = craftMatID.replace(String.valueOf(count), "");
			CraftableCustomMaterial itemToGetName = CraftableCustomMaterial.getCraftableCustomMaterial(craftMat);
			guiInventory.addGuiItem(GuiItems.newGuiItem(ItemManager.createItemStack(itemToGetName.getConfigMaterial().item_id, (short) itemToGetName.getConfigMaterial().item_data, 1))//
					.setTitle(itemToGetName.getName())//
					.setLore("§6" + CivSettings.localize.localizedString("bookReborn_civSpaceMenu"))//
					.setAmount(count));
		}
		String backTo = fromEnded ? CivSettings.localize.localizedString("bookReborn_civSpaceEndedHeading") : CivSettings.localize.localizedString("bookReborn_civSpaceFutureHeading");
		guiInventory.addBackItem(CivSettings.localize.localizedString("loreGui_recipes_back"),CivSettings.localize.localizedString("loregui_backto") + " " + backTo);
		guiInventory.openInventory(player);
	}
}
