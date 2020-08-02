package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class TownInCivTaber extends AbstractTaber {

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase();
		List<String> potentialMatches = new ArrayList<>();
		for (Town town : Commander.getSenderCiv(sender).getTowns()) {
			try {
				if (town.getName().toLowerCase().startsWith(name)) potentialMatches.add(town.getName());
			} catch (Exception e) {}
		}
		return potentialMatches;
	}

}
