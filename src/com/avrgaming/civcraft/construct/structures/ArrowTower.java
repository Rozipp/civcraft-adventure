package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

import java.util.HashSet;
import java.util.Set;

public class ArrowTower extends Structure {

	ProjectileArrowComponent arrowComponent;
	Set<BlockCoord> turretLocation = new HashSet<>();

	public ArrowTower(String id, Town town) {
		super(id, town);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	/** @return the damage */
	public int getDamage() {
		double rate = 1;
		rate += this.getTownOwner().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
		return (int) (arrowComponent.getDamage() * rate);
	}

	@Override
	public int getMaxHitPoints() {
		double rate = 1.0;
		if (this.getTownOwner().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) rate += this.getTownOwner().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
		if (this.getTownOwner().getBuffManager().hasBuff("buff_barricade")) rate += this.getTownOwner().getBuffManager().getEffectiveDouble("buff_barricade");
		return (int) ((double) this.getInfo().max_hitpoints * rate);
	}

	/** @param power the power to set */
	public void setPower(double power) {
		arrowComponent.setPower(power);
	}

	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		if ("/towerfire".equals(sb.command)) {
			turretLocation.add(absCoord);
		}
	}
	
	@Override
	public void onPostBuild() {
		arrowComponent = new ProjectileArrowComponent(this);
		arrowComponent.createComponent(this);
		arrowComponent.setTurretLocation(turretLocation);
	}
}
