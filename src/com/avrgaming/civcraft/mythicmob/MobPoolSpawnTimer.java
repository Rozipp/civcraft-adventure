package com.avrgaming.civcraft.mythicmob;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;
import com.avrgaming.civcraft.main.CivLog;

public class MobPoolSpawnTimer implements Runnable {

	private static Queue<Location> updateLonations = new LinkedList<Location>();
	public static ReentrantLock spawnLock = new ReentrantLock();
	
	// Запускаеться раз в пол секунды
	public static int UPDATE_LIMIT = 100;

	public static void addLocation(Location loc) {
		spawnLock.lock();
		try {
			updateLonations.add(loc);
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
					Location loc = updateLonations.poll();
					if (loc == null) break;

					if (!loc.getChunk().isLoaded()) continue;

					MobStatic.spawnValidCustomMob(loc);
				}
			} finally {
				spawnLock.unlock();
			}
		} else {
			CivLog.warning("Couldn't get sync spawn update lock, skipping until next tick.");
		}
	}
}