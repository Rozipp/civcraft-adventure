/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.admin;

import java.util.ArrayList;
import java.util.LinkedList;

import com.avrgaming.civcraft.command.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.sls.SLSManager;

public class AdminCommand extends MenuAbstractCommand {

	public AdminCommand(String perentCommand) {
		super(perentCommand);
		this.addValidator(Validators.validAdmin);
		displayName = CivSettings.localize.localizedString("adcmd_Name");

		add(new CustomCommand("perm").withDescription(CivSettings.localize.localizedString("adcmd_permDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (resident.isPermOverride()) {
					resident.setPermOverride(false);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_permOff"));
					return;
				}
				resident.setPermOverride(true);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_permOn"));
			}
		}));
		add(new CustomCommand("sbperm").withDescription(CivSettings.localize.localizedString("adcmd_adpermDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (resident.isSBPermOverride()) {
					resident.setSBPermOverride(false);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_sbpermOff"));
					return;
				}
				resident.setSBPermOverride(true);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_sbpermOn"));
			}
		}));
		add(new AdminMobCommand("mob").withDescription(CivSettings.localize.localizedString("cmd_mob_managament")));
		add(new AdminUnitCommand("unit").withDescription("Работа с юнитами"));
		add(new AdminRecoverCommand("recover").withDescription(CivSettings.localize.localizedString("adcmd_recoverDesc")));
		add(new CustomCommand("server").withDescription(CivSettings.localize.localizedString("adcmd_serverDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, Bukkit.getServerName());
			}
		}));
		add(new CustomCommand("chestreport").withDescription(CivSettings.localize.localizedString("adcmd_chestReportDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Integer radius = Commander.getNamedInteger(args, 0);
				Player player = Commander.getPlayer(sender);
				LinkedList<ChunkCoord> coords = new LinkedList<ChunkCoord>();
				for (int x = -radius; x < radius; x++) {
					for (int z = -radius; z < radius; z++) {
						ChunkCoord coord = new ChunkCoord(player.getLocation());
						coord.setX(coord.getX() + x);
						coord.setZ(coord.getZ() + z);
						coords.add(coord);
					}
				}
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_chestReportHeader"));
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
				TaskMaster.syncTask(new ReportChestsTask(sender, coords), 0);
			}
		}));
		add(new CustomCommand("playerreport").withDescription(CivSettings.localize.localizedString("adcmd_playerreportDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Integer radius = Commander.getNamedInteger(args, 0);
				Player player = Commander.getPlayer(sender);
				Location loc = player.getLocation();
				LinkedList<Player> players = new LinkedList<>();
				if (radius > 10) radius = 10;

				if (radius == 0) {
					for (Entity e : loc.getChunk().getEntities()) {
						if (e.getLocation().distance(loc) <= radius) {
							if (e instanceof Player) players.add((Player) e);
						}
					}
				} else {
					int x = (int) loc.getX(), y = (int) loc.getY(), z = (int) loc.getZ();
					int radiusSqr = radius * (radius + 1);
					for (int chX = 0 - radius; chX <= radius; chX++) {
						for (int chZ = 0 - radius; chZ <= radius; chZ++) {
							if (chX * chX + chZ * chZ > radiusSqr) continue;
							Location newloc = new Location(loc.getWorld(), x + (chX * 16), y, z + (chZ * 16));
							TaskMaster.syncTask(new Runnable() {
								@Override
								public void run() {
									Chunk chunk = newloc.getChunk();
									for (Entity e : chunk.getEntities()) {
										if (e instanceof Player) players.add((Player) e);
									}
								}
							});
						}
					}
				}
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_playerreportHeader"));
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
				TaskMaster.syncTask(new ReportPlayerInventoryTask(sender, players), 0);
			}
		}));

		add(new AdminCivCommand("civ").withDescription(CivSettings.localize.localizedString("adcmd_civDesc")));
		add(new AdminTownCommand("town").withDescription(CivSettings.localize.localizedString("adcmd_townDesc")));
		add(new AdminWarCommand("war").withDescription(CivSettings.localize.localizedString("adcmd_warDesc")));
		add(new AdminLagCommand("lag").withDescription(CivSettings.localize.localizedString("adcmd_lagdesc")));
		add(new AdminCampCommand("camp").withDescription(CivSettings.localize.localizedString("adcmd_campDesc")));
		add(new AdminChatCommand("chat").withDescription(CivSettings.localize.localizedString("adcmd_chatDesc")));
		add(new AdminResCommand("res").withDescription(CivSettings.localize.localizedString("adcmd_resDesc")));
		add(new AdminBuildCommand("build").withDescription(CivSettings.localize.localizedString("adcmd_buildDesc")));
		add(new CustomCommand("items").withDescription(CivSettings.localize.localizedString("adcmd_itemsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				GuiInventory.openGuiInventory(Commander.getPlayer(sender), "ItemsSpawn", null);
			}
		}));
		add(new AdminItemCommand("item").withDescription(CivSettings.localize.localizedString("adcmd_itemDesc")));
		add(new AdminTimerCommand("timer").withDescription(CivSettings.localize.localizedString("adcmd_timerDesc")));
		add(new CustomCommand("clearendgame").withDescription(CivSettings.localize.localizedString("adcmd_clearEndGameDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				String key = Commander.getNamedString(args , 1, "enter key.");
				
				ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
				if (entries.size() == 0) {
					throw new CivException(CivSettings.localize.localizedString("adcmd_clearEndGameNoKey"));
				}
				for (SessionEntry entry : entries) {
					if (EndGameCondition.getCivFromSessionData(entry.value) == civ) {
						CivGlobal.getSessionDatabase().delete(entry.request_id, entry.key);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_clearEndGameSuccess", civ.getName()));
					}
				}
			}
		}));
		add(new CustomCommand("endworld").withDescription(CivSettings.localize.localizedString("adcmd_endworldDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.endWorld = !CivGlobal.endWorld;
				if (CivGlobal.endWorld)
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOn"));
				else
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOff"));
			}
		}));
		add(new CustomCommand("heartbeat").withDescription(CivSettings.localize.localizedString("adcmd_heartbeatDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				SLSManager.sendHeartbeat();
			}
		}));
		add(new CustomCommand("clearchat").withDescription(CivSettings.localize.localizedString("clearchat")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (int i = 0; i < 200; ++i) {
					Bukkit.broadcastMessage("");
				}
				final Player player = Commander.getPlayer(sender);
				CivMessage.global(CivSettings.localize.localizedString("chatcleared", player.getName()));
			}
		}));
		add(new CustomCommand("startMission").withDescription(CivSettings.localize.localizedString("adcmd_startMission")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {

			}
		}));
		add(new CustomCommand("count").withDescription(CivSettings.localize.localizedString("adcmd_count")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, CivColor.RoseBold + "Total Residents: " + CivGlobal.getResidents().size());
				CivMessage.send(sender, CivColor.RoseBold + "Total Camps: " + CivGlobal.getCamps().size());
				CivMessage.send(sender, CivColor.RoseBold + "Total Towns: " + CivGlobal.getTowns().size());
				CivMessage.send(sender, CivColor.RoseBold + "Total Civs: " + CivGlobal.getCivs().size());
			}
		}));
		add(new CustomCommand("globalwar").withDescription(CivSettings.localize.localizedString("adcmd_globalWar")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (final Civilization civ : CivGlobal.getCivs()) {
					for (final Civilization civ2 : CivGlobal.getCivs()) {
						if (!civ.getDiplomacyManager().atWarWith(civ2)) {
							CivGlobal.setRelation(civ, civ2, Relation.Status.WAR);
							CivGlobal.setAggressor(civ, civ2, civ);
							CivGlobal.setAggressor(civ2, civ, civ);
						}
					}
				}
				Bukkit.dispatchCommand(sender, "ad war start");
			}
		}));
		add(new CustomCommand("gc").withDescription(CivSettings.localize.localizedString("cmd_gc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final long start = System.nanoTime();
				System.gc();
				CivMessage.sendSuccess(sender, CivColor.LightGreenBold + CivSettings.localize.localizedString("cmd_gc_result", CivColor.GoldBold + formatFloat((System.nanoTime() - start) / 1000000L, 2) + CivColor.LightGreenBold));
			}
		}));
		add(new AdminReportCommand("report").withDescription(CivSettings.localize.localizedString("adcmd_report")));
	}

	public String formatFloat(float num, final int pr) {
		final StringBuilder sb = new StringBuilder();
		sb.append((int) num);
		sb.append('.');
		for (int i = 0; i < pr; ++i) {
			num *= 10.0f;
			sb.append((int) num % 10);
		}
		return sb.toString();
	}

	public void setfullmessage_cmd(CommandSender sender, String[] args) {
		if (args.length < 2) {
			CivMessage.send(sender, CivSettings.localize.localizedString("Current") + CivGlobal.fullMessage);
			return;
		}
		synchronized (CivGlobal.maxPlayers) {
			CivGlobal.fullMessage = args[1];
		}
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetTo") + args[1]);
	}
}
