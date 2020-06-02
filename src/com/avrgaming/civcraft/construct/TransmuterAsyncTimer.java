package com.avrgaming.civcraft.construct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTransmuterRecipe;
import com.avrgaming.civcraft.config.ConfigTransmuterRecipe.ResultItem;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.FoundElement;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class TransmuterAsyncTimer extends CivAsyncTask {

	static int MAXSTEP = 64;

	Construct construct;
	Transmuter transmuter;
	List<ConfigTransmuterRecipe> cTranRs = new ArrayList<>();
	HashMap<String, MultiInventory> multInvs;
	boolean firstRun = true;
	Boolean abort = false;

	public TransmuterAsyncTimer(Construct construct, Transmuter transmuter) {
		this.construct = construct;
		this.transmuter = transmuter;
	}

	private void foundInventory() {
		// Взять все сундуки
		multInvs = new HashMap<>();
		for (ConfigTransmuterRecipe cTranR : cTranRs) {
			MultiInventory sour = this.getMultiInventoryChest(this, cTranR.sourceItem.chest);
			if (sour == null || sour.getMaxSize() < 1) {
				multInvs = null;
				break;
			}
			multInvs.put(cTranR.sourceItem.chest, sour);

			MultiInventory dest = this.getMultiInventoryChest(this, cTranR.resultChest);
			if (dest == null || sour.getMaxSize() < 1) {
				multInvs = null;
				break;
			}
			multInvs.put(cTranR.resultChest, dest);
		}
		if (multInvs == null)
			CivLog.debug("multInvs == null");
		else
			CivLog.debug("multInvs " + multInvs.size());
	}

	public void addRecipe(String s) {
		ConfigTransmuterRecipe ctr = CivSettings.transmuterRecipes.get(s);
		if (ctr != null) this.cTranRs.add(ctr);
	}

	public void clearRecipe() {
		abort = true;
		this.cTranRs.clear();
		multInvs = null;
	}

	public void stop() {
		abort = true;
	}

	@Override
	public void run() {
		abort = false;
		CivLog.debug("Transmuter begin");
		foundInventory();
		if (multInvs == null) {
			CivLog.debug("Transmuter stoped multInvs == null");
			return;
		}
		while (true) {
			int delay = 1;
			synchronized (abort) {
				if (abort) {
					CivLog.debug("Transmuter stoped in begin");
					return;
				}
				ArrayList<FoundElement> foundElements = new ArrayList<>();
				for (ConfigTransmuterRecipe cTranR : cTranRs) {
					if (multInvs.get(cTranR.resultChest).isFool()) {
						CivLog.debug("cTranR.resultChest).isFool");
						continue;
					}

					if (hasEnoughToTransmute(cTranR, foundElements)) {
						try {
							int countDelete = deleteFoundItems(foundElements);
							int countStep = (countDelete - 1) / cTranR.sourceItem.count + 1;
							delay = countStep * cTranR.delay;
							CivLog.debug("delay = " + delay);
							Map<String, Integer> resultItems = getRandomResultItems(cTranR, countStep);
							putResultItems(multInvs.get(cTranR.resultChest), resultItems);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						break;
					}
				}
			}
			try {
				if (delay < 1) delay = 1;
				int i = 1;
				while (i <= delay) {
					Thread.sleep(1000);
					i++;
					if (abort) {
						CivLog.debug("Transmuter stoped in sleep");
						return;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean hasEnoughToTransmute(ConfigTransmuterRecipe cTranR, ArrayList<FoundElement> foundElements) {
		if (cTranR == null) return false;
		// Проверка ести ли все предметы в сундуках в нужных количествах
		MultiInventory sMInv = multInvs.get(cTranR.sourceItem.chest);
		if (sMInv == null || !sMInv.foundElement(cTranR.sourceItem, foundElements)) return false;
		return true;// Если найдены все предметы, значит можно менять их на новые
	}

	private Map<String, Integer> getRandomResultItems(ConfigTransmuterRecipe cTranR, int countStep) {
		Random rand = new Random();
		Map<String, Integer> items = new HashMap<>();

		/* определяем какой предмет нужно возвращать в зависимости от rate */
		for (int i = 0; i < countStep; i++) {
			double k = 0;
			double r = rand.nextDouble() * cTranR.totalRate;
			ResultItem chri = null;
			for (ResultItem ri : cTranR.resultItems) {
				k = k + ri.rate.doubleValue();// * transmuter.modifyChance;
				if (r < k) {
					chri = ri;
					break;
				}
			}
			if (chri == null) chri = cTranR.getOther(rand);

			int count = chri.count;
			if (items.containsKey(chri.item)) count = count + items.get(chri.item);
			items.put(chri.item, count);
		}
		return items;
	}

	private int deleteFoundItems(ArrayList<FoundElement> foundElements) throws InterruptedException {
		// удалить все найденние foundElements
		int count = 0;
		for (FoundElement fe : foundElements) {
			// если инструмент, то вернуть его крепкость
			if (fe.stack.getType().getMaxDurability() > 0) {
				ItemStack is;
				int damage = Math.min(MAXSTEP, fe.stack.getType().getMaxDurability() - fe.stack.getDurability());
				short durability = (short) (fe.stack.getDurability() + damage);
				if (durability >= fe.stack.getType().getMaxDurability())
					is = new ItemStack(Material.AIR);
				else {
					is = fe.stack.clone();
					is.setDurability(durability);
				}
				count = count + damage;
				this.updateInventory(Action.REPLACE, fe.sInv, is, fe.slot);
				break;
			}
			// // Если бутылка, то вернуть пустую
			// if (ItemManager.isCorrectItemStack(fe.stack, null, 373, (short) 0)) {
			// task.updateInventory(Action.REPLACE, fe.sInv, new ItemStack(Material.GLASS_BOTTLE, fe.count), fe.slot);
			// break;
			// }
			// в остальных случаях просто удалить предмет, и вернуть сколько предметов было удалено
			{
				count = count + fe.stack.getAmount();
				this.updateInventory(Action.REPLACE, fe.sInv, new ItemStack(Material.AIR), fe.slot);
			}
		}
		return count;
	}

	/** Заменить все предметы из входных сундуков на нужные предмети в выходном БЕЗ ПРОВЕРКИ НА ВОЗМОЖНОСТЬ ОПЕРАЦИИ */
	private void putResultItems(MultiInventory rMInv, Map<String, Integer> items) throws InterruptedException {
		// Добавить предмет в resultChest
		for (String ri : items.keySet()) {
			int count = items.get(ri);
			ItemStack is = ItemManager.createItemStack(ri, count);
			// int maxSize = is.getType().getMaxStackSize();
			// while (count > 0) {
			// is.setAmount(Math.min(count, maxSize));
			// count = count - is.getAmount();
			this.updateInventory(Action.ADD, rMInv, is.clone());
			// }

		}
	}

	private MultiInventory getMultiInventoryChest(CivAsyncTask task, String chest) {
		ArrayList<ConstructChest> chests = construct.getAllChestsById(chest);
		MultiInventory multiInv = new MultiInventory();
		try {
			for (ConstructChest c : chests) {
				task.syncLoadChunk(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getZ());
				// XXX Couldn't load chunk in 5000 milliseconds! Retrying.
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
