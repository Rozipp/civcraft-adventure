package com.avrgaming.civcraft.construct.titles;

import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.Direction;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

import java.util.HashMap;

public class Title extends Buildable {

    private final HashMap<Integer, BlockCoord> slots = new HashMap<>();
    private final HashMap<Integer, Direction> directions = new HashMap<>();
    private final HashMap<Integer, BlockCoord> buildables = new HashMap<>();

    public Title(String id, Town town) {
        super(id, town);
    }

    public BlockCoord getBlockCoordSlot(int slot) {
        return slots.get(slot);
    }

    public Direction getDirection(int slot) {
        return directions.get(slot);
    }

    public int getNextSlot(ConfigConstructInfo cInfo) throws CivException {
        CivLog.debug("getNextSlot cInfo.id=" + cInfo.id);
        for (Integer slot : slots.keySet()) {
            if (getBuildableInSlot(slot) != null) continue;
            String canBuild = getCanBuildInSlot(slot);
            CivLog.debug("slot " + slot + "  canBuild=" + canBuild);
            if (canBuild.contains(cInfo.id)) return slot;
        }
        throw new CivException("В этом районе для этого здания нет свободных слотов ");
    }

    public void setBuildableToSlot(int slot, Buildable buildable) {
        buildables.put(slot, buildable.getCorner());
        this.getVariables().put("" + slot, "" + buildable.getCorner());
    }

    public Buildable getBuildableInSlot(int slot) {
        if (buildables.containsKey(slot)) return getTownOwner().BM.getStructure(buildables.get(slot));
        else return null;
    }

    public String getCanBuildInSlot(int slot) {
        switch (slot) {
            case 0:
                return "s_library";
            case 1:
                return "ti_observatory";
            case 2:
                return "s_university";
            case 3:
                return "s_school";
        }
        return "";
    }

    @Override
    public void onLoad() throws CivException {
        super.onLoad();
        for (String sslot : getVariables().keySet()) {
            Integer slot = Integer.parseInt(sslot);
            BlockCoord bcoodr = new BlockCoord(getVariables().get(sslot));
            buildables.put(slot, bcoodr);
        }
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
        if (sb.command.equals("/slot")) {
            Integer slot = Integer.parseInt(sb.keyvalues.get("id"));
            Integer size_x = Integer.parseInt(sb.keyvalues.get("x"));
            Integer size_z = Integer.parseInt(sb.keyvalues.get("z"));
            Direction dir = Direction.newDirection(sb);
            directions.put(slot, dir);
            slots.put(slot, dir.convertBlockCoord(absCoord.clone(), size_x, size_z));
        }
    }

    @Override
    public void onComplete() {
    }
}
