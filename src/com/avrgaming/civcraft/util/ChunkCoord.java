/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.util;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class ChunkCoord {

	private String worldname;
	private int x;
	private int z;

	private static ConcurrentHashMap<String, World> worlds = new ConcurrentHashMap<String, World>();

	public static void addWorld(World world) {
		worlds.put(world.getName(), world);
	}

	public static void buildWorldList() {
		for (World world : Bukkit.getWorlds()) {
			worlds.put(world.getName(), world);
		}
	}

	public ChunkCoord(String worldname, int x, int z) {
		this.worldname = worldname;
		this.x = x;
		this.z = z;
	}

	public ChunkCoord(Location location) {
		this.worldname = location.getWorld().getName();
		this.x = castToChunk(location.getBlockX());
		this.z = castToChunk(location.getBlockZ());
	}

	public ChunkCoord(Chunk c) {
		this.worldname = c.getWorld().getName();
		this.x = c.getX();
		this.z = c.getZ();
	}

	public ChunkCoord(BlockCoord coord) {
		this.worldname = coord.getWorldname();
		this.x = castToChunk(coord.getX());
		this.z = castToChunk(coord.getZ());
	}

	public ChunkCoord(Block block) {
		this.worldname = block.getWorld().getName();
		this.x = castToChunk(block.getX());
		this.z = castToChunk(block.getZ());
	}

	@Override
	public String toString() {
		return this.worldname + "," + x + "," + z;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ChunkCoord) {
			ChunkCoord otherCoord = (ChunkCoord) other;
			return otherCoord != null && //
					otherCoord.worldname.equals(worldname) && //
					otherCoord.getX() == x && //
					otherCoord.getZ() == z;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	public static int castToChunk(int i) {
		return (int) Math.floor((double) i / 16.0);
	}

	public int manhattanDistance(ChunkCoord chunkCoord) {
		return Math.abs(chunkCoord.x - this.x) + Math.abs(chunkCoord.z - this.z);
	}

	public double distanceSqr(ChunkCoord chunkCoord) {
		if (!chunkCoord.getWorldname().equals(this.getWorldname())) return Double.MAX_VALUE;
		double dist = Math.pow(this.getX() - chunkCoord.getX(), 2) + Math.pow(this.getZ() - chunkCoord.getZ(), 2);
		return dist;
	}

	public double distance(ChunkCoord chunkCoord) {
		if (!chunkCoord.getWorldname().equals(this.getWorldname())) return Double.MAX_VALUE;
		return Math.sqrt(distanceSqr(chunkCoord));
	}

	public Chunk getChunk() {
		return Bukkit.getWorld(this.worldname).getChunkAt(this.x, this.z);
	}

	public static int getBlockInChunk(int d) {
		return d % 16 + (d < 0 ? 16 : 0);
	}
	//
	// public int compareTo(ChunkCoord o) {
	// int i = worldname.hashCode() - o.hashCode();
	// if (i == 0) {
	// i = x - o.x;
	// if (i == 0) i = z - o.z;
	// }
	// return i;
	// }
}
