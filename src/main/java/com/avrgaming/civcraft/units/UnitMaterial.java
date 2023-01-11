package com.avrgaming.civcraft.units;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.tasks.DelayMoveInventoryItem;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.gpl.AttributeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

/**
 * Клас предметов которые выступают в качестве юнитов
 */
public abstract class UnitMaterial extends CustomMaterial implements CooldownFinisher {

    public static final int LAST_SLOT = 8;
    public static HashMap<String, UnitMaterial> unitMaterials = new HashMap<>();
    private final ConfigUnit configUnit;

    public UnitMaterial(String id, ConfigUnit configUnit) {
        super(id, configUnit.item_id, (short) configUnit.item_data);
        this.configUnit = configUnit;
        this.setName(configUnit.name);
        unitMaterials.put(this.getId(), this);
    }

    // =============== init Unit
    public abstract void initLore(AttributeUtil attrs, UnitObject uo);

    public abstract void initUnitObject(UnitObject uo);

    public ConfigUnit getConfigUnit() {
        return configUnit;
    }

    // ============== extends CustomMaterial @Override

    @Override
    public void onBlockPlaced(BlockPlaceEvent event) {
        event.setCancelled(true);
        CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("unitMaterial_cannotPlace"));
    }

    @Override
    public void onHeld(PlayerItemHeldEvent event) {
    }

    @Override
    public void onInteractEntity(PlayerInteractEntityEvent event) {
    }

    @Override
    public void onCraftItem(CraftItemEvent event) {
        CivMessage.sendError(event.getWhoClicked(), CivSettings.localize.localizedString("unitItem_cannotCraft"));
        event.setCancelled(true);
    }

    @Override
    public void finishCooldown(Player player, ItemStack stack) {
        Resident resident = CivGlobal.getResident(player);
        UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack));
        if (resident.isUnitActive()) {
            // Деактивация юнита
            resident.setUnitObjectId(0);
            UnitStatic.removeChildrenItems(player);
            CivMessage.send(player, CivColor.LightGreenBold + "Юнит деактивирован");

            uo.used(resident);
        } else {
            // Активация юнита
            uo.used(resident);
            resident.setUnitObjectId(uo.getId());
            uo.dressAmmunitions(player);
            CivMessage.send(player, CivColor.LightGreenBold + "Юнит активирован ");
        }

        UnitStatic.updateUnitForPlaeyr(player);
        resident.calculateWalkingModifier(player);
        UnitStatic.setModifiedMovementSpeed(player);
        UnitStatic.setModifiedJumping(player);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        event.setUseInteractedBlock(Result.DENY);
        event.setUseItemInHand(Result.DENY);
        Player player = event.getPlayer();
        ItemStack stack = event.getItem();
        // Cooldown cooldown = Cooldown.getCooldown(stack);
        // if (cooldown != null) {
        // CivMessage.sendError(player, "Подождите " + cooldown.getTime() + " секунд");
        // return;
        // }
        UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack));
        if (uo == null) {
            CivMessage.send(player, "Юнит не найден. Можно спокойно выбросить этото предмет на мусорку");
            return;
        }
        try {
            uo.validateUnitUse(player);
            uo.validLastActivate();
        } catch (CivException e) {
            uo.setLastActivate(0);
            UnitStatic.removeUnit(player, uo.getConfigUnitId());
            CivMessage.send(player, e.getMessage());
            return;
        }

        // Cooldown.startCooldown(player, stack, 5, this);
        finishCooldown(player, stack);
    }

    @Override
    public boolean onDropItem(Player player, ItemStack stack) {
        CivMessage.sendError(player, "Этот предмет нельзя выбросить");
        return true;
    }

    @Override
    public void onPickupItem(EntityPickupItemEvent event) {
        // поднятие с земли предмета
        // if (event.getEntity() instanceof Player) {
        // Player player = (Player) event.getEntity();
        // Resident res = CivGlobal.getResident(player);
        // if (res.isUnitActive()) {
        // CivMessage.sendError(player, CivSettings.localize.localizedString("var_unitMaterial_errorHave", this.getConfigUnit().name));
        // event.setCancelled(true);
        // player.updateInventory();
        // return;
        // }
        // ItemStack stack = event.getItem().getItemStack();
        // UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack));
        // if (uo == null) {
        // CivMessage.sendErrorNoRepeat(player, "Юнит не найден.");
        // event.setCancelled(true);
        // player.updateInventory();
        // return;
        // }
        // try {
        // uo.validateUnitUse(player);
        // } catch (Exception e) {
        // CivMessage.sendErrorNoRepeat(player, e.getMessage());
        // event.setCancelled(true);
        // player.updateInventory();
        // return;
        // }
        // // Prevent dropping in two unit materials.
        // // Если у игрока уже есть юнит в инвентаре, то нового не ложим
        // List<Integer> slots = UnitStatic.findAllUnits(player.getInventory());
        // if (slots.size() != 0) {
        // CivMessage.sendError(player, CivSettings.localize.localizedString("var_unitMaterial_errorHave"));
        // event.setCancelled(true);
        // player.getInventory().remove(stack);
        // player.getWorld().dropItem(player.getLocation().add(player.getLocation().getDirection().multiply(2)), stack);
        // player.updateInventory();
        // return;
        // } else {
        // DelayMoveInventoryItem.beginTask(player, stack, LAST_SLOT);
        // return;
        // }
        // } else {
        event.setCancelled(true);
        // }
    }

    @Override
    public boolean onInvItemPickup(Cancellable event, Player player, Inventory fromInv, ItemStack stack) {
        // Забераем предмет из инвентаря
        if (fromInv.getHolder() instanceof Player) {
            if (CivGlobal.getResident(player).isUnitActive()) {
                CivMessage.sendError(player, "сперва нужно отключить юнита");
                return true;
            }
        }
        CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack)).used(CivGlobal.getResident(player));
        return false;
    }

    @Override
    public boolean onInvItemDrop(Cancellable event, Player player, Inventory toInv, ItemStack stack) {
        // Ложим предмет в инвентарь
        if (!this.isCanUseInventoryTypes(toInv)) {
            CivMessage.sendError(player, "Нельзя использовать этот предмет в инвентаре " + toInv.getType());
            return true;
        }
        if (toInv.getType() == InventoryType.PLAYER) {
            List<Integer> slots = UnitStatic.findAllUnits(player.getInventory());
            if (slots.size() > 0) {
                if (!toInv.getItem(slots.get(0)).equals(stack)) {
                    CivMessage.sendError(player, CivSettings.localize.localizedString("var_unitMaterial_errorHave"));
                    return true;
                }
            }
            DelayMoveInventoryItem.beginTaskSwap(event, player, stack, LAST_SLOT);
        }
        CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack)).used(CivGlobal.getResident(player));
        return false;
    }

    @Override
    public void onItemSpawn(ItemSpawnEvent event) {
    }

    @Override
    public boolean onAttack(EntityDamageByEntityEvent event, ItemStack stack) {
        return false;
    }

    @Override
    public void onPlayerDeath(EntityDeathEvent event, ItemStack stack) {
    }

    @Override
    public boolean isCanUseInventoryTypes(Inventory inv) {
        switch (inv.getType()) {
            case CHEST:
            case ENDER_CHEST:
            case PLAYER:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
    }

    public String getTirToString(String mid) {
        int tir = 0;
        if (mid != null && !mid.isEmpty()) tir = CivSettings.craftableMaterials.get(mid).tier;
        return CivColor.Green + ((tir != 0) ? "T" + tir : "--");
    }
}
