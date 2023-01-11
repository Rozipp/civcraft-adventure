package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.BuildableStatic;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public class WaterStructure extends Structure {

	public static int WATER_LEVEL = 62;
	public static int TOLERANCE = 20;

	public WaterStructure(String id, Town town) {
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
