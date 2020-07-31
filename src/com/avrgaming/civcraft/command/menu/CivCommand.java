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
package com.avrgaming.civcraft.command.menu;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.command.taber.AllCivTaber;
import com.avrgaming.civcraft.command.taber.TownInCivTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
//import com.avrgaming.civcraft.construct.caves.CaveStatus.StatusType;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class CivCommand extends MenuAbstractCommand {

	public CivCommand() {
		super("civ");
		displayName = CivSettings.localize.localizedString("cmd_civ_name");

		add(new CustomCommand("townlist").withDescription(CivSettings.localize.localizedString("cmd_civ_townlistDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				CivMessage.sendHeading(sender, civ.getName() + " " + CivSettings.localize.localizedString("cmd_civ_townListHeading"));
				String out = "";
				for (Town town : civ.getTowns()) {
					out += town.getName() + ",";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("deposit").withAliases("d").withDescription(CivSettings.localize.localizedString("cmd_civ_depositDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_despositPrompt"));
				Resident resident = Commander.getResident(sender);
				Civilization civ = Commander.getSenderCiv(sender);
				try {
					Double amount = Double.valueOf(args[0]);
					if (amount < 1) throw new CivException(amount + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
					amount = Math.floor(amount);
					civ.depositFromResident(resident, Double.valueOf(args[1]));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				} catch (SQLException e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("Deposited") + args[1] + " " + CivSettings.CURRENCY_NAME);
			}
		}));
		add(new CustomCommand("withdraw").withAliases("w").withDescription(CivSettings.localize.localizedString("cmd_civ_withdrawDesc")).withValidator(Validators.validLeader).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_withdrawPrompt"));
				Civilization civ = Commander.getSenderCiv(sender);
				Resident resident = Commander.getResident(sender);
				try {
					Double amount = Double.valueOf(args[0]);
					if (amount < 1) throw new CivException(amount + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
					amount = Math.floor(amount);
					if (!civ.getTreasury().payTo(resident.getTreasury(), Double.valueOf(args[1]))) throw new CivException(CivSettings.localize.localizedString("cmd_civ_withdrawTooPoor"));
				} catch (NumberFormatException e) {
					throw new CivException(args[1] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess", args[1], CivSettings.CURRENCY_NAME));
			}
		}));
		add(new CivInfoCommand().withAliases("i").withDescription(CivSettings.localize.localizedString("cmd_civ_infoDesc")));
		add(new CustomCommand("show").withAliases("s").withDescription(CivSettings.localize.localizedString("cmd_civ_showDesc")).withTabCompleter(new AllCivTaber()).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_showPrompt"));
				Civilization civ = Commander.getNamedCiv(args, 0);
				if (sender instanceof Player)
					CivInfoCommand.show(sender, Commander.getResident(sender), civ);
				else
					CivInfoCommand.show(sender, null, civ);
			}
		}));
		add(new CustomCommand("list").withAliases("l").withDescription(CivSettings.localize.localizedString("cmd_civ_listDesc")).withTabCompleter(new AllCivTaber()).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) {
					String out = "";
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_listHeading"));
					for (Civilization civ : CivGlobal.getCivs()) {
						out += civ.getName() + ", ";
					}
					CivMessage.send(sender, out);
					return;
				}
				Civilization civ = Commander.getNamedCiv(args, 0);
				String out = "";
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_listtowns", args[1]));
				for (Town t : civ.getTowns()) {
					out += t.getName() + ", ";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CivResearchCommand().withAliases("r").withDescription(CivSettings.localize.localizedString("cmd_civ_researchDesc")));
		add(new CivGovCommand().withAliases("g").withDescription(CivSettings.localize.localizedString("cmd_civ_govDesc")));
		add(new CustomCommand("time").withAliases("t").withDescription(CivSettings.localize.localizedString("cmd_civ_timeDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_timeHeading"));
				Resident resident = Commander.getResident(sender);
				ArrayList<String> out = new ArrayList<String>();
				SimpleDateFormat sdf = CivGlobal.dateFormat;
				Calendar cal = Calendar.getInstance();
				cal.setTimeZone(TimeZone.getTimeZone(resident.getTimezone()));
				sdf.setTimeZone(cal.getTimeZone());
				out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_civ_timeServer") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
				cal.setTime(CivGlobal.getNextUpkeepDate());
				out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_civ_timeUpkeep") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
				cal.setTime(CivGlobal.getNextHourlyTickDate());
				out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_civ_timeHourly") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
				if (War.isWarTime()) {
					out.add(CivColor.Yellow + CivSettings.localize.localizedString("cmd_civ_timeWarNow"));
					cal.setTime(War.getStart());
					out.add(CivColor.Yellow + CivSettings.localize.localizedString("cmd_civ_timeWarStarted") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
					cal.setTime(War.getEnd());
					out.add(CivColor.Yellow + CivSettings.localize.localizedString("cmd_civ_timeWarEnds") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
				} else {
					cal.setTime(War.getNextWarTime());
					out.add(CivColor.Green + CivSettings.localize.localizedString("cmd_civ_timeWarNext") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
				}
				out.add("§7" + CivSettings.localize.localizedString("cmd_civ_timeCantDemolishHelp"));

				Player player = Commander.getPlayer(sender);
				if (player == null || player.hasPermission(CivSettings.MINI_ADMIN) || player.isOp()) {
					cal.setTime(CivGlobal.getTodaysSpawnRegenDate());
					out.add(CivColor.LightPurple + CivSettings.localize.localizedString("cmd_civ_timeSpawnRegen") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
					cal.setTime(CivGlobal.getNextRandomEventTime());
					out.add(CivColor.LightPurple + CivSettings.localize.localizedString("cmd_civ_timeRandomEvent") + " " + CivColor.LightGreen + sdf.format(cal.getTime()));
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CivSetCommand().withDescription(CivSettings.localize.localizedString("cmd_civ_setDesc")));
		add(new CivGroupCommand().withDescription(CivSettings.localize.localizedString("cmd_civ_groupDesc")));
		add(new CivDiplomacyCommand().withDescription(CivSettings.localize.localizedString("cmd_civ_dipDesc")));
		add(new CustomCommand("victory").withDescription(CivSettings.localize.localizedString("cmd_civ_victoryDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_victoryHeading"));
				boolean anybody = false;
				for (EndGameCondition endCond : EndGameCondition.endConditions) {
					ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(endCond.getSessionKey());
					if (entries.size() == 0) continue;
					anybody = true;
					for (SessionEntry entry : entries) {
						Civilization civ = EndGameCondition.getCivFromSessionData(entry.value);
						Integer daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);
						CivMessage.send(sender, CivColor.LightBlue + CivColor.BOLD + civ.getName() + CivColor.White + ": " + CivSettings.localize.localizedString("var_cmd_civ_victoryDays",
								(CivColor.Yellow + CivColor.BOLD + daysLeft + CivColor.White), (CivColor.LightPurple + CivColor.BOLD + endCond.getVictoryName() + CivColor.White)));
					}
				}
				if (!anybody) CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_civ_victoryNoOne"));
			}
		}));
		add(new CustomCommand("vote").withDescription(CivSettings.localize.localizedString("cmd_civ_voteDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_voteHeading"));
				if (sender instanceof Player) {
					Player player = (Player) sender;
					Resident resident = CivGlobal.getResident(player);
					if (!resident.hasTown()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_voteNotInTown"));
					Civilization civ = CivGlobal.getCivFromName(args[0]);
					if (civ == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_voteInvalidCiv", args[1]));
					if (!EndConditionDiplomacy.canPeopleVote()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_voteNoCouncil"));
					EndConditionDiplomacy.addVote(civ, resident);
				}
			}
		}));
		add(new CustomCommand("votes").withDescription(CivSettings.localize.localizedString("cmd_civ_votesDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_votesHeading"));
				for (Civilization civ : CivGlobal.getCivs()) {
					Integer votes = EndConditionDiplomacy.getVotesFor(civ);
					if (votes != 0)
						CivMessage.send(sender,
								CivColor.LightBlue + CivColor.BOLD + civ.getName() + CivColor.White + ": " + CivColor.LightPurple + CivColor.BOLD + votes + CivColor.White + " " + CivSettings.localize.localizedString("cmd_civ_votes"));
				}
			}
		}));
		add(new CustomCommand("top5").withDescription(CivSettings.localize.localizedString("cmd_civ_top5Desc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_top5Heading"));
				synchronized (CivGlobal.civilizationScores) {
					int i = 1;
					for (Integer score : CivGlobal.civilizationScores.descendingKeySet()) {
						CivMessage.send(sender, i + ") " + CivColor.Gold + CivGlobal.civilizationScores.get(score).getName() + CivColor.White + " - " + score);
						i++;
						if (i > 5) break;
					}
				}
			}
		}));
		add(new CustomCommand("disbandtown").withDescription(CivSettings.localize.localizedString("cmd_civ_disbandtownDesc")).withTabCompleter(new TownInCivTaber()).withValidator(Validators.validLeader)
				.withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Town town = Commander.getNamedTown(args, 0);
						if (town.getMotherCiv() != null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_disbandtownError"));
						if (town.leaderWantsToDisband) {
							town.leaderWantsToDisband = false;
							CivMessage.send(sender, CivSettings.localize.localizedString("cmd_civ_disbandtownErrorLeader"));
							return;
						}
						town.leaderWantsToDisband = true;
						if (town.leaderWantsToDisband && town.mayorWantsToDisband) {
							CivMessage.sendCiv(town.getCiv(), CivSettings.localize.localizedString("var_cmd_civ_disbandtownSuccess", town.getName()));
							town.delete();
						}
						CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_civ_disbandtownPrompt"));
					}
				}));
		add(new CustomCommand("revolution").withDescription(CivSettings.localize.localizedString("cmd_civ_revolutionDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (War.isWarTime() || War.isWithinWarDeclareDays()) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorWar1", War.time_declare_days));
				if (town.getMotherCiv() == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_revolutionErrorNoMother"));
				Civilization motherCiv = town.getMotherCiv();
				if (!(motherCiv.getCapitolId() == town.getId())) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorNotCapitol", motherCiv.getCapitol().getName()));
				try {
					int revolution_cooldown = CivSettings.getInteger(CivSettings.civConfig, "civ.revolution_cooldown");
					Calendar cal = Calendar.getInstance();
					Calendar revCal = Calendar.getInstance();
					Date conquered = town.getMotherCiv().getConquer_date();
					if (conquered == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_revolutionErrorNoMother"));
					revCal.setTime(town.getMotherCiv().getConquer_date());
					revCal.add(Calendar.DAY_OF_MONTH, revolution_cooldown);
					if (!cal.after(revCal)) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorTooSoon", revolution_cooldown));
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("internalException"));
				}
				double revolutionFee = motherCiv.getRevolutionFee();
				if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
					CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_revolutionConfirm1", revolutionFee, CivSettings.CURRENCY_NAME));
					CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_civ_revolutionConfirm2"));
					CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_civ_revolutionConfirm3"));
					CivMessage.send(sender, CivColor.LightGreen + CivSettings.localize.localizedString("cmd_civ_revolutionConfirm4"));
					return;
				}
				if (!town.getTreasury().hasEnough(revolutionFee)) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_revolutionErrorTooPoor", revolutionFee, CivSettings.CURRENCY_NAME));
				/* Starting a revolution! Give back all of our towns to us. */
				HashSet<String> warCivs = new HashSet<String>();
				for (Town t : CivGlobal.getTowns()) {
					if (t.getMotherCiv() == motherCiv) {
						warCivs.add(t.getCiv().getName());
						t.changeCiv(motherCiv);
						t.setMotherCiv(null);
						t.save();
					}
				}
				for (String warCivName : warCivs) {
					Civilization civ = CivGlobal.getCivFromName(warCivName);
					if (civ != null) {
						CivGlobal.setRelation(civ, motherCiv, Status.WAR);
						/* THEY are the aggressor in a revolution. */
						CivGlobal.setAggressor(civ, motherCiv, civ);
					}
				}
				motherCiv.setConquered(false);
				CivGlobal.removeConqueredCiv(motherCiv);
				CivGlobal.addCiv(motherCiv);
				motherCiv.save();
				town.getTreasury().withdraw(revolutionFee);
				CivMessage.global(CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_revolutionSuccess1", motherCiv.getName()));
			}
		}));
		add(new CustomCommand("claimleader").withDescription(CivSettings.localize.localizedString("cmd_civ_claimleaderDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				Resident resident = Commander.getResident(sender);
				if (!civ.GM.areLeaderInactive()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_claimleaderStillActive"));
				civ.GM.addLeader(resident);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_claimLeaderSuccess", civ.getName()));
				CivMessage.sendCiv(civ, CivSettings.localize.localizedString("var_cmd_civ_claimLeaderBroadcast", resident.getName()));
			}
		}));
		add(new CivMotdCommand().withDescription(CivSettings.localize.localizedString("cmd_civ_motdDesc")));
		add(new CustomCommand("location").withDescription(CivSettings.localize.localizedString("cmd_civ_locationDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				Resident resident = Commander.getResident(sender);
				if (resident.getCiv() == civ) {
					for (Town town : civ.getTowns()) {
						String name = town.getName();
						if (!town.isValid())
							CivMessage.send(sender, CivColor.Rose + CivColor.BOLD + name + CivColor.RESET + CivColor.Gray + CivSettings.localize.localizedString("cmd_civ_locationMissingTownHall"));
						else
							CivMessage.send(sender, CivColor.Rose + CivColor.BOLD + name + CivColor.LightPurple + " - " + CivSettings.localize.localizedString("cmd_civ_locationSuccess") + " " + town.getLocation());
					}
				}
			}
		}));
		add(new CustomCommand("members").withDescription(CivSettings.localize.localizedString("cmd_civ_membersDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Civilization civ = Commander.getSenderCiv(sender);
				String out = "";
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_civ_membersHeading", civ.getName()));
				for (final Town t : civ.getTowns()) {
					for (Resident r : t.getResidents()) {
						out += r.getName() + ", ";
					}
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CivSpaceCommand().withDescription(CivSettings.localize.localizedString("cmd_civ_space_name")));
		add(new CustomCommand("culture").withDescription(CivSettings.localize.localizedString("cmd_civ_culture_name")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Resident resident = Commander.getResident(sender);
				final Civilization civ = Commander.getSenderCiv(sender);
				if (!civ.GM.isLeaderOrAdviser(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_culture_notLeader"));
				boolean hasBurj = false;
				int cultureSummary = 0;
				for (final Town town : civ.getTowns()) {
					if (town.getMotherCiv() == null) cultureSummary += town.SM.getCulture();
					if (town.BM.hasWonder("w_burj")) hasBurj = true;
				}
				final boolean culturePassed = cultureSummary > 16500000;
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_culture_heading"));
				if (culturePassed && hasBurj) {
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_culture_allConditionsPassed"));
					return;
				}
				final int summary = 16500000 - cultureSummary;
				if (!culturePassed) CivMessage.send(sender, "§2" + CivSettings.localize.localizedString("cmd_civ_culture_cultureRequired", CivColor.LightGreenBold + summary + "§2"));
				if (!hasBurj) CivMessage.send(sender, "§2" + CivSettings.localize.localizedString("cmd_civ_culture_burjRequired", CivColor.LightGreenBold + "Burj Kalifa"));
			}
		}));
		add(new CivMarketCommand());
		// add(new CustomCommand("caves").withDescription("[all,founded,available,captured,lost,updated,used] Показать информаци о
		// пещерах").withExecutor(new CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// Player player = Commander.getPlayer(sender);
		// if (args.length < 2 || args[1].equalsIgnoreCase("all")) {
		// CivGlobal.getResident(player).getCiv().showCaveStatus(player, null);
		// return;
		// }
		// StatusType statusType = StatusType.valueOf(args[1]);
		// CivGlobal.getResident(player).getCiv().showCaveStatus(player, statusType);
		// }
		// }));
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		showBasicHelp(sender);
	}

	class CivGovCommand extends MenuAbstractCommand {

		public CivGovCommand() {
			super("gov");
			displayName = CivSettings.localize.localizedString("cmd_civ_gov_name");

			add(new CustomCommand("info").withDescription(CivSettings.localize.localizedString("cmd_civ_gov_infoDesc")).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Civilization civ = Commander.getSenderCiv(sender);

					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_gov_infoHading") + " " + civ.getGovernment().displayName);
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoTrade") + " "//
							+ CivColor.LightGreen + civ.getGovernment().trade_rate + CivColor.Green + " "//
							+ CivSettings.localize.localizedString("cmd_civ_gov_infoCottage") + " "// + CivColor.LightGreen
							+ civ.getGovernment().cottage_rate);
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoUpkeep") + " "//
							+ CivColor.LightGreen + civ.getGovernment().upkeep_rate + CivColor.Green + " "//
							+ CivSettings.localize.localizedString("cmd_civ_gov_infoGrowth") + " "// + CivColor.LightGreen
							+ civ.getGovernment().growth_rate);
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoHammer") + " "//
							+ CivColor.LightGreen + civ.getGovernment().hammer_rate + CivColor.Green + " "//
							+ CivSettings.localize.localizedString("cmd_civ_gov_infoBeaker") + " "// + CivColor.LightGreen
							+ civ.getGovernment().beaker_rate);
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_civ_gov_infoCulture") + " "//
							+ CivColor.LightGreen + civ.getGovernment().culture_rate + CivColor.Green + " "//
							+ CivSettings.localize.localizedString("cmd_civ_gov_infoMaxTax") + " "// + CivColor.LightGreen
							+ civ.getGovernment().maximum_tax_rate);
				}
			}));
			add(new CustomCommand("change").withDescription(CivSettings.localize.localizedString("cmd_civ_gov_changeDesc")).withTabCompleter(new AbstractCashedTaber() {
				@Override
				protected List<String> newTabList(String arg) {
					List<String> l = new ArrayList<>();
					for (ConfigGovernment gov : CivSettings.governments.values()) {
						if (gov.id.equalsIgnoreCase("gov_anarchy")) continue;
						if (gov.displayName.startsWith(arg.toLowerCase())) l.add(gov.displayName);
					}
					return l;
				}
			}).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Civilization civ = Commander.getSenderCiv(sender);
					if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_gov_changePrompt"));
					ConfigGovernment gov = ConfigGovernment.getGovernmentFromName(args[0]);
					if (gov == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_gov_changeInvalid") + " " + args[1]);
					if (!gov.isAvailable(civ)) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_gov_changeNotHere", gov.displayName));
					civ.changeGovernment(civ, gov, false);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_gov_changeSuccess"));
				}
			}));
			add(new CustomCommand("list").withDescription(CivSettings.localize.localizedString("cmd_civ_gov_listDesc")).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Civilization civ = Commander.getSenderCiv(sender);
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_gov_listHeading"));
					ArrayList<ConfigGovernment> govs = ConfigGovernment.getAvailableGovernments(civ);
					for (ConfigGovernment gov : govs) {
						if (gov == civ.getGovernment())
							CivMessage.send(sender, CivColor.Gold + gov.displayName + " " + "(" + CivSettings.localize.localizedString("currentGovernment") + ")");
						else
							CivMessage.send(sender, CivColor.Green + gov.displayName);
					}
				}
			}));
		}

		@Override
		public void doDefaultAction(CommandSender sender) throws CivException {
			showBasicHelp(sender);
		}
	}

	class CivMotdCommand extends MenuAbstractCommand {

		public CivMotdCommand() {
			super("motd");
			displayName = CivSettings.localize.localizedString("cmd_civ_motd_name");

			add(new CustomCommand("set").withDescription(CivSettings.localize.localizedString("cmd_civ_motd_setDesc")).withValidator(Validators.validLeaderAdvisor).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Civilization civ = Commander.getSenderCiv(sender);
					if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_motd_setPrompt"));
					String motd = Commander.combineArgs(Commander.stripArgs(args, 1));
					civ.setMessageOfTheDay(motd);
					civ.save();
					CivMessage.sendCiv(civ, "MOTD:" + " " + motd);
				}
			}));
			add(new CustomCommand("remove").withDescription(CivSettings.localize.localizedString("cmd_civ_motd_removeDesc")).withValidator(Validators.validLeaderAdvisor).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Civilization civ = Commander.getSenderCiv(sender);
					civ.setMessageOfTheDay(null);
					civ.save();
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_motd_removeSuccess"));
				}
			}));
		}

		@Override
		public void doDefaultAction(CommandSender sender) throws CivException {
			showBasicHelp(sender);
			Resident resident = Commander.getResident(sender);
			if (resident.getCiv().MOTD() != null) {
				CivMessage.send(sender, CivColor.LightPurple + "[Civ MOTD] " + CivColor.White + resident.getCiv().MOTD());
			} else {
				CivMessage.send(sender, CivColor.LightPurple + "[Civ MOTD] " + CivColor.White + CivSettings.localize.localizedString("cmd_civ_motd_noneSet"));
			}
		}
	}

}
