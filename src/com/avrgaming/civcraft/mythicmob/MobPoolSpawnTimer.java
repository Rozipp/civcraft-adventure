package com.avrgaming.civcraft.mythicmob;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.avrgaming.civcraft.main.CivLog;

public class MobPoolSpawnTimer implements Runnable {

	public static class SpawnMobTask {
		Location loc;
		String mobId;
		MobSpawner mspawner;

		public SpawnMobTask(Location loc, String mobId, MobSpawner mspawner) {
			this.loc = loc;
			this.mobId = mobId;
			this.mspawner = mspawner;
		}
	}

	private static Queue<SpawnMobTask> updateLocations = new LinkedList<SpawnMobTask>();
	public static ReentrantLock spawnLock = new ReentrantLock();

	// Запускаеться раз в пол секунды
	public static int UPDATE_LIMIT = 100;

	public static void addSpawnMobTask(String mobId, Location loc, MobSpawner mspawner) {
		spawnLock.lock();
		SpawnMobTask smt = new SpawnMobTask(loc, mobId, mspawner);
		try {
			updateLocations.add(smt);
		} finally {
			spawnLock.unlock();
		}
	}

	public MobPoolSpawnTimer() {
	}

	@Override
	public void run() {
		if (spawnLock.tryLock()) {
			try {
				for (int i = 0; i < UPDATE_LIMIT; i++) {
					SpawnMobTask smt = updateLocations.poll();
					if (smt == null)
						break;
					if (smt.loc == null)
						continue;
					if (!smt.loc.getChunk().isLoaded())
						continue;
					Entity en = MobStatic.spawnCustomMob(smt.mobId, smt.loc);
					if (smt.mspawner != null)
						smt.mspawner.mobs.add(en);
				}
			} finally {
				spawnLock.unlock();
			}
		} else {
			CivLog.warning("Couldn't get sync spawn update lock, skipping until next tick.");
		}
	}
}