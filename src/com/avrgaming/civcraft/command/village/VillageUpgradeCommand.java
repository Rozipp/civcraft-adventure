/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command.village;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigVillageUpgrade;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.village.Village;

public class VillageUpgradeCommand extends CommandBase {
	@Override
	public void init() {
		command = "/village upgrade";
		displayName = CivSettings.localize.localizedString("cmd_village_upgrade_name");
		
		
		cs.add("list", CivSettings.localize.localizedString("cmd_village_upgrade_listDesc"));
		cs.add("purchased", CivSettings.localize.localizedString("cmd_village_upgrade_purchasedDesc"));
		cs.add("buy", CivSettings.localize.localizedString("cmd_village_upgrade_buyDesc"));
		
	}

	public void purchased_cmd() throws CivException {
		Village village = this.getCurrentVillage();
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_village_upgrade_purchasedSuccess"));

		String out = "";
		for (ConfigVillageUpgrade upgrade : village.getUpgrades()) {
			out += upgrade.name+", ";
		}
		
		CivMessage.send(sender, out);
	}
	
	private void list_upgrades(Village village) throws CivException {				
		for (ConfigVillageUpgrade upgrade : CivSettings.villageUpgrades.values()) {
			if (upgrade.isAvailable(village)) {
				CivMessage.send(sender, upgrade.name+" "+CivColor.LightGray+CivSettings.localize.localizedString("Cost")+" "+CivColor.Yellow+upgrade.cost);
			}
		}
	}
	
	public void list_cmd() throws CivException {
		Village village = this.getCurrentVillage();

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_village_upgrade_list"));	
		list_upgrades(village);		
	}
	
	public void buy_cmd() throws CivException {
		Village village = this.getCurrentVillage();

		if (args.length < 2) {
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_village_upgrade_list"));
			list_upgrades(village);		
			CivMessage.send(sender, CivSettings.localize.localizedString("cmd_village_upgrade_buyHeading"));
			return;
		}
				
		String combinedArgs = "";
		args = this.stripArgs(args, 1);
		for (String arg : args) {
			combinedArgs += arg + " ";
		}
		combinedArgs = combinedArgs.trim();
		
		ConfigVillageUpgrade upgrade = CivSettings.getVillageUpgradeByNameRegex(village, combinedArgs);
		if (upgrade == null) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_village_upgrade_buyInvalid",combinedArgs));
		}
		
		if (village.hasUpgrade(upgrade.id)) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_upgrade_buyOwned"));
		}
		
		village.purchaseUpgrade(upgrade);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_village_upgrade_buySuccess",upgrade.name));
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {	
		this.validVillageOwner();
	}
}
