package com.avrgaming.civcraft.comm;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class MenuCustomTab extends CustonTaber {

	private MenuCustomCommand perent;

	public MenuCustomTab(MenuCustomCommand perent) {
		super();
		this.perent = perent;
	}

	protected List<String> newTabList(String arg) {
		List<String> tabList = new ArrayList<>();
		for (CustonCommand s : perent.getSubCommands()) {
			if (s.getString_cmd().startsWith(arg)) tabList.add(s.getString_cmd());
		}
		return tabList;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return getTabList(buildArg(args));
	}

}
