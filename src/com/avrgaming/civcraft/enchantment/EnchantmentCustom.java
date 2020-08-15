package com.avrgaming.civcraft.enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.util.CivColor;

public class EnchantmentCustom {
	
	public static Map<String, EnchantmentCustom> allEnchantmentCustom = new HashMap<>();  
	
	// ---- Armor ----
	public static EnchantmentCustom BINDING_CURSE = new EnchantmentVanilla(Enchantment.BINDING_CURSE, "Проклятие несъёмности", ItemSet.ALL, 1);
	public static EnchantmentCustom DEPTH_STRIDER = new EnchantmentVanilla(Enchantment.DEPTH_STRIDER, "Подводная ходьба", ItemSet.BOOTS, 3, "stride");
	public static EnchantmentCustom FROST_WALKER = new EnchantmentVanilla(Enchantment.FROST_WALKER, "Ходьба по воде", ItemSet.BOOTS, 2, "stride");
	public static EnchantmentCustom OXYGEN = new EnchantmentVanilla(Enchantment.OXYGEN, "Подводное дыхание", ItemSet.HELMETS, 3);
	public static EnchantmentCustom PROTECTION_ENVIRONMENTAL = new EnchantmentVanilla(Enchantment.PROTECTION_ENVIRONMENTAL, "Защита", ItemSet.ARMOR, 4, "protection");
	public static EnchantmentCustom PROTECTION_EXPLOSIONS = new EnchantmentVanilla(Enchantment.PROTECTION_EXPLOSIONS, "Взрывоустойчивость", ItemSet.ARMOR, 4, "protection");
	public static EnchantmentCustom PROTECTION_FALL = new EnchantmentVanilla(Enchantment.PROTECTION_FALL, "Невесомость", ItemSet.BOOTS, 4, "protection");
	public static EnchantmentCustom PROTECTION_FIRE = new EnchantmentVanilla(Enchantment.PROTECTION_FIRE, "Огнеупорность", ItemSet.ARMOR, 4, "protection");
	public static EnchantmentCustom PROTECTION_PROJECTILE = new EnchantmentVanilla(Enchantment.PROTECTION_PROJECTILE, "Защита от снарядов", ItemSet.ARMOR, 4, "protection");
	public static EnchantmentCustom THORNS = new EnchantmentVanilla(Enchantment.THORNS, "Шипы", ItemSet.CHESTPLATES, 3);
	public static EnchantmentCustom WATER_WORKER = new EnchantmentVanilla(Enchantment.WATER_WORKER, "Подводник", ItemSet.HELMETS, 1);
	// ---- Swords ----
	public static EnchantmentCustom DAMAGE_ALL = new EnchantmentVanilla(Enchantment.DAMAGE_ALL, "Острота", ItemSet.WEAPONS, 5, "damage");
	public static EnchantmentCustom DAMAGE_ARTHROPODS = new EnchantmentVanilla(Enchantment.DAMAGE_ARTHROPODS, "Гибель насекомых", ItemSet.WEAPONS, 5, "damage");
	public static EnchantmentCustom DAMAGE_UNDEAD = new EnchantmentVanilla(Enchantment.DAMAGE_UNDEAD, "Небесная кара", ItemSet.WEAPONS, 5, "damage");
	public static EnchantmentCustom FIRE_ASPECT = new EnchantmentVanilla(Enchantment.FIRE_ASPECT, "Заговор огня", ItemSet.SWORDS, 2);
	public static EnchantmentCustom KNOCKBACK = new EnchantmentVanilla(Enchantment.KNOCKBACK, "Отдача", ItemSet.SWORDS, 2);
	public static EnchantmentCustom LOOT_BONUS_MOBS = new EnchantmentVanilla(Enchantment.LOOT_BONUS_MOBS, "Добыча", ItemSet.SWORDS, 3);
	public static EnchantmentCustom SWEEPING_EDGE = new EnchantmentVanilla(Enchantment.SWEEPING_EDGE, "Разящий клинок", ItemSet.SWORDS, 3);
	// --- Tools ---
	public static EnchantmentCustom DIG_SPEED = new EnchantmentVanilla(Enchantment.DIG_SPEED, "Эффективность", ItemSet.TOOLS, 5);
	public static EnchantmentCustom LOOT_BONUS_BLOCKS = new EnchantmentVanilla(Enchantment.LOOT_BONUS_BLOCKS, "Удача", ItemSet.TOOLS, 3, "blocks");
	public static EnchantmentCustom SILK_TOUCH = new EnchantmentVanilla(Enchantment.SILK_TOUCH, "Шёлковое касание", ItemSet.TOOLS, 1, "blocks");
	// ---- Bows ----
	public static EnchantmentCustom ARROW_DAMAGE = new EnchantmentVanilla(Enchantment.ARROW_DAMAGE, "Сила", ItemSet.BOWS, 5);
	public static EnchantmentCustom ARROW_FIRE = new EnchantmentVanilla(Enchantment.ARROW_FIRE, "Горящая стрела", ItemSet.BOWS, 1);
	public static EnchantmentCustom ARROW_INFINITE = new EnchantmentVanilla(Enchantment.ARROW_INFINITE, "Бесконечность", ItemSet.BOWS, 1, "infinite");
	public static EnchantmentCustom ARROW_KNOCKBACK = new EnchantmentVanilla(Enchantment.ARROW_KNOCKBACK, "Отбрасывание", ItemSet.BOWS, 2);
	// ---- Fishing ----
	public static EnchantmentCustom LUCK = new EnchantmentVanilla(Enchantment.LUCK, "Везучий рыбак", ItemSet.FISHING, 3);
	public static EnchantmentCustom LURE = new EnchantmentVanilla(Enchantment.LURE, "Приманка", ItemSet.FISHING, 3);
	// ---- Trident ---- 1.13
	// public static CustomEnchantment LOYALTY = new VanillaEnchantment(Enchantment.LOYALTY, ItemSet.TRIDENT, 3, 5, 5, 7, 43, 2, true);
	// public static CustomEnchantment IMPALING = new VanillaEnchantment(Enchantment.IMPALING, TRIDENT, 5, 2, 1, 8, 12, 4, true);
	// public static CustomEnchantment RIPTIDE = new VanillaEnchantment(Enchantment.RIPTIDE, TRIDENT, 3, 2, 10, 7, 43, 4, true);
	// public static CustomEnchantment CHANNELING = new VanillaEnchantment(Enchantment.CHANNELING, TRIDENT, 1, 1, 25, 0, 50, 8, true);
	// ---- All ----
	public static EnchantmentCustom DURABILITY = new EnchantmentVanilla(Enchantment.DURABILITY, "Нерушимость", ItemSet.DURABILITY, 3);
	public static EnchantmentCustom MENDING = new EnchantmentVanilla(Enchantment.MENDING, "Починка", ItemSet.DURABILITY_ALL, 1, "infinite");
	public static EnchantmentCustom VANISHING_CURSE = new EnchantmentVanilla(Enchantment.VANISHING_CURSE, "Проклятье утраты", ItemSet.ALL, 1);

