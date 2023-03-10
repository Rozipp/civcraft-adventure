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

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BaseCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import gpl.AttributeUtil;
import gpl.AttributeUtil.Attribute;
import gpl.AttributeUtil.AttributeType;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagInt;

public class UnitStatic {
	public static int first_exp = 100;
	public static int step_exp = 15;
	public static int max_level = 100;
	public static int exp_for_player = 10;
	public static double percent_exp_per_level_unit = 0.05;
	public static double percent_exp_lost_when_dead = 0.3;
	public static int exp_for_neutral_entity = 0;

	public static HashMap<String, UnitMaterial> unitMaterials = new HashMap<>();
	public static HashMap<String, Integer> expEntity = new HashMap<>();

	public static HashMap<String, ConfigUnit> configUnits = new HashMap<>();
	public static HashMap<String, ConfigUnitComponent> configUnitComponents = new HashMap<>();

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void init() {
		for (ConfigUnit cu : configUnits.values()) {
			String name = "com.avrgaming.civcraft.units." + cu.class_name.replace(" ", "_");
			try {
				Class cls = null;
				cls = Class.forName(name);
				Class partypes[] = {String.class, ConfigUnit.class};
				Constructor cntr = cls.getConstructor(partypes);
				Object arglist[] = {cu.id, cu};
				UnitMaterial unit = (UnitMaterial) cntr.newInstance(arglist);
				unit.initAmmunitions();
				unitMaterials.put(cu.id, unit);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				CivLog.error("-----Class '" + name + "' creation error-----");
				e.printStackTrace();
			}
		}
	}

	public static void spawn(Inventory inv, Town town, String configUnitId) throws CivException {
		UnitMaterial um = UnitStatic.getUnit(configUnitId);
		if (um == null) throw new CivException(CivColor.Red + CivSettings.localize.localizedString("barracks_errorUnknown"));
		ItemStack is = CustomMaterial.spawn(um);

		UnitObject uo = new UnitObject(configUnitId, town.getId());
		CivGlobal.addUnitObject(uo);
		is = setUnitIdNBTTag(is, uo.getId());

		is = initLoreStatic(is, uo);

		int slot = inv.firstEmpty();
		if (slot == -1) throw new CivException(CivSettings.localize.localizedString("var_settler_errorBarracksFull", um.getConfigUnit().name));
		inv.setItem(slot, is);
	}

	public static ItemStack initLoreStatic(ItemStack is, UnitObject uo) {
		int level = uo.getLevel();
		AttributeUtil attrs = new AttributeUtil(is);

		attrs.setName(uo.getName() + " ?????????????? " + level);
		attrs.setLore("");
		attrs.setLore(uo.getConfigUnit().lore);
		attrs.addLore(CivColor.Rose + "???????????? ?? ??????????????????????: " + CivColor.LightBlue + uo.getCivilizationOwner().getName());

		for (LoreEnhancement ench : attrs.getEnhancements()) {
			attrs.addLore(CivColor.Gold + ench.getDisplayName());
		}
		attrs.addEnhancement("LoreEnhancementSoulBound", null, null);
//		attrs.addLore(CivColor.Gold + CivSettings.localize.localizedString("Soulbound"));

		attrs.addLore(CivColor.Rose + "????????: " + CivColor.YellowBold + uo.getExp() + "/" + uo.getTotalExpToNextLevel());

		UnitStatic.getUnit(uo.getConfigUnit().id).initLore(attrs, uo);
		is = attrs.getStack();
		return is;
	}

	public static ItemStack initLoreStatic(ItemStack is) {
		return initLoreStatic(is, CivGlobal.getUnitObject(getUnitIdNBTTag(is)));
	}

	public static void putItemSlot(PlayerInventory inv, ItemStack newStack, int slot, ArrayList<ItemStack> removes) {
		if (newStack == null) return;

		AttributeUtil attrs = new AttributeUtil(newStack);
		attrs.addEnhancement("LoreEnhancementUnitItem", null, null);
		attrs.addLore(CivColor.Gold + "???????????????? ??????????");
		newStack = attrs.getStack();

		ItemStack stack = inv.getItem(slot);
		if (stack != null) removes.add(stack);

		inv.setItem(slot, newStack);
	}

