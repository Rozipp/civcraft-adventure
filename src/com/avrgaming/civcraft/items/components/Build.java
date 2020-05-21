package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Cannon;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class Build extends ItemComponent {

	@Override
	public void onInteract(PlayerInteractEvent event) {
		String building = this.getString("building");
		Player player = event.getPlayer();
		try {
			switch (building) {
			case "Cannon":
				Cannon.newCannon(player);
				break;
			default:
				break;
			}
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
			return;
		}
		player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
	}

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET + CivColor.Gold + CivSettings.localize.localizedString("buildCannon_Lore1"));
		attrUtil.addLore(ChatColor.RESET + CivColor.Rose + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
	}

}
