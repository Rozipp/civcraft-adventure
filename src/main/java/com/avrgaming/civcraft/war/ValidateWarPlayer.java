package com.avrgaming.civcraft.war;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerKickBan;

public class ValidateWarPlayer implements Runnable {
	@Override
	public void run() {
		for (final Player joinedPlayer : Bukkit.getOnlinePlayers()) {
			if (joinedPlayer.isOp()) continue;
			if (!PermissionGroup.hasGroup("Helper", joinedPlayer.getName())) continue;

			final Resident resident = CivGlobal.getResident(joinedPlayer);
			if (resident == null) {
				TaskMaster.syncTask(new PlayerKickBan(joinedPlayer.getName(), true, false, "§cВы были кикнуты, потому что вы не вы не резидент?!"));
				continue;
			}
			if (resident.getCiv() == null) {
				TaskMaster.syncTask(new PlayerKickBan(joinedPlayer.getName(), true, false, "§cВы были кикнуты, ибо сейчас идет война, и у вас нет цивилизации!"));
				continue;
			}
			if (!resident.getCiv().getDiplomacyManager().isAtWar()) {
				TaskMaster.syncTask(new PlayerKickBan(joinedPlayer.getName(), true, false, "§cВы были кикнуты, ибо сейчас идет война и у вашей цивилизации нет войн!"));
				continue;
			}
		}
	}
}
