/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

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
	public abstract void doDefaultAction(CommandSender sender) throws CivException;

	/* Called on syntax error. */
	public void showHelp(CommandSender sender) {
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
			String coment = CivColor.LightGray + aCommand.getDescription();
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
			// GuiItem g = new GuiItem();
			// g.setMaterial(Material.APPLE);
			// g.setTitle(title);
			// g.addLore(coment);
			// gi.addGuiItem(i, g);
		}
		// gi.openInventory();
	}

	public List<CustomCommand> getSubCommands() {
		return subCommands;
	}

	@Override
	public List<String> onTab(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) try {
			return getTabs().get(0).getTabList(sender, args[0]);
		} catch (CivException e) {
			e.printStackTrace();
			CivMessage.sendError(sender, e.getMessage());
			return new ArrayList<>();
		}

		for (CustomCommand cc : this.getSubCommands()) {
			boolean foundalias = false;
			if (cc.getAliases() != null) for (String al : cc.getAliases()) {
				if (al.equalsIgnoreCase(args[0])) {
					foundalias = true;
					break;
				}
			}
			if (foundalias || cc.getString_cmd().equalsIgnoreCase(args[0])) {
				if (cc.getTabs().isEmpty())
					return null;
				else {
					String[] newargs = Commander.stripArgs(args, 1);
					return cc.onTab(sender, cmd, label + " " + args[0], newargs);
				}
			}
		}
		return new ArrayList<>();
	}

	public class MenuCustomExecutor extends CustonExecutor {
		private MenuAbstractCommand perent;

		public MenuCustomExecutor(MenuAbstractCommand perent) {
			super();
			this.perent = perent;
		}

		@Override
		public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
			String ar = "[";
			for (String s : args)
				ar = ar + s + ",";
			ar = ar + "]";
			CivLog.debug("CommandSender:" + sender + "   Command:" + cmd + "   String:" + label + "  args:" + ar);

			if (args.length == 0) {
				perent.doDefaultAction(sender);
				return;
			}

			if (args[0].equalsIgnoreCase("help")) {
				perent.showHelp(sender);
				return;
			}

			String newcomm = args[0];
			String[] newargs = Commander.stripArgs(args, 1);
			String newString_cmd = label + " " + args[0];

			for (CustomCommand ac : perent.getSubCommands()) {
				if (ac.getString_cmd().equalsIgnoreCase(newcomm)) {
					ac.getExecutor().run(sender, cmd, newString_cmd, newargs);
					return;
				}
				if (ac.getAliases() != null && ac.getAliases().contains(newcomm.toLowerCase())) {
					ac.getExecutor().run(sender, cmd, newString_cmd, newargs);
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
			for (CustomCommand s : ((MenuAbstractCommand) perent).getSubCommands()) {
				if (s.getString_cmd().startsWith(arg)) tabList.add(s.getString_cmd());
				if (s.getAliases() != null) {
					for (String al : s.getAliases())
						if (al.startsWith(arg)) tabList.add(al);
				}
			}
			return tabList;
		}

	}

}
