
package com.avrgaming.civcraft.gui.action.book;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigSpaceRocket;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.ItemManager;

public class CivSpaceComponentsNormal implements GuiItemAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		try {
			GuiInventory guiInventory = new GuiInventory(player, null, null)//
					.setRow(1)//
					.setTitle(CivSettings.localize.localizedString("bookReborn_civSpaceComponentsHeading"));

			int i = Integer.valueOf(GuiItems.getActionData(stack, "i"));
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
			guiInventory.openInventory(player);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}
}
