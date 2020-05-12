package com.avrgaming.civcraft.enchantment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.CivColor;

public class CustomEnchantment {

	// ---- Armor ----
	public static CustomEnchantment BINDING_CURSE = new EnchantmentVanilla(Enchantment.BINDING_CURSE, ItemSet.ALL, 1);
	public static CustomEnchantment DEPTH_STRIDER = new EnchantmentVanilla(Enchantment.DEPTH_STRIDER, ItemSet.BOOTS,  3, "stride");
	public static CustomEnchantment FROST_WALKER = new EnchantmentVanilla(Enchantment.FROST_WALKER, ItemSet.BOOTS, 2, "stride");
	public static CustomEnchantment OXYGEN = new EnchantmentVanilla(Enchantment.OXYGEN, ItemSet.HELMETS, 3);
	public static CustomEnchantment PROTECTION_ENVIRONMENTAL = new EnchantmentVanilla(Enchantment.PROTECTION_ENVIRONMENTAL, ItemSet.ARMOR, 4, "protection");
	public static CustomEnchantment PROTECTION_EXPLOSIONS = new EnchantmentVanilla(Enchantment.PROTECTION_EXPLOSIONS, ItemSet.ARMOR, 4, "protection");
	public static CustomEnchantment PROTECTION_FALL = new EnchantmentVanilla(Enchantment.PROTECTION_FALL, ItemSet.BOOTS, 4, "protection");
	public static CustomEnchantment PROTECTION_FIRE = new EnchantmentVanilla(Enchantment.PROTECTION_FIRE, ItemSet.ARMOR, 4, "protection");
	public static CustomEnchantment PROTECTION_PROJECTILE = new EnchantmentVanilla(Enchantment.PROTECTION_PROJECTILE, ItemSet.ARMOR, 4, "protection");
	public static CustomEnchantment THORNS = new EnchantmentVanilla(Enchantment.THORNS, ItemSet.CHESTPLATES, 3);
	public static CustomEnchantment WATER_WORKER = new EnchantmentVanilla(Enchantment.WATER_WORKER, ItemSet.HELMETS, 1);
	// ---- Swords ----
	public static CustomEnchantment DAMAGE_ALL = new EnchantmentVanilla(Enchantment.DAMAGE_ALL, ItemSet.WEAPONS, 5, "damage");
	public static CustomEnchantment DAMAGE_ARTHROPODS = new EnchantmentVanilla(Enchantment.DAMAGE_ARTHROPODS, ItemSet.WEAPONS, 5, "damage");
	public static CustomEnchantment DAMAGE_UNDEAD = new EnchantmentVanilla(Enchantment.DAMAGE_UNDEAD, ItemSet.WEAPONS, 5, "damage");
	public static CustomEnchantment FIRE_ASPECT = new EnchantmentVanilla(Enchantment.FIRE_ASPECT, ItemSet.SWORDS, 2);
	public static CustomEnchantment KNOCKBACK = new EnchantmentVanilla(Enchantment.KNOCKBACK, ItemSet.SWORDS, 2);
	public static CustomEnchantment LOOT_BONUS_MOBS = new EnchantmentVanilla(Enchantment.LOOT_BONUS_MOBS, ItemSet.SWORDS, 3);
	public static CustomEnchantment SWEEPING_EDGE = new EnchantmentVanilla(Enchantment.SWEEPING_EDGE, ItemSet.SWORDS, 3);
	// --- Tools ---
	public static CustomEnchantment DIG_SPEED = new EnchantmentVanilla(Enchantment.DIG_SPEED, ItemSet.TOOLS, 5);
	public static CustomEnchantment LOOT_BONUS_BLOCKS = new EnchantmentVanilla(Enchantment.LOOT_BONUS_BLOCKS, ItemSet.TOOLS,3,"blocks");
	public static CustomEnchantment SILK_TOUCH = new EnchantmentVanilla(Enchantment.SILK_TOUCH, ItemSet.TOOLS, 1,"blocks");
	// ---- Bows ----
	public static CustomEnchantment ARROW_DAMAGE = new EnchantmentVanilla(Enchantment.ARROW_DAMAGE, ItemSet.BOWS, 5);
	public static CustomEnchantment ARROW_FIRE = new EnchantmentVanilla(Enchantment.ARROW_FIRE, ItemSet.BOWS, 1);
	public static CustomEnchantment ARROW_INFINITE = new EnchantmentVanilla(Enchantment.ARROW_INFINITE, ItemSet.BOWS, 1,"infinite");
	public static CustomEnchantment ARROW_KNOCKBACK = new EnchantmentVanilla(Enchantment.ARROW_KNOCKBACK, ItemSet.BOWS, 2);
	// ---- Fishing ----
	public static CustomEnchantment LUCK = new EnchantmentVanilla(Enchantment.LUCK, ItemSet.FISHING, 3);
	public static CustomEnchantment LURE = new EnchantmentVanilla(Enchantment.LURE, ItemSet.FISHING, 3);
	// ---- Trident ---- 1.13
	// public static CustomEnchantment LOYALTY = new VanillaEnchantment(Enchantment.LOYALTY, ItemSet.TRIDENT, 3, 5, 5, 7, 43, 2, true);
	// public static CustomEnchantment IMPALING = new VanillaEnchantment(Enchantment.IMPALING, TRIDENT, 5, 2, 1, 8, 12, 4, true);
	// public static CustomEnchantment RIPTIDE = new VanillaEnchantment(Enchantment.RIPTIDE, TRIDENT, 3, 2, 10, 7, 43, 4, true);
	// public static CustomEnchantment CHANNELING = new VanillaEnchantment(Enchantment.CHANNELING, TRIDENT, 1, 1, 25, 0, 50, 8, true);
	// ---- All ----
	public static CustomEnchantment DURABILITY = new EnchantmentVanilla(Enchantment.DURABILITY, ItemSet.DURABILITY, 3);
	public static CustomEnchantment MENDING = new EnchantmentVanilla(Enchantment.MENDING, ItemSet.DURABILITY_ALL, 1, "infinite");
	public static CustomEnchantment VANISHING_CURSE = new EnchantmentVanilla(Enchantment.VANISHING_CURSE, ItemSet.ALL, 1);

