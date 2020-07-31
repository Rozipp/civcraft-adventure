package com.avrgaming.civcraft.command;

import java.lang.reflect.Field;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;

public class CommanderRegistration {

	private static String fromPlugin = CivCraft.getPlugin().getName();
	protected static CommandMap cmap;

	public static void register(CustomCommand custonCommand) {
		ReflectCommand command;
		if (custonCommand.getString_cmd() != null && !custonCommand.getString_cmd().isEmpty())
			command = new ReflectCommand(custonCommand);
		else
			throw new CommandNotPreparedException("Command does not have a name.");

		getCommandMap().register((fromPlugin != null ? fromPlugin : ""), command);
		CivLog.debug("register command " + custonCommand.getString_cmd());
	}

	private static CommandMap getCommandMap() {
		if (cmap == null) {
			try {
				final Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
				f.setAccessible(true);
				cmap = (CommandMap) f.get(Bukkit.getServer());
				return getCommandMap();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
			if (cmap != null) return cmap;
		return getCommandMap();
	}

	private static final class ReflectCommand extends Command {

		private CustomCommand custonCommand;

		protected ReflectCommand(CustomCommand custonCommand) {
			super(custonCommand.getString_cmd());
			this.custonCommand = custonCommand;
			if (custonCommand.getAliases() != null) this.setAliases(custonCommand.getAliases());
			if (custonCommand.getDescription() != null) this.setDescription(custonCommand.getDescription());
			if (custonCommand.getPermission() != null) this.setPermission(custonCommand.getPermission());
			if (custonCommand.getPermissionMessage() != null) this.setPermissionMessage(custonCommand.getPermissionMessage());
			if (custonCommand.getUsage() != null) this.setUsage(custonCommand.getUsage());
		}

		@Override
		public boolean execute(CommandSender sender, String commandLabel, String[] args) {
			return custonCommand.onCommand(sender, this, commandLabel, args);
		}

		@Override
		public List<String> tabComplete(CommandSender sender, String commandLabel, String[] args) {
			return custonCommand.onTab(sender, this, commandLabel, args);
		}
	}

	@SuppressWarnings("serial")
	public static class CommandNotPreparedException extends RuntimeException {
		public CommandNotPreparedException(String message) {
			super(message);
		}
	}

}
