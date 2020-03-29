/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.template.Template;

public class WaterStructure extends Structure {

	public static int WATER_LEVEL = 62;
	public static int TOLERANCE = 20;

	public WaterStructure(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public WaterStructure(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	@Override
	public Location repositionCenter(Location center, Template tpl) {
		Location loc = center.clone();
		String dir = tpl.getDirection();
		double x_size = tpl.getSize_x();
		double z_size = tpl.getSize_z();
		// Reposition tile improvements
		if (this.isTileImprovement()) {
			// just put the center at 0,0 of this chunk?
			loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
			//loc = center.getChunk().getBlock(arg0, arg1, arg2)
		} else {
			if (dir.equalsIgnoreCase("east")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc.setX(loc.getX());
			} else
				if (dir.equalsIgnoreCase("west")) {
					loc.setZ(loc.getZ() - (z_size / 2));
					loc.setX(loc.getX() - (x_size));

				} else
					if (dir.equalsIgnoreCase("north")) {
						loc.setX(loc.getX() - (x_size / 2));
						loc.setZ(loc.getZ() - (z_size));
					} else
						if (dir.equalsIgnoreCase("south")) {
							loc.setX(loc.getX() - (x_size / 2));
							loc.setZ(loc.getZ());

						}
		}

		if (this.getTemplateYShift() != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(WATER_LEVEL + this.getTemplateYShift());
		}

		return loc;
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		super.checkBlockPermissionsAndRestrictions(player);

		if (Math.abs(this.getCorner().getY() - WATER_LEVEL) > TOLERANCE) {
			throw new CivException(CivSettings.localize.localizedString("buildable_Water_notValidWaterSpot"));
		}

	}

	@Override
	public String getMarkerIconName() {
		return "anchor";
	}
}
