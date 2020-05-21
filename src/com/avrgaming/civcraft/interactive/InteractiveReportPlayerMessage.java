package com.avrgaming.civcraft.interactive;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.global.reports.ReportManager;
import com.avrgaming.global.reports.ReportManager.ReportType;

public class InteractiveReportPlayerMessage implements InteractiveResponse {

	ReportType type;
	String playerName;
	
	public InteractiveReportPlayerMessage(String playerName, ReportType type) {
		this.type = type;
		this.playerName = playerName;
	}
	
	@Override
	public void respond(String message, Player player) {
		Resident resident = CivGlobal.getResident(player);
		
		ReportManager.reportPlayer(playerName, type, message, resident.getName());
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_interactive_report_success",playerName));
		resident.clearInteractiveMode();
	}

}
