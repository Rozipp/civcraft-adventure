/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.units;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Road;
import com.avrgaming.civcraft.util.CivColor;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import gpl.AttributeUtil;
import gpl.AttributeUtil.Attribute;
import gpl.AttributeUtil.AttributeType;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagInt;

public class UnitStatic {

	public static float T1_metal_speed;
	public static float T2_metal_speed;
	public static float T3_metal_speed;
	public static float T4_metal_speed;
	public static float normal_speed;

	public static int first_exp = 100;
	public static int step_exp = 15;
	public static int max_level = 100;
	public static int exp_for_player = 10;
	public static double percent_exp_per_level_unit = 0.05;
	public static double percent_exp_lost_when_dead = 0.3;
	public static int exp_for_neutral_entity = 0;

	public static HashMap<String, Integer> expEntity = new HashMap<>();

	public static HashMap<String, ConfigUnit> configUnits = new HashMap<>();
	public static HashMap<String, ConfigUnitComponent> configUnitComponents = new HashMap<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void init() {
		for (ConfigUnit cu : configUnits.values()) {
			String name = "com.avrgaming.civcraft.units." + cu.class_name.replace(" ", "_");
			try {
				Class cls = null;
				cls = Class.forName(name);
				Class partypes[] = { String.class, ConfigUnit.class };
				Constructor cntr = cls.getConstructor(partypes);
				Object arglist[] = { cu.id, cu };
				cntr.newInstance(arglist);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				CivLog.error("-----Class '" + name + "' creation error-----");
				e.printStackTrace();
			}
		}
	}

	public static void spawn(Inventory inv, Town town, String configUnitId) throws CivException {
		UnitMaterial um = UnitStatic.getUnit(configUnitId);
		if (um == null)
			throw new CivException(CivColor.Red + CivSettings.localize.localizedString("barracks_errorUnknown"));
		ItemStack is = CustomMaterial.spawn(um);

		UnitObject uo = new UnitObject(configUnitId, town);
		CivGlobal.addUnitObject(uo);
		is = setUnitIdNBTTag(is, uo.getId());

		is = initLoreStatic(is, uo);

		int slot = inv.firstEmpty();
		if (slot == -1)
			throw new CivException(
					CivSettings.localize.localizedString("var_settler_errorBarracksFull", um.getConfigUnit().name));
		inv.setItem(slot, is);
	}

	public static void respawn(Player player) throws CivException {
		Resident res = CivGlobal.getResident(player);
//		int uId = res.getUnitObjectId();
//		CivLog.debug("res.getUnitObjectId() = " + uId);
//		if (uId == -1)
//			throw new CivException("У это игрока не было юнита");

		UnitObject uo = null;
		for (UnitObject uot : CivGlobal.getUnitObjects())
			if (uot.getLastResident() == res) {
				uo = uot;
				break;
			}

//		if (uo.getLastResident() != res)
//			throw new CivException(
//					"Последний юнит игрока " + res.getName() + " принадлежит игроку " + uo.getLastResident().getName());

		UnitMaterial um = UnitStatic.getUnit(uo.getConfigUnitId());
		ItemStack is = CustomMaterial.spawn(um);
		is = setUnitIdNBTTag(is, uo.getId());
		is = initLoreStatic(is, uo);

		Inventory inv = player.getInventory();
		int slot = inv.firstEmpty();
		if (slot == -1)
			throw new CivException(
					CivSettings.localize.localizedString("var_settler_errorBarracksFull", um.getConfigUnit().name));
		inv.setItem(slot, is);
	}

	public static ItemStack initLoreStatic(ItemStack is, UnitObject uo) {
		int level = uo.getLevel();
		AttributeUtil attrs = new AttributeUtil(is);

		attrs.setName(uo.getName() + " уровень " + level);
		attrs.setLore("");
		attrs.setLore(uo.getConfigUnit().lore);
		attrs.addLore(CivColor.Rose + "Создан в городе: " + CivColor.LightBlue + uo.getTownOwner().getName());

		for (LoreEnhancement ench : attrs.getEnhancements()) {
			attrs.addLore(CivColor.Gold + ench.getDisplayName());
		}
		attrs.addEnhancement("LoreEnhancementSoulBound", null, null);
		attrs.addLore(CivColor.Rose + "Опыт: " + CivColor.YellowBold + uo.getExp() + "/" + uo.getTotalExpToNextLevel());

		UnitStatic.getUnit(uo.getConfigUnit().id).initLore(attrs, uo);
		is = attrs.getStack();
		return is;
	}

