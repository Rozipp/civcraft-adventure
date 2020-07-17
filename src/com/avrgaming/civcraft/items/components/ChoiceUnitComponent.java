package com.avrgaming.civcraft.items.components;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.lorestorage.GuiInventory;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import gpl.AttributeUtil;

public class ChoiceUnitComponent extends ItemComponent {
	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Resident resident = CivGlobal.getResident(player);
		int unitid = resident.getUnitObjectId();
		if (unitid <= 0) {
			CivMessage.send(player, "Юнит не найден");
			event.setCancelled(true);
			return;
		}
		GuiInventory.getGuiInventory(player, "ChoiceUnitComponent", null).openInventory();
	}

	@Override
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		event.setCancelled(true);
	}

	@Override
	public void onPlayerLeashEvent(PlayerLeashEntityEvent event) {
	}
}
