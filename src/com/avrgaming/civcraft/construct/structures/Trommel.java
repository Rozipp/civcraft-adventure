/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.components.TransmuterComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class Trommel extends Structure {

	private int level = 1;

	public Trommel(String id, Town town) throws CivException {
		super(id, town);
	}

	public Trommel(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() {
		super.delete();
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + this.getDisplayName() + "</u></b><br/>";
		out += CivSettings.localize.localizedString("Level") + " " + this.level;
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "minecart";
	}

	@Override
	public void onSecondUpdate(CivAsyncTask task) {
		if (!CivGlobal.trommelsEnabled) return;
		if (getTransmuter() == null) return;
//		Long begin = System.currentTimeMillis();
//		CivLog.debug(getTransmuter().processConsumption().toString() + " after " + (System.currentTimeMillis() - begin));
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
		this.level = getTown().BM.saved_trommel_level;
		modifyTransmuterChance();
		getTransmuter().setLevel(level);
	}

	public void modifyTransmuterChance() {
		Double chance = 1.0;
		chance += 0.10 * this.level;
		double extraction = this.getTown().getBuffManager().getEffectiveDouble("buff_extraction");
		chance += (extraction > 2) ? 2 : extraction;
		chance += this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel");

		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.despotism_rate");
			if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")) chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.penalty_rate");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		getTransmuter().setModifyChance(chance);
	}

}