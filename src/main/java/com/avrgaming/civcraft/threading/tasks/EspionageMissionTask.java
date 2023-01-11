/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.Spy;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class EspionageMissionTask implements Runnable {

	ConfigMission mission;
	String playerName;
	Town target;
	int secondsLeft;
	Location startLocation;

	public EspionageMissionTask(ConfigMission mission, String playerName, Location startLocation, Town target, int seconds) {
		this.mission = mission;
		this.playerName = playerName;
		this.target = target;
		this.startLocation = startLocation;
		this.secondsLeft = seconds;
	}

	@Override
	public void run() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		Resident resident = CivGlobal.getResident(player);
		CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("espionage_missionStarted"));

		while (secondsLeft > 0) {

			if (secondsLeft > 0) {
				secondsLeft--;

				/* Add base exposure. */
				resident.setPerformingMission(true);

				/* Process exposure penalities */
				if (target.processSpyExposure(resident)) {
					CivMessage.global(CivColor.Yellow + CivSettings.localize.localizedString("var_espionage_missionFailedAlert",
							(CivColor.White + player.getName()), mission.name, target.getName()));
					CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("espionage_missionFailed"));
					UnitStatic.removeUnit(player, "u_spy");
					resident.setPerformingMission(false);
					return;
				}

				if ((secondsLeft % 15) == 0) {
					CivMessage.send(player, CivColor.Yellow + CivColor.BOLD + CivSettings.localize.localizedString("var_espionage_secondsRemain", secondsLeft));
				} else
					if (secondsLeft < 15) {
						CivMessage.send(player,
								CivColor.Yellow + CivColor.BOLD + CivSettings.localize.localizedString("var_espionage_secondsRemain", secondsLeft));
					}

			}

			ChunkCoord coord = new ChunkCoord(player.getLocation());
			CultureChunk cc = CivGlobal.getCultureChunk(coord);

			if (cc == null || cc.getCiv() != target.getCiv()) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("espionage_missionAborted"));
				return;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
		resident.setPerformingMission(false);

		class PerformMissionTask implements Runnable {
			ConfigMission mission;
			String playerName;
			public PerformMissionTask(ConfigMission mission, String playerName) {
				this.mission = mission;
				this.playerName = playerName;
			}
			@Override
			public void run() {
				Spy.performMission(mission, playerName);
			}
		}

		TaskMaster.syncTask(new PerformMissionTask(mission, playerName));
	}

}
