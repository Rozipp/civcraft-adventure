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

import gpl.AttributeUtil;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveTownName;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class FoundTown extends ItemComponent implements CallbackInterface {

	/* XXXX THIS IS NOT BEING USED RIGHT NOW. SEE SETTLER INSTEAD */
	
	String townName;
	
	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET+CivColor.Gold+"Founds a Town");
		attrUtil.addLore(ChatColor.RESET+CivColor.Rose+CivSettings.localize.localizedString("itemLore_RightClickToUse"));			
	}
	
	public void foundTown(Player player) throws CivException {
		
		Resident resident = CivGlobal.getResident(player);

		if (resident == null || !resident.hasTown()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotRes"));
			return;
		}

		//validateUnitUse(player, event.getItem());

		double minDistance;
		try {
			minDistance = CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance");
		} catch (InvalidConfiguration e) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("internalException"));
			e.printStackTrace();
			return;
		}

		for (Town town : CivGlobal.getTowns()) {
			Townhall townhall = town.getTownHall();
			if (townhall == null) continue;

			double distSqr = townhall.getCenterLocation().distanceSquared(player.getLocation());
			if (distSqr < minDistance * minDistance) {
				DecimalFormat df = new DecimalFormat();
				CivMessage.sendError(player,
						CivSettings.localize.localizedString("var_settler_errorTooClose", town.getName(), df.format(Math.sqrt(distSqr)), minDistance));
				return;
			}
		}

		/* Build a preview for the Capitol structure. */
		CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
		ConfigBuildableInfo info = CivSettings.structures.get("s_townhall");
		try {
			BuildableStatic.buildVerifyStatic(player, info, player.getLocation(), this);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}
	
	public void onInteract(PlayerInteractEvent event) {
		
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) &&
				!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		
		try {
			foundTown(event.getPlayer());
		} catch (CivException e) {
			CivMessage.sendError(event.getPlayer(), e.getMessage());
		}
		
		class SyncTask implements Runnable {
			String name;
				
			public SyncTask(String name) {
				this.name = name;
			}
			
			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(name);
				} catch (CivException e) {
					return;
				}
				player.updateInventory();
			}
		}
		TaskMaster.syncTask(new SyncTask(event.getPlayer().getName()));

	}

	@Override
	public void execute(String playerName) {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		Resident resident = CivGlobal.getResident(playerName);
		resident.desiredTownLocation = player.getLocation();

		CivMessage.sendHeading(player, CivSettings.localize.localizedString("settler_heading"));
		CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("settler_prompt1"));
		CivMessage.send(player, " ");
		CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("settler_prompt2"));
		CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("build_cancel_prompt"));

		resident.setInteractiveMode(new InteractiveTownName());

//		if (!StringUtils.isAlpha(townName) || !StringUtils.isAsciiPrintable(townName)) {
//			CivMessage.send(player, CivColor.Rose + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_town_nameInvalid"));
//			return;
//		}
//
//		townName = townName.replace(" ", "_");
//		townName = townName.replace("\"", "");
//		townName = townName.replace("\'", "");
//
//		resident.desiredTownName = townName;
//		CivMessage.send(player, CivColor.LightGreen
//				+ CivSettings.localize.localizedString("var_interactive_town_confirmName", CivColor.Yellow + resident.desiredTownName + CivColor.LightGreen));
//
//		class SyncTask implements Runnable {
//			Resident resident;
//
//			public SyncTask(Resident resident) {
//				this.resident = resident;
//			}
//
//			@Override
//			public void run() {
//				Player player;
//				try {
//					player = CivGlobal.getPlayer(resident);
//				} catch (CivException e) {
//					return;
//				}
//
//				CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_town_surveyResults"));
//				CivMessage.send(player, TownCommand.survey(player.getLocation()));
//
//				Location capLoc = resident.getCiv().getCapitolTownHallLocation();
//				if (capLoc == null) {
//					CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_town_noCapitol"));
//					resident.clearInteractiveMode();
//					return;
//				}
//
//				CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_town_confirm"));
//
//				resident.setInteractiveMode(new InteractiveConfirmTownCreation());
//			}
//		}
//
//		TaskMaster.syncTask(new SyncTask(resident));
	}	

}
