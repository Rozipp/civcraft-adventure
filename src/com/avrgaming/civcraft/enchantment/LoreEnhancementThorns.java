package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementThorns extends LoreEnhancement {

	public LoreEnhancementThorns() {
		this.variables.put("amount", "0.1"); //на сколько будет повышаться значение при увеличении на 1 уровень
	}

	public String getLoreString(double baseLevel) {
		return CivColor.Blue + "отдача " + baseLevel * 100 + "% ";//CivSettings.localize.localizedString("itemLore_Speed_Bonus");
	}

}
