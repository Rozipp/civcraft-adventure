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

import com.avrgaming.donate.Donate;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import pvptimer.PvPListener;
import pvptimer.PvPTimer;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Transmuter;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.endgame.EndConditionNotificationTask;
import com.avrgaming.civcraft.event.EventTimerTask;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.fishing.FishingListener;
import com.avrgaming.civcraft.items.CraftableCustomMaterialListener;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.mythicmob.MobListener;
import com.avrgaming.civcraft.mythicmob.MobPoolSpawnTimer;
import com.avrgaming.civcraft.mythicmob.MobStatic;
import com.avrgaming.civcraft.randomevents.RandomEventSweeper;
import com.avrgaming.civcraft.sessiondb.SessionDBAsyncTimer;
import com.avrgaming.civcraft.structure.Farm;
import com.avrgaming.civcraft.structure.farm.FarmGrowthSyncTask;
import com.avrgaming.civcraft.structure.farm.FarmPreCachePopulateTimer;
import com.avrgaming.civcraft.structurevalidation.StructureValidationChecker;
import com.avrgaming.civcraft.structurevalidation.StructureValidationPunisher;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.*;
import com.avrgaming.civcraft.threading.tasks.ArrowProjectileTask;
import com.avrgaming.civcraft.threading.tasks.ProjectileComponentTimer;
import com.avrgaming.civcraft.threading.timers.*;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.units.UnitInventoryListener;
import com.avrgaming.civcraft.units.UnitListener;
import com.avrgaming.civcraft.units.CooldownTimerTask;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.WarListener;
import com.avrgaming.global.scores.CalculateScoreTimer;
import com.avrgaming.global.scores.GlobalTickEvent;
import com.avrgaming.sls.SLSManager;
import com.avrgaming.civcraft.command.town.*;
import com.avrgaming.civcraft.command.resident.*;
import com.avrgaming.civcraft.command.plot.*;
import com.avrgaming.civcraft.command.civ.*;
import com.avrgaming.civcraft.command.market.*;
import com.avrgaming.civcraft.command.*;
import com.avrgaming.civcraft.command.debug.*;
import com.avrgaming.civcraft.command.admin.*;
import com.avrgaming.civcraft.command.camp.CampCommand;
import com.avrgaming.civcraft.listener.*;
import com.avrgaming.civcraft.listener.armor.ArmorListener;

public final class CivCraft extends JavaPlugin {

	private static JavaPlugin plugin;
	public static boolean isDisable = false;
	public static Random civRandom = new Random();

	@Override
	public void onEnable() {
		CivCraft.plugin = this;
		civRandom.setSeed(Calendar.getInstance().getTimeInMillis());

		this.saveDefaultConfig();

		CivLog.init(this);
		BukkitObjects.initialize(this);

		// Load World Populators
		// BukkitObjects.getWorlds().get(0).getPopulators().add(new TradeGoodPopulator());

		try {
			CivSettings.init(this);

			SQL.initialize();
			if (CivGlobal.isHaveTestFlag("clear_table_data")) {
				SQL.clearTableData();
			}
			SQL.initCivObjectTables();
			ChunkCoord.buildWorldList();

			CivGlobal.loadGlobals();

			try {
				SLSManager.init();
			} catch (CivException e1) {
				e1.printStackTrace();
			} catch (InvalidConfiguration e1) {
				e1.printStackTrace();
			}

		} catch (InvalidConfiguration | SQLException | IOException | InvalidConfigurationException | CivException | ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}

		initCommands();

		registerEvents();

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
		
		Transmuter.stopAllTransmuter();
		TaskMaster.stopAll();
		MobStatic.despawnAll();
//		HandlerList.unregisterAll(this);
	}

	private void initCommands() {
		// Init commands
		getCommand("town").setExecutor(new TownCommand());
		getCommand("resident").setExecutor(new ResidentCommand());
		getCommand("dbg").setExecutor(new DebugCommand());
		getCommand("plot").setExecutor(new PlotCommand());
		getCommand("accept").setExecutor(new AcceptCommand());
		getCommand("deny").setExecutor(new DenyCommand());
		getCommand("civ").setExecutor(new CivCommand());
		getCommand("tc").setExecutor(new TownChatCommand());
		getCommand("cc").setExecutor(new CivChatCommand());
		// getCommand("gc").setExecutor(new GlobalChatCommand());
		getCommand("ad").setExecutor(new AdminCommand());
		getCommand("econ").setExecutor(new EconCommand());
		getCommand("pay").setExecutor(new PayCommand());
		getCommand("build").setExecutor(new BuildCommand());
		getCommand("market").setExecutor(new MarketCommand());
		getCommand("select").setExecutor(new SelectCommand());
		getCommand("here").setExecutor(new HereCommand());
		getCommand("camp").setExecutor(new CampCommand());
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("trade").setExecutor(new TradeCommand());
		getCommand("kill").setExecutor(new KillCommand());
		getCommand("enderchest").setExecutor(new EnderChestCommand());
		getCommand("map").setExecutor(new MapCommand());
		getCommand("wiki").setExecutor(new WikiCommand());
		getCommand("vcc").setExecutor(new CampChatCommand());
		getCommand("donate").setExecutor(new Donate());
	}

