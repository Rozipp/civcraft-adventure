package com.avrgaming.civcraft.structure.farm;

import com.avrgaming.civcraft.util.BlockCoord;

public class GrowBlock {
	
	public GrowBlock(String world, int x, int y, int z, int typeid, int data, boolean spawn) {
		this.bcoord = new BlockCoord(world, x, y, z);
		this.typeId = typeid;
		this.data = data;
		this.spawn = spawn;
	}
	
	public GrowBlock(BlockCoord bcoord, int typeid, int data, boolean spawn) {
		this.bcoord = bcoord;
		this.typeId = typeid;
		this.data = data;
		this.spawn = spawn;
	}
	
	public BlockCoord bcoord;
	public int typeId;
	public int data;
	public boolean spawn;
}
