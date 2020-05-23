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
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.construct.ChoiseTemplate;
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.interactive.InteractiveGetName;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import gpl.AttributeUtil;

public class FoundCamp extends ItemComponent implements CallbackInterface {

	private Player player;
	private Resident resident;
	private Camp camp;

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET + CivColor.Gold + CivSettings.localize.localizedString("buildcamp_lore1"));
		attrUtil.addLore(ChatColor.RESET + CivColor.Rose + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
		attrUtil = Enchantments.addEnchantment(attrUtil, CustomEnchantment.SoulBound, 1);
		attrUtil.addLore(CivColor.Gold + CivSettings.localize.localizedString("itemLore_Soulbound"));
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		campName = null;
		templateTheme = null;
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		player = event.getPlayer();
		resident = CivGlobal.getResident(player);
		try {
			camp = Camp.newCamp(player, player.getLocation());
			new ChoiseTemplate(player, camp, this);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

	private String templateTheme = null;
	private String campName = null;

	@Override
	public void execute(String... strings) {
		if (templateTheme == null) {
			templateTheme = strings[0];
			CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildcamp_Heading"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("buildcamp_prompt1"));
			CivMessage.send(player, " ");
			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("buildcamp_prompt2"));
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("build_cancel_prompt"));

			InteractiveGetName interactive = new InteractiveGetName();
			interactive.cancelMessage = CivSettings.localize.localizedString("interactive_camp_cancel");
			interactive.invalidMessage = CivSettings.localize.localizedString("interactive_camp_invalid");
			interactive.undoPreviewCancel = true;
			resident.setInteractiveMode(interactive);
			return;
		}

		if (campName == null) {
			campName = strings[0];
			try {
				if (CivGlobal.getCamp(campName) != null) throw new InvalidNameException("(" + campName + ") " + CivSettings.localize.localizedString("camp_nameTaken"));
				camp.setName(campName);
			} catch (InvalidNameException e) {
				CivMessage.sendError(player, e.getMessage());
				campName = null;
				return;
			}

			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(player.getInventory().getItemInMainHand());
			if (craftMat == null || !craftMat.hasComponent("FoundCamp")) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("camp_missingItem"));
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}
			resident.clearInteractiveMode();
			resident.undoPreview();
			try {
				camp.createCamp(player);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			return;
		}
	}
}
