package com.avrgaming.civcraft.populators;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigCave;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.construct.Cave;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.BlockCoord;

public class CavePopulator extends BlockPopulator {

	public static String getHumanBuffList(final ConfigTradeGood tradeGood) {
		final StringBuilder buffs = new StringBuilder();
		for (final ConfigBuff configBuff : tradeGood.buffs.values()) {
			buffs.append(configBuff.name).append(", ");
		}
		return buffs.toString();
	}

	public boolean checkForDuplicateCave(String worldName, int centerX, int centerY, int centerZ) {
		/*
		 * Search downward to bedrock for any trade goodies here. If we find one, don't
		 * generate.
		 */

		BlockCoord coord = new BlockCoord(worldName, centerX, centerY, centerZ);
		for (int y = centerY; y > 0; y--) {
			coord.setY(y);

			if (CivGlobal.getTradeGood(coord) != null) {
				/* Already a trade goodie here. DONT Generate it. */
				return true;
			}
		}
		return false;
	}

	@Override
	public void populate(World world, Random random, Chunk source) {

//		ChunkCoord cCoord = new ChunkCoord(source);
//		TradeGoodPick pick = CivGlobal.tradeGoodPreGenerator.goodPicks.get(cCoord);
//		if (pick != null) {
			int centerX = (source.getX() << 4) + 8;
			int centerZ = (source.getZ() << 4) + 8;
			int centerY = world.getHighestBlockYAt(centerX, centerZ);
			BlockCoord coord = new BlockCoord(world.getName(), centerX, centerY, centerZ);

			if (checkForDuplicateCave(world.getName(), centerX, centerY, centerZ))
				return;

			ConfigCave caveConf = CivSettings.caves.get("f_cave1");
			if (caveConf == null) {
				System.out.println("Could not find suitable good type during populate! aborting.");
				return;
			}

			Cave cave;
			try {
				cave = new Cave(caveConf.id, coord);
				cave.build(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
//		}
	}
}
