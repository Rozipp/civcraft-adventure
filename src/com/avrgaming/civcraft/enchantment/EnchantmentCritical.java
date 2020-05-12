package com.avrgaming.civcraft.loreenhancements;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class LoreEnhancementCritical extends LoreEnhancement {

	public LoreEnhancementCritical() {
		this.variables.put("amount", "0.1");
	}
	@Override
	public String getLoreString(Double baseLevel) {
		return CivColor.Blue + "шанс двойного урона " + baseLevel.intValue() * 100 + "%";
	}

	@Override
	public double getLevel(AttributeUtil attrs) {
		if (attrs.hasEnhancement("LoreEnhancementCritical")) {
			/* Get base Level. */
			return Double.valueOf(attrs.getEnhancementData("LoreEnhancementCritical", "level"));
		}
		return 0;
	}

	@Override
	public boolean canEnchantItem(ItemStack item) {
		return isWeapon(item);
	}

	public static boolean randomCriticalAttack(AttributeUtil attrs) {
		String value = attrs.getEnhancementData("LoreEnhancementCritical", "value");
		double d = (value != null && value != "") ? Double.valueOf(value) : 0;
		return CivCraft.civRandom.nextDouble() < d;
	}

	@Override
	public String serialize(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		return attrs.getEnhancementData("LoreEnhancementCritical", "level");
	}

	@Override
	public ItemStack deserialize(ItemStack stack, String data) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setEnhancementData("LoreEnhancementCritical", "level", data);
		attrs.setName(attrs.getName() + CivColor.LightBlue + "(+" + Double.valueOf(data) + ")");
		return attrs.getStack();
	}
}