package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.threading.TaskMaster;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Ищет инвентаре предмет того же типа что stack и меняет его в инвентаре игрока
 * player с предметом из слота toSlot
 */
public class DelayMoveInventoryItem implements Runnable {

    /*
     * Sometimes we want to preform an action on an inventory after a very short
     * delay. For example, if we want to lock an item to a slot, we cannot cancel
     * the move event since that results in the item being 'dropped' on the ground.
     * Instead we have to let the move event complete, and then issue another action
     * to move the item back.
     */

    private final Cancellable event;
    private final int toSlot;
    private final Inventory inventory;
    private final Player player;
    private final ItemStack stack;
    private final Action action;

    public DelayMoveInventoryItem(Cancellable event, Player player, Inventory inventory, ItemStack stack, int toSlot, Action action) {
        this.action = action;
        this.event = event;
        this.player = player;
        this.inventory = inventory;
        this.toSlot = toSlot;
        this.stack = stack.clone();
    }

    public static void beginTaskSwap(Cancellable event, Player player, ItemStack stack, int slot) {
        TaskMaster.syncTask(new DelayMoveInventoryItem(event, player, player.getInventory(), stack, slot, Action.SWAP), 1);
    }

    public static void beginTaskRespawn(Cancellable event, Player player, Inventory inventory, ItemStack stack, int slot) {
        TaskMaster.syncTask(new DelayMoveInventoryItem(event, player, inventory, stack, slot, Action.RESPAWN), 1);
    }

    @Override
    public void run() {
        if (event.isCancelled())            return;

        if (action == Action.RESPAWN) {
            inventory.setItem(toSlot, stack);
            player.updateInventory();
            return;
        }

        if (action == Action.SWAP) {
            int fromSlot = -1;
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack is = inventory.getItem(i);
                if (is == null) continue;
                if (is.equals(stack)) {
                    fromSlot = i;
                    break;
                }
            }

            if (fromSlot == -1) return;
            if (fromSlot == toSlot) return;
            player.updateInventory();
            ItemStack fromStack = inventory.getItem(fromSlot);
            ItemStack toStack = inventory.getItem(toSlot);
            if (fromStack != null) {
                inventory.setItem(toSlot, fromStack);
                inventory.setItem(fromSlot, toStack);
            }
            player.updateInventory();
        }
    }

    enum Action {
        SWAP, RESPAWN
    }
}
