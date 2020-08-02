package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;

public class AllResidentTaber extends AbstractTaber {

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase();
		List<String> potentialMatches = new ArrayList<>();
		for (Resident resident : CivGlobal.getResidents()) {
			try {
				if (resident.getName().toLowerCase().startsWith(name)) potentialMatches.add(resident.getName());
			} catch (Exception e) {}
		}
		return potentialMatches;
	}
}
