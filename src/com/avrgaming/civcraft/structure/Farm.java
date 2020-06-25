/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ChunkCoord;

public class Farm extends Structure {

	public static final long GROW_RATE = (int) CivSettings.getIntegerStructure("farm.grow_tick_rate");
	public static final int CROP_GROW_LIGHT_LEVEL = 9;
	public static final int MUSHROOM_GROW_LIGHT_LEVEL = 12;
	public static final int MAX_SUGARCANE_HEIGHT = 4;

	public FarmChunk farmChunk = null;

	public Farm(String id, Town town) throws CivException {
		super(id, town);
	}

	public Farm(ResultSet rs) throws SQLException, CivException {
		super(rs);
		build_farm(this.getCorner().getChunkCoord());
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
	public String getDynmapDescription() {
		return null;
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
	public void runOnBuild(ChunkCoord cChunk) throws CivException {
		build_farm(cChunk);
	}

	public void build_farm(ChunkCoord cChunk) {
		this.farmChunk = new FarmChunk(cChunk, this);
	}

	@Override
	public void onLoad() {
		ArrayList<SessionEntry> entries = new ArrayList<SessionEntry>();
		entries = CivGlobal.getSessionDatabase().lookup(farmChunk.getSessionKey());
		int missedGrowths = 0;

		if (entries.size() > 0) missedGrowths = Integer.valueOf(entries.get(0).value);

		class AsyncTask extends CivAsyncTask {
			int missedGrowths;

			public AsyncTask(int missedGrowths) {
				this.missedGrowths = missedGrowths;
			}

			@Override
			public void run() {
				farmChunk.setMissedGrowthTicks(missedGrowths);
				farmChunk.processMissedGrowths(true, this);
				farmChunk.saveMissedGrowths();
			}
		}

		TaskMaster.asyncTask(new AsyncTask(missedGrowths), 0);
	}

}