	public static ItemStack addAttribute(ItemStack stack, String name, Integer value) {
		if (stack == null) return null;
		AttributeUtil attrs;
		switch (name.toLowerCase()) {
			//================= Helmet
			case "oxygen" :
				stack.addEnchantment(Enchantment.OXYGEN, value);
				break;
			case "waterworker" :
				stack.addEnchantment(Enchantment.WATER_WORKER, 1);
				break;
			//================= Chestplate	
			case "maxheal" :
				attrs = new AttributeUtil(stack);
				attrs.add(Attribute.newBuilder().name("maxheal").type(AttributeType.GENERIC_MAX_HEALTH).amount(value).build());
				attrs.addLore(CivColor.Blue + "+" + value + " MaxHeal");
				stack = attrs.getStack();
				break;
			case "protection" :
				attrs = new AttributeUtil(stack);
				LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementDefense", value);
				stack = attrs.getStack();
				break;
			case "thorns" :
				attrs = new AttributeUtil(stack);
				LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementThorns", value);
				stack = attrs.getStack();
				break;
			//================= Leggings	
			case "jumping" :

				break;
			case "speed" :
				attrs = new AttributeUtil(stack);
				LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementSpeed", value);
				stack = attrs.getStack();
				break;
			//================= Boots	
			case "depthstrider" :
				stack.addEnchantment(Enchantment.DEPTH_STRIDER, value);
				break;
			case "againstfall" :
//				stack.addEnchantment(Enchantment.PROTECTION_FALL, value);
				break;
			case "frostwalker" :
				stack.addEnchantment(Enchantment.FROST_WALKER, value);
				break;
			//=================Ammunitions	
			case "fireprotection" :
				stack.addEnchantment(Enchantment.PROTECTION_FIRE, value);
				break;
			//================= Sword
			case "swordattack" :
				attrs = new AttributeUtil(stack);
				LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementAttack", value);
				stack = attrs.getStack();
				break;
			case "swordknockback" :
				stack.addEnchantment(Enchantment.ARROW_KNOCKBACK, value);
				break;
			case "fireaspect" :
				stack.addEnchantment(Enchantment.FIRE_ASPECT, value);
				break;
			case "looting" :
				stack.addEnchantment(Enchantment.LOOT_BONUS_MOBS, value);
				break;
			//================= Bow
			case "bowattack" :
				attrs = new AttributeUtil(stack);
				LoreEnhancement.addLoreEnchancementValue(attrs, "LoreEnhancementAttack", value);
				stack = attrs.getStack();
				break;
			case "bowknockback" :
				stack.addEnchantment(Enchantment.ARROW_KNOCKBACK, 1);
				break;
			case "flame" :
				stack.addEnchantment(Enchantment.ARROW_FIRE, value);
				break;
			case "infinite" :
				stack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
				break;
			default :
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

	/** ?????????????? ?????????? ?? ??????????????????. ?????????????????? ?????? ????????. ?????????????????? ?????????????? ?? ???????? ????????????. */
	public static void updateUnitForPlaeyr(Player player) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;
		int unitId = resident.getUnitId();
		if (unitId > 0) {
			UnitObject uo = CivGlobal.getUnitObject(unitId);
			ItemStack is = findUnit(player);
			Boolean errorLastSlot = !player.getInventory().getItem(UnitMaterial.LAST_SLOT).equals(is);
			player.getInventory().remove(is);

			is = initLoreStatic(is, uo);
			if (errorLastSlot) {
				ItemStack isother = player.getInventory().getItem(UnitMaterial.LAST_SLOT);
				player.getInventory().remove(isother);
				player.getInventory().setItem(UnitMaterial.LAST_SLOT, is);
				player.getInventory().addItem(isother);
			} else
				player.getInventory().setItem(UnitMaterial.LAST_SLOT, is);
			player.setExp(uo.getFloatExp());
			player.setLevel(uo.getLevel());
			player.updateInventory();
		} else {
			player.setExp(0);
			player.setLevel(0);
			player.updateInventory();
		}
	}

	public static void addExpToPlayer(Player player, int exp) {
		Resident res = CivGlobal.getResident(player);
		UnitObject uo = CivGlobal.getUnitObject(res.getUnitId());
		CivMessage.send(player, CivColor.LightGray + "   " + "?????? " + CivColor.PurpleBold + uo.getName() + CivColor.LightGray + " ?????????????? " + CivColor.Yellow
				+ exp + CivColor.LightGray + " ???????????? ??????????");
		uo.addExp(exp);
		UnitStatic.updateUnitForPlaeyr(player);
	}

	public static int calcLevel(int exp) {
		double d = Math.pow((2 * first_exp - step_exp), 2) + 8 * step_exp * exp;
		return (int) Math.floor((Math.sqrt(d) - 2 * first_exp + step_exp) / (2.0 * step_exp));
	}

	public static UnitMaterial getUnit(String unit_id) {
		return unitMaterials.get(unit_id);
	}
	/** ?????????????????? ?? NBTTag ???????????????????? value ?????? ???????????? "unit_id" */
	public static ItemStack setUnitIdNBTTag(ItemStack stack, int value) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
		if (nmsStack == null) return stack;
		if (nmsStack.getTag() == null) nmsStack.setTag(new NBTTagCompound());
		nmsStack.getTag().set("unit_id", new NBTTagInt(value));
		return CraftItemStack.asCraftMirror(nmsStack);
	}

	/** @???????????????????? ???????????????????? ???? NBTTag'?? ?????? ???????????? "unit_id" */
	public static int getUnitIdNBTTag(ItemStack stack) {
		net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
		if (nmsStack != null && nmsStack.getTag() != null) {
			return nmsStack.getTag().getInt("unit_id");
		}
		return 0;
	}

