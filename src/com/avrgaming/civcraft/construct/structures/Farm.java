package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.farm.FarmChunk;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ChunkCoord;

import java.util.ArrayList;

public class Farm extends Structure {

	public static final long GROW_RATE = CivSettings.getIntegerStructure("farm.grow_tick_rate");
	public static final int MAX_SUGARCANE_HEIGHT = 4;

	public FarmChunk farmChunk = null;

	public Farm(String id, Town town) {
		super(id, town);
	}

	@Override
	public void delete() {
		if (this.getCorner() != null) {
			ChunkCoord ccoord = farmChunk.getCCoord();
			CivGlobal.removeFarmChunk(ccoord);
			CivGlobal.getSessionDatabase().delete_all(farmChunk.getSessionKey());
		}

		super.delete();
	}



	@Override
	public boolean isCanRestoreFromTemplate() {
		return false;
	}

	@Override
	public String getMarkerIconName() {
		return "basket";
	}

	@Override
	public void runOnBuild(ChunkCoord cChunk) {
		build_farm(cChunk);
	}

	public void build_farm(ChunkCoord cChunk) {
		this.farmChunk = new FarmChunk(cChunk, this);
	}

	@Override
	public void onLoad() {
		build_farm(this.getCorner().getChunkCoord());
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(farmChunk.getSessionKey());
		int missedGrowths = (entries.size() > 0) ? Integer.parseInt(entries.get(0).value) : 0;
		TaskMaster.asyncTask(new CivAsyncTask() {
			@Override
			public void run() {
				farmChunk.setMissedGrowthTicks(missedGrowths);
				farmChunk.processMissedGrowths(true, this);
				farmChunk.saveMissedGrowths();
			}
		}, 0);
	}

}