	private void startTimers() {

		TaskMaster.asyncTask("SQLUpdate", new SQLUpdate(), 0);
		// Sync Timers
		TaskMaster.syncTimer(SyncBuildUpdateTask.class.getName(), new SyncBuildUpdateTask(), 0, 1);
		TaskMaster.syncTimer(SyncUpdateChunks.class.getName(), new SyncUpdateChunks(), 0, TimeTools.toTicks(1));
		TaskMaster.syncTimer(SyncLoadChunk.class.getName(), new SyncLoadChunk(), 0, 1);
		TaskMaster.syncTimer(SyncGetChestInventory.class.getName(), new SyncGetChestInventory(), 0, 1);
		TaskMaster.syncTimer(SyncUpdateInventory.class.getName(), new SyncUpdateInventory(), 0, 1);
		TaskMaster.syncTimer(SyncGrowTask.class.getName(), new SyncGrowTask(), 0, 1);
		TaskMaster.syncTimer(PlayerLocationCacheUpdate.class.getName(), new PlayerLocationCacheUpdate(), 0, 10);
		TaskMaster.asyncTimer("RandomEventSweeper", new RandomEventSweeper(), 0, TimeTools.toTicks(10));
		// Structure event timers
		TaskMaster.asyncTimer("UpdateEventTimer", new UpdateSecondTimer(), TimeTools.toTicks(1));
		TaskMaster.asyncTimer("UpdateMinuteEventTimer", new UpdateMinuteEventTimer(), TimeTools.toTicks(20));
		TaskMaster.asyncTimer("RegenTimer", new RegenTimer(), TimeTools.toTicks(5));
		TaskMaster.asyncTimer("BeakerTimer", new BeakerTimer(60), TimeTools.toTicks(60));
		TaskMaster.syncTimer("UnitTrainTimer", new UnitTrainTimer(), TimeTools.toTicks(1));
		TaskMaster.syncTimer("UnitMaterialColdownTimer", new CooldownTimerTask(), 20);
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
		TaskMaster.asyncTask(new StructureValidationChecker(), TimeTools.toTicks(120));
		TaskMaster.asyncTimer("StructureValidationPunisher", new StructureValidationPunisher(), TimeTools.toTicks(3600));
		TaskMaster.asyncTimer("SessionDBAsyncTimer", new SessionDBAsyncTimer(), 10);
		TaskMaster.asyncTimer("pvptimer", new PvPTimer(), TimeTools.toTicks(30));

		MobStatic.startMobSpawnTimer();
		TaskMaster.syncTimer("MobPoolSpawner", new MobPoolSpawnTimer(), TimeTools.toTicks(1));
		// TODO from furnex
		TaskMaster.asyncTimer("GlobalTickEvent", new GlobalTickEvent(), 0L, TimeTools.toTicks(30L));
		//XXX Типа во время войны устанавливает бар прореса сверху. Сейчас не доделанный
		//TaskMaster.asyncTimer("ChangePlayerTime", new ChangePlayerTime(), TimeTools.toTicks(1L));
	}

	private void registerEvents() {
		final PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(new BlockListener(), this);
		pluginManager.registerEvents(new ChatListener(), this);
		pluginManager.registerEvents(new BonusGoodieManager(), this);
		pluginManager.registerEvents(new MarkerPlacementManager(), this);
		pluginManager.registerEvents(new CustomItemListener(), this);
		pluginManager.registerEvents(new UnitInventoryListener(), this);
		pluginManager.registerEvents(new PlayerListener(), this);
		pluginManager.registerEvents(new DebugListener(), this);
		pluginManager.registerEvents(new CraftableCustomMaterialListener(), this);
		pluginManager.registerEvents(new LoreGuiItemListener(), this);

		Boolean useEXPAsCurrency = true;
		try {
			useEXPAsCurrency = CivSettings.getBoolean(CivSettings.civConfig, "global.use_exp_as_currency");
		} catch (InvalidConfiguration e) {
			CivLog.error("Unable to check if EXP should be enabled. Disabling.");
			e.printStackTrace();
		}
		if (useEXPAsCurrency) {
			pluginManager.registerEvents(new DisableXPListener(), this);
		}
		pluginManager.registerEvents(new TradeInventoryListener(), this);
		pluginManager.registerEvents(new WarListener(), this);
		pluginManager.registerEvents(new FishingListener(), this);
		pluginManager.registerEvents(new PvPListener(), this);
		pluginManager.registerEvents(new UnitListener(), this);
		pluginManager.registerEvents(new MobListener(), this);

		pluginManager.registerEvents(new ArmorListener(getConfig().getStringList("blocked")), this);
	}

	public boolean hasPlugin(String name) {
		Plugin p;
		p = getServer().getPluginManager().getPlugin(name);
		return (p != null);
	}

	public static JavaPlugin getPlugin() {
		return plugin;
	}

}
