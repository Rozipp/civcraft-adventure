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
package com.avrgaming.civcraft.comm;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public abstract class MenuCustomCommand extends CustonCommand {

	protected String displayName = "FIXME";
	private List<CustonCommand> subCommands = new ArrayList<CustonCommand>();

	public MenuCustomCommand(String perentCommand) {
		super(perentCommand);
		this.setExecutor(new MenuCustomExecutor(this));
		this.setTab(new MenuCustomTab(this));
	}

	public void add(CustonCommand menu) {
		this.getSubCommands().add(menu);
	}

	/* Called when no arguments are passed. */
	public abstract void doDefaultAction(CommandSender sender) throws CivException;

	/* Called on syntax error. */
	public void showHelp(CommandSender sender) {
		showBasicHelp(sender);
	}

	/* Called before command is executed to check permissions. */
	public abstract void permissionCheck(CommandSender sender) throws CivException;

	public void showBasicHelp(CommandSender sender) {
		CivMessage.sendHeading(sender, displayName + " " + CivSettings.localize.localizedString("cmd_CommandHelpTitle"));
		CivMessage.send(sender, CivColor.LightPurple + "Help " + CivColor.BOLD + CivColor.Green + getString_cmd());

		// GuiInventory gi = new GuiInventory(player, null);
		// gi.setTitle(this.perentCommand);
		for (Integer i = 0; i < getSubCommands().size(); i++) {
			CustonCommand aCommand = getSubCommands().get(i);

			String nomer = CivColor.BOLD + CivColor.Green + "(" + i + ")";

			String altComms = "";
			for (String s : aCommand.getAliases())
				altComms = altComms + " (" + s + ")";
			Integer index = aCommand.getDescription().lastIndexOf("]") + 1;
			String title = CivColor.LightPurple + aCommand.getString_cmd() + altComms + aCommand.getDescription().substring(0, index);

			String coment = CivColor.LightGray + aCommand.getDescription().substring(index);
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

	public List<CustonCommand> getSubCommands() {
		return subCommands;
	}

}
