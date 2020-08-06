/************************************************************************* AVRGAMING LLC __________________
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

//import com.avrgaming.civcraft.construct.caves.Cave;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class UpdateCivtickTimer extends CivAsyncTask {

	public static ReentrantLock lock = new ReentrantLock();

	@Override
	public void run() {
		if (!lock.tryLock()) return;
		try {
			// Loop through each structure, if it has an update function call it in another async process
			Date now = new Date();
			for (Civilization civ : CivGlobal.getCivs()) {
				if (civ.isValid()) {
					for (Town town : civ.getTowns()) {
						town.onCivtickUpdate(this);
					}
				}
				civ.onCivtickUpdate();
			}
			CivLog.debug("onCivtickUpdate time = " + (System.currentTimeMillis() - now.getTime()));
			
			for (Camp camp : CivGlobal.getCamps()) {
				camp.onCivtickUpdate(this);
			}

//			for (Cave cave : CivGlobal.getCaves()) {
//				cave.onCivtickUpdate();
//			}
		} finally {
			lock.unlock();
		}

	}

}
