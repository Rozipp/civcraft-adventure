package com.avrgaming.civcraft.loreenhancements;

import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementJumping extends LoreEnhancement {
	
	public LoreEnhancementJumping() {
		this.variables.put("amount", "1"); //на сколько будет повышаться значение при увеличении на 1 уровень
	}

	@Override
	public String getLoreString(Double baseLevel) {
		return CivColor.Blue + "Пригучесть уровень " + baseLevel;
	}
	
	@Override
	public String getDisplayName() {
		return "Jumping";
	}
}
