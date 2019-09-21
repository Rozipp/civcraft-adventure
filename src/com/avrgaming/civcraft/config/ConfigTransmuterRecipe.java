package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ItemManager;

public class ConfigTransmuterRecipe {

	public String id;
	public Integer delay = 0;
	public String resultChest;
	public LinkedList<SourceItem> sourceItems = new LinkedList<SourceItem>();
	public LinkedList<ResultItem> resultItems = new LinkedList<ResultItem>();
	public ResultItem lastResultItems;
	private Integer allRate = 0;

	public static class SourceItem {
		public String[] item;
		public Integer count;
		public String chest;
	}
	public static class ResultItem {
		public String item;
		public Integer count;
		public Integer rate;
	}

	public int getAllRate() {
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
			try {
				if (configSourceItems != null) {
					for (Map<?, ?> ingred : configSourceItems) {
						SourceItem sourceItem = new SourceItem();
						String source_item = (String) ingred.get("item");
						if (source_item == null) throw new CivException("ConfigTransmuterRecipe sourceItem.item = null in recipe " + ctr.id);
						String[] source_item_split = source_item.split(",");
						for (String s : source_item_split)
							if (ItemManager.createItemStack(s, 1) == null) {
								throw new CivException("ConfigTransmuterRecipe can not create sourceItem " + sourceItem.item + " in recipe " + ctr.id);
							}
						sourceItem.item = source_item_split;
						sourceItem.count = ((obj = ingred.get("count")) == null) ? 1 : (Integer) obj;
						sourceItem.chest = (String) ingred.get("chest");
						ctr.sourceItems.add(sourceItem);
					}
				}
			} catch (CivException e) {
				CivLog.error(e.getMessage());
				continue;
			}
			List<Map<?, ?>> configResultItems = (List<Map<?, ?>>) b.get("result_item");
			int max_rate = 0;
			ResultItem max_rate_item = null;
			try {
				if (configResultItems != null) {
					for (Map<?, ?> ingred : configResultItems) {
						ResultItem resultItem = new ResultItem();
						resultItem.item = (String) ingred.get("item");
						if (ItemManager.createItemStack(resultItem.item, 1) == null) {
							throw new CivException("ConfigTransmuterRecipe can not create resultItem " + resultItem.item + " in recipe " + ctr.id);
						}
						resultItem.count = ((obj = ingred.get("count")) == null) ? 1 : (Integer) obj;
						resultItem.rate = (Integer) ingred.get("rate");
						if (max_rate < resultItem.rate) {
							max_rate = resultItem.rate;
							max_rate_item = resultItem;
						}
						ctr.resultItems.add(resultItem);
					}
				}
			} catch (CivException e) {
				CivLog.error(e.getMessage());
				continue;
			}
			// результат с самым большым шансом нужен для организации повышения шанса других предметов
			ctr.resultItems.remove(max_rate_item);
			ctr.lastResultItems = max_rate_item;
			int allRate = max_rate;
			for (ResultItem ri : ctr.resultItems)
				allRate = allRate + ri.rate;
			ctr.allRate = allRate;
			transmuterRecipes.put(ctr.id, ctr);
		}

		CivLog.info("Loaded " + transmuterRecipes.size() + " TransmuterItems");
	}
}
