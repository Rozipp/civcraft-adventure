/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.old;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class AdminCivCommand extends MenuAbstractCommand {

	public AdminCivCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_civ_name");

		add(new CustomCommand("disband").withDescription(CivSettings.localize.localizedString("adcmd_civ_disbandDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				Player player = Commander.getPlayer(sender);
				String ss = Commander.combineArgs(Commander.stripArgs(args, 1));
				CivMessage.sendCiv(civ, CivSettings.localize.localizedString("adcmd_civ_disbandAlert", player.getName(), "\"" + ss + "\""));
				civ.delete();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_disbandSuccess", player.getName(), "\"" + ss + "\""));
			}
		}));
		add(new CustomCommand("addleader").withDescription(CivSettings.localize.localizedString("adcmd_civ_addLeaderDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Civilization civ = Commander.getNamedCiv(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						civ.GM.addLeader(resident);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_addLeaderSuccess", resident.getName(), civ.getName()));
					}
				}));
		add(new CustomCommand("addadviser").withDescription(CivSettings.localize.localizedString("adcmd_civ_addAdvisorDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Civilization civ = Commander.getNamedCiv(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						civ.GM.addAdviser(resident);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_addAdvisorSuccess", resident.getName(), civ.getName()));

					}
				}));
		add(new CustomCommand("rmleader").withDescription(CivSettings.localize.localizedString("adcmd_civ_rmLeaderDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Civilization civ = Commander.getNamedCiv(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						if (civ.GM.isLeader(resident)) {
							civ.GM.removeLeader(resident);
							CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderSuccess", resident.getName(), civ.getName()));
						} else {
							CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderNotInGroup", resident.getName(), civ.getName()));
						}
					}
				}));
		add(new CustomCommand("rmadviser").withDescription(CivSettings.localize.localizedString("adcmd_civ_rmAdvisorDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new ResidentInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Civilization civ = Commander.getNamedCiv(args, 0);
						Resident resident = Commander.getNamedResident(args, 1);
						if (civ.GM.isAdviser(resident)) {
							civ.GM.removeAdviser(resident);
							CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorSuccess", resident.getName(), civ.getName()));
						} else {
							CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorNotInGroup", resident.getName(), civ.getName()));
						}
					}
				}));
		add(new CustomCommand("givetech").withDescription(CivSettings.localize.localizedString("adcmd_civ_giveTechDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (ConfigTech tech : CivSettings.techs.values()) {
					String name = tech.name.replace(" ", "_");
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_civ_giveTechPrompt"));
				ConfigTech tech = CivSettings.techs.get(args[1].replace("_", " "));
				if (tech == null) throw new CivException(CivSettings.localize.localizedString("adcmd_civ_giveTechInvalid") + args[2]);
				if (civ.hasTechnologys(tech.id)) throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_giveTechAlreadyhas", civ.getName(), tech.id));
				civ.addTech(tech);
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_giveTechSuccess", tech.name, civ.getName()));
			}
		}));
		add(new CustomCommand("beakerrate").withDescription(CivSettings.localize.localizedString("adcmd_civ_beakerRateDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				Double amount = Commander.getNamedDouble(args, 1);
				civ.setBaseBeakers(amount);
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_beakerRateSuccess", civ.getName(), amount));
			}
		}));
		add(new CustomCommand("toggleadminciv").withDescription(CivSettings.localize.localizedString("adcmd_civ_toggleadminCivDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				civ.setAdminCiv(!civ.isAdminCiv());
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_toggleAdminCivSuccess", civ.getName(), civ.isAdminCiv()));
			}
		}));
		add(new CustomCommand("alltech").withDescription(CivSettings.localize.localizedString("adcmd_civ_alltechDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				for (ConfigTech tech : CivSettings.techs.values()) {
					civ.addTech(tech);
				}
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_alltechSuccess"));
			}
		}));
		add(new CustomCommand("setrelation").withDescription(CivSettings.localize.localizedString("adcmd_civ_setRelationDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new CivInWorldTaber())
				.withTabCompleter(new AbstractCashedTaber() {
					@Override
					protected List<String> newTabList(String arg) {
						List<String> l = new ArrayList<>();
						if ("NEUTRAL".startsWith(arg.toUpperCase())) l.add("NEUTRAL");
						if ("PEACE".startsWith(arg.toUpperCase())) l.add("PEACE");
						if ("ALLY".startsWith(arg.toUpperCase())) l.add("ALLY");
						if ("WAR".startsWith(arg.toUpperCase())) l.add("WAR");
						if ("HOSTILE".startsWith(arg.toUpperCase())) l.add("HOSTILE");
						return l;
					}
				}).withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						if (args.length < 3) throw new CivException(CivSettings.localize.localizedString("Usage") + " [civ] [otherCiv] [NEUTRAL|HOSTILE|WAR|PEACE|ALLY]");
						Civilization civ = Commander.getNamedCiv(args, 0);
						Civilization otherCiv = Commander.getNamedCiv(args, 1);
						Relation.Status status = Relation.Status.valueOf(args[2].toUpperCase());
						CivGlobal.setRelation(civ, otherCiv, status);
						if (status.equals(Status.WAR)) {
							CivGlobal.setAggressor(civ, otherCiv, civ);
							CivGlobal.setAggressor(otherCiv, civ, civ);
						}
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_setrelationSuccess", civ.getName(), otherCiv.getName(), status.name()));
					}
				}));
		add(new CustomCommand("info").withDescription(CivSettings.localize.localizedString("adcmd_civ_infoDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				// Civilization civ = Commander.getNamedCiv(args, 0);
				// CivInfoCommand cmd = new CivInfoCommand();
				// cmd.senderCivOverride = civ;
				// cmd.onCommand(sender, null, "info", Commander.stripArgs(args, 1));
			}
		}));
		add(new CustomCommand("merge").withDescription(CivSettings.localize.localizedString("adcmd_civ_mergeDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization oldciv = Commander.getNamedCiv(args, 0);
				Civilization newciv = Commander.getNamedCiv(args, 1);
				if (oldciv == newciv) throw new CivException(CivSettings.localize.localizedString("adcmd_civ_mergeSameError"));
				newciv.mergeInCiv(oldciv);
				CivMessage.global(CivSettings.localize.localizedString("var_adcmd_civ_mergeSuccess", oldciv.getName(), newciv.getName()));
			}
		}));
		add(new CustomCommand("setgov").withDescription(CivSettings.localize.localizedString("adcmd_civ_setgovDesc")).withTabCompleter(new CivInWorldTaber()).withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (ConfigGovernment gov : CivSettings.governments.values()) {
					String name = gov.displayName.replace(" ", "_");
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_civ_setgovPrompt"));
				ConfigGovernment gov = CivSettings.governments.get(args[1].replace("_", ""));
				if (gov == null) throw new CivException(CivSettings.localize.localizedString("adcmd_civ_setGovInvalidGov") + " gov_monarchy, gov_depostism... etc");
				// Remove any anarchy timers
				String key = "changegov_" + civ.getId();
				CivGlobal.getSessionDatabase().delete_all(key);
				civ.setGovernment(gov.id);
				CivMessage.global(CivSettings.localize.localizedString("var_adcmd_civ_setGovSuccessBroadcast", civ.getName(), CivSettings.governments.get(gov.id).displayName));
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_setGovSuccess"));
			}
		}));
		add(new CustomCommand("bankrupt").withDescription(CivSettings.localize.localizedString("adcmd_civ_bankruptDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				if (args.length < 2) {
					CivMessage.send(sender, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("adcmd_civ_bankruptConfirmPrompt"));
					CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_civ_bankruptConfirmCmd"));
				}
				civ.getTreasury().setBalance(0);
				for (Town town : civ.getTowns()) {
					town.getTreasury().setBalance(0);
					town.save();
					for (Resident resident : town.getResidents()) {
						resident.getTreasury().setBalance(0);
						resident.save();
					}
				}
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_bankruptSuccess", civ.getName()));
			}
		}));
		add(new CustomCommand("conquered").withDescription(CivSettings.localize.localizedString("adcmd_civ_concqueredDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				civ.setConquered(true);
				CivGlobal.removeCiv(civ);
				CivGlobal.addConqueredCiv(civ);
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_conqueredSuccess"));
			}
		}));
		add(new CustomCommand("unconquer").withDescription(CivSettings.localize.localizedString("adcmd_civ_unconquerDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				String conquerCiv = Commander.getNamedString(args, 0, "conquered civ");
				Civilization civ = CivGlobal.getConqueredCiv(conquerCiv);
				if (civ == null) civ = CivGlobal.getCivFromName(conquerCiv);
				if (civ == null) throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_NoCivByThatNane", conquerCiv));
				civ.setConquered(false);
				CivGlobal.removeConqueredCiv(civ);
				CivGlobal.addCiv(civ);
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_unconquerSuccess"));
			}
		}));
		add(new CustomCommand("liberate").withDescription(CivSettings.localize.localizedString("adcmd_civ_liberateDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization motherCiv = Commander.getNamedCiv(args, 0);
				/* Liberate the civ. */
				for (Town t : CivGlobal.getTowns()) {
					if (t.getMotherCiv() == motherCiv) {
						t.changeCiv(motherCiv);
						t.setMotherCiv(null);
						t.save();
					}
				}
				motherCiv.setConquered(false);
				CivGlobal.removeConqueredCiv(motherCiv);
				CivGlobal.addCiv(motherCiv);
				motherCiv.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_liberateSuccess") + " " + motherCiv.getName());
			}
		}));
		add(new CustomCommand("setvotes").withDescription(CivSettings.localize.localizedString("adcmd_civ_setvotesDesc")).withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				Integer votes = Commander.getNamedInteger(args, 1);
				EndConditionDiplomacy.setVotes(civ, votes);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_civ_setVotesSuccess", civ.getName(), votes));
			}
		}));
		add(new CustomCommand("rename").withDescription(CivSettings.localize.localizedString("adcmd_civ_renameDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getNamedCiv(args, 0);
				String name = Commander.getNamedString(args, 1, CivSettings.localize.localizedString("adcmd_civ_newNamePrompt"));
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_civ_renameUseUnderscores"));
				try {
					civ.rename(name);
				} catch (InvalidNameException e) {
					throw new CivException(e.getMessage());
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_civ_renameCivSuccess"));
			}
		}));
	}

	// public void setmaster_cmd() throws CivException {
	// Civilization vassal = getNamedCiv(1);
	// Civilization master = getNamedCiv(2);
	// if (vassal == master) throw new CivException("cannot make vassal and master the same");
	// CivGlobal.setVassalState(master, vassal);
	// CivMessage.sendSuccess(sender, "Vassaled " + vassal.getName() + " to " + master.getName());
	// }
}