	public static ConfigUnit getPlayerConfigUnit(final Player player) {
		int unitId = CivGlobal.getResident(player).getUnitId();
		return CivGlobal.getUnitObject(unitId).getConfigUnit();
	}

	/** ?????????????? ?????????????? ?????????? Unit ?? ?????????????????? ???????????? */
	public static ItemStack findUnit(final Player player) {
		ItemStack st = player.getInventory().getItem(UnitMaterial.LAST_SLOT);
		if (st != null) {
			final CustomMaterial material = CustomMaterial.getCustomMaterial(st);
			if (material != null && material instanceof UnitMaterial) return st;
		}
		for (final ItemStack stack : player.getInventory().getContents()) {
			if (stack != null) {
				final CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
				if (material != null && material instanceof UnitMaterial) return stack;
			}
		}
		return null;
	}

	/** ?????????????? ?????? ???????????????? ?????????? ???? ?????????????????? */
	public static void removeChildrenItems(Player player) {
		Resident res = CivGlobal.getResident(player);
		int unitId = res.getUnitId();
		if (unitId <= 0) unitId = UnitStatic.getUnitIdNBTTag(UnitStatic.findUnit(player));
		UnitObject uo = CivGlobal.getUnitObject(unitId);
		for (int i = 0; i <= 40; i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack == null) continue;
			if (CustomMaterial.hasEnhancement(stack, "LoreEnhancementUnitItem")) {
				String mid = CustomMaterial.getMID(stack);
				if (uo != null) uo.setAmunitionSlot(mid, i);
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
			if (material != null && material instanceof UnitMaterial && ((UnitMaterial) material).getConfigUnit().id.equalsIgnoreCase(id)) {
				player.getInventory().remove(stack);
			}
			player.updateInventory();
		}
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
			final BaseCustomMaterial craftMat = BaseCustomMaterial.getBaseCustomMaterial(armorContents[i]);
			if (craftMat == null) return false;
			if (!craftMat.getConfigId().contains(ss)) return false;
		}
		return true;
	}

	public static boolean isWearingAnyMetal(final Player player) {
		return isWearingAnyChain(player) || isWearingAnyGold(player) || isWearingAnyIron(player) || isWearingAnyDiamond(player);
	}
	public static boolean isWearingAnyChain(final Player player) {
		return (player.getEquipment().getBoots() != null && player.getEquipment().getBoots().getType().equals((Object) Material.CHAINMAIL_BOOTS))
				|| (player.getEquipment().getChestplate() != null
						&& player.getEquipment().getChestplate().getType().equals((Object) Material.CHAINMAIL_CHESTPLATE))
				|| (player.getEquipment().getHelmet() != null && player.getEquipment().getHelmet().getType().equals((Object) Material.CHAINMAIL_HELMET))
				|| (player.getEquipment().getLeggings() != null && player.getEquipment().getLeggings().getType().equals((Object) Material.CHAINMAIL_LEGGINGS));
	}
	public static boolean isWearingAnyGold(final Player player) {
		return (player.getEquipment().getBoots() != null && player.getEquipment().getBoots().getType().equals((Object) Material.GOLD_BOOTS))
				|| (player.getEquipment().getChestplate() != null && player.getEquipment().getChestplate().getType().equals((Object) Material.GOLD_CHESTPLATE))
				|| (player.getEquipment().getHelmet() != null && player.getEquipment().getHelmet().getType().equals((Object) Material.GOLD_HELMET))
				|| (player.getEquipment().getLeggings() != null && player.getEquipment().getLeggings().getType().equals((Object) Material.GOLD_LEGGINGS));
	}
	public static boolean isWearingAnyIron(final Player player) {
		return (player.getEquipment().getBoots() != null && ItemManager.getTypeId(player.getEquipment().getBoots()) == 309)
				|| (player.getEquipment().getChestplate() != null && ItemManager.getTypeId(player.getEquipment().getChestplate()) == 307)
				|| (player.getEquipment().getHelmet() != null && ItemManager.getTypeId(player.getEquipment().getHelmet()) == 306)
				|| (player.getEquipment().getLeggings() != null && ItemManager.getTypeId(player.getEquipment().getLeggings()) == 308);
	}
	public static boolean isWearingAnyDiamond(final Player player) {
		return (player.getEquipment().getBoots() != null && ItemManager.getTypeId(player.getEquipment().getBoots()) == 313)
				|| (player.getEquipment().getChestplate() != null && ItemManager.getTypeId(player.getEquipment().getChestplate()) == 311)
				|| (player.getEquipment().getHelmet() != null && ItemManager.getTypeId(player.getEquipment().getHelmet()) == 310)
				|| (player.getEquipment().getLeggings() != null && ItemManager.getTypeId(player.getEquipment().getLeggings()) == 312);
	}
}
