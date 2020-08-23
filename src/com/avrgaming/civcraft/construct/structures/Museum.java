package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class Museum extends Structure {

	public Museum(String id, Town town) {
		super(id, town);
	}
	
	@Override
	public String getMarkerIconName() {
		return "flower";
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
		this.removeBuffFromTown(this.getTownOwner(), "buff_art_appreciation");
	}

	protected void addBuffs() {
		this.addBuffToTown(this.getTownOwner(), "buff_art_appreciation");
	}
	
	protected void addBuffToTown(Town town, String id) {
		try {
			town.getBuffManager().addBuff(id, id, this.getDisplayName()+" in "+this.getTownOwner().getName());
		} catch (CivException e) {
			e.printStackTrace();
		}
	}
	
	protected void removeBuffFromTown(Town town, String id) {
		town.getBuffManager().removeBuff(id);
	}
}
