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
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.components.ProjectileCannonComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

public class CannonTower extends Structure {

	ProjectileCannonComponent cannonComponent;
	Set<BlockCoord> turretLocation = new HashSet<>();

	public CannonTower(String id, Town town) throws CivException {
		super(id, town);
	}

	public CannonTower(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	public int getDamage() {
		double rate = 1;
		rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
		return (int) (cannonComponent.getDamage() * rate);
	}

	@Override
	public int getMaxHitPoints() {
		double rate = 1.0;
		if (this.getTown().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
		if (this.getTown().getBuffManager().hasBuff("buff_barricade")) rate += this.getTown().getBuffManager().getEffectiveDouble("buff_barricade");
		return (int) ((double) this.getInfo().max_hitpoints * rate);
	}

	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		switch (sb.command) {
		case "/towerfire":
			turretLocation.add(absCoord);
			break;
		}
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		super.checkBlockPermissionsAndRestrictions(player);
		try {
			double build_distanceSqr = Math.pow(CivSettings.getDouble(CivSettings.warConfig, "cannon_tower.build_distance"), 2);

			for (Town town : this.getTown().getCiv().getTowns()) {
				for (Structure struct : town.BM.getStructures()) {
					if (struct instanceof CannonTower) {
						Location center = struct.getCenterLocation();
						double distanceSqr = center.distanceSquared(this.getCenterLocation());
						if (distanceSqr <= build_distanceSqr) {
							throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToCannonTower", (center.getX() + "," + center.getY() + "," + center.getZ())));
						}
					}
				}
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			throw new CivException(e.getMessage());
		}
	}
	@Override
	public void onPostBuild() {
		cannonComponent = new ProjectileCannonComponent(this);
		cannonComponent.createComponent(this);
		cannonComponent.setTurretLocation(turretLocation);
	}
}
