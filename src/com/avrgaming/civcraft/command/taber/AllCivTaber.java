package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;

public class AllCivTaber extends AbstractTaber {

	public AllCivTaber() {
	}

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase().replace("%", "(\\w*)");
		List<String> potentialMatches = new ArrayList<>();
		for (Civilization civ : CivGlobal.getCivs()) {
			String str = civ.getName().toLowerCase();
			try {
				if (str.startsWith(name)) potentialMatches.add(civ.getName());
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		return potentialMatches;
	}

}