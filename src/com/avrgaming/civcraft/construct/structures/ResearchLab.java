package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;

public class ResearchLab extends Structure {
	
	public ResearchLab(String id, Town town) {
		super(id, town);
	}

	@Override
	public String getMarkerIconName() {
		return "warning";
	}

	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}
	
	@Override
	public void onComplete() {
		addBuffs();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}
	
	protected void removeBuffs() {
		this.removeBuffFromTown(this.getTownOwner(), "buff_profit_sharing");
	}

	protected void addBuffs() {
		this.addBuffToTown(this.getTownOwner(), "buff_profit_sharing");
	}
	
	protected void addBuffToTown(Town town, String id) {
		try {
			town.getBuffManager().addBuff(id, id, this.getDisplayName()+" in "+this.getTownOwner().getName());
		} catch (CivException e) {
			e.printStackTrace();
		}
	}
	
	protected void addBuffToCiv(Civilization civ, String id) {
		for (Town t : civ.getTowns()) {
			addBuffToTown(t, id);
		}
	}
	
	protected void removeBuffFromTown(Town town, String id) {
		town.getBuffManager().removeBuff(id);
	}
	
	protected void removeBuffFromCiv(Civilization civ, String id) {
		for (Town t : civ.getTowns()) {
			removeBuffFromTown(t, id);
		}
	}

}
