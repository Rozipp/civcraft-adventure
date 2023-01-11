package com.avrgaming.civcraft.command.taber;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;

/** Клас кешируемых аргументов для дополнения клавишей Tab
 * @author rozipp */
public abstract class AbstractCashedTaber implements AbstractTaber {

	protected final Map<String, List<String>> cashTabList = new LinkedHashMap<>();

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
