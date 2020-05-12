package com.avrgaming.civcraft.units;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.threading.TaskMaster;

public class CooldownTimerTask implements Runnable {

	public static ReentrantLock lock = new ReentrantLock();

	public static Set<Cooldown> cooldowns = new HashSet<>();

	public static void addCooldown(Cooldown cooldown) {
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				try {
					while (!lock.tryLock()) {}
					cooldowns.add(cooldown);
				} finally {
					lock.unlock();
				}
			}
		}, 0);
	}

	@Override
	public void run() {
		if (!lock.tryLock()) return;
		List<Cooldown> finished = new LinkedList<>();
		try {
			// Loop through each structure, if it has an update function call it in another async process
			for (Cooldown key : cooldowns) {
				key.processItem();
				if (key.isRefresh()) finished.add(key);
			}

			for (Cooldown key : finished) {
				key.finish();
				cooldowns.remove(key);
			}
		} finally {
			lock.unlock();
		}
	}

}