	public static CustomEnchantment Attack = new CustomEnchantment(101, "Attack", ItemSet.ALLWEAPONS, 100, null);
	public static CustomEnchantment BuyItem = new CustomEnchantment(102, "Buy Item", ItemSet.NONE, 1, null);
	public static CustomEnchantment Critical = new EnchantmentCritical(103);
	public static CustomEnchantment Defense = new EnchantmentDefense(104);
	public static CustomEnchantment Evrei = new CustomEnchantment(105, CivColor.LightPurpleBold + "evrei", ItemSet.NONE, 1, null);
	public static CustomEnchantment Jumping = new CustomEnchantment(105, "Jumping", ItemSet.LEGGINGS, 100, null);
	public static CustomEnchantment Levitate = new CustomEnchantment(106, CivColor.LightGrayBold + "levitate", ItemSet.BOOTS, 1, null);
	public static CustomEnchantment LightningStrike = new CustomEnchantment(107, "LightStrike", ItemSet.WEAPONS, 1, null);
	public static CustomEnchantment NoRepair = new CustomEnchantment(108, CivColor.GrayBold + "noRepair", ItemSet.NONE, 1, null);
	public static CustomEnchantment NoTech = new CustomEnchantment(109, CivColor.RoseBold + "noTech", ItemSet.NONE, 1, null);
	public static CustomEnchantment Poison = new CustomEnchantment(110, CivColor.LightGreenBold + "poision", ItemSet.WEAPONS, 1, null);
	public static CustomEnchantment Punchout = new EnchantmentPunchout(111);
	public static CustomEnchantment SoulBound = new CustomEnchantment(112, CivColor.GoldBold + "Soulbound", ItemSet.NONE, 1, null);
	public static CustomEnchantment Speed = new EnchantmentSpeed(113);
	public static CustomEnchantment TechOnly = new CustomEnchantment(114, CivColor.LightGrayBold + "techOnly", ItemSet.NONE, 1, null);
	public static CustomEnchantment Thorns = new CustomEnchantment(115, CivColor.Blue + "Отдача", ItemSet.CHESTPLATES, 5, null);
	public static CustomEnchantment UnitItem = new CustomEnchantment(116, CivColor.GoldBold + "Предмет юнита", ItemSet.NONE, 1, null);

