package com.avrgaming.civcraft.command.debug;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.construct.CampHourlyTick;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.CultureProcessAsyncTask;
import com.avrgaming.civcraft.threading.timers.DailyTimer;
import com.avrgaming.civcraft.threading.timers.EffectEventTimer;
import com.avrgaming.civcraft.threading.timers.SyncTradeTimer;

public class DebugRuntimerCommand extends CommandBase{

	@Override
	public void init() {
		command = "/dbg runtimer";
		displayName = "Run timer Commands";

		cs.add("newday", "DailyTimer");
		cs.add("effect", "EffectEventTimer");
		cs.add("synctrade", "SyncTradeTimer");
		cs.add("camp", "CampHourlyTick");
		cs.add("culture", "CultureProcessAsyncTask");
	}

	public void culture_cmd() {
		CivMessage.send(sender, "Starting a CultureProcessAsyncTask");
		TaskMaster.asyncTask("cultureProcess", new CultureProcessAsyncTask(), 0);
	}
	
	public void camp_cmd() {
		CivMessage.send(sender, "Starting a CampHourlyTick");
		TaskMaster.syncTask(new CampHourlyTick(), 0);
	}
	
	public void synctrade_cmd() {
		CivMessage.send(sender, "Starting a SyncTradeTimer");
		TaskMaster.syncTask(new SyncTradeTimer(), 0);
	}
	
	public void effect_cmd() {
		CivMessage.send(sender, "Starting a EffectEventTimer");
		TaskMaster.asyncTask("EffectEventTimer", new EffectEventTimer(), 0);
	}
	
	public void newday_cmd() {
		CivMessage.send(sender, "Starting a new day...");
		TaskMaster.syncTask(new DailyTimer(), 0);
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showBasicHelp();
	}

	@Override
	public void showHelp() {
		showHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
	}

}
