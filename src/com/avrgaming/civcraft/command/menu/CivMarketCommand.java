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
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class CivMarketCommand extends MenuAbstractCommand {

	public CivMarketCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_market_Name");
		this.addValidator(Validators.validLeaderAdvisor);
		add(new CustomCommand("towns").withAliases("t").withDescription(CivSettings.localize.localizedString("cmd_market_buy_townsDesc")).withTabCompleter(new AbstractTaber() {
			@Override
			public List<String> getTabList(CommandSender sender, String arg) throws CivException {
				List<String> l = new ArrayList<>();
				for (Town town : CivGlobal.getTowns()) {
					if (!town.isCapitol() && town.isForSale() && town.getName().toLowerCase().startsWith(arg)) l.add(town.getName());
				}
				return l;
			}
		}).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization senderCiv = Commander.getSenderCiv(sender);
				if (args.length < 1) {
					DecimalFormat df = new DecimalFormat("#.#");
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_market_buy_townsHeading"));
					for (Town town : CivGlobal.getTowns()) {
						if (!town.isCapitol() && town.isForSale()) CivMessage.send(sender, town.getName() + " - " + CivColor.Yellow + df.format(town.getForSalePrice()) + " " + CivSettings.CURRENCY_NAME);
					}
					CivMessage.send(sender, CivSettings.localize.localizedString("cmd_market_buy_townsPrompt"));
					return;
				}
				Town town = Commander.getNamedTown(args, 0);
				if (senderCiv.isForSale()) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_ErrorCivForSale"));
				if (town.getCiv() == senderCiv) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_townsOwned"));
				if (town.isCapitol()) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_townCapitol"));
				if (!town.isForSale()) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_NotForSale"));
				if (War.isWarTime() || War.isWithinWarDeclareDays()) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_warOrClose"));
				senderCiv.buyTown(town);
				CivMessage.global(CivSettings.localize.localizedString("var_cmd_market_buy_townsBroadcast", town.getName(), senderCiv.getName()));
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_market_buy_townsSuccess", args[1]));
			}
		}));
		add(new CustomCommand("civs").withAliases("c").withDescription(CivSettings.localize.localizedString("cmd_market_buy_civsDesc")).withTabCompleter(new AbstractTaber() {
			@Override
			public List<String> getTabList(CommandSender sender, String arg) throws CivException {
				List<String> l = new ArrayList<>();
				for (Civilization civ : CivGlobal.getCivs()) {
					if (civ.isForSale() && civ.getName().toLowerCase().startsWith(arg)) l.add(civ.getName());
				}
				return null;
			}
		}).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization senderCiv = Commander.getSenderCiv(sender);
				if (args.length < 1) {
					DecimalFormat df = new DecimalFormat("#.#");
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_market_buy_civsHeading"));
					for (Civilization civ : CivGlobal.getCivs()) {
						if (civ.isForSale()) CivMessage.send(sender, civ.getName() + " - " + CivColor.Yellow + df.format(civ.getTotalSalePrice()) + " " + CivSettings.CURRENCY_NAME);
					}
					CivMessage.send(sender, CivSettings.localize.localizedString("cmd_market_buy_civsPrompt"));
					return;
				}
				Civilization civBought = Commander.getNamedCiv(args, 0);
				if (senderCiv.isForSale()) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_ErrorCivForSale"));
				if (civBought == senderCiv) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_civsOwned"));
				if (!civBought.isForSale()) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_NotForSale"));
				if (War.isWarTime() || War.isWithinWarDeclareDays()) throw new CivException(CivSettings.localize.localizedString("cmd_market_buy_warOrClose"));
				senderCiv.buyCiv(civBought);
				CivMessage.global(CivSettings.localize.localizedString("var_cmd_market_buy_civsSuccess", civBought.getName(), senderCiv.getName()));
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_market_buy_civsSuccess2", args[1]));
			}
		}));
	}
}
