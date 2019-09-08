package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;

public class ChoiceUnitComponent implements GuiAction {
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player) event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);
		UnitObject uo = CivGlobal.getUnitObject(resident.getUnitId());

		String component = LoreGuiItem.getActionData(stack, "component");

		uo.addComponent(component, 1);

		event.getWhoClicked().closeInventory();
		UnitStatic.updateUnitForPlaeyr(player);
		
		uo.removeLevelUp();
		uo.save();
		uo.rebuildUnitItem(player);
	}
}