	public static EnchantmentCustom Attack = new AttackEnchantment(101);
	public static EnchantmentCustom BuyItem = new EnchantmentCustom(102, "buy_item", CivSettings.localize.localizedString("itemLore_Buy"), ItemSet.NONE, 1, null);
	public static EnchantmentCustom Critical = new CriticalEnchantment(103);
	public static EnchantmentCustom Defense = new DefenseEnchantment(104);
	public static EnchantmentCustom Evrei = new EnchantmentCustom(105, "evrei", CivSettings.localize.localizedString("itemLore_evrei"), ItemSet.NONE, 1, null);
	public static EnchantmentCustom Jumping = new EnchantmentCustom(105, "jumping", "Прыгучесть", ItemSet.LEGGINGS, 100, null);
	public static EnchantmentCustom Levitate = new LevitateEnchantment(106);
	public static EnchantmentCustom LightningStrike = new LightningStrikeEnchantment(107);
	public static EnchantmentCustom NoRepair = new EnchantmentCustom(108, "norepair", CivSettings.localize.localizedString("itemLore_noRepair"), ItemSet.NONE, 1, null);
	public static EnchantmentCustom NoTech = new EnchantmentCustom(109, "notech", CivSettings.localize.localizedString("itemLore_noTech"), ItemSet.NONE, 1, null);
	public static EnchantmentCustom Poison = new PoisonEnchantment(110);
	public static EnchantmentCustom Punchout = new PunchoutEnchantment(111);
	public static EnchantmentCustom SoulBound = new EnchantmentCustom(112, "soulbound", CivSettings.localize.localizedString("itemLore_Soulbound"), ItemSet.ALL, 1, null);
	public static EnchantmentCustom Speed = new SpeedEnchantment(113);
	public static EnchantmentCustom TechOnly = new EnchantmentCustom(114, "techonly", CivColor.LightGrayBold + CivSettings.localize.localizedString("itemLore_techOnly"), ItemSet.NONE, 1, null);
	public static EnchantmentCustom Thorns = new EnchantmentCustom(115, "recoil", CivColor.Blue + "Отдача", ItemSet.CHESTPLATES, 5, null);
	public static EnchantmentCustom UnitItem = new EnchantmentCustom(116, "unit_item", CivColor.GoldBold + "Предмет юнита", ItemSet.NONE, 1, null);

