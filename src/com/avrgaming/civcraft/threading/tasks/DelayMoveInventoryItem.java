/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.threading.TaskMaster;

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

	public int toSlot;
	public Player player;
	public ItemStack stack;

	public DelayMoveInventoryItem(Player player, int toSlot, ItemStack stack) {
		this.toSlot = toSlot;
		this.player = player;
		this.stack = stack;
	}

	public static void beginTask(Player player, ItemStack is, int slot) {
		TaskMaster.syncTask(new DelayMoveInventoryItem(player, slot, is), 1);
	}

	@Override
	public void run() {
		int fromSlot = -1;
		CustomMaterial loreMat = CustomMaterial.getCustomMaterial(stack);
		if (loreMat == null) {
			for (ItemStack is : player.getInventory()) {
				if (is == null)
					continue;
				if (is.equals(stack)) {
					fromSlot = player.getInventory().first(is);
					break;
				}
			}
		} else {
			for (ItemStack is : player.getInventory()) {
				if (is == null)
					continue;
				CustomMaterial lm = CustomMaterial.getCustomMaterial(is);
				if (lm == null)
					continue;
				if (lm.getId() == loreMat.getId()) {
					fromSlot = player.getInventory().first(is);
					break;
				}
			}
		}
		if (fromSlot == -1)
			return;

		ItemStack fromStack = player.getInventory().getItem(fromSlot);
		ItemStack toStack = player.getInventory().getItem(toSlot);

		if (fromStack != null) {
			player.getInventory().setItem(toSlot, fromStack);
			player.getInventory().setItem(fromSlot, toStack);
		}
		player.updateInventory();
	}
}
