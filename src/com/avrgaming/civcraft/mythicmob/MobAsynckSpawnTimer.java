package com.avrgaming.civcraft.mythicmob;

import static com.avrgaming.civcraft.main.CivCraft.civRandom;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class MobAsynckSpawnTimer implements Runnable {
	public static int SPAWN_COOLDOWN = getMobsConfigOrDef("SPAWN_COOLDOWN", 15);
	public static int SPAWN_FOR_PLAYER_LIMIT = getMobsConfigOrDef("SPAWN_FOR_PLAYER_LIMIT", 5);
	public static int MIN_SPAWN_DISTANCE = getMobsConfigOrDef("MIN_SPAWN_DISTANCE", 20);
	public static int MAX_SPAWN_DISTANCE = getMobsConfigOrDef("MAX_SPAWN_DISTANCE", 40);
	public static int MOB_AREA_LIMIT = getMobsConfigOrDef("MOB_AREA_LIMIT", 10);
	public static int Y_SHIFT = 3;

	private static Set<MobSpawner> updateSpawns = new HashSet<MobSpawner>();

	public static void addSpawnerTask(MobSpawner mSpawn) {
		synchronized (updateSpawns) {
			updateSpawns.add(mSpawn);
		}
	}

	public static void removeSpawnerTask(MobSpawner mSpawn) {
		synchronized (updateSpawns) {
			updateSpawns.remove(mSpawn);
		}
	}

	@Override
	public void run() {
		for (MobSpawner msr : updateSpawns) {
			msr.spawn();
		}
		for (int j = 0; j < SPAWN_FOR_PLAYER_LIMIT; j++) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (!player.getWorld().getName().equals("world")) continue;
				// Сколько возле игрока в радиусе rmax мобов.
				int rmax = MAX_SPAWN_DISTANCE / 16;
				int rMaxSqr = rmax * (rmax + 1);
				boolean isMany = false;
				int count = 0;
				for (int chX = 0 - rmax; chX <= rmax && !isMany; chX++) {
					for (int chZ = 0 - rmax; chZ <= rmax && !isMany; chZ++) {
						if (chX * chX + chZ * chZ > rMaxSqr) continue;
						Chunk chunk = player.getLocation().add(chX * 16, 0, chZ * 16).getChunk();
						for (Entity e : chunk.getEntities()) {
							if (MobStatic.isMithicMobEntity(e)) if (++count >= MOB_AREA_LIMIT) {
								isMany = true;
								break;
							}
						}
					}
				}
				if (isMany) continue; // Если больше MOB_AREA_LIMIT то не делаем нового

				// находим новое место для спавна моба
				double radius = civRandom.nextDouble() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE) + MIN_SPAWN_DISTANCE;
				double fi = 2 * civRandom.nextDouble() * Math.PI;
				int x = (int) Math.round(radius * Math.cos(fi)) + player.getLocation().getBlockX();
				int z = (int) Math.round(radius * Math.sin(fi)) + player.getLocation().getBlockZ();
				World world = player.getWorld();
				int y = world.getHighestBlockYAt(x, z) + Y_SHIFT;
				Location mobLoc = new Location(world, x, y, z);
				// две простые проверки
				if (CivGlobal.getTownChunk(new ChunkCoord(mobLoc)) != null) continue;
				int blockFace = (ItemManager.getTypeId(mobLoc.getBlock().getRelative(BlockFace.DOWN)));
				if (blockFace == CivData.WATER || blockFace == CivData.WATER_RUNNING //
						|| blockFace == CivData.LAVA || blockFace == CivData.LAVA_RUNNING)
					continue;

				// ищем игрока в радиусе rmin. Если нашли, не спавним моба
				int rmin = MIN_SPAWN_DISTANCE / 16;
//				int rMinSqr = rmin * (rmin + 1);
				boolean isNearbyPlayer = false;
				for (int chX = 0 - rmin; chX <= rmin && !isNearbyPlayer; chX++) {
					for (int chZ = 0 - rmin; chZ <= rmin && !isNearbyPlayer; chZ++) {
//						if (chX * chX + chZ * chZ > rMaxSqr) continue;
						Chunk chunk = mobLoc.add(chX * 16, 0, chZ * 16).getChunk();
						for (Entity e : chunk.getEntities()) {
							if (e instanceof Player) {
								isNearbyPlayer = true;
								break;
							}
						}
					}
				}
				if (isNearbyPlayer) continue;

				MobPoolSpawnTimer.addSpawnMobTask(null, mobLoc, null);
			}
		}
	}

	private static Integer getMobsConfigOrDef(String s, int i) {
		try {
			return CivSettings.getInteger(CivSettings.mobsConfig, s);
		} catch (InvalidConfiguration e) {
			return i;
		}
	}
}
