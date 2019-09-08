/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.debug;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.test.TestGetChestThread;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.timers.LagSimulationTimer;
import com.avrgaming.civcraft.util.ItemManager;

public class DebugTestCommand extends CommandBase {

	/* Here we'll build a collection of integration tests that can be run on server start to verify everything is working. */

	@Override
	public void init() {
		command = "/dbg test ";
		displayName = "Test Commands";

		cs.add("getsyncchesttest", "Does a performance test by getting chests. NEVER RUN THIS ON PRODUCTION.");
		cs.add("setlag", "[tps] - tries to set the tps to this amount to simulate lag.");
		cs.add("getitem", "[umid] - give umid material.");
	}

	public void getitem_cmd() throws CivException {
		Player player = getPlayer();
		String umid = getNamedString(1, "введите umid предмета");
		ItemStack stack = ItemManager.createItemStack(umid, 1);
		if (stack == null) {
			CivMessage.sendSuccess(sender, "Item not found");
			return;
		}
		player.getInventory().addItem(stack);
		CivMessage.sendSuccess(sender, "Item added");
	}

	public void setlag_cmd() throws CivException {
		Integer tps = getNamedInteger(1);
		TaskMaster.syncTimer("lagtimer", new LagSimulationTimer(tps), 0);
		CivMessage.sendSuccess(sender, "Let the lagging begin.");
	}

	public void getsyncchesttest_cmd() throws CivException {
		Integer count = getNamedInteger(1);

		for (int i = 0; i < count; i++) {
			TaskMaster.asyncTask(new TestGetChestThread(), 0);
		}

		CivMessage.sendSuccess(sender, "Started " + count + " threads, watch logs.");
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
//		if (!getPlayer().getName().equalsIgnoreCase("netizen539")) {
//			throw new CivException("You must be netizen to run these commands.");
//		}
	}

}
