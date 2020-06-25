package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Transmuter;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;

public class Quarry extends Structure {

	public int level;
	public Transmuter transmuter = new Transmuter(this);

	public Quarry(String id, Town town) throws CivException {
		super(id, town);
		this.level = town.SM.saved_quarry_level;
	}

	public Quarry(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() {
		transmuter.stop();
		super.delete();
	}

	@Override
	public void onSecondUpdate() {
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

	@Override
	public void onMinuteUpdate() {
		modifyTransmuterChance();
	}

	public void modifyTransmuterChance() {
		Double chance = 1.0;
		double extraction = this.getTown().getBuffManager().getEffectiveDouble("buff_extraction");
		chance += (extraction > 2) ? 2 : extraction;
		chance += this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel");

		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.despotism_rate");
			if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.penalty_rate");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		transmuter.setModifyChance(chance);
	}

	@Override
	public void onPostBuild() {
		this.level = getTown().SM.saved_quarry_level;
		modifyTransmuterChance();
		this.transmuter.clearRecipe();
		for (int i = 1; i <= level; i++)
			this.transmuter.addRecipe("quarry" + i);
		if (CivGlobal.quarriesEnabled) this.transmuter.start();
	}

	public int getLevel() {
		return level;
	}
}
