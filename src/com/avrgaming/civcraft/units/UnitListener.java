package com.avrgaming.civcraft.units;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDeath(final PlayerDeathEvent event) {
		Player death = event.getEntity();
		Resident deathRes = CivGlobal.getResident(death);
		if (deathRes == null) return;
		Player killer = null;
		Resident killerRes = null;
		if (deathRes.getLastAttackTime() - System.currentTimeMillis() < 2000) {
			killerRes = deathRes.getLastAttacker();
			try {
				killer = CivGlobal.getPlayer(killerRes);
			} catch (CivException e) {
				e.printStackTrace();
			}
		}

		int unitId = deathRes.getUnitObjectId();
		UnitObject uo = CivGlobal.getUnitObject(unitId);
		if (uo != null) {
			int lostExp = (int) Math.round(UnitStatic.percent_exp_lost_when_dead * uo.getExpToNextLevel());

			UnitStatic.removeChildrenItems(death);
			uo.removeExp(lostExp);
			deathRes.setUnitObjectId(0);
			UnitStatic.updateUnitForPlaeyr(death);
			deathRes.calculateWalkingModifier(death);
			UnitStatic.setModifiedMovementSpeed(death);
		}
		if (killer != null) {
			int killUnitId = killerRes.getUnitObjectId();
			UnitObject killUo = CivGlobal.getUnitObject(killUnitId);
			if (killUo != null) {
				int addExp = (int) Math.round(UnitStatic.percent_exp_per_level_unit * uo.getExpToNextLevel());
				UnitStatic.addExpToPlayer(killer, addExp);
				CivMessage.global("Игрок " + killer.getName() + " убил игрока " + death.getName() + " и забрал себе " + addExp + " единиц опыта");
			}
			CivMessage.global("Игрок " + killer.getName() + " убил игрока " + death.getName());
		}

	}

}
