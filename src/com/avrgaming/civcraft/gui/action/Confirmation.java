
package com.avrgaming.civcraft.gui.action;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItem;
import com.avrgaming.civcraft.gui.GuiItemAction;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivMessage;

public class Confirmation implements GuiItemAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		try {
			GuiInventory inv = new GuiInventory(player, player, null).setRow(3)//
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
			inv.addLastItem(player.getUniqueId());
			inv.openInventory(player);
		} catch (CivException e) {
			CivMessage.send(player, e.getMessage());
		}
	}
}
