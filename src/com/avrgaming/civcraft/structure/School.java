package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class School extends Structure {

	
	public School(String id, Town town) throws CivException {
		super(id, town);
	}

	public School(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}
	
	@Override
	public String getMarkerIconName() {
		return "walk";
	}

}
