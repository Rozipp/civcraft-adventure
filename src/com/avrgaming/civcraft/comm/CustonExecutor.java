package com.avrgaming.civcraft.comm;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;

public abstract class CustonExecutor {
	
	public abstract boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) throws CivException;
	
}
