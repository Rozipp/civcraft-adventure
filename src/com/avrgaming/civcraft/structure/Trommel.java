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
	public Transmuter transmuter;

	public Trommel(String id, Town town) throws CivException {
		super(id, town);
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
	public void onCivtickUpdate() {
		modifyTransmuterChance();
	}

	public void modifyTransmuterChance() {
		Double chance = 1.0;
		chance += 0.10 * getTown().BM.saved_trommel_level;
		double extraction = this.getTown().getBuffManager().getEffectiveDouble("buff_extraction");
		chance += (extraction > 2) ? 2 : extraction;
		chance += this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel");

		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.despotism_rate");
			if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.penalty_rate");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		transmuter.setModifyChance(chance);
	}

	@Override
	public void onPostBuild() {
		transmuter = new Transmuter(this);
		this.level = getTown().BM.saved_trommel_level;
		modifyTransmuterChance();
		this.addTromelRecipe(level);
		if (CivGlobal.trommelsEnabled) this.transmuter.start();
	}

	public void addTromelRecipe(Integer level) {
		switch (level) {
		case 4:
			this.transmuter.addRecipe("trommel_andesit");
		case 3:
			this.transmuter.addRecipe("trommel_diorit");
		case 2:
			this.transmuter.addRecipe("trommel_granit");
		case 1:
			this.transmuter.addRecipe("trommel_clay");
			this.transmuter.addRecipe("trommel_cobblestone");
		}
	}

}
