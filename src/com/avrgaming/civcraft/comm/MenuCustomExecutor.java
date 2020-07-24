package com.avrgaming.civcraft.comm;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;

public class MenuCustomExecutor extends CustonExecutor {
	private MenuCustomCommand perent;

	public MenuCustomExecutor(MenuCustomCommand abstractCommand) {
		super();
		perent = abstractCommand;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		String ar = "[";
		for (String s : args)
			ar = ar + s + ",";
		ar = ar + "]";
		CivLog.debug("CommandSender:" + sender + "   Command:" + cmd + "   String:" + label + "  args:" + args);

		try {
			if (args.length == 0) {
				perent.doDefaultAction(sender);
				return false;
			}
		} catch (CivException e) {
			CivMessage.sendError(sender, e.getMessage());
			return false;
		}

		if (args[0].equalsIgnoreCase("help")) {
			perent.showHelp(sender);
			return true;
		}

		String[] newargs = Commander.stripArgs(args, 1);
		String newString_cmd = label + " " + args[0];

		for (CustonCommand ac : perent.getSubCommands()) {
			if (ac.getString_cmd().equalsIgnoreCase(args[0]) || ac.getAliases().contains(args[0].toLowerCase())) {
				return ac.getExecutor().onCommand(sender, cmd, newString_cmd, newargs);
			}
		}
		return false;
	}

}
