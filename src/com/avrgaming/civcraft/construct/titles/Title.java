package com.avrgaming.civcraft.construct.titles;

import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Title extends Buildable {
    public Title(String id, Town town) {
        super(id, town);
    }

    @Override
    public void validCanProgressBuild() throws CivException {

    }

    @Override
    public String getMarkerIconName() {
        return null;
    }

    @Override
    public void onPostBuild() {

    }

    @Override
    public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {

    }

    @Override
    public void onComplete() {

    }
}
