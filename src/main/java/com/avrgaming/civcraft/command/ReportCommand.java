package com.avrgaming.civcraft.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveReportBug;
import com.avrgaming.civcraft.interactive.InteractiveReportPlayer;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class ReportCommand extends MenuAbstractCommand {

	public ReportCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("cmd_reprot_Name");
		add(new CustomCommand("player").withDescription(CivSettings.localize.localizedString("cmd_report_playerDesc")).withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Resident reportedResident = Commander.getNamedResident(args, 0);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_report_Heading"));
				CivMessage.send(sender, "§e" + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_1", reportedResident.getName()));
				CivMessage.send(sender, " ");
				CivMessage.send(sender, "§e" + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_2") + CivSettings.localize.localizedString("cmd_report_3"));
				CivMessage.send(sender, "§e" + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("interactive_report_description"));
				CivMessage.send(sender, CivColor.LightGray + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_4"));
				resident.setDesiredReportPlayerName(reportedResident.getName());
				resident.setInteractiveMode(new InteractiveReportPlayer(resident.getName()));
			}
		}));
		add(new CustomCommand("bug").withDescription(CivSettings.localize.localizedString("cmd_report_bugDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_report_Heading"));
				CivMessage.send(sender, "§e" + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_5"));
				CivMessage.send(sender, " ");
				CivMessage.send(sender, "§e" + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_6") + CivSettings.localize.localizedString("cmd_report_7"));
				CivMessage.send(sender, "§e" + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("interactive_report_descriptionBuG"));
				CivMessage.send(sender, CivColor.LightGray + (Object) ChatColor.BOLD + CivSettings.localize.localizedString("cmd_report_8"));
				resident.setInteractiveMode(new InteractiveReportBug());
			}
		}));
	}
	// XXX Import from Furnex
	// public void player_cmd() throws CivException {
	// Resident resident = getResident();
	// Resident reportedResident = getNamedResident(1);
	//
	// CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_report_Heading"));
	// CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_1")+"
	// "+reportedResident.getName());
	// CivMessage.send(sender, " ");
	// CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_2")+"
	// "+CivColor.LightGreen+ChatColor.BOLD+ReportManager.getReportTypes());
	// CivMessage.send(sender, " ");
	// CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+ CivSettings.localize.localizedString("cmd_report_3")+
	// CivSettings.localize.localizedString("cmd_report_4"));
	// CivMessage.send(sender, CivColor.LightGray+ChatColor.BOLD+CivSettings.localize.localizedString("cmd_report_5"));
	// resident.setInteractiveMode(new InteractiveReportPlayer(reportedResident.getName()));
	// }

}
