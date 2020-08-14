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
package com.avrgaming.civcraft.command.old;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.CampNameTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class AdminResCommand extends MenuAbstractCommand {

	public AdminResCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_res_Name");
		add(new CustomCommand("settown").withDescription(CivSettings.localize.localizedString("adcmd_res_setTownDesc")).withTabCompleter(new ResidentInWorldTaber()).withTabCompleter(new TownInWorldTaber())
				.withExecutor(new CustomExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Resident resident = Commander.getNamedResident(args, 0);
						Town town = Commander.getNamedTown(args, 1);
						if (resident.hasTown()) resident.getTown().removeResident(resident);
						try {
							town.addResident(resident);
						} catch (AlreadyRegisteredException e) {
							e.printStackTrace();
							throw new CivException(CivSettings.localize.localizedString("adcmd_res_settownErrorInTown"));
						}
						town.save();
						resident.save();
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_res_setTownSuccess", resident.getName(), town.getName()));
					}
				}));
		add(new CustomCommand("setcamp").withDescription(CivSettings.localize.localizedString("adcmd_res_setcampDesc")).withTabCompleter(new ResidentInWorldTaber()).withTabCompleter(new CampNameTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getNamedResident(args, 0);
				Camp camp = Commander.getNamedCamp(args, 1);
				if (resident.hasCamp()) resident.getCamp().removeMember(resident);
				camp.addMember(resident);
				camp.save();
				resident.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_res_setcampSuccess", resident.getName(), camp.getName()));
			}
		}));
		add(new CustomCommand("cleartown").withDescription(CivSettings.localize.localizedString("adcmd_res_clearTownDesc")).withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getNamedResident(args, 0);
				if (resident.hasTown()) resident.getTown().removeResident(resident);
				resident.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_res_cleartownSuccess", resident.getName()));
			}
		}));
		add(new CustomCommand("rename").withDescription(CivSettings.localize.localizedString("adcmd_res_renameDesc")).withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getNamedResident(args, 0);
				String newName = Commander.getNamedString(args, 1, CivSettings.localize.localizedString("adcmd_res_renamePrompt"));
				Resident newResident = CivGlobal.getResident(newName);
				if (newResident != null) throw new CivException(CivSettings.localize.localizedString("var_adcmd_res_renameExists", newResident.getName(), resident.getName()));
				try {/* Create a dummy resident to make sure name is valid. */
					new Resident(null, newName);
				} catch (InvalidNameException e1) {
					throw new CivException(CivSettings.localize.localizedString("adcmd_res_renameInvalid"));
				}
				try {/* Delete the old resident object. */
					resident.delete();
				} catch (SQLException e) {
					e.printStackTrace();
					throw new CivException(e.getMessage());
				}
				CivGlobal.removeResident(resident);/* Remove resident from CivGlobal tables. */
				try {/* Change the resident's name. */
					resident.setName(newName);
				} catch (InvalidNameException e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("internalCommandException") + " " + e.getMessage());
				}
				CivGlobal.addResident(resident); /* Resave resident to DB and global tables. */
				resident.save();
				CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_res_renameSuccess"));
			}
		}));
	}
}
