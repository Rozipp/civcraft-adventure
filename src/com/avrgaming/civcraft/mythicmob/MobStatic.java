package com.avrgaming.civcraft.mythicmob;

import static com.avrgaming.civcraft.main.CivCraft.civRandom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.avrgaming.civcraft.main.CivLog;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.exceptions.InvalidMobTypeException;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;

public class MobStatic {

	public static HashMap<String, LinkedList<ConfigMobs>> biomes = new HashMap<>();
	public static HashSet<EntityType> disableMobs = new HashSet<>();

	static class MobDrop {
		String uid;
		double chance;
		public MobDrop(String uid, double chance) {
			this.uid = uid;
			this.chance = chance;
		}
	}

	public static boolean isMithicMobEntity(LivingEntity e) {
		return MythicMobs.inst().getAPIHelper().isMythicMob(e);
	}
	public static boolean isMithicMobEntity(Entity e) {
		return isMithicMobEntity((LivingEntity) e);
	}

	public static ActiveMob getMithicMob(Entity entity) {
		return MythicMobs.inst().getAPIHelper().getMythicMobInstance(entity);
	}
	
	
	public static LinkedList<ConfigMobs> getValidMobsForBiome(Biome biome) {
		LinkedList<ConfigMobs> mobs = biomes.get(biome.name());
		if (mobs == null) mobs = new LinkedList<ConfigMobs>();
		return mobs;
	}

	public static void despawnAll() {
		MythicMobs.inst().getMobManager().removeAllAllMobs();
	}

	public static void spawnRandomCustomMob(Location location) {
		LinkedList<ConfigMobs> validMobs = getValidMobsForBiome(location.getBlock().getBiome());
		if (validMobs.isEmpty()) return;

		String mmid = validMobs.get(civRandom.nextInt(validMobs.size())).uid;
		try {
			MythicMobs.inst().getAPIHelper().spawnMythicMob(mmid, location);
		} catch (InvalidMobTypeException e) {
			CivLog.error("MythicMobs can not spawn mobType " + mmid);
		}
	}

	public static void loadConfigBiome(FileConfiguration cfg, Map<String, ConfigMobs> globalMobs) {
		biomes.clear();
		List<Map<?, ?>> bi = cfg.getMapList("biomes");
		for (Map<?, ?> b : bi) {
			String id = (String) b.get("id");
			Biome biome = Biome.valueOf(id.toUpperCase());
			if (biome == null) {
				CivLog.error("Not foud biome \"" + id + "\"");
				continue;
			}
			LinkedList<ConfigMobs> mobs = new LinkedList<ConfigMobs>();
			List<?> ms = (List<?>) b.get("mobs");
			if (ms != null) {
				for (Object obj : ms) {
					if (obj instanceof String) {
						ConfigMobs cm = globalMobs.get((String) obj);
						if (cm != null) mobs.add(cm);
					}
				}
			}
			biomes.put(id, mobs);
		}
	}

	public static void loadConfigDrops(FileConfiguration cfg, Map<String, ConfigMobs> mobs) {
		List<Map<?, ?>> drops = cfg.getMapList("drops");
		for (Map<?, ?> d : drops) {
			String id = ((String) d.get("id")).toLowerCase();
			ConfigMobs mob = mobs.get(id);
			if (mob == null) {
				CivLog.error("?????? ???????????? ?????????? ???? ???????????? ?????? " + id);
				continue;
			}
			List<?> dropString = (List<?>) d.get("drop");
			if (dropString != null) {
				for (Object obj : dropString) {
					String[] dropSplit = ((String) obj).split(" ");
					mob.drops.add(new MobDrop(dropSplit[0], Double.valueOf(dropSplit[1])));
				}
			}
			mob.drops.add(new MobDrop(Material.BONE.name(), 0.1));
			mob.drops.add(new MobDrop(Material.SUGAR.name(), 0.1));
			mob.drops.add(new MobDrop(Material.SULPHUR.name(), 0.25));
			mob.drops.add(new MobDrop(Material.POTATO_ITEM.name(), 0.1));
			mob.drops.add(new MobDrop(Material.CARROT_ITEM.name(), 0.1));
			mob.drops.add(new MobDrop(Material.COAL.name(), 0.1));
			mob.drops.add(new MobDrop(Material.STRING.name(), 0.1));
			mob.drops.add(new MobDrop(Material.SLIME_BALL.name(), 0.02));
			mobs.put(mob.uid, mob);
		}
	}

	public static void loadDisableMobs(FileConfiguration cfg) {
		List<?> disable = (List<?>) cfg.get("disable");
		if (disable != null) {
			for (Object obj : disable) {
				String type = ((String) obj).toUpperCase();
				EntityType et = EntityType.valueOf(type);
				if (et != null) disableMobs.add(et);
			}
		}
	}
}