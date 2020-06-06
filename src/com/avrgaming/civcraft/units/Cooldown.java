package com.avrgaming.civcraft.units;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

public class Cooldown {

	String id = "";
	Player player;
	Resident resedint;
	ItemStack stack;
	final Integer cooldown;
	int slot = -1;
	Long beginTime;
	Integer time;
	CooldownFinisher finisher;

	public Cooldown(Player player, ItemStack stack, int cooldown, CooldownFinisher finisher) {
		this.id = Integer.toString(CivCraft.civRandom.nextInt(Integer.MAX_VALUE));
		this.stack = ItemManager.setProperty(stack, "cooldown", id.toString());
		this.cooldown = cooldown;
		this.finisher = finisher;
		this.time = 0;

		this.player = player;
		resedint = CivGlobal.getResident(player);
		slot = player.getInventory().getHeldItemSlot();
		if (!player.getInventory().getItem(slot).equals(stack)) {
			slot = -1;
			for (int i = 0; i <= 40; i++) {
				ItemStack is = player.getInventory().getItem(i);
				if (is == null) continue;
				if (is.equals(stack)) {
					slot = i;
					break;
				}
			}
		}
		if (slot == -1) return;

		beginTime = System.currentTimeMillis();
		time = cooldown;

//		setItemCount(time);
	}

	private void setItemCount(int count) {
		if (count > 120) count = count / 60;
		if (count < 1 || count >= cooldown) count = 1;
		stack.setAmount(count);
		player.getInventory().setItem(slot, stack);
	}

	public void processItem() {
		Long now = System.currentTimeMillis();
		time = cooldown - (int) ((now - beginTime) / 1000);
		setItemCount(time);
	}

	public void finish() {
		finisher.finishCooldown(player, stack);
	}

	public Integer getTime() {
		return time;
	}

	public static void startCooldown(Player player, ItemStack stack, int cooldown) {
		startCooldown(player, stack, cooldown, null);
	}

	public static void startCooldown(Player player, ItemStack stack, int cooldown, CooldownFinisher finisher) {
		Cooldown newCooldown = new Cooldown(player, stack, cooldown, finisher);
		CooldownSynckTask.addCooldown(newCooldown.id, newCooldown);
	}

	public static boolean isCooldown(ItemStack is) {
		return CooldownSynckTask.cooldowns.containsKey(ItemManager.getProperty(is, "cooldown"));
	}

	public static Cooldown getCooldown(ItemStack is) {
		return CooldownSynckTask.cooldowns.get(ItemManager.getProperty(is, "cooldown"));
	}

}