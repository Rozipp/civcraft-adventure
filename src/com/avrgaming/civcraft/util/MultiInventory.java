/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.SourceItem;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.sync.SyncUpdateInventory;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.google.common.collect.ObjectArrays;

public class MultiInventory {

	public ArrayList<Inventory> invs = new ArrayList<Inventory>();

	/* Returns number of items removed. */
	private int removeItemFromInventory(Inventory inv, String mid, int type, short data, int amount, Boolean direct) {
		int removed = 0;
		int notRemoved = amount;

		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if (!ItemManager.isCorrectItemStack(stack, mid, type, data)) {
				continue;
			}

			/* We've got the item we're looking for, lets remove as many as we can. */
			int stackAmount = stack.getAmount();

			if (stackAmount > notRemoved) {
				/* We've got more than enough in this stack. Reduce it's size. */
				stack.setAmount(stackAmount - notRemoved);
				removed = notRemoved;
				notRemoved = 0; /* We've got it all. */
			} else
				if (stackAmount == notRemoved) {
					/* Got the entire stack, null it out. */
					contents[i] = null;
					removed = notRemoved;
					notRemoved = 0;
				} else {
					/* There was some in this stack, but not enough for all we were looking for. */
					removed += stackAmount;
					notRemoved -= stackAmount;
					contents[i] = null;
				}

			if (notRemoved == 0) {
				/* We've removed everything. No need to continue. */
				break;
			}
		}

