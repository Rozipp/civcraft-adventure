package com.avrgaming.civcraft.construct.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Town;

public class ChichenItza extends Wonder {

	public ChichenItza(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCivOwner(), "buff_chichen_itza_tower_hp");
		removeBuffFromCiv(this.getCivOwner(), "buff_chichen_itza_regen_rate");
		removeBuffFromTown(this.getTownOwner(), "buff_chichen_itza_cp_bonus_hp");
		// This is where the Itza's buff to CP is removed
		for (ControlPoint cp : this.getTownOwner().getCityhall().getControlPoints().values()) {
			cp.setMaxHitpoints((cp.getMaxHitpoints() - (int) this.getTownOwner().getBuffManager().getEffectiveDouble("buff_chichen_itza_cp_bonus_hp")));
		}
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCivOwner(), "buff_chichen_itza_tower_hp");
		addBuffToCiv(this.getCivOwner(), "buff_chichen_itza_regen_rate");
		addBuffToTown(this.getTownOwner(), "buff_chichen_itza_cp_bonus_hp");
		// This is where the Itza's buff to CP applies
		for (ControlPoint cp : this.getTownOwner().getCityhall().getControlPoints().values()) {
			cp.setMaxHitpoints((cp.getMaxHitpoints() + (int) this.getTownOwner().getBuffManager().getEffectiveDouble("buff_chichen_itza_cp_bonus_hp")));
		}
	}

	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}

	@Override
	public void onComplete() {
		addBuffs();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}

}
