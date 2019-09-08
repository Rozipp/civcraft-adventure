package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementJumping extends LoreEnhancement {
	
	public LoreEnhancementJumping() {
		this.variables.put("amount", "0.1"); //на сколько будет повышаться значение при увеличении на 1 уровень
	}

	@Override
	public String getLoreString(double baseLevel) {
		return CivColor.Blue + "+" + baseLevel * 100 + "% " + CivSettings.localize.localizedString("itemLore_Speed_Bonus");
	}
	
	@Override
	public String getDisplayName() {
		return "Jumping";
	}
}
