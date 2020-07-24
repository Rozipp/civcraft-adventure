
package com.avrgaming.civcraft.construct.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class Burj extends Wonder {
    public Burj(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public Burj(String id, Town town) throws CivException {
        super(id, town);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromCiv(this.getCiv(), "buff_burj_growth");
        this.removeBuffFromCiv(this.getCiv(), "buff_burj_happy");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToCiv(this.getCiv(), "buff_burj_growth");
        this.addBuffToCiv(this.getCiv(), "buff_burj_happy");
    }
}

