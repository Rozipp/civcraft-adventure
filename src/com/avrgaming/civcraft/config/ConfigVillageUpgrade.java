package com.avrgaming.civcraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.village.Village;

public class ConfigVillageUpgrade {

	public String id;
	public String name;
	public double cost;
	public String annex;
	public List<String> transmuter_recipe;
	public int level;
	public String require_upgrade = null;

	public static HashMap<String, Integer> categories = new HashMap<String, Integer>();

	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigVillageUpgrade> upgrades) {
		upgrades.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("upgrades");
		for (Map<?, ?> level : culture_levels) {
			ConfigVillageUpgrade upgrade = new ConfigVillageUpgrade();

			upgrade.id = (String) level.get("id");
			upgrade.name = (String) level.get("name");
			upgrade.cost = (Double) level.get("cost");
			upgrade.annex = (String) level.get("annex");
			String temp = (String) level.get("transmuter_recipe");
			if (temp != null) {
				upgrade.transmuter_recipe = new ArrayList<String>();
				String[] split = temp.split(",");
				for (String s : split)
					upgrade.transmuter_recipe.add(s);
			}
			upgrade.level = (Integer) level.get("level");
			upgrade.require_upgrade = (String) level.get("require_upgrade");
			upgrades.put(upgrade.id, upgrade);
		}
		CivLog.info("Loaded " + upgrades.size() + " village upgrades.");
	}

	public boolean isAvailable(Village village) {
		if (village.hasUpgrade(this.id)) {
			return false;
		}

		if (this.require_upgrade == null || this.require_upgrade.equals("")) {
			return true;
		}

		if (village.hasUpgrade(this.require_upgrade)) {
			return true;
		}
		return false;
	}

	public void processAction(Village village) {

		if (this.annex == null) {
			CivLog.warning("No annex found for upgrade:" + this.id);
			return;
		}
		CivMessage.sendVillage(village, CivSettings.localize.localizedString("village_upgrade_" + annex + level));
//			default :
//				CivLog.warning(CivSettings.localize.localizedString("var_village_upgrade_unknown", annex, id));
	}

}
