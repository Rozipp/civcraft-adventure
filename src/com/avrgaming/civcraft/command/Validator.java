package com.avrgaming.civcraft.command;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;

public abstract class Validator {
	public abstract void isValide(CommandSender sender) throws CivException;
}
