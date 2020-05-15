package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.List;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

/**
 * <b>Хранит в себе команды, их номера и коментарий к командам</b>
 * 
 * @author Rozipp
 */
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

	/** Never used */
	public ListSelectElements(String perentCommand) {
		this.perentCommand = perentCommand;
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

	public void showListSelectElement(Object sender) {
		CivMessage.send(sender, CivColor.LightPurple + "Help " + CivColor.BOLD + CivColor.Green + perentCommand);
		for (Integer i = 0; i < size(); i++) {
			String string = "";
			string = CivColor.addTabToString(string, CivColor.BOLD + CivColor.Green + "(" + i + ")", 8);
			String getCt = getComent(i);
			Integer index = getCt.lastIndexOf("]") + 1;
			String altComms = "";
			for (String s : getAltCommands(i))
				altComms = altComms + "(" + s + ")";
			string = CivColor.addTabToString(string, CivColor.LightPurple + getCommand(i) +altComms + getCt.substring(0, index), 18);
			string = CivColor.addTabToString(string, CivColor.LightGray + getComent(i).substring(index), 0);
			string = string.replace("[", CivColor.Yellow + "[");
			string = string.replace("]", "]" + CivColor.LightGray);
			string = string.replace("(", CivColor.Yellow + "(");
			string = string.replace(")", ")" + CivColor.LightGray);

			CivMessage.send(sender, string);
		}
	}
}