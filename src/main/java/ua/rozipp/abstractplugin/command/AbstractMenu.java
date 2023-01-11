package ua.rozipp.abstractplugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import ua.rozipp.abstractplugin.AColor;
import ua.rozipp.abstractplugin.AMessenger;
import ua.rozipp.abstractplugin.command.taber.AbstractCashedTaber;
import ua.rozipp.abstractplugin.exception.AException;
import ua.rozipp.abstractplugin.exception.InvalidPermissionException;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Абстрактный клас служит оболочкой для создания подменю
 * </p>
 * @author rozipp */
public abstract class AbstractMenu extends CustomCommand {

	protected String displayName = "FIXME";
	private final List<CustomCommand> subCommands = new ArrayList<>();

	public AbstractMenu(String perentCommand) {
		super(perentCommand);
		this.setExecutor(new MenuCustomExecutor(this));
		this.addTab(new MenuCustomTab(this));
	}

	public void add(CustomCommand menu) {
		this.subCommands.add(menu);
	}

	/* Called when no arguments are passed. */
	public void doDefaultAction(CommandSender sender) {
		showBasicHelp(sender);
	}

	public void showBasicHelp(CommandSender sender) {
		getMessenger().sendHeading(sender, displayName + " " + getLocalize().getString(sender, "cmd_CommandHelpTitle"));
		getMessenger().sendMessageString(sender, AColor.LightPurple + "Help " + AColor.BOLD + AColor.Green + getCommandString());

		// GuiInventory gi = new GuiInventory(player, null);
		// gi.setTitle(this.perentCommand);
		for (int i = 0; i < subCommands.size(); i++) {
			CustomCommand aCommand = subCommands.get(i);
			try {
				aCommand.valid(sender);
			} catch (InvalidPermissionException e) {
				continue;
			}

			String nomer = AColor.BOLD + AColor.Green + "(" + i + ")";

			StringBuilder altComms = new StringBuilder();
			if (aCommand.getAliases() != null) for (String s : aCommand.getAliases())
				altComms.append(" (").append(s).append(")");
			// Integer index = aCommand.getDescription().lastIndexOf("]") + 1;
			// String title = RJMColor.LightPurple + aCommand.getString_cmd() + altComms + aCommand.getDescription().substring(0, index);
			String title = AColor.LightPurple + aCommand.getCommandString() + altComms;
			String coment = AColor.LightGray + (aCommand.getDescription() != null ? aCommand.getDescription().trim() : "null");
			// String coment = RJMColor.LightGray + aCommand.getDescription().substring(index);
			coment = coment.replace("[", AColor.Yellow + "[");
			coment = coment.replace("]", "]" + AColor.LightGray);
			coment = coment.replace("(", AColor.Yellow + "(");
			coment = coment.replace(")", ")" + AColor.LightGray);

			String string = "";
			string = AMessenger.addTabToString(string, nomer, 8);
			string = AMessenger.addTabToString(string, title, 18);
			string = AMessenger.addTabToString(string, coment, 0);
			getMessenger().sendMessageString(sender, string);
		}
	}

	public List<CustomCommand> getSubCommands() {
		return subCommands;
	}

	@Override
	public List<String> onTab(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) try {
			return getTabs().get(0).getTabList(sender, args[0].toLowerCase());
		} catch (AException e) {
			e.printStackTrace();
			getMessenger().sendErrorString(sender, e.getMessage());
			return new ArrayList<>();
		}
		String arg = args[0];
		String[] newargs = getPlugin().getCommander().stripArgs(args, 1);
		try {
			int index = Integer.parseInt(arg);
			CustomCommand cc = this.getSubCommands().get(index);
			return cc.onTab(sender, cmd, label + " " + arg, newargs);
		} catch (Exception ignored) {}

		for (CustomCommand cc : this.getSubCommands()) {
			if (cc.getCommandString().equalsIgnoreCase(arg)) {
				return cc.onTab(sender, cmd, label + " " + arg, newargs);
			}
			if (cc.getAliases() != null) {
				for (String al : cc.getAliases()) {
					if (al.equalsIgnoreCase(arg)) return cc.onTab(sender, cmd, label + " " + arg, newargs);
				}
			}
		}
		return new ArrayList<>();
	}

	public class MenuCustomExecutor implements CustomExecutor {
		private final AbstractMenu perent;

		public MenuCustomExecutor(AbstractMenu perent) {
			super();
			this.perent = perent;
		}

		@Override
		public void run(CommandSender sender, Command cmd, String label, String[] args) throws InvalidCommandArgument {
			if (args.length == 0) {
				perent.doDefaultAction(sender);
				return;
			}

			if (args[0].equalsIgnoreCase("help")) {
				perent.showBasicHelp(sender);
				return;
			}

			String newcomm = args[0];
			String[] newargs = getPlugin().getCommander().stripArgs(args, 1);

			try {
				int index = Integer.parseInt(newcomm);
				if (index < 0 || index >= subCommands.size()) throw new InvalidCommandArgument("Недопустимый индекс команды");
				CustomCommand cc = subCommands.get(index);
				cc.onCommand(sender, cmd, label + " " + cc.getCommandString(), newargs);
				return;
			} catch (NumberFormatException ignored) {}

			for (CustomCommand cc : perent.getSubCommands()) {
				if (cc.getCommandString().equalsIgnoreCase(newcomm)) {
					cc.onCommand(sender, cmd, label + " " + cc.getCommandString(), newargs);
					return;
				}
				if (cc.getAliases() != null && cc.getAliases().contains(newcomm.toLowerCase())) {
					cc.onCommand(sender, cmd, label + " " + cc.getCommandString(), newargs);
					return;
				}
			}
			throw new InvalidCommandArgument("Command not found");
		}

	}

	public static class MenuCustomTab extends AbstractCashedTaber {
		AbstractMenu perent;

		public MenuCustomTab(AbstractMenu perent) {
			this.perent = perent;
		}

		@Override
		protected List<String> newTabList(String arg) {
			List<String> tabList = new ArrayList<>();
			for (CustomCommand s : perent.getSubCommands()) {
				if (s.getCommandString().startsWith(arg)) tabList.add(s.getCommandString());
				// else {
				// if (s.getAliases() != null) {
				// for (String al : s.getAliases())
				// if (al.startsWith(arg)) tabList.add(al);
				// }
				// }
			}
			return tabList;
		}

	}

}
