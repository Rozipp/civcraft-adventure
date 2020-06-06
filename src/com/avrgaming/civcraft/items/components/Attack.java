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
import gpl.AttributeUtil.Attribute;
import gpl.AttributeUtil.AttributeType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.EnchantmentAttack;
import com.avrgaming.civcraft.enchantment.EnchantmentLevitate;
import com.avrgaming.civcraft.enchantment.EnchantmentPoison;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

@SuppressWarnings("deprecation")
public class Attack extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {

		// Add generic attack damage of 0 to clear the default lore on item.
		attrs.add(Attribute.newBuilder().name("Attack").type(AttributeType.GENERIC_ATTACK_DAMAGE).amount(0).build());
		attrs.addLore(CivColor.Rose + "" + this.getDouble("value") + " " + CivSettings.localize.localizedString("itemLore_Attack"));
		return;
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {

		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {
			CivMessage.send(resident, CivColor.Rose + CivSettings.localize.localizedString("itemLore_Warning") + " - " + CivColor.LightGray + CivSettings.localize.localizedString("itemLore_attackHalfDamage"));
		}
	}

	@Override
	public void onAttack(EntityDamageByEntityEvent event, ItemStack inHand) {
		double dmg = this.getDouble("value");
		double extraAtt = 0.0;
		Resident resident;
		if (Enchantments.hasEnchantment(inHand, CustomEnchantment.Attack)) extraAtt += EnchantmentAttack.onAttack(Enchantments.getLevelEnchantment(inHand, CustomEnchantment.Attack));
		if (Enchantments.hasEnchantment(inHand, CustomEnchantment.Poison)) EnchantmentPoison.onAttack(event);
		if (Enchantments.hasEnchantment(inHand, CustomEnchantment.Levitate)) EnchantmentLevitate.onAttack(event);
		if (Enchantments.hasEnchantment(inHand, CustomEnchantment.LightningStrike)) EnchantmentLevitate.onAttack(event);
		
		dmg = dmg * event.getOriginalDamage(DamageModifier.BASE);
		
		if (event.getDamager() instanceof Player) {
			resident = CivGlobal.getResident(((Player) event.getDamager()));
			if (!resident.hasTechForItem(inHand)) dmg = dmg / 2;
		}
		dmg += extraAtt;

		if (dmg < CivCraft.minDamage) dmg = CivCraft.minDamage;
		event.setDamage(dmg);
	}

}
