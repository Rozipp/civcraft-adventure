package com.avrgaming.civcraft.threading.tasks;

import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.war.War;

public class ChangePlayerTime extends BukkitRunnable
{
    public static boolean removeBarNow;
    public static Integer mins;
    
    @SuppressWarnings("unused")
	public void run() {
        final Player[] players = new Player[Bukkit.getOnlinePlayers().size()];
        if (ChangePlayerTime.mins == null) {
            int minutes = 0;
            try {
                minutes = CivSettings.getInteger(CivSettings.warConfig, "war.time_length");
            }
            catch (InvalidConfiguration e) {
                e.printStackTrace();
            }
            ChangePlayerTime.mins = minutes;
        }
        if (ChangePlayerTime.removeBarNow) {
            ChangePlayerTime.removeBarNow = false;
        }
        if (War.isWarTime()) {
            final long lenght = 60000 * ChangePlayerTime.mins;
            final float progress = (lenght - (War.getEnd().getTime() - (float)Calendar.getInstance().getTimeInMillis())) / lenght;
        }
    }
    
    static {
        ChangePlayerTime.removeBarNow = false;
        ChangePlayerTime.mins = null;
    }
}

