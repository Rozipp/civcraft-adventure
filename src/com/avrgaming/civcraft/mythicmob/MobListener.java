package com.avrgaming.civcraft.mythicmob;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.units.UnitStatic;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicConditionLoadEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicDropLoadEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDespawnEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobLootDropEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicReloadedEvent;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicTargeterLoadEvent;

public class MobListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityCombust(EntityCombustEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			if (MobStatic.isMithicMobEntity((LivingEntity) event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onMobDeath(MythicMobDeathEvent event) {
		ConfigMobs cm = CivSettings.mobs.get(event.getMobType().getInternalName());
		List<ItemStack> itemDrops = event.getDrops();
		itemDrops = cm.getItemsDrop(itemDrops);
		event.setDrops(itemDrops);

		if (event.getKiller() instanceof Player) {
			Player killer = (Player) event.getKiller();
			if (!CivGlobal.getResident(killer).isUnitActive()) return;
			String mobName = event.getMobType().getInternalName();
			CivLog.debug("MythicMobDeathEvent " + mobName);
			UnitStatic.addExpToPlayer(killer, UnitStatic.getExpEntity(mobName));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onMobDespawn(MythicMobDespawnEvent event) {
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSpawnEntity(MythicMobLootDropEvent event) {
		CivLog.debug("MythicMobLootDropEvent");
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSpawnEntity(MythicMobSpawnEvent event) {
//		Entity en = event.getEntity();
//		ConfigMobs cm = CivSettings.mobs.get(event.getMobType().getInternalName());
//		en.setCustomName(cm.name);
//		en.setCustomNameVisible(true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSpawnEntity(MythicDropLoadEvent event) {
		CivLog.debug("MythicDropLoadEvent");
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onSpawnEntity(MythicConditionLoadEvent event) {
		CivLog.debug("MythicConditionLoadEvent");
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSpawnEntity(MythicMechanicLoadEvent event) {
		CivLog.debug("MythicMechanicLoadEvent");
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSpawnEntity(MythicReloadedEvent event) {
		CivLog.debug("MythicReloadedEvent");
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onSpawnEntity(MythicTargeterLoadEvent event) {
		CivLog.debug("MythicTargeterLoadEvent");
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
//		if (!MythicMobs.inst().getAPIHelper().isMythicMob(event.getEntity())) return;
//		ActiveMob am = MythicMobs.inst().getAPIHelper().getMythicMobInstance(event.getEntity());
//		Entity en = (Entity) BukkitAdapter.adapt((Entity) am);
//		CivLog.debug("getInternalName " + am.getType().getInternalName());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof LivingEntity)) return;
		if (!MobStatic.isMithicMobEntity(event.getEntity())) return;

		switch (event.getCause()) {
			case SUFFOCATION :
				Location loc = event.getEntity().getLocation();
				int y = loc.getWorld().getHighestBlockAt(loc.getBlockX(), loc.getBlockZ()).getY() + 4;
				loc.setY(y);
				event.getEntity().teleport(loc);
			case CONTACT :
			case FALL :
			case FIRE :
			case FIRE_TICK :
			case LAVA :
			case MELTING :
			case DROWNING :
			case FALLING_BLOCK :
			case BLOCK_EXPLOSION :
			case ENTITY_EXPLOSION :
			case LIGHTNING :
			case MAGIC :
				event.setCancelled(true);
				break;
			default :
				break;
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLeash(PlayerLeashEntityEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			if (MobStatic.isMithicMobEntity(event.getEntity())) {
				CivMessage.sendError(event.getPlayer(), "This beast cannot be tamed.");
				event.setCancelled(true);
				return;
			}
		}
	}
}
