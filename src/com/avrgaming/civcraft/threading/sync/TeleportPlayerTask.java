package com.avrgaming.civcraft.threading.sync;

import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class TeleportPlayerTask extends BukkitRunnable
{
	public Resident resident;
    public Player player;
    public Location location;
    public Camp camp;
    
	public TeleportPlayerTask(final Resident resident, final Player player, final Location location, final Camp camp) {
        this.resident = resident;
        this.player = player;
        this.location = location;
        this.camp = camp;
    }

	public void run() {
        this.resident.isTeleporting = true;
        CivLog.info("Телепортирование игрока " + this.resident.getName() + " в лагерь " + this.camp.getName());
        try {
            int i = 10;
            while (i >= 0) {
                final long timeNow = Calendar.getInstance().getTimeInMillis();
                if (!this.resident.isTeleporting) {
                    CivMessage.sendError(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_aborted"));
                    return;
                }
                if (this.resident.getCamp() == null) {
                    CivMessage.sendError(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_aborted4"));
                    return;
                }
                if (i != 0) {
                    CivMessage.sendSuccess(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_waiting", "§c" + i + "§a", "§6" + Resident.plurals(i, "\u0441\u0435\u043a\u0443\u043d\u0434\u0443", "\u0441\u0435\u043a\u0443\u043d\u0434\u044b", "\u0441\u0435\u043a\u0443\u043d\u0434")));}
                else {
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin)CivCraft.getPlugin(), () -> {
                        final long teleport = timeNow + 1800000L;
                        CivMessage.sendSuccess(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_succuses", CivGlobal.dateFormat.format(teleport)));
                        this.resident.setNextTeleport(teleport);
                        this.resident.save();
                        return;
                    });
                }
                --i;
                Thread.sleep(1000L);
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin)CivCraft.getPlugin(), () -> {
            this.player.teleport(this.location);
            this.resident.isTeleporting = false;
        });
    }

	public void run(final boolean async) {
        if (async) {
            this.runTaskAsynchronously((Plugin)CivCraft.getPlugin());
        }
        else {
            this.runTask((Plugin)CivCraft.getPlugin());
        }
    }
}
