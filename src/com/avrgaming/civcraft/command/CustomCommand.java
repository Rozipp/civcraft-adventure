package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CustomCommand {

	private String string_cmd;
	private String description;
	private List<String> aliases = null;
	private String usage = null;
	private String permission = null;
	private String permissionMessage = null;
	private Validator validator = null;
	private CustonExecutor executor = null;
	private List<AbstractTaber> tabs = new ArrayList<>();

	public CustomCommand(String string_cmd) {
		this.string_cmd = string_cmd;
	}

	public CustomCommand withValidator(Validator validator) {
		this.validator = validator;
		return this;
	}

	public CustomCommand withExecutor(CustonExecutor commandExecutor) {
		this.executor = commandExecutor;
		return this;
	}

	public CustomCommand withPermissionMessage(String message) {
		this.permissionMessage = ChatColor.translateAlternateColorCodes('&', message);
		return this;
	}

	public CustomCommand withPermission(String permission) {
		this.permission = permission;
		return this;
	}

	public CustomCommand withUsage(String usage) {
		this.usage = usage;
		return this;
	}

	public CustomCommand withAliases(String... aliases) {
		this.aliases = Arrays.asList(aliases);
		return this;
	}

	public CustomCommand withDescription(String description) {
		this.description = description;
		return this;
	}

	public CustomCommand withTabCompleter(AbstractTaber tab) {
		this.addTab(tab);
		return this;
	}

	public void addTab(AbstractTaber tab) {
		this.tabs.add(tab);
	}

	public void setAliases(String... aliases) {
		this.aliases = Arrays.asList(aliases);
	}

	public void valide(CommandSender sender) throws CivException {
		if (validator != null) validator.isValide(sender);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (executor != null) try {
			valide(sender);
			executor.run(sender, cmd, label, args);
		} catch (CivException e) {
			CivMessage.sendError(sender, e.getMessage());
			e.printStackTrace(); // TODO убрать после дебага
			return false;
		}
		return true;
	}

	public List<String> onTab(CommandSender sender, Command cmd, String label, String[] args) {
		int index = args.length - 1;
		if (index >= 0 && index < getTabs().size()) {
			try {
				return getTabs().get(index).getTabList(sender, args[index]);
			} catch (CivException e) {
				e.printStackTrace();
				CivMessage.sendError(sender, e.getMessage());
			}
		}
		return new ArrayList<>();
	}

	public abstract class CustonExecutor {
		public abstract void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException;
	}

}