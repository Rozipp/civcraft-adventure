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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;

public class CivGroupCommand extends MenuAbstractCommand {

	public CivGroupCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_civ_group_name");
		this.addValidator(Validators.validLeader);

		add(new CustomCommand("addleared").withAliases("al").withDescription(CivSettings.localize.localizedString("cmd_civ_group_addLeaderDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				Resident newMember = Commander.getNamedResident(args, 0);
				if (newMember.getCiv() != civ) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_addNotInCiv"));
				civ.GM.addLeader(newMember);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_addSuccess", newMember.getName(), civ.GM.leadersGroupName));
				CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_civ_group_addNotify", civ.GM.leadersGroupName, civ.getName()));
			}
		}));
		add(new CustomCommand("addadviser").withAliases("aa").withDescription(CivSettings.localize.localizedString("cmd_civ_group_addAdviserDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				Resident newMember = Commander.getNamedResident(args, 0);
				if (newMember.getCiv() != civ) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_addNotInCiv"));
				civ.GM.addAdviser(newMember);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_addSuccess", newMember.getName(), civ.GM.advisersGroupName));
				CivMessage.sendSuccess(newMember, CivSettings.localize.localizedString("var_cmd_civ_group_addNotify", civ.GM.advisersGroupName, civ.getName()));
			}
		}));
		add(new CustomCommand("removeleared").withAliases("rl").withDescription(CivSettings.localize.localizedString("cmd_civ_group_removeLeaderDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				Resident resident = Commander.getResident(sender);
				Resident oldMember = Commander.getNamedResident(args, 0);
				if (resident == oldMember) throw new CivException(CivSettings.localize.localizedString("cmd_civ_group_removeYourself"));
				civ.GM.removeLeader(oldMember);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_removeSuccess", oldMember.getName(), civ.GM.leadersGroupName));
				CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_civ_group_removeNotify1", civ.GM.leadersGroupName, civ.getName()));
			}
		}));
		add(new CustomCommand("removeadviser").withAliases("ra").withDescription(CivSettings.localize.localizedString("cmd_civ_group_removeAdviserDesc")).withValidator(Validators.validLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				Resident oldMember = Commander.getNamedResident(args, 0);
				civ.GM.removeAdviser(oldMember);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_group_removeSuccess", oldMember.getName(), civ.GM.advisersGroupName));
				CivMessage.send(oldMember, CivColor.Rose + CivSettings.localize.localizedString("var_cmd_civ_group_removeNotify1", civ.GM.advisersGroupName, civ.getName()));
			}
		}));
		add(new CustomCommand("renameleaders").withDescription(CivSettings.localize.localizedString("cmd_civ_group_renameLeaderDesc")).withValidator(Validators.validLeader).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				String newName = Commander.getNamedString(args, 0, "Введите новое имя групы");
				civ.GM.renameGroup(civ.GM.leadersGroup, newName);
				CivMessage.sendCiv(Commander.getSenderCiv(sender), "Група лидеров цивилизации переименована на " + newName);
			}
		}));
		add(new CustomCommand("renameadvisers").withDescription(CivSettings.localize.localizedString("cmd_civ_group_renameAdviserDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				String newName = Commander.getNamedString(args, 0, "Введите новое имя групы");
				civ.GM.renameGroup(civ.GM.advisersGroup, newName);
				CivMessage.sendCiv(Commander.getSenderCiv(sender), "Група помощников цивилизации переименована на " + newName);
			}
		}));
		add(new CustomCommand("info").withAliases("i").withDescription(CivSettings.localize.localizedString("cmd_civ_group_infoDesc"))
				.withTabCompleter(new AbstractTaber() {
					@Override
					public List<String> getTabList(CommandSender sender, String arg) throws CivException {
						List<String> l = new ArrayList<>();
						Civilization civ = Commander.getSenderCiv(sender);
						l.add(civ.GM.advisersGroupName);
						l.add(civ.GM.leadersGroupName);
						return l;
					}
				})
				.withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Civilization civ = Commander.getSenderCiv(sender);
				String groupName = Commander.getNamedString(args, 0, "Enter group name");
				if (args.length > 1) {
					PermissionGroup grp = civ.GM.getGroup(groupName);
					if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_group_removeInvalid", args[1]));
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_group_listGroup") + " " + args[1]);
					String residents = "";
					for (Resident res : grp.getMemberList()) {
						residents += res.getName() + " ";
					}
					CivMessage.send(sender, residents);
				} else {
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_civ_group_listHeading"));
					PermissionGroup grp = civ.GM.leadersGroup;
					CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_group_listGroup", grp.getName() + CivColor.LightGray, grp.getMemberCount()));
					grp = civ.GM.advisersGroup;
					CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_civ_group_listGroup", grp.getName() + CivColor.LightGray, grp.getMemberCount()));
				}
			}
		}));
	}
}
