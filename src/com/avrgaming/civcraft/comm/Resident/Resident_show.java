package com.avrgaming.civcraft.comm.Resident;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Resident;

public class Resident_show extends AbstractSubCommand {

	public Resident_show() {
		super("resident show");
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public void onComm(CommandSender arg0, Command arg1, String arg2, String[] arg3) throws CivException {
		if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_res_showPrompt"));
		Resident resident = getNamedResident(1);
		ResidentCommand.show(sender, resident);
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
