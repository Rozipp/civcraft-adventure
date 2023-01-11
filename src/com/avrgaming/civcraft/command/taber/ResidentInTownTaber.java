package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

/** Возвращает имена игроков в выбранном городе
 * @author rozipp */
public class ResidentInTownTaber implements AbstractTaber {

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		Town town = Commander.getSelectedTown(sender);
		String name = arg.toLowerCase();
		List<String> potentialMatches = new ArrayList<>();
		for (Resident resident : town.getResidents()) {
			try {
				if (resident.getName().toLowerCase().startsWith(name)) potentialMatches.add(resident.getName());
			} catch (Exception e) {}
		}
		return potentialMatches;
	}
}
