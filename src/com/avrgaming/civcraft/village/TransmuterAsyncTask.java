package com.avrgaming.civcraft.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ConfigTransmuterRecipe;
import com.avrgaming.civcraft.config.ConfigTransmuterRecipe.ResultItem;
import com.avrgaming.civcraft.config.ConfigTransmuterRecipe.SourceItem;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.object.ConstructChest;
import com.avrgaming.civcraft.structure.Construct;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class TransmuterAsyncTask extends CivAsyncTask {

	Construct construct;
	ConfigTransmuterRecipe cTranR;

	public TransmuterAsyncTask(Construct construct, ConfigTransmuterRecipe cTranR) {
		this.construct = construct;
		this.cTranR = cTranR;
	}

	class FoundElement { // найденые в MultiInventory предметы
		Inventory sInv;
		Integer slot;
		ItemStack stack;
		Integer count;
		public FoundElement(Inventory sInv, Integer slot, ItemStack stack, Integer count) {
			this.sInv = sInv;
			this.slot = slot;
			this.stack = stack;
			this.count = count;
		}
	}

	@Override
	public void run() {
		ReentrantLock lock = construct.transmuterLocks.get(cTranR.id);
		if (lock.tryLock()) {
			try {
				HashMap<String, MultiInventory> multInv = new HashMap<>();
				ArrayList<FoundElement> foundElements = new ArrayList<>();

				if (hasEnoughToTransmute(this, cTranR, multInv, foundElements)) // проверка возможности операции
					processTransmute(this, cTranR, multInv, foundElements); //выполнение операции

				//уснуть на указанное в ConfigTransmuterRecipe.delay количество секунд
				if (cTranR.delay != 0) Thread.sleep(1000 * cTranR.delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}
	}

	boolean hasEnoughToTransmute(CivAsyncTask task, ConfigTransmuterRecipe cTranI, HashMap<String, MultiInventory> multInv,
			ArrayList<FoundElement> foundElements) {
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

		boolean fool = true;
		for (ItemStack stack : dest.getContents()) //проверка на полность resultChest
			if (stack == null) {
				fool = false;
				break;
			}
		if (fool) return false;

		// Проверка ести ли все предметы в сундуках в нужных количествах
		for (SourceItem si : cTranI.sourceItems) {
			MultiInventory sMInv = multInv.get(si.chest);
			ItemStack foundStack = null;
			Integer foundSlot = -1;
			for (Inventory sInv : sMInv.invs) {
				foundStack = null;
				foundSlot = -1;
				// находим в каком слоте лежит нужный предмет
				for (int j = 0; j < sInv.getSize(); j++) {
					ItemStack st = sInv.getItem(j);
					if (st == null) continue;
					for (String s : si.item)
						if (ItemManager.isCorrectItemStack(st, s) && st.getAmount() >= si.count) {
							foundStack = st;
							foundSlot = j;
							break;
						}
					if (foundStack != null) break; // Если предмет найден, то другие альтернативы не проверяем
				}
				// если не наши в этом инвентаре, то ищем в следующем
				if (foundStack == null || foundSlot == -1) continue;

				foundElements.add(new FoundElement(sInv, foundSlot, foundStack, si.count));
				break;
			}
		}
		return foundElements.size() == cTranI.sourceItems.size();// Если найдены все предметы, значит можно менять их на новые
	}
	/** Заменить все предметы из входных сундуков на нужные предмети в выходном БЕЗ ПРОВЕРКИ НА ВОЗМОЖНОСТЬ ОПЕРАЦИИ */
	void processTransmute(CivAsyncTask task, ConfigTransmuterRecipe cTranI, HashMap<String, MultiInventory> multInv, ArrayList<FoundElement> foundElements) {
		Random rand = new Random();
		/* определяем какой предмет нужно возвращать в зависимости от rate */
		double i = 0;
		double r = rand.nextDouble() * cTranI.getAllRate();
		ResultItem resI = null;
		for (ResultItem ri : cTranI.resultItems) {
			i = i + construct.modifyTransmuterChance(ri.rate.doubleValue());
			if (r < i) {
				resI = ri;
				break;
			}
		}
		if (resI == null) resI = cTranI.lastResultItems;

		try {
			// удалить все найденние sourceItems
			for (FoundElement fe : foundElements) {
				// если инструмент, то вернуть чуть поломанный
				if (fe.stack.getType().getMaxDurability() > 0) { 
					int damage = fe.stack.getDurability() + fe.count;
					if (damage >= fe.stack.getType().getMaxDurability()) { //если урон большой, то удаляем инструмент
						if (fe.stack.getAmount() == 1)
							task.updateInventory(Action.REPLACE, fe.sInv, new ItemStack(Material.AIR), fe.slot);
						else {
							fe.stack.setAmount(fe.stack.getAmount() - 1);
							task.updateInventory(Action.REPLACE, fe.sInv, fe.stack, fe.slot);
						}
					} else {
						fe.stack.setDurability((short) damage);
						task.updateInventory(Action.REPLACE, fe.sInv, fe.stack, fe.slot);
					}
					break;
				}
				//Если бутылка, то вернуть пустую
				if (ItemManager.isCorrectItemStack(fe.stack, null, 373, (short) 0)) {
					task.updateInventory(Action.REPLACE, fe.sInv, new ItemStack(Material.GLASS_BOTTLE, fe.count), fe.slot);
					break;
				}
				// в остальных случаях просто удалить предмет
				fe.stack.setAmount(fe.stack.getAmount() - fe.count);
				task.updateInventory(Action.REPLACE, fe.sInv, fe.stack, fe.slot);
				break;
			}
			//Добавить предмет в resultChest
			MultiInventory rMInv = multInv.get(cTranI.resultChest);
			task.updateInventory(Action.ADD, rMInv, ItemManager.createItemStack(resI.item, resI.count));
		} catch (InterruptedException e) {
			return;
		}
		return;
	}

	private MultiInventory getMultiInventoryChest(CivAsyncTask task, String chest) {
		ArrayList<ConstructChest> chests = construct.getAllChestsById(chest);
		MultiInventory multiInv = new MultiInventory();
		try {

			for (ConstructChest c : chests) {
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
}
