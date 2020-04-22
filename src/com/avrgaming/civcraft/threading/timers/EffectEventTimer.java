/*************************** AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.timers;

import java.text.DecimalFormat;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.AttrSource;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.CivColor;

public class EffectEventTimer extends CivAsyncTask {
	public static ReentrantLock runningLock = new ReentrantLock();

	@Override
	public void run() {
		if (runningLock.tryLock()) {
			try {
				/* Clear the last taxes so they don't accumulate. */
				for (Civilization civ : CivGlobal.getCivs()) {
					civ.lastTaxesPaidMap.clear();
				}

				// Loop through each structure, if it has an update function call it in another async process
				for (Structure struct : CivGlobal.getStructures()) {
					Townhall townhall = struct.getTown().getTownHall();

					if (townhall == null) continue;
					if (!struct.isActive()) continue;
					struct.onHourlyUpdate(this);
				}

				/* Process any hourly attributes for this town. - Culture */
				for (Town town : CivGlobal.getTowns()) {
					double cultureGenerated;

					// highjack this loop to display town hall warning.
					Townhall townhall = town.getTownHall();
					if (townhall == null) {
						CivMessage.sendTown(town, CivColor.Yellow + CivSettings.localize.localizedString("effectEvent_noTownHall"));
						continue;
					}

					AttrSource cultureSources = town.getCulture();

					// Get amount generated after culture rate/bonus.
					cultureGenerated = cultureSources.total;
					cultureGenerated = Math.round(cultureGenerated);
					town.addAccumulatedCulture(cultureGenerated);

					// Get from unused beakers.
					DecimalFormat df = new DecimalFormat();
					double unusedBeakers = town.getUnusedBeakers();

					try {
						double cultureToBeakerConversion = CivSettings.getDouble(CivSettings.cultureConfig, "beakers_per_culture");
						if (unusedBeakers > 0) {
							double cultureFromBeakers = unusedBeakers * cultureToBeakerConversion;
							cultureFromBeakers = Math.round(cultureFromBeakers);
							unusedBeakers = Math.round(unusedBeakers);

							if (cultureFromBeakers > 0) {
								CivMessage.sendTown(town,
										CivColor.LightGreen + CivSettings.localize.localizedString("var_effectEvent_convertBeakers",
												(CivColor.LightPurple + df.format(unusedBeakers) + CivColor.LightGreen),
												(CivColor.LightPurple + df.format(cultureFromBeakers) + CivColor.LightGreen)));
								cultureGenerated += cultureFromBeakers;
								town.addAccumulatedCulture(cultureFromBeakers);
								town.setUnusedBeakers(0);
							}
						}
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
						return;
					}

					cultureGenerated = Math.round(cultureGenerated);
					CivMessage.sendTown(town, CivColor.LightGreen + CivSettings.localize.localizedString("var_effectEvent_generatedCulture",
							(CivColor.LightPurple + cultureGenerated + CivColor.LightGreen)));
				}

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
