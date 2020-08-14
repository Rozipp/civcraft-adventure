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

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;

public class AdminLagCommand extends MenuAbstractCommand {

	public AdminLagCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_lag_cmdDesc");

		add(new CustomCommand("trommels").withDescription(CivSettings.localize.localizedString("adcmd_lag_trommelsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.trommelsEnabled = !CivGlobal.trommelsEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_trommel") + " " + CivGlobal.trommelsEnabled);
			}
		}));
		add(new CustomCommand("quarries").withDescription(CivSettings.localize.localizedString("adcmd_lag_quarryDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.quarriesEnabled = !CivGlobal.quarriesEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_quarry") + " " + CivGlobal.quarriesEnabled);
			}
		}));
		add(new CustomCommand("fishery").withDescription(CivSettings.localize.localizedString("adcmd_lag_fishHatcherDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.fisheryEnabled = !CivGlobal.fisheryEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_fishHatchery") + " " + CivGlobal.fisheryEnabled);
			}
		}));
		add(new CustomCommand("grinders").withDescription(CivSettings.localize.localizedString("adcmd_lag_grinderDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.mobGrinderEnabled = !CivGlobal.mobGrinderEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_mobGrinders") + " " + CivGlobal.mobGrinderEnabled);
			}
		}));
		add(new CustomCommand("towers").withDescription(CivSettings.localize.localizedString("adcmd_lag_towersDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.towersEnabled = !CivGlobal.towersEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_towers") + " " + CivGlobal.towersEnabled);
			}
		}));
		add(new CustomCommand("growth").withDescription(CivSettings.localize.localizedString("adcmd_lag_growthDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.growthEnabled = !CivGlobal.growthEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_growth") + " " + CivGlobal.growthEnabled);
			}
		}));
		add(new CustomCommand("trade").withDescription(CivSettings.localize.localizedString("adcmd_lag_tradeDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.tradeEnabled = !CivGlobal.tradeEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_trade") + " " + CivGlobal.tradeEnabled);
			}
		}));
		add(new CustomCommand("score").withDescription(CivSettings.localize.localizedString("adcmd_lag_scoreDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.scoringEnabled = !CivGlobal.scoringEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_scoring") + " " + CivGlobal.scoringEnabled);
			}
		}));
		add(new CustomCommand("warning").withDescription(CivSettings.localize.localizedString("adcmd_lag_warningDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.growthEnabled = !CivGlobal.growthEnabled;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_warnings") + " " + CivGlobal.warningsEnabled);
			}
		}));
		add(new CustomCommand("blockupdate").withDescription(CivSettings.localize.localizedString("adcmd_lag_blockUpdateDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Integer blocks = Commander.getNamedInteger(args, 0);
				SyncBuildUpdateTask.UPDATE_LIMIT = blocks;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_lag_blockupdateSet", blocks));
			}
		}));
		add(new CustomCommand("speedchunks").withDescription(CivSettings.localize.localizedString("adcmd_lag_speedCheckOnChunk")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.speedChunks = !CivGlobal.speedChunks;
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_SpeedCheck") + " " + CivGlobal.speedChunks);
			}
		}));
	}

}
