/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.units;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.FoundTownCallback;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;

import gpl.AttributeUtil;

public class Settler extends UnitMaterial {

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

		UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(event.getItem()));
		if (!uo.validateUnitUse(event.getPlayer())) return;

		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
		try {
			new FoundTownCallback(event.getPlayer());
		} catch (CivException e) {
			CivMessage.sendError(event.getPlayer(), e.getMessage());
		}

		event.getPlayer().updateInventory();
	}
}
