package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;

import java.util.TreeMap;

public class TheGreatPyramid extends Wonder {

	public TheGreatPyramid(String id, Town town) {
		super(id, town);
	}

	private Civilization calculateNearestCivilization() {
		TreeMap<Double, Civilization> civMaps = CivGlobal.findNearestCivilizations(this.getTownOwner());
		Civilization nearestCiv = null;
		if (civMaps.size() > 0) {
			nearestCiv = civMaps.firstEntry().getValue();
		}
		return nearestCiv;
	}
	
	@Override
	protected void addBuffs() {
		addBuffToTown(this.getTownOwner(), "buff_pyramid_cottage_consume");
		addBuffToTown(this.getTownOwner(), "buff_pyramid_cottage_bonus");
		addBuffToCiv(this.getCivOwner(), "buff_pyramid_culture");
		addBuffToTown(this.getTownOwner(), "buff_pyramid_leech");
		Civilization nearest = calculateNearestCivilization();
		if (nearest != null) {
			addBuffToCiv(nearest, "debuff_pyramid_leech");
		}
	}
	
	@Override
	protected void removeBuffs() {
		removeBuffFromTown(this.getTownOwner(), "buff_pyramid_cottage_consume");
		removeBuffFromTown(this.getTownOwner(), "buff_pyramid_cottage_bonus");
		removeBuffFromCiv(this.getCivOwner(), "buff_pyramid_culture");
		removeBuffFromTown(this.getTownOwner(), "buff_pyramid_leech");
		Civilization nearest = calculateNearestCivilization();
		if (nearest != null) {
			removeBuffFromCiv(nearest, "debuff_pyramid_leech");
		}
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
