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
package com.avrgaming.civcraft.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.avrgaming.civcraft.main.CivCraft;

import lombok.Getter;

@Getter
public class BlockCoord {

	private int x;
	private int y;
	private int z;

	private World world = null;
	private Location location = null;

	public BlockCoord(World world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockCoord(String worldname, int x, int y, int z) {
		this.setWorldname(worldname);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockCoord(Location location) {
		this.setFromLocation(location);
	}

	public BlockCoord(String string) {
		String[] split = string.split(",");
		this.setWorldname(split[0]);
		this.x = Integer.valueOf(split[1]);
		this.y = Integer.valueOf(split[2]);
		this.z = Integer.valueOf(split[3]);
	}

	public BlockCoord(BlockCoord obj) {
		this.x = obj.getX();
		this.y = obj.getY();
		this.z = obj.getZ();
		this.world = obj.getWorld();
	}

	public BlockCoord(Block block) {
		this.x = block.getX();
		this.y = block.getY();
		this.z = block.getZ();
		this.world = block.getWorld();
	}

	public BlockCoord(SimpleBlock sb) {
		this.setWorldname(sb.worldname);
		this.x = sb.x;
		this.y = sb.y;
		this.z = sb.z;
	}

	public BlockCoord(ChunkCoord chunkCoord) {
		this.world = chunkCoord.getWorld();
		this.x = chunkCoord.getX() << 4;
		this.y = 64;
		this.z = chunkCoord.getZ() << 4;
	}

	public BlockCoord(ChunkCoord chunkCoord, int dx, int dy, int dz) {
		this.world = chunkCoord.getWorld();
		this.x = chunkCoord.getX() << 4 + dx;
		this.y = dy;
		this.z = chunkCoord.getZ() << 4 + dz;
	}

	public void setFromLocation(Location location) {
		this.world = location.getWorld();
		this.x = location.getBlockX();
		this.y = location.getBlockY();
		this.z = location.getBlockZ();
		this.location = location.clone();
	}

	public boolean inMainWorld() {
		return this.world.equals(CivCraft.mainWorld);
	}

	private void setWorldname(String worldname) {
		this.world = Bukkit.getWorld(worldname);
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	@Override
	public String toString() {
		return this.world.getName() + "," + this.x + "," + this.y + "," + this.z;
	}

	public String toStringNotWorld() {
		return this.x + "," + this.y + "," + this.z;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BlockCoord) {
			BlockCoord otherCoord = (BlockCoord) other;
			return otherCoord.getWorld().equals(this.getWorld()) && otherCoord.getX() == x && otherCoord.getY() == y && otherCoord.getZ() == z;
		}
		return false;
	}

	public Location getLocation() {
		if (location == null) location = new Location(this.getWorld(), x, y, z);
		return location;
	}

	public Block getBlock() {
		return getWorld().getBlockAt(this.x, this.y, this.z);
	}

	public Block getBlockRelative(int dx, int dy, int dz) {
		return getWorld().getBlockAt(this.x + dx, this.y + dy, this.z + dz);
	}

	public BlockCoord getRelative(int dx, int dy, int dz) {
		return new BlockCoord(getWorld(), getX() + dx, getY() + dy, getZ() + dz);
	}

	public ChunkCoord getChunkCoord() {
		return new ChunkCoord(this);
	}

	public double distance(BlockCoord corner) {
		return Math.sqrt(distanceSquared(corner));
	}

	public double distanceXZ(BlockCoord corner) {
		return Math.sqrt(distanceXZSquared(corner));
	}

	public double distanceXZSquared(BlockCoord corner) {
		if (!corner.getWorld().equals(this.getWorld())) return Double.MAX_VALUE;
		return Math.pow(corner.getX() - this.getX(), 2) + Math.pow(corner.getZ() - this.getZ(), 2);
	}

	public double distanceSquared(BlockCoord corner) {
		if (!corner.getWorld().equals(this.getWorld())) return Double.MAX_VALUE;
		return Math.pow(corner.getX() - this.getX(), 2) + Math.pow(corner.getY() - this.getY(), 2) + Math.pow(corner.getZ() - this.getZ(), 2);
	}

	public Location getCenteredLocation() {
		/* Get a specialized location that is exactly centered in a single block. This prevents the respawn algorithm from detecting the location as
		 * "in a wall" and searching upwards for a spawn point. */
		Location loc = new Location(getWorld(), (x + 0.5), (y + 0.5), (z + 0.5));
		return loc;
	}

}
