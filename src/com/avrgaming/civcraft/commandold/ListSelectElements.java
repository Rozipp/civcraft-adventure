package com.avrgaming.civcraft.commandold;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

/** <b>Хранит в себе команды, их номера и коментарий к командам</b>
 * @author Rozipp */
public class ListSelectElements {

	class CommandElement {
		String command;
		List<String> altComm = new ArrayList<String>();
		String coment;
	}

	public String perentCommand = "FIXME";
	/** Выводить ли сообщение по умолчанию? */
	protected boolean sendUnknownToDefault = false;
	private List<CommandElement> commands = new ArrayList<CommandElement>();

	public ListSelectElements() {
	}

	public List<CommandElement> getCommandElemets() {
		return commands;
	}

	public String getCommand(Integer i) {
		return commands.get(i).command;
	}

	public List<String> getAltCommands(Integer i) {
		return commands.get(i).altComm;
	}

	public String getComent(Integer i) {
		return commands.get(i).coment;
	}

	public Integer size() {
		return commands.size();
	}

	public void add(String command, String coment) {
		CommandElement comEl = new CommandElement();
		comEl.command = command;
		comEl.coment = coment;
		this.commands.add(comEl);
	}

	public void add(String command, String alt1, String coment) {
		CommandElement comEl = new CommandElement();
		comEl.command = command;
		comEl.coment = coment;
		comEl.altComm.add(alt1);
		this.commands.add(comEl);
	}

	public void add(String command, String alt1, String alt2, String coment) {
		CommandElement comEl = new CommandElement();
		comEl.command = command;
		comEl.coment = coment;
		comEl.altComm.add(alt1);
		comEl.altComm.add(alt2);
		this.commands.add(comEl);
	}

	public void add(String command, String alt1, String alt2, String alt3, String coment) {
		CommandElement comEl = new CommandElement();
		comEl.command = command;
		comEl.coment = coment;
		comEl.altComm.add(alt1);
		comEl.altComm.add(alt2);
		comEl.altComm.add(alt3);
		this.commands.add(comEl);
	}

	public void add(String command, String[] altComm, String coment) {
		CommandElement comEl = new CommandElement();
		comEl.command = command;
		comEl.coment = coment;
		for (String s : altComm)
			comEl.altComm.add(s);
		this.commands.add(comEl);
	}

	public void showListSelectElement(Player player) {
		CivMessage.send(player, CivColor.LightPurple + "Help " + CivColor.BOLD + CivColor.Green + perentCommand);

		// GuiInventory gi = new GuiInventory(player, null);
		// gi.setTitle(this.perentCommand);
		for (Integer i = 0; i < size(); i++) {
			String string = "";
			String nomer = CivColor.BOLD + CivColor.Green + "(" + i + ")";
			string = CivColor.addTabToString(string, nomer, 8);
			String altComms = "";
			for (String s : getAltCommands(i))
				altComms = altComms + " (" + s + ")";
			Integer index = getComent(i).lastIndexOf("]") + 1;
			String title = CivColor.LightPurple + getCommand(i) + altComms + getComent(i).substring(0, index);

			string = CivColor.addTabToString(string, title, 18);

			String coment = CivColor.LightGray + getComent(i).substring(index);
			coment = coment.replace("[", CivColor.Yellow + "[");
			coment = coment.replace("]", "]" + CivColor.LightGray);
			coment = coment.replace("(", CivColor.Yellow + "(");
			coment = coment.replace(")", ")" + CivColor.LightGray);

			string = CivColor.addTabToString(string, coment, 0);
			CivMessage.send(player, string);
			// GuiItem g = new GuiItem();
			// g.setMaterial(Material.APPLE);
			// g.setTitle(title);
			// g.addLore(coment);
			// gi.addGuiItem(i, g);
		}
		// gi.openInventory();
	}
}