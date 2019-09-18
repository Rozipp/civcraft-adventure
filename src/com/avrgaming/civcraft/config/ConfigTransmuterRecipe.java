package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTransmuterRecipe {

	public String id;
	public Integer delay = 0;
	public String resultChest;
	public LinkedList<SourceItem> sourceItems = new LinkedList<SourceItem>();
	public LinkedList<ResultItem> resultItems = new LinkedList<ResultItem>();
	private Integer allRate = 0;

	public static class SourceItem {
		public String item;
		public int count;
		public String chest;
	}
	public static class ResultItem {
		public String item;
		public int count;
		public int rate;
	}
	
	public int getAllRate() {
		if (allRate == 0) {
			for (ResultItem ri : resultItems)
				allRate = allRate + ri.rate;
		}
		return allRate;
	}

	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg, HashMap<String, ConfigTransmuterRecipe> transmuterRecipes) {
		transmuterRecipes.clear();
		List<Map<?, ?>> configTransmuter = cfg.getMapList("transmuter");
		for (Map<?, ?> b : configTransmuter) {
			ConfigTransmuterRecipe ctr = new ConfigTransmuterRecipe();
			Object obj;
			ctr.id = (String) b.get("id");
			ctr.delay = ((obj = b.get("delay")) == null) ? 0 : (Integer) obj;
			ctr.resultChest = (String) b.get("result_chest");
			List<Map<?, ?>> configSourceItems = (List<Map<?, ?>>) b.get("source_item");
			if (configSourceItems != null) {
				for (Map<?, ?> ingred : configSourceItems) {
					SourceItem sourceItem = new SourceItem();
					sourceItem.item = (String) ingred.get("item");
					sourceItem.count = ((obj = ingred.get("count")) == null) ? 1 : (Integer) obj;
					sourceItem.chest = (String) ingred.get("chest");
					ctr.sourceItems.add(sourceItem);
				}
			}
			List<Map<?, ?>> configResultItems = (List<Map<?, ?>>) b.get("result_item");
			if (configResultItems != null) {
				for (Map<?, ?> ingred : configResultItems) {
					ResultItem resultItem = new ResultItem();
					resultItem.item = (String) ingred.get("item");
					resultItem.count = ((obj = ingred.get("count")) == null) ? 1 : (Integer) obj;
					resultItem.rate = (Integer) ingred.get("rate");
					ctr.resultItems.add(resultItem);
				}
			}
			transmuterRecipes.put(ctr.id, ctr);
		}

		CivLog.info("Loaded " + transmuterRecipes.size() + " TransmuterItems");
	}
}
