package com.avrgaming.civcraft.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTransmuterRecipe {

	public ReentrantLock lock = new ReentrantLock();
	public String id;
	public Integer delay = 0;
	public String resultChest;
	public LinkedList<SourceItem> sourceItems;
	public LinkedList<ResultItem> resultItems;

	public ConfigTransmuterRecipe() {
		sourceItems = new LinkedList<SourceItem>();
		resultItems = new LinkedList<ResultItem>();
	}

	public ConfigTransmuterRecipe(String id, String resultChest) {
		this.id = id;
		this.resultChest = resultChest;
		sourceItems = new LinkedList<SourceItem>();
		resultItems = new LinkedList<ResultItem>();
	}

	public void addSourceItem(String item, int amount, String chest) {
		SourceItem si = new SourceItem();
		si.item = item;
		si.count = amount;
		si.chest = chest;
		sourceItems.add(si);
	}

	public void addResultItem(String resultItem, int resultAmount, int resultRate) {
		ResultItem ri = new ResultItem();
		ri.item = resultItem;
		ri.count = resultAmount;
		ri.rate = resultRate;
		resultItems.add(ri);
	}

	public Collection<SourceItem> getSourceItems() {
		return sourceItems;
	}

	public Collection<ResultItem> getResultItems() {
		return resultItems;
	}

	public int getAllRate() {
		int sum = 0;
		for (ResultItem ri : resultItems)
			sum = sum + ri.rate;
		return sum;
	}

	public static ConfigTransmuterRecipe getTransmuterRecipeForId(String id) {
		return CivSettings.transmuterRecipes.get(id);
	}

	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg, HashMap<String, ConfigTransmuterRecipe> transmuterRecipes) {
		transmuterRecipes.clear();
		List<Map<?, ?>> configTransmuter = cfg.getMapList("transmuter");
		for (Map<?, ?> b : configTransmuter) {
			ConfigTransmuterRecipe ctr = new ConfigTransmuterRecipe();
			Object obj;
			ctr.id = (String) b.get("id");
			ctr.delay = ((obj = b.get("delay")) == null) ? 0 :(Integer) obj;
			ctr.resultChest = (String) b.get("result_chest");
			List<Map<?, ?>> configSourceItems = (List<Map<?, ?>>) b.get("source_item");
			if (configSourceItems != null) {
				for (Map<?, ?> ingred : configSourceItems) {
					SourceItem sourceItem = new SourceItem();
					sourceItem.item = (String) ingred.get("item");
					sourceItem.count = ((obj = ingred.get("count")) == null) ? 0 : (Integer) obj;
					sourceItem.chest = (String) ingred.get("chest");
					ctr.sourceItems.add(sourceItem);
				}
			}
			List<Map<?, ?>> configResultItems = (List<Map<?, ?>>) b.get("result_item");
			if (configResultItems != null) {
				for (Map<?, ?> ingred : configResultItems) {
					ResultItem resultItem = new ResultItem();
					resultItem.item = (String) ingred.get("item");
					resultItem.count = ((obj = ingred.get("count")) == null) ? 0 : (Integer) obj;
					resultItem.rate = (Integer) ingred.get("rate");
					ctr.resultItems.add(resultItem);
				}
			}
			transmuterRecipes.put(ctr.id, ctr);
		}

		CivLog.info("Loaded " + transmuterRecipes.size() + " TransmuterItems");
	}
}
