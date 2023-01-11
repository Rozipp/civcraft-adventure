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
package com.avrgaming.civcraft.command.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.CampHourlyTick;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.CultureProcessAsyncTask;
import com.avrgaming.civcraft.threading.timers.DailyTimer;
import com.avrgaming.civcraft.threading.timers.TownHourlyTick;

public class AdminTimerCommand extends MenuAbstractCommand {

	public AdminTimerCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_timer_name");

		add(new CustomCommand("showalltimers").withDescription("show all the timer information.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, "Timers");
				SimpleDateFormat sdf = CivGlobal.dateFormat;

				CivMessage.send(sender, "Now:" + sdf.format(new Date()));
				for (EventTimer timer : EventTimer.timers.values()) {
					CivMessage.send(sender, timer.getName());
					CivMessage.send(sender, "    next: " + sdf.format(timer.getNext().getTime()));
					if (timer.getLast().getTime().getTime() == 0) {
						CivMessage.send(sender, "    last: never");
					} else {
						CivMessage.send(sender, "    last: " + sdf.format(timer.getLast().getTime()));
					}
				}
			}
		}));
		add(new CustomCommand("set").withDescription(CivSettings.localize.localizedString("adcmd_timer_setDesc")).withTabCompleter(new AbstractTaber() {
			@Override
			public List<String> getTabList(CommandSender sender, String arg) throws CivException {
				List<String> l = new ArrayList<>();
				for (EventTimer timer : EventTimer.timers.values()) {
					String name = timer.getName();
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_timer_setPrompt"));
				String timerName = args[0];
				EventTimer timer = EventTimer.timers.get(timerName);
				if (timer == null) throw new CivException(CivSettings.localize.localizedString("var_adcmd_timer_runInvalid", args[0]));
				String dateStr = args[1];
				SimpleDateFormat parser = new SimpleDateFormat("d:M:y:H:m");
				Calendar next = EventTimer.getCalendarInServerTimeZone();
				try {
					next.setTime(parser.parse(dateStr));
					timer.setNext(next);
					timer.save();
					CivMessage.sendSuccess(sender, "Set timer " + timer.getName() + " to " + parser.format(next.getTime()));
				} catch (ParseException e) {
					throw new CivException(args[2] + CivSettings.localize.localizedString("adcmd_road_setRaidTimeError"));
				}
			}
		}));
		add(new CustomCommand("run").withDescription(CivSettings.localize.localizedString("adcmd_timer_runDesc")).withTabCompleter(new AbstractTaber() {
			@Override
			public List<String> getTabList(CommandSender sender, String arg) throws CivException {
				List<String> l = new ArrayList<>();
				for (EventTimer timer : EventTimer.timers.values()) {
					String name = timer.getName();
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("adcmd_timer_runPrompt"));
				EventTimer timer = EventTimer.timers.get(args[0]);
				if (timer == null) throw new CivException(CivSettings.localize.localizedString("var_adcmd_timer_runInvalid", args[0]));
				Calendar next;
				try {
					next = timer.getEventFunction().getNextDate();
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("adcmd_timer_runError"));
				}
				timer.getEventFunction().process();
				timer.setLast(EventTimer.getCalendarInServerTimeZone());
				timer.setNext(next);
				timer.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_timer_runSuccess"));
			}
		}));
		add(new CustomCommand("newday").withDescription("DailyTimer").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, "Starting a new day...");
				TaskMaster.syncTask(new DailyTimer(), 0);
			}
		}));
		add(new CustomCommand("effect").withDescription("EffectEventTimer").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, "Starting a EffectEventTimer");
				TaskMaster.asyncTask("EffectEventTimer", new TownHourlyTick(), 0);
			}
		}));
		add(new CustomCommand("camp").withDescription("CampHourlyTick").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, "Starting a CampHourlyTick");
				TaskMaster.syncTask(new CampHourlyTick(), 0);
			}
		}));
		add(new CustomCommand("culture").withDescription("CultureProcessAsyncTask").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, "Starting a CultureProcessAsyncTask");
				TaskMaster.asyncTask("cultureProcess", new CultureProcessAsyncTask(), 0);
			}
		}));
		add(new CustomCommand("processculture").withDescription("forces a culture reprocess").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivGlobal.processCulture();
				CivMessage.sendSuccess(sender, "Forced process of culture");
			}
		}));
	}

}
