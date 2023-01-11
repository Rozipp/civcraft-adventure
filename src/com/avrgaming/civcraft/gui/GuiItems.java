package com.avrgaming.civcraft.gui;

import java.lang.reflect.Constructor;

import gpl.AttributeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.util.ItemManager;

public class GuiItems {
	public static final int INV_ROW_COUNT = 9;

	public static GuiItem newGuiItem(ItemStack stack) {
		return new GuiItem(stack);
	}

	public static GuiItem newGuiItem() {
		return new GuiItem();
	}

	public static ItemStack addGui(ItemStack stack, String name) {
		return ItemManager.setProperty(stack, "GUI", name);
	}
	public static ItemStack addGuiAction(ItemStack stack, String action) {
		return ItemManager.setProperty(stack, "GUI_ACTION", action);
	}

	public static ItemStack addGuiAtributes(ItemStack stack, String name,  String action) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setCivCraftProperty("GUI", name);
		attrs.addLore("GUI:" + name);
		if (action!= null) {
			attrs.setCivCraftProperty("GUI_ACTION", action);
			attrs.addLore("GUI_ACTION:" + action);
		}

		return attrs.getStack();
	}

	public static ItemStack removeGuiAtributes(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.removeCivCraftProperty("GUI");
		attrs.removeCivCraftProperty("GUI_ACTION");
		return attrs.getStack();
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

	public static void processAction(String action, ItemStack stack, Player player) {
		/* Get class name from reflection and perform assigned action */
		try {
			Class<?> clazz = Class.forName("com.avrgaming.civcraft.gui.action." + action);
			Constructor<?> constructor = clazz.getConstructor();
			GuiItemAction instance = (GuiItemAction) constructor.newInstance();
			instance.performAction(player, stack);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
