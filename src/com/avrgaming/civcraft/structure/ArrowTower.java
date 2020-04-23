/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;

public class ArrowTower extends Structure {

	ProjectileArrowComponent arrowComponent;

	public ArrowTower(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		this.setHitpoints(this.getMaxHitPoints());
	}

	public ArrowTower(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	@Override
	public void loadSettings() {
		super.loadSettings();
		arrowComponent = new ProjectileArrowComponent(this);
		arrowComponent.createComponent(this);
	}

	/**
	 * @return the damage
	 */
	public int getDamage() {
		double rate = 1;
		rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
		return (int) (arrowComponent.getDamage() * rate);
	}

	@Override
	public int getMaxHitPoints() {
		double rate = 1.0;
		if (this.getTown().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) { rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp"); }
		if (this.getTown().getBuffManager().hasBuff("buff_barricade")) { rate += this.getTown().getBuffManager().getEffectiveDouble("buff_barricade"); }
		if (this.getCiv().getCapitol() != null && this.getCiv().getCapitol().getBuffManager().hasBuff("level5_extraTowerHPTown")) { rate *= this.getCiv().getCapitol().getBuffManager().getEffectiveDouble("level5_extraTowerHPTown"); }
		return (int) ((double) this.getInfo().max_hitpoints * rate);
	}

	/**
	 * @param power the power to set
	 */
	public void setPower(double power) {
		arrowComponent.setPower(power);
	}

	@Override
	public void setTurretLocation(BlockCoord absCoord) {
		arrowComponent.setTurretLocation(absCoord);
	}

}
