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
package com.avrgaming.civcraft.comm.Resident;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.comm.AbstractSubCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class Resident_toggle extends AbstractSubCommand {

	public Resident_toggle() {
		super("resident toggle");
		displayName = CivSettings.localize.localizedString("cmd_res_toggle_name");

		add("map", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_mapDesc"));
		add("info", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_infoDesc"));
		add("showtown", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_showtownDesc"));
		add("showciv", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_showcivDesc"));
		add("showscout", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_showscoutDesc"));
		add("combatinfo", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_combatinfoDesc"));
		add("itemdrops", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_itemdropsDesc"));
		add("titles", null, new String[] {}, new ArgType[] {}, CivSettings.localize.localizedString("cmd_res_toggle_titleAPIDesc"));
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		return null;
	}

	@Override
	public void onComm(CommandSender arg0, Command arg1, String arg2, String[] arg3) throws CivException {
		toggle();
	}

	private void toggle() throws CivException {
		Resident resident = getResident();

		boolean result;
		switch (args[0].toLowerCase()) {
		case "map":
			resident.setShowMap(!resident.isShowMap());
			result = resident.isShowMap();
			break;
		case "showtown":
			resident.setShowTown(!resident.isShowTown());
			result = resident.isShowTown();
			break;
		case "showciv":
			resident.setShowCiv(!resident.isShowCiv());
			result = resident.isShowCiv();
			break;
		case "showscout":
			resident.setShowScout(!resident.isShowScout());
			result = resident.isShowScout();
			break;
		case "info":
			resident.setShowInfo(!resident.isShowInfo());
			result = resident.isShowInfo();
			break;
		case "combatinfo":
			resident.setCombatInfo(!resident.isCombatInfo());
			result = resident.isCombatInfo();
			break;
		case "itemdrops":
			resident.toggleItemMode();
			return;
		default:
			throw new CivException(CivSettings.localize.localizedString("cmd_unkownFlag") + " " + args[0]);
		}

		resident.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_toggled") + " " + args[0] + " -> " + result);
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
