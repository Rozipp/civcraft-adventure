/*************************** AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.timers;

import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class TownHourlyTick extends CivAsyncTask {
	public static ReentrantLock runningLock = new ReentrantLock();

	@Override
	public void run() {
		if (runningLock.tryLock()) {
			try {
				/* Clear the last taxes so they don't accumulate. */
				Date now = new Date();
				for (Civilization civ : CivGlobal.getCivs()) {
					civ.lastTaxesPaidMap.clear();
					if (civ.isValid()) {
						for (Town town : civ.getTowns()) {
							town.onHourlyUpdate(this);
						}
					}
				}
				CivLog.debug("onHourlyUpdate time = " + (System.currentTimeMillis() - now.getTime()));

				/* Checking for expired vassal states. */
				CivGlobal.checkForExpiredRelations();
			} finally {
				runningLock.unlock();
			}
		} else {
			CivLog.error("COULDN'T GET LOCK FOR HOURLY TICK. LAST TICK STILL IN PROGRESS?");
		}
	}
}
