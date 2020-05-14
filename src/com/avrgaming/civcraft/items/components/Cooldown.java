package com.avrgaming.civcraft.items.components;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class Cooldown extends ItemComponent {

	com.avrgaming.civcraft.units.Cooldown cooldown = null;

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.GOLD + "Cooldown " + getString("cooldown"));
		attrUtil.addLore(ChatColor.RESET + CivColor.Rose + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
	}
	@Override
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack stack = event.getItem();
		if (cooldown == null) cooldown = new com.avrgaming.civcraft.units.Cooldown(stack, this, getInteger("cooldown"));
		if (!cooldown.isCanUse()) {
			CivMessage.sendError(player, "Вы не моежетет использовать это");
			event.setCancelled(true);
			return;
		}

		if (!cooldown.isRefresh()) {
			CivMessage.sendError(player, "Вы не моежетет использовать это сейчас. Подождите немного");
			event.setCancelled(true);
			return;
		}

		setAttribute("lock", "lock");
		cooldown.beginCooldown(player);
	}

}
