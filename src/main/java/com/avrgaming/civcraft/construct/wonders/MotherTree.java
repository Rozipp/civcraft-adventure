package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.object.Town;

public class MotherTree extends Wonder {

	public MotherTree(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCivOwner(), "buff_mother_tree_growth");
		addBuffToCiv(this.getCivOwner(), "buff_mother_tree_tile_improvement_cost");
		addBuffToTown(this.getTownOwner(), "buff_mother_tree_tile_improvement_bonus");
	}
	
	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCivOwner(), "buff_mother_tree_growth");
		removeBuffFromCiv(this.getCivOwner(), "buff_mother_tree_tile_improvement_cost");
		removeBuffFromTown(this.getTownOwner(), "buff_mother_tree_tile_improvement_bonus");
	}
	
}
