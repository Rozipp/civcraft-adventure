/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.timers;

import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.construct.Cave;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class UpdateSecondTimer extends CivAsyncTask {

	public static ReentrantLock lock = new ReentrantLock();

	@Override
	public void run() {
		if (!lock.tryLock()) return;
		try {
			// Loop through each structure, if it has an update function call it in another async process
			for (Structure struct : CivGlobal.getStructures()) {
				if (!struct.isActive()) continue;
				struct.onSecondUpdate();
			}
			for (Camp camp : CivGlobal.getCamps()) {
				camp.onSecondUpdate();
			}
			for (Cave cave : CivGlobal.getCaves()) {
				cave.onSecondUpdate();
			}
		} finally {
			lock.unlock();
		}

	}

}
