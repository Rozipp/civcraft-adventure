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
package com.avrgaming.civcraft.command.town;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.structures.Bank;
import com.avrgaming.civcraft.construct.structures.Blacksmith;
import com.avrgaming.civcraft.construct.structures.Grocer;
import com.avrgaming.civcraft.construct.structures.Library;
import com.avrgaming.civcraft.construct.structures.ScoutTower;
import com.avrgaming.civcraft.construct.structures.Store;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;

public class TownSetCommand extends CommandBase {

	@Override
	public void init() {
		command = "/town set";
		displayName = CivSettings.localize.localizedString("cmd_town_set_name");
		
		cs.add("bankfee", CivSettings.localize.localizedString("cmd_town_set_bankfeeDesc"));
		cs.add("storefee", CivSettings.localize.localizedString("cmd_town_set_storefeeDesc"));
		cs.add("grocerfee", CivSettings.localize.localizedString("cmd_town_set_grocerfeeDesc"));
		cs.add("libraryfee", CivSettings.localize.localizedString("cmd_town_set_libraryfeeDesc"));
		cs.add("blacksmithfee", CivSettings.localize.localizedString("cmd_town_set_blacksmithfeeDesc"));
//		cs.add("stablefee", CivSettings.localize.localizedString("cmd_town_set_stablefeeDesc"));
		
		cs.add("scoutrate", CivSettings.localize.localizedString("cmd_town_set_scoutrateDesc"));
		
	}
	
//	public void stablefee_cmd() throws CivException {
//		Town town = getSelectedTown();
//		Integer feeInt = getNamedInteger(1);
//		
//		Structure struct = town.findStructureByConfigId("s_stable");
//		if (struct == null) {
//			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_stablefeeNone"));
//		}
//		
//		Stable stable = (Stable)struct;
//		
//		if (feeInt < Stable.FEE_MIN || feeInt > Stable.FEE_MAX) {
//			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_stablefeeRates"));
//		}
//	
//		stable.setNonResidentFee(((double)feeInt/100));
//		stable.updateSignText();
//		
//		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess",feeInt));
//	}
	
	public void scoutrate_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer rate = getNamedInteger(1);
		
		if (rate != 10 && rate != 30 && rate != 60) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_scoutrateRates"));
		}
		
		for (Structure struct : town.BM.getStructures()) {
			if (struct instanceof ScoutTower) {
				((ScoutTower)struct).setReportSeconds(rate);
			} 
//			else if (struct instanceof ScoutShip) {
//				((ScoutShip)struct).setReportSeconds(rate);
//			}
		}
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_scoutrateSuccess",rate));
	}
	
	public void blacksmithfee_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer feeInt = getNamedInteger(1);
		
		if (feeInt < 5 || feeInt > 15) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
		}
		
		Structure struct = town.BM.getFirstStructureById("s_blacksmith");
		if (struct == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_blacksmithfeeNone"));
		}

		((Blacksmith)struct).setNonResidentFee(((double)feeInt/100));
		((Blacksmith)struct).updateSignText();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess",feeInt));
	}
	
	
	public void libraryfee_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer feeInt = getNamedInteger(1);
		
		if (feeInt < 5 || feeInt > 15) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
		}
		
		Structure struct = town.BM.getFirstStructureById("s_library");
		if (struct == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_libraryfeeNone"));
		}

		((Library)struct).setNonResidentFee(((double)feeInt/100));
		((Library)struct).updateSignText();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess",feeInt));
	}
	
	public void grocerfee_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer feeInt = getNamedInteger(1);
		
		if (feeInt < 5 || feeInt > 15) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
		}
		
		Structure struct = town.BM.getFirstStructureById("s_grocer");
		if (struct == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_grocerfeeNone"));
		}

		((Grocer)struct).setNonResidentFee(((double)feeInt/100));
		((Grocer)struct).updateSignText();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess",feeInt));
		
	}
	
	public void storefee_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer feeInt = getNamedInteger(1);
		
		if (feeInt < 5 || feeInt > 15) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
		}
		
		Structure struct = town.BM.getFirstStructureById("s_store");
		if (struct == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_storefeeNone"));
		}
		
		((Store)struct).setNonResidentFee(((double)feeInt/100));
		((Store)struct).updateSignText();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess",feeInt));
		
	}
	
	public void bankfee_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer feeInt = getNamedInteger(1);
		
		if (feeInt < 5 || feeInt > 15) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_rate5to15"));
		}
		
		Structure struct = town.BM.getFirstStructureById("s_bank");
		if (struct == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_town_set_bankfeeNone"));
		}
		
		((Bank)struct).setNonResidentFee(((double)feeInt/100));
		((Bank)struct).updateSignText();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_set_feeSuccess",feeInt));
		
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
        final Town town = this.getSelectedTown();
        if (!town.GM.isMayorOrAssistant(this.getResident()) && !town.getCiv().GM.isLeader(this.getResident())) {
            throw new CivException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
        }
    }

}
