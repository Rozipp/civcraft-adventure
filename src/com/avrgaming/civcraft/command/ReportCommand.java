package com.avrgaming.civcraft.command;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveReportBug;
import com.avrgaming.civcraft.interactive.InteractiveReportPlayer;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class ReportCommand extends CommandBase {

	@Override
	public void init() {
		command = "/report";
		displayName = CivSettings.localize.localizedString("cmd_reprot_Name");
		
		cs.add("player", CivSettings.localize.localizedString("cmd_report_playerDesc"));
		cs.add("bug", CivSettings.localize.localizedString("cmd_report_bugDesc"));

	}
	
	public void bug_cmd() throws CivException {
        Resident resident = this.getResident();
        CivMessage.sendHeading(this.sender, CivSettings.localize.localizedString("cmd_report_Heading"));
        CivMessage.send((Object)this.sender, "§e" + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_5"));
        CivMessage.send((Object)this.sender, " ");
        CivMessage.send((Object)this.sender, "§e" + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_6") + CivSettings.localize.localizedString("cmd_report_7"));
        CivMessage.send((Object)this.sender, "§e" + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("interactive_report_descriptionBuG"));
        CivMessage.send((Object)this.sender, CivColor.LightGray + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_8"));
        resident.setInteractiveMode(new InteractiveReportBug());
    }

	public void player_cmd() throws CivException {
        Resident resident = this.getResident();
        Resident reportedResident = this.getNamedResident(1);
        CivMessage.sendHeading(this.sender, CivSettings.localize.localizedString("cmd_report_Heading"));
        CivMessage.send((Object)this.sender, "§e" + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_1", reportedResident.getName()));
        CivMessage.send((Object)this.sender, " ");
        CivMessage.send((Object)this.sender, "§e" + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_2") + CivSettings.localize.localizedString("cmd_report_3"));
        CivMessage.send((Object)this.sender, "§e" + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("interactive_report_description"));
        CivMessage.send((Object)this.sender, CivColor.LightGray + (Object)ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_4"));
        resident.setDesiredReportPlayerName(reportedResident.getName());
        resident.setInteractiveMode(new InteractiveReportPlayer(command));
    }
	
	//XXX Import from Furnex
//	public void player_cmd() throws CivException {
//		Resident resident = getResident();
//		Resident reportedResident = getNamedResident(1);
//		
//		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_report_Heading"));
//		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_1")+" "+reportedResident.getName());
//		CivMessage.send(sender, " ");
//		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_2")+" "+CivColor.LightGreen+ChatColor.BOLD+ReportManager.getReportTypes());
//		CivMessage.send(sender, " ");
//		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+ CivSettings.localize.localizedString("cmd_report_3")+
//				CivSettings.localize.localizedString("cmd_report_4"));
//		CivMessage.send(sender, CivColor.LightGray+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_5"));
//		resident.setInteractiveMode(new InteractiveReportPlayer(reportedResident.getName()));
//	}
//	
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
		
	}

}
