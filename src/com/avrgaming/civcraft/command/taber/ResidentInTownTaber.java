package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class ResidentInTownTaber extends AbstractTaber {

	public ResidentInTownTaber() {
	}

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		Town town = Commander.getSelectedTown(sender);
		String name = arg.toLowerCase().replace("%", "(\\w*)");
		List<String> potentialMatches = new ArrayList<>();
		for (Resident resident : town.getResidents()) {
			String str = resident.getName().toLowerCase();
			try {
				if (str.startsWith(name)) potentialMatches.add(resident.getName());
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		return potentialMatches;
	}
}
