package com.avrgaming.civcraft.mythicmob;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.listener.SimpleListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.units.UnitStatic;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;

public class MobListener extends SimpleListener{

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityCombust(EntityCombustEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			if (MobStatic.isMithicMobEntity(event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onMobDeath(MythicMobDeathEvent event) {
		ConfigMobs cm = CivSettings.mobs.get(event.getMobType().getInternalName());
		List<ItemStack> itemDrops = event.getDrops();
		Player killer = null;
		if (event.getKiller() instanceof Player) killer = (Player) event.getKiller();

		Double modify = 1.0;
		if (killer != null) {
			ItemStack stack = killer.getInventory().getItemInMainHand();
			if (Enchantments.hasEnchantment(stack, EnchantmentCustom.LOOT_BONUS_MOBS))
				modify = modify + 0.3 * Enchantments.getLevelEnchantment(stack, EnchantmentCustom.LOOT_BONUS_MOBS);
		}
		event.setDrops(cm.getItemsDrop(itemDrops, modify));

		if (killer != null) {
			if (!CivGlobal.getResident(killer).isUnitActive()) return;
			String mobName = event.getMobType().getInternalName();
			UnitStatic.addExpToPlayer(killer, UnitStatic.getExpEntity(mobName));
		}
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
