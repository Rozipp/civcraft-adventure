package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class TownInCivTaber extends AbstractTaber {

	public TownInCivTaber() {
	}

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase().replace("%", "(\\w*)");
		List<String> potentialMatches = new ArrayList<>();
		for (Town town : Commander.getSenderCiv(sender).getTowns()) {
			String str = town.getName().toLowerCase();
			try {
				if (str.startsWith(name)) potentialMatches.add(town.getName());
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		return potentialMatches;
	}

}
