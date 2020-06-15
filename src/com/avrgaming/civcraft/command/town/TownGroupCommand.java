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
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;

public class TownGroupCommand extends CommandBase {

	@Override
	public void init() {
		command = "/town group";
		displayName = CivSettings.localize.localizedString("cmd_town_group_name");

		cs.add("addmayor", "am", "addm", "[player] Добавить мера");
		cs.add("addassistant", "aa", "adda", "[player] Добавить ассистента мера");
		cs.add("adddefault", "[player] Добавить в стандартную групу");
		cs.add("removemayor", "rm", "delm", "[player] Удалить мера");
		cs.add("removeassistant", "ra", "dela", "[player] Удалить ассистента мера");
		cs.add("removedefault", "[player] Удалить игрока из стандартной групы");
		cs.add("renamemayor", "[new Name] переименовать групу меров");
		cs.add("renameassistant", "[new Name] переименовать групу ассистентов меров");
		cs.add("renamedefault", "[new Name] переименовать стандарнтую групу");

		cs.add("new", CivSettings.localize.localizedString("cmd_town_group_newDesc"));
		cs.add("delete", "del", CivSettings.localize.localizedString("cmd_town_group_deleteDesc"));
		cs.add("add", CivSettings.localize.localizedString("cmd_town_group_addDesc"));
		cs.add("remove", CivSettings.localize.localizedString("cmd_town_group_removeDesc"));
		cs.add("info", "i", CivSettings.localize.localizedString("cmd_town_group_infoDesc"));
	}

	public void info_cmd() throws CivException {
		Town town = getSelectedTown();
		if (args.length >= 2) {
			PermissionGroup grp = town.GM.getGroup(args[1]);
			if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_infoInvalid", town.getName(), args[1]));

			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_town_group_infoHeading") + "(" + town.getName() + "):" + args[1]);

			String residents = "";
			for (Resident res : grp.getMemberList()) {
				residents += res.getName() + " ";
			}
			CivMessage.send(sender, residents);
		} else {
			CivMessage.sendHeading(sender, town.getName() + " " + CivSettings.localize.localizedString("cmd_town_group_infoHeading2"));
			for (PermissionGroup grp : town.GM.getAllGroups()) {
				CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_group_listGroup", grp.getName() + CivColor.LightGray, grp.getMemberCount()));
			}
		}
	}

	public void remove_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident oldMember = getNamedResident(1);
		PermissionGroup grp = getNamedPermissionGroup(town, 2);

		if (town.GM.isProtectedGroup(grp)) throw new CivException("Для защищенных груп используйте другую команду");

