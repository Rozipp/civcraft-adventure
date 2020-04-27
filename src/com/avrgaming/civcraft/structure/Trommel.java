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

import org.bukkit.Location;

import com.avrgaming.civcraft.construct.Transmuter;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;

public class Trommel extends Structure {

	private int level = 1;
	public Transmuter transmuter = new Transmuter(this);

	public Trommel(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		level = town.saved_trommel_level;
	}

	public Trommel(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() throws SQLException {
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
	public double modifyTransmuterChance(Double chance) {
		double increase = chance * (this.getTown().getBuffManager().getEffectiveDouble("buff_extraction") + this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel"));
		chance += increase;

		// try {
		// if (this.getTown().getGovernment().id.equals("gov_despotism")) {
		// chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.despotism_rate");
		// } else if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")){
		// chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.penalty_rate");
		// }
		// } catch (InvalidConfiguration e) {
		// e.printStackTrace();
		// }
		return chance;
	}

	@Override
	public void onPostBuild() {
		this.level = getTown().saved_trommel_level;
		this.transmuter.addAllRecipeToLevel(level);
		if (CivGlobal.trommelsEnabled) this.transmuter.run();
	}

}
