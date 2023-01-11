package com.avrgaming.civcraft.items.components;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

import com.avrgaming.gpl.AttributeUtil;

public class Cooldown extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.GOLD + "Cooldown " + getString("cooldown"));
		attrUtil.addLore(ChatColor.RESET + CivColor.Rose + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack stack = event.getItem();
		com.avrgaming.civcraft.units.Cooldown cooldown = com.avrgaming.civcraft.units.Cooldown.getCooldown(stack);
		if (cooldown != null) {
			CivMessage.sendError(player, "Подождите " + cooldown.getTime() + " секунд");
			event.setCancelled(true);
			return;
		}
		com.avrgaming.civcraft.units.Cooldown.startCooldown(player, stack, getInteger("cooldown"));
	}

}
