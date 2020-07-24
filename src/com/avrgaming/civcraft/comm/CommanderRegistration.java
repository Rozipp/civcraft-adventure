package com.avrgaming.civcraft.comm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;

public class CommanderRegistration {

	private static String fromPlugin = CivCraft.getPlugin().getName();
	protected static CommandMap cmap;

	public static void register(CustonCommand custonCommand) {
		ReflectCommand command;
		if (custonCommand.getString_cmd() != null && !custonCommand.getString_cmd().isEmpty())
			command = new ReflectCommand(custonCommand.getString_cmd());
		else
			throw new CommandNotPreparedException("Command does not have a name.");

		if (custonCommand.getExecutor() != null) command.setExecutor(custonCommand.getExecutor());
		if (custonCommand.getAliases() != null) command.setAliases(custonCommand.getAliases());
		if (custonCommand.getDescription() != null) command.setDescription(custonCommand.getDescription());
		if (custonCommand.getPermission() != null) command.setPermission(custonCommand.getPermission());
		if (custonCommand.getPermissionMessage() != null) command.setPermissionMessage(custonCommand.getPermissionMessage());
		if (custonCommand.getUsage() != null) command.setUsage(custonCommand.getUsage());
		if (custonCommand.getTab() != null) command.setTabCompleter(custonCommand.getTab());

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
			if (cmap != null) {
				return cmap;
			}
		return getCommandMap();
	}

	private static final class ReflectCommand extends Command {

		private CustonExecutor exe = null;
		private CustonTaber tab = null;

		protected ReflectCommand(String command) {
			super(command);
		}

		private void setExecutor(CustonExecutor exe) {
			this.exe = exe;
		}

		private void setTabCompleter(CustonTaber tab) {
			this.tab = tab;
		}

		@Override
		public boolean execute(CommandSender sender, String commandLabel, String[] args) {
			CivLog.debug("execute()  " + commandLabel);

			if (exe != null) try {
				return exe.onCommand(sender, this, commandLabel, args);
			} catch (Exception e) {
				CivMessage.sendError(sender, e.getMessage());
			}
			return false;
		}

		@Override
		public List<String> tabComplete(CommandSender sender, String commandLabel, String[] args) {
			if (tab != null) return tab.onTabComplete(sender, this, getUsage(), args);
			return new ArrayList<>();
		}
	}

	@SuppressWarnings("serial")
	public static class CommandNotPreparedException extends RuntimeException {
		public CommandNotPreparedException(String message) {
			super(message);
		}
	}

}
