package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.object.Town;

public class TheColossus extends Wonder {

	public TheColossus(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void removeBuffs() {
		this.removeBuffFromTown(this.getTownOwner(), "buff_colossus_reduce_upkeep");
		this.removeBuffFromTown(this.getTownOwner(), "buff_colossus_coins_from_culture");
	}

	@Override
	protected void addBuffs() {
		this.addBuffToTown(this.getTownOwner(), "buff_colossus_reduce_upkeep");
		this.addBuffToTown(this.getTownOwner(), "buff_colossus_coins_from_culture");
		
	}
	
}
