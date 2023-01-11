
package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.object.Town;

public class Burj extends Wonder {

    public Burj(String id, Town town) {
        super(id, town);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromCiv(this.getCivOwner(), "buff_burj_growth");
        this.removeBuffFromCiv(this.getCivOwner(), "buff_burj_happy");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToCiv(this.getCivOwner(), "buff_burj_growth");
        this.addBuffToCiv(this.getCivOwner(), "buff_burj_happy");
    }
}

