package com.avrgaming.civcraft.items;

import com.avrgaming.civcraft.config.ConfigCraftableMaterial;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.gpl.AttributeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public abstract class CustomMaterial {

    public static final String MID_TAG = CivColor.Black + "MID";
    protected static Map<String, CustomMaterial> materials = new HashMap<>();
    protected static Map<String, BaseCustomMaterial> unitMaterials = new HashMap<>();
    protected static Map<String, BaseCustomMaterial> craftableMaterials = new HashMap<>();
    private final LinkedList<String> lore = new LinkedList<>();
    private String id;
    private int typeID;
    private short damage;
    private String name;

    public CustomMaterial(String id, int typeID, short damage) {
        this.id = id.toLowerCase();
        this.typeID = typeID;
        this.damage = damage;
        /* Adding quotes around id since NBTString does it =\ */
        materials.put(this.getId(), this);
    }

    /* Sets MID in NBT Data, use for production. */
    public static String getMID(ItemStack stack) {
        String mid = ItemManager.getProperty(stack, "mid");
        if (mid == null) return "";
        return mid.toLowerCase();
    }

    // ----------CustomMaterial
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

    // ----------BaseCustomMaterial
    public static BaseCustomMaterial getBaseCustomMaterial(ItemStack stack) {
        if (stack == null) return null;
        String mid = getMID(stack);
        BaseCustomMaterial result = craftableMaterials.get(mid);
        if (result != null)
            return result;
        else return unitMaterials.get(mid);
    }

    public static BaseCustomMaterial getBaseCustomMaterial(String mid) {
        return (BaseCustomMaterial) materials.get(mid.toLowerCase());
    }

    public static boolean isBaseCustomMaterial(ItemStack stack) {
        if (stack == null) return false;
        String mid = getMID(stack);
        return craftableMaterials.containsKey(mid) || unitMaterials.containsKey(mid);
    }

    // ----------CraftableCustomMaterial
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

    // ----------UnitCustomMaterial
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

    // ----------
    public static ItemStack spawn(CustomMaterial material) {
        return spawn(material, 1);
    }

    public static ItemStack spawn(CustomMaterial material, int quantity) {
        ItemStack stack = ItemManager.createItemStack(material.getTypeID(), material.getDamage(), quantity);
        AttributeUtil attrs = new AttributeUtil(stack);
        attrs.setCivCraftProperty("mid", material.getId());
        attrs.setName(material.getName());
//		Boolean isShiny = false;
        if (material instanceof BaseCustomMaterial) {
            BaseCustomMaterial craftMat = (BaseCustomMaterial) material;
            if (material instanceof CraftableCustomMaterial)
                attrs.addLore(CivColor.ITALIC + ((ConfigCraftableMaterial) craftMat.getConfigMaterial()).category);
            if (craftMat.getConfigMaterial().tradeable) attrs.setCivCraftProperty("tradeable", "true");
            if (craftMat.getConfigMaterial().tradeValue >= 0)
                attrs.setCivCraftProperty("tradeValue", "" + craftMat.getConfigMaterial().tradeValue);
//			isShiny = craftMat.getConfigMaterial().shiny;
        }

        if (material.getLore() != null) attrs.setLore(material.getLore());

        material.applyAttributes(attrs);
        stack = attrs.getStack();

        ItemMeta meta = stack.getItemMeta();
//		if (isShiny) meta.addEnchant(Enchantment.LURE, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public LinkedList<String> getLore() {
        return this.lore;
    }

    public void setLore(String[] lore) {
        this.lore.clear();
        if (lore != null) {
            Collections.addAll(this.lore, lore);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void applyAttributes(AttributeUtil attrs) {
        /* This is called when the item is created via the LoreMaterial.spawn() command. Can optionally be overriden by classes. */
    }

    /** Можно ли использовать предмет с этим типом инвентаря */
    public abstract boolean isCanUseInventoryTypes(Inventory inv);

    /* Events for this Material */
    public abstract void onInteract(PlayerInteractEvent event);

    public abstract void onInteractEntity(PlayerInteractEntityEvent event);

    public abstract void onBlockPlaced(BlockPlaceEvent event);

    public abstract void onHeld(PlayerItemHeldEvent event);

    public abstract boolean onAttack(EntityDamageByEntityEvent event, ItemStack stack); /* Called when this item is in inventory. */

    public abstract void onItemSpawn(ItemSpawnEvent event);

    public abstract void onCraftItem(CraftItemEvent event);

    public abstract void onPickupItem(EntityPickupItemEvent event);

    public abstract boolean onDropItem(Player player, ItemStack stack);

    /** Предмет поднять из инвентаря */
    public abstract boolean onInvItemPickup(Cancellable event, Player player, Inventory fromInv, ItemStack stack);

    /** Предмет брошен в инвентарь. Все изменения скопируй в onInvItemPickup() */
    public abstract boolean onInvItemDrop(Cancellable event, Player player, Inventory toInv, ItemStack stack);

    public abstract void onPlayerDeath(EntityDeathEvent event, ItemStack stack);

    public abstract void onInventoryClose(InventoryCloseEvent event);

    public void onDefense(EntityDamageByEntityEvent event, ItemStack stack) {
    }

    public int onStructureBlockBreak(ConstructDamageBlock dmgBlock, int damage) {
        return damage;
    }

    public void onInventoryOpen(InventoryOpenEvent event, ItemStack stack) {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomMaterial)) return false;
        return this.getId().equals(((CustomMaterial) o).getId());
    }
}