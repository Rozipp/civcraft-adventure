package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.object.Town;

public class School extends Structure {

	
	public School(String id, Town town) {
		super(id, town);
	}

	@Override
	public String getMarkerIconName() {
		return "walk";
	}

}
