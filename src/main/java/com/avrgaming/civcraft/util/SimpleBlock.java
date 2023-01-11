package com.avrgaming.civcraft.util;

import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.main.CivLog;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class SimpleBlock {

	// public static final int SIGN = 1;
	// public static final int CHEST = 2;
	// public static final int SIGN_LITERAL = 3;

	public enum SimpleType {
		NORMAL, COMMAND, LITERAL, COMMANDDBG
	}

	private int type;
	private byte data;
	public int x;
	public int y;
	public int z;

	public SimpleType specialType;
	public String command;
	public String[] message = new String[4];
	public String worldname;
	public Buildable buildable;
	public Map<String, String> keyvalues = new HashMap<>();

	public SimpleBlock(Block block) {
		this.x = block.getX();
		this.y = block.getY();
		this.z = block.getZ();
		this.worldname = block.getWorld().getName();
		this.type = ItemManager.getTypeId(block);
		this.data = ItemManager.getData(block);
		this.specialType = SimpleType.NORMAL;
	}

	public SimpleBlock(BlockCoord bc, SimpleBlock sb) {
		if (sb == null) {
			this.x = bc.getX();
			this.y = bc.getY();
			this.z = bc.getZ();
			this.worldname = bc.getWorld().getName();
			this.type = 0;
			this.data = 0;
			this.specialType = SimpleType.NORMAL;
			return;
		}
		this.x = bc.getX() + sb.x;
		this.y = bc.getY() + sb.y;
		this.z = bc.getZ() + sb.z;
		this.worldname = bc.getWorld().getName();
		this.type = sb.type;
		this.data = sb.data;
		this.specialType = sb.specialType;
		this.buildable = sb.buildable;
		this.command = sb.command;
		this.keyvalues = sb.keyvalues;
		this.message = sb.message;
	}

	public SimpleBlock(String hash, int type, byte data) {
		String[] split = hash.split(",");
		this.worldname = split[0];
		this.x = Integer.parseInt(split[1]);
		this.y = Integer.parseInt(split[2]);
		this.z = Integer.parseInt(split[3]);
		this.type = type;
		this.data = data;
		this.specialType = SimpleType.NORMAL;
	}

	public SimpleBlock(int type, int data) {
		this.type = (short) type;
		this.data = (byte) data;
		this.specialType = SimpleType.NORMAL;
	}

	@SuppressWarnings("deprecation")
	public SimpleBlock(String umat) {
		this.specialType = SimpleType.NORMAL;
		try{
			if (umat.contains(":")) {
				String[] spl = umat.split(":");
				this.type = Integer.parseInt(spl[0]);
				this.data = Byte.parseByte(spl[1]);
			} else {
				Material mat = Material.valueOf(umat.toUpperCase());
				this.type = mat.getId();
				this.data = 0;
			}
		} catch (Exception e){
			CivLog.error("Create SimpleBlock error. umat = " + umat);
			e.printStackTrace();
			this.type = 0;
			this.data = 0;
		}
	}

	public SimpleBlock(String worldname, int x, int y, int z, int type, int data) {
		this.worldname = worldname;
		this.x = x;
		this.y = y;
		this.z = z;
		this.type = type;
		this.data = (byte) data;
		this.specialType = SimpleType.NORMAL;
	}

	public String getKey() {
		return this.worldname + "," + this.x + "," + this.y + "," + this.z;
	}

	public static String getKeyFromBlockCoord(BlockCoord coord) {
		return coord.getWorld().getName() + "," + coord.getX() + "," + coord.getY() + "," + coord.getZ();
	}

	/** @return the type */
	public int getType() {
		return type;
	}

	@SuppressWarnings("deprecation")
	public Material getMaterial() {
		return Material.getMaterial(type);
	}

	/** @param type the type to set */
	public void setType(int type) {
		this.type = (short) type;
	}

	public void setTypeAndData(int type, int data) {
		this.type = (short) type;
		this.data = (byte) data;
	}

	/** @return the data */
	public byte getData() {
		return data;
	}

	/** @param data the data to set */
	public void setData(int data) {
		this.data = (byte) data;
	}

	public void setBlockCoord(BlockCoord bc) {
		this.worldname = bc.getWorld().getName();
		this.x = bc.getX();
		this.y = bc.getY();
		this.z = bc.getZ();
	}

	/** Returns true if it's air.
	 * @return if air */
	public boolean isAir() {
		return type == (byte) 0x0;
	}

	public String getKeyValueString() {
		String out = "";

		for (String key : keyvalues.keySet()) {
			String value = keyvalues.get(key);
			out += key + ":" + value + ",";
		}

		return out;
	}

	public Block getBlock() {
		return Bukkit.getWorld(this.worldname).getBlockAt(this.x, this.y, this.z);
	}

	public Location getLocation() {
		return new Location(Bukkit.getWorld(this.worldname), this.x, this.y, this.z);
	}

	public SimpleBlock clone() {
		SimpleBlock sb = new SimpleBlock(worldname, x, y, z, type, data);
		sb.buildable = buildable;
		sb.command = command;
		sb.keyvalues = keyvalues;
		sb.message = message;
		sb.specialType = specialType;
		return sb;
	}
}
