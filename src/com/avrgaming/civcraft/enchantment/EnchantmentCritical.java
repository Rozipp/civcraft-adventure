package com.avrgaming.civcraft.enchantment;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivCraft;


public class EnchantmentCritical extends CustomEnchantment {

	static double procentPerLevel = 0.05; 
	
	public EnchantmentCritical(int i) {
		super(i, "Critical", "Криты", ItemSet.SWORDS, 100, null);
	}

	public static boolean randomCriticalAttack(ItemStack item) {
		int level = Enchantments.getLevelEnchantment(item, CustomEnchantment.Critical);
		double d = procentPerLevel * level;
		return CivCraft.civRandom.nextDouble() < d;
	}

}