/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.EnchantmentPunchout;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class StructureBlockHitEvent implements Runnable {

	/* Called when a structure block is hit, this async task quickly determines if the block hit should take damage during war. */
	Player player;
	ConstructDamageBlock dmgBlock;

	public StructureBlockHitEvent(Player player, ConstructDamageBlock dmgBlock) {
		this.player = player;
		this.dmgBlock = dmgBlock;
	}

	@Override
	public void run() {
		if (player == null || !player.isOnline()) return;
		
		if (dmgBlock.allowDamageNow(player)) {
			/* Do our damage. */
			int damage = 1;

			ItemStack hand = player.getInventory().getItemInMainHand();
			if (hand != null && hand.getType() != Material.AIR) {
				CustomMaterial material = CustomMaterial.getCustomMaterial(hand);
				if (material != null) damage = material.onStructureBlockBreak(dmgBlock, damage);
				if (Enchantments.hasEnchantment(hand, CustomEnchantment.Punchout)) damage = EnchantmentPunchout.onStructureBlockBreak(dmgBlock, damage);
			}

			if (damage > 1 && dmgBlock.isDamageable()) CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_StructureBlockHitEvent_punchoutDmg", (damage - 1)));
			dmgBlock.getOwner().onDamage(damage, player, dmgBlock);
		} else {
			CivMessage.sendErrorNoRepeat(player, CivSettings.localize.localizedString("var_StructureBlockHitEvent_Invulnerable", dmgBlock.getOwner().getDisplayName()));
		}
	}
}