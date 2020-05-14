package com.avrgaming.civcraft.enchantment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Arrays;

/** EnchantmentAPI © 2017 com.sucy.enchant.vanilla.VanillaEnchantment */
public class EnchantmentVanilla extends CustomEnchantment {

	public EnchantmentVanilla(Enchantment ench, ItemSet itemSet, int maxLevel) {
		this(ench, itemSet, maxLevel, "Default");
	}

	public EnchantmentVanilla(Enchantment ench, ItemSet itemSet, int maxLevel, String group) {
		super(ench);
		enchantment = ench;
		this.naturalItems.addAll(Arrays.asList(itemSet.getItems()));
		this.maxLevel = maxLevel;
		this.group = group;
	}

	@Override
	public ItemStack addToItem(final ItemStack item, final int level) {
		if (item.getType() == Material.BOOK) {
			item.setType(Material.ENCHANTED_BOOK);
		}
		if (item.getType() == Material.ENCHANTED_BOOK) {
			final EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
			meta.addStoredEnchant(this.enchantment, level, true);
			item.setItemMeta(meta);
			return item;
		}
		item.addUnsafeEnchantment(this.enchantment, level);
		return item;
	}

	@Override
	public ItemStack removeFromItem(final ItemStack item) {
		if (item.getType() == Material.ENCHANTED_BOOK) {
			final EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
			meta.removeStoredEnchant(this.enchantment);
			item.setItemMeta(meta);
		}
		item.removeEnchantment(this.enchantment);
		return item;
	}

	@Override
	public String getDisplayName(int level) {
		return ChatColor.GRAY + enchantment.getName().toLowerCase().trim() + (enchantment.getMaxLevel() > 1 ? " " + RomanNumerals.toNumerals(level) : "");
	}
}