/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import java.util.Random;

import gpl.AttributeUtil;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementPunchout;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class StructureBlockHitEvent implements Runnable {

	/* Called when a structure block is hit, this async task quickly determines if the block hit should take damage during war. */
	String playerName;
	BlockCoord coord;
	ConstructDamageBlock dmgBlock;
	World world;

	public StructureBlockHitEvent(String player, BlockCoord coord, ConstructDamageBlock dmgBlock, World world) {
		this.playerName = player;
		this.coord = coord;
		this.dmgBlock = dmgBlock;
		this.world = world;
	}

	@Override
	public void run() {

		if (playerName == null) {
			return;
		}
		Player player;
		Resident resident;
		try {
			player = CivGlobal.getPlayer(this.playerName);
			resident = CivGlobal.getResident(player);
		} catch (CivException e) {
			return;
		}
		if (dmgBlock.allowDamageNow(player)) {
			/* Do our damage. */
			int damage = 1;
			CustomMaterial material = CustomMaterial.getCustomMaterial(player.getInventory().getItemInMainHand());
			if (material != null) {
				damage = material.onStructureBlockBreak(dmgBlock, damage);
			}

			if (player.getInventory().getItemInMainHand() != null && !player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
				AttributeUtil attrs = new AttributeUtil(player.getInventory().getItemInMainHand());
				for (LoreEnhancement enhance : attrs.getEnhancements()) {
					if (enhance instanceof LoreEnhancementPunchout) damage = ((LoreEnhancementPunchout) enhance).onStructureBlockBreak(dmgBlock, damage);
				}
			}
			int addinationalDamage = 0;
			if (resident.getCiv() != null && resident.getCiv().getCapitol() != null) {
				if (resident.getCiv().getCapitol().getBuffManager().hasBuff("level6_extraCPdmgTown")
						&& (CivGlobal.getNearestStructure(player.getLocation()) instanceof Capitol
								|| CivGlobal.getNearestStructure(player.getLocation()) instanceof Townhall)) {
					addinationalDamage += this.getAddinationalBreak();
				}
				if (resident.getCiv().getCapitol().getBuffManager().hasBuff("level6_extraStrucutreDmgTown")
						&& !(CivGlobal.getNearestStructure(player.getLocation()) instanceof Capitol)
						&& !(CivGlobal.getNearestStructure(player.getLocation()) instanceof Townhall)) {
					addinationalDamage += this.getAddinationalBreak();
				}
			}
			if (damage > 1 && dmgBlock.isDamageable()) {
				CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_StructureBlockHitEvent_punchoutDmg", (damage - 1)));
			}
			if (addinationalDamage != 0) {
				CivMessage.send(player, "§a" + CivSettings.localize.localizedString("var_StructureBlockHitEvent_talentDmg", "§2" + addinationalDamage + "§a",
						"§2" + CivSettings.localize.localizedString("Damage")));
				damage += addinationalDamage;
			}
			dmgBlock.getOwner().onDamage(damage, world, player, dmgBlock.getCoord(), dmgBlock);
		} else {
			CivMessage.sendErrorNoRepeat(player,
					CivSettings.localize.localizedString("var_StructureBlockHitEvent_Invulnerable", dmgBlock.getOwner().getDisplayName()));
		}
	}
	public int getAddinationalBreak() {
		final Random rand = CivCraft.civRandom;
		int damage = 0;
		if (rand.nextInt(100) <= 50) {
			damage += rand.nextInt(2);
		}
		return damage;
	}
}