
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.construct.Transmuter;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;

public class SilkwormFarm extends Structure {
	
	public Transmuter transmuter = new Transmuter(this);

	public SilkwormFarm(String id, Town town) throws CivException {
		super(id, town);
	}

	public SilkwormFarm(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}
	
	@Override
	public void delete() {
		transmuter.stop();
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
	public void onPostBuild() {
		this.transmuter.addRecipe("silkwormfarm");
		if (CivGlobal.trommelsEnabled) this.transmuter.start();
	}
}
