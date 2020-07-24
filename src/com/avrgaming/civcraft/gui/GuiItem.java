package com.avrgaming.civcraft.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.util.ItemManager;

import gpl.AttributeUtil;

public class GuiItem {

	private ItemStack stack = null;
	private ItemStack stackOrigin = null;

	private Integer amount = 1;
	private String title = null;
	private List<String> lore = new ArrayList<>();
	private String action = null;
	private Map<String, String> data = new HashMap<>();
	private boolean dirt = true;

	public GuiItem() {
	}

	public GuiItem(Material mat) {
		this.stackOrigin = ItemManager.createItemStack(mat, amount);
	}

	public GuiItem(ItemStack stack) {
		this.stackOrigin = stack;
	}

	// ------------ builders

	public GuiItem setStack(ItemStack stack) {
		this.stackOrigin = stack;
		return this;
	}

	public GuiItem setMaterial(Material mat) {
		// if (this.stackOrigin == null)
		this.stackOrigin = ItemManager.createItemStack(mat, amount);
		return this;
	}

	public GuiItem setAmount(int amount) {
		stackOrigin.setAmount(amount);
		return this;
	}

	public GuiItem setTitle(String title) {
		this.title = title;
		this.dirt = true;
		return this;
	}

	public GuiItem setLore(String... lore) {
		this.lore.clear();
		return addLore(lore);
	}

	public GuiItem addLore(String... lore) {
		for (String s : lore)
			this.lore.add(s);
		this.dirt = true;
		return this;
	}

	public GuiItem setAction(String action) {
		this.action = action;
		this.dirt = true;
		return this;
	}

	public GuiItem setActionData(String key, String value) {
		this.data.put(key, value);
		this.dirt = true;
		return this;
	}

	/** Добавляет предмету действие (CallbackGui), которое возвращает на GuiInventory.execute(String... strings) строку data */
	public GuiItem setCallbackGui(String data) {
		this.action = "CallbackGui";
		this.data.put("data", data);
		return this;
	}

	public GuiItem setOpenInventory(String className, String arg) {
		this.action = "OpenInventory";
		this.data.put("className", className);
		if (arg != null) this.data.put("arg", arg);
		return this;
	}

	// -------------- getStack

	public ItemStack getStack() {
		if (stack == null || dirt) {
			if (stackOrigin == null || stackOrigin.getType() == Material.AIR) stackOrigin = ItemManager.createItemStack(Material.WOOL, amount);
			stack = stackOrigin.clone();
			dirt = false;
		}
		AttributeUtil attrs = new AttributeUtil(stack);
		if (title == null)
			attrs.setCivCraftProperty("GUI", "" + ItemManager.getTypeId(stack));
		else {
			attrs.setCivCraftProperty("GUI", title);
			attrs.setName(title);
		}
		if (lore != null) attrs.setLore(lore);
		if (action != null) {
			attrs.setCivCraftProperty("GUI_ACTION", action);
//			attrs.addLore("GUI_ACTION " + action); // TODO debag
		}

		if (!data.isEmpty()) {
			for (String key : data.keySet()) {
				attrs.setCivCraftProperty("GUI_ACTION_DATA:" + key, data.get(key));
//				attrs.addLore("GUI_ACTION_DATA:" + key + " " + data.get(key)); // TODO debag
			}
		}
		stack = attrs.getStack();
		Enchantments.addEnchantment(stack, EnchantmentCustom.SoulBound, 1);
		ItemMeta meta = stack.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		stack.setItemMeta(meta);
		
		return stack;
	}
}