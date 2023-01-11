package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Resident;

/** Возвращает имена всех жителей в кемпе
 * @author rozipp */
public class ResidentInCampTaber implements AbstractTaber {

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		Camp camp = Commander.getCurrentCamp(sender);
		String name = arg.toLowerCase();
		List<String> potentialMatches = new ArrayList<>();
		for (Resident resident : camp.getMembers()) {
			try {
				if (resident.getName().toLowerCase().startsWith(name)) potentialMatches.add(resident.getName());
			} catch (Exception e) {}
		}
		return potentialMatches;
	}
}
