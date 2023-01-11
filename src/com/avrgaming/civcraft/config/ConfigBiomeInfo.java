package com.avrgaming.civcraft.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigBiomeInfo {

	enum Surface {
		hills(1, 1), mountainous(0, 2), oceanic(1, 0), plains(2, 0), swampland(1, 0), none(0, 0);

		private final int food;
		private final int hammer;

		Surface(int f, int h) {
			this.food = f;
			this.hammer = h;
		}
	}

	enum Humidity {
		forests(0, 3), irrigation(4, 0), none(0, 0);

		private final int food;
		private final int hammer;

		Humidity(int f, int h) {
			this.food = f;
			this.hammer = h;
		}
	}

	enum Climate {
		frozen(-1, 0), jungles(0, 2), desert(-1, -1), mesa(0, 1), savanna(1, 1), normal(0, 0);

		private final int food;
		private final int hammer;

		Climate(int f, int h) {
			this.food = f;
			this.hammer = h;
		}
	}

	public Integer id;
	public Biome biome;
	public Surface surface;
	public Humidity humidity;
	public Climate climate;
	public boolean beauty;
	public String mobs;

	public static void loadConfig(FileConfiguration cfg, EnumMap<Biome, ConfigBiomeInfo> culture_biomes) {
		culture_biomes.clear();
		List<Map<?, ?>> list = cfg.getMapList("culture_biomes");
		for (Map<?, ?> cl : list) {

			ConfigBiomeInfo biome = new ConfigBiomeInfo();
			biome.biome = Biome.valueOf((String) cl.get("name"));
			biome.surface = Surface.valueOf((String) cl.get("surface"));
			biome.humidity = Humidity.valueOf((String) cl.get("humidity"));
			biome.climate = Climate.valueOf((String) cl.get("climate"));
			String beauty = (String) cl.get("beauty");
			biome.beauty = beauty.equals("beauty");
			biome.mobs = (String) cl.get("mobs");

			culture_biomes.put(biome.biome, biome);
		}
		CivLog.info("Loaded " + culture_biomes.size() + " Culture Biomes.");
	}

	public double getHammers() {
		int hammers = surface.hammer + humidity.hammer + climate.hammer;
		return Math.max(hammers, 0);
	}

	public double getGrowth() {
		int food = surface.food + humidity.food + climate.food;
		return Math.max(food, 0);
	}

}
