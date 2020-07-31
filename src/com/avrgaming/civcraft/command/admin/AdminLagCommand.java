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
package com.avrgaming.civcraft.command.admin;

import com.avrgaming.civcraft.command.oldcommands.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;

public class AdminLagCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad lag";
		displayName = CivSettings.localize.localizedString("adcmd_lag_cmdDesc");
		
		cs.add("trommels", CivSettings.localize.localizedString("adcmd_lag_trommelsDesc"));
		cs.add("quarries", CivSettings.localize.localizedString("adcmd_lag_quarryDesc"));
		cs.add("fishery", CivSettings.localize.localizedString("adcmd_lag_fishHatcherDesc"));
		cs.add("grinders", CivSettings.localize.localizedString("adcmd_lag_grinderDesc"));
		cs.add("towers", CivSettings.localize.localizedString("adcmd_lag_towersDesc"));
		cs.add("growth", CivSettings.localize.localizedString("adcmd_lag_growthDesc"));
		cs.add("trade", CivSettings.localize.localizedString("adcmd_lag_tradeDesc"));
		cs.add("score", CivSettings.localize.localizedString("adcmd_lag_scoreDesc"));
		cs.add("warning", CivSettings.localize.localizedString("adcmd_lag_warningDesc"));
		cs.add("blockupdate", CivSettings.localize.localizedString("adcmd_lag_blockUpdateDesc"));
		cs.add("speedchunks", CivSettings.localize.localizedString("adcmd_lag_speedCheckOnChunk"));
		
	}
	
	public void blockupdate_cmd() throws CivException {
		Integer blocks = this.getNamedInteger(1);
		
		SyncBuildUpdateTask.UPDATE_LIMIT = blocks;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_lag_blockupdateSet",blocks));
	}
	
	public void speedchunks_cmd() {
		CivGlobal.speedChunks = !CivGlobal.speedChunks;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_SpeedCheck")+" "+CivGlobal.speedChunks);
	}
	
	public void score_cmd() {
		CivGlobal.scoringEnabled = !CivGlobal.scoringEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_scoring")+" "+CivGlobal.scoringEnabled);
	}

	public void grinders_cmd() {
		CivGlobal.mobGrinderEnabled = !CivGlobal.mobGrinderEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_mobGrinders")+" "+CivGlobal.mobGrinderEnabled);
	}
	
	public void trommels_cmd() {
		CivGlobal.trommelsEnabled = !CivGlobal.trommelsEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_trommel")+" "+CivGlobal.trommelsEnabled);
	}
	
	public void quarries_cmd() {
		CivGlobal.quarriesEnabled = !CivGlobal.quarriesEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_quarry")+" "+CivGlobal.quarriesEnabled);
	}
	
	public void fishery_cmd() {
		CivGlobal.fisheryEnabled = !CivGlobal.fisheryEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_fishHatchery")+" "+CivGlobal.fisheryEnabled);
	}
	
	public void towers_cmd() {
		CivGlobal.towersEnabled = !CivGlobal.towersEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_towers")+" "+CivGlobal.towersEnabled);
	}
	
	public void growth_cmd() {
		CivGlobal.growthEnabled = !CivGlobal.growthEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_growth")+" "+CivGlobal.growthEnabled);
	}
	
	public void trade_cmd() {
		CivGlobal.tradeEnabled = !CivGlobal.tradeEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_trade")+" "+CivGlobal.tradeEnabled);
	}
	
	public void warning_cmd() {
		CivGlobal.growthEnabled = !CivGlobal.growthEnabled;
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_lag_warnings")+" "+CivGlobal.warningsEnabled);
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		//Admin is checked in parent command.
	}

}
