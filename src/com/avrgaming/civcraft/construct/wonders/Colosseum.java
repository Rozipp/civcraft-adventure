package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.object.Town;

public class Colosseum extends Wonder {

	public Colosseum(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCivOwner(), "buff_colosseum_happiness_to_towns");
		removeBuffFromTown(this.getTownOwner(), "buff_colosseum_happiness_for_town");
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCivOwner(), "buff_colosseum_happiness_to_towns");
		addBuffToTown(this.getTownOwner(), "buff_colosseum_happiness_for_town");
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
