package com.avrgaming.civcraft.loreenhancements;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class LoreEnhancementSpeed extends LoreEnhancement {

	public LoreEnhancementSpeed() {
		this.variables.put("amount", "0.02"); //на сколько будет повышаться значение при увеличении на 1 уровень
	}

	public String getLoreString(double baseLevel) {
		return CivColor.Blue + "+" + baseLevel * 100 + "% " + CivSettings.localize.localizedString("itemLore_Speed_Bonus");
	}

	@Override
	public String serialize(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		return attrs.getEnhancementData("LoreEnhancementSpeed", "value");
	}

	@Override
	public ItemStack deserialize(ItemStack stack, String data) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setEnhancementData("LoreEnhancementSpeed", "value", data);
		attrs.setName(attrs.getName() + CivColor.LightBlue + "(+" + Double.valueOf(data) + ")");
		return attrs.getStack();
	}
}
