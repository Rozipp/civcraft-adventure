/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.units;

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

import gpl.AttributeUtil;

public class Settler extends UnitMaterial implements CallbackInterface {

	String townName;

	public Settler(String id, ConfigUnit configUnit) {
		super(id, configUnit);
	}

	@Override
	public void initLore(AttributeUtil attrs, UnitObject uo) {
		// String h = CivColor.RoseBold + uo.getComponent(Equipments.HELMET);
		// String c = CivColor.RoseBold + uo.getComponent(Equipments.CHESTPLATE);
		// String l = CivColor.RoseBold + uo.getComponent(Equipments.LEGGINGS);
		// String b = CivColor.RoseBold + uo.getComponent(Equipments.BOOTS);
		// String s = CivColor.RoseBold + uo.getComponent(Equipments.SWORD);
		// String t = CivColor.RoseBold + uo.getComponent(Equipments.TWOHAND);
		// attrs.addLore(CivColor.RoseBold + " Амуниция:");
		// attrs.addLore(CivColor.Rose + " правая Голова : T" + h + CivColor.Rose + " левая");
		// attrs.addLore(CivColor.Rose + " рука Грудь : T" + c + CivColor.Rose + " рука ");
		// attrs.addLore(CivColor.Rose + " Т" + s + CivColor.Rose + " Ноги : T" + l + CivColor.Rose + " Т" + t);
		// attrs.addLore(CivColor.Rose + " Ступни : T" + b);
	}

	@Override
	public void initUnitObject(UnitObject uo) {
		uo.setEquipment(Equipments.MAINHAND, "mat_stone_sword");
		uo.addComponent("u_foundtown");
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

		try {
			foundTown(event.getPlayer());
		} catch (CivException e) {
			CivMessage.sendError(event.getPlayer(), e.getMessage());
		}

		String name = event.getPlayer().getName();
		TaskMaster.syncTask(new Runnable() {
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
		});
	}

	public void foundTown(Player player) throws CivException {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotRes"));
			return;
		}

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
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_settler_errorTooClose", town.getName(), df.format(Math.sqrt(distSqr)), minDistance));
				return;
			}
		}

		CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
		ConfigBuildableInfo info = CivSettings.structures.get("s_townhall");
		try {
			BuildableStatic.buildVerifyStatic(player, info, player.getLocation(), this);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

	public void onBuildTown(Player player, String townName) {
		this.townName = townName;
		Resident resident = CivGlobal.getResident(player);

		if (resident == null || !resident.hasTown()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotRes"));
			return;
		}

		// validateUnitUse(player, event.getItem());

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
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_settler_errorTooClose", town.getName(), df.format(Math.sqrt(distSqr)), minDistance));
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
	}
}
