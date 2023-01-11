package com.avrgaming.civcraft.items.components;

import java.util.Date;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveSpyMission;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.war.War;

import gpl.AttributeUtil;

public class Espionage extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
	}

	public void onInteract(PlayerInteractEvent event) {
		try {
			if (War.isWarTime()) {
				throw new CivException(CivSettings.localize.localizedString("missionBook_errorDuringWar"));
			}
			
			ConfigMission mission = CivSettings.missions.get(this.getString("espionage_id"));
			if (mission == null) {
				throw new CivException(CivSettings.localize.localizedString("missionBook_errorInvalid")+" "+this.getString("espionage_id"));
			}
			
			Resident resident = CivGlobal.getResident(event.getPlayer());
			if (resident == null || !resident.hasTown()) {
				throw new CivException(CivSettings.localize.localizedString("missionBook_errorNotResident"));
			}
			Date now = new Date();
			
			if (!event.getPlayer().isOp()) { 
				try {
					int spyRegisterTime = CivSettings.getInteger(CivSettings.unitMaterialsConfig, "espionage.spy_register_time");
					int spyOnlineTime = CivSettings.getInteger(CivSettings.unitMaterialsConfig, "espionage.spy_online_time");
					
					long expire = resident.getRegistered() + (spyRegisterTime*60*1000);
					if (now.getTime() <= expire) {
						throw new CivException(CivSettings.localize.localizedString("missionBook_errorTooSoon"));
					}
					
					expire = resident.getLastOnline() + (spyOnlineTime*60*1000);
					if (now.getTime() <= expire) {
						throw new CivException(CivSettings.localize.localizedString("missionBook_errorPlayLonger"));
					}
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
				}
			}
						
			UnitObject uo = UnitStatic.getPlayerUnitObject(event.getPlayer());
			if (uo == null || !uo.getConfigUnitId().equals("u_spy")) {
				event.getPlayer().getInventory().remove(event.getItem());
				throw new CivException(CivSettings.localize.localizedString("missionBook_errorNotSpy"));
			}
			
			ChunkCoord coord = new ChunkCoord(event.getPlayer().getLocation());
			CultureChunk cc = CivGlobal.getCultureChunk(coord);
			TownChunk tc = CivGlobal.getTownChunk(coord);
		
			if (cc == null || cc.getCiv() == resident.getCiv()) {
				throw new CivException(CivSettings.localize.localizedString("missionBook_errorDifferentCiv"));
			}
			
			if ((cc != null && cc.getCiv().isAdminCiv()) || (tc != null && tc.getTown().getCiv().isAdminCiv())) {
				throw new CivException(CivSettings.localize.localizedString("missionBook_errorAdminCiv"));
			}
			
			if (CivGlobal.isCasualMode()) {
				if (!cc.getCiv().getDiplomacyManager().isHostileWith(resident.getCiv()) &&
					!cc.getCiv().getDiplomacyManager().atWarWith(resident.getCiv())) {
					throw new CivException(CivSettings.localize.localizedString("var_missionBook_errorCasualNotWar",cc.getCiv().getName()));
				}
			}
			
			resident.setInteractiveMode(new InteractiveSpyMission(mission, event.getPlayer().getName(), event.getPlayer().getLocation(), cc.getTown()));
		} catch (CivException e) {
			CivMessage.sendError(event.getPlayer(), e.getMessage());
		}
	}

	public void onItemSpawn(ItemSpawnEvent event) {
		event.setCancelled(true);
	}
	
}
