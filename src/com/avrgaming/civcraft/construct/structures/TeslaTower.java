package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.components.ProjectileLightningComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class TeslaTower extends Structure {

	ProjectileLightningComponent teslaComponent;
	Set<BlockCoord> turretLocation = new HashSet<>();

	public TeslaTower(String id, Town town) {
		super(id, town);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	public int getDamage() {
		double rate = 1;
		return (int) (teslaComponent.getDamage() * rate);
	}

	@Override
	public int getMaxHitPoints() {
		double rate = 1.0;
		if (this.getTownOwner().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) rate += this.getTownOwner().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
		if (this.getTownOwner().getBuffManager().hasBuff("buff_barricade")) rate += this.getTownOwner().getBuffManager().getEffectiveDouble("buff_barricade");
		return (int) ((double) this.getInfo().max_hitpoints * rate);
	}

	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		if (sb.command.equals("/towerfire")) {
			turretLocation.add(absCoord);
		}
	}
	
	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		super.checkBlockPermissionsAndRestrictions(player);
		try {
			double build_distanceSqr = CivSettings.getDouble(CivSettings.warConfig, "tesla_tower.build_distance");
			for (Town town : this.getTownOwner().getCiv().getTowns()) {
				for (Structure struct : town.BM.getStructures()) {
					if (struct instanceof TeslaTower) {
						Location center = struct.getCenterLocation();
						double distanceSqr = center.distanceSquared(this.getCenterLocation());
						if (distanceSqr <= build_distanceSqr) throw new CivException(CivSettings.localize.localizedString("var_buildable_tooCloseToTeslaTower", "" + center.getX() + "," + center.getY() + "," + center.getZ()));
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
		teslaComponent = new ProjectileLightningComponent(this);
		teslaComponent.createComponent(this);
		teslaComponent.setTurretLocation(turretLocation);
	}
}
