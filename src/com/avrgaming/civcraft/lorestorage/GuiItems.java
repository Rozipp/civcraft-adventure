/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.lorestorage;

import java.lang.reflect.Constructor;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.util.ItemManager;

public class GuiItems {
	public static final int MAX_INV_SIZE = 54;
	public static final int INV_ROW_COUNT = 9;

	public static GuiItem newGuiItem(ItemStack stack) {
		return new GuiItem(stack);
	}

	public static GuiItem newGuiItem() {
		return new GuiItem();
	}

	public static boolean isGUIItem(ItemStack stack) {
		return ItemManager.getProperty(stack, "GUI") != null;
	}

	public static String getAction(ItemStack stack) {
		return ItemManager.getProperty(stack, "GUI_ACTION");
	}

	public static String getActionData(ItemStack stack, String key) {
		return ItemManager.getProperty(stack, "GUI_ACTION_DATA:" + key);
	}

	public static void processAction(String action, ItemStack stack, InventoryClickEvent event) {
		/* Get class name from reflection and perform assigned action */
		try {
			Class<?> clazz = Class.forName("com.avrgaming.civcraft.loregui." + action);
			Constructor<?> constructor = clazz.getConstructor();
			GuiItemAction instance = (GuiItemAction) constructor.newInstance();
			instance.performAction(event, stack);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
