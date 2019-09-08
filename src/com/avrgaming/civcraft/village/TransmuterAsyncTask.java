package com.avrgaming.civcraft.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ResultItem;
import com.avrgaming.civcraft.config.SourceItem;
import com.avrgaming.civcraft.config.TransmuterItem;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class TransmuterAsyncTask extends CivAsyncTask {

	Buildable buildable;
	TransmuterItem cTranI;

	public TransmuterAsyncTask(Buildable buildable, TransmuterItem cTranI) {
		this.buildable = buildable;
		this.cTranI = cTranI;
	}

	@Override
	public void run() {
		ReentrantLock reentrantLock = ((Village) buildable).locks.get(cTranI.id);
		if (reentrantLock.tryLock()) {
			try {
				HashMap<String, MultiInventory> multInv = new HashMap<>();
				if (hasEnoughToTransmute(this, cTranI, multInv)) processTransmute(this, cTranI, multInv);
			} finally {
				reentrantLock.unlock();
			}
		}
	}

	boolean hasEnoughToTransmute(CivAsyncTask task, TransmuterItem cTranI, HashMap<String, MultiInventory> multInv) {
		if (cTranI == null) return false;
		// Проверка ести ли все сундуки

		for (SourceItem si : cTranI.sourceItems) {
			MultiInventory mi = this.getMultiInventoryChest(task, si.chest);
			if (mi == null) return false;
			multInv.put(si.chest, mi);
		}

		MultiInventory dest = this.getMultiInventoryChest(task, cTranI.resultChest);
		if (dest == null) return false;
		multInv.put(cTranI.resultChest, dest);

		// Проверка ести ли все предметы в сундуках в нужных количествах
		for (SourceItem si : cTranI.sourceItems) {
			MultiInventory source = multInv.get(si.chest);
			boolean found = false;
			for (ItemStack stack : source.getContents()) {
				if (stack == null) continue;
				if (!ItemManager.isCorrectItemStack(stack, si.item)) continue;
				//проверка количества предметов.
				if (si.count > stack.getAmount()) continue;

				found = true;
				break; //предмет найден выходим из цикла поисика предмета
			}
			if (!found) {
				return false; //один из предметов не найден, остальные предметы не проверяем
			}
		}

		for (ItemStack stack : dest.getContents()) //проверка на полность сундука
			if (stack == null) return true; // если сюда дошли, значит можно менять предметы
		return false;
	}
	/** Заменить все предметы из входных сундуков на нужные предмети в выходном БЕЗ ПРОВЕРКИ НА ВОЗМОЖНОСТЬ ОПЕРАЦИИ */
	void processTransmute(CivAsyncTask task, TransmuterItem cTranI, HashMap<String, MultiInventory> multInv) {
		/* определяем какой предмет нужно возвращать в зависимости от rate */
		Random rand = new Random();
		int i = 0;
		int allRand = cTranI.getAllRate();
		double r = rand.nextDouble() * allRand;
		ResultItem resI = null;
		for (ResultItem ri : cTranI.resultItems) {
			i = i + ri.rate;
			if (r < i) {
				resI = ri;
				break;
			}
		}
		try {
			// удалить все sourceItems
			for (SourceItem si : cTranI.sourceItems) {
				MultiInventory sMInv = multInv.get(si.chest);
				ItemStack is = getItemStack(si.item, si.count);
				if (is.getType().getMaxDurability() > 0) {
					ItemStack stack = null;
					for (ItemStack st : sMInv.getContents()) {
						if (CustomMaterial.getMID(st).equalsIgnoreCase(si.item)) {
							stack = st;
							break;
						}
					}
					if (stack == null) return;
					int modifier = 1;//checkDigSpeed(stack);
					short damage = stack.getDurability();
					if (!task.updateInventory(Action.REMOVE, sMInv, stack)) return;
					damage += modifier;
					stack.setDurability(damage);
					if (damage < stack.getType().getMaxDurability() && stack.getAmount() == 1) {
						task.updateInventory(Action.ADD, sMInv, stack);
					}
				} else {
					task.updateInventory(Action.REMOVE, sMInv, is);
					if (si.item.contains("373")) task.updateInventory(Action.ADD, sMInv, getItemStack("374", si.count));
				}
			}
			//Добавить предмет в resultChest
			MultiInventory rMInv = multInv.get(cTranI.resultChest);
			task.updateInventory(Action.ADD, rMInv, getItemStack(resI.item, resI.count));
		} catch (InterruptedException e) {
			return;
		}
		return;
	}

	private MultiInventory getMultiInventoryChest(CivAsyncTask task, String chest) {
		ArrayList<StructureChest> chests = buildable.getAllChestsById(chest);
		MultiInventory multiInv = new MultiInventory();
		try {

			for (StructureChest c : chests) {
				task.syncLoadChunk(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getZ());
				Inventory tmp;
				try {
					tmp = task.getChestInventory(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getY(), c.getCoord().getZ(), false);
					multiInv.addInventory(tmp);
				} catch (CivTaskAbortException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		return multiInv;
	}

	ItemStack getItemStack(String ss, int amount) {
		if (ss.contains(":")) return ItemManager.createItemStack(ss, amount);
		if (ss.contains("_")) return CustomMaterial.spawn(CraftableCustomMaterial.getCraftableCustomMaterial(ss), amount);
		return ItemManager.createItemStack(Integer.parseInt(ss), amount);
	}
}
