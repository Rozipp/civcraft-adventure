package com.avrgaming.civcraft.construct.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class GlobeTheatre extends Wonder {

	public GlobeTheatre(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public GlobeTheatre(String id, Town town) throws CivException {
		super(id, town);
	}

	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCiv(), "buff_globe_theatre_happiness_to_towns");
		removeBuffFromTown(this.getTown(), "buff_globe_theatre_culture_from_towns");
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCiv(), "buff_globe_theatre_happiness_to_towns");
		addBuffToTown(this.getTown(), "buff_globe_theatre_culture_from_towns");
	}
	
	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}
	
	@Override
	public void onComplete() {
		addBuffs();
	}

}
