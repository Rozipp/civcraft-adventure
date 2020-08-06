
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.components.TransmuterComponent;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class SilkwormFarm extends Structure {

	public SilkwormFarm(String id, Town town) throws CivException {
		super(id, town);
	}

	public SilkwormFarm(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() {
		super.delete();
	}

	@Override
	public String getDynmapDescription() {
		return "";
	}

	@Override
	public String getMarkerIconName() {
		return "bighouse";
	}

	@Override
	public void onSecondUpdate(CivAsyncTask task) {
		if (!CivGlobal.fisheryEnabled) return;
		if (getTransmuter() == null) return;
		getTransmuter().processConsumption();
	}

	private TransmuterComponent transmuter;

	public TransmuterComponent getTransmuter() {
		if (transmuter == null) transmuter = (TransmuterComponent) this.getComponent("TransmuterComponent");
		return transmuter;
	}

	@Override
	public void onPostBuild() {
	}
}
