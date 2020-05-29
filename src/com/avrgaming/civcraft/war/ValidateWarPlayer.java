package com.avrgaming.civcraft.war;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ValidateWarPlayer implements Runnable {
	@Override
	public void run() {
		War.validatePlayerJoin((Player[]) Bukkit.getOnlinePlayers().toArray(new Player[Bukkit.getOnlinePlayers().size()]));
	}
}
