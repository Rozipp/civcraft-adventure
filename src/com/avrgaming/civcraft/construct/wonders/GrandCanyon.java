
package com.avrgaming.civcraft.construct.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class GrandCanyon
extends Wonder {
    public GrandCanyon(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public GrandCanyon(String id, Town town) throws CivException {
        super(id, town);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTown(), "buff_grandcanyon_rush");
        this.removeBuffFromTown(this.getTown(), "rush");
        this.removeBuffFromTown(this.getTown(), "buff_grandcanyon_hammers");
        this.removeBuffFromTown(this.getTown(), "buff_grandcanyon_quarry_and_trommel");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToTown(this.getTown(), "buff_grandcanyon_rush");
        this.addBuffToTown(this.getTown(), "rush");
        this.addBuffToTown(this.getTown(), "buff_grandcanyon_hammers");
        this.addBuffToTown(this.getTown(), "buff_grandcanyon_quarry_and_trommel");
    }
}

