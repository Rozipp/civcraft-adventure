package com.avrgaming.civcraft.units;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.mythicmob.MobStatic;
import com.avrgaming.civcraft.object.Resident;

public class UnitListener implements Listener {

	public UnitListener() { /* для Listener */
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityDeath(final EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if (entity != null) {
			if (MobStatic.isMithicMobEntity(entity)) return;
			Player killer = event.getEntity().getKiller();
			if (killer != null) {
				if (!CivGlobal.getResident(killer).isUnitActive()) return;
				if (entity instanceof Player) {
					Player player = (Player) entity;
					Resident res = CivGlobal.getResident(player);
					int exp = UnitStatic.exp_for_player;
					if (res.isUnitActive()) {
						UnitObject uo = CivGlobal.getUnitObject(res.getUnitObjectId());
						exp = exp + CivGlobal.getUnitObject(res.getUnitObjectId()).getLevel() * CivSettings.unitConfig.getInt("exp_for_unit_per_level");
						exp = (int) (exp + uo.getExpToNextLevel() * UnitStatic.percent_exp_per_level_unit);
					}
					UnitStatic.addExpToPlayer(killer, exp);
				}
				UnitStatic.addExpToPlayer(killer, UnitStatic.exp_for_neutral_entity);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		Player player = event.getEntity();
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;
		int unitId = resident.getUnitObjectId();
		if (unitId <= 0) return;
		UnitObject uo = CivGlobal.getUnitObject(unitId);

		UnitStatic.removeChildrenItems(player);
		uo.removeExp((int) Math.round(UnitStatic.percent_exp_lost_when_dead * uo.getExpToNextLevel()));
		resident.setUnitObjectId(0);
		UnitStatic.updateUnitForPlaeyr(player);
		resident.calculateWalkingModifier(player);
//		Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin) ItemJoin.getInstance(), (Runnable) new Runnable() {
//			@Override
//			public void run() {
		UnitStatic.setModifiedMovementSpeed(player);
//			}
//		}, 1L);
	}

}
