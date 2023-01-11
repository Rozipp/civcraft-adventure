package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;

public class NotreDame extends Wonder {

	public NotreDame(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void removeBuffs() {
		this.removeBuffFromCiv(this.getCivOwner(), "buff_notre_dame_no_anarchy");
		this.removeBuffFromTown(this.getTownOwner(), "buff_notre_dame_coins_from_peace");
		this.removeBuffFromTown(this.getTownOwner(), "buff_notre_dame_extra_war_penalty");
	}

	@Override
	protected void addBuffs() {
		this.addBuffToCiv(this.getCivOwner(), "buff_notre_dame_no_anarchy");
		this.addBuffToTown(this.getTownOwner(), "buff_notre_dame_coins_from_peace");
		this.addBuffToTown(this.getTownOwner(), "buff_notre_dame_extra_war_penalty");

	}

	public void processPeaceTownCoins() {
		double totalCoins = 0;
		int peacefulTowns = 0;
		double coinsPerTown = this.getTownOwner().getBuffManager().getEffectiveInt("buff_notre_dame_coins_from_peace");
		
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.isAdminCiv()) 			continue;
			if (civ.getDiplomacyManager().isAtWar())			continue;
			peacefulTowns++;
			totalCoins += (coinsPerTown*civ.getTowns().size());
		}
		
		this.getTownOwner().depositTaxed(totalCoins);
		CivMessage.sendTown(this.getTownOwner(), CivSettings.localize.localizedString("var_NotreDame_generatedCoins",totalCoins,CivSettings.CURRENCY_NAME,peacefulTowns));
	}

}
