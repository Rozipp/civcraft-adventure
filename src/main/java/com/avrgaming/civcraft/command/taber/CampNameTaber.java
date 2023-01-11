package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;

public class CampNameTaber implements AbstractTaber {

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase();
		List<String> potentialMatches = new ArrayList<>();
		for (Camp camp : CivGlobal.getCamps()) {
			try {
				if (camp.getName().toLowerCase().startsWith(name)) potentialMatches.add(camp.getName());
			} catch (Exception e) {}
		}
		return potentialMatches;
	}

}
