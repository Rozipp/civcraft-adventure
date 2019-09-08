package com.avrgaming.civcraft.mythicmob;

import static com.avrgaming.civcraft.main.CivCraft.civRandom;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.mythicmob.MobStatic.MobDrop;
import com.avrgaming.civcraft.util.ItemManager;

public class ConfigMobs {

	public String uid;
	public String name;
	public LinkedList<MobDrop> drops;

	public ConfigMobs (String uid, String name) {
		this.uid = uid;
		this.name = name;
		drops = new LinkedList<MobDrop>();
	}
	
	public LinkedList<MobDrop> getRandomDrops() {
		Random rand = civRandom;
		LinkedList<MobDrop> dropped = new LinkedList<MobDrop>();
		for (MobDrop d : drops) {
			int chance = rand.nextInt(1000);
			if (chance < (d.chance * 1000)) dropped.add(d);
		}
		return dropped;
	}

	public List<ItemStack> getItemsDrop(List<ItemStack> itemsDrop) {
		itemsDrop.clear();
		LinkedList<MobDrop> dropped = getRandomDrops();
		for (MobDrop d : dropped) {
			ItemStack stack = ItemManager.createItemStack(d.uid, 1);
			itemsDrop.add(stack);
		}
		return itemsDrop;
	}

	static class MobLevel {
		String id;
		String name;
		public MobLevel(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	static class MobType {
		String id;
		String name;
		public MobType(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigMobs> mobs) {
		HashMap<String, MobLevel> mobLevels = new HashMap<>();
		HashMap<String, MobType> mobTypes = new HashMap<>();

		List<Map<?, ?>> levels = cfg.getMapList("levels");
		for (Map<?, ?> m : levels) {
			MobLevel ml = new MobLevel(((String) m.get("id")).toLowerCase(), (String) m.get("name"));
			mobLevels.put(ml.id, ml);
		}
		List<Map<?, ?>> type = cfg.getMapList("mobs");
		for (Map<?, ?> m : type) {
			MobType mt = new MobType(((String) m.get("id")).toLowerCase(), (String) m.get("name"));
			mobTypes.put(mt.id, mt);
		}

		mobs.clear();
		for (MobLevel ml : mobLevels.values()) {
			for (MobType mt : mobTypes.values()) {
				ConfigMobs cm = new ConfigMobs(ml.id + "_" + mt.id, ml.name + " " + mt.name);
				mobs.put(cm.uid, cm);
			}
		}
		
		MobStatic.loadConfigDrops(cfg, mobs);
		MobStatic.loadConfigBiome(cfg, mobs);
		MobStatic.loadDisableMobs(cfg);
		CivLog.info("Loaded " + mobs.size() + " mobs setings.");
	}
}