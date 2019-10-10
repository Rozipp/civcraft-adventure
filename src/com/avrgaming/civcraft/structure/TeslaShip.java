package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import com.avrgaming.civcraft.components.ProjectileLightningComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TeslaTower;
import com.avrgaming.civcraft.structure.WaterStructure;
import com.avrgaming.civcraft.util.BlockCoord;

public class TeslaShip extends WaterStructure {
	ProjectileLightningComponent teslaComponent;

	public TeslaShip(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		this.setHitpoints(this.getMaxHitPoints());
	}

	public TeslaShip(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	@Override
	public void loadSettings() {
		super.loadSettings();
		teslaComponent = new ProjectileLightningComponent(this, this.getCenterLocation());
		teslaComponent.createComponent(this);
	}

	public int getDamage() {
		double rate = 1;
//		rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
		return (int) (teslaComponent.getDamage() * rate);
	}

	@Override
	public int getMaxHitPoints() {
		double rate = 1.0;
		if (this.getTown().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) {
			rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
		}
		if (this.getTown().getBuffManager().hasBuff("buff_barricade")) {
			rate += this.getTown().getBuffManager().getEffectiveDouble("buff_barricade");
		}
		if (this.getCiv().getCapitol() != null && this.getCiv().getCapitol().getBuffManager().hasBuff("level5_extraTowerHPTown")) {
			rate *= this.getCiv().getCapitol().getBuffManager().getEffectiveDouble("level5_extraTowerHPTown");
		}
		return (int) ((double) this.getInfo().max_hitpoints * rate);
	}

//	public void setDamage(int damage) {
//		cannonComponent.setDamage(damage);
//	}

	public void setTurretLocation(BlockCoord absCoord) {
		teslaComponent.setTurretLocation(absCoord);
	}

//	@Override
//	public void fire(Location turretLoc, Location playerLoc) {
//		turretLoc = adjustTurretLocation(turretLoc, playerLoc);
//		Vector dir = getVectorBetween(playerLoc, turretLoc);
//		
//		Fireball fb = turretLoc.getWorld().spawn(turretLoc, Fireball.class);
//		fb.setDirection(dir);
//		// NOTE cannon does not like it when the dir is normalized or when velocity is set.
//		fb.setYield((float)yield);
//		CivCache.cannonBallsFired.put(fb.getUniqueId(), new CannonFiredCache(this, playerLoc, fb));
//	}

	@Override
	public void onCheck() throws CivException {
		try {
			double build_distanceSqr = CivSettings.getDouble(CivSettings.warConfig, "tesla_tower.build_distance");
			for (Town town : this.getTown().getCiv().getTowns()) {
				for (Structure struct : town.getStructures()) {
					if (struct instanceof TeslaTower) {
						Location center = struct.getCenterLocation();
						double distanceSqr = center.distanceSquared(this.getCenterLocation());
						if (distanceSqr <= build_distanceSqr)
							throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToTeslaTower",
									"" + center.getX() + "," + center.getY() + "," + center.getZ()));
					}
//					if (struct instanceof TeslaShip) {
//						Location center = struct.getCenterLocation();
//						double distanceSqr = center.distanceSquared(this.getCenterLocation());
//						if (distanceSqr <= build_distanceSqr)
//							throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToTeslaShip",
//									"" + center.getX() + "," + center.getY() + "," + center.getZ()));
//					}
				}
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			throw new CivException(e.getMessage());
		}

	}

}
