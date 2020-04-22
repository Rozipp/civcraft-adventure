package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigConsumeLevel {
	public int level; /* Current level number */
	public Map<Integer, Integer> consumes; /* A map of block ID's and amounts required for this level to progress */
	public int count; /* Number of times that consumes must be met to level up */
	public double culture; /* Culture generated each time for the temple */
	public double hammers; /* hammers generated each time hour */
	public double coins; /* Coins generated each time for the cottage */

	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigConsumeLevel> consumelevels, String subString) {
		consumelevels.clear();
		Object obj;
		List<Map<?, ?>> cons_list = cfg.getMapList(subString + "_levels");
		for (Map<?, ?> cl : cons_list) {
			ConfigConsumeLevel consumelevel = new ConfigConsumeLevel();
			consumelevel.level = (Integer) cl.get("level");
			
			Map<Integer, Integer> consumes_list = null;
			List<?> consumes = (List<?>) cl.get("consumes");
			if (consumes != null) {
				consumes_list = new HashMap<Integer, Integer>();
				for (int i = 0; i < consumes.size(); i++) {
					String line = ((obj = consumes.get(i)) != null) ? (String) obj : "";
					String[] split = line.split(",");
					consumes_list.put(Integer.valueOf(split[0]), Integer.valueOf(split[1]));
				}
			}
			consumelevel.consumes = consumes_list;
			consumelevel.count = (Integer) cl.get("count");
			consumelevel.culture = ((obj = cl.get("culture")) != null) ? (Double) obj : 0.0;
			consumelevel.hammers = ((obj = cl.get("hammers")) != null) ? (Double) obj : 0.0;
			consumelevel.coins = ((obj = cl.get("coins")) != null) ? (Double) obj : 0.0;

			consumelevels.put(consumelevel.level, consumelevel);

		}
		CivLog.info("Loaded " + consumelevels.size() + " " + subString + " levels.");
	}
}
