
package com.avrgaming.civcraft.gui.action;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItem;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;

public class Confirmation implements GuiItemAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			GuiInventory inv = new GuiInventory(player, null).setRow(3)//
					.setTitle("§a" + CivSettings.localize.localizedString("resident_tradeNotconfirmed"));
			String fields = GuiItems.getActionData(stack, "passFields");
			String action = GuiItems.getActionData(stack, "passAction");
			String confirmText = GuiItems.getActionData(stack, "confirmText");
			GuiItem confirm = GuiItems.newGuiItem()//
					.setTitle("§a" + confirmText)//
					.setMaterial(Material.EMERALD_BLOCK)//
					.setAction(action);
			for (String field : fields.split(",")) {
				confirm.setActionData(field, GuiItems.getActionData(stack, field));
			}
			inv.addGuiItem(11, confirm);
			inv.addLastItem();
			inv.openInventory();
		}
	}
}
