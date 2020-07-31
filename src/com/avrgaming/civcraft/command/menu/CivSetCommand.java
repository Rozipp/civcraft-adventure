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

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.util.DecimalHelper;

public class CivSetCommand extends MenuAbstractCommand {

	public CivSetCommand() {
		super("set");
		displayName = CivSettings.localize.localizedString("cmd_civ_set_Name");
		this.setValidator(Validators.validLeaderAdvisor);

		add(new CustomCommand("taxes").withDescription(CivSettings.localize.localizedString("cmd_civ_set_taxesDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (args.length < 1) {
					CivMessage.send(sender, "Current income percentage:" + " " + civ.getIncomeTaxRateString());
					return;
				}
				double newPercentage = vaildatePercentage(args[0]);
				if (newPercentage > civ.getGovernment().maximum_tax_rate)
					throw new CivException(CivSettings.localize.localizedString("cmd_civ_set_overmax") + "(" + DecimalHelper.formatPercentage(civ.getGovernment().maximum_tax_rate) + ")");
				civ.setIncomeTaxRate(newPercentage);
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_set_taxesSuccess") + " " + args[0] + " " + "%");
			}
		}));
		add(new CustomCommand("science").withDescription(CivSettings.localize.localizedString("cmd_civ_set_scienceDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (args.length < 1) {
					CivMessage.send(sender, CivSettings.localize.localizedString("cmd_civ_set_currentScience") + " " + civ.getSciencePercentage());
					return;
				}
				double newPercentage = vaildatePercentage(args[0]);
				civ.setSciencePercentage(newPercentage);
				civ.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_set_scienceSuccess", args[0]));
			}
		}));
		add(new CustomCommand("color").withDescription(CivSettings.localize.localizedString("cmd_civ_set_colorDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				if (args.length < 1) {
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_set_currentColor", Integer.toHexString(civ.getColor())));
					return;
				}
				try {
					int color = Integer.parseInt(args[0], 16);
					if (color > Civilization.HEX_COLOR_MAX) throw new CivException(CivSettings.localize.localizedString("cmd_civ_set_colorInvalid"));
					if (color == 0xFF0000) throw new CivException(CivSettings.localize.localizedString("cmd_civ_set_colorIsBorder"));
					civ.setColor(color);
					civ.save();
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_set_colorSuccess", Integer.toHexString(color)));
				} catch (NumberFormatException e) {
					throw new CivException(args[0] + " " + CivSettings.localize.localizedString("cmd_civ_set_colorInvalid"));
				}
			}
		}));
	}

	private double vaildatePercentage(String arg) throws CivException {
		try {
			arg = arg.replace("%", "");
			Integer amount = Integer.valueOf(arg);
			if (amount < 0 || amount > 100) {
				throw new CivException(CivSettings.localize.localizedString("cmd_civ_set_invalidPercent") + " 0% & 100%");
			}
			return ((double) amount / 100);
		} catch (NumberFormatException e) {
			throw new CivException(arg + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
		}

	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		showBasicHelp(sender);
	}

}
