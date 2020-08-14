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
import com.avrgaming.civcraft.construct.structures.Bank;
import com.avrgaming.civcraft.construct.structures.Blacksmith;
import com.avrgaming.civcraft.construct.structures.Grocer;
import com.avrgaming.civcraft.construct.structures.Library;
import com.avrgaming.civcraft.construct.structures.ScoutTower;
import com.avrgaming.civcraft.construct.structures.Store;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;

public class TownSetCommand extends MenuAbstractCommand {

	public TownSetCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_town_set_name");
		this.addValidator(Validators.validMayorAssistantLeader);

		add(new CustomCommand("bankfee").withDescription(CivSettings.localize.localizedString("cmd_town_set_bankfeeDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Integer feeInt = Commander.getNamedInteger(args, 0);
				if (feeInt < 5 || feeInt > 15) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
				Structure struct = town.BM.getFirstStructureById("s_bank");
				if (struct == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_bankfeeNone"));
				((Bank) struct).setNonResidentFee(((double) feeInt / 100));
				((Bank) struct).updateSignText();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
			}
		}));
		add(new CustomCommand("storefee").withDescription(CivSettings.localize.localizedString("cmd_town_set_storefeeDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Integer feeInt = Commander.getNamedInteger(args, 0);
				if (feeInt < 5 || feeInt > 15) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
				Structure struct = town.BM.getFirstStructureById("s_store");
				if (struct == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_storefeeNone"));
				((Store) struct).setNonResidentFee(((double) feeInt / 100));
				((Store) struct).updateSignText();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
			}
		}));
		add(new CustomCommand("grocerfee").withDescription(CivSettings.localize.localizedString("cmd_town_set_grocerfeeDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Integer feeInt = Commander.getNamedInteger(args, 0);
				if (feeInt < 5 || feeInt > 15) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
				Structure struct = town.BM.getFirstStructureById("s_grocer");
				if (struct == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_grocerfeeNone"));
				((Grocer) struct).setNonResidentFee(((double) feeInt / 100));
				((Grocer) struct).updateSignText();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
			}
		}));
		add(new CustomCommand("libraryfee").withDescription(CivSettings.localize.localizedString("cmd_town_set_libraryfeeDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Integer feeInt = Commander.getNamedInteger(args, 0);
				if (feeInt < 5 || feeInt > 15) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
				Structure struct = town.BM.getFirstStructureById("s_library");
				if (struct == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_libraryfeeNone"));
				((Library) struct).setNonResidentFee(((double) feeInt / 100));
				((Library) struct).updateSignText();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
			}
		}));
		add(new CustomCommand("blacksmithfee").withDescription(CivSettings.localize.localizedString("cmd_town_set_blacksmithfeeDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Integer feeInt = Commander.getNamedInteger(args, 0);
				if (feeInt < 5 || feeInt > 15) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
				Structure struct = town.BM.getFirstStructureById("s_blacksmith");
				if (struct == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_blacksmithfeeNone"));
				((Blacksmith) struct).setNonResidentFee(((double) feeInt / 100));
				((Blacksmith) struct).updateSignText();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
			}
		}));
		// add(new CustomCommand("stablefee").withDescription(CivSettings.localize.localizedString("cmd_town_set_stablefeeDesc")).withExecutor(new
		// CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// Town town = Commander.getSelectedTown(sender);
		// Integer feeInt = Commander.getNamedInteger(args, 0);
		// Structure struct = town.BM.getFirstStructureById("s_stable");
		// if (struct == null) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_stablefeeNone"));
		// Stable stable = (Stable) struct;
		// if (feeInt < Stable.FEE_MIN || feeInt > Stable.FEE_MAX) throw new
		// CivException(CivSettings.localize.localizedString("cmd_town_set_stablefeeRates"));
		// stable.setNonResidentFee(((double) feeInt / 100));
		// stable.updateSignText();
		// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess", feeInt));
		// }
		// }));
		add(new CustomCommand("scoutrate").withDescription(CivSettings.localize.localizedString("cmd_town_set_scoutrateDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Integer rate = Commander.getNamedInteger(args, 0);
				if (rate != 10 && rate != 30 && rate != 60) throw new CivException(CivSettings.localize.localizedString("cmd_town_set_scoutrateRates"));
				for (Structure struct : town.BM.getStructures()) {
					if (struct instanceof ScoutTower) ((ScoutTower) struct).setReportSeconds(rate);
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_scoutrateSuccess", rate));
			}
		}));
	}
}
