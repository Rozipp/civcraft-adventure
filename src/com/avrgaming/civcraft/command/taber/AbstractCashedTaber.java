package com.avrgaming.civcraft.command.taber;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;

public abstract class AbstractCashedTaber extends AbstractTaber {

	public AbstractCashedTaber() {
	}

	protected Map<String, List<String>> cashTabList = new LinkedHashMap<>();

	protected List<String> addTabList(String arg, List<String> tabList) {
		if (tabList.isEmpty()) {
			if (arg.isEmpty()) tabList = null;
		}
		cashTabList.put(arg, tabList);
		return tabList;
	}

	protected abstract List<String> newTabList(String arg);

	@Override
	public List<String> getTabList(CommandSender sender, String arg) {
		if (cashTabList.containsKey(arg))
			return cashTabList.get(arg);
		else
			return addTabList(arg, newTabList(arg));
	}

}
