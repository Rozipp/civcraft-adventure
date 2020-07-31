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
package com.avrgaming.civcraft.command.newcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.CivLeaderQuestionTask;
import com.avrgaming.civcraft.questions.PlayerQuestionTask;
import com.avrgaming.civcraft.questions.Question;

public class DenyCommand extends CustomCommand {

	public DenyCommand() {
		super("deny");
		this.setAliases("no");
		this.setExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (!(sender instanceof Player)) throw new CivException(CivSettings.localize.localizedString("cmd_MustBePlayer"));
				Player player = (Player) sender;
				PlayerQuestionTask task = (PlayerQuestionTask) Question.getQuestionTask(player.getName());
				if (task != null) {
					/* We have a question, and the answer was "Accepted" so notify the task. */
					synchronized (task) {
						task.setResponse("deny");
						task.notifyAll();
					}
					return;
				}
				Resident resident = CivGlobal.getResident(player);
				if (resident.getCiv().GM.isLeader(resident)) {
					CivLeaderQuestionTask civTask = (CivLeaderQuestionTask) Question.getQuestionTask("civ:" + resident.getCiv().getName());
					if (civTask != null) {
						synchronized (civTask) {
							civTask.setResponse("deny");
							civTask.setResponder(resident);
							civTask.notifyAll();
						}
					}
					return;
				}
				throw new CivException(CivSettings.localize.localizedString("cmd_acceptError"));
			}
		});
	}
}
