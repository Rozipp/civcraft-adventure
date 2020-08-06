package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

/**
 * <p>
 * Абстрактный клас служит оболочкой для создания подменю
 * </p>
 * @author rozipp */
public abstract class MenuAbstractCommand extends CustomCommand {

	protected String displayName = "FIXME";
	private List<CustomCommand> subCommands = new ArrayList<CustomCommand>();

	public MenuAbstractCommand(String perentCommand) {
		super(perentCommand);
		this.setExecutor(new MenuCustomExecutor(this));
		this.addTab(new MenuCustomTab(this));
	}

	public void add(CustomCommand menu) {
		this.subCommands.add(menu);
	}

	/* Called when no arguments are passed. */
	public void doDefaultAction(CommandSender sender) throws CivException {
		showBasicHelp(sender);
	}

	public void showBasicHelp(CommandSender sender) {
		CivMessage.sendHeading(sender, displayName + " " + CivSettings.localize.localizedString("cmd_CommandHelpTitle"));
		CivMessage.send(sender, CivColor.LightPurple + "Help " + CivColor.BOLD + CivColor.Green + getString_cmd());

		// GuiInventory gi = new GuiInventory(player, null);
		// gi.setTitle(this.perentCommand);
		for (Integer i = 0; i < subCommands.size(); i++) {
			CustomCommand aCommand = subCommands.get(i);
			try {
				aCommand.valide(sender);
			} catch (CivException e) {
				continue;
			}

			String nomer = CivColor.BOLD + CivColor.Green + "(" + i + ")";

			String altComms = "";
			if (aCommand.getAliases() != null) for (String s : aCommand.getAliases())
				altComms = altComms + " (" + s + ")";
			// Integer index = aCommand.getDescription().lastIndexOf("]") + 1;
			// String title = CivColor.LightPurple + aCommand.getString_cmd() + altComms + aCommand.getDescription().substring(0, index);
			String title = CivColor.LightPurple + aCommand.getString_cmd() + altComms;
			String coment = CivColor.LightGray + (aCommand.getDescription() != null ? aCommand.getDescription().trim() : "null");
			// String coment = CivColor.LightGray + aCommand.getDescription().substring(index);
			coment = coment.replace("[", CivColor.Yellow + "[");
			coment = coment.replace("]", "]" + CivColor.LightGray);
			coment = coment.replace("(", CivColor.Yellow + "(");
			coment = coment.replace(")", ")" + CivColor.LightGray);

			String string = "";
			string = CivColor.addTabToString(string, nomer, 8);
			string = CivColor.addTabToString(string, title, 18);
			string = CivColor.addTabToString(string, coment, 0);
			CivMessage.send(sender, string);
		}
	}

	public List<CustomCommand> getSubCommands() {
		return subCommands;
	}

	@Override
	public List<String> onTab(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) try {
			return getTabs().get(0).getTabList(sender, args[0].toLowerCase());
		} catch (CivException e) {
			e.printStackTrace();
			CivMessage.sendError(sender, e.getMessage());
			return new ArrayList<>();
		}
		String arg = args[0];
		String[] newargs = Commander.stripArgs(args, 1);
		try {
			Integer index = Integer.parseInt(arg);
			CustomCommand cc = this.getSubCommands().get(index);
			return cc.onTab(sender, cmd, label + " " + arg, newargs);
		} catch (Exception e) {}

		for (CustomCommand cc : this.getSubCommands()) {
			if (cc.getString_cmd().equalsIgnoreCase(arg)) {
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

	public class MenuCustomExecutor implements CustonExecutor {
		private MenuAbstractCommand perent;

		public MenuCustomExecutor(MenuAbstractCommand perent) {
			super();
			this.perent = perent;
		}

		@Override
		public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
			if (args.length == 0) {
				perent.doDefaultAction(sender);
				return;
			}

			if (args[0].equalsIgnoreCase("help")) {
				perent.showBasicHelp(sender);
				return;
			}

			String newcomm = args[0];
			String[] newargs = Commander.stripArgs(args, 1);

			try {
				Integer index = Integer.parseInt(newcomm);
				if (index < 0 || index >= subCommands.size()) throw new CivException("Недопустимый индекс команды");
				CustomCommand cc = subCommands.get(index);
				cc.onCommand(sender, cmd, label + " " + cc.getString_cmd(), newargs);
				return;
			} catch (NumberFormatException e) {}

			for (CustomCommand cc : perent.getSubCommands()) {
				if (cc.getString_cmd().equalsIgnoreCase(newcomm)) {
					cc.onCommand(sender, cmd, label + " " + cc.getString_cmd(), newargs);
					return;
				}
				if (cc.getAliases() != null && cc.getAliases().contains(newcomm.toLowerCase())) {
					cc.onCommand(sender, cmd, label + " " + cc.getString_cmd(), newargs);
					return;
				}
			}
			throw new CivException("Command not found");
		}

	}

	public class MenuCustomTab extends AbstractCashedTaber {
		MenuAbstractCommand perent;

		public MenuCustomTab(MenuAbstractCommand perent) {
			this.perent = perent;
		}

		@Override
		protected List<String> newTabList(String arg) {
			List<String> tabList = new ArrayList<>();
			for (CustomCommand s : perent.getSubCommands()) {
				if (s.getString_cmd().startsWith(arg)) tabList.add(s.getString_cmd());
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
