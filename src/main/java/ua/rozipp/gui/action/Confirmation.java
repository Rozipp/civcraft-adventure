
package ua.rozipp.gui.action;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import ua.rozipp.gui.GuiAction;
import com.avrgaming.civcraft.main.CivMessage;

public class Confirmation extends GuiAction {
	@Override
	public void performAction(Player player, ItemStack stack) {
		try {
			GuiInventory inv = new GuiInventory(player, player, null).setRow(3)//
					.setTitle("§a" + CivSettings.localize.localizedString("resident_tradeNotconfirmed"));
			String fields = GuiItem.getActionData(stack, "passFields");
			String action = GuiItem.getActionData(stack, "passAction");
			String confirmText = GuiItem.getActionData(stack, "confirmText");
			GuiItem confirm = GuiItem.newGuiItem()//
					.setTitle("§a" + confirmText)//
					.setMaterial(Material.EMERALD_BLOCK)//
					.setAction(action);
			for (String field : fields.split(",")) {
				confirm.setActionData(field, GuiItem.getActionData(stack, field));
			}
			inv.addGuiItem(11, confirm);
			inv.addLastItem(player.getUniqueId());
			inv.openInventory(player);
		} catch (CivException e) {
			CivMessage.send(player, e.getMessage());
		}
	}
}
