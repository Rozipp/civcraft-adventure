package com.avrgaming.civcraft.comm.Resident;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;

public class Resident_resetspawn extends AbstractSubCommand {

	public Resident_resetspawn() {
		super("resident resetspawn");
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		// TODO Автоматически созданная заглушка метода
		return null;
	}

	@Override
	public void onComm(CommandSender arg0, Command arg1, String arg2, String[] arg3) throws CivException {
		Player player = getPlayer();
		Location spawn = player.getWorld().getSpawnLocation();
		player.setBedSpawnLocation(spawn, true);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("cmd_res_resetspawnSuccess"));
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
