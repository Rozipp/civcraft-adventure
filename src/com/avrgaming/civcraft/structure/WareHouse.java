
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Structure;

public class WareHouse
extends Structure {
    public WareHouse(String id, Town town) throws CivException {
        super(id, town);
    }

    public WareHouse(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public String getDynmapDescription() {
        return "=)";
    }

    @Override
    public String getMarkerIconName() {
        return "flower";
    }
}

