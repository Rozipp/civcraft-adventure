package com.avrgaming.civcraft.comm.Resident;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.comm.AbstractCommand;
import com.avrgaming.civcraft.comm.Commander;
import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Resident;

public class Resident_info extends AbstractCommand{

	public Resident_info() {
		super("resident info");
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		Resident resident = Commander.getResident(sender);
		ResidentCommand.show(sender, resident);
		return false;
	}

}
