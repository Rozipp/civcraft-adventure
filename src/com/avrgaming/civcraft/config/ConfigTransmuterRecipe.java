package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTransmuterRecipe {

	public String id;
	public Integer delay = 1;
	public String resultChest;
	public LinkedList<SourceItem> sourceItems = new LinkedList<>();
	public Integer totalSourceItems = 0;
	public LinkedList<ResultItem> resultItems = new LinkedList<>();
	public LinkedList<ResultItem> resultOther = new LinkedList<>();
	public Integer totalRate = 0;

	public ResultItem getOther(Random rand) {
		return resultOther.get(rand.nextInt(resultOther.size()));
	}

	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg, HashMap<String, ConfigTransmuterRecipe> transmuterRecipes) {
		transmuterRecipes.clear();
		List<Map<?, ?>> configTransmuter = cfg.getMapList("transmuter");
		for (Map<?, ?> b : configTransmuter) {
			ConfigTransmuterRecipe ctr = new ConfigTransmuterRecipe();
			Object obj;
			ctr.id = (String) b.get("id");
			ctr.delay = ((obj = b.get("delay")) == null) ? 1 : (Integer) obj;
			if (ctr.delay < 1) ctr.delay = 1;
			ctr.resultChest = (String) b.get("result_chest");
			ctr.totalRate = (Integer) b.get("total_rate");

			List<Map<?, ?>> configSourceItems = (List<Map<?, ?>>) b.get("source_item");
			if (configSourceItems != null) {
				for (Map<?, ?> ingred : configSourceItems) {
					SourceItem sourceItem = new SourceItem();
					String source_item = (String) ingred.get("item");
					sourceItem.items = source_item.split(",");
					sourceItem.count = ((obj = ingred.get("count")) == null) ? 1 : (Integer) obj;
					ctr.totalSourceItems += sourceItem.count;
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

			String result_other_string = (String) b.get("result_other");

			for (String ss : result_other_string.split(",")) {
				ResultItem resultItem = new ResultItem();
				resultItem.item = ss.trim();
				resultItem.count = 1;
				resultItem.rate = 0;
				ctr.resultOther.add(resultItem);
			}

			transmuterRecipes.put(ctr.id, ctr);
		}

		CivLog.info("Loaded " + transmuterRecipes.size() + " TransmuterRecipe");
	}
}
