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
package com.avrgaming.civcraft.command.civ;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;

public class CivGroupCommand extends CommandBase {

	@Override
	public void init() {
		command = "/civ group";
		displayName = CivSettings.localize.localizedString("cmd_civ_group_name");

		cs.add("addleared", "al", "addl", CivSettings.localize.localizedString("cmd_civ_group_addLeaderDesc"));
		cs.add("addadviser", "aa", "adda", CivSettings.localize.localizedString("cmd_civ_group_addAdviserDesc"));
		cs.add("removeleared", "rl", "dell", CivSettings.localize.localizedString("cmd_civ_group_removeLeaderDesc"));
		cs.add("removeadviser", "ra", "dela", CivSettings.localize.localizedString("cmd_civ_group_removeAdviserDesc"));
		cs.add("renameleaders", CivSettings.localize.localizedString("cmd_civ_group_renameLeaderDesc"));
		cs.add("renameadvisers", CivSettings.localize.localizedString("cmd_civ_group_renameAdviserDesc"));
		cs.add("info", "i", CivSettings.localize.localizedString("cmd_civ_group_infoDesc"));
	}

	public void renameadvisers_cmd() throws CivException, InvalidNameException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		String newName = getNamedString(1, "Введите новое имя групы");

		if (!civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_removeOnlyLeader"));
		if (civ.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " + newName);

		String oldName = civ.GM.advisersGroupName;
		civ.GM.renameProtectedGroup(oldName, newName);
		CivMessage.sendCiv(resident.getCiv(), "Група цивилизации " + oldName + " переименована на " + newName);
	}

	public void renameleaders_cmd() throws CivException, InvalidNameException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		String newName = getNamedString(1, "Введите новое имя групы");

		if (!civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_removeOnlyLeader"));
		if (civ.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " + newName);
		
		String oldName = civ.GM.leaderGroupName;
		civ.GM.renameProtectedGroup(oldName, newName);
		CivMessage.sendCiv(resident.getCiv(), "Група цивилизации " + oldName + " переименована на " + newName);
	}

	public void removedviser_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		Resident oldMember = getNamedResident(1);

		if (!civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_removeOnlyLeader"));
		if (!civ.GM.isAdviser(oldMember)) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_group_removeNotInGroup", oldMember.getName()));

		civ.GM.addAdviser(oldMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_removeSuccess", oldMember.getName(), civ.GM.advisersGroupName));
		CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_civ_group_removeNotify1", civ.GM.advisersGroupName, civ.getName()));
	}

	public void removeleader_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		Resident oldMember = getNamedResident(1);

		if (!civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_removeOnlyLeader"));
		if (!civ.GM.isLeader(oldMember)) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_group_removeNotInGroup", oldMember.getName()));
		if (resident == oldMember) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_removeYourself"));

		civ.GM.addLeader(oldMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_removeSuccess", oldMember.getName(), civ.GM.leaderGroupName));
		CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_civ_group_removeNotify1", civ.GM.leaderGroupName, civ.getName()));
	}

	public void addadviser_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		Resident newMember = getNamedResident(1);

		if (!civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_addOnlyLeader"));
		if (newMember.getCiv() != civ) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_addNotInCiv"));

		civ.GM.addAdviser(newMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_addSuccess", newMember.getName(), civ.GM.advisersGroupName));
		CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_civ_group_addNotify", civ.GM.advisersGroupName, civ.getName()));
	}

	public void addleader_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		Resident newMember = getNamedResident(1);

		if (!civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_addOnlyLeader"));
		if (newMember.getCiv() != civ) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_addNotInCiv"));

		civ.GM.addLeader(newMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_addSuccess", newMember.getName(), civ.GM.leaderGroupName));
		CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_civ_group_addNotify", civ.GM.leaderGroupName, civ.getName()));
	}

	public void info_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		String groupName = getNamedString(1, "Enter group name");

		if (args.length > 1) {
			PermissionGroup grp = null;
			if (groupName.equalsIgnoreCase(civ.GM.leaderGroupName)) {
				grp = civ.GM.leaderGroup;
			} else
				if (groupName.equalsIgnoreCase(civ.GM.advisersGroupName)) {
					grp = civ.GM.adviserGroup;
				} else {
					throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_group_removeInvalid", args[1]));
				}

			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_group_listGroup") + " " + args[1]);

			String residents = "";
			for (Resident res : grp.getMemberList()) {
				residents += res.getName() + " ";
			}
			CivMessage.send(sender, residents);

		} else {
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_group_listHeading"));

			PermissionGroup grp = civ.GM.leaderGroup;
			CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_group_listGroup", grp.getName() + CivColor.LightGray, grp.getMemberCount()));

			grp = civ.GM.adviserGroup;
			CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_group_listGroup", grp.getName() + CivColor.LightGray, grp.getMemberCount()));
		}
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
		this.validLeaderAdvisor();
	}

}
