
package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.construct.RespawnLocationHolder;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Neuschwanstein extends Wonder implements RespawnLocationHolder {
	private final ArrayList<BlockCoord> revivePoints = new ArrayList<>();

    protected Neuschwanstein(String id, Town town) {
        super(id, town);
    }

    @Override
    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTownOwner(), "buff_neuschwanstein_culture");
    }

    @Override
    protected void addBuffs() {
        this.addBuffToTown(this.getTownOwner(), "buff_neuschwanstein_culture");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.getCivOwner().updateReviveSigns();
    }

    @Override
    public String getRespawnName() {
        String townInfo = CivColor.LightGray + "Neuschwanstein";
        Town town = this.getTownOwner();
        return townInfo + "\n" + CivColor.Gold + town.getName() + "\nLocation:\n" + CivColor.LightGreen + this.getCorner().getX() + " " + this.getCorner().getY() + " " + this.getCorner().getZ();
    }

    public void setRevivePoint(BlockCoord absCoord) {
        this.revivePoints.add(absCoord);
    }

    @Override
    public List<BlockCoord> getRespawnPoints() {
        return this.revivePoints;
    }

    @Override
    public BlockCoord getRandomRevivePoint() {
        if (this.revivePoints.size() == 0 || !this.isComplete()) {
            return new BlockCoord(this.getCorner());
        }
        Random rand = CivCraft.civRandom;
        int index = rand.nextInt(this.revivePoints.size());
        return this.revivePoints.get(index);
    }

    @Override
    public boolean isTeleportReal() {
        return !this.isDestroyed();
    }
}

