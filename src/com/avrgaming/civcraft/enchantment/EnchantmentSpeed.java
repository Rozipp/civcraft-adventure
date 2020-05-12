package com.avrgaming.civcraft.enchantment;

import org.bukkit.ChatColor;

public class EnchantmentSpeed extends CustomEnchantment {

	private static Double percentPerLevel = 0.02;

	protected EnchantmentSpeed(int id) {
		super(id, "Speed_Bonus", ItemSet.LEGGINGS, 100, null);
	}

	@Override
	public String getDisplayName(int level) {
		return ChatColor.BLUE + "Speed " + " +" + percentPerLevel * level * 100 + "%";
	}
	
	public static Double getModifitedSpeed(int level) {
		return percentPerLevel * level;
	}

}
