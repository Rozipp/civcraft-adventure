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
package com.avrgaming.civcraft.threading.tasks;

import java.util.LinkedList;
import java.util.Queue;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Cityhall;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;

public class UpdateTechBar extends CivAsyncTask {

	private Civilization civ;

	public UpdateTechBar(Civilization civ) {
		this.civ = civ;
	}

	@Override
	public void run() {
		Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();

		for (Town town : civ.getTowns()) {
			double percentageDone = 0.0;
			Cityhall cityhall = town.getCityhall();

			if (!town.isActive()) return;

			SimpleBlock sb;
			if (cityhall.getTechnameSign() != null) {
				ConstructSign sign = cityhall.getTechnameSign();
				sb = new SimpleBlock(CivData.WALL_SIGN, sign.getDirection());
				sb.setBlockCoord(sign.getCoord());
				sb.specialType = Type.LITERAL;

				if (civ.getResearchTech() != null) {
					sb.message[0] = CivSettings.localize.localizedString("Researching");
					sb.message[1] = "";
					sb.message[2] = civ.getResearchTech().name;
					sb.message[3] = "";
				} else {
					sb.message[0] = CivSettings.localize.localizedString("Researching");
					sb.message[1] = "";
					sb.message[2] = CivSettings.localize.localizedString("Nothing");
					sb.message[3] = "";
				}
				sbs.add(sb);
				cityhall.addConstructBlock(sign.getCoord(), false);
			}

			if (cityhall.getTechdataSign() != null) {
				ConstructSign sign = cityhall.getTechdataSign();
				sb = new SimpleBlock(CivData.WALL_SIGN, sign.getDirection());
				sb.setBlockCoord(sign.getCoord());
				sb.specialType = Type.LITERAL;

				if (civ.getResearchTech() != null) {
					percentageDone = Math.round(percentageDone * 100);
					sb.message[0] = CivSettings.localize.localizedString("UpdateTechBar_sign_Percent");
					sb.message[1] = CivSettings.localize.localizedString("UpdateTechBar_sign_Complete");
					sb.message[2] = "" + percentageDone + "%";
					sb.message[3] = "";
				} else {
					sb.message[0] = CivSettings.localize.localizedString("UpdateTechBar_sign_Use");
					sb.message[1] = "/civ research";
					sb.message[2] = CivSettings.localize.localizedString("UpdateTechBar_sign_toStart");
					sb.message[3] = CivSettings.localize.localizedString("UpdateTechBar_sign_Researching");
				}
				sbs.add(sb);
				cityhall.addConstructBlock(sign.getCoord(), false);
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
	}
}
