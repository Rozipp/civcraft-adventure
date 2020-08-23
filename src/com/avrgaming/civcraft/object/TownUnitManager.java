package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.guiinventory.UnitInventory;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TownUnitManager {
    private final Town town;
    private final Set<Integer> unitList = new HashSet<>();
    private UnitInventory unitInventory;

    public TownUnitManager(Town town) {
        this.town = town;
    }

    public void showUnits(Player player) throws CivException {
        if (unitInventory == null) {
            unitInventory = new UnitInventory(player, town);
        }
        unitInventory.openInventory(player);
    }

    public void addUnit(Integer uId) {
        unitList.add(uId);
        if (unitInventory != null) {
            Inventory inv = unitInventory.getInventory();
            if (inv != null) {
                UnitObject uo = CivGlobal.getUnitObject(uId);
                if (uo != null) inv.addItem(UnitStatic.respawn(uo));
            }
        }
    }

    public void removeUnit(Integer uoId) {
        unitList.remove(uoId);
    }

    public Collection<Integer> getUnits(){
        return unitList;
    }

    public int getUnitTypeCount(String id) {
        int count = 0;
        for (Integer uoId : unitList) {
            if (id.equalsIgnoreCase(CivGlobal.getUnitObject(uoId).getConfigUnitId())) count++;
        }
        return count;
    }

}
