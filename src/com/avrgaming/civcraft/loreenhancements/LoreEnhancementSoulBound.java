package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementSoulBound extends LoreEnhancement {
	
	public AttributeUtil add(AttributeUtil attrs) {
		attrs.addEnhancement("LoreEnhancementSoulBound", null, null);
		attrs.addLore(CivColor.Gold+getDisplayName());
		return attrs;
	}
	
	public boolean canEnchantItem(ItemStack item) {
		return isWeaponOrArmor(item);
	}
	
	public boolean hasEnchantment(ItemStack item) {
		AttributeUtil attrs = new AttributeUtil(item);
		return attrs.hasEnhancement("LoreEnhancementSoulBound");
	}
	
	public String getDisplayName() {
		return CivSettings.localize.localizedString("itemLore_Soulbound");
	}
	
	@Override
	public String serialize(ItemStack stack) {
		return "";
	}

	@Override
	public ItemStack deserialize(ItemStack stack, String data) {
		return stack;
	}
}
