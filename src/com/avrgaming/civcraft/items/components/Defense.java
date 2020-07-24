/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.DefenseEnchantment;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class Defense extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		attrs.addLore(CivColor.Blue + "" + this.getDouble("value") + " " + CivSettings.localize.localizedString("newItemLore_Defense"));
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {

		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {
			CivMessage.send(resident, CivColor.Rose + CivSettings.localize.localizedString("itemLore_Warning") + " - " + CivColor.LightGray + CivSettings.localize.localizedString("itemLore_defenseHalfPower"));
		}
	}

	@Override
	public void onDefense(EntityDamageByEntityEvent event, ItemStack stack) {
		double defValue = this.getDouble("value");

		/* Try to get any extra defense enhancements from this item. */
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat == null) return;

		double extraDef = 0;
		if (Enchantments.hasEnchantment(stack, EnchantmentCustom.Defense)) {
			extraDef += DefenseEnchantment.defensePerLevel * Enchantments.getLevelEnchantment(stack, EnchantmentCustom.Defense);
		}

		defValue += extraDef;
		double damage = event.getDamage();

		if (event.getEntity() instanceof Player) {
			Resident resident = CivGlobal.getResident(((Player) event.getEntity()));
			if (!resident.hasTechForItem(stack)) defValue = defValue / 2;
		}

		damage -= defValue;
		if (damage < CivCraft.minDamage) {
			/* Always do at least 0.5 damage. */
			damage = CivCraft.minDamage;
		}

		event.setDamage(damage);
	}

}
