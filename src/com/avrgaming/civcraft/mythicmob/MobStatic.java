package com.avrgaming.civcraft.mythicmob;

import static com.avrgaming.civcraft.main.CivCraft.civRandom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.TimeTools;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.bukkit.BukkitAPIHelper;
import io.lumine.xikage.mythicmobs.api.exceptions.InvalidMobTypeException;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;

public class MobStatic {

	public static HashMap<String, LinkedList<ConfigMobs>> biomes = new HashMap<>();
	public static HashSet<EntityType> disableMobs = new HashSet<>();
	public static HashSet<String> disableCustomMobs = new HashSet<>();
	private static MobAsynckSpawnTimer task;

	static class MobDrop {
		String uid;
		double chance;

		public MobDrop(String uid, double chance) {
			this.uid = uid;
			this.chance = chance;
		}
	}

	public static BukkitAPIHelper API() {
		return MythicMobs.inst().getAPIHelper();
	}

	public static boolean isMithicMobEntity(Entity e) {
		return API().isMythicMob(e);
	}

	public static ActiveMob getMithicMob(Entity entity) {
		return API().getMythicMobInstance(entity);
	}

	public static LinkedList<ConfigMobs> getValidMobsForBiome(Biome biome) {
		LinkedList<ConfigMobs> mobs = biomes.get(biome.name());
		if (mobs == null) mobs = new LinkedList<ConfigMobs>();
		return mobs;
	}

	public static void startMobSpawnTimer() {
		if (task == null) task = new MobAsynckSpawnTimer();
		TaskMaster.asyncTimer("MobAsynckSpawner", task, TimeTools.toTicks(MobAsynckSpawnTimer.SPAWN_COOLDOWN));
	}

	public static void stopMobSpawnTimer() {
		if (task != null) TaskMaster.cancelTimer("MobAsynckSpawner");
	}

	public static int despawnAll() {
		return MythicMobs.inst().getMobManager().despawnAllMobs();
	}

	public static void despawnMobsFromRadius(Location loc, int radius) {
		Set<Entity> mobs = getMobsFromRadius(loc, radius);
		int count = 0;
		for (Entity e : mobs) {
			e.remove();
			count++;
		}
		CivLog.debug("kill " + count + "  mobs");
	}

	public static Set<Entity> getMobsFromRadius(Location loc, int radius) {
		Set<Entity> mobs = new HashSet<>();
		if (!loc.getWorld().getName().equals("world")) return mobs;
		int r = radius / 16;
		int rSqr = r * (r + 1);

		int cx = loc.getBlockX() / 16;
		int cz = loc.getBlockZ() / 16;
		World world = loc.getWorld();

		for (int chX = 0 - r; chX <= r; chX++) {
			for (int chZ = 0 - r; chZ <= r; chZ++) {
				if (chX * chX + chZ * chZ > rSqr) continue;
				Chunk chunk = world.getChunkAt(cx + chX, cz + chZ);
				for (Entity e : chunk.getEntities()) {
					if (MobStatic.isMithicMobEntity(e)) mobs.add(e);
				}
			}
		}
		return mobs;
	}

	public static Entity spawnCustomMob(String mobId, Location location) {
		String mmid;
		if (mobId == null) {
			LinkedList<ConfigMobs> validMobs = getValidMobsForBiome(location.getBlock().getBiome());
			if (validMobs.isEmpty()) return null;
			mmid = validMobs.get(civRandom.nextInt(validMobs.size())).uid;
		} else
			mmid = mobId;
		if (disableCustomMobs.contains(mmid)) return null;
		try {
			return API().spawnMythicMob(mmid, location);
		} catch (InvalidMobTypeException e) {
			CivLog.error("MythicMobs can not spawn mobType " + mmid);
			return null;
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
				CivLog.error("При чтении дропа не найден моб " + id);
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