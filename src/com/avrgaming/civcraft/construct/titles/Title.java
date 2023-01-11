package com.avrgaming.civcraft.construct.titles;

import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

import java.util.HashMap;

public class Title extends Buildable {

    private HashMap<String, BlockCoord> slots = new HashMap<>();

    public Title(String id, Town town) {
        super(id, town);
    }

    public BlockCoord getSlotBlockCoord(String constr_id){
        return slots.get(constr_id);
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
        ConstructSign structSign;
        switch (sb.command) {
            case "/slot":
                String id = sb.keyvalues.get("id");
                String bid = "";
                switch (id) {
                    case "0":
                        bid = "s_library";
                        break;
                    case "1":
                        bid = "s_lib";
                        break;
                    case "2":
                        bid = "s_university";
                        break;
                    case "3":
                        bid = "s_school";
                        break;
                }
                slots.put(bid, absCoord);
                break;
        }
    }

    @Override
    public void onComplete() {
    }
}
