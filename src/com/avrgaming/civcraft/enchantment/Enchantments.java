package com.avrgaming.civcraft.enchantment;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.util.ItemManager;

import gpl.AttributeUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** EnchantmentAPI © 2017 com.sucy.enchant.api.Enchantments */
public class Enchantments {
	public static Map<Integer, EnchantmentCustom> enchantmentList = new HashMap<>();

	public static void addEnchantment(ItemStack item, EnchantmentCustom enchantment, Integer level) {
		Objects.requireNonNull(item, "Item cannot be null");
		Validate.isTrue(level > 0, "Level must be at least 1");
		if (item.getType() == Material.BOOK) item.setType(Material.ENCHANTED_BOOK);

		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
		int lvl = Enchantments.getLevelEnchantment(item, enchantment);
		if (lvl > 0) lore.remove(enchantment.getDisplayName(lvl));
		lore.add(0, enchantment.getDisplayName(level));
		meta.setLore(lore);
		meta.addEnchant(enchantment.enchantment, level, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		item.setItemMeta(meta);
	}

	public static void addEnchantment(AttributeUtil attrUtil, EnchantmentCustom enchantment, Integer level) {
		Objects.requireNonNull(attrUtil, "Item cannot be null");
		Validate.isTrue(level > 0, "Level must be at least 1");

		final List<String> lore = attrUtil.getLore();
		if (lore == null) new ArrayList<>();
		int lvl = attrUtil.getEnchantLevel(enchantment.enchantment);
		if (lvl > 0) {
			lore.remove(enchantment.getDisplayName(lvl));
		}
		lore.add(0, enchantment.getDisplayName(level));
		attrUtil.setLore(lore);
		attrUtil.addEnchantment(enchantment.enchantment, level);
	}

	public static void removeEnchantment(ItemStack item, EnchantmentCustom enchantment) {
		Objects.requireNonNull(item, "Item cannot be null");
		final int lvl = Enchantments.getLevelEnchantment(item, enchantment);
		if (lvl > 0) {
			final ItemMeta meta = item.getItemMeta();
			final List<String> lore = meta.getLore();
			lore.remove(enchantment.getDisplayName(lvl));
			meta.setLore(lore);
			meta.removeEnchant(enchantment.enchantment);
			item.setItemMeta(meta);
		}
	}

	public static Integer getLevelEnchantment(final ItemStack item, EnchantmentCustom enchantment) {
		if (item == null) return 0;
		return item.getItemMeta().getEnchantLevel(enchantment.enchantment);
	}

	public static EnchantmentCustom getCustomEnchantment(int id) {
		return enchantmentList.get(id);
	}

	public static Map<EnchantmentCustom, Integer> getCustomEnchantments(final ItemStack item) {
		final HashMap<EnchantmentCustom, Integer> list = new HashMap<EnchantmentCustom, Integer>();
		if (!ItemManager.isPresent(item)) return list;

		final ItemMeta meta = item.getItemMeta();
		if (meta == null || !meta.hasLore()) return list;

		for (Enchantment ench : meta.getEnchants().keySet()) {
			@SuppressWarnings("deprecation")
			Integer id = ench.getId();
			if (enchantmentList.containsKey(id)) {
				EnchantmentCustom enchant = enchantmentList.get(id);
				if (enchant == null) continue;
				int level = meta.getEnchants().get(ench);
				if (level > 0) list.put(enchant, level);
			}
		}
		return list;
	}

	public static Map<EnchantmentCustom, Integer> getAllEnchantments(final ItemStack item) {
		final Map<EnchantmentCustom, Integer> result = getCustomEnchantments(item);
		return result;
	}

	public static boolean hasEnchantment(final ItemStack item, final EnchantmentCustom enchantment) {
		if (item == null || !item.hasItemMeta()) return false;
		return item.getItemMeta().hasEnchant(enchantment.enchantment);
	}

	public static ItemStack removeAllEnchantments(final ItemStack item) {
		item.getEnchantments().forEach((enchant, level) -> item.removeEnchantment(enchant));
		return item;
	}

}