	protected Enchantment enchantment;

	public final Set<Material> naturalItems = new HashSet<>();

	public int id;
	public String name;
	public String group;
	public int maxLevel;

	@SuppressWarnings("deprecation")
	protected CustomEnchantment(Enchantment enchant) {
		this.id = enchant.getId();
		this.name = enchant.getName().trim();
		CivLog.debug("register new vanila enchant  " + name);
		if (!Enchantments.enchantmentList.containsKey(id)) Enchantments.enchantmentList.put(id, this);
	}

	protected CustomEnchantment(int id, String name, ItemSet itemSet, int maxLevel, String group) {
		Validate.notEmpty(name, "The name must be present and not empty");
		CivLog.debug("register new Custom enchant  " + name);

		this.id = id;
		this.addNaturalItems(itemSet);
		this.name = name.trim();
		this.group = (group == null) ? "Default" : group;
		this.maxLevel = maxLevel;

		if (!Enchantments.enchantmentList.containsKey(id)) Enchantments.enchantmentList.put(id, this);
		enchantment = new VirtualEnchantment(id);
	}

	protected CustomEnchantment(final String name, ItemSet itemSet, int maxLevel, String group) {
		this(getNextFreeId(), name, itemSet, maxLevel, group);
	}

	public void addNaturalItems(final Material... materials) {
		for (Material material : materials) {
			Objects.requireNonNull(material, "Cannot add a null natural material");
			naturalItems.add(material);
		}
	}

	public void addNaturalItems(final ItemSet... items) {
		for (ItemSet item : items) {
			for (Material material : item.getItems()) {
				Objects.requireNonNull(material, "Cannot add a null natural material");
				naturalItems.add(material);
			}
		}
	}

	/** @param item item to add to
	 * @param level enchantment level
	 * @return item with the enchantment */
	public ItemStack addToItem(final ItemStack item, final int level) {
		Objects.requireNonNull(item, "Item cannot be null");
		Validate.isTrue(level > 0, "Level must be at least 1");
		if (item.getType() == Material.BOOK) {
			item.setType(Material.ENCHANTED_BOOK);
		}
		final ItemMeta meta = item.getItemMeta();
		final List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
		final int lvl = Enchantments.getCustomEnchantments(item).getOrDefault(this, 0);
		if (lvl > 0) {
			lore.remove(this.getDisplayName(lvl));
		}
		lore.add(0, this.getDisplayName(level));
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	/** @param item item to remove from
	 * @return item without this enchantment */
	public ItemStack removeFromItem(final ItemStack item) {
		Objects.requireNonNull(item, "Item cannot be null");
		final int lvl = Enchantments.getCustomEnchantments(item).getOrDefault(this, 0);
		if (lvl > 0) {
			final ItemMeta meta = item.getItemMeta();
			final List<String> lore = meta.getLore();
			lore.remove(this.getDisplayName(lvl));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
		return item;
	}

	private static int getNextFreeId() {
		int i = 100;
		while (!Enchantments.enchantmentList.containsKey(i)) {
			i++;
		}
		return i;
	}

	public String getName() {
		return name;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public static CustomEnchantment getByName(String enchant_id) {
		for (CustomEnchantment ce : Enchantments.enchantmentList.values())
			if (ce.getName().equalsIgnoreCase(enchant_id)) return ce;
		// TODO Перепроверить совпадение enchant_id с именами CustomEnchantment
		return null;
	}

	public boolean canEnchantItem(ItemStack item) {
		return enchantment.canEnchantItem(item);
	}

	public String getDisplayName(int level) {
		if (enchantment.getName().startsWith("§")) return enchantment.getName().toLowerCase().trim() + (enchantment.getMaxLevel() > 1 ? " " + level : ""); 
		return ChatColor.GRAY + enchantment.getName().toLowerCase().trim() + (enchantment.getMaxLevel() > 1 ? " " + level : "");
	}

}
