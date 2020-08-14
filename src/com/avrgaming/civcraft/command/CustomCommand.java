package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Validators.Validator;
import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Общий клас для команд. После создания команды,добавление параметров возможно как через сеттеры (set...()), так через билдеры (with..())
 * </p>
 * @param executor    - вызываться при выполнении команды. Обязательный параметр.
 * @param description - хранит описание, для вывода в help-е подменю - List<String> aliases - Вариванты альтернативных команд
 * @param validator   - Проверка команды на доступность для CommandSender. Обрабатываються в порядке добавления;
 * @param tab         - Класы дополнения клавишей Tab. Обрабатываються в порядке добавления
 * @author rozipp */
@Setter
@Getter
public class CustomCommand {

	private String string_cmd;
	private String description;
	private List<String> aliases = null;
	private String usage = null;
	private String permission = null;
	private String permissionMessage = null;
	private List<Validator> validators = new ArrayList<>();
	private CustomExecutor executor = null;
	private List<AbstractTaber> tabs = new ArrayList<>();

	public CustomCommand(String string_cmd) {
		this.string_cmd = string_cmd;
	}

	public CustomCommand withValidator(Validator validator) {
		this.addValidator(validator);
		return this;
	}

	public CustomCommand withExecutor(CustomExecutor commandExecutor) {
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

	public void addValidator(Validator validator) {
		this.validators.add(validator);
	}

	public void setAliases(String... aliases) {
		this.aliases = Arrays.asList(aliases);
	}

	public void valide(CommandSender sender) throws CivException {
		if (validators == null) return;
		for (Validator v : validators)
			v.isValide(sender);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		try {
			if (executor == null) throw new CivException("Команда в разработке");
			valide(sender);
			executor.run(sender, cmd, label, args);
			return true;
		} catch (CivException e) {
			CivMessage.sendError(sender, e.getMessage());
			return false;
		}
	}

	public List<String> onTab(CommandSender sender, Command cmd, String label, String[] args) {
		int index = args.length - 1;
		if (index >= 0 && index < getTabs().size()) {
			try {
				return getTabs().get(index).getTabList(sender, args[index].toLowerCase());
			} catch (CivException e) {
				e.printStackTrace();
				CivMessage.sendError(sender, e.getMessage());
			}
		}
		return new ArrayList<>();
	}

	static public interface CustomExecutor {
		public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException;
	}

}