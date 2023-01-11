package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.object.Town;

public class GlobeTheatre extends Wonder {

	public GlobeTheatre(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCivOwner(), "buff_globe_theatre_happiness_to_towns");
		removeBuffFromTown(this.getTownOwner(), "buff_globe_theatre_culture_from_towns");
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCivOwner(), "buff_globe_theatre_happiness_to_towns");
		addBuffToTown(this.getTownOwner(), "buff_globe_theatre_culture_from_towns");
	}
	
	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}
	
	@Override
	public void onComplete() {
		addBuffs();
	}

}
