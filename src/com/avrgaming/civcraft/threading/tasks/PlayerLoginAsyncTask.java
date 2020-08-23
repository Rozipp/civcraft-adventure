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

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class PlayerLoginAsyncTask implements Runnable {

	UUID playerUUID;

	public PlayerLoginAsyncTask(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	public Player getPlayer() throws CivException {
		return Bukkit.getPlayer(playerUUID);
	}

	@Override
	public void run() {
		try {
			if (playerUUID == null || getPlayer() == null) {
				CivLog.info("Skipping PlayerLoginAsyncTask, otherwise NPE.");
				return;
			}
			CivLog.info("Running PlayerLoginAsyncTask for " + getPlayer().getName() + " UUID(" + playerUUID + ")");
			Resident resident = CivGlobal.getResidentViaUUID(playerUUID);
			if (resident != null && !resident.getName().equals(getPlayer().getName())) {
				CivGlobal.removeResident(resident);
				resident.setName(getPlayer().getName());
				resident.save();
				CivGlobal.addResident(resident);
			}

			/* Test to see if player has changed their name. If they have, these residents will not match. Disallow players changing their name without
			 * admin approval. */
			if (CivGlobal.getResidentViaUUID(getPlayer().getUniqueId()) != resident) {
				TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, CivSettings.localize.localizedString("PlayerLoginAsync_usernameChange1") + CivSettings.localize.localizedString("PlayerLoginAsync_usernameChange2")));
				return;
			}

			if (resident == null) {
				CivLog.info("No resident found. Creating for " + getPlayer().getName());
				try {
					resident = new Resident(getPlayer().getUniqueId(), getPlayer().getName());
				} catch (InvalidNameException e) {
					TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, CivSettings.localize.localizedString("PlayerLoginAsync_usernameInvalid")));
					return;
				}

				CivGlobal.addResident(resident);
				CivLog.info("Added resident:" + resident.getName());
				resident.setRegistered(System.currentTimeMillis());
				resident.setProtected(true);
				int mins = CivSettings.civConfig.getInt("global.pvp_timer", 120);
				CivMessage.send(resident, CivColor.LightGray + CivSettings.localize.localizedString("var_PlayerLoginAsync_pvpTimerPropmt1", mins));
				CivMessage.send(resident, CivColor.LightGray + CivSettings.localize.localizedString("PlayerLoginAsync_pvpTimerPropmt2"));
			}

			/* Resident is present. Lets check the UUID against the stored UUID. We are not going to allow residents to change names without admin
			 * permission. If someone logs in with a name that does not match the stored UUID, we'll kick them. */
			if (resident.getUuid() == null) {
				/* This resident does not yet have a UUID stored. Free lunch. */
				resident.setUuid(getPlayer().getUniqueId());
				CivLog.info("Resident named:" + resident.getName() + " was acquired by UUID:" + resident.getUuid().toString());
			} else
				if (!resident.getUuid().equals(getPlayer().getUniqueId())) {
					TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, CivSettings.localize.localizedString("PlayerLoginAsync_usernameInUse")));
					return;
				}

			UnitStatic.removeChildrenItems(getPlayer());
			resident.setUnitObjectId(0);
			if (!resident.isGivenKit()) TaskMaster.syncTask(new GivePlayerStartingKit(resident.getName()));

			if (War.isWarTime() && War.isOnlyWarriors()) {
				if (!getPlayer().isOp() || getPlayer().hasPermission(CivSettings.MINI_ADMIN)) {
					if (!resident.hasTown() || !resident.getTown().getCiv().getDiplomacyManager().isAtWar()) {
						TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, CivSettings.localize.localizedString("PlayerLoginAsync_onlyWarriorsAllowed")));
						return;
					}
				} else {// Allowed to connect since player is OP or mini admin.
				}

			}

			if (!getPlayer().isOp()) {
				CultureChunk cc = CivGlobal.getCultureChunk(new ChunkCoord(getPlayer().getLocation()));
				if (cc != null && cc.getCiv() != resident.getCiv()) {
					Relation.Status status = cc.getCiv().getDiplomacyManager().getRelationStatus(getPlayer());
					String color = PlayerChunkNotifyAsyncTask.getNotifyColor(cc, status, getPlayer());
					String relationName = status.name();

					if (War.isWarTime() && status.equals(Relation.Status.WAR) && !cc.getCiv().isAdminCiv()) {
						/* Test for players who were not logged in when war time started. If they were not logged in, they are enemies, and are inside our borders
						 * they need to be teleported back to their own town hall. */

						if (resident.getLastOnline() < War.getStart().getTime()) {
							resident.teleportHome();
							CivMessage.send(resident, CivColor.LightGray + CivSettings.localize.localizedString("PlayerLoginAsync_loginDuringWar"));
						}
					} else
						if (status.equals(Relation.Status.HOSTILE) || status.equals(Relation.Status.WAR) && !cc.getCiv().isAdminCiv()) {
							resident.teleportHome();
							CivMessage.send(resident, CivColor.LightGray + CivSettings.localize.localizedString("PlayerLoginAsync_loginNotAllies"));
						}

					CivMessage.sendCiv(cc.getCiv(), CivSettings.localize.localizedString("var_PlayerLoginAsync_inBorderAlert", (color + getPlayer().getDisplayName() + "(" + relationName + ")")));
				}
			}

			resident.setLastOnline(System.currentTimeMillis());
			resident.setLastIP(getPlayer().getAddress().getAddress().getHostAddress());
			resident.save();

			// TODO send town board messages?
			// TODO set default modes?
			resident.showPlayerLoginWarnings(getPlayer());
			resident.loadPerks(getPlayer());
			resident.calculateWalkingModifier(getPlayer());

			// Check for pending respawns.
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup("global:respawnPlayer");
			ArrayList<SessionEntry> deleted = new ArrayList<SessionEntry>();

			for (SessionEntry e : entries) {
				String[] split = e.value.split(":");

				BlockCoord coord = new BlockCoord(split[1]);
				getPlayer().teleport(coord.getLocation());
				deleted.add(e);
			}

			for (SessionEntry e : deleted) {
				CivGlobal.getSessionDatabase().delete(e.request_id, "global:respawnPlayer");
			}

			try {
				Player p = CivGlobal.getPlayer(resident);

				ArrayList<SessionEntry> deathEvents = CivGlobal.getSessionDatabase().lookup("pvplogger:death:" + resident.getName());
				if (deathEvents.size() != 0) {
					CivMessage.send(resident, CivColor.Rose + CivColor.BOLD + CivSettings.localize.localizedString("PlayerLoginAsync_killedWhilePVPLogged"));
					TaskMaster.syncTask(new Runnable() {
						@Override
						public void run() {
							p.setHealth(0);
							CivGlobal.getSessionDatabase().delete_all("pvplogger:death:" + p.getName());
						}
					});
				}
			} catch (CivException e1) {}

			if (EndConditionDiplomacy.canPeopleVote()) {
				CivMessage.send(resident, CivColor.LightGreen + CivSettings.localize.localizedString("PlayerLoginAsync_councilOf8"));
			}
			Civilization civ = resident.getCiv();
			if (civ != null) {
				if (civ.MOTD() != null) {
					CivMessage.send(resident, CivColor.LightPurple + "[Civ MOTD] " + CivColor.White + resident.getCiv().MOTD());
				}
			}

		} catch (CivException playerNotFound) {
			// Player logged out while async task was running.
		} catch (InvalidNameException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
