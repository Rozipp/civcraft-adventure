
package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.wonders.Wonder;

public class StockExchange
extends Wonder {
    private int level = 0;

    public StockExchange(ResultSet rs) throws SQLException, CivException {
        super(rs);
    }

    public StockExchange(String id, Town town) throws CivException {
        super(id, town);
        this.setLevel(town.SM.saved_stock_exchange_level);
    }

    @Override
    protected void removeBuffs() {
    }

    @Override
    protected void addBuffs() {
    }

    @Override
    public String getDynmapDescription() {
        String out = "<u><b>" + CivSettings.localize.localizedString("stockexchange_dynmapName") + "</u></b><br/>";
        out = out + CivSettings.localize.localizedString("Level") + " " + this.level;
        return out;
    }

    public int getLevel() {
        return this.level;
    }

    public final void setLevel(int level) {
        this.level = level;
    }

    @Override
    public void onPostBuild() {
        this.level = this.getTown().SM.saved_stock_exchange_level;
    }
}

