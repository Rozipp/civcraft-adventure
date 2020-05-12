package com.avrgaming.civcraft.units;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.items.components.ItemComponent;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;

public class Cooldown {

	// Integer id = 0;
	Player player;
	Resident resedint;
	final ItemStack stack;
	ItemComponent component;
	final Integer cooldown;
	int slot = -1;
	Long beginTime;
	Integer time;

	public Cooldown(ItemStack is, ItemComponent component, int cooldown) {
		this.stack = is;
		this.component = component;
		this.cooldown = cooldown;
		this.time = 0;
	}

	private void setItemCount(int count) {
		// XXX временно выкл if (resedint.isUnitActive()) {
		if (count > 120) count = count / 60;
		if (count < 1 || count >= cooldown) count = 1;
		stack.setAmount(count);
		player.getInventory().setItem(slot, stack);
		// }
	}

	public void beginCooldown(Player player) {
		this.player = player;
		resedint = CivGlobal.getResident(player);
		slot = -1;
		for (int i = 0; i <= 40; i++) {
			ItemStack is = player.getInventory().getItem(i);
			if (is == null) continue;
			if (is.equals(stack)) {
				slot = i;
				break;
			}
		}
		if (slot == -1) return;

		beginTime = System.currentTimeMillis();
		time = cooldown;

		setItemCount(time);
		CooldownTimerTask.addCooldown(this);
	}

	public void processItem() {
		Long now = System.currentTimeMillis();
		time = cooldown - (int) ((now - beginTime) / 1000);
		setItemCount(time);
	}

	public boolean isCanUse() {
		return true;
	}

	public boolean isRefresh() {
		return time < 1;
	}

	public void finish() {
		component.setAttribute("lock", null);
	}
}