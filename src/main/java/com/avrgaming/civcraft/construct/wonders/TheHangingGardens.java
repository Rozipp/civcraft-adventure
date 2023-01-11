package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import org.bukkit.entity.Player;

public class TheHangingGardens extends Wonder {

	public TheHangingGardens(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCivOwner(), "buff_hanging_gardens_growth");
		addBuffToCiv(this.getCivOwner(), "buff_hanging_gardens_additional_growth");
		addBuffToTown(this.getTownOwner(), "buff_hanging_gardens_regen");
	}

	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCivOwner(), "buff_hanging_gardens_growth");
		removeBuffFromCiv(this.getCivOwner(), "buff_hanging_gardens_additional_growth");
		removeBuffFromTown(this.getTownOwner(), "buff_hanging_gardens_regen");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCivtickUpdate(CivAsyncTask task) {
		for (Town t : this.getTownOwner().getCiv().getTowns()) {
			for (Resident res : t.getResidents()) {
				try {
					Player player = CivGlobal.getPlayer(res);
					if (player.isDead() || !player.isValid()) continue;
					if (player.getHealth() >= 20) continue;

					TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
					if (tc == null || tc.getTown() != this.getTownOwner()) continue;

					if (player.getHealth() > player.getMaxHealth())
						player.setHealth(player.getMaxHealth());
					else
						player.setHealth(player.getHealth() + 1);
				} catch (CivException e) {
					// Player not online;
				}
			}
		}
	}
}
