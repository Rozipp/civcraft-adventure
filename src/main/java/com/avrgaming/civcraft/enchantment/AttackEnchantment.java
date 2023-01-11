package com.avrgaming.civcraft.enchantment;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.config.CivSettings;

public class AttackEnchantment extends EnchantmentCustom {
	
	private static Double attakPerLevel = 1.0;
	
	public AttackEnchantment(int i) {
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
