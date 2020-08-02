package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;

/** Возвращает все города в мире
 * @author rozipp
 *
 */
public class TownInWorldTaber implements AbstractTaber {

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		String name = arg.toLowerCase();
		List<String> potentialMatches = new ArrayList<>();
		for (Town town : CivGlobal.getTowns()) {
			try {
				if (town.getName().toLowerCase().startsWith(name)) potentialMatches.add(town.getName());
			} catch (Exception e) {}
		}
		return potentialMatches;
	}

}
