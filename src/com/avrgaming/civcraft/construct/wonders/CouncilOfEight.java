package com.avrgaming.civcraft.construct.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class CouncilOfEight extends Wonder {

	public CouncilOfEight(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public CouncilOfEight(String id, Town town) throws CivException {
		super(id, town);
	}

	@Override
	protected void removeBuffs() {
	}

	@Override
	protected void addBuffs() {		
	}

}
