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
package com.avrgaming.civcraft.command.old;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.randomevents.ConfigRandomEvent;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class AdminTownCommand extends MenuAbstractCommand {

	public AdminTownCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_town_name");

		add(new CustomCommand("disband").withDescription(CivSettings.localize.localizedString("adcmd_town_disbandDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				if (town.isCapitol()) throw new CivException(CivSettings.localize.localizedString("adcmd_town_disbandError"));
				CivMessage.sendTown(town, CivSettings.localize.localizedString("adcmd_town_disbandBroadcast"));
				town.delete();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_disbandSuccess"));
			}
		}));
		add(new CustomCommand("claim").withDescription(CivSettings.localize.localizedString("adcmd_town_claimDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Player player = Commander.getPlayer(sender);
				TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
				if (tc == null) {
					tc = new TownChunk(town, player.getLocation());
					CivGlobal.addTownChunk(tc);
					try {
						town.addTownChunk(tc);
					} catch (AlreadyRegisteredException e) {
						e.printStackTrace();
					}
					tc.save();
					town.save();
					CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_adcmd_town_claimSucess", town.getName()));
				} else {
					CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_town_claimErrorOwned", town.getName()));
				}
			}
		}));
		add(new CustomCommand("unclaim").withDescription(CivSettings.localize.localizedString("adcmd_town_unclaimDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Player player = Commander.getPlayer(sender);
				TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
				if (tc != null) {
					tc.getTown().removeTownChunk(tc);
					CivGlobal.removeTownChunk(tc);
					try {
						tc.delete();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					town.save();
					CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_adcmd_town_unclaimSuccess", town.getName()));
				} else {
					CivMessage.sendError(sender, CivSettings.localize.localizedString("adcmd_town_unclaimErrorNotOwned"));
				}
			}
		}));
		add(new CustomCommand("hammerrate").withDescription(CivSettings.localize.localizedString("adcmd_town_hammerrateDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_town_hammerratePrompt"));
				Town town = Commander.getNamedTown(args, 0);
				try {
					town.SM.setBaseHammers(Double.valueOf(args[1]));
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_hammerrateSuccess", args[1], args[2]));
				} catch (NumberFormatException e) {
					throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
				town.save();
			}
		}));
		add(new CustomCommand("addmayor").withDescription(CivSettings.localize.localizedString("adcmd_town_addmayorDesc")).withTabCompleter(new TownInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getNamedTown(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						town.GM.addMayor(resident);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_addmayorSuccess", resident.getName(), town.getName()));
					}
				}));
		add(new CustomCommand("addassistant").withDescription(CivSettings.localize.localizedString("adcmd_town_addAssistantDesc")).withTabCompleter(new TownInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getNamedTown(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						town.GM.addAssistant(resident);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_addAssistantSuccess", resident.getName(), town.getName()));
					}
				}));
		add(new CustomCommand("rmmayor").withDescription(CivSettings.localize.localizedString("adcmd_town_rmmayorDesc")).withTabCompleter(new TownInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getNamedTown(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_rmmayorNotInTown", resident.getName(), town.getName()));
						town.GM.removeMayor(resident);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_rmmayorSuccess", resident.getName(), town.getName()));

					}
				}));
		add(new CustomCommand("rmassistant").withDescription(CivSettings.localize.localizedString("adcmd_town_rmassistantDesc")).withTabCompleter(new TownInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getNamedTown(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						if (!town.GM.isAssistant(resident)) throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_rmassistantNotInTown", resident.getName(), town.getName()));
						town.GM.removeAssistant(resident);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_rmassistantSuccess", resident.getName(), town.getName()));

					}
				}));
		add(new CustomCommand("tp").withDescription(CivSettings.localize.localizedString("adcmd_town_tpDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Cityhall cityhall = town.getCityhall();
				if (sender instanceof Player) {
					if (cityhall != null && cityhall.isComplete()) {
						BlockCoord bcoord = cityhall.getRandomRevivePoint();
						((Player) sender).teleport(bcoord.getLocation());
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_tpSuccess", town.getName()));
						return;
					} else {
						if (town.getTownChunks().size() > 0) {
							ChunkCoord coord = town.getTownChunks().iterator().next().getChunkCoord();
							((Player) sender).teleport(new Location(coord.getWorld(), (coord.getX() << 4), 100, (coord.getZ() << 4)));
							CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_tpSuccess", town.getName()));
							return;
						}
					}
					throw new CivException(CivSettings.localize.localizedString("adcmd_town_tpError"));
				}
			}
		}));
		add(new CustomCommand("culture").withDescription(CivSettings.localize.localizedString("adcmd_town_cultureDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Integer culture = Commander.getNamedInteger(args, 1);
				town.SM.addCulture(culture);
				town.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_var_adcmd_town_cultureSuccess", town.getName(), culture));
			}
		}));
		add(new CustomCommand("info").withDescription(CivSettings.localize.localizedString("adcmd_town_infoDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				// FIXME
			}
		}));
		add(new CustomCommand("setciv").withDescription(CivSettings.localize.localizedString("adcmd_town_setcivDesc")).withTabCompleter(new TownInWorldTaber()).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Civilization civ = Commander.getNamedCiv(args, 1);
				if (town.getCiv() == civ) throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_setcivErrorInCiv", civ.getName()));
				if (town.isCapitol()) throw new CivException(CivSettings.localize.localizedString("adcmd_town_setcivErrorCapitol"));
				town.changeCiv(civ);
				CivGlobal.processCulture();
				CivMessage.global(CivSettings.localize.localizedString("var_adcmd_town_setcivSuccess1", town.getName(), civ.getName()));
			}
		}));
		add(new CustomCommand("select").withDescription(CivSettings.localize.localizedString("adcmd_town_selectDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Town selectTown = Commander.getNamedTown(args, 0);
				if (resident.getSelectedTown() == null && resident.getTown() == selectTown) throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_selectAlreadySelected", selectTown.getName()));
				if (resident.getSelectedTown() == selectTown) throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_selectAlreadySelected", selectTown.getName()));
				resident.setSelectedTown(selectTown);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_selectSuccess", selectTown.getName()));
			}
		}));
		add(new CustomCommand("claimradius").withDescription(CivSettings.localize.localizedString("adcmd_town_claimradiusDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Integer radius = Commander.getNamedInteger(args, 0);
				int count = claimradius(town, Commander.getPlayer(sender).getLocation(), radius);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_claimradiusSuccess", count));
			}
		}));
		add(new CustomCommand("chestreport").withDescription(CivSettings.localize.localizedString("adcmd_town_chestReportDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Queue<ChunkCoord> coords = new LinkedList<ChunkCoord>();
				for (TownChunk tc : town.getTownChunks()) {
					ChunkCoord coord = tc.getChunkCoord();
					coords.add(coord);
				}
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_town_chestReportStart") + " " + town.getName());
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
				TaskMaster.syncTask(new ReportChestsTask(sender, coords), 0);
			}
		}));
		add(new CustomCommand("rebuildgroups").withDescription(CivSettings.localize.localizedString("adcmd_town_rebuildgroupsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, "FIXME");
				// FIXME
				// Town town = Commander.getNamedTown(args ,0);
				// if (town.GM.getDefaultGroup() == null) {
				// PermissionGroup residents;
				// try {
				// residents = new PermissionGroup(town, "residents");
				// town.setDefaultGroup(residents);
				// try {
				// residents.saveNow();
				// town.saveNow();
				// } catch (SQLException e) {
				// e.printStackTrace();
				// }
				// } catch (InvalidNameException e1) {
				// e1.printStackTrace();
				// }
				// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_rebuildgroupsResidents"));
				// }
				// if (town.GM.getAssistantGroup() == null) {
				// PermissionGroup assistant;
				// try {
				// assistant = new PermissionGroup(town, "assistants");
				// town.setAssistantGroup(assistant);
				// try {
				// assistant.saveNow();
				// town.saveNow();
				// } catch (SQLException e) {
				// e.printStackTrace();
				// }
				// } catch (InvalidNameException e) {
				// e.printStackTrace();
				// }
				// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_rebuildgroupsAssistants"));
				// }
				// if (town.GM.getMayorGroup() == null) {
				// PermissionGroup mayor;
				// try {
				// mayor = new PermissionGroup(town, "mayors");
				// town.setMayorGroup(mayor);
				// try {
				// mayor.saveNow();
				// town.saveNow();
				// } catch (SQLException e) {
				// e.printStackTrace();
				// }
				// } catch (InvalidNameException e) {
				// e.printStackTrace();
				// }
				// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_rebuildgroupsMayors"));
				// }
			}
		}));
		add(new CustomCommand("capture").withDescription(CivSettings.localize.localizedString("adcmd_town_captureDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				Town town = Commander.getNamedTown(args, 1);
				town.onDefeat(civ);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_captureSuccess", town.getName(), civ.getName()));
			}
		}));
		add(new CustomCommand("setmotherciv").withDescription(CivSettings.localize.localizedString("adcmd_town_setmothercivDesc")).withTabCompleter(new TownInWorldTaber()).withTabCompleter(new CivInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getNamedTown(args, 0);
						Civilization civ = Commander.getNamedCiv(args, 1);
						town.setMotherCiv(civ);
						town.save();
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_setMotherCivSuccess", town.getName(), civ.getName()));

					}
				}));
		add(new CustomCommand("sethappy").withDescription(CivSettings.localize.localizedString("adcmd_town_sethappyDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				double happy = Commander.getNamedDouble(args, 1);
				town.SM.baseHappy = happy;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_sethappySuccess", happy));
			}
		}));
		add(new CustomCommand("setunhappy").withDescription(CivSettings.localize.localizedString("adcmd_town_setunhappyDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				double unhappy = Commander.getNamedDouble(args, 1);
				town.SM.baseUnhappy = unhappy;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_setunhappySuccess", unhappy));
			}
		}));
		add(new CustomCommand("event").withDescription(CivSettings.localize.localizedString("adcmd_town_eventDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				if (args.length < 1) {
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_town_eventHeading"));
					String out = "";
					for (ConfigRandomEvent configEvent : CivSettings.randomEvents.values()) {
						out += configEvent.id + ",";
					}
					CivMessage.send(sender, out);
					return;
				}
				ConfigRandomEvent event = CivSettings.randomEvents.get(args[1]);
				RandomEvent randEvent = new RandomEvent(event);
				randEvent.start(town);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_eventRenameSuccess") + " " + event.name);
			}
		}));
		add(new CustomCommand("rename").withDescription(CivSettings.localize.localizedString("adcmd_town_renameDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_town_renameUnderscores"));
				String name = Commander.getNamedString(args, 1, CivSettings.localize.localizedString("EnterTownName"));
				try {
					town.rename(name);
				} catch (InvalidNameException e) {
					throw new CivException(e.getMessage());
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_renameSuccess"));
			}
		}));
		add(new CustomCommand("eventcancel").withDescription(CivSettings.localize.localizedString("adcmd_town_eventcancelDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				if (town.getActiveEvent() == null) throw new CivException(CivSettings.localize.localizedString("adcmd_town_eventcancel_noEvent", "§6" + town.getName() + CivColor.Red));
				try {
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_eventcancel_succusess", "§b" + town.getName() + CivColor.Red, "§6" + town.getActiveEvent().configRandomEvent.name));
					CivMessage.sendTown(town, CivSettings.localize.localizedString("adcmd_town_eventcancel_succusessTown", "§a" + town.getActiveEvent().configRandomEvent.name + CivColor.RESET, ((Player) sender).getDisplayName()));
					town.getActiveEvent().delete();
					town.setActiveEvent(null);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}));
		add(new CustomCommand("givebuff").withDescription("[id] gives this id buff to a town.").withTabCompleter(new TownInWorldTaber()).withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (ConfigBuff buff : CivSettings.buffs.values()) {
					String name = buff.id;
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				if (args.length < 2) throw new CivException("Enter the buff id");
				ConfigBuff buff = CivSettings.buffs.get(args[1]);
				if (buff == null) throw new CivException("No buff id:" + args[1]);
				town.getBuffManager().addBuff(buff.id, buff.id, "Debug");
				CivMessage.sendSuccess(sender, "Gave buff " + buff.name + " to town");
			}
		}));
		add(new CustomCommand("addpeoples").withDescription("[город][количесвто] Добавить в городе жителей").withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				int count = Commander.getNamedInteger(args, 1);
				town.PM.bornPeoples(count);
			}
		}));
		add(new CustomCommand("removepeoples").withDescription("[город][количество] Удалить из города жителей").withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				int count = Commander.getNamedInteger(args, 1);
				town.PM.deadPeoples(count);
			}
		}));
	}

	public static int claimradius(Town town, Location loc, Integer radius) {
		ChunkCoord coord = new ChunkCoord(loc);
		int count = 0;
		for (int x = -radius; x < radius; x++) {
			for (int z = -radius; z < radius; z++) {
				try {
					TownChunk.autoClaim(town, coord.getRelative(x, z));
					count++;
				} catch (CivException e) {}
			}
		}
		town.save();
		return count;
	}

}
