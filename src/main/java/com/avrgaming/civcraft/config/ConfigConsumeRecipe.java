package com.avrgaming.civcraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.TownPeoplesManager.Prof;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;

public class ConfigConsumeRecipe {

	public String id;
	public String source_chest;
	public StorageType storage_type;
	public Prof worker_prof;
	public Map<Integer, ConsumeLevel> levels;

	public static class ConsumeLevel {
		public int level;
		public List<SourceItem> sourceItems;
		public int point;
		public double storage_result;
	};

	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigConsumeRecipe> consumelevels) {
		consumelevels.clear();
		Object obj;
		List<Map<?, ?>> consumes_list = cfg.getMapList("consumes");
		for (Map<?, ?> cls : consumes_list) {
			ConfigConsumeRecipe configConsumelevel = new ConfigConsumeRecipe();
			configConsumelevel.id = (String) cls.get("id");
			configConsumelevel.source_chest = (String) cls.get("source_chest");
			configConsumelevel.storage_type = StorageType.valueOf(((String) cls.get("storage_type")).toUpperCase());
			configConsumelevel.worker_prof = Prof.valueOf(((String) cls.get("worker_prof")).toUpperCase());

			configConsumelevel.levels = new HashMap<>();
			List<Map<?, ?>> levels_list = (List<Map<?, ?>>) cls.get("levels");
			for (Map<?, ?> ls : levels_list) {
				ConsumeLevel consumelevel = new ConsumeLevel();
				consumelevel.level = (Integer) ls.get("level");
				List<Map<?, ?>> configSourceItems = (List<Map<?, ?>>) ls.get("source_items");
				if (configSourceItems != null) {
					consumelevel.sourceItems = new ArrayList<>();
					for (Map<?, ?> ingred : configSourceItems) {
						SourceItem sourceItem = new SourceItem();
						String source_item = (String) ingred.get("item");
						sourceItem.items = source_item.split(",");
						sourceItem.count = ((obj = ingred.get("count")) == null) ? 1 : (Integer) obj;
						sourceItem.chest = ((obj = ingred.get("chest")) == null) ? configConsumelevel.source_chest : (String) obj;
						consumelevel.sourceItems.add(sourceItem);
					}
				}
				consumelevel.point = (Integer) ls.get("point");
				consumelevel.storage_result = (Double) ls.get("storage_result");
				configConsumelevel.levels.put(consumelevel.level, consumelevel);
			}

			consumelevels.put(configConsumelevel.id, configConsumelevel);
		}
		CivLog.info("Loaded " + consumelevels.size() + " consumes.");
	}
}
