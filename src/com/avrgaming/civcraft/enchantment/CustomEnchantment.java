package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class LoreEnhancement implements Listener {
	public AttributeUtil add(AttributeUtil attrs) {
		return attrs;
	}

	private static HashMap<String, LoreEnhancement> enhancements = new HashMap<String, LoreEnhancement>();
	public HashMap<String, String> variables = new HashMap<String, String>();

	public static void init() {
		LoreEnhancement.enhancements.put("LoreEnhancementSoulBound", new LoreEnhancementSoulBound());
		LoreEnhancement.enhancements.put("LoreEnhancementAttack", new LoreEnhancementAttack());
		LoreEnhancement.enhancements.put("LoreEnhancementDefense", new LoreEnhancementDefense());
		LoreEnhancement.enhancements.put("LoreEnhancementPunchout", new LoreEnhancementPunchout());
		LoreEnhancement.enhancements.put("LoreEnhancementSpeed", new LoreEnhancementSpeed());
		LoreEnhancement.enhancements.put("LoreEnhancementUnitItem", new LoreEnhancementUnitItem());
		LoreEnhancement.enhancements.put("LoreEnhancementThorns", new LoreEnhancementThorns());
		LoreEnhancement.enhancements.put("LoreEnhancementJumping", new LoreEnhancementJumping());
		LoreEnhancement.enhancements.put("LoreEnhancementCritical", new LoreEnhancementCritical());
	}

	public static LoreEnhancement getFromName(String name) {
		return LoreEnhancement.enhancements.get(name);
	}

	public static void addLoreEnchancementValue(AttributeUtil attrs, String name, Integer level) {
		LoreEnhancement le = getFromName(name);
		Double amount = (le != null) ? Double.valueOf(le.variables.getOrDefault("amount", "1.0")) : 1;
		Double value = amount * level;
		attrs.addEnhancement(name, "level", level.toString());
		attrs.addEnhancement(name, "value", value.toString());
		attrs.addLore(le.getLoreString(value));
	}

	public String getLoreString(Double baseLevel) {
		if (baseLevel == 0) return CivColor.Blue + this.getDisplayName();
		return CivColor.Blue + this.getDisplayName() + " " + baseLevel;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		ItemStack stack = event.getItemDrop().getItemStack();
		if (stack == null) return;
		AttributeUtil attrs = new AttributeUtil(stack);
		if (!attrs.hasEnhancements()) return;
		for (LoreEnhancement enhance : attrs.getEnhancements()) {
			if (enhance instanceof LoreEnhancementUnitItem) {
				event.setCancelled(true);
				event.getPlayer().updateInventory();
				CivMessage.send(event.getPlayer(), CivSettings.localize.localizedString("unitItem_cannotDrop"));
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack stack = event.getItem();
		if (stack == null) return;
		AttributeUtil attrs = new AttributeUtil(stack);
		if (!attrs.hasEnhancements()) return;
		for (LoreEnhancement enhance : attrs.getEnhancements()) {
			if (enhance instanceof LoreEnhancementUnitItem) {
				Player player = (Player) event.getPlayer();
				if (!CivGlobal.getResident(player).isUnitActive()) {
					UnitStatic.removeChildrenItems(player);
					event.setCancelled(true);
					CivMessage.send(event.getPlayer(), "Юнит не активен. Предметы юнита были удалены");
				}
			}
		}
	}
	public boolean canEnchantItem(ItemStack item) {
		return true;
	}

	public static boolean isWeapon(ItemStack item) {
		switch (ItemManager.getTypeId(item)) {
			case CivData.WOOD_SWORD :
			case CivData.STONE_SWORD :
			case CivData.IRON_SWORD :
			case CivData.GOLD_SWORD :
			case CivData.DIAMOND_SWORD :
			case CivData.WOOD_AXE :
			case CivData.STONE_AXE :
			case CivData.IRON_AXE :
			case CivData.GOLD_AXE :
			case CivData.DIAMOND_AXE :
			case CivData.BOW :
				return true;
			default :
				return false;
		}
	}

	public static boolean isArmor(ItemStack item) {
		switch (ItemManager.getTypeId(item)) {
			case CivData.LEATHER_BOOTS :
			case CivData.LEATHER_CHESTPLATE :
			case CivData.LEATHER_HELMET :
			case CivData.LEATHER_LEGGINGS :
			case CivData.IRON_BOOTS :
			case CivData.IRON_CHESTPLATE :
			case CivData.IRON_HELMET :
			case CivData.IRON_LEGGINGS :
			case CivData.DIAMOND_BOOTS :
			case CivData.DIAMOND_CHESTPLATE :
			case CivData.DIAMOND_HELMET :
			case CivData.DIAMOND_LEGGINGS :
			case CivData.CHAIN_BOOTS :
			case CivData.CHAIN_CHESTPLATE :
			case CivData.CHAIN_HELMET :
			case CivData.CHAIN_LEGGINGS :
			case CivData.GOLD_BOOTS :
			case CivData.GOLD_CHESTPLATE :
			case CivData.GOLD_HELMET :
			case CivData.GOLD_LEGGINGS :
				return true;
			default :
				return false;
		}
	}

	public static boolean isTool(ItemStack item) {
		switch (ItemManager.getTypeId(item)) {
			case CivData.WOOD_SHOVEL :
			case CivData.WOOD_PICKAXE :
			case CivData.WOOD_AXE :
			case CivData.STONE_SHOVEL :
			case CivData.STONE_PICKAXE :
			case CivData.STONE_AXE :
			case CivData.IRON_SHOVEL :
			case CivData.IRON_PICKAXE :
			case CivData.IRON_AXE :
			case CivData.DIAMOND_SHOVEL :
			case CivData.DIAMOND_PICKAXE :
			case CivData.DIAMOND_AXE :
			case CivData.GOLD_SHOVEL :
			case CivData.GOLD_PICKAXE :
			case CivData.GOLD_AXE :
				return true;
			default :
				return false;
		}
	}

	public static boolean isWeaponOrArmor(ItemStack item) {
		return isWeapon(item) || isArmor(item);
	}

	public boolean hasEnchantment(ItemStack item) {
		return false;
	}

	public String getDisplayName() {
		return "LoreEnchant";
	}

	public double getLevel(AttributeUtil attrs) {
		return Double.valueOf(this.variables.getOrDefault("level", "0.0"));
	}

	public String serialize(ItemStack stack) {
		return "";
	}
	public ItemStack deserialize(ItemStack stack, String data) {
		return stack;
	}

}
