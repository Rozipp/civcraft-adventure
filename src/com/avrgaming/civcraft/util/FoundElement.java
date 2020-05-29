package com.avrgaming.civcraft.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class FoundElement { // найденые в MultiInventory предметы
	public Inventory sInv;
	public Integer slot;
	public ItemStack stack;
	public Integer count;
	public boolean foolStack = false;

	public FoundElement() {

	}

	public FoundElement(Inventory sInv, Integer slot, ItemStack stack, Integer count) {
		this.sInv = sInv;
		this.slot = slot;
		this.stack = stack;
		this.count = count;
	}

	@Override
	public boolean equals(Object object) {
		FoundElement other = (FoundElement) object;
		return this.slot == other.slot && this.sInv.equals(other.sInv);
	}
}
