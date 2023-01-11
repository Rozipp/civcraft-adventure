
package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.object.Town;

public class SilkwormFarm extends Structure {

	public SilkwormFarm(String id, Town town) {
		super(id, town);
	}

	@Override
	public String getDynmapDescription() {
		return "";
	}

	@Override
	public String getMarkerIconName() {
		return "bighouse";
	}

}