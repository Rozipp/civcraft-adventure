
package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.object.Town;

public class StockExchange extends Wonder {
    private int level = 0;

    public StockExchange(String id, Town town){
        super(id, town);
        this.setLevel(town.BM.saved_stock_exchange_level);
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
        this.level = this.getTownOwner().BM.saved_stock_exchange_level;
    }
}

