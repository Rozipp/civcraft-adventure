package com.avrgaming.civcraft.enchantment;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.config.CivSettings;

public class EnchantmentDefense extends CustomEnchantment {

	public static Double defensePerLevel = 1.0;

	public EnchantmentDefense(int i) {
		super(i, "Defense", ItemSet.ARMOR, 100, null);
	}

	public String getDisplayName(int level) {
		return ChatColor.RED + CivSettings.localize.localizedString("newItemLore_Defense") + " +" + defensePerLevel * level;
	}

}
