/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.sync;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

public class SyncBuildUpdateTask implements Runnable {

	public static int UPDATE_LIMIT = CivSettings.getIntBase("update_limit_for_sync_build_task");

	private static Queue<SimpleBlock> updateBlocks = new LinkedList<SimpleBlock>();

	public static ReentrantLock buildBlockLock = new ReentrantLock();

	public static void queueSimpleBlock(Queue<SimpleBlock> sbList) {
		buildBlockLock.lock();
		try {
			updateBlocks.addAll(sbList);
		} finally {
			buildBlockLock.unlock();
		}
	}

	public SyncBuildUpdateTask() {
	}

	/*
	 * Runs once, per tick and changes the blocks represented by SimpleBlock up to
	 * UPDATE_LIMIT times.
	 */
	@Override
	public void run() {
		if (updateBlocks.isEmpty())
			return;
		if (buildBlockLock.tryLock()) {
			try {
				for (int i = 0; i < UPDATE_LIMIT; i++) {
					SimpleBlock sb = updateBlocks.poll();
					if (sb == null)
						break;

					Block block = sb.getBlock();

					/* Handle Special Blocks */
					switch (sb.specialType) {
					case COMMAND:
						ItemManager.setTypeIdAndData(block, CivData.AIR, 0, false);
						break;
					case LITERAL:
						ItemManager.setTypeIdAndData(block, sb.getType(), sb.getData(), false);
						if (block.getState() instanceof Sign) {
							Sign s = (Sign) block.getState();
							for (int j = 0; j < 4; j++) {
								s.setLine(j, sb.message[j]);
							}
							s.update();
						} else {
							ItemManager.setTypeIdAndData(block, CivData.AIR, 0, false);
						}
						break;
					case NORMAL:
//	if (ItemManager.getTypeId(block) != sb.getType() && sb.getType() != CivData.AIR) // Не уберает рамку
						ItemManager.setTypeIdAndData(block, sb.getType(), sb.getData(), false);
						break;
					}
					if (sb.buildable != null)
						sb.buildable.savedBlockCount++;
				}
			} finally {
				buildBlockLock.unlock();
			}
		} else {
			CivLog.warning("Couldn't get sync build update lock, skipping until next tick.");
		}
	}

}
