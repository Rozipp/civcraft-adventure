/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.timers;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Quarry;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.TrommelAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.village.TransmuterAsyncTask;
import com.avrgaming.civcraft.village.Village;

public class UpdateEventTimer extends CivAsyncTask {

	public static ReentrantLock lock = new ReentrantLock();

	public UpdateEventTimer() {
	}

	@Override
	public void run() {

		if (!lock.tryLock()) {
			return;
		}

		try {
			// Loop through each structure, if it has an update function call it in another async process
			Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
			while (iter.hasNext()) {
				Structure struct = iter.next().getValue();
				if (!struct.isActive()) continue;
				if (struct.getUpdateEvent() != null && !struct.getUpdateEvent().equals("")) {
					if (struct.getUpdateEvent().equals("trommel_process")) {
						if (!CivGlobal.trommelsEnabled) {
							continue;
						}
						TaskMaster.asyncTask("trommel-" + struct.getCorner().toString(), new TrommelAsyncTask(struct), 0);
					}
					if (struct instanceof Quarry) {
						if (!CivGlobal.quarriesEnabled) continue;
						for (String ctrId : struct.locks.keySet()) {
							if (!struct.locks.get(ctrId).isLocked())
								TaskMaster.asyncTask("quarry-" + struct.getCorner() + ";tr-" + ctrId,
										new TransmuterAsyncTask(struct, CivSettings.transmuterRecipes.get(ctrId)), 0);
						}
					}
				}

				struct.onUpdate();
			}

//			for (Wonder wonder : CivGlobal.getWonders()) {
//				wonder.onUpdate();
//			}
			for (Village village : CivGlobal.getVillages()) {
				for (String ctrId : village.locks.keySet()) {
					if (!village.locks.get(ctrId).isLocked())
						TaskMaster.asyncTask("village-" + village.getCorner() + ";tr-" + ctrId,
								new TransmuterAsyncTask(village, CivSettings.transmuterRecipes.get(ctrId)), 0);
				}
			}

		} finally {
			lock.unlock();
		}

	}

}