	public static void putItemSlot(PlayerInventory inv, ItemStack newStack, int slot, ArrayList<ItemStack> removes) {
		if (newStack == null)
			return;

		AttributeUtil attrs = new AttributeUtil(newStack);
		attrs.addEnhancement("LoreEnhancementUnitItem", null, null);
		attrs.addLore(CivColor.Gold + "Передмет юнита");
		newStack = attrs.getStack();

		ItemStack stack = inv.getItem(slot);
		if (stack != null)
			removes.add(stack);

		inv.setItem(slot, newStack);
	}

	public static ItemStack addAttribute(ItemStack stack, String name, Integer value) {
		if (stack == null)
			return null;
		AttributeUtil attrs;
		switch (name.toLowerCase()) {
		// ================= Helmet
		case "oxygen":
			stack.addEnchantment(Enchantment.OXYGEN, value);
			break;
		case "waterworker":
			stack.addEnchantment(Enchantment.WATER_WORKER, 1);
			break;
		// ================= Chestplate
		case "maxheal":
			attrs = new AttributeUtil(stack);
			attrs.add(Attribute.newBuilder().name("maxheal").type(AttributeType.GENERIC_MAX_HEALTH).amount(value)
					.build());
			attrs.addLore(CivColor.Blue + "+" + value + " MaxHeal");
			stack = attrs.getStack();
			break;
		case "protection":
			attrs = new AttributeUtil(stack);
			LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementDefense", value);
			stack = attrs.getStack();
			break;
		case "thorns":
			attrs = new AttributeUtil(stack);
			LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementThorns", value);
			stack = attrs.getStack();
			break;
		// ================= Leggings
		case "jumping":
			attrs = new AttributeUtil(stack);
			LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementJumping", value);
			stack = attrs.getStack();
			break;
		case "speed":
			attrs = new AttributeUtil(stack);
			LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementSpeed", value);
			stack = attrs.getStack();
			break;
		// ================= Boots
		case "depthstrider":
			stack.addEnchantment(Enchantment.DEPTH_STRIDER, value);
			break;
		case "againstfall":
//				stack.addEnchantment(Enchantment.PROTECTION_FALL, value);
			break;
		case "frostwalker":
			stack.addEnchantment(Enchantment.FROST_WALKER, value);
			break;
		// =================Ammunitions
		case "fireprotection":
			stack.addEnchantment(Enchantment.PROTECTION_FIRE, value);
			break;
		// ================= Sword
		case "swordattack":
			attrs = new AttributeUtil(stack);
			LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementAttack", value);
			stack = attrs.getStack();
			break;
		case "critical":
			attrs = new AttributeUtil(stack);
			LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementCritical", value);
			stack = attrs.getStack();
			break;
		case "swordknockback":
			stack.addEnchantment(Enchantment.ARROW_KNOCKBACK, value);
			break;
		case "fireaspect":
			stack.addEnchantment(Enchantment.FIRE_ASPECT, value);
			break;
		case "looting":
			stack.addEnchantment(Enchantment.LOOT_BONUS_MOBS, value);
			break;
		// ================= Bow
		case "bowattack":
			attrs = new AttributeUtil(stack);
			LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementAttack", value);
			stack = attrs.getStack();
			break;
		case "bowknockback":
			stack.addEnchantment(Enchantment.ARROW_KNOCKBACK, 1);
			break;
		case "flame":
			stack.addEnchantment(Enchantment.ARROW_FIRE, value);
			break;
		case "infinite":
			stack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
			break;
		default:
			break;
		}
		return stack;
	}

	public static int getExpEntity(String entityName) {
		return expEntity.getOrDefault(entityName, 0);
	}

	public static void addExpEntity(String entityName, Integer exp) {
		expEntity.put(entityName, exp);
	}

	/**
	 * Находит юнита в инвентаре. Обновляет его лоре. Обновляет уровень и опыт
	 * игрока.
	 */
	public static void updateUnitForPlaeyr(Player player) {
		Inventory inv = player.getInventory();
		Resident resident = CivGlobal.getResident(player);
		if (resident == null)
			return;
		int unitId = resident.getUnitObjectId();
		if (unitId > 0) {
			UnitObject uo = CivGlobal.getUnitObject(unitId);
			ItemStack is = findUnit(player);
			if (is == null) {
				CivLog.error("В инвентаре не найден юнит");
				return;
			}
			ItemStack isother = inv.getItem(UnitMaterial.LAST_SLOT);
			if (isother == null)
				inv.setItem(UnitMaterial.LAST_SLOT, is);
			else {
				Boolean swap = !is.equals(isother);

				inv.remove(is);
				is = initLoreStatic(is, uo);

				if (swap) {
					inv.remove(isother);
					inv.setItem(UnitMaterial.LAST_SLOT, is);
					inv.addItem(isother);
				} else
					inv.setItem(UnitMaterial.LAST_SLOT, is);
			}
			player.setExp(uo.getFloatExp());
			player.setLevel(uo.getLevel());
		} else {
			player.setExp(0);
			player.setLevel(0);
		}
		player.updateInventory();
	}

