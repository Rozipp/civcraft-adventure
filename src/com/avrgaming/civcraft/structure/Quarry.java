package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Transmuter;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;

public class Quarry extends Structure {

	public int level;
	public Transmuter transmuter = new Transmuter(this);

	public Quarry(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		this.level = town.saved_quarry_level;
	}

	public Quarry(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() throws SQLException {
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
	public double modifyTransmuterChance(Double chance) {
		double increase = chance * (this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION) + this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_quarry_and_trommel"));
		chance += increase;

		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.despotism_rate");
			} else if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")) {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.penalty_rate");
			}

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		return chance;
	}

	@Override
	public void onPostBuild() {
		this.level = getTown().saved_quarry_level;
		this.transmuter.addAllRecipeToLevel(level);
		if (CivGlobal.quarriesEnabled) this.transmuter.run();
	}

	public int getLevel() {
		return level;
	}
}
