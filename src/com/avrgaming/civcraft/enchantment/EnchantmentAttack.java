package com.avrgaming.civcraft.enchantment;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.config.CivSettings;

public class EnchantmentAttack extends CustomEnchantment {
	
	private static Double attakPerLevel = 1.0;
	
	public EnchantmentAttack(int i) {
		super(i, "attack", CivSettings.localize.localizedString("itemLore_Attack"), ItemSet.ALLWEAPONS, 100, null);
	}

	@Override
	public String getDisplayName(int level) {
		return ChatColor.YELLOW + this.displayName + " +" + level;
	}
	
	public static Double onAttack(int level) {
		return attakPerLevel *level;
	}
}
