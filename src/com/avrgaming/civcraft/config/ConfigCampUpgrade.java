package com.avrgaming.civcraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;

public class ConfigCampUpgrade {

	public String id;
	public String name;
	public double cost;
	public String annex;
	public List<String> transmuter_recipe;
	public int level;
	public String require_upgrade = null;

	public static HashMap<String, Integer> categories = new HashMap<String, Integer>();

	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigCampUpgrade> upgrades) {
		upgrades.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("upgrades");
		for (Map<?, ?> level : culture_levels) {
			ConfigCampUpgrade upgrade = new ConfigCampUpgrade();

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
		CivLog.info("Loaded " + upgrades.size() + " camp upgrades.");
	}

	public boolean isAvailable(Camp camp) {
		if (camp.hasUpgrade(this.id)) {
			return false;
		}

		if (this.require_upgrade == null || this.require_upgrade.equals("")) {
			return true;
		}

		if (camp.hasUpgrade(this.require_upgrade)) {
			return true;
		}
		return false;
	}

	public void processAction(Camp camp) {

		if (this.annex == null) {
			CivLog.warning("No annex found for upgrade:" + this.id);
			return;
		}
		CivMessage.sendCamp(camp, CivSettings.localize.localizedString("camp_upgrade_" + annex + level));
//			default :
//				CivLog.warning(CivSettings.localize.localizedString("var_camp_upgrade_unknown", annex, id));
	}

}
