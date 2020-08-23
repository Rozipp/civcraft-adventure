package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.components.TransmuterComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class Quarry extends Structure {

	public int level;

	public Quarry(String id, Town town) {
		super(id, town);
		this.level = town.BM.saved_quarry_level;
	}

	@Override
	public void delete() {
		super.delete();
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + this.getDisplayName() + "</u></b><br/>";
		out += CivSettings.localize.localizedString("Level") + " " + level;
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "minecart";
	}

	public int getLevel() {
		return level;
	}

	@Override
	public void onSecondUpdate(CivAsyncTask task) {
		if (!CivGlobal.quarriesEnabled) return;
		if (getTransmuter() == null) return;
		getTransmuter().processConsumption();
	}

	@Override
	public void onCivtickUpdate(CivAsyncTask task) {
		modifyTransmuterChance();
	}

	private TransmuterComponent transmuter;

	public TransmuterComponent getTransmuter() {
		if (transmuter == null) transmuter = (TransmuterComponent) this.getComponent("TransmuterComponent");
		return transmuter;
	}

	@Override
	public void onPostBuild() {
		this.level = getTownOwner().BM.saved_quarry_level;
		modifyTransmuterChance();
		getTransmuter().setLevel(level);
	}

	public void modifyTransmuterChance() {
		double chance = 1.0;
		double extraction = this.getTownOwner().getBuffManager().getEffectiveDouble("buff_extraction");
		chance += (extraction > 2) ? 2 : extraction;
		chance += this.getTownOwner().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel");

		try {
			if (this.getTownOwner().getGovernment().id.equals("gov_despotism")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.despotism_rate");
			if (this.getTownOwner().getGovernment().id.equals("gov_theocracy") || this.getTownOwner().getGovernment().id.equals("gov_monarchy")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.penalty_rate");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		getTransmuter().setModifyChance(chance);
	}

}
