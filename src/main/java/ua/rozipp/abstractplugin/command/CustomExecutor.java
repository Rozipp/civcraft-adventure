package ua.rozipp.abstractplugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import ua.rozipp.abstractplugin.exception.AException;

public interface CustomExecutor {
		void run(CommandSender sender, Command cmd, String label, String[] args) throws AException, InvalidCommandArgument;
	}