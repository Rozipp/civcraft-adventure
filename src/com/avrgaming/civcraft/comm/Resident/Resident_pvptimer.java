package com.avrgaming.civcraft.comm.Resident;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class Resident_pvptimer extends AbstractSubCommand {

	public Resident_pvptimer() {
		super("resident pvptimer");
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public void onComm(CommandSender arg0, Command arg1, String arg2, String[] arg3) throws CivException {
		Resident resident = getResident();
		if (!resident.isProtected()) CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_res_pvptimerNotActive"));
		resident.setProtected(false);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_pvptimerSuccess"));

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