		if (direct) {
			inv.setContents(contents);
		} else {
			this.updateInventory(inv, contents, Action.SETCONTENTS);
		}
		return removed;
	}

	private void updateInventory(Inventory inv, ItemStack[] contents, Action action) {
		UpdateInventoryRequest request = new UpdateInventoryRequest(SyncUpdateInventory.lock);
		request.cont = contents;
		request.inv = inv;
		request.action = action;
		this.doSyncUpdateInventory(request);
	}

	private void updateInventory(ItemStack stack, Action action) {
		UpdateInventoryRequest request = new UpdateInventoryRequest(SyncUpdateInventory.lock);
		request.stack = stack;
		request.multiInv = this;
		request.action = action;
		this.doSyncUpdateInventory(request);
	}

	private void updateInventory(Inventory inv, Integer slot, ItemStack stack, Action action) {
		UpdateInventoryRequest request = new UpdateInventoryRequest(SyncUpdateInventory.lock);
		request.stack = stack;
		request.inv = inv;
		request.slot = slot;
		request.multiInv = this;
		request.action = action;
		this.doSyncUpdateInventory(request);
	}

	private void doSyncUpdateInventory(UpdateInventoryRequest request) {
		SyncUpdateInventory.lock.lock();
		try {
			SyncUpdateInventory.requestQueue.add(request);
			while (!request.finished) {
				/* We await for the finished flag to be set, at this time the await function will give up the lock above and automagically re-lock when its
				 * finished. */
				try {
					request.condition.await(5000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!request.finished) CivLog.warning("Couldn't update MultiInventory in 5000 milliseconds! Retrying.");
			}
		} finally {
			SyncUpdateInventory.requestQueue.remove(request);
			SyncUpdateInventory.lock.unlock();
		}
	}

	public void addInventory(Inventory inv) {
		invs.add(inv);
	}

	public void setItemStack(Inventory inv, Integer slot, ItemStack items, Boolean sync) {
		if (sync) {
			inv.setItem(slot, items);
		} else {
			this.updateInventory(inv, slot, items, Action.REPLACESTACK);
		}
	}

	public void addItems(List<ItemStack> items, Boolean sync) {
		if (sync)
			for (ItemStack is : items)
				addItemStackSync(is);
		else
			for (ItemStack is : items)
				addItemStackAsync(is);
	}

	public void addItemStack(ItemStack items, Boolean sync) {
		if (sync) {
			addItemStackSync(items);
		} else {
			addItemStackAsync(items);
		}
	}

	public void addItemStackAsync(ItemStack items) {
		this.updateInventory(items, Action.ADDSTACK);
	}

	public int addItemStackSync(ItemStack items) {
		// Try to find an open spot in any of the inventories.
		HashMap<Integer, ItemStack> leftovers;
		int leftoverAmount = 0;
		for (Inventory inv : invs) {

			// 1) Leftovers is the same size as items. This inv was full, move on to next.
			// 2) Leftovers is none or zero, we are done return
			// 3) keep trying to place the leftovers in another chest.

			leftoverAmount = 0;
			leftovers = inv.addItem(items);

			for (ItemStack i : leftovers.values()) {
				leftoverAmount += i.getAmount();
			}

			if (leftoverAmount == 0) return 0;
			if (leftoverAmount == items.getAmount()) continue;

			// Some items were deposited, update the items count and try again.
			items.setAmount(leftoverAmount);
		}
		return leftoverAmount;
	}

	/* Validates that we have the right amount of this item before it takes it away. */
	public boolean removeItem(String mid, int type, short data, int amount, Boolean direct) throws CivException {
		ArrayList<ItemInvPair> toBeRemoved = new ArrayList<ItemInvPair>();

		int count = amount;
		for (Inventory inv : invs) {
			for (ItemStack item : inv.getContents()) {
				if (!ItemManager.isCorrectItemStack(item, mid, type, data)) continue;

				// Three possibilities,
				// 1) This item stack has more than we are looking for. So we add a new item stack to the hashmap, with the size
				// for the amount we want to remove. Break, and it will then be removed.
				// 2) This item stack is exactly equal to the amount we want removed. Add it to the hashmap, and break.
				// 3) This item is NOT large enough for what we want, update the count, add it to the hashmap and keep looking.

				if (item.getAmount() > count) {
					toBeRemoved.add(new ItemInvPair(inv, mid, type, data, count));
					count = 0;
					break;
				} else
					if (item.getAmount() == count) {
						toBeRemoved.add(new ItemInvPair(inv, mid, type, data, count));
						count = 0;
						break;
					} else {
						toBeRemoved.add(new ItemInvPair(inv, mid, type, data, item.getAmount()));
						count -= item.getAmount();
					}
			}
		}

		// If count is not zero, we failed to find what we needed.
		if (count != 0) return false;

		// We now have a hashmap full of items to remove.
		// <Integer, ItemStack> leftovers = new HashMap<Integer, ItemStack>();
		int totalActuallyRemoved = 0;
		for (ItemInvPair invPair : toBeRemoved) {
			Inventory inv = invPair.inv;
			totalActuallyRemoved += removeItemFromInventory(inv, invPair.mid, invPair.type, invPair.data, invPair.amount, direct);
		}

		if (totalActuallyRemoved != amount) {
			throw new CivException("Inventory Error! We tried to remove " + amount + " items but could only remove " + totalActuallyRemoved);
		}

		return true;

	}

	public boolean removeItemStackSync(ItemStack item, Boolean direct) throws CivException {
		CustomMaterial loreMat = CustomMaterial.getCustomMaterial(item);
		if (loreMat != null) {
			return removeItem(loreMat.getId(), 0, (short) 0, item.getAmount(), direct);
		} else {
			/* Vanilla item. no custom id. */
			return removeItem(null, ItemManager.getTypeId(item), ItemManager.getData(item), item.getAmount(), direct);
		}
	}

	public boolean removeItem(int typeid, int amount, Boolean sync) throws CivException {
		return removeItem(null, typeid, (short) 0, amount, sync);
	}

	public boolean removeItemStackAsync(int typeid, int amount) throws CivException {
		return removeItem(null, typeid, (short) 0, amount, false);
	}

	public boolean contains(String mid, int type, short data, int amount) {
		int count = 0;
		for (Inventory inv : invs) {
			for (ItemStack item : inv.getContents()) {
				if (item == null) continue;
				if (mid != null) {
					CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(item);
					if (craftMat == null) continue;
					if (!craftMat.getConfigId().equals(mid)) continue;
				} else {
					/* Vanilla item. */
					if (ItemManager.getTypeId(item) != type) continue;

					/* Only check the data if this item doesnt use durability. */
					if (ItemManager.getMaterial(type).getMaxDurability() == 0) {
						if (ItemManager.getData(item) != data) continue;
					}
				}

				count += item.getAmount();
				if (count >= amount) break;
			}
		}

		if (count >= amount) return true;
		return false;
	}

	public ItemStack[] getContents() {
		int size = 0;
		for (Inventory inv : invs) {
			size += inv.getContents().length;
		}

		ItemStack[] array = new ItemStack[size];

		for (Inventory inv : invs) {
			array = ObjectArrays.concat(array, inv.getContents(), ItemStack.class);
		}
		return array;
	}

	public int getInventoryCount() {
		return this.invs.size();
	}

	public boolean isFool() {
		for (Inventory inv : invs)
			if (inv.firstEmpty() >= 0) return false;
		return true;
	}

	public int getMaxSize() {
		int size = 0;
		for (Inventory i : invs) {
			size = size + i.getSize();
		}
		return size;
	}

	public int hasEnough(SourceItem sourceItem, int neadFound) {
		// Проверка ести ли все предметы в сундуках в нужных количествах
		Integer count = neadFound * sourceItem.count;
		for (Inventory inv : this.invs) {
			// находим в каком слоте лежит нужный предмет
			for (int j = 0; j < inv.getSize(); j++) {
				ItemStack stack = inv.getItem(j);
				if (stack == null || stack.getType() == Material.AIR) continue;
				for (String umid : sourceItem.items) {
					if (ItemManager.isCorrectItemStack(stack, umid)) {
						int amount = stack.getAmount();
						if (stack.getType().getMaxDurability() > 0) { // если инструмент, то вернуть поломанным
							amount = stack.getType().getMaxDurability() - stack.getDurability();
						}
						count = count - amount;
						if (count <= 0) return neadFound;
					}
				}
			}
		}
		return neadFound - count / sourceItem.count;
	}

	public void deleteFoundItems(SourceItem sourceItem, int deleteCount) throws InterruptedException {
		// удалить все найденние foundElements
		int count = deleteCount * sourceItem.count;
		for (Inventory inv : this.invs) {
			// находим в каком слоте лежит нужный предмет
			for (int slot = 0; slot < inv.getSize(); slot++) {
				ItemStack stack = inv.getItem(slot);
				if (stack == null || stack.getType() == Material.AIR) continue;
				for (String umid : sourceItem.items)
					if (ItemManager.isCorrectItemStack(stack, umid)) {
						if (stack.getType().getMaxDurability() > 0) { // если инструмент, то вернуть поломанным
							int damage = Math.min(count, stack.getType().getMaxDurability() - stack.getDurability());
							short durability = (short) (stack.getDurability() + damage);
							if (durability >= stack.getType().getMaxDurability())
								inv.setItem(slot, new ItemStack(Material.AIR));
							else {
								stack.setDurability(durability);
								inv.setItem(slot, stack);
							}
							count = count - damage;
						} else {
							int damage = Math.min(count, stack.getAmount());
							if (stack.getAmount() == damage)
								inv.setItem(slot, new ItemStack(Material.AIR));
							else {
								stack.setAmount(stack.getAmount() - damage);
								inv.setItem(slot, stack);
							}
							count = count - damage;
						}
						if (count == 0) return;
					}
			}
		}
	}

	public static MultiInventory getMultiInventoryChestSync(List<ConstructChest> chests) {
		MultiInventory multiInv = new MultiInventory();
		try {
			for (ConstructChest c : chests) {
				multiInv.addInventory(getChestInventorySync(c.getCoord()));
			}
		} catch (CivException e) {
			e.printStackTrace();
			return null;
		}
		return multiInv;
	}

	private static Inventory getChestInventorySync(BlockCoord blockCoord) throws CivException {
		Chunk chunk = blockCoord.getChunkCoord().getChunk();
		if (!chunk.isLoaded() && !chunk.load()) throw new CivException("Couldn't load chunk at " + chunk.getX() + "," + chunk.getZ());

		Block b = blockCoord.getBlock();
		try {
			Chest chest = (Chest) b.getState();
			return chest.getBlockInventory();
		} catch (Exception e) {
			throw new CivException(e.getMessage());
		}
	}
}