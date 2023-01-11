package com.avrgaming.civcraft.threading.tasks;

import java.util.HashSet;

import com.avrgaming.civcraft.construct.wonders.Battledome;
import com.avrgaming.civcraft.construct.wonders.Wonder;

//import org.bukkit.Bukkit;
//import org.bukkit.World;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class BattledomeMobSpawnTask extends CivAsyncTask {

	Battledome battledome;
	
	public static HashSet<String> debugTowns = new HashSet<String>();

	public static void debug(Battledome battledome, String msg) {
		if (debugTowns.contains(battledome.getTownOwner().getName())) {
			CivLog.warning("BattledomeDebug:"+battledome.getTownOwner().getName()+":"+msg);
		}
	}	
	
	public BattledomeMobSpawnTask(Wonder battledome) {
		this.battledome = (Battledome)battledome;
	}
	
	public void processBattledomeSpawn() {
		if (!battledome.isActive()) {
			debug(battledome, "Battledome inactive...");
			return;
		}
		
		debug(battledome, "Processing Battledome...");
	}
	
	@Override
	public void run() {
		if (this.battledome.lock.tryLock()) {
			try {
				try {
					processBattledomeSpawn();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} finally {
				this.battledome.lock.unlock();
			}
		} else {
			debug(this.battledome, "Failed to get lock while trying to start task, aborting.");
		}
	}

}
