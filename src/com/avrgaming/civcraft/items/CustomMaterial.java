/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.items;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.avrgaming.civcraft.config.ConfigCraftableMaterial;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.mysql.jdbc.StringUtils;

import gpl.AttributeUtil;

public abstract class CustomMaterial {

	private String id;
	private int typeID;
	private short damage;

	private LinkedList<String> lore = new LinkedList<String>();
	private String name;

	protected static Map<String, CustomMaterial> materials = new HashMap<String, CustomMaterial>();
	protected static Map<String, BaseCustomMaterial> unitMaterials = new HashMap<String, BaseCustomMaterial>();
	protected static Map<String, BaseCustomMaterial> craftableMaterials = new HashMap<String, BaseCustomMaterial>();

	public static final String MID_TAG = CivColor.Black + "MID";

	public CustomMaterial(String id, int typeID, short damage) {
		this.id = id.toLowerCase();
		this.typeID = typeID;
		this.damage = damage;
		/* Adding quotes around id since NBTString does it =\ */
		materials.put(this.getId(), this);
		this.addMaterial();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/* Sets MID in NBT Data, use for production. */
	public static String getMID(ItemStack stack) {
		String mid = ItemManager.getProperty(stack, "mid");
		if (mid == null) return "";
		return mid.toLowerCase();
	}

	public static void setMIDAndName(AttributeUtil attrs, String mid, String name) {
		attrs.setCivCraftProperty("mid", mid);
		attrs.setName(name);
	}

	public abstract void addMaterial();

	//----------CustomMaterial
	public static CustomMaterial getCustomMaterial(ItemStack stack) {
		if (stack == null) return null;
		return materials.get(getMID(stack));
	}
	public static CustomMaterial getCustomMaterial(String mid) {
		return materials.get(mid.toLowerCase());
	}
	public static Collection<CustomMaterial> getAllCustomMaterial() {
		return materials.values();
	}
	public static boolean isCustomMaterial(ItemStack stack) {
		if (stack == null) return false;
		return materials.containsKey(getMID(stack));
	}

	//----------BaseCustomMaterial
	public static BaseCustomMaterial getBaseCustomMaterial(ItemStack stack) {
		if (stack == null) return null;
		String mid = getMID(stack);
		BaseCustomMaterial result = craftableMaterials.get(mid);
		if (result != null)
			return result;
		else
			return unitMaterials.get(mid);
	}
	public static BaseCustomMaterial getBaseCustomMaterial(String mid) {
		return (BaseCustomMaterial) materials.get(mid.toLowerCase());
	}
	public static boolean isBaseCustomMaterial(ItemStack stack) {
		if (stack == null) return false;
		String mid = getMID(stack);
		return craftableMaterials.containsKey(mid) || unitMaterials.containsKey(mid);
	}

	//----------CraftableCustomMaterial
	public static CraftableCustomMaterial getCraftableCustomMaterial(ItemStack stack) {
		if (stack == null) return null;
		return getCraftableCustomMaterial(getMID(stack));
	}
	public static CraftableCustomMaterial getCraftableCustomMaterial(String mid) {
		return (CraftableCustomMaterial) craftableMaterials.get(mid.toLowerCase());
	}
	public static Collection<BaseCustomMaterial> getAllCraftableCustomMaterial() {
		return craftableMaterials.values();
	}
	public static boolean isCraftableCustomMaterial(ItemStack stack) {
		if (stack == null) return false;
		return craftableMaterials.containsKey(getMID(stack));
	}

	//----------UnitCustomMaterial
	public static UnitCustomMaterial getUnitCustomMaterial(ItemStack stack) {
		if (stack == null) return null;
		return getUnitCustomMaterial(getMID(stack));
	}
	public static UnitCustomMaterial getUnitCustomMaterial(String mid) {
		return (UnitCustomMaterial) unitMaterials.get(mid.toLowerCase());
	}
	public static Collection<BaseCustomMaterial> getAllUnitCustomMaterial() {
		return unitMaterials.values();
	}
	public static boolean isUnitCustomMaterial(ItemStack stack) {
		if (stack == null) return false;
		return unitMaterials.containsKey(getMID(stack));
	}

	//----------
	public static ItemStack spawn(CustomMaterial material) {
		return spawn(material, 1);
	}

	public static ItemStack spawn(CustomMaterial material, int quantity) {
		ItemStack stack = ItemManager.createItemStack(material.getTypeID(), material.getDamage(), quantity);
		AttributeUtil attrs = new AttributeUtil(stack);
		setMIDAndName(attrs, material.getId(), material.getName());
		Boolean isShiny = false;
		if (material instanceof BaseCustomMaterial) {
			BaseCustomMaterial craftMat = (BaseCustomMaterial) material;
			if (material instanceof CraftableCustomMaterial) attrs.addLore(CivColor.ITALIC + ((ConfigCraftableMaterial) craftMat.getConfigMaterial()).category);
			if (craftMat.getConfigMaterial().tradeable) attrs.setCivCraftProperty("tradeable", "true");
			if (craftMat.getConfigMaterial().tradeValue >= 0) attrs.setCivCraftProperty("tradeValue", "" + craftMat.getConfigMaterial().tradeValue);
			isShiny = craftMat.getConfigMaterial().shiny;
		}

		if (material.getLore() != null) attrs.setLore(material.getLore());

		material.applyAttributes(attrs);
		ItemStack newStack = attrs.getStack();

		if (isShiny) addGlow(newStack);

		ItemMeta im = newStack.getItemMeta();
		im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newStack.setItemMeta(im);
		return newStack;
	}

	public static void addGlow(ItemStack stack) {
		ItemMeta meta = stack.getItemMeta();
		meta.addEnchant(Enchantment.LURE, 1, false);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		stack.setItemMeta(meta);
	}

	public int getTypeID() {
		return typeID;
	}

	public void setTypeID(int typeID) {
		this.typeID = typeID;
	}

	public short getDamage() {
		return damage;
	}

	public void setDamage(short damage) {
		this.damage = damage;
	}

	public void addLore(String lore) {
		this.lore.add(lore);
	}

	public void setLore(String lore) {
		this.lore.clear();
		this.lore.add(lore);
	}

	public void setLore(String[] lore) {
		this.lore.clear();
		if (lore != null) {
			for (String str : lore) {
				this.lore.add(str);
			}
		}
	}

	public LinkedList<String> getLore() {
		return this.lore;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static ItemStack addEnhancement(ItemStack stack, LoreEnhancement enhancement) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs = enhancement.add(attrs);
		return attrs.getStack();
	}

	public static boolean hasEnhancement(ItemStack stack, String enhName) {
		AttributeUtil attrs = new AttributeUtil(stack);
		return attrs.hasEnhancement(enhName);
	}

	public static boolean hasEnhancements(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		return attrs.hasEnhancements();
	}

	public static LinkedList<LoreEnhancement> getEnhancements(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		return attrs.getEnhancements();
	}

	public void applyAttributes(AttributeUtil attrs) {
		/* This is called when the item is created via the LoreMaterial.spawn() command. Can optionally be overriden by classes. */
		return;
	}

	public static String serializeEnhancements(ItemStack stack) {
		String out = "";

		for (LoreEnhancement enh : CraftableCustomMaterial.getEnhancements(stack)) {
			out += enh.getClass().getName() + "@" + enh.serialize(stack) + ",";
		}
		String outEncoded = new String(Base64Coder.encode(out.getBytes()));
		return outEncoded;
	}

	public static ItemStack deserializeEnhancements(ItemStack stack, String serial) {
		String in = StringUtils.toAsciiString(Base64Coder.decode(serial));
		String[] enhancementsStrs = in.split(",");

		for (String enhString : enhancementsStrs) {
			String[] split = enhString.split("@");
			String className = split[0];
			String data = "";
			if (split.length > 1) {
				data = split[1];
			}

			try {
				Class<?> cls = Class.forName(className);
				LoreEnhancement enh = (LoreEnhancement) cls.newInstance();
				AttributeUtil attrs = new AttributeUtil(stack);
				attrs.addEnhancement(cls.getSimpleName(), null, null);
				stack = attrs.getStack();
				stack = enh.deserialize(stack, data);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return stack;
	}

	/** Можно ли использовать предмет с этим типом инвентаря */
	public abstract boolean isCanUseInventoryTypes(Inventory inv);

	/* Events for this Material */
	public abstract void onHit(EntityDamageByEntityEvent event); /* Called when this is the item in-hand */
	public abstract void onInteract(PlayerInteractEvent event);
	public abstract void onInteractEntity(PlayerInteractEntityEvent event);
	public abstract void onBlockPlaced(BlockPlaceEvent event);
	public abstract void onBlockBreak(BlockBreakEvent event);
	public abstract void onBlockDamage(BlockDamageEvent event);
	public abstract void onBlockInteract(PlayerInteractEvent event);
	public abstract void onHold(PlayerItemHeldEvent event);
	public abstract boolean onAttack(EntityDamageByEntityEvent event, ItemStack stack); /* Called when this item is in inventory. */
	public abstract void onItemSpawn(ItemSpawnEvent event);
	public abstract void onDropItem(PlayerDropItemEvent event);
	public abstract void onCraftItem(CraftItemEvent event);
	public abstract void onPickupItem(EntityPickupItemEvent event);
	/** Предмет брошен в инвентарь */
	public abstract void onInvItemDrop(InventoryClickEvent event, Inventory toInv, ItemStack stack);
	/** Предмет поднять из инвентаря */
	public abstract void onInvItemPickup(InventoryClickEvent event, Inventory fromInv, ItemStack stack);
	/** Предмет (предметы) ложаться в инвентарь через протягиваение с зажатой кнопкой мыши */
	public abstract void onInvDrag(InventoryDragEvent event, Inventory toInv, ItemStack stack);
	public abstract void onPlayerDeath(EntityDeathEvent event, ItemStack stack);
	public abstract void onInventoryClose(InventoryCloseEvent event);
	public void onDefense(EntityDamageByEntityEvent event, ItemStack stack) {
	}
	public int onStructureBlockBreak(ConstructDamageBlock dmgBlock, int damage) {
		return damage;
	}
	public void onInventoryOpen(InventoryOpenEvent event, ItemStack stack) {
	}

}