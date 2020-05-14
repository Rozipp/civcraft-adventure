package com.avrgaming.civcraft.enchantment;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.units.UnitStatic;

public class CustomEnchantmentListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (event.isCancelled()) return;
		ItemStack stack = event.getItemDrop().getItemStack();
		if (stack == null) return;
		if (Enchantments.hasEnchantment(stack, CustomEnchantment.UnitItem)) {
			event.setCancelled(true);
			event.getPlayer().updateInventory();
			CivMessage.send(event.getPlayer(), CivSettings.localize.localizedString("unitItem_cannotDrop"));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		CivLog.debug("CustomEnchantmentListener.onPlayerInteract");
		ItemStack stack = event.getItem();
		if (stack == null) return;
		if (Enchantments.hasEnchantment(stack, CustomEnchantment.UnitItem)) {
			Player player = (Player) event.getPlayer();
			if (!CivGlobal.getResident(player).isUnitActive()) {
				UnitStatic.removeChildrenItems(player);
				event.setCancelled(true);
				CivMessage.send(event.getPlayer(), "Юнит не активен. Предметы юнита были удалены");
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onDamage(PlayerItemDamageEvent event) {
		if (Enchantments.hasEnchantment(event.getItem(), CustomEnchantment.UnitItem)) {
			Player player = (Player) event.getPlayer();
			if (!CivGlobal.getResident(player).isUnitActive()) {
				UnitStatic.removeChildrenItems(player);
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDefenseAndAttack(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player) {
			Player player = (Player) event.getDamager();
			if (Enchantments.hasEnchantment(player.getInventory().getItemInMainHand(), CustomEnchantment.UnitItem)) {
				if (!CivGlobal.getResident(player).isUnitActive()) {
					event.setCancelled(true);
					UnitStatic.removeChildrenItems(player);
				}
			}
		}
	}
}
