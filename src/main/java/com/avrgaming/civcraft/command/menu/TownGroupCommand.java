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
package com.avrgaming.civcraft.command.menu;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.GroupInTown;
import com.avrgaming.civcraft.command.taber.ResidentInTownTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;

public class TownGroupCommand extends MenuAbstractCommand {

	public TownGroupCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_town_group_name");
		this.addValidator(Validators.validMayorAssistantLeader);
		// add(new CustomCommand("addmayor").withDescription("[player] Добавить мера"));
		// add(new CustomCommand("addassistant").withDescription("[player] Добавить ассистента мера"));
		// add(new CustomCommand("adddefault").withDescription("[player] Добавить в стандартную групу"));
		// add(new CustomCommand("removemayor").withDescription("[player] Удалить мера"));
		// add(new CustomCommand("removeassistant").withDescription("[player] Удалить ассистента мера"));
		// add(new CustomCommand("removedefault").withDescription("[player] Удалить игрока из стандартной групы"));
		// add(new CustomCommand("renamemayor").withDescription("[new Name] переименовать групу меров"));
		// add(new CustomCommand("renameassistant").withDescription("[new Name] переименовать групу ассистентов меров"));
		// add(new CustomCommand("renamedefault").withDescription("[new Name] переименовать стандарнтую групу"));

