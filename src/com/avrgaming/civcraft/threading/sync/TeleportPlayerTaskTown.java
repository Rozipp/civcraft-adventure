package com.avrgaming.civcraft.threading.sync;

import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class TeleportPlayerTaskTown extends BukkitRunnable
{
    public Resident resident;
    public Player player;
    public Location location;
    public Town town;
    
    public TeleportPlayerTaskTown(final Resident resident, final Player player, final Location location, final Town town) {
        this.resident = resident;
        this.player = player;
        this.location = location;
        this.town = town;
    }
    
    public void run(final boolean async) {
        if (async) {
            this.runTaskAsynchronously((Plugin)CivCraft.getPlugin());
        }
        else {
            this.runTask((Plugin)CivCraft.getPlugin());
        }
    }
    
    public void run() {
        this.resident.isTeleporting = true;
        CivLog.info("\u0422\u0435\u043b\u0435\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0438\u0433\u0440\u043e\u043a\u0430 " + this.resident.getName() + " \u0432 \u0433\u043e\u0440\u043e\u0434 " + this.town.getName());
        try {
            int i = 10;
            while (i >= 0) {
                final long timeNow = Calendar.getInstance().getTimeInMillis();
                if (!this.resident.isTeleporting) {
                    CivMessage.sendError(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_aborted"));
                    return;
                }
                if (this.resident.getTown() == null) {
                    CivMessage.sendError(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_aborted3"));
                    return;
                }
                if (!this.resident.getTreasury().hasEnough(5000.0)) {
                    CivMessage.sendError(this.resident, CivSettings.localize.localizedString("var_teleport_notEnoughMoney", "§a" + (5000 - (int)this.resident.getTreasury().getBalance()) + "§c", "§c" + CivMessage.plurals(5000 - (int)this.resident.getTreasury().getBalance(), "\u043c\u043e\u043d\u0435\u0442\u0430", "\u043c\u043e\u043d\u0435\u0442\u044b", "\u043c\u043e\u043d\u0435\u0442")));
                    return;
                }
                if (i != 0) {
                    CivMessage.sendSuccess(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_waiting", "§c" + i + "§a", "§6" + CivMessage.plurals(i, "\u0441\u0435\u043a\u0443\u043d\u0434\u0443", "\u0441\u0435\u043a\u0443\u043d\u0434\u044b", "\u0441\u0435\u043a\u0443\u043d\u0434")));
                    CivSettings.localize.localizedString("cmd_camp_teleport_waiting", CivColor.RoseBold + i + CivColor.LightGreenBold, CivColor.GoldBold + CivMessage.plurals(i, "\u0441\u0435\u043a\u0443\u043d\u0434\u0443", "\u0441\u0435\u043a\u0443\u043d\u0434\u044b", "c\u0435\u043a\u0443\u043d\u0434"));
                }
                else {
                    if (!this.resident.getTreasury().hasEnough(5000.0)) {
                        CivMessage.sendError(this.resident, CivSettings.localize.localizedString("var_teleport_notEnoughMoney", "§a" + (5000 - (int)this.resident.getTreasury().getBalance()) + "§c", "§c" + CivMessage.plurals(5000 - (int)this.resident.getTreasury().getBalance(), "\u043c\u043e\u043d\u0435\u0442\u0430", "\u043c\u043e\u043d\u0435\u0442\u044b", "\u043c\u043e\u043d\u0435\u0442")));
                        return;
                    }
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin)CivCraft.getPlugin(), () -> {
                    	final long teleport = timeNow + 300000L;
                        CivMessage.sendSuccess(this.resident, CivSettings.localize.localizedString("cmd_camp_teleport_succuses", CivGlobal.dateFormat.format(teleport)));
                        this.resident.getTreasury().withdraw(5000.0);
                        try {
                            CivGlobal.setTeleportCooldown("teleportCommand", 5, CivGlobal.getPlayer(this.resident));
                        }
                        catch (CivException ex) {}
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
}