	protected Enchantment enchantment;
	protected String displayName;

	public final Set<Material> naturalItems = new HashSet<>();

	public int id;
	public String name;
	public String group;
	public int maxLevel;

	@SuppressWarnings("deprecation")
	protected EnchantmentCustom(Enchantment enchant) {
		this.id = enchant.getId();
		this.name = enchant.getName().trim();
		if (!Enchantments.enchantmentList.containsKey(id)) Enchantments.enchantmentList.put(id, this);
		allEnchantmentCustom.put(this.name, this);
	}

	protected EnchantmentCustom(int id, String name, String displayName, ItemSet itemSet, int maxLevel, String group) {
		Validate.notEmpty(name, "The name must be present and not empty");
		this.id = id;
		this.displayName = displayName;
		this.addNaturalItems(itemSet);
		this.name = name.trim();
		this.group = (group == null) ? "Default" : group;
		this.maxLevel = maxLevel;

		if (!Enchantments.enchantmentList.containsKey(id)) Enchantments.enchantmentList.put(id, this);
		enchantment = new EnchantmentVirtual(id);
		allEnchantmentCustom.put(this.name, this);
	}

	protected EnchantmentCustom(final String name, String displyName, ItemSet itemSet, int maxLevel, String group) {
		this(getNextFreeId(), name, displyName, itemSet, maxLevel, group);
		allEnchantmentCustom.put(this.name, this);
	}

	public void addNaturalItems(final Material... materials) {
		for (Material material : materials) {
			Objects.requireNonNull(material, "Cannot add a null natural material");
			naturalItems.add(material);
		}
	}

	public void addNaturalItems(final ItemSet... items) {
		for (ItemSet item : items) {
			if (item.equals(ItemSet.NONE)) {
				naturalItems.clear();
				break;
			}
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

	public boolean canEnchantItem(ItemStack item) {
		return enchantment.canEnchantItem(item);
	}

	public String getDisplayName(int level) {
		if (enchantment.getName().startsWith("§")) return displayName + (enchantment.getMaxLevel() > 1 ? " " + level : "");
		return ChatColor.GRAY + displayName + (enchantment.getMaxLevel() > 1 ? " " + level : "");
	}

}