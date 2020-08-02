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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.TownInCivTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.endgame.EndConditionScience;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DecimalHelper;

public class CivInfoCommand extends MenuAbstractCommand {

	public CivInfoCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_civ_info_name");

		add(new CustomCommand("upkeep").withDescription(CivSettings.localize.localizedString("cmd_civ_info_upkeepDesc")).withValidator(Validators.validLeaderAdvisor).withTabCompleter(new TownInCivTaber()).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (args.length < 1) {
					DecimalFormat df = new DecimalFormat("#.#");
					CivMessage.sendHeading(sender, civ.getName() + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading"));
					for (Town town : civ.getTowns()) {
						CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Town") + " " + CivColor.LightGreen + town.getName() + " - " + CivColor.Green + CivSettings.localize.localizedString("Total") + " "
								+ CivColor.LightGreen + getTownTotalLastTick(town, civ));
					}
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("WarColon") + " " + CivColor.LightGreen + df.format(civ.getWarUpkeep()));

					Object[] arrobject = new Object[2];
					arrobject[0] = "\u00a7a" + (civ.getCapitol().getBonusUpkeep() == 1.0 ? "No" : "Yes");
					arrobject[1] = "\u00a76" + (civ.getCapitol().getBonusUpkeep() == 1.0 ? "" : (civ.getCapitol().getBonusUpkeep() == 2.5 ? "2.5x (Enemy doesn't have Notre Dame)" : "5x (Enemy has Notre Dame"));
					CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("var_cmd_civ_info_upkeepTownWar", arrobject));
					CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading2"));
					CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading3"));
					return;
				} else {
					Town town = civ.getTown(args[0]);
					if (town == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_info_upkeepTownInvalid", args[1]));

					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_info_upkeepTownHeading1", town.getName()));
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Base") + " " + CivColor.LightGreen + civ.getUpkeepPaid(town, "base"));
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Distance") + " " + CivColor.LightGreen + civ.getUpkeepPaid(town, "distance"));
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("DistanceUpkeep") + " " + CivColor.LightGreen + civ.getUpkeepPaid(town, "distanceUpkeep"));
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Debt") + " " + CivColor.LightGreen + civ.getUpkeepPaid(town, "debt"));
					Object[] arrobject = new Object[2];
					arrobject[0] = "\u00a7a" + (civ.getCapitol().getBonusUpkeep() == 1.0 ? "No" : "Yes");
					arrobject[1] = "\u00a76" + (civ.getCapitol().getBonusUpkeep() == 1.0 ? "" : (civ.getCapitol().getBonusUpkeep() == 2.5 ? "2.5x (Enemy doesn't have Notre Dame)" : "5x (Enemy has Notre Dame"));
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("var_cmd_civ_info_upkeepTownWar", arrobject));
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + getTownTotalLastTick(town, civ));

					CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_civ_info_upkeepHeading2"));
				}
			}
		}));
		add(new CustomCommand("taxes").withDescription(CivSettings.localize.localizedString("cmd_civ_info_taxesDesc")).withValidator(Validators.validLeaderAdvisor).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_info_taxesHeading"));
				for (Town t : civ.getTowns()) {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Town") + " " + CivColor.LightGreen + t.getName() + " - " + CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen
							+ civ.lastTaxesPaidMap.get(t.getName()));
				}
			}
		}));
		add(new CustomCommand("beakers").withDescription(CivSettings.localize.localizedString("cmd_civ_info_beakersDesc")).withValidator(Validators.validLeaderAdvisor).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				DecimalFormat df = new DecimalFormat("#.#");
				Civilization civ = Commander.getSenderCiv(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_info_beakersHeading"));
				ArrayList<String> out = new ArrayList<String>();
				for (Town t : civ.getTowns()) {
					for (Buff b : t.getBuffManager().getEffectiveBuffs(Buff.SCIENCE_RATE)) {
						out.add(CivColor.Green + CivSettings.localize.localizedString("From") + " " + b.getSource() + ": " + CivColor.LightGreen + b.getDisplayDouble());
					}
				}
				out.add(CivColor.LightBlue + "------------------------------------");
				out.add(CivColor.Green + CivSettings.localize.localizedString("Total") + " " + CivColor.LightGreen + df.format(civ.getBeakersCivtick()));
				CivMessage.send(sender, out);
			}
		}));
		add(new CustomCommand("online").withDescription(CivSettings.localize.localizedString("cmd_civ_info_onlineDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_info_onlineHeading", civ.getName()));
				String out = "";
				for (Resident resident : civ.getOnlineResidents()) {
					out += resident.getName() + " ";
				}
				CivMessage.send(sender, out);
			}
		}));
	}

	private double getTownTotalLastTick(Town town, Civilization civ) {
		double total = 0;
		for (String key : civ.lastUpkeepPaidMap.keySet()) {
			String townName = key.split(",")[0];

			if (townName.equalsIgnoreCase(town.getName())) {
				total += civ.lastUpkeepPaidMap.get(key);
			}
		}
		return total;
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		Civilization civ = Commander.getSenderCiv(sender);
		Resident resident = Commander.getResident(sender);
		show(sender, resident, civ);
		CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_civ_info_help"));
	}

	public static void show(CommandSender sender, Resident resident, Civilization civ) {

		boolean isOP = false;
		if (sender instanceof Player) {
			Player player;
			try {
				player = CivGlobal.getPlayer(resident);
				if (player.isOp()) isOP = true;
			} catch (CivException e) {
				/* Allow console to display. */
			}
		} else {
			/* We're the console. */
			isOP = true;
		}

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_cmd_civ_info_showHeading", civ.getName()));

		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Score") + " " + CivColor.LightGreen + civ.getScore() + CivColor.Green + " " + CivSettings.localize.localizedString("Towns") + " " + CivColor.LightGreen
				+ civ.getTownCount());
		if (civ.hasResident(resident)) {
			CivMessage.send((Object) sender, "§2" + CivSettings.localize.localizedString("Goverment", new StringBuilder().append("§a").append(civ.getGovernment().displayName).toString()));
		}
		if (civ.GM.getLeaderGroup() == null) {
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Leaders") + " " + CivColor.Rose + "NONE");
		} else {
			CivMessage.send(sender, CivColor.Green + civ.GM.leadersGroupName + ": " + CivColor.LightGreen + civ.GM.getLeaderGroup().getMembersString());
		}

		if (civ.GM.getAdviserGroup() == null) {
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Advisors") + " " + CivColor.Rose + "NONE");
		} else {
			CivMessage.send(sender, CivColor.Green + civ.GM.advisersGroupName + ": " + CivColor.LightGreen + civ.GM.getAdviserGroup().getMembersString());
		}

		if (resident == null || civ.hasResident(resident)) {
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_civ_info_showTax") + " " + CivColor.LightGreen + civ.getIncomeTaxRateString() + CivColor.Green + " "
					+ CivSettings.localize.localizedString("cmd_civ_info_showScience") + " " + CivColor.LightGreen + DecimalHelper.formatPercentage(civ.getSciencePercentage()));
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Beakers") + " " + CivColor.LightGreen + civ.getBeakersCivtick() + CivColor.Green + " " + CivSettings.localize.localizedString("Online") + " "
					+ CivColor.LightGreen + civ.getOnlineResidents().size());
		}

		if (resident == null || civ.GM.isLeaderOrAdviser(resident) || isOP) {
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Treasury") + " " + CivColor.LightGreen + civ.getTreasury().getBalance() + CivColor.Green + " " + CivSettings.CURRENCY_NAME);
		}

		if (civ.getTreasury().inDebt()) {
			CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("InDebt") + " " + civ.getTreasury().getDebt() + " Coins.");
			CivMessage.send(sender, CivColor.Yellow + civ.getDaysLeftWarning());
		}

		for (EndGameCondition endCond : EndGameCondition.endConditions) {
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(endCond.getSessionKey());
			if (entries.size() == 0) {
				continue;
			}

			for (SessionEntry entry : entries) {
				if (civ == EndGameCondition.getCivFromSessionData(entry.value)) {
					Integer daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);

					CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_info_daysTillVictoryNew", CivColor.LightBlue + CivColor.BOLD + civ.getName() + CivColor.White,
							CivColor.Yellow + CivColor.BOLD + daysLeft + CivColor.White, CivColor.LightPurple + CivColor.BOLD + endCond.getVictoryName() + CivColor.White));
					break;
				}
			}
		}

		Integer votes = EndConditionDiplomacy.getVotesFor(civ);
		if (votes > 0) {
			CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_votesHeading", CivColor.LightBlue + CivColor.BOLD + civ.getName() + CivColor.White, CivColor.LightPurple + CivColor.BOLD + votes + CivColor.White));
		}

		Double beakers = EndConditionScience.getBeakersFor(civ);
		if (beakers > 0) {
			DecimalFormat df = new DecimalFormat("#.#");
			CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_info_showBeakersTowardEnlight", CivColor.LightBlue + CivColor.BOLD + civ.getName() + CivColor.White,
					CivColor.LightPurple + CivColor.BOLD + df.format(beakers) + CivColor.White));
		}

		String out = CivColor.Green + CivSettings.localize.localizedString("Towns") + " ";
		for (Town town : civ.getTowns()) {
			if (town.isCapitol()) {
				out += CivColor.Gold + town.getName();
			} else
				if (town.getMotherCiv() != null) {
					out += CivColor.Yellow + town.getName();
				} else {
					out += CivColor.White + town.getName();
				}
			out += ", ";
		}

		CivMessage.send(sender, out);
	}

}
