
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Location;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Structure;

public class Castle extends Structure {
    protected Castle(Location center, String id, Town town) throws CivException {
        super(center, id, town);
    }

    public Castle(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    @Override
    public String getMarkerIconName() {
        return "door";
    }
}

