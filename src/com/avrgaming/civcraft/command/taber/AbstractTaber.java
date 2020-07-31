package com.avrgaming.civcraft.command.taber;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;

public abstract class AbstractTaber  {

	public AbstractTaber() {
	}

	public abstract List<String> getTabList(CommandSender sender, String arg) throws CivException;

}
