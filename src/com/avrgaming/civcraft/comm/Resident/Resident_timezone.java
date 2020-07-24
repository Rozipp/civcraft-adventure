package com.avrgaming.civcraft.comm.Resident;

import java.util.List;
import java.util.TimeZone;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class Resident_timezone extends AbstractSubCommand {

	public Resident_timezone() {
		super("resident timezone");
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public void onComm(CommandSender arg0, Command arg1, String arg2, String[] arg3) throws CivException {
		Resident resident = getResident();

		if (args.length < 2) {
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_timezoneCurrent") + " " + resident.getTimezone());
			return;
		}

		if (args[1].equalsIgnoreCase("list")) {
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_res_timezoneHeading"));
			String out = "";
			for (String zone : TimeZone.getAvailableIDs()) {
				out += zone + ", ";
			}
			CivMessage.send(sender, out);
			return;
		}

		TimeZone timezone = TimeZone.getTimeZone(args[1]);

		if (timezone.getID().equals("GMT") && !args[1].equalsIgnoreCase("GMT")) {
			CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("var_cmd_res_timezonenotFound1", args[1]));
			CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_res_timezoneNotFound3"));
		}

		resident.setTimezone(timezone.getID());
		resident.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_res_timezoneSuccess", timezone.getID()));

	}

	@Override
	public void doDefaultAction() throws CivException {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void showHelp() {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void permissionCheck() throws CivException {
		// TODO Автоматически созданная заглушка метода

	}

}
