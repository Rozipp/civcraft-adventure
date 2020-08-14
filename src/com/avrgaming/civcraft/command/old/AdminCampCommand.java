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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.CampNameTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;

public class AdminCampCommand extends MenuAbstractCommand {

	public AdminCampCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_camp_name");

		add(new CustomCommand("destroy").withDescription(CivSettings.localize.localizedString("adcmd_camp_destroyDesc")).withTabCompleter(new CampNameTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Camp camp = Commander.getNamedCamp(args, 0);
				camp.destroy();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_camp_destroyedSuccess"));
			}
		}));
		add(new CustomCommand("setraidtime").withDescription(CivSettings.localize.localizedString("adcmd_camp_setRaidTimeDesck")).withTabCompleter(new CampNameTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Camp camp = Commander.getNamedCamp(args, 0);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_camp_setRaidTimeInvlidInput"));
				String dateStr = args[1];
				SimpleDateFormat parser = new SimpleDateFormat("d:M:y:H:m");
				Date next;
				try {
					next = parser.parse(dateStr);
					camp.setNextRaidDate(next);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_camp_setRaidTimeSuccess"));
				} catch (ParseException e) {
					throw new CivException(CivSettings.localize.localizedString("var_adcmd_camp_setRaidTimeFailedFormat", args[2]));
				}
			}
		}));
		add(new CustomCommand("rebuild").withDescription(CivSettings.localize.localizedString("adcmd_camp_rebuildDesc")).withTabCompleter(new CampNameTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Camp camp = Commander.getNamedCamp(args, 0);
				camp.repairFromTemplate();
				camp.processCommandSigns();
				CivMessage.send(sender, CivSettings.localize.localizedString("Repaired"));
			}
		}));
	}

}
