/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.ChunkCoord;

public class WaterStructure extends Structure {

	public static int WATER_LEVEL = 62;
	public static int TOLERANCE = 20;

	public WaterStructure(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public WaterStructure(String id, Town town) throws CivException {
		super(id, town);
	}

	@Override
	public Location repositionCenter(Location center, Template tpl) {
		Location loc = BuildableStatic.repositionCenterStatic(center, this.getTemplateYShift(), tpl);
		loc.setY(WATER_LEVEL + this.getTemplateYShift());
		return loc;
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		for (ChunkCoord chunkCoord : this.getChunksCoords()) {
			Biome biome = chunkCoord.getChunk().getBlock(7, 64, 7).getBiome();
			switch (biome) {
			case OCEAN:
			case BEACHES:
			case STONE_BEACH:
			case COLD_BEACH:
			case DEEP_OCEAN:
			case RIVER:
			case FROZEN_OCEAN:
			case FROZEN_RIVER:
				break;
			default:
				throw new CivException(CivSettings.localize.localizedString("var_buildable_notEnoughWater", this.getDisplayName()));
			}
		}

		if (Math.abs(this.getCorner().getY() - WATER_LEVEL) > TOLERANCE) throw new CivException(CivSettings.localize.localizedString("buildable_Water_notValidWaterSpot"));
		super.checkBlockPermissionsAndRestrictions(player);
	}

	@Override
	public String getMarkerIconName() {
		return "anchor";
	}
}
