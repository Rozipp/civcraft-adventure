/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.threading.sync;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.construct.farm.FarmChunk;
import com.avrgaming.civcraft.construct.farm.GrowBlock;
import com.avrgaming.civcraft.construct.structures.Farm;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.sync.request.GrowRequest;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class SyncGrowTask implements Runnable {

	public static Queue<GrowRequest> requestQueue = new LinkedList<GrowRequest>();
	public static ReentrantLock lock;

	public static final int UPDATE_LIMIT = 200;

	public SyncGrowTask() {
		lock = new ReentrantLock();
	}

	@Override
	public void run() {
		if (!CivGlobal.growthEnabled) return;

		HashSet<ChunkCoord> unloadedChunk = new HashSet<>();

		if (lock.tryLock()) {
			try {
				for (int i = 0; i < UPDATE_LIMIT; i++) {
					GrowRequest request = requestQueue.poll();
					if (request == null) continue;
					request.result = false;

					for (GrowBlock growBlock : request.growBlocks) {
						ChunkCoord ccoord = growBlock.bcoord.getChunkCoord();
						if (unloadedChunk.contains(ccoord)) continue;
						if (!ccoord.getChunk().isLoaded()) unloadedChunk.add(ccoord);

						switch (growBlock.typeId) {
						case CivData.CARROTS:
						case CivData.WHEAT:
						case CivData.POTATOES:
							if ((growBlock.data - 1) != ItemManager.getData(growBlock.bcoord.getBlock())) continue; // XXX replanted??
							break;
						}

						if (ItemManager.getTypeId(growBlock.bcoord.getBlock()) != growBlock.typeId) {
							if (growBlock.spawn)
								ItemManager.setTypeId(growBlock.bcoord.getBlock(), growBlock.typeId);
							else
								continue;
						}
						ItemManager.setData(growBlock.bcoord.getBlock(), growBlock.data);
						request.result = true;
					}
					request.finished = true;
					request.condition.signalAll();
				}

				// increment any farms that were not loaded.
				for (ChunkCoord ccoord : unloadedChunk) {
					for (Construct construct : CivGlobal.getConstructsFromChunk(ccoord)) {
						FarmChunk farmChunk = null;
						if (construct instanceof Farm) farmChunk = ((Farm) construct).farmChunk;
						if (construct instanceof Camp) farmChunk = ((Camp) construct).farmChunk;
						if (farmChunk != null) {
							farmChunk.incrementMissedGrowthTicks();
							farmChunk.saveMissedGrowths();
						}
					}
				}

			} finally {
				lock.unlock();
			}
		} else {
			CivLog.warning("SyncGrowTask: lock busy, retrying next tick.");
		}
	}
}
