package com.avrgaming.civcraft.mythicmob;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;

public class MobSpawner {

	public static int MAX_MOBS_SPAWNER = 10;

	public Location loc;
	public String mobId;
	public List<Entity> mobs;
	public boolean active = false;
	private Construct owner = null;
	private Long nextTime = (long) 0;

	public MobSpawner(Location loc, String mobId) {
		this.loc = loc;
		this.mobId = mobId;
		this.owner = null;
		mobs = new LinkedList<Entity>();
	}

	public void activate() {
		this.active = true;
		MobAsynckSpawnTimer.addSpawnerTask(this);
	}

	public void diactivate() {
		this.active = false;
		MobAsynckSpawnTimer.removeSpawnerTask(this);
	}

	public void spawn() {
		Date now = new Date();
		if (now.getTime() < this.nextTime) {
			return;
		}

		if (loc == null) {
			diactivate();
			return;
		}

		if (!loc.getChunk().isLoaded()) {
			diactivate();
			CivLog.error("chunk not loaded for spawner."
					+ ((this.owner != null) ? "  Owner = " + this.owner.getDisplayName() : ""));
			return;
		}

		if (mobs.size() < MAX_MOBS_SPAWNER) {
			MobPoolSpawnTimer.addSpawnMobTask(mobId, loc, this);
		} else {
			Iterator<Entity> mIterator = mobs.iterator();
			while (mIterator.hasNext()) {
				Entity en = mIterator.next();
				if (en == null || en.isDead())
					mIterator.remove();
			}
		}
		this.nextTime = now.getTime() + CivCraft.civRandom.nextInt(10000);
	}

}
