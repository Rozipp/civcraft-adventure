package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.war.War;

public class ValidateAll implements Runnable
{
    @Override
    public void run() {
        if (War.isWarTime()) {
            War.validatePlayerJoin((Player[])Bukkit.getOnlinePlayers().toArray(new Player[Bukkit.getOnlinePlayers().size()]));
        }
    }
}
