
package com.avrgaming.civcraft.command.old;

import java.text.SimpleDateFormat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Report;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class AdminReportCommand extends MenuAbstractCommand {
	public AdminReportCommand(String perentCommand) {
		super(perentCommand);
		this.displayName = CivSettings.localize.localizedString("adcmd_report_name");
		add(new CustomCommand("buglist").withDescription(CivSettings.localize.localizedString("adcmd_report_buglistDesc", Bukkit.getServer().getServerName())).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				SimpleDateFormat sdf = CivGlobal.dateFormat;
				CivMessage.sendHeading(sender, "Server Bugs " + Bukkit.getServerName());
				for (Report report : CivGlobal.getReports()) {
					if (report.isClosed() || !report.isBug()) continue;
					String message = CivColor.LightGray + "(" + report.getId() + ") " + "§a" + "Reporter: " + CivColor.Red + report.getReportedBy() + " " + "§d" + "Time: " + sdf.format(report.getTime()) + " " + "§b" + "Proof: "
							+ report.getProof();
					CivMessage.send(sender, message);
				}
			}
		}));
		add(new CustomCommand("playerlist").withDescription(CivSettings.localize.localizedString("adcmd_report_playerlistDesc", Bukkit.getServer().getServerName())).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				SimpleDateFormat sdf = CivGlobal.dateFormat;
				CivMessage.sendHeading(sender, "Player Reports " + Bukkit.getServerName());
				for (Report report : CivGlobal.getReports()) {
					if (report.isClosed() || report.isBug()) continue;
					String message = CivColor.LightGray + "(" + report.getId() + ") " + "§a" + "Reporter: " + CivColor.Red + report.getReportedBy() + " " + "§d" + "Time: " + sdf.format(report.getTime()) + " " + "§b" + "Proof: "
							+ report.getProof() + " " + "§2" + "Player: " + report.getCause();
					CivMessage.send(sender, message);
				}
			}
		}));
		add(new CustomCommand("close").withDescription(CivSettings.localize.localizedString("adcmd_report_closelistDesc", Bukkit.getServer().getServerName())).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivColor.Red + CivSettings.localize.localizedString("adcmd_report_close_enterID"));
				Integer reportID = Integer.parseInt(args[0]);
				Report report = CivGlobal.getReportById(reportID);
				if (report == null) throw new CivException(CivColor.Red + CivSettings.localize.localizedString("adcmd_report_close_unkReport", reportID));
				if (report.isClosed()) {
					Object[] arrobject = new Object[2];
					arrobject[0] = reportID;
					arrobject[1] = report.isBug() ? "bug" : "player";
					throw new CivException(CivColor.Red + CivSettings.localize.localizedString("adcmd_report_close_closedReport", arrobject));
				}
				if (args.length < 2) throw new CivException(CivColor.Red + CivSettings.localize.localizedString("adcmd_report_close_enterArgs"));
				StringBuilder messages = new StringBuilder();
				for (int i = 1; i < args.length; ++i) {
					messages.append(args[i]).append(i == args.length - 1 ? "" : " ");
				}
				report.close(sender.getName(), messages.toString());
				CivMessage.sendSuccess(sender, CivColor.Red + CivSettings.localize.localizedString("adcmd_report_close_succusess", reportID, messages.toString()));
				Resident senderResident = CivGlobal.getResident(report.getReportedBy());
				senderResident.setReportChecked(true);
				senderResident.setReportResult(sender.getName() + "///" + messages + "///" + report.getCloseTime());
				senderResident.save();
				CivMessage.sendSuccess(senderResident, CivColor.BOLD + CivColor.UNDERLINE + senderResident.getReportResult().split("///")[0] + " responded to your report. Use '/res report' for more infomation.");
			}
		}));
	}
}
