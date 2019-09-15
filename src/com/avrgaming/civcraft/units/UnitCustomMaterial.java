/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.units;

import java.util.List;

import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.items.BaseCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.util.CivColor;

public class UnitCustomMaterial extends BaseCustomMaterial {

	private UnitMaterial parent = null;
	private int socketSlot = 0;

	public UnitCustomMaterial(String id, int minecraftId, short damage) {
		super(id, minecraftId, damage);
	}
	@Override
	public void addMaterial() {
		CustomMaterial.unitMaterials.put(this.getId(), this);
	}
	
	public static void buildStaticMaterials() {
		/* Loads in materials from configuration file. */
		for (ConfigMaterial cfgMat : CivSettings.unitMaterials.values()) {
			UnitCustomMaterial custMat = new UnitCustomMaterial(cfgMat.id, cfgMat.item_id, (short) cfgMat.item_data);
			custMat.setName(cfgMat.name);
			custMat.setLore(cfgMat.lore);
			custMat.configMaterial = cfgMat;
			custMat.buildComponents();
			if (custMat.components.containsKey("Espionage")) {
				ConfigMission mission = CivSettings.missions.get(custMat.components.get("Espionage").getString("espionage_id"));
				if (mission.slot > 0) {
					custMat.setName(mission.name);
					for (String str : mission.description) {
						custMat.addLore(str);
					}
					custMat.addLore(CivColor.Yellow+mission.cost+" "+CivSettings.CURRENCY_NAME);
					custMat.setSocketSlot(mission.slot);
					Spy.missionBooks.put(custMat.getId(), custMat);
					Spy.allowedSubslots.add(custMat.getSocketSlot());
				}
			}
		}
	}

	public UnitMaterial getParent() {
		return parent;
	}

	public void setParent(UnitMaterial parent) {
		this.parent = parent;
	}

	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
		// Never let these spawn as items.
		event.setCancelled(true);
	}

	public void setLoreArray(List<String> lore) {
		super.setLore("");
		for (String str : lore) {
			this.addLore(str);
		}
		this.addLore(CivColor.Gold + CivSettings.localize.localizedString("Soulbound"));
	}

	public int getSocketSlot() {
		return socketSlot;
	}

	public void setSocketSlot(int socketSlot) {
		this.socketSlot = socketSlot;
	}

	@Override
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		//	CivLog.debug("\tMissionBook )
		event.setCancelled(true);
	}

	@Override
	public boolean isCanUseInventoryTypes(Inventory inv) {
		switch (inv.getType()) {
			case CHEST :
			case CRAFTING :
			case DROPPER :
			case ENDER_CHEST :
			case HOPPER :
			case PLAYER :
			case SHULKER_BOX :
			case WORKBENCH :
				return true;
			default :
				return false;
		}
	}

}