		add(new CustomCommand("new").withDescription(CivSettings.localize.localizedString("cmd_town_group_newDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				String arg = Commander.getNamedString(args, 0, CivSettings.localize.localizedString("cmd_town_group_newPrompt"));
				Town town = Commander.getSelectedTown(sender);
				if (town.GM.hasGroup(arg)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " + arg);
				if (town.GM.isProtectedGroup(arg)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newProtected"));
				try {
					town.GM.newGroup(arg);
				} catch (InvalidNameException e) {
					throw new CivException(e.getMessage());
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_newSuccess", arg));
			}
		}));
		add(new CustomCommand("rename").withDescription("[OldGroupName][NewGroupName] Переименовать групу").withTabCompleter(new GroupInTown()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				PermissionGroup grp = Commander.getNamedPermissionGroup(town, args, 0);
				String newName = Commander.getNamedString(args, 1, "Введите новое имя групе");
				town.GM.renameGroup(grp, newName);
				CivMessage.sendSuccess(sender, "Група " + args[0] + " переименована на " + args[1]);
			}
		}));
		add(new CustomCommand("add").withDescription(CivSettings.localize.localizedString("cmd_town_group_addDesc")).withTabCompleter(new ResidentInTownTaber()).withTabCompleter(new GroupInTown()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Resident senderResident = Commander.getResident(sender);
				Resident resident = Commander.getNamedResident(args, 0);
				PermissionGroup grp = Commander.getNamedPermissionGroup(town, args, 1);
				// if (town.GM.isProtectedGroup(grp)) throw new CivException("Для защищенных груп используйте другую команду");
				town.GM.addToGroup(grp, resident);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", resident.getName(), grp.getName(), town.getName()));
				if (resident != senderResident) CivMessage.sendSuccess(resident, CivSettings.localize.localizedString("var_cmd_town_group_addAlert", grp.getName(), grp.getTown().getName()));
			}
		}));
		add(new CustomCommand("remove").withDescription(CivSettings.localize.localizedString("cmd_town_group_removeDesc")).withTabCompleter(new ResidentInTownTaber()).withTabCompleter(new GroupInTown()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Resident senderResident = Commander.getResident(sender);
				Resident resident = Commander.getNamedResident(args, 0);
				PermissionGroup grp = Commander.getNamedPermissionGroup(town, args, 1);
				// if (town.GM.isProtectedGroup(grp)) throw new CivException("Для защищенных груп используйте другую команду");
				town.GM.removeFromGroup(grp, resident);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", resident.getName(), grp.getName(), town.getName()));
				if (resident != senderResident) CivMessage.send(resident, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", grp.getName(), grp.getTown().getName()));
			}
		}));
		add(new CustomCommand("delete").withAliases("del").withDescription(CivSettings.localize.localizedString("cmd_town_group_deleteDesc")).withTabCompleter(new GroupInTown()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				PermissionGroup grp = Commander.getNamedPermissionGroup(town, args, 0);
				if (town.GM.isProtectedGroup(grp)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_deleteProtected"));
				if (grp.getMemberCount() > 0) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_deleteNotEmpty"));
				town.GM.removeGroup(grp);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_group_deleteSuccess") + " " + args[0]);
			}
		}));
		add(new CustomCommand("info").withAliases("i").withDescription(CivSettings.localize.localizedString("cmd_town_group_infoDesc")).withTabCompleter(new GroupInTown()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (args.length >= 1) {
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
		}));
	}

	// public void renamedefault_cmd() throws CivException, InvalidNameException {
	// Town town = getSelectedTown();
	// Resident resident = getResident();
	// String newName = getNamedString(1, "Введите новое имя групы");
	//
	// if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
	// if (town.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " +
	// newName);
	//
	// String oldName = town.GM.defaultGroupName;
	// town.GM.renameProtectedGroup(oldName, newName);
	// CivMessage.sendCiv(resident.getCiv(), "Група города " + oldName + " переименована на " + newName);
	// }
	//
	// public void renameassistant_cmd() throws CivException, InvalidNameException {
	// Town town = getSelectedTown();
	// Resident resident = getResident();
	// String newName = getNamedString(1, "Введите новое имя групы");
	//
	// if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
	// if (town.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " +
	// newName);
	//
	// String oldName = town.GM.assistantGroupName;
	// town.GM.renameProtectedGroup(oldName, newName);
	// CivMessage.sendCiv(resident.getCiv(), "Група города " + oldName + " переименована на " + newName);
	// }
	//
	// public void renamemayor_cmd() throws CivException, InvalidNameException {
	// Town town = getSelectedTown();
	// Resident resident = getResident();
	// String newName = getNamedString(1, "Введите новое имя групы");
	//
	// if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
	// if (town.GM.getGroup(newName) != null) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_newExists") + " " +
	// newName);
	//
	// String oldName = town.GM.mayorGroupName;
	// town.GM.renameProtectedGroup(oldName, newName);
	// CivMessage.sendCiv(resident.getCiv(), "Група города " + oldName + " переименована на " + newName);
	// }
	//
	// public void removedefault_cmd() throws CivException {
	// Town town = getSelectedTown();
	// Resident commandSenderResident = getResident();
	// Resident oldMember = getNamedResident(1);
	//
	// if (!town.GM.isMayor(commandSenderResident)) throw new
	// CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
	//
	// town.GM.removeDefault(oldMember);
	//
	// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", oldMember.getName(),
	// town.GM.defaultGroupName, town.getName()));
	// if (oldMember != commandSenderResident) CivMessage.send(oldMember, CivColor.Rose +
	// CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", town.GM.defaultGroupName, town.getName()));
	// }
	//
	// public void removeassistant_cmd() throws CivException {
	// Town town = getSelectedTown();
	// Resident commandSenderResident = getResident();
	// Resident oldMember = getNamedResident(1);
	//
	// if (!town.GM.isMayor(commandSenderResident)) throw new
	// CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
	//
	// town.GM.removeAssistant(oldMember);
	//
	// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", oldMember.getName(),
	// town.GM.assistantGroupName, town.getName()));
	// if (oldMember != commandSenderResident) CivMessage.send(oldMember, CivColor.Rose +
	// CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", town.GM.assistantGroupName, town.getName()));
	// }
	//
	// public void removemayor_cmd() throws CivException {
	// Town town = getSelectedTown();
	// Resident commandSenderResident = getResident();
	// Resident oldMember = getNamedResident(1);
	//
	// if (!town.GM.isMayor(commandSenderResident)) throw new
	// CivException(CivSettings.localize.localizedString("cmd_town_group_removeOnlyMayor"));
	// if (town.GM.isOneMayor(oldMember)) throw new CivException(CivSettings.localize.localizedString("cmd_town_group_removeOneMayor"));
	//
	// town.GM.removeMayor(oldMember);
	//
	// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_removeSuccess1", oldMember.getName(),
	// town.GM.mayorGroupName, town.getName()));
	// if (oldMember != commandSenderResident) CivMessage.send(oldMember, CivColor.Rose +
	// CivSettings.localize.localizedString("var_cmd_town_group_removeAlert", town.GM.mayorGroupName, town.getName()));
	// }
	//
	// public void adddefault_cmd() throws CivException {
	// Town town = getSelectedTown();
	// Resident commandSenderResident = getResident();
	// Resident newMember = getNamedResident(1);
	//
	// if (!town.getCiv().GM.isLeader(commandSenderResident) && !town.GM.isMayor(commandSenderResident)) throw new
	// CivException(CivSettings.localize.localizedString("cmd_town_group_addOnlyMayor"));
	// if (!newMember.hasTown()) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addNotInTown",
	// newMember.getName()));
	// if (!newMember.getCiv().equals(town.getCiv())) throw new
	// CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError3", newMember.getName(), newMember.getCiv().getName(),
	// town.getCiv().getName()));
	//
	// town.GM.addDefault(newMember);
	//
	// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", newMember.getName(),
	// town.GM.defaultGroupName, town.getName()));
	// if (newMember != commandSenderResident) CivMessage.sendSuccess(newMember,
	// CivSettings.localize.localizedString("var_cmd_town_group_addAlert", town.GM.defaultGroupName, town.getName()));
	// }
	//
	// public void addassistant_cmd() throws CivException {
	// Town town = getSelectedTown();
	// Resident commandSenderResident = getResident();
	// Resident newMember = getNamedResident(1);
	//
	// if (!town.getCiv().GM.isLeader(commandSenderResident) && !town.GM.isMayor(commandSenderResident)) throw new
	// CivException(CivSettings.localize.localizedString("cmd_town_group_addOnlyMayor"));
	// if (!newMember.hasTown()) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addNotInTown",
	// newMember.getName()));
	// if (!newMember.getCiv().equals(town.getCiv())) throw new
	// CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError3", newMember.getName(), newMember.getCiv().getName(),
	// town.getCiv().getName()));
	//
	// town.GM.addAssistant(newMember);
	//
	// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", newMember.getName(),
	// town.GM.assistantGroupName, town.getName()));
	// if (newMember != commandSenderResident) CivMessage.sendSuccess(newMember,
	// CivSettings.localize.localizedString("var_cmd_town_group_addAlert", town.GM.assistantGroupName, town.getName()));
	// }
	//
	// public void addmayor_cmd() throws CivException {
	// Town town = getSelectedTown();
	// Resident commandSenderResident = getResident();
	// Resident newMember = getNamedResident(1);
	//
	// if (!town.getCiv().GM.isLeader(commandSenderResident) && !town.GM.isMayor(commandSenderResident)) throw new
	// CivException(CivSettings.localize.localizedString("cmd_town_group_addOnlyMayor"));
	// if (!newMember.hasTown()) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addNotInTown",
	// newMember.getName()));
	// if (!town.hasResident(newMember)) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError1",
	// newMember.getName(), newMember.getTown().getName(), town.getName()));
	// if (newMember.getCiv().equals(town.getCiv())) throw new CivException(CivSettings.localize.localizedString("var_cmd_town_group_addError3",
	// newMember.getName(), newMember.getCiv().getName(), town.getCiv().getName()));
	//
	// town.GM.addMayor(newMember);
	//
	// CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_town_group_addSuccess1", newMember.getName(),
	// town.GM.mayorGroupName, town.getName()));
	// if (newMember != commandSenderResident) CivMessage.sendSuccess(newMember,
	// CivSettings.localize.localizedString("var_cmd_town_group_addAlert", town.GM.mayorGroupName, town.getName()));
	// }
}
