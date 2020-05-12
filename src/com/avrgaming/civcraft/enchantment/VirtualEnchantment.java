package com.avrgaming.civcraft.enchantment;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.util.ItemManager;

public class VirtualEnchantment extends Enchantment {

	public VirtualEnchantment(int id) {
		super(id);
		fix(this);
	}

	static void fix(Enchantment ench) {
		try {
			if (!Arrays.asList(Enchantment.values()).contains(ench)) {
				Field f = Enchantment.class.getDeclaredField("acceptingNew");
				f.setAccessible(true);
				f.set(null, true);
				Enchantment.registerEnchantment(ench);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean canEnchantItem(ItemStack item) {
		if (!ItemManager.isPresent(item)) return false;
		Material material = item.getType();
		if (material == Material.BOOK || material == Material.ENCHANTED_BOOK) return true;

		@SuppressWarnings("deprecation")
		CustomEnchantment ce = Enchantments.enchantmentList.get(this.getId());
		return ce.naturalItems.contains(material);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean conflictsWith(Enchantment other) {
		Objects.requireNonNull(other, "Cannot check against a null item");
		
		CustomEnchantment cethis = Enchantments.enchantmentList.get(this.getId());
		if (other == this) return true;
		
		if (cethis.group.equals("Default")) return false;
		
		CustomEnchantment ceother = Enchantments.enchantmentList.get(other.getId());
		return cethis.group.equals(ceother.group);
	}

	@Override
	public EnchantmentTarget getItemTarget() {
		return EnchantmentTarget.ALL;
	}

	@Override
	public int getMaxLevel() {
		@SuppressWarnings("deprecation")
		CustomEnchantment ce = Enchantments.enchantmentList.get(this.getId());
		return ce.maxLevel;
	}

	@Override
	public String getName() {
		@SuppressWarnings("deprecation")
		CustomEnchantment ce = Enchantments.enchantmentList.get(this.getId());
		return ce.name;
	}

	@Override
	public int getStartLevel() {
		return 0;
	}

	@Override
	public boolean isCursed() {
		return false;
	}

	@Override
	public boolean isTreasure() {
		return false;
	}
	
}
