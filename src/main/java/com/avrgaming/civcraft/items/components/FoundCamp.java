/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.items.components;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.FoundCampCallback;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.gpl.AttributeUtil;

public class FoundCamp extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET + CivColor.Gold + CivSettings.localize.localizedString("buildcamp_lore1"));
		attrUtil.addLore(ChatColor.RESET + CivColor.Rose + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
		Enchantments.addEnchantment(attrUtil, EnchantmentCustom.SoulBound, 1);
		attrUtil.addLore(CivColor.Gold + CivSettings.localize.localizedString("itemLore_Soulbound"));
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) 
			return;
		try {
			Player player = event.getPlayer();
			CivGlobal.getResident(player).setPendingCallback(new FoundCampCallback(player));
		} catch (CivException e) {
			CivMessage.sendError(event.getPlayer(), e.getMessage());
		}
		event.getPlayer().updateInventory();
		
	}

}