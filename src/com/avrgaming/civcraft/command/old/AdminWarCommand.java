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

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerKickBan;
import com.avrgaming.civcraft.war.War;

public class AdminWarCommand extends MenuAbstractCommand {

	public AdminWarCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_war_name");

		add(new CustomCommand("start").withDescription(CivSettings.localize.localizedString("adcmd_war_startDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				War.setWarTime(true);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_war_startSuccess"));
			}
		}));
		add(new CustomCommand("stop").withDescription(CivSettings.localize.localizedString("adcmd_war_stopDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				War.setWarTime(false);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_war_stopSuccess"));
			}
		}));
		// add(new CustomCommand("setlastwar").withDescription("takes a date of the form: DAY:MONTH:YEAR:HOUR:MIN (24 hour time)").withExecutor(new
		// CustomExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// if (args.length < 1) throw new CivException("Enter a date like DAY:MONTH:YEAR:HOUR:MIN");
		// String dateStr = args[0];
		// SimpleDateFormat parser = new SimpleDateFormat("d:M:y:H:m");
		// Date lastwar;
		// try {
		// lastwar = parser.parse(dateStr);
		// War.setLastWarTime(lastwar);
		// CivMessage.sendSuccess(sender, "Set last war date");
		// } catch (ParseException e) {
		// throw new CivException("Couldnt parse " + args[1] + " into a date, use format: DAY:MONTH:YEAR:HOUR:MIN");
		// }
		// }
		// }));
		add(new CustomCommand("onlywarriors").withDescription(CivSettings.localize.localizedString("adcmd_war_onlywarriorsDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				War.setOnlyWarriors(!War.isOnlyWarriors());
				if (War.isOnlyWarriors()) {
					for (Player player : Bukkit.getOnlinePlayers()) {
						Resident resident = CivGlobal.getResident(player);
						if (player.isOp() || player.hasPermission(CivSettings.MINI_ADMIN)) {
							CivMessage.send(sender, CivSettings.localize.localizedString("var_adcmd_war_onlywarriorsSkippedAdmin", player.getName()));
							continue;
						}
						if (resident == null || !resident.hasTown() || !resident.getTown().getCiv().getDiplomacyManager().isAtWar()) {
							TaskMaster.syncTask(new PlayerKickBan(player.getName(), true, false, CivSettings.localize.localizedString("adcmd_war_onlywarriorsKickMessage")));
						}
					}
					CivMessage.global(CivSettings.localize.localizedString("adcmd_war_onlywarriorsStart"));
				} else
					CivMessage.global(CivSettings.localize.localizedString("adcmd_war_onlywarriorsEnd"));
			}
		}));
	}
}
