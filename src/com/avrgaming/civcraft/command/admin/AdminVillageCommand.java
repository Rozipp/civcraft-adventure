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
package com.avrgaming.civcraft.command.admin;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.village.Village;

public class AdminVillageCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad village";
		displayName = CivSettings.localize.localizedString("adcmd_village_name");	
		
		cs.add("destroy", CivSettings.localize.localizedString("adcmd_village_destroyDesc"));
		cs.add("setraidtime", CivSettings.localize.localizedString("adcmd_village_setRaidTimeDesck"));
		cs.add("rebuild", CivSettings.localize.localizedString("adcmd_village_rebuildDesc"));
	}
	
	public void rebuild_cmd() throws CivException {
		Village village = this.getNamedVillage(1);
		
		try {
			village.repairFromTemplate();
		} catch (IOException e) {
		} catch (CivException e) {
			e.printStackTrace();
		}
		village.reprocessCommandSigns();
		CivMessage.send(sender, CivSettings.localize.localizedString("Repaired"));
	}
	
	public void setraidtime_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		
		if (!resident.hasVillage()) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_village_setRaidTimeNovillage"));
		}
		
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_village_setRaidTimeInvlidInput"));
		}
		
		Village village = resident.getVillage();
		
		String dateStr = args[2];
		SimpleDateFormat parser = new SimpleDateFormat("d:M:y:H:m");
		
		Date next;
		try {
			next = parser.parse(dateStr);
			village.setNextRaidDate(next);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_village_setRaidTimeSuccess"));
		} catch (ParseException e) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_village_setRaidTimeFailedFormat",args[2]));
		}
		
	}
	
	public void destroy_cmd() throws CivException {
		Village village = getNamedVillage(1);		
		village.destroy();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_village_destroyedSuccess"));
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
		
	}

}
