package com.avrgaming.civcraft.units;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface CooldownFinisher {

	public void finishCooldown(Player player, ItemStack stack);
}
