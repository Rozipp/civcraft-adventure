
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class BeMine extends Structure {
	public BeMine(Location center, String id, Town town) throws CivException {
		super(id, town);
	}

	public BeMine(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public String getMarkerIconName() {
		return "offlineuser";
	}
}
