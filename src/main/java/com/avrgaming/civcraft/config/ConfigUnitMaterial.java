package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.CivColor;

public class ConfigUnitMaterial extends ConfigMaterial {

	/* Optional */
	public int slot = 1;

	public static void loadConfigUnit(FileConfiguration cfg, Map<String, ConfigUnitMaterial> materials) {
		ConfigMission.loadConfig(cfg, CivSettings.missions);
		materials.clear();
		List<Map<?, ?>> configMaterials = cfg.getMapList("materials");
		for (Map<?, ?> b : configMaterials) {
			ConfigUnitMaterial mat = loadOneUnitConfig(b);
			materials.put(mat.id, mat);
		}
		CivLog.info("Loaded " + materials.size() + " Unit Materials.");
	}

	@SuppressWarnings("unchecked")
	public static ConfigUnitMaterial loadOneUnitConfig(Map<?, ?> b) {
		ConfigUnitMaterial mat = new ConfigUnitMaterial();

		/* Mandatory Settings */
		mat.id = (String) b.get("id");
		String[] itemSplit = ((String) b.get("item_id")).split(":");
		mat.item_id = Integer.valueOf(itemSplit[0]);
		mat.item_data = Integer.valueOf(itemSplit[1]);
		mat.name = (String) b.get("name");
		mat.name = CivColor.colorize(mat.name);

		List<?> configLore = (List<?>) b.get("lore");
		if (configLore != null) {
			String[] lore = new String[configLore.size()];
			int i = 0;
			for (Object obj : configLore) {
				if (obj instanceof String) {
					lore[i] = (String) obj;
					i++;
				}
			}
		}

		Boolean isShiny = (Boolean) b.get("shiny");
		if (isShiny != null) mat.shiny = isShiny;

		Double tValue = (Double) b.get("tradeValue");
		if (tValue != null) {
			mat.tradeable = true;
			mat.tradeValue = tValue;
		} else
			mat.tradeable = false;

		List<Map<?, ?>> comps = (List<Map<?, ?>>) b.get("components");
		if (comps != null) {
			for (Map<?, ?> compObj : comps) {
				HashMap<String, String> compMap = new HashMap<String, String>();
				for (Object key : compObj.keySet())
					compMap.put((String) key, (String) compObj.get(key));
				mat.components.add(compMap);
			}
		}

		mat.slot = (Integer) b.get("slot");
		
		return mat;
	}

}
