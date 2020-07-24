package com.avrgaming.civcraft.enchantment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import com.avrgaming.civcraft.util.RomanNumerals;

import java.util.Arrays;

/** EnchantmentAPI © 2017 com.sucy.enchant.vanilla.VanillaEnchantment */
public class EnchantmentVanilla extends EnchantmentCustom {

	public EnchantmentVanilla(Enchantment ench, String displayName, ItemSet itemSet, int maxLevel) {
		this(ench, displayName, itemSet, maxLevel, "Default");
	}

	public EnchantmentVanilla(Enchantment ench, String displayName, ItemSet itemSet, int maxLevel, String group) {
		super(ench);
		this.displayName = displayName;
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
		return ChatColor.GRAY + displayName + (enchantment.getMaxLevel() > 1 ? " " + RomanNumerals.toNumerals(level) : "");
	}
}
