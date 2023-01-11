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
package com.avrgaming.civcraft.construct.farm;

import java.util.concurrent.TimeUnit;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class FarmGrowthSyncTask extends CivAsyncTask {

	// XXX Despite being named a sync timer, this task is now actually asynchronous

	@Override
	public void run() {
		try {
			if (!CivGlobal.growthEnabled) return;

			for (FarmChunk fc : CivGlobal.getFarmChunks()) {
				if (fc.getConstruct() == null) {
					System.out.println("FarmChunkError: Could not process farm chunk, town or struct was null. Orphan?");
					continue;
				}

				/* Since we're now async, we can wait on this lock. */
				try {
					if (fc.lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
						try {
							fc.processGrowth(this);
						} catch (InterruptedException e) {
							e.printStackTrace();
							continue;
						}
					} else {
						System.out.println("FarmChunkError: Lock Error");
						continue;
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					continue;
				} finally {
					fc.lock.unlock();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}