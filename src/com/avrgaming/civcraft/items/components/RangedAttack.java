package com.avrgaming.civcraft.items.components;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.EnchantmentAttack;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class RangedAttack extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(CivColor.Rose + this.getDouble("value") + " " + CivSettings.localize.localizedString("itemLore_RangedAttack"));
	}

	private static double ARROW_MAX_VEL = 6.0;

	@Override
	public void onInteract(PlayerInteractEvent event) {
		if (UnitStatic.isWearingAnyMetal(event.getPlayer())) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemLore_RangedAttack_errorMetal"));
			return;
		}
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {

		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {
			CivMessage.send(resident, CivColor.Rose + CivSettings.localize.localizedString("itemLore_Warning") + " - " + CivColor.LightGray + CivSettings.localize.localizedString("itemLore_attackHalfDamage"));
		}
	}

	public void onRangedAttack(EntityDamageByEntityEvent event, ItemStack inHand) {
		if (event.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) event.getDamager();
			if (arrow.getShooter() instanceof Player) {
				Player attacker = (Player) arrow.getShooter();
				if (UnitStatic.isWearingAnyMetal(attacker)) {
					event.setCancelled(true);
					CivMessage.sendError(attacker, CivSettings.localize.localizedString("itemLore_RangedAttack_errorMetal"));
					return;
				}
			}
		}
		double dmg = this.getDouble("value");
		double extraAtt = 0.0;
		if (Enchantments.hasEnchantment(inHand, CustomEnchantment.Attack)) dmg += EnchantmentAttack.onAttack(Enchantments.getLevelEnchantment(inHand, CustomEnchantment.Attack));

		Vector vel = event.getDamager().getVelocity();
		double magnitudeSquared = Math.pow(vel.getX(), 2) + Math.pow(vel.getY(), 2) + Math.pow(vel.getZ(), 2);
		double percentage = magnitudeSquared / ARROW_MAX_VEL;
		if (percentage > 1) percentage = 1;
		dmg = percentage * dmg;

		if (event.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) event.getDamager();
			if (arrow.getShooter() instanceof Player) {
				Resident resident = CivGlobal.getResident(((Player) arrow.getShooter()));
				if (!resident.hasTechForItem(inHand)) {
					dmg = dmg / 2;
				}
			}
		}
		
		dmg += extraAtt;
		if (dmg < CivCraft.minDamage) dmg = CivCraft.minDamage;
		event.setDamage(dmg);
	}
}
