package com.avrgaming.civcraft.threading.sync;

import org.bukkit.scheduler.*;
import com.avrgaming.civcraft.object.*;

import org.bukkit.entity.*;
import org.bukkit.plugin.*;
import java.util.*;
import com.avrgaming.civcraft.config.*;
import com.avrgaming.civcraft.construct.Camp;

import org.bukkit.*;
import com.avrgaming.civcraft.main.*;

public class RefreshPlayerTask extends BukkitRunnable {
	public Resident resident;
	public Player player;
	public Camp camp;

	public RefreshPlayerTask(final Resident resident, final Player player, final Camp camp) {
		this.resident = resident;
		this.player = player;
		this.camp = camp;
	}

	public void run(final boolean async) {
		if (async)
			this.runTaskAsynchronously((Plugin) CivCraft.getPlugin());
		else
			this.runTask((Plugin) CivCraft.getPlugin());
	}

	public void run() {
		this.resident.isRefresh = true;
		CivLog.info("Телепортирование игрока " + this.resident.getName() + " в лагерь " + this.camp.getName());
		try {
			int i = 10;
			while (i >= 0) {
				final long timeNow = Calendar.getInstance().getTimeInMillis();
				if (!this.resident.isRefresh) {
					CivMessage.sendError(this.resident, CivSettings.localize.localizedString("cmd_camp_refresh_aborted"));
					return;
				}
				if (this.resident.getCamp() == null) {
					CivMessage.sendError(this.resident, CivSettings.localize.localizedString("cmd_camp_refresh_aborted4"));
					return;
				}
				if (i != 0) {
					CivMessage.sendSuccess(this.resident, CivSettings.localize.localizedString("cmd_camp_refresh_waiting", "§c" + i + "§a", "§6" + CivMessage.plurals(i, "секунду", "секунды", "секунд")));
				} else {
					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) CivCraft.getPlugin(), () -> {
						final long refresh;
						refresh = timeNow + 1800000L;
						CivMessage.sendSuccess(this.resident, CivSettings.localize.localizedString("cmd_camp_refresh_succuses", CivGlobal.dateFormat.format(refresh)));
						this.resident.setNextRefresh(refresh);
						this.resident.save();
						return;
					});
				}
				--i;
				Thread.sleep(1000L);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) CivCraft.getPlugin(), () -> {
			this.resident.isRefresh = false;
		});
	}
}
