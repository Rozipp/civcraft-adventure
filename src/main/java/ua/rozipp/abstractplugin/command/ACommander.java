package ua.rozipp.abstractplugin.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.rozipp.abstractplugin.ALocalizer;
import ua.rozipp.abstractplugin.APlugin;
import ua.rozipp.abstractplugin.exception.AException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * Главный Модуль управления командами.
 * </p>
 * <p>
 * Регистрирует команды по новому, а так же по старому.
 * Хранит в себе статические команды:
 * <li> для работы с исполнителем команды (sender)
 * <li> для работы с аргументами (args)
 * </p>
 * @author rozipp */
public class ACommander {

	private int count = 0;
	private final APlugin plugin;
	private final ALocalizer localize;

	protected CommandMap commandMap;

	public ACommander(APlugin plugin) {
		this.plugin = plugin;
		localize = plugin.getLocalizer();
	}

	// ----------------- arg utils
	public String convertCommand(String comm) {
		String rus = "фисвуапршолдьтщзйкыегмцчня";
		String eng = "abcdefghijklmnopqrstuvwxyz";

		String res = comm;
		for (int i = 0; i < rus.length(); i++)
			res = res.replace(rus.charAt(i), eng.charAt(i));
		return res;
	}

	public String[] stripArgs(String[] someArgs, int amount) {
		if (amount >= someArgs.length) return new String[0];
		String[] argsLeft = new String[someArgs.length - amount];
		System.arraycopy(someArgs, amount, argsLeft, 0, argsLeft.length);
		return argsLeft;
	}

	public String combineArgs(String[] someArgs) {
		StringBuilder combined = new StringBuilder();
		for (String str : someArgs) {
			combined.append(str).append(" ");
		}
		return combined.toString().trim();
	}

	public String makeInfoString(HashMap<String, String> kvs, String lowColor, String highColor) {
		StringBuilder out = new StringBuilder();
		for (String key : kvs.keySet()) {
			out.append(lowColor).append(key).append(": ").append(highColor).append(kvs.get(key)).append(" ");
		}
		return out.toString();
	}

	// -------------- CommandSender

	public Player getPlayer(CommandSender sender) throws AException {
		if (sender instanceof Player) return (Player) sender;
		throw new AException(localize.getString(sender, "cmd_MustBePlayer"));
	}

	// --------------------- get args

	public Integer getNamedInteger(String[] args, int index) throws ua.rozipp.abstractplugin.command.InvalidCommandArgument {
		if (args.length < (index + 1)) throw new ua.rozipp.abstractplugin.command.InvalidCommandArgument(localize.getString("", "cmd_enterNumber"));
		try {
			return Integer.valueOf(args[index]);
		} catch (NumberFormatException e) {
			throw new ua.rozipp.abstractplugin.command.InvalidCommandArgument(args[index] + " " + localize.getString("", "cmd_enterNumerError2"));
		}
	}

	public Double getNamedDouble(String[] args, int index) throws ua.rozipp.abstractplugin.command.InvalidCommandArgument {
		if (args.length < (index + 1)) throw new ua.rozipp.abstractplugin.command.InvalidCommandArgument(localize.getString("", "cmd_enterNumber"));
		try {
			return Double.valueOf(args[index]);
		} catch (NumberFormatException e) {
			throw new ua.rozipp.abstractplugin.command.InvalidCommandArgument(args[index] + " " + localize.getString("", "cmd_enterNumerError"));
		}
	}

	public String getNamedString(String[] args, int index, String messageError) throws ua.rozipp.abstractplugin.command.InvalidCommandArgument {
		if (args.length < (index + 1)) throw new InvalidCommandArgument(messageError);
		return args[index];
	}

	// ---------------------- register

	public void register(ua.rozipp.abstractplugin.command.CustomCommand custonCommand) {
		if (custonCommand.getCommandString() == null || custonCommand.getCommandString().isEmpty())
			throw new CommandNotPreparedException("Command does not have a name.");

		getCommandMap().register((plugin != null ? plugin.getName() : ""), new ReflectCommand(custonCommand));
		count++;
	}

	private CommandMap getCommandMap() {
		if (commandMap == null) {
			try {
				final Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
				f.setAccessible(true);
				commandMap = (CommandMap) f.get(Bukkit.getServer());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return commandMap;
	}

	public int getCount() {
		return this.count;
	}

	/** Клас-обертка для регистрации класов CustomCommand как команды плагина
	 * @author rozipp */
	private static final class ReflectCommand extends Command {

		private final ua.rozipp.abstractplugin.command.CustomCommand custonCommand;

		private ReflectCommand(CustomCommand custonCommand) {
			super(custonCommand.getCommandString());
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

	public static class CommandNotPreparedException extends RuntimeException {
		public CommandNotPreparedException(String message) {
			super(message);
		}
	}

}
