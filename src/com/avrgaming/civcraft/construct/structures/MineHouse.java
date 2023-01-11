package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.object.Town;

public class MineHouse extends Structure {

	public MineHouse(String id, Town town) {
		super(id, town);
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}
	
	@Override
	public String getMarkerIconName() {
		return "building";
	}
}
