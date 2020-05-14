/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.units;

import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.config.ConfigUnitMaterial;
import com.avrgaming.civcraft.items.BaseCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.items.components.ItemComponent;
import com.avrgaming.civcraft.util.CivColor;

public class UnitCustomMaterial extends BaseCustomMaterial {

	private int socketSlot = 0;

	public UnitCustomMaterial(String id, int minecraftId, short damage) {
		super(id, minecraftId, damage);
		CustomMaterial.unitMaterials.put(this.getId(), this);
	}

	public static void buildStaticMaterials() {
		/* Loads in materials from configuration file. */
		for (ConfigUnitMaterial cfgMat : CivSettings.unitMaterials.values()) {
			UnitCustomMaterial custMat = new UnitCustomMaterial(cfgMat.id, cfgMat.item_id, (short) cfgMat.item_data);
			custMat.setName(cfgMat.name);
			custMat.setLore(cfgMat.lore);
			custMat.setSocketSlot(cfgMat.slot);
			custMat.configMaterial = cfgMat;
			custMat.buildComponents();
			if (custMat.components.containsKey("Espionage")) {
				ConfigMission mission = CivSettings.missions.get(custMat.components.get("Espionage").getString("espionage_id"));
				custMat.addLore(CivColor.Yellow + mission.cost + " " + CivSettings.CURRENCY_NAME);
			}
		}
	}

	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
		event.setCancelled(true);
	}

	@Override
	public void onDropItem(PlayerDropItemEvent event) {
		ItemComponent ic = components.get("Cooldown");
		if (ic != null && ic.getString("lock") != null) {
			event.setCancelled(true);
			event.getItemDrop().setItemStack(null);
		}
	}

	@Override
	public void onPickupItem(EntityPickupItemEvent event) {
		ItemComponent ic = components.get("Cooldown");
		if (ic != null && ic.getString("lock") != null) event.setCancelled(true);
	}

	@Override
	public void onInvItemPickup(InventoryClickEvent event, Inventory fromInv, ItemStack stack) {
		ItemComponent ic = components.get("Cooldown");
		if (ic != null && ic.getString("lock") != null) event.setCancelled(true);
	}

	@Override
	public void onInvItemDrop(InventoryClickEvent event, Inventory toInv, ItemStack stack) {
		ItemComponent ic = components.get("Cooldown");
		if (ic != null && ic.getString("lock") != null) event.setCancelled(true);
	}

	@Override
	public void onInvItemDrag(InventoryDragEvent event, Inventory toInv, ItemStack stack) {
		ItemComponent ic = components.get("Cooldown");
		if (ic != null && ic.getString("lock") != null) event.setCancelled(true);
	}

	public int getSocketSlot() {
		return socketSlot;
	}

	public void setSocketSlot(int socketSlot) {
		this.socketSlot = socketSlot;
	}

	@Override
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		// CivLog.debug("\tMissionBook )
		event.setCancelled(true);
	}

	@Override
	public boolean isCanUseInventoryTypes(Inventory inv) {
		switch (inv.getType()) {
		case CHEST:
		case DROPPER:
		case ENDER_CHEST:
		case HOPPER:
		case PLAYER:
		case SHULKER_BOX:
		case WORKBENCH:
			return true;
		default:
			return false;
		}
	}

}
