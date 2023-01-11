
package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.object.Town;

public class GrandCanyon extends Wonder {

    public GrandCanyon(String id, Town town) {
        super(id, town);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTownOwner(), "buff_grandcanyon_rush");
        this.removeBuffFromTown(this.getTownOwner(), "rush");
        this.removeBuffFromTown(this.getTownOwner(), "buff_grandcanyon_hammers");
        this.removeBuffFromTown(this.getTownOwner(), "buff_grandcanyon_quarry_and_trommel");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToTown(this.getTownOwner(), "buff_grandcanyon_rush");
        this.addBuffToTown(this.getTownOwner(), "rush");
        this.addBuffToTown(this.getTownOwner(), "buff_grandcanyon_hammers");
        this.addBuffToTown(this.getTownOwner(), "buff_grandcanyon_quarry_and_trommel");
    }
}

