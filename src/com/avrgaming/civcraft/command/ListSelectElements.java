package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.Collection;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

/** <b>Хранит в себе команды, их номера и коментарий к командам</b>
 * 
 * @author Rozipp */
public class ListSelectElements {

	public String perentCommand = "FIXME";
	/** Выводить ли сообщение по умолчанию? */
	protected boolean sendUnknownToDefault = false;
	private ArrayList<String> commands;
	private ArrayList<String> coments;

	public ListSelectElements() {
		this.commands = new ArrayList<String>();
		this.coments = new ArrayList<String>();
	}
	/** Never used */
	public ListSelectElements(String perentCommand) {
		this.commands = new ArrayList<String>();
		this.coments = new ArrayList<String>();
		this.perentCommand = perentCommand;
	}

	public String getCommand(Integer i) {
		return commands.get(i);
	}

	public Collection<String> getCommands() {
		return commands;
	}

	public String getComent(Integer i) {
		return coments.get(i);
	}

	public Collection<String> getComents() {
		return coments;
	}

	public Integer size() {
		return coments.size();
	}

	public void add(String command, String coment) {
		this.commands.add(command);
		this.coments.add(coment);
	}

	public void showListSelectElement(Object sender) {
		CivMessage.send(sender, CivColor.LightPurple + "Help " + CivColor.BOLD + CivColor.Green + perentCommand);
		for (Integer i = 0; i < size(); i++) {
			String string = "";
			string = CivColor.AddTabToString(string, CivColor.BOLD + CivColor.Green + "(" + i + ")", 8);
			String getCt = getComent(i);
			Integer index = getCt.lastIndexOf("]") + 1;
			string = CivColor.AddTabToString(string, CivColor.LightPurple + getCommand(i) + getCt.substring(0, index), 18);
			string = CivColor.AddTabToString(string, CivColor.LightGray + getComent(i).substring(index), 0);
			string = string.replace("[", CivColor.Yellow + "[");
			string = string.replace("]", "]" + CivColor.LightGray);
			string = string.replace("(", CivColor.Yellow + "(");
			string = string.replace(")", ")" + CivColor.LightGray);

			CivMessage.send(sender, string);
		}
	}
}