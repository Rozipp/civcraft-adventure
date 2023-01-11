package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.object.Town;

public class GreatLighthouse extends Wonder {

	public GreatLighthouse(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void addBuffs() {
		addBuffToTown(this.getTownOwner(), "buff_great_lighthouse_tower_range");
		addBuffToCiv(this.getCivOwner(), "buff_great_lighthouse_trade_ship_income");
	}
	
	@Override
	protected void removeBuffs() {
		removeBuffFromTown(this.getTownOwner(), "buff_great_lighthouse_tower_range");
		removeBuffFromCiv(this.getCivOwner(), "buff_great_lighthouse_trade_ship_income");
	}
	
}
