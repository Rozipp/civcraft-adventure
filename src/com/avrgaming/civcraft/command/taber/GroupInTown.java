package com.avrgaming.civcraft.command.taber;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.permission.PermissionGroup;

public class GroupInTown extends AbstractTaber {
	public GroupInTown() {
	}

	@Override
	public List<String> getTabList(CommandSender sender, String arg) throws CivException {
		Town town = Commander.getSelectedTown(sender);
		ArrayList<String> l = new ArrayList<>();
		for (PermissionGroup grp : town.GM.getAllGroups()) {
			if (grp.getName().toLowerCase().startsWith(arg)) l.add(grp.getName());
		}
		return l;
	}
}