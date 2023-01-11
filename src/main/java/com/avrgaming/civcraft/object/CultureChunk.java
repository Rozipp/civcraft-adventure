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
package com.avrgaming.civcraft.object;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBiomeInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.BiomeCache;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

@Getter
@Setter
public class CultureChunk {

	private Town town;
	private ChunkCoord chunkCoord;
	private int distance = 0;
	private Biome biome = null;

	public CultureChunk(Town town, ChunkCoord coord) {
		this.town = town;
		this.chunkCoord = coord;
		biome = BiomeCache.getBiome(this);
	}

	public int getDistanceToNearestEdge(ArrayList<TownChunk> edges) {
		int distance = Integer.MAX_VALUE;

		for (TownChunk tc : edges) {
			int tmp = tc.getChunkCoord().manhattanDistance(this.chunkCoord);
			if (tmp < distance) {
				distance = tmp;
			}
		}

		return distance;
	}

	public String getOnLeaveString() {
		return CivColor.LightPurple + CivSettings.localize.localizedString("var_cultureLeaveMsg", town.getCiv().getName());
	}

	public String getOnEnterString() {
		return CivColor.LightPurple + CivSettings.localize.localizedString("var_cultureEnterMsg", town.getCiv().getName());
	}

	public double getPower() {
		// power = max/(distance^2).
		// if distance == 0, power = DOUBLEMAX;

		if (this.distance == 0) {
			return Double.MAX_VALUE;
		}

		ConfigCultureLevel clc = CivSettings.cultureLevels.get(getTown().SM.getLevel());
		double power = clc.amount / (Math.pow(distance, 2));

		return power;
	}

	public Biome getBiome() {
		return biome;
	}

	public void setBiome(Biome biome) {
		this.biome = biome;
	}

	@Override
	public String toString() {
		return this.chunkCoord.toString();
	}

	public ConfigBiomeInfo getCultureBiomeInfo() {
		if (this.biome != null) {
			ConfigBiomeInfo info = CivSettings.getCultureBiome(this.biome);
			return info;
		} else {
			// This can happen within 1 tick of the chunk being created, that's OK. 
			return CivSettings.getCultureBiome(Biome.VOID);
		}
	}

	public double getHappiness() {
		return (getCultureBiomeInfo().beauty ? 1 : 0);// + getAdditionalAttributes(AttributeTypeKeys.HAPPINESS.name());
	}

	public double getHammers() {
		return getCultureBiomeInfo().getHammers();// + getAdditionalAttributes(AttributeTypeKeys.HAMMERS.name());
	}

	public double getGrowth() {
		return getCultureBiomeInfo().getGrowth();// + getAdditionalAttributes(AttributeTypeKeys.GROWTH.name());
	}

//	private double getAdditionalAttributes(String attrType) {
//		if (getBiome() == null) return 0.0;
//
//		Component.componentsLock.lock();
//		try {
//			ArrayList<Component> attrs = Component.componentsByType.get("AttributeBiomeBase");
//			double total = 0;
//
//			if (attrs == null) {
//				return total;
//			}
//
//			for (Component comp : attrs) {
//				if (comp instanceof AttributeBiomeRadiusPerLevel) {}
//
//				if (comp instanceof AttributeBiomeBase) {
//					AttributeBiomeBase attrComp = (AttributeBiomeBase) comp;
//					if (attrComp.getAttribute().equals(attrType)) {
//						total += attrComp.getGenerated(this);
//					}
//				}
//			}
//			return total;
//		} finally {
//			Component.componentsLock.unlock();
//		}
//	}

	public static void showInfo(Player player) {
		Biome biome = getBiomeFromLocation(player.getLocation());

		ConfigBiomeInfo info = CivSettings.getCultureBiome(biome);
		// CivLog.debug("showing info.");

		CivMessage.send(player, CivColor.LightPurple + biome.name() + CivColor.Green + " " + CivSettings.localize.localizedString("Happiness") + " " + CivColor.LightGreen + info.beauty + CivColor.Green + " "
				+ CivSettings.localize.localizedString("Hammers") + " " + CivColor.LightGreen + info.getHammers() + CivColor.Green + " " + CivSettings.localize.localizedString("Growth") + " " + CivColor.LightGreen + info.getGrowth());
	}

	public static Biome getBiomeFromLocation(Location loc) {
		Block block = loc.getChunk().getBlock(0, 0, 0);
		return block.getBiome();
	}

	public Civilization getCiv() {
		return town.getCiv();
	}

}