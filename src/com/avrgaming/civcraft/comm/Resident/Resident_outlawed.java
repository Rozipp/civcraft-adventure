package com.avrgaming.civcraft.comm.Resident;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class Resident_outlawed extends AbstractSubCommand {

	public Resident_outlawed() {
		super("resident outlawed");
	}


	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public void onComm(CommandSender arg0, Command arg1, String arg2, String[] arg3) throws CivException {
		Resident resident = this.getResident();
		StringBuilder outlaws = new StringBuilder();
		for (Town town : CivGlobal.getTowns()) {
			if (!town.isOutlaw(resident)) continue;
			outlaws.append(CivColor.Red).append(town.getName()).append(" [").append(town.getCiv().getName()).append("] ").append("\n");
		}
		if (outlaws.toString().equals("")) {
			throw new CivException(CivSettings.localize.localizedString("cmd_res_outlawed_noOne"));
		}
		CivMessage.send((Object) this.sender, CivSettings.localize.localizedString("cmd_res_outlawed_list", outlaws.toString()));
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
