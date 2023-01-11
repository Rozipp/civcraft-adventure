package com.avrgaming.civcraft.enchantment;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.config.CivSettings;

public class SpeedEnchantment extends EnchantmentCustom {

	private static Double percentPerLevel = 0.02;

	protected SpeedEnchantment(int id) {
		super(id, "speed", CivSettings.localize.localizedString("itemLore_Speed_Bonus"), ItemSet.LEGGINGS, 100, null);
	}

	@Override
	public String getDisplayName(int level) {
		return ChatColor.BLUE + this.displayName + " +" + percentPerLevel * level * 100 + "%";
	}

	public static Double getModifitedSpeed(int level) {
		return percentPerLevel * level;
	}

}
