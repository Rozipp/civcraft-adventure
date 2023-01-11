package ua.rozipp.abstractplugin.command;

import org.bukkit.command.CommandSender;
import ua.rozipp.abstractplugin.exception.InvalidPermissionException;

public abstract class AbstractValidator {
	public abstract void isValid(CommandSender sender) throws InvalidPermissionException;
}