		town.GM.removeFromGroup(oldMember, grp);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", oldMember.getName(), grp.getName(), town.getName()));
		if (oldMember != commandSenderResident) CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", grp.getName(), grp.getTown().getName()));
	}

	public void add_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident newMember = getNamedResident(1);
		PermissionGroup grp = this.getNamedPermissionGroup(town, 2);

		if (town.GM.isProtectedGroup(grp)) throw new CivException("Для защищенных груп используйте другую команду");

		town.GM.addToGroup(newMember, grp);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", newMember.getName(), grp.getName(), town.getName()));
		if (newMember != commandSenderResident) CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_town_group_addAlert", grp.getName(), grp.getTown().getName()));
	}

	public void delete_cmd() throws CivException {
		Town town = getSelectedTown();
		PermissionGroup grp = this.getNamedPermissionGroup(town, 1);

		if (town.GM.isProtectedGroup(grp)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_deleteProtected"));
		if (grp.getMemberCount() > 0) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_deleteNotEmpty"));

		town.GM.removeGroup(grp);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_group_deleteSuccess") + " " + args[1]);
	}

	public void new_cmd() throws CivException {
		if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newPrompt"));

		Town town = getSelectedTown();
		if (town.GM.hasGroup(args[1])) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " + args[1]);
		if (town.GM.isProtectedGroupName(args[1])) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newProtected"));

		try {
			town.GM.newGroup(args[1]);
		} catch (InvalidNameException e) {
			throw new CivException(e.getMessage());
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_newSuccess", args[1]));
	}

	public void renamedefault_cmd() throws CivException, InvalidNameException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		String newName = getNamedString(1, "Введите новое имя групы");

		if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
		if (town.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " + newName);

		String oldName = town.GM.defaultGroupName;
		town.GM.renameProtectedGroup(oldName, newName);
		CivMessage.sendCiv(resident.getCiv(), "Група города " + oldName + " переименована на " + newName);
	}

	public void renameassistant_cmd() throws CivException, InvalidNameException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		String newName = getNamedString(1, "Введите новое имя групы");

		if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
		if (town.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " + newName);

		String oldName = town.GM.assistantGroupName;
		town.GM.renameProtectedGroup(oldName, newName);
		CivMessage.sendCiv(resident.getCiv(), "Група города " + oldName + " переименована на " + newName);
	}

	public void renamemayor_cmd() throws CivException, InvalidNameException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		String newName = getNamedString(1, "Введите новое имя групы");

		if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
		if (town.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " + newName);

		String oldName = town.GM.mayorGroupName;
		town.GM.renameProtectedGroup(oldName, newName);
		CivMessage.sendCiv(resident.getCiv(), "Група города " + oldName + " переименована на " + newName);
	}

	public void removedefault_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident oldMember = getNamedResident(1);

		if (!town.GM.isMayor(commandSenderResident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));

		town.GM.removeDefault(oldMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", oldMember.getName(), town.GM.defaultGroupName, town.getName()));
		if (oldMember != commandSenderResident) CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", town.GM.defaultGroupName, town.getName()));
	}

	public void removeassistant_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident oldMember = getNamedResident(1);

		if (!town.GM.isMayor(commandSenderResident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));

		town.GM.removeAssistant(oldMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", oldMember.getName(), town.GM.assistantGroupName, town.getName()));
		if (oldMember != commandSenderResident) CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", town.GM.assistantGroupName, town.getName()));
	}

	public void removemayor_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident oldMember = getNamedResident(1);

		if (!town.GM.isMayor(commandSenderResident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
		if (town.GM.isOneMayor(oldMember)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOneMayor"));

		town.GM.removeMayor(oldMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", oldMember.getName(), town.GM.mayorGroupName, town.getName()));
		if (oldMember != commandSenderResident) CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", town.GM.mayorGroupName, town.getName()));
	}

	public void adddefault_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident newMember = getNamedResident(1);

		if (!town.getCiv().GM.isLeader(commandSenderResident) && !town.GM.isMayor(commandSenderResident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_addOnlyMayor"));
		if (!newMember.hasTown()) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addNotInTown", newMember.getName()));
		if (!newMember.getCiv().equals(town.getCiv())) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError3", newMember.getName(), newMember.getCiv().getName(), town.getCiv().getName()));

		town.GM.addDefault(newMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", newMember.getName(), town.GM.defaultGroupName, town.getName()));
		if (newMember != commandSenderResident) CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_town_group_addAlert", town.GM.defaultGroupName, town.getName()));
	}

	public void addassistant_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident newMember = getNamedResident(1);

		if (!town.getCiv().GM.isLeader(commandSenderResident) && !town.GM.isMayor(commandSenderResident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_addOnlyMayor"));
		if (!newMember.hasTown()) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addNotInTown", newMember.getName()));
		if (!newMember.getCiv().equals(town.getCiv())) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError3", newMember.getName(), newMember.getCiv().getName(), town.getCiv().getName()));

		town.GM.addAssistant(newMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", newMember.getName(), town.GM.assistantGroupName, town.getName()));
		if (newMember != commandSenderResident) CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_town_group_addAlert", town.GM.assistantGroupName, town.getName()));
	}

	public void addmayor_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident newMember = getNamedResident(1);

		if (!town.getCiv().GM.isLeader(commandSenderResident) && !town.GM.isMayor(commandSenderResident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_addOnlyMayor"));
		if (!newMember.hasTown()) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addNotInTown", newMember.getName()));
		if (!town.hasResident(newMember)) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError1", newMember.getName(), newMember.getTown().getName(), town.getName()));
		if (newMember.getCiv().equals(town.getCiv())) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError3", newMember.getName(), newMember.getCiv().getName(), town.getCiv().getName()));

		town.GM.addMayor(newMember);

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", newMember.getName(), town.GM.mayorGroupName, town.getName()));
		if (newMember != commandSenderResident) CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_town_group_addAlert", town.GM.mayorGroupName, town.getName()));
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		this.validMayorAssistantLeader();
		return;
	}

	@Override
	public void doDefaultAction() {
		showHelp();
	}

}
