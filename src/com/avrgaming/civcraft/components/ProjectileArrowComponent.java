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
package com.avrgaming.civcraft.components;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;

public class ProjectileArrowComponent extends ProjectileComponent {

	public ProjectileArrowComponent(Construct construct) {
		super(construct);
	}

	private double power;

	@Override
	public void loadSettings() {
		try {
			setDamage(CivSettings.getInteger(CivSettings.warConfig, "arrow_tower.damage"));
			power = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.power");
			range = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.range");
			if (this.getTown().getBuffManager().hasBuff("buff_great_lighthouse_tower_range") && (this.getConstruct()).getConfigId().equals("s_arrowtower")) {
				range *= this.getTown().getBuffManager().getEffectiveDouble("buff_great_lighthouse_tower_range");
			} else
				if (this.getTown().getBuffManager().hasBuff("buff_ingermanland_water_range") && ((this.getConstruct()).getConfigId().equals("w_grand_ship_ingermanland") || (this.getConstruct()).getConfigId().equals("s_arrowship"))) {
					range *= this.getTown().getBuffManager().getEffectiveDouble("buff_ingermanland_water_range");
				}
			min_range = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.min_range");

			this.proximityComponent.setCenter(new BlockCoord(getConstruct().getCenterLocation()));
			this.proximityComponent.setRadius(range);
			this.proximityComponent.createComponent(getConstruct());
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	@Override
	public void fire(Location turretLoc, Entity targetEntity) {
		//if (!(this.getConstruct()).isValid()) return;

		Location playerLoc = targetEntity.getLocation();
		playerLoc.setY(playerLoc.getY() + 1); // Target the head instead of feet.

		turretLoc = adjustTurretLocation(turretLoc, playerLoc);
		Vector dir = getVectorBetween(playerLoc, turretLoc).normalize();
		Arrow arrow = getConstruct().getCorner().getLocation().getWorld().spawnArrow(turretLoc, dir, (float) power, 0.0f);
		arrow.setVelocity(dir.multiply(power));

		if (getConstruct().getTownOwner().getBuffManager().hasBuff(Buff.FIRE_BOMB)) {
			arrow.setFireTicks(1000);
		}

		CivCache.arrowsFired.put(arrow.getUniqueId(), new ArrowFiredCache(this, targetEntity, arrow));
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public Town getTown() {
		return getConstruct().getTownOwner();
	}

}
