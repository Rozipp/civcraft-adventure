
package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.object.Town;

public class BeMine extends Structure {
	public BeMine(String id, Town town) {
		super(id, town);
	}

	@Override
	public String getMarkerIconName() {
		return "offlineuser";
	}
}
