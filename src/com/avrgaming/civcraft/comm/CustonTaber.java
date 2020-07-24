package com.avrgaming.civcraft.comm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public abstract class CustonTaber implements TabCompleter {
	
	protected Map<String, List<String>> cashTabList = new LinkedHashMap<>();

	protected List<String> addTabList(String arg, List<String> tabList) {
		if (tabList.isEmpty()) {
			if (arg.isEmpty())
				tabList = null;
			else
				return getTabList("");
		}
		cashTabList.put(arg, tabList);
		return tabList;
	}

	protected abstract List<String> newTabList(String arg);

	protected List<String> getTabList(String arg) {
		if (cashTabList.containsKey(arg))
			return cashTabList.get(arg);
		else
			return addTabList(arg, newTabList(arg));
	}

	protected String buildArg(String[] args) {
		return (args.length < 1 || args[0] == null) ? "" : args[0];
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return getTabList(buildArg(args));
	}
}
