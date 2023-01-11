package com.avrgaming.civcraft.units;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.threading.TaskMaster;

public class CooldownSynckTask implements Runnable {

	public static ReentrantLock lock = new ReentrantLock();

	public static Map<String, Cooldown> cooldowns = new HashMap<>();

	public static void addCooldown(String id, Cooldown cooldown) {
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				try {
					lock.lock();
					cooldowns.put(id, cooldown);
				} finally {
					lock.unlock();
				}
			}
		}, 0);
	}

	@Override
	public void run() {
		if (lock.tryLock()) {
			try {
				// Loop through each structure, if it has an update function call it in another async process
				Set<String> delete = new HashSet<String>();
				for (String key : cooldowns.keySet()) {
					Cooldown cooldown = cooldowns.get(key);
					cooldown.processItem();
					if (cooldown.getTime() < 1) {
						delete.add(key);
						if (cooldown.finisher != null) cooldown.finish();
					}
				}
				for (String key : delete) {
					cooldowns.remove(key);
				}
			} finally {
				lock.unlock();
			}
		}
	}

}
