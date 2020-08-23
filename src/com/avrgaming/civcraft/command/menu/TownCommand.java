/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.menu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.command.taber.ResidentInTownTaber;
import com.avrgaming.civcraft.command.taber.TownInCivTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBiomeInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.questions.ChangeTownRequest;
import com.avrgaming.civcraft.questions.JoinTownResponse;
import com.avrgaming.civcraft.questions.Question;
import com.avrgaming.civcraft.threading.sync.TeleportPlayerTaskTown;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class TownCommand extends MenuAbstractCommand {

	public static final long INVITE_TIMEOUT = 30000; // 30 seconds

	public TownCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_town_name");
		this.setAliases("t", "ещцт", "е");
		add(new CustomCommand("claim").withDescription(CivSettings.localize.localizedString("cmd_town_claimDesc")).withValidator(Validators.validMayorAssistant).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (War.isWarTime()) throw new CivException("§c" + CivSettings.localize.localizedString("wartime_now_cenceled"));
				Player player = Commander.getPlayer(sender);
				Town town = Commander.getSelectedTown(sender);
				TownChunk tc = TownChunk.claim(town, new ChunkCoord(player.getLocation()));
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_town_chunk_success", tc.getCoordToString(), town.getTownChunks().size(), town.getMaxPlots()));
			}
		}));
		add(new CustomCommand("unclaim").withDescription(CivSettings.localize.localizedString("cmd_town_unclaimDesc")).withValidator(Validators.validMayorAssistant).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Player player = Commander.getPlayer(sender);
				Resident resident = Commander.getResident(sender);
				TownChunk tc = Commander.getStandingTownChunk(sender);
				if (town.getCiv().isAdminCiv()) if (player.hasPermission(CivSettings.MODERATOR) && !player.hasPermission(CivSettings.MINI_ADMIN)) throw new CivException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
				if (!town.GM.isMayorOrAssistant(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
				if (town.getTownChunks().size() <= 1) throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaimError"));
				if (tc.getTown() != resident.getTown()) throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaimNotInTown"));
				if (tc.perms.getOwner() != null && tc.perms.getOwner() != resident) throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaimOtherRes"));

				for (Construct construct : CivGlobal.getConstructsFromChunk(tc.getChunkCoord())) {
					if (construct instanceof Buildable && town.equals(construct.getTownOwner())) throw new CivException(CivSettings.localize.localizedString("cmd_town_unclaim_errorStructure"));
				}
				TownChunk.unclaim(tc);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_unclaimSuccess", tc.getCoordToString()));
			}
		}));
		add(new TownGroupCommand("group").withDescription(CivSettings.localize.localizedString("cmd_town_groupDesc")));
		add(new TownUpgradeCommand("upgrade").withAliases("up").withDescription(CivSettings.localize.localizedString("cmd_town_upgradeDesc")));
		add(new TownInfoCommand("info").withDescription(CivSettings.localize.localizedString("cmd_town_infoDesc")));
		add(new CustomCommand("add").withDescription(CivSettings.localize.localizedString("cmd_town_addDesc")).withValidator(Validators.validMayorAssistantLeader).withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident newResident = Commander.getNamedResident(args, 0);
				Player player = Commander.getPlayer(sender);
				Town town = Commander.getSelectedTown(sender);
				if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_town_addWar"));
				if (War.isWithinWarDeclareDays() && town.getCiv().getDiplomacyManager().isAtWar())
					throw new CivException(CivSettings.localize.localizedString("cmd_town_addCloseToWar") + " " + War.time_declare_days + " " + CivSettings.localize.localizedString("cmd_civ_dip_declareTooCloseToWar4"));
				if (newResident.hasCamp()) {
					CivMessage.send(newResident, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_addAlertError1", newResident.getName(), town.getName()));
					throw new CivException(CivSettings.localize.localizedString("var_cmd_town_addhasCamp", newResident.getName()));
				}
				if (town.hasResident(newResident)) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_addInTown", newResident.getName()));
				if (newResident.getTown() != null) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_addhasTown", newResident.getName(), newResident.getTown().getName()));
				JoinTownResponse join = new JoinTownResponse();
				join.town = town;
				join.resident = newResident;
				join.sender = player;
				newResident.validateJoinTown(town);
				Question.questionPlayer(player, CivGlobal.getPlayer(newResident), CivSettings.localize.localizedString("var_cmd_town_addInvite", town.getName()), INVITE_TIMEOUT, join);
				CivMessage.sendSuccess(sender, CivColor.LightGray + CivSettings.localize.localizedString("var_cmd_town_addSuccess", args[1], town.getName()));
			}
		}));
		add(new CustomCommand("members").withDescription(CivSettings.localize.localizedString("cmd_town_membersDesc")).withValidator(Validators.validHasTown).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_town_membersHeading", town.getName()));
				String out = "";
				for (Resident res : town.getResidents()) {
					out += res.getName() + ", ";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("deposit").withDescription(CivSettings.localize.localizedString("cmd_town_depositDesc")).withValidator(Validators.validHasTown).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_despositPrompt"));
				Resident resident = Commander.getResident(sender);
				Town town = Commander.getSelectedTown(sender);
				Double amount = Commander.getNamedDouble(args, 0);
				try {
					if (amount < 1) throw new CivException(amount + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
					amount = Math.floor(amount);
					town.depositFromResident(amount, resident);
				} catch (NumberFormatException e) {
					throw new CivException(args[0] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_despositSuccess", args[0], CivSettings.CURRENCY_NAME));
			}
		}));
		add(new CustomCommand("withdraw").withAliases("w").withDescription(CivSettings.localize.localizedString("cmd_town_withdrawDesc")).withValidator(Validators.validMayor).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_town_withdrawPrompt"));
				Town town = Commander.getSelectedTown(sender);
				Resident resident = Commander.getResident(sender);
				try {
					Double amount = Double.valueOf(args[0]);
					if (amount < 1) throw new CivException(amount + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
					amount = Math.floor(amount);
					if (!town.getTreasury().payTo(resident.getTreasury(), Double.valueOf(args[0]))) throw new CivException(CivSettings.localize.localizedString("cmd_town_withdrawNotEnough"));
				} catch (NumberFormatException e) {
					throw new CivException(args[0] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess", args[0], CivSettings.CURRENCY_NAME));
				CivLog.moneylog(sender.getName(), "/town withdraw " + args[0]);
			}
		}));
		add(new TownSetCommand("set").withDescription(CivSettings.localize.localizedString("cmd_town_setDesc")));
		add(new CustomCommand("leave").withDescription(CivSettings.localize.localizedString("cmd_town_leaveDesc")).withValidator(Validators.validHasTown).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Resident resident = Commander.getResident(sender);
				if (town != resident.getTown()) throw new CivException(CivSettings.localize.localizedString("cmd_town_leaveNotSelected"));
				if (town.GM.isOneMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_leaveOnlyMayor"));
				town.removeResident(resident);
				if (resident.isCivChat()) resident.setCivChat(false);
				if (resident.isTownChat()) {
					resident.setTownChat(false);
					CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_town_leaveTownChat"));
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_leaveSuccess", town.getName()));
				CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_leaveBroadcast", resident.getName()));
				resident.save();
				town.save();
			}
		}));
		add(new CustomCommand("show").withDescription(CivSettings.localize.localizedString("cmd_town_showDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				if (sender instanceof Player) {
					TownInfoCommand.show(sender, Commander.getResident(sender), town, town.getCiv());
				} else {
					TownInfoCommand.show(sender, null, town, town.getCiv());
				}

				try {
					Civilization civ = Commander.getSenderCiv(sender);
					if (town.getCiv() != civ) {
						if (sender instanceof Player) {
							Player player = (Player) sender;
							Location ourCapLoc = civ.getCapitolLocation();
							if (ourCapLoc == null) throw new CivException("Capitol not found error");
							double potentialDistanceLow;
							double potentialDistanceHigh;
							try {
								if (!town.isValid()) {
									Location theirTownHallLoc = town.getLocation();
									potentialDistanceLow = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, true);
									potentialDistanceHigh = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, false);

									CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_showCost1", potentialDistanceLow));
									CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_showCost3"));
									CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_town_showCost3", potentialDistanceHigh, CivSettings.CURRENCY_NAME));
								} else {
									CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_showNoTownHall"));
								}
							} catch (InvalidConfiguration e) {
								e.printStackTrace();
								CivMessage.sendError(sender, CivSettings.localize.localizedString("internalException"));
							}
						}
					}
				} catch (CivException e) {
					// Playe not part of a civ, thats ok dont show anything.
				}
			}
		}));
		add(new CustomCommand("evict").withDescription(CivSettings.localize.localizedString("cmd_town_evictDesc")).withTabCompleter(new ResidentInTownTaber()).withValidator(Validators.validMayor).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Resident resident = Commander.getResident(sender);
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_town_evictPrompt"));
				Resident residentToKick = Commander.getNamedResident(args, 0);
				if (residentToKick.getTown() != town) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_evictNotInTown", args[1]));
				if (town.GM.isMayorOrAssistant(residentToKick)) throw new CivException(CivSettings.localize.localizedString("cmd_town_evictDemoteFirst"));
				town.removeResident(residentToKick);
				CivMessage.send(residentToKick, CivColor.Yellow + CivSettings.localize.localizedString("cmd_town_evictAlert"));
				CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_evictSuccess1", residentToKick.getName(), resident.getName()));
			}
		}));
		// add(new CustomCommand("list").withDescription(CivSettings.localize.localizedString("cmd_town_listDesc")).withExecutor(new
		// CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// String out = "";
		// CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_listHeading"));
		// for (Town town : CivGlobal.getTowns()) {
		// out += town.getName() + "(" + town.getCiv().getName() + ")" + ", ";
		// }
		// CivMessage.send(sender, out);
		// }
		// }));
		// add(new CustomCommand("top5").withDescription(CivSettings.localize.localizedString("cmd_town_top5Desc")).withExecutor(new
		// CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_top5Heading"));
		// // TreeMap<Integer, Town> scores = new TreeMap<Integer, Town>();
		// // for (Town town : CivGlobal.getTowns()) {
		// // if (town.getCiv().isAdminCiv()) continue;
		// // scores.put(town.getScore(), town);
		// // }
		// synchronized (CivGlobal.townScores) {
		// int i = 1;
		// for (Integer score : CivGlobal.townScores.descendingKeySet()) {
		// CivMessage.send(sender, i + ") " + CivColor.Gold + CivGlobal.townScores.get(score).getName() + CivColor.White + " - " + score);
		// i++;
		// if (i > 5) break;
		// }
		// }
		// }
		// }));
		add(new CustomCommand("disbandtown").withDescription(CivSettings.localize.localizedString("cmd_town_disbandtownDesc")).withValidator(Validators.validMayor).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (town.getMotherCiv() != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_disbandtownConquered"));
				if (town.isCapitol()) throw new CivException(CivSettings.localize.localizedString("cmd_town_disbandtownCapitol"));
				if (town.mayorWantsToDisband) {
					town.mayorWantsToDisband = false;
					throw new CivException(CivSettings.localize.localizedString("cmd_civ_disbandtownErrorLeader"));
				}
				town.mayorWantsToDisband = true;
				if (town.leaderWantsToDisband && town.mayorWantsToDisband) {
					CivMessage.sendCiv(town.getCiv(), CivSettings.localize.localizedString("Town") + " " + town.getName() + " " + CivSettings.localize.localizedString("cmd_civ_disbandtownSuccess"));
					town.delete();
				}
				CivMessage.send(sender, CivSettings.localize.localizedString("cmd_town_disbandtownSuccess"));
			}
		}));
		add(new TownOutlawCommand("outlaw").withDescription(CivSettings.localize.localizedString("cmd_town_outlawDesc")));
		add(new CustomCommand("leavegroup").withDescription(CivSettings.localize.localizedString("cmd_town_leavegroupDesc")).withExecutor(new CustomExecutor() {
			// TODO Переместить в групы
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				PermissionGroup grp = Commander.getNamedPermissionGroup(town, args, 1);
				Resident resident = Commander.getResident(sender);
				if (!grp.hasMember(resident)) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_leavegroupNotIn1", grp.getName(), town.getName()));
				if (grp == town.GM.getMayorGroup() && town.GM.isOneMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_leavegroupLastMayor"));
				if (grp == town.getCiv().GM.leadersGroup && town.getCiv().GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_leavegroupLastLead"));
				grp.removeMember(resident);
				grp.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_leavegroupSuccess", grp.getName(), town.getName()));
			}
		}));
		add(new CustomCommand("select").withDescription(CivSettings.localize.localizedString("cmd_town_selectDesc")).withValidator(Validators.validLeaderAdvisor).withTabCompleter(new TownInCivTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Town selectTown = Commander.getNamedTown(args, 0);
				if (resident.getSelectedTown() == null && resident.getTown() == selectTown) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_selectedAlready", selectTown.getName()));
				if (resident.getSelectedTown() == selectTown) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_selectedAlready", selectTown.getName()));
				selectTown.validateResidentSelect(resident);
				resident.setSelectedTown(selectTown);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_selecteSuccess", selectTown.getName()));
			}
		}));
		// add(new CustomCommand("capture").withDescription("[town] - instantly captures this town if they have a missing or illegally placed town
		// hall during WarTime.").withValidator(Validators.validLeaderAdvisor)
		// .withTabCompleter(new TownInCivTaber(this)).withExecutor(new CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// if (!War.isWarTime()) throw new CivException("Can only use this command during war time.");
		// Town town = Commander.getNamedTown(args, 0);
		// Civilization civ = Commander.getSenderCiv(sender);
		// if (town.getCiv().isAdminCiv()) throw new CivException("Cannot capture spawn town.");
		// if (!town.isValid()) throw new CivException("Cannot capture, this town has a valid town hall.");
		// if (town.claimed) throw new CivException("Town has already been claimed this war time.");
		// if (town.getMotherCiv() != null) throw new CivException("Cannot capture a town already captured by another civ!");
		// if (town.isCapitol()) {
		// town.getCiv().onDefeat(civ);
		// CivMessage.global("The capitol civilization of " + town.getCiv().getName() + " had an illegal or missing town hall and was claimed by " +
		// civ.getName());
		// } else {
		// town.onDefeat(civ);
		// CivMessage.global("The town of " + town.getName() + " had an illegal or missing town hall and was claimed by " + civ.getName());
		// }
		// town.claimed = true;
		// }
		// }));
		add(new CustomCommand("capitulate").withDescription(CivSettings.localize.localizedString("cmd_town_capitulateDesc")).withValidator(Validators.ValidMotherCiv).withValidator(Validators.validMayor).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (town.getMotherCiv() == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_capitulateErrorNoMother"));
				if (town.getMotherCiv().getCapitolId() == town.getId()) throw new CivException(CivSettings.localize.localizedString("cmd_town_capitulateCapitol"));
				if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
					CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_town_capitulatePrompt1", town.getCiv().getName()));
					CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_town_capitulateConfirm"));
					return;
				}
				/* Town is capitulating, no longer need a mother civ. */
				town.setMotherCiv(null);
				town.save();
				CivMessage.global(CivSettings.localize.localizedString("var_cmd_town_capitulateSuccess1", town.getName(), town.getCiv().getName()));
			}
		}));
		add(new CustomCommand("survey").withDescription(CivSettings.localize.localizedString("cmd_town_surveyDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				CivMessage.send(player, survey(player.getLocation()));
			}
		}));
		add(new TownEventCommand("event").withDescription(CivSettings.localize.localizedString("cmd_town_eventDesc")).withValidator(Validators.validHasTown));
		add(new CustomCommand("claimmayor").withDescription(CivSettings.localize.localizedString("cmd_town_claimmayorDesc")).withValidator(Validators.validHasTown).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Resident resident = Commander.getResident(sender);
				if (resident.getTown() != town) throw new CivException(CivSettings.localize.localizedString("cmd_town_claimMayorNotInTown"));
				if (!town.areMayorsInactive()) throw new CivException(CivSettings.localize.localizedString("cmd_town_claimMayorNotInactive"));
				town.GM.addMayor(resident);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_claimmayorSuccess", town.getName()));
				CivMessage.sendTown(town, CivSettings.localize.localizedString("var_cmd_town_claimmayorSuccess2", resident.getName()));
			}
		}));
		add(new CustomCommand("movestructure").withDescription("[coord] [town] moves the structure specified by the coord to the specfied town.").withValidator(Validators.validLeader).withExecutor(new CustomExecutor() {
			// .withTabCompleter(new )//TODO
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				String coordString = Commander.getNamedString(args, 0, "Coordinate of structure. Example: world,555,65,444");
				Town targetTown = Commander.getNamedTown(args, 1);
				Structure struct;
				try {
					struct = CivGlobal.getStructure(new BlockCoord(coordString));
				} catch (Exception e) {
					throw new CivException("Invalid structure coordinate. Example: world,555,65,444");
				}
				if (struct instanceof Cityhall) throw new CivException("Cannot move town halls or capitols.");
				if (War.isWarTime()) throw new CivException("Cannot move structures during war time.");
				if (struct == null) throw new CivException("Structure at:" + coordString + " is not found.");
				if (town.getCiv() != targetTown.getCiv()) throw new CivException("You can only move structures between towns in your own civ.");
				town.BM.removeBuildable(struct);
				targetTown.BM.addBuildable(struct);
				struct.setSQLOwner(targetTown);
				struct.save();
				CivMessage.sendSuccess(sender, "Moved structure " + coordString + " to town " + targetTown.getName());
			}
		}));
		add(new CustomCommand("enablestructure").withDescription(CivSettings.localize.localizedString("cmd_town_enableStructureDesc")).withValidator(Validators.validMayorAssistantLeader)
				// .withTabCompleter(new )//TODO
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getSelectedTown(sender);
						Resident resident = Commander.getResident(sender);
						String coordString = Commander.getNamedString(args, 0, CivSettings.localize.localizedString("cmd_town_enableStructurePrompt"));
						Structure struct;
						try {
							struct = CivGlobal.getStructure(new BlockCoord(coordString));
						} catch (Exception e) {
							throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureInvalid"));
						}

						if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureWar"));
						if (struct == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_enableStructureNotFound", coordString));
						if (!resident.getCiv().GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureNotLead"));
						if (!town.BM.isStructureAddable(struct)) throw new CivException(CivSettings.localize.localizedString("cmd_town_enableStructureOverLimit"));

						/* Readding structure will make it valid. */
						town.BM.removeBuildable(struct);
						town.BM.addBuildable(struct);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_enableStructureSuccess"));
					}
				}));
		add(new CustomCommand("location").withAliases("l").withDescription(CivSettings.localize.localizedString("cmd_town_locationDesc")).withValidator(Validators.validHasTown).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Resident resident = Commander.getResident(sender);
				if (resident.getTown() == town) {
					if (!town.isValid()) {
						CivMessage.send(sender, CivColor.LightGreen + CivColor.BOLD + town.getName() + " - " + CivColor.Rose + CivColor.BOLD + CivSettings.localize.localizedString("cmd_civ_locationMissingTownHall"));
					} else {
						CivMessage.send(sender,
								CivColor.LightGreen + CivColor.BOLD + town.getName() + " - " + CivColor.LightGreen + CivSettings.localize.localizedString("Location") + " " + CivColor.LightPurple + new BlockCoord(town.getLocation()));
					}
				}
			}
		}));
		add(new CustomCommand("changetown").withDescription(CivSettings.localize.localizedString("cmd_town_switchtown")).withValidator(Validators.validHasTown).withTabCompleter(new TownInCivTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Resident resident = Commander.getResident(sender);
				final Town town = Commander.getNamedTown(args, 0);
				final Player player = Commander.getPlayer(sender);
				if (War.isWarTime()) throw new CivException("§c" + CivSettings.localize.localizedString("wartime_now_cenceled"));
				if (resident.getTown() == town) throw new CivException(CivSettings.localize.localizedString("var_switchtown_own"));
				if (resident.getTown().getMotherCiv() != town.getMotherCiv()) throw new CivException(CivSettings.localize.localizedString("var_switchtown_captured"));
				if (town.getCiv() != resident.getCiv()) throw new CivException(CivSettings.localize.localizedString("var_switchtown_now_own"));
				if (town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("var_switchtown_last_mayor"));
				if (town.getResidents().size() == 1) throw new CivException(CivSettings.localize.localizedString("var_switchtown_lastResident", "§6" + town.getName() + "§c"));
				ChangeTownRequest request = new ChangeTownRequest();
				request.resident = resident;
				request.from = resident.getTown();
				request.to = town;
				request.civ = resident.getCiv();
				final String fullPlayerName = player.getDisplayName();
				try {
					Question.questionLeaders(player, resident.getCiv(), CivSettings.localize.localizedString("var_changetownrequest_requestMessage", fullPlayerName, "§c" + resident.getTown().getName(), "§c" + town.getName()), 30000L,
							request);
					CivMessage.send(sender, "§7" + CivSettings.localize.localizedString("var_switchtown_pleaseWait"));
				} catch (CivException e) {
					CivMessage.sendError(player, e.getMessage());
				}
			}
		}));
		add(new CustomCommand("teleport").withAliases("tp").withDescription(CivSettings.localize.localizedString("cmd_town_teleportDesc")).withValidator(Validators.validHasTown).withTabCompleter(new TownInCivTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						// final Player player = this.getPlayer();
						final Resident resident = Commander.getResident(sender);
						final Town town = Commander.getNamedTown(args, 0);

						if (War.isWarTime()) throw new CivException("§c" + CivSettings.localize.localizedString("wartime_now_cenceled"));
						if (resident.getTown().getMotherCiv() != town.getMotherCiv()) throw new CivException(CivSettings.localize.localizedString("var_teleport_motherCivNotNull"));
						if (town.getCiv() != resident.getCiv()) throw new CivException(CivSettings.localize.localizedString("var_teleport_NotYourCiv", "§a" + town.getCiv().getName() + "§c"));
						if (!resident.getTreasury().hasEnough(5000.0))
							throw new CivException(CivSettings.localize.localizedString("var_teleport_notEnoughMoney", "§a" + (5000 - (int) resident.getTreasury().getBalance()) + "§c",
									"§c" + CivMessage.plurals(5000 - (int) resident.getTreasury().getBalance(), "монета", "монеты", "монет")));
						final long nextUse = CivGlobal.getTeleportCooldown("teleportCommand", resident);
						final long timeNow = Calendar.getInstance().getTimeInMillis();
						if (nextUse > timeNow) throw new CivException(CivSettings.localize.localizedString("var_teleport_cooldown", "§6" + CivGlobal.dateFormat.format(nextUse)));
						final TeleportPlayerTaskTown teleportPlayerTask = new TeleportPlayerTaskTown(resident, town.getCityhall().getRandomRevivePoint().getLocation(), resident.getTown());
						teleportPlayerTask.run(true);
					}
				}));
		add(new CustomCommand("getunit").withAliases("unit", "gu").withDescription("Открыть сундук с юнитами").withValidator(Validators.validMayor).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Commander.getSelectedTown(sender).unitInventory.showUnits(Commander.getPlayer(sender));
			}
		}));
		add(new TownPeopleCommand("people").withDescription("Управление населением города").withValidator(Validators.validMayor));
	}

	public static ArrayList<String> survey(Location loc) {
		ChunkCoord start = new ChunkCoord(loc);
		ConfigCultureLevel lvl = CivSettings.cultureLevels.get(1);

		ArrayList<String> outList = new ArrayList<String>();

		Queue<ChunkCoord> closedSet = new LinkedList<ChunkCoord>();
		Queue<ChunkCoord> openSet = new LinkedList<ChunkCoord>();
		openSet.add(start);
		/* Try to get the surrounding chunks and get their biome info. */
		// Enqueue all neighbors.
		while (!openSet.isEmpty()) {
			ChunkCoord node = openSet.poll();

			if (closedSet.contains(node)) continue;
			if (node.manhattanDistance(start) > lvl.chunks) continue;

			closedSet.add(node);

			// Enqueue all neighbors.
			int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
			for (int i = 0; i < 4; i++) {
				ChunkCoord nextCoord = node.getRelative(offset[i][0], offset[i][1]);
				if (closedSet.contains(nextCoord)) continue;
				openSet.add(nextCoord);
			}
		}

		HashMap<String, Integer> biomes = new HashMap<String, Integer>();

		// double coins = 0.0;
		double hammers = 0.0;
		double growth = 0.0;
		double happiness = 0.0;
		DecimalFormat df = new DecimalFormat();

		for (ChunkCoord c : closedSet) {
			/* Increment biome counts. */
			Biome biome = c.getChunk().getWorld().getBiome(c.getX() * 16, c.getZ() * 16);

			if (!biomes.containsKey(biome.name())) {
				biomes.put(biome.name(), 1);
			} else {
				Integer value = biomes.get(biome.name());
				biomes.put(biome.name(), value + 1);
			}

			ConfigBiomeInfo info = CivSettings.getCultureBiome(biome);

			// coins += info.coins;
			hammers += info.getHammers();
			growth += info.getGrowth();
			happiness += info.beauty ? 1 : 0;
		}

		outList.add(CivColor.LightBlue + CivSettings.localize.localizedString("cmd_town_biomeList"));
		// int totalBiomes = 0;
		String out = "";
		for (String biome : biomes.keySet()) {
			Integer count = biomes.get(biome);
			out += CivColor.Green + biome + ": " + CivColor.LightGreen + count + CivColor.Green + ", ";
			// totalBiomes += count;
		}
		outList.add(out);
		// outList.add(CivColor.Green+"Biome Count: "+CivColor.LightGreen+totalBiomes);

		outList.add(CivColor.LightBlue + CivSettings.localize.localizedString("cmd_town_totals"));
		outList.add(CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_happiness") + " " + CivColor.LightGreen + df.format(happiness) + CivColor.Green + " " + CivSettings.localize.localizedString("Hammers") + " "
				+ CivColor.LightGreen + df.format(hammers) + CivColor.Green + " " + CivSettings.localize.localizedString("cmd_town_growth") + " " + CivColor.LightGreen + df.format(growth));
		return outList;
	}
}
