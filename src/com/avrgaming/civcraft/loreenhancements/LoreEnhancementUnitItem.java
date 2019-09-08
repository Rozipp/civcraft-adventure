package com.avrgaming.civcraft.loreenhancements;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class LoreEnhancementUnitItem extends LoreEnhancement {
	public String getDisplayName() {
		return "Предмет юнита";
	}

	public AttributeUtil add(AttributeUtil attrs) {
		attrs.addEnhancement("LoreEnhancementUnitItem", null, null);
		attrs.addLore(CivColor.LightBlue + getDisplayName());
		return attrs;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onDamage(PlayerItemDamageEvent event) {
		if ((new AttributeUtil(event.getItem())).hasEnhancement("LoreEnhancementUnitItem")) {
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
			if ((new AttributeUtil(player.getInventory().getItemInMainHand())).hasEnhancement("LoreEnhancementUnitItem")) {
				if (!CivGlobal.getResident(player).isUnitActive()) {
					event.setCancelled(true);
					UnitStatic.removeChildrenItems(player);
				}
			}
		}
	}
	@Override
	public String serialize(ItemStack stack) {
		return "";
	}

	@Override
	public ItemStack deserialize(ItemStack stack, String data) {
		return stack;
	}
}
