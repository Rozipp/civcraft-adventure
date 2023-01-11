package com.avrgaming.civcraft.construct.farm;

import com.avrgaming.civcraft.util.BlockCoord;

public class GrowBlock {
	
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
