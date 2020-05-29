/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Transmuter;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;

public class Trommel extends Structure {

	private int level = 1;
	public Transmuter transmuter = new Transmuter(this);

	public Trommel(String id, Town town) throws CivException {
		super(id, town);
		level = town.saved_trommel_level;
	}

	public Trommel(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() {
		transmuter.stop();
		super.delete();
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + this.getDisplayName() + "</u></b><br/>";
		out += "Level: " + this.level;
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
		Double chance = 1.0 + (0.5 * getTown().saved_trommel_level);
		double increase = (this.getTown().getBuffManager().getEffectiveDouble("buff_extraction") + this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel"));
		chance += increase;

		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.despotism_rate");
			if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.penalty_rate");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		transmuter.modifyChance = chance;
	}

	@Override
	public void onPostBuild() {
		this.level = getTown().saved_trommel_level;
		this.addTromelRecipe(level);
		if (CivGlobal.trommelsEnabled) this.transmuter.start();
	}

	public void addTromelRecipe(Integer level) {
		switch (level) {
		case 0:
			break;
		case 1:
		case 2:
		case 3:
		case 4:
			this.transmuter.addRecipe("trommel_clay");
			this.transmuter.addRecipe("trommel_cobblestone");
			this.transmuter.addRecipe("trommel_granit");
			this.transmuter.addRecipe("trommel_diorit");
			this.transmuter.addRecipe("trommel_andesit");
			break;
		default:
			break;
		}
	}

}
