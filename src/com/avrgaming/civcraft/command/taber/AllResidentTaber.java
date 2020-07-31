package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;

public class AllResidentTaber extends AbstractTaber {

	public AllResidentTaber() {
	}

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase().replace("%", "(\\w*)");
		List<String> potentialMatches = new ArrayList<>();
		for (Resident resident : CivGlobal.getResidents()) {
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
