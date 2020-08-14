/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import pvptimer.PvPTimer;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructvalidation.StructureValidationChecker;
import com.avrgaming.civcraft.construct.constructvalidation.StructureValidationPunisher;
import com.avrgaming.civcraft.construct.farm.FarmGrowthSyncTask;
import com.avrgaming.civcraft.construct.farm.FarmPreCachePopulateTimer;
import com.avrgaming.civcraft.construct.structures.Farm;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.endgame.EndConditionNotificationTask;
import com.avrgaming.civcraft.event.EventTimerTask;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.listener.SimpleListener;
import com.avrgaming.civcraft.mythicmob.SyncMobSpawnTimer;
import com.avrgaming.civcraft.mythicmob.MobStatic;
import com.avrgaming.civcraft.randomevents.RandomEventSweeper;
import com.avrgaming.civcraft.sessiondb.SessionDBAsyncTimer;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.*;
import com.avrgaming.civcraft.threading.tasks.ArrowProjectileTask;
import com.avrgaming.civcraft.threading.tasks.ProjectileComponentTimer;
import com.avrgaming.civcraft.threading.timers.*;
import com.avrgaming.civcraft.units.CooldownSynckTask;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.global.scores.CalculateScoreTimer;
import com.avrgaming.global.scores.GlobalTickEvent;
//import com.avrgaming.sls.SLSManager;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public final class CivCraft extends JavaPlugin {

	private static JavaPlugin plugin;
	public static boolean isDisable = false;
	public static Random civRandom = new Random();
	public static double minDamage = 0.2; //FIXME Сделать константой в файле civ.yml
	public static World mainWorld;
	public static World cavesWorld;

	@Override
	public void onEnable() {
		CivCraft.plugin = this;
		civRandom.setSeed(Calendar.getInstance().getTimeInMillis());

		this.saveDefaultConfig();

		CivLog.init(this);
		BukkitObjects.initialize(this);
		CivCraft.mainWorld = Bukkit.getServer().getWorld("world");
		CivCraft.cavesWorld = Bukkit.getServer().getWorld("caves");

		// Load World Populators
		// BukkitObjects.getWorlds().get(0).getPopulators().add(new TradeGoodPopulator());

		try {
			CivSettings.init(this);

			SQL.initialize();
			if (CivGlobal.isHaveTestFlag("clear_table_data")) {
				SQL.clearTableData();
			}
			SQL.initCivObjectTables();

			CivGlobal.loadGlobals();

//			try { //FIXME ENABLE FOR ONLINE
//				SLSManager.init();
//			} catch (CivException e1) {
//				e1.printStackTrace();
//			} catch (InvalidConfiguration e1) {
//				e1.printStackTrace();
//			}

		} catch (InvalidConfiguration | SQLException | IOException | InvalidConfigurationException | CivException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}

		Commander.initCommands();
		SimpleListener.registerAll();
		startTimers();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		isDisable = true;
		SQLUpdate.save();
		try {
			SQL.getGameConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			SQL.getGlobalConnection().close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		TaskMaster.stopAll();
		MobStatic.despawnAll();
		// HandlerList.unregisterAll(this);
	}

	private void startTimers() {

		TaskMaster.asyncTask("SQLUpdate", new SQLUpdate(), 0);
		// Sync Timers
		TaskMaster.syncTimer(SyncBuildUpdateTask.class.getName(), new SyncBuildUpdateTask(), 0, 1);
		TaskMaster.syncTimer(SyncUpdateChunks.class.getName(), new SyncUpdateChunks(), 0, 1);
		TaskMaster.syncTimer(SyncLoadChunk.class.getName(), new SyncLoadChunk(), 0, 1);
		TaskMaster.syncTimer(SyncGetChestInventory.class.getName(), new SyncGetChestInventory(), 0, 1);
		TaskMaster.syncTimer(SyncUpdateInventory.class.getName(), new SyncUpdateInventory(), 0, 1);
		TaskMaster.syncTimer(SyncGrowTask.class.getName(), new SyncGrowTask(), 0, 1);
		TaskMaster.syncTimer(SyncMobSpawnTimer.class.getName(), new SyncMobSpawnTimer(), 0, 1);
		
		TaskMaster.syncTimer(PlayerLocationCacheUpdate.class.getName(), new PlayerLocationCacheUpdate(), 0, 10);
		TaskMaster.asyncTimer(RandomEventSweeper.class.getName(), new RandomEventSweeper(), 0, TimeTools.toTicks(10));
		// Structure event timers
		TaskMaster.asyncTimer(UpdateSecondTimer.class.getName(), new UpdateSecondTimer(), TimeTools.toTicks(1));
		TaskMaster.asyncTimer(UpdateCivtickTimer.class.getName(), new UpdateCivtickTimer(), TimeTools.civtickMinecraftTick);
		TaskMaster.asyncTimer(RegenTimer.class.getName(), new RegenTimer(), TimeTools.toTicks(5));
		try {
			double arrow_firerate = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.fire_rate");
			TaskMaster.syncTimer("arrowTower", new ProjectileComponentTimer(), (int) (arrow_firerate * 20));
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		TaskMaster.syncTimer("arrowhomingtask", new ArrowProjectileTask(), 5);
		// Global Event timers
		TaskMaster.syncTimer("FarmCropCache", new FarmPreCachePopulateTimer(), TimeTools.toTicks(30));
		TaskMaster.asyncTimer("FarmGrowthTimer", new FarmGrowthSyncTask(), TimeTools.toTicks(Farm.GROW_RATE));
		TaskMaster.asyncTimer("announcer", new AnnouncementTimer("tips.txt", 5), 0, TimeTools.toTicks(60 * 60));
		TaskMaster.asyncTimer("announcerwar", new AnnouncementTimer("war.txt", 60), 0, TimeTools.toTicks(60 * 60));
		TaskMaster.asyncTimer("ChangeGovernmentTimer", new ChangeGovernmentTimer(), TimeTools.toTicks(60));
		TaskMaster.asyncTimer("CalculateScoreTimer", new CalculateScoreTimer(), 0, TimeTools.toTicks(60));
		TaskMaster.asyncTimer(PlayerProximityComponentTimer.class.getName(), new PlayerProximityComponentTimer(), TimeTools.toTicks(1));
		TaskMaster.asyncTimer(EventTimerTask.class.getName(), new EventTimerTask(), TimeTools.toTicks(5));
		TaskMaster.asyncTimer("EndGameNotification", new EndConditionNotificationTask(), TimeTools.toTicks(3600));
		TaskMaster.asyncTask(StructureValidationChecker.class.getName(), new StructureValidationChecker(), TimeTools.toTicks(120));
		TaskMaster.asyncTimer("StructureValidationPunisher", new StructureValidationPunisher(), TimeTools.toTicks(3600));
		TaskMaster.asyncTimer("SessionDBAsyncTimer", new SessionDBAsyncTimer(), 10);
		TaskMaster.asyncTimer("pvptimer", new PvPTimer(), TimeTools.toTicks(30));
		TaskMaster.syncTimer(CooldownSynckTask.class.getName(), new CooldownSynckTask(), 20);
		MobStatic.startMobSpawnTimer();
		// TODO from furnex
		TaskMaster.asyncTimer("GlobalTickEvent", new GlobalTickEvent(), 0L, TimeTools.toTicks(30L));
		// XXX Типа во время войны устанавливает бар прореса сверху. Сейчас не доделанный
		// TaskMaster.asyncTimer("ChangePlayerTime", new ChangePlayerTime(), TimeTools.toTicks(1L));
	}

	public boolean hasPlugin(String name) {
		Plugin p;
		p = getServer().getPluginManager().getPlugin(name);
		return (p != null);
	}

	public static JavaPlugin getPlugin() {
		return plugin;
	}

	public static WorldEditPlugin getWorldEdit() {
		Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

		// WorldGuard may not be loaded
		if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
			return null; // Maybe you want throw an exception instead
		}

		return (WorldEditPlugin) plugin;
	}
}
