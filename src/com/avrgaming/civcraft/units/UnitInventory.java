package com.avrgaming.civcraft.units;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;

public class UnitInventory {
	public Town town;
	public Inventory guiInventory = null;
	public Chest chest = null;
	public Set<Integer> unitList = new HashSet<>();

	public UnitInventory(Town town) {
		this.town = town;
	}

	public void buildInventory() {
		if (guiInventory == null) {
			guiInventory = UnitInventoryListener.createInventory(chest, "Юниты города  " + town.getName());
		}
		guiInventory.clear();
		List<Integer> deleteUnit = new ArrayList<Integer>();
		for (Integer uId : unitList) {
			UnitObject uo = CivGlobal.getUnitObject(uId);
			if (uo == null) {
				deleteUnit.add(uId);
				continue;
			}

			// ItemStack foundItem = ItemManager.foundItem(guiInventory, uo.getLastHashCode());
			// if (foundItem != null) {
			// CivLog.debug("foundItem");
			// continue;
			// }

			if (!uo.validLastActivate()) {
				guiInventory.addItem(UnitStatic.respawn(uo.getId()));
				continue;
			}

			guiInventory.addItem(UnitInventoryListener.buildGuiUnit(uo));
		}
		for (Integer uId : deleteUnit) {
			removeUnit(uId);
		}
	}

	public void showUnits(Player player) throws CivException {
		if (chest == null) throw new CivException("У вас нет бараков");
		buildInventory();
		player.openInventory(guiInventory);
	}


	public void addUnit(Integer uoId) {
		unitList.add(uoId);
	}

	public void removeUnit(Integer uoId) {
		unitList.remove(uoId);
	}

	public int getUnitTypeCount(String id) {
		Integer count = 0;
		for (Integer uoId : unitList) {
			if (id.equalsIgnoreCase(CivGlobal.getUnitObject(uoId).getConfigUnitId())) count++;
		}
		return count;
	}
	
}