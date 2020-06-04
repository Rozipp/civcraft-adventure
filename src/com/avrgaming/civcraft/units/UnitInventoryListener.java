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
package com.avrgaming.civcraft.units;

import java.sql.Date;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.ItemManager;

import gpl.AttributeUtil;

public class UnitInventoryListener implements Listener {

	public static HashSet<Inventory> guiInventories = new HashSet<>();

	public static ItemStack buildGuiUnit(UnitObject uo) {
		Material mat = Material.BEDROCK;
		ItemStack stack = new ItemStack(mat);
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setCivCraftProperty("GUIU", "" + uo.getId());
		attrs.setName(uo.getName() + " Уровень " + uo.getLevel());
		attrs.setLore("");

		if (uo.getLastResident() != null) {
			attrs.addLore("Взял игрок " + uo.getLastResident().getName());
			attrs.addLore("Последний раз использован ");
			attrs.addLore(CivGlobal.dateFormat.format(new Date(uo.getLastActivate())));
		} else attrs.addLore("Юнит безследно изчез");

		return attrs.getStack();
	}

	public static Inventory createInventory(InventoryHolder invHolder, String title) {
		Inventory inv = Bukkit.createInventory(invHolder, invHolder.getInventory().getType(), title);
		UnitInventoryListener.guiInventories.add(inv);
		return inv;
	}

	public static boolean isGUIItem(ItemStack stack) {
		String title = ItemManager.getProperty(stack, "GUIU");
		return title != null;
	}

	public static boolean isGUIInventory(Inventory inv) {
		return guiInventories.contains(inv);
	}

	/* First phase of inventory click that cancels any event that was clicked on a gui item. */
	@EventHandler(priority = EventPriority.HIGH)
	public void OnInventoryClick(InventoryClickEvent event) {
		if (!isGUIInventory(event.getView().getTopInventory())) return;
		if (event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD || event.getAction() == InventoryAction.HOTBAR_SWAP) {
			event.setCancelled(true);
			return;
		}
		ItemStack current = event.getCurrentItem();
		ItemStack cursor = event.getCursor();
		if (isGUIItem(current) || isGUIItem(cursor)) {
			event.setCancelled(true);
		}

		CustomMaterial cmCurrent = CustomMaterial.getCustomMaterial(current);
		CustomMaterial cmCursor = CustomMaterial.getCustomMaterial(cursor);
		boolean isCurrent = (cmCurrent != null) && (cmCurrent instanceof UnitMaterial);
		boolean isCursor = (cmCursor != null) && (cmCursor instanceof UnitMaterial);

		if (event.isShiftClick()) {
			if (!isCurrent) {
				event.setCancelled(true);
				return;
			}
			return;
		}

		if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
			if (cursor != null && cursor.getType() != Material.AIR && !isCursor && isCurrent) {
				event.setCancelled(true);
				return;
			}
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void OnInventoryDragEvent(InventoryDragEvent event) {
		if (event.isCancelled()) return;
		for (int slot : event.getRawSlots()) {
			if (slot < event.getView().getTopInventory().getSize()) {
				if (isGUIInventory(event.getView().getTopInventory())) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void OnInventoryCloseEvent(InventoryCloseEvent event) {
		if (guiInventories.contains(event.getView().getTopInventory())) {
			for (ItemStack is : event.getInventory()) {
				if (is == null) continue;
				UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(is));
				if (uo != null) {
					uo.setLastResident(null);
					uo.setLastActivate(0);
				}
			}
		}
	}

}
