package com.avrgaming.civcraft.construct.titles;

import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.object.Town;
import com.google.common.collect.Lists;

import java.util.List;

public class ScienceTitle extends Title {

    public ScienceTitle(String id, Town town) {
        super(id, town);
    }

    @Override
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
}
