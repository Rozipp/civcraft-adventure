package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;

public class AllCivTaber extends AbstractTaber {

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase();
		List<String> potentialMatches = new ArrayList<>();
		for (Civilization civ : CivGlobal.getCivs()) {
			try {
				if (civ.getName().toLowerCase().startsWith(name)) potentialMatches.add(civ.getName());
			} catch (Exception e) {}
		}
		return potentialMatches;
	}
}