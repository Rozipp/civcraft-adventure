package com.avrgaming.civcraft.items;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ConfigCraftableMaterial;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.items.components.ItemComponent;
import com.avrgaming.civcraft.main.CivMessage;

import gpl.AttributeUtil;

public abstract class BaseCustomMaterial extends CustomMaterial {

    protected ConfigMaterial configMaterial;

    /* Components that are registered to this object. */
    public HashMap<String, ItemComponent> components = new HashMap<>();

    public BaseCustomMaterial(String id, int typeID, short damage) {
        super(id, typeID, damage);
    }

    public static void buildStaticMaterials() {
        /* Loads in materials from configuration file. */
    }

    public void buildComponents() {
        List<HashMap<String, String>> compInfoList = this.configMaterial.components;
        if (compInfoList != null) {
            for (HashMap<String, String> compInfo : compInfoList) {
                String className = "com.avrgaming.civcraft.items.components." + compInfo.get("name");
                Class<?> someClass;

                try {
                    someClass = Class.forName(className);
                    ItemComponent itemCompClass;
                    itemCompClass = (ItemComponent) someClass.newInstance();
                    itemCompClass.setName(compInfo.get("name"));

                    for (String key : compInfo.keySet()) {
                        itemCompClass.setAttribute(key, compInfo.get(key));
                    }

                    itemCompClass.createComponent();
                    this.components.put(itemCompClass.getName(), itemCompClass);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        for (ItemComponent ic : this.components.values()) {
            ic.onInteract(event);
        }
    }

    @Override
    public void onInteractEntity(PlayerInteractEntityEvent event) {
    }

    @Override
    public void onBlockPlaced(BlockPlaceEvent event) {
        for (ItemComponent ic : this.components.values()) {
            if (ic.onBlockPlaced(event)) return;
        }
        event.setCancelled(true);
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
        for (ItemComponent comp : this.components.values()) {
            comp.onHold(event);
        }
    }

    @Override
    public boolean onDropItem(Player player, ItemStack stack) {
        return false;
    }

    @Override
    public void onCraftItem(CraftItemEvent event) {
    }

    @Override
    public void onPickupItem(EntityPickupItemEvent event) {
    }

    @Override
    public void onItemSpawn(ItemSpawnEvent event) {
        for (ItemComponent comp : this.components.values()) {
            comp.onItemSpawn(event);
        }
    }

    @Override
    public boolean onAttack(EntityDamageByEntityEvent event, ItemStack stack) {
        for (ItemComponent comp : this.components.values()) {
            comp.onAttack(event, stack);
        }
        return false;
    }

    @Override
    public boolean onInvItemDrop(Cancellable event, Player player, Inventory toInv, ItemStack stack) {
        if (!this.isCanUseInventoryTypes(toInv)) {
            CivMessage.sendError(player, "Нельзя использовать этот предмет в инвентаре " + toInv.getType());
            return true;
        }
        return false;
    }

    @Override
    public boolean onInvItemPickup(Cancellable event, Player player, Inventory fromInv, ItemStack stack) {
        return false;
    }

    @Override
    public void onPlayerDeath(EntityDeathEvent event, ItemStack stack) {
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    @Override
    public int onStructureBlockBreak(ConstructDamageBlock sb, int damage) {
        for (ItemComponent component : this.components.values()) {
            damage = component.onStructureBlockBreak(sb, damage);
        }
        return damage;
    }

    @Override
    public void applyAttributes(AttributeUtil attrUtil) {
        for (ItemComponent comp : this.components.values()) {
            comp.onPrepareCreate(attrUtil);
        }
    }

    public ConfigMaterial getConfigMaterial() {
        return this.configMaterial;
    }

    public String getConfigId() {
        return this.configMaterial.id;
    }

    @Override
    public int hashCode() {
        return this.configMaterial.id.hashCode();
    }

    public Collection<ItemComponent> getComponents() {
        return this.components.values();
    }

    public void addComponent(ItemComponent itemComp) {
        this.components.put(itemComp.getName(), itemComp);
    }

    @Override
    public void onDefense(EntityDamageByEntityEvent event, ItemStack stack) {
        /* Search components for defense value. */
        for (ItemComponent comp : this.components.values()) {
            comp.onDefense(event, stack);
        }
    }

    public void onItemDurabilityChange(PlayerItemDamageEvent event) {
        for (ItemComponent comp : this.components.values()) {
            comp.onDurabilityChange(event);
        }
    }

    public boolean hasComponent(String string) {
        return this.components.containsKey(string);
    }

    public ItemComponent getComponent(String string) {
        return this.components.get(string);
    }

    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        /* Search components for defense value. */
        for (ItemComponent comp : this.components.values()) {
            comp.onPlayerInteractEntity(event);
        }
    }

    public void onPlayerLeashEvent(PlayerLeashEntityEvent event) {
        for (ItemComponent comp : this.components.values()) {
            comp.onPlayerLeashEvent(event);
        }
    }

    public void onRangedAttack(EntityDamageByEntityEvent event, ItemStack inHand) {
        for (ItemComponent comp : this.components.values()) {
            comp.onRangedAttack(event, inHand);
        }
    }

    public ItemChangeResult onDurabilityDeath(PlayerDeathEvent event, ItemStack stack) {
        ItemChangeResult result = null;
        for (ItemComponent comp : this.components.values()) {
            result = comp.onDurabilityDeath(event, result, stack);
        }
        return result;
    }

    @Override
    public void onInventoryOpen(InventoryOpenEvent event, ItemStack stack) {
        for (ItemComponent comp : this.components.values()) {
            comp.onInventoryOpen(event, stack);
        }
    }

    public boolean isVanilla() {
        return ((ConfigCraftableMaterial) this.configMaterial).vanilla;
    }

    @Override
    public boolean isCanUseInventoryTypes(Inventory inv) {
        if (inv == null) return false;
        switch (inv.getType()) {
            case CHEST:
            case CRAFTING:
            case DROPPER:
            case ENDER_CHEST:
            case HOPPER:
            case PLAYER:
            case SHULKER_BOX:
            case WORKBENCH:
                return true;
            default:
                return false;
        }
    }
}
