package com.avrgaming.civcraft.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTransmuterRecipe;
import com.avrgaming.civcraft.config.ResultItem;
import com.avrgaming.civcraft.config.SourceItem;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class TransmuterComponent extends Component {
	private static int MAXSTACKSIZE = 64;
	private static HashMap<String, HashMap<Integer, List<ConfigTransmuterRecipe>>> cash = new HashMap<>();

	private int level = 0;
	private HashMap<Integer, List<ConfigTransmuterRecipe>> levelRecipes = new HashMap<>();
	private Long sleepTo = 0L;
	Double modifyChance = 1.0;
	// public HashMap<String, MultiInventory> multInvs = null;

	@Override
	public void createComponent(Construct constr, boolean async) {
		super.createComponent(constr, async);
		String recipes = this.getString("recipes");
		if (cash.containsKey(recipes)) {
			levelRecipes = cash.get(recipes);
			return;
		}
		try {
			for (String rl : recipes.split(" ")) {
				String[] rlsplit = rl.split(":");
				Integer level = 0;
				String trRs = rlsplit[0];
				if (rlsplit.length > 1) {
					level = Integer.parseInt(rlsplit[0]);
					trRs = rlsplit[1];
				}
				List<ConfigTransmuterRecipe> list = new ArrayList<>();
				for (String trR : trRs.split(",")) {
					ConfigTransmuterRecipe ctr = CivSettings.transmuterRecipes.get(trR);
					list.add(ctr);
				}
				levelRecipes.put(level, list);
			}
			cash.put(recipes, levelRecipes);
		} catch (Exception e) {
			CivLog.error("Create TransmuterComponent error. Recipes string = '" + recipes + "'");
			e.printStackTrace();
		}
	}

	public enum Result {
		STAGNATE, WORK, SLEEP, UNKNOWN
	}

	public void setModifyChance(double modifyChance) {
		this.modifyChance = modifyChance;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public List<ConfigTransmuterRecipe> getRecipes() {
		if (!levelRecipes.containsKey(level))
			return new ArrayList<>();
		else
			return levelRecipes.get(level);
	}

	private void sleep(Integer delay) {
		sleepTo = System.currentTimeMillis() + delay * 1000 - 100;
	}

	private boolean isSleep() {
		return System.currentTimeMillis() < sleepTo;
	}

	public Result processConsumption() {
		if (isSleep()) return Result.SLEEP;

		List<ConfigTransmuterRecipe> cTranRs = getRecipes();
		if (cTranRs.isEmpty()) return Result.UNKNOWN;

		HashMap<String, MultiInventory> multInvs = foundInventory();
		if (multInvs == null) return Result.UNKNOWN;

		for (ConfigTransmuterRecipe cTranR : cTranRs) {
			if (multInvs.get(cTranR.resultChest).isFool()) continue;

			int found = MAXSTACKSIZE;
			for (SourceItem si : cTranR.sourceItems) {
				int foundNow = multInvs.get(si.chest).hasEnough(si, found);
				if (foundNow < found) found = foundNow;
			}
			if (found > 0) {
				try {
					for (SourceItem si : cTranR.sourceItems) {
						multInvs.get(si.chest).deleteFoundItems(si, found);
					}
					List<ItemStack> resultItems = getRandomResultItems(cTranR, found);
					multInvs.get(cTranR.resultChest).addItems(resultItems, true);
					sleep(found * cTranR.delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return Result.WORK;
			}
		}
		return Result.STAGNATE;
	}

	public HashMap<String, MultiInventory> foundInventory() {
		// Взять все сундуки
		HashMap<String, MultiInventory> multInvs = new HashMap<>();
		for (ConfigTransmuterRecipe cTranR : getRecipes()) {
			for (SourceItem si : cTranR.sourceItems) {
				MultiInventory sour = MultiInventory.getMultiInventoryChestSync(getConstruct().getAllChestsById(si.chest));
				if (sour == null || sour.getMaxSize() < 1) return null;
				multInvs.put(si.chest, sour);
			}
			MultiInventory dest = MultiInventory.getMultiInventoryChestSync(getConstruct().getAllChestsById(cTranR.resultChest));
			if (dest == null || dest.getMaxSize() < 1) return null;
			multInvs.put(cTranR.resultChest, dest);
		}
		return multInvs;
	}

	private List<ItemStack> getRandomResultItems(ConfigTransmuterRecipe cTranR, int countStep) {
		Random rand = new Random();
		Map<String, Integer> items = new HashMap<>();

		/* определяем какой предмет нужно возвращать в зависимости от rate */
		for (int i = 0; i < countStep; i++) {
			double k = 0;
			double r = rand.nextDouble() * cTranR.totalRate;
			ResultItem chri = null;
			for (ResultItem ri : cTranR.resultItems) {
				k = k + ri.rate.doubleValue() * modifyChance;
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
		
		List<ItemStack> result = new ArrayList<>();
		for (String umid : items.keySet()) {
			result.add(ItemManager.createItemStack(umid, items.get(umid)));
		}
		return result;
	}

}