	public static void addExpToPlayer(Player player, int exp) {
		Resident res = CivGlobal.getResident(player);
		UnitObject uo = CivGlobal.getUnitObject(res.getUnitObjectId());
		if (uo == null)
			return;
		CivMessage.send(player, CivColor.LightGray + "   " + "Ваш " + CivColor.PurpleBold + uo.getName()
				+ CivColor.LightGray + " получил " + CivColor.Yellow + exp + CivColor.LightGray + " единиц опыта");
		uo.addExp(exp);
		UnitStatic.updateUnitForPlaeyr(player);
	}

	public static int calcLevel(int exp) {
		double d = Math.pow((2 * first_exp - step_exp), 2) + 8 * step_exp * exp;
		return (int) Math.floor((Math.sqrt(d) - 2 * first_exp + step_exp) / (2.0 * step_exp));
	}

	public static UnitMaterial getUnit(String unit_id) {
		return UnitMaterial.unitMaterials.get(unit_id);
	}

	/** Добавляет в NBTTag информацию value под ключом "unit_id" */
	public static ItemStack setUnitIdNBTTag(ItemStack stack, int value) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
		if (nmsStack == null)
			return stack;
		if (nmsStack.getTag() == null)
			nmsStack.setTag(new NBTTagCompound());
		nmsStack.getTag().set("unit_id", new NBTTagInt(value));
		return CraftItemStack.asCraftMirror(nmsStack);
	}

	/** @Возвращает информацию из NBTTag'а под ключем "unit_id" */
	public static int getUnitIdNBTTag(ItemStack stack) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
		if (nmsStack != null && nmsStack.getTag() != null) {
			return nmsStack.getTag().getInt("unit_id");
		}
		return 0;
	}

	public static UnitObject getPlayerUnitObject(final Player player) {
		int unitId = CivGlobal.getResident(player).getUnitObjectId();
		return CivGlobal.getUnitObject(unitId);
	}

	/** находит предмет класа UnitMaterial в инвентаре игрока */
	public static ItemStack findUnit(final Player player) {
		ItemStack st = player.getInventory().getItem(UnitMaterial.LAST_SLOT);
		if (st != null) {
			final CustomMaterial material = CustomMaterial.getCustomMaterial(st);
			if (material != null && material instanceof UnitMaterial)
				return st;
		}
		List<Integer> items = findAllUnits(player.getInventory());
		return (items.size() > 0) ? player.getInventory().getItem(items.get(0)) : null;
	}

	/** находит всех предметов класа UnitMaterial в инвентаре игрока */
	public static List<Integer> findAllUnits(final Inventory inv) {
		List<Integer> items = new ArrayList<Integer>();
		for (final ItemStack stack : inv.getContents()) {
			if (stack != null) {
				final CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
				if (material != null && material instanceof UnitMaterial)
					items.add(inv.first(stack));
			}
		}
		return items;
	}

	/** Удаляет все предмети юнита из инвентаря */
	public static void removeChildrenItems(Player player) {
		if (player == null)
			return;
		Resident res = CivGlobal.getResident(player);
		int unitId = res.getUnitObjectId();
		if (unitId <= 0)
			unitId = UnitStatic.getUnitIdNBTTag(UnitStatic.findUnit(player));
		UnitObject uo = CivGlobal.getUnitObject(unitId);
		for (int i = 0; i <= 40; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack == null)
				continue;
			if (CustomMaterial.hasEnhancement(stack, "LoreEnhancementUnitItem")) {
				String mid = CustomMaterial.getMID(stack);
				if (uo != null)
					uo.setAmunitionSlot(mid, i);
				player.getInventory().setItem(i, null);
			}
		}

		player.updateInventory();
	}

	public static void removeUnit(final Player player, final String id) {
		removeChildrenItems(player);
		ItemStack stack = findUnit(player);
		if (stack != null) {
			final CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
			if (material != null && material instanceof UnitMaterial
					&& ((UnitMaterial) material).getConfigUnit().id.equalsIgnoreCase(id)) {
				player.getInventory().remove(stack);
			}
			player.updateInventory();
		}
		CivGlobal.getResident(player).setUnitObjectId(0);
	}

	public static void setModifiedMovementSpeed(Player player) {
		/* Change move speed based on armor. */
		double speed = UnitStatic.normal_speed;
		Resident resident = CivGlobal.getResident(player);
		Entity vehicleEntity = player.getVehicle();
		if (resident != null) {
			speed = resident.getWalkingModifier();
			if (resident.isOnRoad()) {
				if (vehicleEntity != null && vehicleEntity.getType().equals(EntityType.HORSE)) {
					Vector vec = vehicleEntity.getVelocity();
					double yComp = vec.getY();

					vec.multiply(Road.ROAD_HORSE_SPEED);
					vec.setY(yComp); /* Do not multiply y velocity. */

					vehicleEntity.setVelocity(vec);
				} else {
					speed *= Road.ROAD_PLAYER_SPEED;
				}
			}
		}

		player.setWalkSpeed((float) Math.min(1.0f, speed));
	}

	public static void setModifiedJumping(Player player) {
		/* Change move speed based on armor. */
		ItemStack[] stacks = player.getInventory().getArmorContents();
		for (ItemStack is : stacks) {
			if (is == null)
				continue;
			AttributeUtil attrs = new AttributeUtil(is);
			if (attrs.hasEnhancement("LoreEnhancementJumping")) {
				Double level = Double.valueOf(attrs.getEnhancementData("LoreEnhancementJumping", "level"));
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 9999 * 20, level.intValue()));
				return;
//				Double.valueOf(attrs.getEnhancementData("LoreEnhancementSpeed", "value"));
			}
		}
		player.removePotionEffect(PotionEffectType.JUMP);
	}

	public static boolean isWearingFullComposite(final Player player) {
		return _isWearingFullLeather(player, "mat_composite_leather");
	}

	public static boolean isWearingFullHardened(final Player player) {
		return _isWearingFullLeather(player, "mat_hardened_leather");
	}

	public static boolean isWearingFullRefined(final Player player) {
		return _isWearingFullLeather(player, "mat_refined_leather");
	}

	public static boolean isWearingFullBasicLeather(final Player player) {
		return _isWearingFullLeather(player, "mat_leather_");
	}

	public static boolean _isWearingFullLeather(final Player player, String ss) {
		ItemStack[] armorContents = player.getInventory().getArmorContents();
		int length = armorContents.length;
		for (int i = 0; i < length; ++i) {
			final CraftableCustomMaterial craftMat = CraftableCustomMaterial
					.getCraftableCustomMaterial(armorContents[i]);
			if (craftMat == null)
				return false;
			if (!craftMat.getConfigId().contains(ss))
				return false;
		}
		return true;
	}

	public static boolean isWearingAnyMetal(final Player player) {
		for (ItemStack is : player.getInventory().getArmorContents()) {
			if (is == null)
				continue;
			if (isWearingAnyChain(is.getType()))
				return true;
			if (isWearingAnyDiamond(is.getType()))
				return true;
			if (isWearingAnyGold(is.getType()))
				return true;
			if (isWearingAnyIron(is.getType()))
				return true;
		}
		return false;
	}

	public static boolean isWearingAnyChain(Material mat) {
		if (mat == null)
			return false;
		return mat.equals(Material.CHAINMAIL_BOOTS) || mat.equals(Material.CHAINMAIL_CHESTPLATE)
				|| mat.equals(Material.CHAINMAIL_HELMET) || mat.equals(Material.CHAINMAIL_LEGGINGS);
	}

	public static boolean isWearingAnyGold(Material mat) {
		if (mat == null)
			return false;
		return mat.equals(Material.GOLD_BOOTS) || mat.equals(Material.GOLD_CHESTPLATE)
				|| mat.equals(Material.GOLD_HELMET) || mat.equals(Material.GOLD_LEGGINGS);
	}

	public static boolean isWearingAnyIron(Material mat) {
		if (mat == null)
			return false;
		return mat.equals(Material.IRON_BOOTS) || mat.equals(Material.IRON_CHESTPLATE)
				|| mat.equals(Material.IRON_HELMET) || mat.equals(Material.IRON_LEGGINGS);
	}

	public static boolean isWearingAnyDiamond(Material mat) {
		if (mat == null)
			return false;
		return mat.equals(Material.DIAMOND_BOOTS) || mat.equals(Material.DIAMOND_CHESTPLATE)
				|| mat.equals(Material.DIAMOND_HELMET) || mat.equals(Material.DIAMOND_LEGGINGS);
	}
}
