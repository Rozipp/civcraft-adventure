package com.avrgaming.civcraft.comm;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CustonCommand {

	private String string_cmd;
	private String description;
	private List<String> aliases = null;
	private String usage = null;
	private String permission = null;
	private String permissionMessage = null;
	private CustonExecutor executor = null;
	private CustonTaber tab = null;

	public CustonCommand(String string_cmd) {
		this.setString_cmd(string_cmd);
	}

	public CustonCommand withExecutor(CustonExecutor commandExecutor) {
		this.executor = commandExecutor;
		return this;
	}

	public CustonCommand withPermissionMessage(String message) {
		this.permissionMessage = ChatColor.translateAlternateColorCodes('&', message);
		return this;
	}

	public CustonCommand withPermission(String permission) {
		this.permission = permission;
		return this;
	}

	public CustonCommand withUsage(String usage) {
		this.usage = usage;
		return this;
	}

	public CustonCommand withAliases(String... aliases) {
		this.aliases = Arrays.asList(aliases);
		return this;
	}

	public CustonCommand withDescription(String description) {
		this.setDescription(description);
		return this;
	}

	public CustonCommand withTabCompleter(CustonTaber tab) {
		this.setTab(tab);
		return this;
	}

	public void setAliases(String... aliases) {
		this.aliases = Arrays.asList(aliases);
	}

}