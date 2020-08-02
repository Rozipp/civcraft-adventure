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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.command.taber.TownInCivTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.questions.CapitulateRequest;
import com.avrgaming.civcraft.questions.ChangeRelationResponse;
import com.avrgaming.civcraft.questions.CivQuestionTask;
import com.avrgaming.civcraft.questions.DiplomacyGiftResponse;
import com.avrgaming.civcraft.questions.Question;
import com.avrgaming.civcraft.questions.QuestionResponseInterface;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class CivDiplomacyCommand extends MenuAbstractCommand {
	public static final long INVITE_TIMEOUT = 30000; // 30 seconds

	public CivDiplomacyCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_civ_dip_name");

		add(new CustomCommand("show").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_showDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1)
					show(sender, Commander.getSenderCiv(sender));
				else
					show(sender, Commander.getNamedCiv(args, 0));
			}
		}));
		add(new CustomCommand("declare").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_declareDesc")).withValidator(Validators.validLeaderAdvisor)
				.withTabCompleter(new CivInWorldTaber())
				.withTabCompleter(new AbstractCashedTaber() {
					@Override
					protected List<String> newTabList(String arg) {
						List<String> l = new ArrayList<>();
						if ("WAR".startsWith(arg.toUpperCase())) l.add("WAR");
						if ("HOSTILE".startsWith(arg.toUpperCase())) l.add("HOSTILE");
						return l;
					}
				}).withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Civilization ourCiv = Commander.getSenderCiv(sender);
						if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
						if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declarePrompt"));
						Civilization otherCiv = Commander.getNamedCiv(args, 0);
						if (ourCiv.getId() == otherCiv.getId()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareYourself"));
						if (otherCiv.isAdminCiv()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareAdmin"));
						try {
							Relation.Status status = Relation.Status.valueOf(args[1].toUpperCase());
							Relation.Status currentStatus = ourCiv.getDiplomacyManager().getRelationStatus(otherCiv);
							// boolean aidingAlly = false;

							if (currentStatus == status) throw new CivException(CivSettings.localize.localizedString("var_AlreadyStatusWithCiv", status.name(), otherCiv.getName()));

							switch (status) {
							case HOSTILE:
								if (currentStatus == Relation.Status.WAR) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_declareAtWar", status.name()));
								break;
							case WAR:
								if (CivGlobal.isCasualMode()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareCasual"));
								if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));

								if (War.isWithinWarDeclareDays()) {
									if (War.isCivAggressorToAlly(otherCiv, ourCiv)) {
										if (War.isWithinAllyDeclareHours()) {
											throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_declareTooCloseToWar1", War.getAllyDeclareHours()));
										} else {
											// aidingAlly = true;
										}
									} else {
										throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_declareTooCloseToWar2", War.time_declare_days));
									}
								}
								if (ourCiv.getTreasury().inDebt()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareInDebt"));
								break;
							default:
								throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareInvalid"));
							}

							CivGlobal.setRelation(ourCiv, otherCiv, status);
							// Boolean aidingAlly is in commentaries a couple lines higher (2 times)
							// if (aidingAlly) {
							// /* If we're aiding an ally, the other civ is the true aggressor. */
							// CivGlobal.setAggressor(otherCiv, ourCiv, otherCiv);
							// } else {
							// CivGlobal.setAggressor(ourCiv, otherCiv, ourCiv);
							// }
							CivGlobal.setAggressor(ourCiv, otherCiv, ourCiv);

						} catch (IllegalArgumentException e) {
							throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_declareUnknown"));
						}
					}
				}));
		add(new CustomCommand("request").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_requestDesc")).withValidator(Validators.validLeaderAdvisor).withTabCompleter(new CivInWorldTaber())
				.withTabCompleter(new AbstractCashedTaber() {
					@Override
					protected List<String> newTabList(String arg) {
						List<String> l = new ArrayList<>();
						if ("NEUTRAL".startsWith(arg.toUpperCase())) l.add("NEUTRAL");
						if ("PEACE".startsWith(arg.toUpperCase())) l.add("PEACE");
						if ("ALLY".startsWith(arg.toUpperCase())) l.add("ALLY");
						return l;
					}
				}).withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Civilization ourCiv = Commander.getSenderCiv(sender);
						if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
						if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestPrompt"));
						Civilization otherCiv = Commander.getNamedCiv(args, 0);
						if (ourCiv.getId() == otherCiv.getId()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestSameCiv"));
						try {
							Relation.Status status = Relation.Status.valueOf(args[2].toUpperCase());
							Relation.Status currentStatus = ourCiv.getDiplomacyManager().getRelationStatus(otherCiv);
							if (currentStatus == status) throw new CivException(CivSettings.localize.localizedString("var_AlreadyStatusWithCiv", status.name(), otherCiv.getName()));
							String message = CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_requestHasRequested", ourCiv.getName()) + " ";
							switch (status) {
							case NEUTRAL:
								message += CivSettings.localize.localizedString("cmd_civ_dip_requestNeutral");
								break;
							case PEACE:
								message += CivSettings.localize.localizedString("cmd_civ_dip_requestPeace");
								break;
							case ALLY:
								message += CivSettings.localize.localizedString("cmd_civ_dip_requestAlly");
								if (War.isWithinWarDeclareDays()) {
									if (ourCiv.getDiplomacyManager().isAtWar() || otherCiv.getDiplomacyManager().isAtWar())
										throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_dip_requestErrorWar1", War.time_declare_days));
								}
								break;
							case WAR:
								if (!CivGlobal.isCasualMode()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestErrorCasual"));
								message += CivSettings.localize.localizedString("cmd_civ_dip_requestWar");
								break;
							default:
								throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestPrompt"));
							}
							message += ". " + CivSettings.localize.localizedString("cmd_civ_dip_requestQuestion");
							ChangeRelationResponse relationresponse = new ChangeRelationResponse();
							relationresponse.fromCiv = ourCiv;
							relationresponse.toCiv = otherCiv;
							relationresponse.status = status;
							Question.requestRelation(ourCiv, otherCiv, message, INVITE_TIMEOUT, relationresponse);
							CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dip_requestSuccess"));
						} catch (IllegalArgumentException e) {
							throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_requestInvalid"));
						}
					}
				}));
		add(new CivDiplomacyGiftCommand().withDescription(CivSettings.localize.localizedString("cmd_civ_dip_giftDesc")));
		add(new CustomCommand("global").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_globalDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_globalHeading"));
				for (Civilization civ : CivGlobal.getCivs()) {
					for (Relation relation : civ.getDiplomacyManager().getRelations()) {
						CivMessage.send(sender, civ.getName() + ": " + relation.toString());
					}
				}
			}
		}));
		add(new CustomCommand("wars").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_warsDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_warsHeading"));
				HashSet<String> usedRelations = new HashSet<String>();
				for (Civilization civ : CivGlobal.getCivs()) {
					for (Relation relation : civ.getDiplomacyManager().getRelations()) {
						if (relation.getStatus().equals(Status.WAR)) {
							if (!usedRelations.contains(relation.getPairKey())) {
								CivMessage.send(sender, CivColor.LightBlue + CivColor.BOLD + relation.getCiv().getName() + CivColor.Rose + " <-- " + CivSettings.localize.localizedString("WAR") + " --> " + CivColor.LightBlue + CivColor.BOLD
										+ relation.getOtherCiv().getName());
								usedRelations.add(relation.getPairKey());
							}
						}
					}
				}
			}
		}));
		add(new CustomCommand("respond").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_respondDesc")).withValidator(Validators.validLeaderAdvisor).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_respondPrompt"));
				CivQuestionTask task = Question.getCivQuestionTask(Commander.getSenderCiv(sender));
				if (task == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_respondNoRequest"));
				if (args[0].equalsIgnoreCase("yes") || args[0].equalsIgnoreCase("accept")) {
					synchronized (task) {
						task.setResponse("accept");
						task.notifyAll();
					}
				} else
					if (args[0].equalsIgnoreCase("no") || args[0].equalsIgnoreCase("decline")) {
						synchronized (task) {
							task.setResponse("decline");
							task.notifyAll();
						}
					} else {
						throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_respondPrompt"));
					}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dip_respondSuccess"));
			}
		}));
		add(new CustomCommand("liberate").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_liberateDesc")).withValidator(Validators.ValidMotherCiv).withTabCompleter(new TownInCivTaber()).withValidator(Validators.validLeader).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
				Town town = Commander.getNamedTown(args, 0);
				Civilization civ = Commander.getSenderCiv(sender);
				Civilization motherCiv = town.getMotherCiv();
				if (motherCiv == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_liberateNotCaptured"));
				if (town.getId() == motherCiv.getCapitolId()) {
					Civilization capitolOwnerCiv = town.getCiv();
					/* Liberate the civ. */
					for (Town t : CivGlobal.getTowns()) {
						if (t.getMotherCiv() == motherCiv && t.getCiv() == capitolOwnerCiv) {
							t.changeCiv(motherCiv);
							t.setMotherCiv(null);
							t.save();
						}
					}
					motherCiv.setConquered(false);
					CivGlobal.removeConqueredCiv(motherCiv);
					CivGlobal.addCiv(motherCiv);
					motherCiv.save();
					CivMessage.global(CivSettings.localize.localizedString("var_cmd_civ_liberateSuccess1", motherCiv.getName(), civ.getName()));
				} else {
					if (motherCiv.isConquered()) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_liberateError1", town.getName()));
					/* Liberate just the town. */
					town.changeCiv(motherCiv);
					town.setMotherCiv(null);
					town.save();
					CivMessage.global(CivSettings.localize.localizedString("var_cmd_town_liberateSuccess", town.getName(), civ.getName(), motherCiv.getName()));
				}
			}
		}));
		add(new CustomCommand("capitulate").withDescription(CivSettings.localize.localizedString("cmd_civ_dip_capitulateDesc")).withValidator(Validators.validHasTown).withTabCompleter(new TownInCivTaber()).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_errorDuringWar"));
				Town town = Commander.getNamedTown(args, 0);
				Resident resident = Commander.getResident(sender);
				Civilization motherCiv = town.getMotherCiv();
				if (motherCiv == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_capitulateErrorNoMother"));
				if (!town.getMotherCiv().GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dip_capitulateErrorNotLeader"));
				boolean entireCiv = (town.getMotherCiv().getCapitolId() == town.getId());
				String requestMessage = "";
				CapitulateRequest capitulateResponse = new CapitulateRequest();
				if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
					if (entireCiv) {
						CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm1", town.getCiv().getName()));
						CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm3", town.getName()));
					} else {
						CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm1b", town.getCiv().getName()));
						CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateConfirm3", town.getName()));
					}
					return;
				}
				if (entireCiv) {
					requestMessage = CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dip_capitulateRequest1", motherCiv.getName());
					capitulateResponse.from = town.getMotherCiv().getName();
				} else {
					capitulateResponse.from = "Town of " + town.getName();
					requestMessage = CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("cmd_civ_dip_capitulateRequest1b", town.getName());
				}
				capitulateResponse.playerName = resident.getName();
				capitulateResponse.capitulator = town;
				capitulateResponse.to = town.getCiv().getName();
				Question.requestRelation(motherCiv, town.getCiv(), requestMessage, INVITE_TIMEOUT, capitulateResponse);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dip_capitulateSuccess"));
			}
		}));
	}

	public void show(CommandSender sender, Civilization civ) {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_dip_showHeading", CivColor.Yellow + civ.getName()));
		for (Relation relation : civ.getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Relation.Status.NEUTRAL) continue;
			CivMessage.send(sender, relation.toString());
		}
		int warCount = civ.getDiplomacyManager().getWarCount();
		if (warCount != 0) CivMessage.send(sender, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_civ_dip_showSuccess1", warCount));
		CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_civ_dip_showNeutral"));
	}

	class CivDiplomacyGiftCommand extends MenuAbstractCommand {

		public CivDiplomacyGiftCommand() {
			super("gift");
			displayName = CivSettings.localize.localizedString("cmd_civ_dipgift_name");
			this.withValidator(Validators.validLeader);

			add(new CustomCommand("entireciv").withDescription(CivSettings.localize.localizedString("cmd_civ_dipgift_entirecivDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Civilization fromCiv = Commander.getSenderCiv(sender);
					Civilization toCiv = Commander.getNamedCiv(args, 0);
					if (fromCiv == toCiv) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_entirecivSelf"));
					if (fromCiv.getDiplomacyManager().isAtWar() || toCiv.getDiplomacyManager().isAtWar()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_entirecivAtWar"));
					fromCiv.validateGift();
					toCiv.validateGift();
					if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_entirecivDuringWar"));
					if (War.isWithinWarDeclareDays())
						throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_entirecivClostToWar1") + " " + War.time_declare_days + " " + CivSettings.localize.localizedString("cmd_civ_dip_declareTooCloseToWar4"));
					DiplomacyGiftResponse dipResponse = new DiplomacyGiftResponse();
					dipResponse.giftedObject = fromCiv;
					dipResponse.fromCiv = fromCiv;
					dipResponse.toCiv = toCiv;
					sendGiftRequest(toCiv, fromCiv, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_cmd_civ_dipgift_entirecivRequest1", fromCiv.getName()) + " "
							+ CivSettings.localize.localizedString("var_cmd_civ_dipgift_entirecivRequest2", fromCiv.getMergeCost(), CivSettings.CURRENCY_NAME), dipResponse);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dipgift_entirecivSuccess"));
				}
			}));
			add(new CustomCommand("town").withDescription(CivSettings.localize.localizedString("cmd_civ_dipgift_townDesc")).withTabCompleter(new TownInCivTaber()).withTabCompleter(new CivInWorldTaber())
					.withExecutor(new CustonExecutor() {
						@Override
						public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
							Civilization fromCiv = Commander.getSenderCiv(sender);
							Town giftedTown = Commander.getNamedTown(args, 0);
							Civilization toCiv = Commander.getNamedCiv(args, 1);
							if (giftedTown.getCiv() != fromCiv) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_townNotYours"));
							if (giftedTown.getCiv() == toCiv) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_townNotInCiv"));
							if (giftedTown.getMotherCiv() != null && toCiv != giftedTown.getMotherCiv()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_townNotMother"));
							if (giftedTown.isCapitol()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_townNotCapitol"));
							if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_townNotDuringWar"));
							if (fromCiv.getDiplomacyManager().isAtWar() || toCiv.getDiplomacyManager().isAtWar()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_townNotAtWar"));
							fromCiv.validateGift();
							toCiv.validateGift();
							giftedTown.validateGift();
							DiplomacyGiftResponse dipResponse = new DiplomacyGiftResponse();
							dipResponse.giftedObject = giftedTown;
							dipResponse.fromCiv = fromCiv;
							dipResponse.toCiv = toCiv;
							sendGiftRequest(toCiv, fromCiv, CivSettings.localize.localizedString("var_cmd_civ_dipgift_townRequest1", fromCiv.getName(), giftedTown.getName(), giftedTown.getGiftCost(), CivSettings.CURRENCY_NAME),
									dipResponse);
							CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_dipgift_entirecivSuccess"));
						}
					}));
		}

		private void sendGiftRequest(Civilization toCiv, Civilization fromCiv, String message, QuestionResponseInterface finishedFunction) throws CivException {
			CivQuestionTask task = Question.civQuestions.get(toCiv.getName());
			if (task != null) {
				/* Civ already has a question pending. Lets deny this question until it times out this will allow questions to come in on a pseduo 'first
				 * come first serve' and prevents question spamming. */
				throw new CivException(CivSettings.localize.localizedString("cmd_civ_dipgift_sendHasPending"));
			}

			task = new CivQuestionTask(toCiv, fromCiv, message, 30000, finishedFunction);
			Question.civQuestions.put(toCiv.getName(), task);
			TaskMaster.asyncTask("", task, 0);
		}
	}
}
