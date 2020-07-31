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

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class CivResearchCommand extends MenuAbstractCommand {

	public CivResearchCommand() {
		super("research");
		displayName = CivSettings.localize.localizedString("cmd_civ_research_name");
		this.setValidator(Validators.validLeaderAdvisor);

		add(new CustomCommand("list").withDescription(CivSettings.localize.localizedString("cmd_civ_research_listDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				ArrayList<ConfigTech> techs = ConfigTech.getAvailableTechs(civ);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_Available"));
				for (ConfigTech tech : techs) {
					CivMessage.send(sender, tech.name + CivColor.LightGray + " " + CivSettings.localize.localizedString("Cost") + " " + CivColor.Yellow + tech.getAdjustedTechCost(civ) + CivColor.LightGray + " "
							+ CivSettings.localize.localizedString("Beakers") + " " + CivColor.Yellow + tech.getAdjustedBeakerCost(civ));
				}
			}
		}));
		add(new CustomCommand("progress").withDescription(CivSettings.localize.localizedString("cmd_civ_research_progressDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_current"));
				if (civ.getResearchTech() != null) {
					int percentageComplete = (int) ((civ.getResearchProgress() / civ.getResearchTech().getAdjustedBeakerCost(civ)) * 100);
					CivMessage.send(sender,
							CivSettings.localize.localizedString("var_cmd_civ_research_current", civ.getResearchTech().name, percentageComplete, (civ.getResearchProgress() + " / " + civ.getResearchTech().getAdjustedBeakerCost(civ))));
				} else {
					CivMessage.send(sender, CivSettings.localize.localizedString("cmd_civ_research_NotAnything"));
				}
			}
		}));
		add(new CustomCommand("on").withDescription(CivSettings.localize.localizedString("cmd_civ_research_onDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_onPrompt"));
				Town capitol = civ.getCapitol();
				if (capitol == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_missingCapitol", civ.getName()) + " " + CivSettings.localize.localizedString("internalCommandException"));
				if (!capitol.isValid()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_missingTownHall"));
				if (!capitol.getCityhall().isActive()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_incompleteTownHall"));
				String techname = Commander.combineArgs(Commander.stripArgs(args, 1));
				ConfigTech tech = CivSettings.getTechByName(techname);
				if (tech == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_NotFound", techname));
				civ.startTechnologyResearch(tech);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_research_start", tech.name));
			}
		}));
		add(new CustomCommand("change").withDescription(CivSettings.localize.localizedString("cmd_civ_research_changeDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_changePrompt"));
				String techname = Commander.combineArgs(Commander.stripArgs(args, 1));
				ConfigTech tech = CivSettings.getTechByName(techname);
				if (tech == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_NotFound", techname));
				if (!civ.getTreasury().hasEnough(tech.getAdjustedTechCost(civ))) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_research_NotEnough1", CivSettings.CURRENCY_NAME, tech.name));
				if (!tech.isAvailable(civ)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_NotAllowedNow"));
				if (civ.getResearchTech() != null) {
					civ.setResearchProgress(0);
					CivMessage.send(sender, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_civ_research_lostProgress1", civ.getResearchTech().name));
					civ.setResearchTech(null);
				}
				civ.startTechnologyResearch(tech);
				CivMessage.sendCiv(civ, CivSettings.localize.localizedString("var_cmd_civ_research_start", tech.name));
			}
		}));
		add(new CustomCommand("finished").withDescription(CivSettings.localize.localizedString("cmd_civ_research_finishedDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_finishedHeading"));
				String out = "";
				for (ConfigTech tech : civ.getTechs()) {
					out += tech.name + ", ";
				}
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("era").withDescription(CivSettings.localize.localizedString("cmd_civ_research_eraDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_research_era"));
				CivMessage.send(sender, CivColor.White + CivSettings.localize.localizedString("var_cmd_civ_research_currentEra", CivColor.LightBlue + CivGlobal.localizedEraString(civ.getCurrentEra())));
				CivMessage.send(sender, CivColor.White + CivSettings.localize.localizedString("var_cmd_civ_research_highestEra", CivColor.LightBlue + CivGlobal.localizedEraString(CivGlobal.highestCivEra)));
				double eraRate = ConfigTech.eraRate(civ);
				if (eraRate == 0.0)
					CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_civ_research_eraNoDiscount"));
				else
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("var_cmd_civ_research_eraDiscount", (eraRate * 100), CivSettings.CURRENCY_NAME));
			}
		}));
		// add(new CustomCommand("calc").withDescription(CivSettings.localize.localizedString("cmd_civ_researchcalc_Desc")).withExecutor(new
		// CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// SimpleDateFormat sdf = CivGlobal.dateFormat;
		// Resident resident = Commander.getResident(sender);
		// Civilization civ = Commander.getSenderCiv(sender);
		// if (resident == null) throw new CivException(CivSettings.localize.localizedString("resident_null"));
		// if (civ.getResearchTech() == null) throw new CivException(CivSettings.localize.localizedString("no_research"));
		// double mins = (civ.getResearchTech().getAdjustedBeakerCost(civ) - civ.getResearchProgress()) / civ.getBeakersCivtick() * 60.0;
		// long timeNow = Calendar.getInstance().getTimeInMillis();
		// double seconds = mins * 60.0;
		// long endResearch = (long) ((double) timeNow + 1000.0 * seconds);
		// CivMessage.sendCiv(civ, CivSettings.localize.localizedString("cmd_civ_research_calc_result", civ.getResearchTech().name,
		// sdf.format(endResearch)));
		// }
		// }));
		// add(new CustomCommand("queuelist").withDescription(CivSettings.localize.localizedString("cmd_civ_research_queueList")).withExecutor(new
		// CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// Civilization civ = Commander.getSenderCiv(sender);
		// if (civ.getTechQueue() == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueErrorListRemove"));
		// CivMessage.sendCiv(civ, CivSettings.localize.localizedString("cmd_civ_research_queueListSucusses") + "§d" + civ.getTechQueue().name);
		// }
		// }));
		// add(new CustomCommand("queueadd").withDescription(CivSettings.localize.localizedString("cmd_civ_research_queueAdd")).withExecutor(new
		// CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// Civilization civ = Commander.getSenderCiv(sender);
		// if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueEnterName"));
		// if (!civ.getCapitol().isValid()) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueTownHallNULL"));
		// if (!civ.getCapitolCityHall().isActive()) throw new
		// CivException(CivSettings.localize.localizedString("cmd_civ_research_queueNotCompletedTownHall"));
		// String techname = Commander.combineArgs(Commander.stripArgs(args, 1));
		// ConfigTech tech = CivSettings.getTechByName(techname);
		// if (civ.getResearchTech() == tech) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueArleadyThis"));
		// if (civ.getResearchTech() == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueNoResearchingNow",
		// tech.name));
		// if (tech == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueUnknownTech", techname));
		// if (civ.getTechQueue() != null) {
		// if (civ.getTechQueue() == tech) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueArleayIn"));
		// if (civ.getResearchTech() == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueNoResearchingNow",
		// tech.name));
		// ConfigTech oldQueue = civ.getTechQueue();
		// civ.setTechQueue(tech);
		// CivMessage.sendCiv(civ, CivSettings.localize.localizedString("cmd_civ_research_queueSucussesAdded", tech.name));
		// CivMessage.send(sender, CivColor.YellowBold + CivSettings.localize.localizedString("cmd_civ_research_queueSucussesWithWarning",
		// oldQueue.name, tech.name));
		// civ.save();
		// } else {
		// civ.setTechQueue(tech);
		// CivMessage.sendCiv(civ, CivSettings.localize.localizedString("cmd_civ_research_queueSucussesAdded", tech.name));
		// civ.save();
		// }
		// }
		// }));
		// add(new
		// CustomCommand("queueremove").withDescription(CivSettings.localize.localizedString("cmd_civ_research_queueRemove")).withExecutor(new
		// CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// Civilization civ = Commander.getSenderCiv(sender);
		// if (civ.getTechQueue() == null) throw new CivException(CivSettings.localize.localizedString("cmd_civ_research_queueErrorListRemove"));
		// ConfigTech oldQueue = civ.getTechQueue();
		// civ.setTechQueue(null);
		// CivMessage.sendCiv(civ, CivSettings.localize.localizedString("cmd_civ_research_queueRemoveSucusses", oldQueue.name));
		// civ.save();
		// }
		// }));
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		showBasicHelp(sender);
	}
}
