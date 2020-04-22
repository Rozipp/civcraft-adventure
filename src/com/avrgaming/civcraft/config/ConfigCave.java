package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.mythicmob.ConfigMobs;
import com.avrgaming.civcraft.object.Civilization;

public class ConfigCave implements Comparable<ConfigCave> {

	enum TypeCave {
		Wonder, Buff, Mob
	}

	public static class Treasure {
		String material;
		int count;
		int rate;
	}

	public String id;
	public String name;
	public String template_name;
	public String template_entrance;
	public String require_techs;
	public int updateTime = 360000;
	public TypeCave type;
	public String mobId;
	public HashMap<String, Treasure> treasures;
	public HashMap<String, ConfigBuff> buffs;
	public String hemiString = null;
	public int rarity = 0;

	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigCave> caves) {
		caves.clear();
		List<Map<?, ?>> cave_entranse = cfg.getMapList("cave_entranse");
		for (Map<?, ?> g : cave_entranse) {
			ConfigCave cave = new ConfigCave();
			Object obj;
			cave.id = (String) g.get("id");
			cave.name = (String) g.get("name");
			cave.template_name = (String) g.get("template");
			cave.template_entrance = (String) g.get("template_entrance");

			cave.require_techs = (String) g.get("require_techs");
			cave.mobId = loadMobs((String) g.get("mobs"));

			cave.treasures = loadTreasures(cfg.getMapList("treasure"));
			cave.buffs = loadBuffsString((String) g.get("buffs"));
			cave.hemiString = ((String) g.get("hemispheres"));
			cave.rarity = ((obj = g.get("rarity")) != null) ? ((Integer) obj) : 0;

			caves.put(cave.id, cave);
		}

		CivLog.info("Loaded " + caves.size() + " Caves.");
	}

	public static String loadMobs(String string) {
		ConfigMobs cMob = CivSettings.mobs.get(string.replace(" ", ""));
		if (cMob != null) {
			return cMob.uid;
		}
		return "lesser_yobo";
	}

	public static HashMap<String, Treasure> loadTreasures(List<Map<?, ?>> treasure) {
		HashMap<String, Treasure> treasures = new HashMap<String, ConfigCave.Treasure>();
		for (Map<?, ?> g : treasure) {
			Treasure tr = new Treasure();
			tr.material = (String) g.get("material");
			tr.count = (Integer) g.get("count");
			tr.rate = (Integer) g.get("rate");
		}
		return treasures;
	}

	public static HashMap<String, ConfigBuff> loadBuffsString(String bonus) {
		HashMap<String, ConfigBuff> buffs = new HashMap<String, ConfigBuff>();
		String[] keys = bonus.split(",");

		for (String key : keys) {
			ConfigBuff cBuff = CivSettings.buffs.get(key.replace(" ", ""));
			if (cBuff != null) {
				buffs.put(key, cBuff);
			}
		}
		return buffs;
	}

	@Override
	public int compareTo(ConfigCave otherGood) {
		if (this.rarity < otherGood.rarity) {
			// A lower rarity should go first.
			return 1;
		} else if (this.rarity == otherGood.rarity) {
			return 0;
		}
		return -1;
	}

	public boolean isAvailable(Civilization civ) {
		if (require_techs == null || require_techs.equals(""))
			return true;

		String[] requireTechs = require_techs.split(":");

		for (String reqTech : requireTechs) {
			if (!civ.hasTechnology(reqTech))
				return false;
		}
		return true;
	}

}
