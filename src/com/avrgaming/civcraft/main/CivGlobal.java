/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.avrgaming.civcraft.object.*;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Cave;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructBlock;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.construct.WarCamp;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.populators.TradeGoodPreGenerate;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.sessiondb.SessionDatabase;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Bank;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.structure.Market;
import com.avrgaming.civcraft.structure.Road;
import com.avrgaming.civcraft.structure.RoadBlock;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.structure.Wall;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.CultureProcessAsyncTask;
import com.avrgaming.civcraft.threading.tasks.onLoadTask;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.TagManager;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;
import com.avrgaming.global.perks.PerkManager;

public class CivGlobal {

	public static final double MIN_FRAME_DISTANCE = 3.0;

	public static double LIGHTHOUSE_WATER_PLAYER_SPEED = 1.5;
	public static double LIGHTHOUSE_WATER_BOAT_SPEED = 1.1;

	private static boolean useEconomy;
	public static Economy econ;

	public static SimpleDateFormat dateFormat;

	private static Map<String, Resident> residents = new ConcurrentHashMap<String, Resident>();
	private static Map<UUID, Resident> residentsViaUUID = new ConcurrentHashMap<UUID, Resident>();
	private static Map<Integer, UnitObject> unitObjects = new ConcurrentHashMap<Integer, UnitObject>();

	private static Map<String, Town> towns = new ConcurrentHashMap<String, Town>();
	private static Map<String, Civilization> civs = new ConcurrentHashMap<String, Civilization>();
	private static Map<Integer, Coalition> coalitions = new ConcurrentHashMap<Integer, Coalition>();
	private static Map<String, Civilization> conqueredCivs = new ConcurrentHashMap<String, Civilization>();
	private static Map<String, Civilization> adminCivs = new ConcurrentHashMap<String, Civilization>();
	private static Map<ChunkCoord, TownChunk> townChunks = new ConcurrentHashMap<ChunkCoord, TownChunk>();
	private static Map<ChunkCoord, CultureChunk> cultureChunks = new ConcurrentHashMap<ChunkCoord, CultureChunk>();

	private static Map<BlockCoord, Structure> structures = new ConcurrentHashMap<BlockCoord, Structure>();
	private static Map<BlockCoord, Wonder> wonders = new ConcurrentHashMap<BlockCoord, Wonder>();
	private static Map<BlockCoord, ConstructBlock> constructBlocks = new ConcurrentHashMap<BlockCoord, ConstructBlock>();
	private static Map<ChunkCoord, HashSet<Construct>> constructsInChunk = new ConcurrentHashMap<ChunkCoord, HashSet<Construct>>();
	private static Map<BlockCoord, ConstructSign> constructSigns = new ConcurrentHashMap<BlockCoord, ConstructSign>();
	private static Map<BlockCoord, ConstructChest> constructChests = new ConcurrentHashMap<BlockCoord, ConstructChest>();
	private static Map<BlockCoord, TradeGood> tradeGoods = new ConcurrentHashMap<BlockCoord, TradeGood>();
	private static Map<BlockCoord, ProtectedBlock> protectedBlocks = new ConcurrentHashMap<BlockCoord, ProtectedBlock>();
	private static Map<ChunkCoord, FarmChunk> farmChunks = new ConcurrentHashMap<ChunkCoord, FarmChunk>();
	private static Queue<FarmChunk> farmChunkUpdateQueue = new LinkedList<FarmChunk>();
	private static Map<UUID, ItemFrameStorage> protectedItemFrames = new ConcurrentHashMap<UUID, ItemFrameStorage>();
	private static Map<BlockCoord, BonusGoodie> bonusGoodies = new ConcurrentHashMap<BlockCoord, BonusGoodie>();
	private static Map<ChunkCoord, Cave> caves = new ConcurrentHashMap<ChunkCoord, Cave>();
	private static Map<ChunkCoord, HashSet<Wall>> wallChunks = new ConcurrentHashMap<ChunkCoord, HashSet<Wall>>();
	private static Map<BlockCoord, RoadBlock> roadBlocks = new ConcurrentHashMap<BlockCoord, RoadBlock>();
	private static Map<BlockCoord, CustomMapMarker> customMapMarkers = new ConcurrentHashMap<BlockCoord, CustomMapMarker>();
	private static Map<String, Camp> camps = new ConcurrentHashMap<String, Camp>();
	public static HashSet<BlockCoord> vanillaGrowthLocations = new HashSet<BlockCoord>();
	private static Map<BlockCoord, Market> markets = new ConcurrentHashMap<BlockCoord, Market>();
	public static HashSet<String> researchedTechs = new HashSet<String>();
	private static Map<Integer, Report> reports = new HashMap<Integer, Report>();

	public static long cantDemolishFrom;
	public static long cantDemolishUntil;

	public static void addReport(final Report report) {
		CivGlobal.reports.put(report.getId(), report);
	}

	/* TODO change this to true for MC 1.8 */
	public static boolean useUUID = true;

	public static Map<Integer, Boolean> CivColorInUse = new ConcurrentHashMap<Integer, Boolean>();
	public static TradeGoodPreGenerate tradeGoodPreGenerator = new TradeGoodPreGenerate();

	// TODO fix the duplicate score issue...
	public static TreeMap<Integer, Civilization> civilizationScores = new TreeMap<Integer, Civilization>();
	public static TreeMap<Integer, Town> townScores = new TreeMap<Integer, Town>();

	public static HashMap<String, Date> playerFirstLoginMap = new HashMap<String, Date>();
	public static HashSet<String> banWords = new HashSet<String>();

	// public static Scoreboard globalBoard;

	public static Integer maxPlayers = -1;
	public static HashSet<String> betaPlayers = new HashSet<String>();
	public static String fullMessage = "";
	public static Boolean betaOnly = false;

	// TODO convert this to completely static?
	private static SessionDatabase sessionDatabase;

	public static SessionDatabase getSessionDatabase() {
		return sessionDatabase;
	}

	public static boolean trommelsEnabled = true;
	public static boolean quarriesEnabled = true;
	public static boolean fisheryEnabled = true;
	public static boolean mobGrinderEnabled = true;
	public static boolean towersEnabled = true;
	public static boolean growthEnabled = true;
	public static Boolean banWordsAlways = false;
	public static boolean banWordsActive = false;
	public static boolean scoringEnabled = true;
	public static boolean warningsEnabled = true;
	public static boolean tradeEnabled = true;
	public static boolean loadCompleted = false;
	public static boolean speedChunks = false;
	public static int minBuildHeight = 1;

	public static ArrayList<Town> orphanTowns = new ArrayList<Town>();
	public static ArrayList<Civilization> orphanCivs = new ArrayList<Civilization>();

	public static boolean checkForBooks = true;
	public static boolean debugDateBypass = false;
	public static boolean endWorld = false;
	public static PerkManager perkManager = null;
	public static boolean installMode = false;

	public static int highestCivEra = 0;

	// TODO Помоему это нигде не используеться
	public static TradeGoodPreGenerate preGenerator;

	public static int ruinsGenerated = 0;

	public static String localizedEraString(int era) {
		String newEra = "";
		switch (era) {
		case 0: // ANCIENT
			newEra = "announce_ancientEra";
			break;
		case 1: // CLASSICAL
			newEra = "announce_classicalEra";
			break;
		case 2: // MEDIEVAL
			newEra = "announce_medievalEra";
			break;
		case 3: // RENAISSANCE
			newEra = "announce_renaissanceEra";
			break;
		case 4: // INDUSTRIAL
			newEra = "announce_industrialEra";
			break;
		case 5: // MODERN
			newEra = "announce_modernEra";
			break;
		case 6: // ATOMIC
			newEra = "announce_atomicEra";
			break;
		case 7: // INFORMATION
			newEra = "announce_informationEra";
			break;
		default:
			break;
		}
		return CivSettings.localize.localizedString(newEra);
	}

	public static void setCurrentEra(int era, Civilization civ) {
		if (era > highestCivEra && !civ.isAdminCiv()) {
			highestCivEra = era;
			CivMessage.globalTitle(CivColor.Green + localizedEraString(highestCivEra), CivColor.LightGreen
					+ CivSettings.localize.localizedString("var_announce_newEraCiv", civ.getName()));

		}
	}

	public static void loadGlobals() throws SQLException, CivException {
		/*
		 * Don't use CivSettings.getBoolean() to prevent error when using old config
		 * Must be loaded before residents are loaded
		 */
		useEconomy = CivSettings.civConfig.getBoolean("global.use_vault");

		CivLog.heading("Loading CivCraft Objects From Database");
		try {
			dateFormat = new SimpleDateFormat(CivSettings.getStringBase("simple_date_format"));
		} catch (InvalidConfiguration e) {
			dateFormat = new SimpleDateFormat("M/dd/yy h:mm:ss a z");
		}
		sessionDatabase = new SessionDatabase();
		loadCamps();
		loadCivs();
		loadRelations();
		loadCoalitions();
		loadTowns();
		loadResidents();
		loadPermissionGroups();
		loadTownChunks();
		loadWonders();
		loadStructures();
		loadWallBlocks();
		loadRoadBlocks();
		loadCaves();
		loadTradeGoodies();
		loadRandomEvents();
		loadProtectedBlocks();
//		loadStructureSign();
		loadUnitObjects();
		loadReports();
		EventTimer.loadGlobalEvents();
		EndGameCondition.init();
		War.init();

		CivLog.heading("--- Done <3 ---");

		/*
		 * Load in upgrades after all of our objects are loaded, resolves dependencies
		 */
		processUpgrades();
		processCulture();

		/* Finish with an onLoad event. */
		TaskMaster.syncTask(new onLoadTask());

		/* Check for orphan civs now */
		for (Civilization civ : civs.values()) {
			Town capitol = civ.getTown(civ.getCapitolName());
			if (capitol == null)
				orphanCivs.add(civ);
		}

		checkForInvalidStructures();

		try {
			minBuildHeight = CivSettings.getInteger(CivSettings.civConfig, "global.min_build_height");
		} catch (InvalidConfiguration e) {
			minBuildHeight = 1;
			e.printStackTrace();
		}

		try {
			speedChunks = CivSettings.getBoolean(CivSettings.civConfig, "global.speed_check_chunks");
		} catch (InvalidConfiguration e) {
			speedChunks = false;
			e.printStackTrace();
		}

		cantDemolishFrom = getNextRepoTime().getTime() - 3600000L;
		cantDemolishUntil = getNextRepoTime().getTime() + 14400000L;

		loadCompleted = true;
	}

	public static void checkForInvalidStructures() {
		// TODO Убрать
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		while (iter.hasNext()) {
			Structure struct = iter.next().getValue();
			if (struct instanceof Capitol) {
				if (struct.getTown().getMotherCiv() == null) {
					if (!struct.getTown().isCapitol()) {
						struct.markInvalid();
						struct.setInvalidReason(CivSettings.localize.localizedString("cap_CanExistInCapitol"));
					}
				}
			}
		}
	}

	private static void processUpgrades() throws CivException {
		for (Town town : towns.values()) {
			try {
				town.loadUpgrades();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void loadCoalitions() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Coalition.TABLE_NAME);
			rs = ps.executeQuery();
			int count = 0;

			while (rs.next()) {
				try {
					CivGlobal.addCoalition(new Coalition(rs));
				} catch (Exception e) {
					e.printStackTrace();
				}
				count++;
			}

			CivLog.info("Loaded " + count + " Coalitions");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadReports() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + "REPORTS");
			rs = ps.executeQuery();
			while (rs.next()) {
				try {
					final Report report = new Report(rs);
					CivGlobal.reports.put(report.getId(), report);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CivLog.info("Loaded " + CivGlobal.towns.size() + " Reports");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	private static void loadCaves() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Cave.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				Cave cave;
				try {
					cave = new Cave(rs);
					caves.put(cave.getCornerEntrance().getChunkCoord(), cave);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + caves.size() + " Caves");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	private static void loadTradeGoodies() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + TradeGood.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				TradeGood good;
				try {
					good = new TradeGood(rs);
					tradeGoods.put(good.getCoord(), good);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + tradeGoods.size() + " Trade Goods");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	private static void loadCivs() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Civilization.TABLE_NAME);
			rs = ps.executeQuery();
			int count = 0;

			while (rs.next()) {
				try {
					Civilization civ = new Civilization(rs);

					if (highestCivEra < civ.getCurrentEra() && !civ.isAdminCiv()) {
						highestCivEra = civ.getCurrentEra();
					}

					if (!civ.isConquered()) {
						CivGlobal.addCiv(civ);
					} else {
						CivGlobal.addConqueredCiv(civ);
					}
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CivLog.info("Loaded " + count + " Civs");
		} finally {
			SQL.close(rs, ps, context);
		}

	}

	private static void loadRelations() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Relation.TABLE_NAME);
			rs = ps.executeQuery();
			int count = 0;

			while (rs.next()) {
				try {
					new Relation(rs);
				} catch (Exception e) {
					e.printStackTrace();
				}
				count++;
			}

			CivLog.info("Loaded " + count + " Relations");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadPermissionGroups() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + PermissionGroup.TABLE_NAME);
			rs = ps.executeQuery();
			int count = 0;

			while (rs.next()) {
				try {
					new PermissionGroup(rs);
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + count + " PermissionGroups");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadResidents() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Resident.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				Resident res;
				try {
					res = new Resident(rs);
					CivGlobal.addResident(res);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + residents.size() + " Residents");
		} finally {
			SQL.close(rs, ps, context);
		}
		for (Camp vill : CivGlobal.getCamps()) {
			vill.setSQLOwner(CivGlobal.getResident(vill.getOwnerName()));
		}
	}

	public static void loadTowns() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Town.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				try {
					Town town = new Town(rs);
					towns.put(town.getName().toLowerCase(), town);
					WarRegen.restoreBlocksFor(town.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			WarRegen.restoreBlocksFor(WarCamp.RESTORE_NAME);
			CivLog.info("Loaded " + towns.size() + " Towns");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadCamps() throws SQLException {
		Camp.loadStaticSettings();

		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Camp.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				try {
					Camp camp = new Camp(rs);
					CivGlobal.addCamp(camp);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			SQL.close(rs, ps, context);
		}

		CivLog.info("Loaded " + camps.size() + " Camps");
	}

	public static void loadTownChunks() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + TownChunk.TABLE_NAME);
			rs = ps.executeQuery();
			while (rs.next()) {
				try {
					TownChunk tc = new TownChunk(rs);
					townChunks.put(tc.getChunkCoord(), tc);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CivLog.info("Loaded " + townChunks.size() + " TownChunks");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadStructures() throws SQLException, CivException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Structure.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				try {
					Structure struct = Structure.newStructure(rs);
					structures.put(struct.getCorner(), struct);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + structures.size() + " Structures");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadWonders() throws SQLException, CivException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Wonder.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				try {
					Wonder wonder = Wonder.newWonder(rs);
					wonders.put(wonder.getCorner(), wonder);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + wonders.size() + " Wonders");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	private static void loadWallBlocks() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + WallBlock.TABLE_NAME);
			rs = ps.executeQuery();

			int count = 0;
			while (rs.next()) {
				try {
					new WallBlock(rs);
					count++;
				} catch (Exception e) {
					CivLog.warning(e.getMessage());
					// e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + count + " Wall Block");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	private static void loadRoadBlocks() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + RoadBlock.TABLE_NAME);
			rs = ps.executeQuery();

			int count = 0;
			while (rs.next()) {
				try {
					new RoadBlock(rs);
					count++;
				} catch (Exception e) {
					CivLog.warning(e.getMessage());
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + count + " Road Block");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadRandomEvents() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + RandomEvent.TABLE_NAME);
			rs = ps.executeQuery();

			int count = 0;
			while (rs.next()) {
				try {
					new RandomEvent(rs);
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + count + " Active Random Events");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void loadProtectedBlocks() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + ProtectedBlock.TABLE_NAME);
			rs = ps.executeQuery();

			int count = 0;
			while (rs.next()) {
				try {
					ProtectedBlock pb = new ProtectedBlock(rs);
					CivGlobal.addProtectedBlock(pb);
					count++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CivLog.info("Loaded " + count + " Protected Blocks");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

//	private static void loadStructureSign() throws SQLException {
//		Connection context = null;
//		ResultSet rs = null;
//		PreparedStatement ps = null;
//
//		try {
//			context = SQL.getGameConnection();
//			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + BuildableSign.TABLE_NAME);
//			rs = ps.executeQuery();
//
//			while (rs.next()) {
//				BuildableSign strSign;
//				try {
//					strSign = new BuildableSign(rs);
//					buildableSigns.put(strSign.getCoord(), strSign);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//
//			CivLog.info("Loaded " + buildableSigns.size() + " Structure Sign");
//		} finally {
//			SQL.close(rs, ps, context);
//		}
//	}
	public static void loadUnitObjects() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + UnitObject.TABLE_NAME);
			rs = ps.executeQuery();
			while (rs.next()) {
				try {
					UnitObject uo = new UnitObject(rs);
					if (uo.getTownOwner() == null)
						uo.delete();
					else
						CivGlobal.addUnitObject(uo);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CivLog.info("Loaded " + unitObjects.size() + " UnitObjects");
		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static Player getPlayer(Resident resident) throws CivException {
		Player player = Bukkit.getPlayer(resident.getUid());
		if (player == null)
			throw new CivException("No player named" + " " + resident.getName());
		return player;
	}

	public static Player getPlayer(String name) throws CivException {
		Resident res = CivGlobal.getResident(name);
		if (res == null)
			throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noResident", name));
		Player player = Bukkit.getPlayer(res.getUid());
		if (player == null)
			throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noPlayer", name));
		return player;
	}

	// ---------- Resident
	public static void addResident(Resident res) {
		residents.put(res.getName(), res);
		residentsViaUUID.put(res.getUid(), res);
	}

	public static void removeResident(Resident res) {
		residents.remove(res.getName());
		residentsViaUUID.remove(res.getUid());
	}

	public static boolean hasResident(String name) {
		return residents.containsKey(name);
	}

	public static Resident getResident(String name) {
		return residents.get(name);
	}

	public static Resident getResident(Player player) {
		return getResident(player.getName());
	}

	public static Resident getResidentViaUUID(UUID uuid) {
		return residentsViaUUID.get(uuid);
	}

	/**
	 * make lookup via ID faster(use hashtable)
	 * 
	 * @deprecated
	 */
	public static Resident getResidentFromId(int id) {
		for (Resident resident : residents.values()) {
			if (resident.getId() == id)
				return resident;
		}
		return null;
	}

	public static Collection<Resident> getResidents() {
		return residents.values();
	}

	// --------------- Town
	public static void addTown(Town town) {
		towns.put(town.getName().toLowerCase(), town);
	}

	public static void removeTown(Town town) {
		towns.remove(town.getName().toLowerCase());
	}

	public static Town getTown(String name) {
		if (name == null)
			return null;
		return towns.get(name.toLowerCase());
	}

	public static Collection<Town> getTowns() {
		return towns.values();
	}

	/** Use only load. Make lookup via ID faster(use hashtable) */
	public static Town getTownFromId(int id) {
		for (Town t : towns.values()) {
			if (t.getId() == id)
				return t;
		}
		return null;
	}

	// -------------- TownChunk
	public static void addTownChunk(TownChunk tc) {
		townChunks.put(tc.getChunkCoord(), tc);
		return;
	}

	public static void removeTownChunk(TownChunk tc) {
		if (tc.getChunkCoord() != null)
			townChunks.remove(tc.getChunkCoord());
	}

	public static TownChunk getTownChunk(Location location) {
		ChunkCoord coord = new ChunkCoord(location);
		return townChunks.get(coord);
	}

	public static TownChunk getTownChunk(ChunkCoord coord) {
		return townChunks.get(coord);
	}

	public static Collection<TownChunk> getTownChunks() {
		return townChunks.values();
	}

	// ------------ Civilization
	public static void addCiv(Civilization civ) {
		civs.put(civ.getName().toLowerCase(), civ);
		if (civ.isAdminCiv())
			addAdminCiv(civ);
	}

	public static void removeCiv(Civilization civilization) {
		civs.remove(civilization.getName().toLowerCase());
		// TODO протестить надо
		int cid = civilization.getDiplomacyManager().getCoalitionId();
		if (cid != 0)
			coalitions.get(cid).removeCiv(cid);
		if (civilization.isAdminCiv())
			removeAdminCiv(civilization);
	}

	public static Civilization getCiv(String name) {
		return civs.get(name.toLowerCase());
	}

	public static Civilization getCivFromId(int id) {
		for (Civilization civ : civs.values()) {
			if (civ.getId() == id)
				return civ;
		}
		return null;
	}

	public static Collection<Civilization> getCivs() {
		return civs.values();
	}

	/*
	 * Gets a TreeMap of the civilizations sorted based on the distance to the
	 * provided town. Ignores the civilization the town belongs to.
	 */
	public static TreeMap<Double, Civilization> findNearestCivilizations(Town town) {
		Townhall townhall = town.getTownHall();
		TreeMap<Double, Civilization> returnMap = new TreeMap<Double, Civilization>();
		if (townhall == null)
			return returnMap;
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ == town.getCiv())
				continue;
			// Get shortest distance of any of this civ's towns.
			double shortestDistance = Double.MAX_VALUE;
			for (Town t : civ.getTowns()) {
				Townhall tempTownHall = t.getTownHall();
				if (tempTownHall == null)
					continue;
				double tmpDistance = tempTownHall.getCorner().distanceSquared(townhall.getCorner());
				if (tmpDistance < shortestDistance)
					shortestDistance = tmpDistance;
			}
			// Now insert the shortest distance into the tree map.
			returnMap.put(shortestDistance, civ);
		}
		// Map returned will be sorted.
		return returnMap;
	}

	// ----------------- ConqueredCiv
	public static void addConqueredCiv(Civilization civ) {
		conqueredCivs.put(civ.getName().toLowerCase(), civ);
	}

	public static void removeConqueredCiv(Civilization civ) {
		conqueredCivs.remove(civ.getName().toLowerCase());
	}

	public static Civilization getConqueredCiv(String name) {
		return conqueredCivs.get(name.toLowerCase());
	}

	public static Collection<Civilization> getConqueredCivs() {
		return conqueredCivs.values();
	}

	public static Civilization getConqueredCivFromId(int id) {
		for (Civilization civ : getConqueredCivs()) {
			if (civ.getId() == id)
				return civ;
		}
		return null;
	}

	// ----------- AdminCiv
	public static void addAdminCiv(Civilization civ) {
		adminCivs.put(civ.getName(), civ);
	}

	public static void removeAdminCiv(Civilization civ) {
		adminCivs.remove(civ.getName());
	}

	public static boolean isAdminCivs(Civilization civ) {
		return adminCivs.containsValue(civ);
	}

	public static Collection<Civilization> getAdminCivs() {
		return adminCivs.values();
	}

	// ----------- PermissionGroup
	public static PermissionGroup getPermissionGroup(Town town, Integer id) {
		return town.getGroupFromId(id);
	}

	public static PermissionGroup getPermissionGroupFromName(Town town, String name) {
		for (PermissionGroup grp : town.getGroups()) {
			if (grp.getName().equalsIgnoreCase(name))
				return grp;
		}
		return null;
	}

	public static Collection<PermissionGroup> getPermissionGroups() {
		ArrayList<PermissionGroup> groups = new ArrayList<PermissionGroup>();
		for (Town t : towns.values()) {
			for (PermissionGroup grp : t.getGroups()) {
				if (grp != null)
					groups.add(grp);
			}
		}
		for (Civilization civ : civs.values()) {
			if (civ.getLeaderGroup() != null)
				groups.add(civ.getLeaderGroup());
			if (civ.getAdviserGroup() != null)
				groups.add(civ.getAdviserGroup());
		}
		return groups;
	}

	// --------- CultureChunk
	public static void addCultureChunk(CultureChunk cc) {
		cultureChunks.put(cc.getChunkCoord(), cc);
	}

	public static void removeCultureChunk(CultureChunk cc) {
		cultureChunks.remove(cc.getChunkCoord());
	}

	public static CultureChunk getCultureChunk(ChunkCoord coord) {
		return cultureChunks.get(coord);
	}

	public static CultureChunk getCultureChunk(Location location) {
		ChunkCoord coord = new ChunkCoord(location);
		return getCultureChunk(coord);
	}

	public static Collection<CultureChunk> getCultureChunks() {
		return cultureChunks.values();
	}

	public static void processCulture() {
		TaskMaster.asyncTask("culture-process", new CultureProcessAsyncTask(), 0);
	}

	// ---------------- Structure
	public static void addStructure(Structure structure) {
		structures.put(structure.getCorner(), structure);
	}

	public static void removeStructure(Structure structure) {
		structures.remove(structure.getCorner());
	}

	public static Structure getStructure(BlockCoord center) {
		return structures.get(center);
	}

	public static Collection<Structure> getStructures() {
		return structures.values();
	}

	public static Iterator<Entry<BlockCoord, Structure>> getStructureIterator() {
		return structures.entrySet().iterator();
	}

	public static Structure getStructureById(int id) {
		for (Structure struct : structures.values()) {
			if (struct.getId() == id)
				return struct;
		}
		return null;
	}

	// --------------- Wonder
	public static void addWonder(Wonder wonder) {
		wonders.put(wonder.getCorner(), wonder);
	}

	public static Wonder getWonder(BlockCoord coord) {
		return wonders.get(coord);
	}

	public static void removeWonder(Wonder wonder) {
		if (wonder.getCorner() != null)
			wonders.remove(wonder.getCorner());
	}

	public static Collection<Wonder> getWonders() {
		return wonders.values();
	}

	public static Wonder getWonderByConfigId(String id) {
		for (Wonder wonder : wonders.values()) {
			if (wonder.getConfigId().equals(id))
				return wonder;
		}
		return null;
	}

	public static Wonder getWonderById(int id) {
		for (Wonder wonder : wonders.values()) {
			if (wonder.getId() == id)
				return wonder;
		}
		return null;
	}

	public static int getWonderCount() {
		int count = 0;
		for (final Wonder wonder : getWonders()) {
			if (wonder != null && !wonder.getConfigId().contains("w_colosseum"))
				++count;
		}
		return count;
	}

	public static Structure getNearestStructure(Location location) {
		Structure nearest = null;
		double lowest_distance = Double.MAX_VALUE;
		for (Structure struct : structures.values()) {
			Location loc = new Location(Bukkit.getWorld("world"), struct.getCenterLocation().getX(),
					struct.getCorner().getLocation().getY(), struct.getCenterLocation().getZ());
			double distance = loc.distance(location);
			if (distance < lowest_distance) {
				lowest_distance = distance;
				nearest = struct;
			}
		}
		for (Structure wonder : wonders.values()) {
			Location loc = new Location(Bukkit.getWorld("world"), wonder.getCenterLocation().getX(),
					wonder.getCorner().getLocation().getY(), wonder.getCenterLocation().getZ());
			double distance = loc.distance(location);
			if (distance < lowest_distance) {
				lowest_distance = distance;
				nearest = wonder;
			}
		}
		return nearest;
	}

	// ---------------- ConstructBlock
	public static void addConstructBlock(BlockCoord coord, Construct owner, boolean damageable) {
		ConstructBlock sb = new ConstructBlock(coord, owner);
		sb.setDamageable(damageable);
		constructBlocks.put(coord, sb);

		if (!(owner instanceof Wall) && !(owner instanceof Road)) {
			ChunkCoord cc = new ChunkCoord(coord);
			HashSet<Construct> constructs = constructsInChunk.get(cc);
			if (constructs == null)
				constructs = new HashSet<Construct>();
			if (constructs.contains(owner))
				return;
			constructs.add(owner);
			constructsInChunk.put(cc, constructs);
		}
	}

	public static void removeConstructBlock(BlockCoord coord) {
		ConstructBlock bb = constructBlocks.get(coord);
		if (bb == null)
			return;
		constructBlocks.remove(coord);

		ChunkCoord cc = new ChunkCoord(coord);
		HashSet<Construct> constructs = constructsInChunk.get(cc);
		if (constructs != null) {
			constructs.remove(bb.getOwner());
			if (constructs.size() > 0)
				constructsInChunk.put(cc, constructs);
			else
				constructsInChunk.remove(cc);
		}
	}

	public static ConstructBlock getConstructBlock(BlockCoord coord) {
		return constructBlocks.get(coord);
	}

	public static HashSet<Construct> getConstructFromChunk(BlockCoord coord) {
		return constructsInChunk.get(new ChunkCoord(coord));
	}

	public static HashSet<Construct> getConstructFromChunk(ChunkCoord cc) {
		return constructsInChunk.get(cc);
	}

	public static Construct getConstructAt(ChunkCoord cc) {
		if (getConstructFromChunk(cc) == null)
			return null;
		for (Construct b : getConstructFromChunk(cc))
			return b;
		return null;
	}

	public static Construct getConstructAt(BlockCoord bc) {
		if (getConstructFromChunk(bc) == null)
			return null;
		for (Construct b : getConstructFromChunk(bc))
			return b;
		return null;
	}

	// ------------- ConstructSign
	public static void addConstructSign(ConstructSign sign) {
		constructSigns.put(sign.getCoord(), sign);
	}

	public static void removeConstructSign(ConstructSign sign) {
		constructSigns.remove(sign.getCoord());
	}

	public static ConstructSign getConstructSign(BlockCoord coord) {
		return constructSigns.get(coord);
	}

	public static Collection<ConstructSign> getConstructSigns() {
		return constructSigns.values();
	}

	// -------------- ConstructChest
	public static void addConstructChest(ConstructChest structChest) {
		constructChests.put(structChest.getCoord(), structChest);
	}

	public static void removeConstructChest(ConstructChest chest) {
		constructChests.remove(chest.getCoord());
	}

	public static ConstructChest getConstructChest(BlockCoord coord) {
		return constructChests.get(coord);
	}

	// --------------- Wall
	public static HashSet<Wall> getWallChunk(ChunkCoord coord) {
		HashSet<Wall> walls = wallChunks.get(coord);
		if (walls != null && walls.size() > 0)
			return walls;
		else
			return null;
	}

	public static void addWallChunk(Wall wall, ChunkCoord coord) {
		HashSet<Wall> walls = wallChunks.get(coord);
		if (walls == null)
			walls = new HashSet<Wall>();
		walls.add(wall);
		wallChunks.put(coord, walls);
		wall.wallChunks.add(coord);
	}

	public static void removeWallChunk(Wall wall, ChunkCoord coord) {
		wallChunks.remove(coord);
	}

	// --------------- Road
	public static void addRoadBlock(RoadBlock rb) {
		roadBlocks.put(rb.getCoord(), rb);
	}

	public static void removeRoadBlock(RoadBlock rb) {
		roadBlocks.remove(rb.getCoord());
	}

	public static RoadBlock getRoadBlock(BlockCoord coord) {
		return roadBlocks.get(coord);
	}

	// ----------------- ProtectedBlock
	public static void addProtectedBlock(ProtectedBlock pb) {
		protectedBlocks.put(pb.getCoord(), pb);
	}

	public static ProtectedBlock getProtectedBlock(BlockCoord coord) {
		return protectedBlocks.get(coord);
	}

	// ----------- TradeGood
	public static void addTradeGood(TradeGood good) {
		tradeGoods.put(good.getCoord(), good);
	}

	public static TradeGood getTradeGood(BlockCoord coord) {
		return tradeGoods.get(coord);
	}

	public static Collection<TradeGood> getTradeGoods() {
		return tradeGoods.values();
	}

	// ------------ FarmChunk
	public static void addFarmChunk(ChunkCoord coord, FarmChunk fc) {
		farmChunks.put(coord, fc);
		CivGlobal.queueFarmChunk(fc);
	}

	public static FarmChunk getFarmChunk(ChunkCoord coord) {
		return farmChunks.get(coord);
	}

	public static boolean farmChunkValid(FarmChunk fc) {
		return farmChunks.containsKey(fc.getCoord());
	}

	public static Collection<FarmChunk> getFarmChunks() {
		return farmChunks.values();
	}

	public static void dequeueFarmChunk(FarmChunk fc) {
		farmChunkUpdateQueue.remove(fc);
	}

	public static void queueFarmChunk(FarmChunk fc) {
		farmChunkUpdateQueue.add(fc);
	}

	public static FarmChunk pollFarmChunk() {
		return farmChunkUpdateQueue.poll();
	}

	public static void removeFarmChunk(ChunkCoord coord) {
		FarmChunk fc = getFarmChunk(coord);
		if (fc != null)
			CivGlobal.dequeueFarmChunk(fc);
		farmChunks.remove(coord);
	}

	// ------------ UnitObject
	public static void addUnitObject(UnitObject uo) {
		unitObjects.put(uo.getId(), uo);
	}

	public static UnitObject getUnitObject(int id) {
		return unitObjects.get(id);
	}

	public static void removeUnitObject(UnitObject uo) {
		unitObjects.remove(uo.getId());
	}

	public static Collection<UnitObject> getUnitObjects() {
		return unitObjects.values();
	}

	// -------------- BonusGoodie and ProtectedItemFrame
	public static void addBonusGoodie(BonusGoodie goodie) {
		bonusGoodies.put(goodie.getOutpost().getCorner(), goodie);
	}

	public static BonusGoodie getBonusGoodie(BlockCoord bcoord) {
		return bonusGoodies.get(bcoord);
	}

	public static boolean isBonusGoodie(ItemStack item) {
		if (item == null)
			return false;
		if (ItemManager.getTypeId(item) == CivData.AIR)
			return false;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return false;
		if (!meta.hasLore() || meta.getLore().size() < BonusGoodie.LoreIndex.values().length)
			return false;
		if (!meta.getLore().get(BonusGoodie.LoreIndex.TYPE.ordinal()).equals(BonusGoodie.LORE_TYPE))
			return false;
		return true;
	}

	public static BonusGoodie getBonusGoodie(ItemStack item) {
		if (!isBonusGoodie(item))
			return null;
		ItemMeta meta = item.getItemMeta();
		String outpostLocation = meta.getLore().get(BonusGoodie.LoreIndex.OUTPOSTLOCATION.ordinal());
		BlockCoord bcoord = new BlockCoord(outpostLocation);
		return getBonusGoodie(bcoord);
	}

	public static Collection<BonusGoodie> getBonusGoodies() {
		return bonusGoodies.values();
	}

	public static void addProtectedItemFrame(ItemFrameStorage framestore) {
		protectedItemFrames.put(framestore.getUUID(), framestore);
		ItemFrameStorage.attachedBlockMap.put(framestore.getAttachedBlock(), framestore);
	}

	public static ItemFrameStorage getProtectedItemFrame(UUID id) {
		return protectedItemFrames.get(id);
	}

	public static void removeProtectedItemFrame(UUID id) {
		CivLog.debug("Remove ID: " + id);
		if (id == null)
			return;
		ItemFrameStorage store = getProtectedItemFrame(id);
		if (store == null)
			return;
		ItemFrameStorage.attachedBlockMap.remove(store.getAttachedBlock());
		protectedItemFrames.remove(id);
	}

	public static void checkForDuplicateGoodies() {
		// Look through protected item frames and repo and duplicates we find.
		HashMap<String, Boolean> outpostsInFrames = new HashMap<String, Boolean>();
		for (ItemFrameStorage fs : protectedItemFrames.values()) {
			try {
				if (fs.noFrame() || fs.isEmpty())
					continue;
			} catch (CivException e) {
				e.printStackTrace();
				continue;
			}
			BonusGoodie goodie = getBonusGoodie(fs.getItem());
			if (goodie == null)
				continue;
			if (outpostsInFrames.containsKey(goodie.getOutpost().getCorner().toString())) {
				// CivMessage.sendTown(goodie.getOutpost().getTown(), CivColor.Rose+"WARNING:
				// "+CivColor.Yellow+"Duplicate goodie item detected for good "+
				// goodie.getDisplayName()+" at outpost
				// "+goodie.getOutpost().getCorner().toString()+
				// ". Item was reset back to outpost.");
				fs.clearItem();
			} else
				outpostsInFrames.put(goodie.getOutpost().getCorner().toString(), true);
		}
	}

	// ------------ Cave
	public static void addCave(Cave cave) {
		caves.put(cave.getCornerEntrance().getChunkCoord(), cave);
	}

	public static void removeCave(ChunkCoord ccoord) {
		caves.remove(ccoord);
	}

	public static Cave getCave(ChunkCoord ccoord) {
		return caves.get(ccoord);
	}

	public static Cave getCaveFromId(int id) {
		for (Cave cave : caves.values()) {
			if (cave.getId() == id)
				return cave;
		}
		return null;
	}

	public static Collection<Cave> getCaves() {
		return caves.values();
	}

	// ------------ Camp
	public static void addCamp(Camp camp) {
		camps.put(camp.getName().toLowerCase(), camp);
	}

	public static void removeCamp(String name) {
		camps.remove(name.toLowerCase());
	}

	public static Camp getCamp(String name) {
		return camps.get(name.toLowerCase());
	}

	public static Camp getCampFromId(int campID) {
		for (Camp camp : camps.values()) {
			if (camp.getId() == campID)
				return camp;
		}
		return null;
	}

	public static Collection<Camp> getCamps() {
		return camps.values();
	}

//	// ------------- CampBlock
//	public static void addCampBlock(CampBlock cb) {
//		campBlocks.put(cb.getCoord(), cb);
//		ChunkCoord coord = new ChunkCoord(cb.getCoord());
//		campChunks.put(coord, cb.getCamp());
//	}
//	public static CampBlock getCampBlock(BlockCoord bcoord) {
//		return campBlocks.get(bcoord);
//	}
//	public static void removeCampBlock(BlockCoord bcoord) {
//		campBlocks.remove(bcoord);
//	}
//	public static Camp getCampFromChunk(ChunkCoord coord) {
//		return campChunks.get(coord);
//	}
//	public static void removeCampChunk(ChunkCoord coord) {
//		campChunks.remove(coord);
//	}

	// ------------- Market
	public static void addMarket(Market market) {
		markets.put(market.getCorner(), market);
	}

	public static void removeMarket(Market market) {
		markets.remove(market.getCorner());
	}

	public static Collection<Market> getMarkets() {
		return markets.values();
	}

	// ---------- Total count function
	public static int getTotalCamps() {
		int total = 0;
		for (final Camp camp : getCamps()) {
			if (camp != null)
				++total;
		}
		return total;
	}

	public static int getTotalCivs() {
		int total = 0;
		for (final Civilization civ : getCivs()) {
			if (!civ.isAdminCiv())
				++total;
		}
		return total;
	}

	public static int getTotalTowns() {
		int total = 0;
		for (final Town town : getTowns()) {
			if (town != null)
				++total;
		}
		return total;
	}

	public static int getTotalBanks() {
		int total = 0;
		for (final Structure strucutre : getStructures()) {
			if (strucutre instanceof Bank)
				++total;
		}
		return total;
	}

	public static int getTownHalls() {
		int total = 0;
		for (final Structure strucutre : getStructures()) {
			if (strucutre instanceof Townhall)
				++total;
		}
		return total;
	}

	// --------------- Coalition
	public static void addCoalition(Coalition coal) {
		coalitions.put(coal.getId(), coal);
		Coalition.message("Цивилизация " + coal.getCreator().getName() + " объявила о создании коалиции под названием "
				+ coal.getName());
	}

	public static Coalition getCoalition(String name) {
		for (Coalition c : coalitions.values()) {
			if (c.getName().equalsIgnoreCase(name))
				return c;
		}
		return null;
	}

	public static Coalition getCoalition(int id) {
		return coalitions.get(id);
	}

	public static Collection<Coalition> getCoalitions() {
		return coalitions.values();
	}

	public static void removeCoalition(Coalition coal) {
		coalitions.remove(coal.getId());
		Coalition.message("Коалиция " + coal.getName() + " была рассформирована");
	}

	// -------------- Report
	public static Report getReportById(final int id) {
		return CivGlobal.reports.get(id);
	}

	public static Collection<Report> getReports() {
		return CivGlobal.reports.values();
	}

	public static Report getReportByCloseTime(long closeTime) {
		for (Report report : CivGlobal.reports.values()) {
			if (report.isClosed() && report.getCloseTime() == closeTime)
				return report;
		}
		return null;
	}

	// ----------- CustomMarker
	public static void addCustomMarker(Location location, String name, String desc, String icon) {
		CustomMapMarker marker = new CustomMapMarker();
		marker.name = name;
		marker.description = desc;
		marker.icon = icon;
		customMapMarkers.put(new BlockCoord(location), marker);
	}

	public static void removeCustomMarker(Location location) {
		customMapMarkers.remove(new BlockCoord(location));
	}

	public static void removeCustomMarker(BlockCoord coord) {
		customMapMarkers.remove(coord);
	}

	public static Collection<CustomMapMarker> getCustomMarkers() {
		return customMapMarkers.values();
	}

	public static Location getLocationFromHash(String hash) {
		String split[] = hash.split(",");
		Location loc = new Location(BukkitObjects.getWorld(split[0]), Double.valueOf(split[1]),
				Double.valueOf(split[2]), Double.valueOf(split[3]));
		return loc;
	}

	public static String getXYFromBlockCoord(BlockCoord coord) {
		return coord.getX() + ":" + coord.getZ() + ":" + coord.getWorldname();
	}

	public static int getLeftoverSize(HashMap<Integer, ItemStack> leftovers) {
		int total = 0;
		for (ItemStack stack : leftovers.values()) {
			total += stack.getAmount();
		}
		return total;
	}

	public static int getSecondsBetween(long t1, long t2) {
		return (int) ((t2 - t1) / 1000);
	}

	public static boolean isHaveTestFlag(String flagname) {
		try {
			if (CivSettings.getStringBase(flagname).equalsIgnoreCase("true"))
				return true;
		} catch (InvalidConfiguration e) {
		}
		return false;
	}

	public static boolean hasTimeElapsed(SessionEntry se, double seconds) {
		long now = System.currentTimeMillis();
		int secondsBetween = getSecondsBetween(se.time, now);

		// First determine the time between two events.
		if (secondsBetween < seconds) {
			return false;
		}
		return true;
	}

	public static Date getNextUpkeepDate() {

		EventTimer daily = EventTimer.timers.get("daily");
		return daily.getNext().getTime();

//		int upkeepHour;
//		try {
//			upkeepHour = CivSettings.getInteger(CivSettings.civConfig, "global.daily_upkeep_hour");
//		} catch (InvalidConfiguration e) {
//			e.printStackTrace();
//			return null;
//		}
//		
//		Calendar c = Calendar.getInstance();
//		Date now = c.getTime();
//		
//		c.set(Calendar.HOUR_OF_DAY, upkeepHour);
//		c.set(Calendar.MINUTE, 0);
//		c.set(Calendar.SECOND, 0);
//		
//		if (now.after(c.getTime())) {
//			c.add(Calendar.DATE, 1);
//		}
//		
//		return c.getTime();
	}

	public static Date getNextHourlyTickDate() {
		EventTimer hourly = EventTimer.timers.get("hourly");
		return hourly.getNext().getTime();
	}

	/*
	 * Empty, duplicate frames can cause endless headaches by making item frames
	 * show items that are unobtainable. This function attempts to correct the issue
	 * by finding any duplicate, empty frames and removing them.
	 */
	public static void checkForEmptyDuplicateFrames(ItemFrameStorage frame) {
		if (frame.noFrame())
			return;
		Chunk chunk = frame.getLocation().getChunk();
		ArrayList<Entity> removed = new ArrayList<Entity>();
		HashMap<Integer, Boolean> droppedItems = new HashMap<Integer, Boolean>();

		try {
			if (!frame.isEmpty())
				droppedItems.put(ItemManager.getTypeId(frame.getItem()), true);
		} catch (CivException e1) {
			e1.printStackTrace();
		}
		for (Entity entity : chunk.getEntities()) {
			if (entity instanceof ItemFrame) {
				if (frame.isOurEntity(entity))
					continue;
				int x = frame.getLocation().getBlockX();
				int y = frame.getLocation().getBlockY();
				int z = frame.getLocation().getBlockZ();
				if (x == entity.getLocation().getBlockX() && y == entity.getLocation().getBlockY()
						&& z == entity.getLocation().getBlockZ()) {
					// We have found a duplicate item frame here.
					ItemFrame eFrame = (ItemFrame) entity;
					boolean eFrameEmpty = (eFrame.getItem() == null || eFrame.getItem().getType().equals(Material.AIR));
					if (!eFrameEmpty) {
						Boolean droppedAlready = droppedItems.get(ItemManager.getTypeId(eFrame.getItem()));
						if (droppedAlready == null || droppedAlready == false) {
							droppedItems.put(ItemManager.getTypeId(eFrame.getItem()), true);
							eFrame.getLocation().getWorld().dropItemNaturally(eFrame.getLocation(), eFrame.getItem());
						}
					}
					removed.add(eFrame);
				}
			}
		}
		for (Entity e : removed) {
			e.remove();
		}
	}

	public static Entity getEntityAtLocation(Location loc) {
		Chunk chunk = loc.getChunk();
		for (Entity entity : chunk.getEntities()) {
			if (entity.getLocation().getBlock().equals(loc.getBlock())) {
				return entity;
			}
		}
		return null;
	}

	public static Entity getEntityClassFromUUID(World world, Class<?> c, UUID id) {
		for (Entity e : world.getEntitiesByClasses(c)) {
			if (e.getUniqueId().equals(id))
				return e;
		}
		return null;
	}

	public static Date getNextRandomEventTime() {
		EventTimer repo = EventTimer.timers.get("random");
		return repo.getNext().getTime();
	}

	public static Date getNextRepoTime() {
		EventTimer repo = EventTimer.timers.get("repo-goodies");
		return repo.getNext().getTime();
	}

	public static boolean allowDemolishOutPost() {
		final long now = Calendar.getInstance().getTimeInMillis();
		return now < CivGlobal.cantDemolishFrom || now > CivGlobal.cantDemolishUntil;
	}

	public static void setAggressor(Civilization civ, Civilization otherCiv, Civilization aggressor) {
		civ.getDiplomacyManager().setAggressor(aggressor, otherCiv);
		otherCiv.getDiplomacyManager().setAggressor(aggressor, civ);
	}

	public static void setRelation(Civilization civ, Civilization otherCiv, Status status) {
		if (civ.getId() == otherCiv.getId())
			return;
		civ.getDiplomacyManager().setRelation(otherCiv, status, null);
		otherCiv.getDiplomacyManager().setRelation(civ, status, null);
		String out = "";
		switch (status) {
		case NEUTRAL:
			out += CivColor.LightGray + CivSettings.localize.localizedString("civGlobal_relation_Neutral")
					+ CivColor.White;
			break;
		case HOSTILE:
			out += CivColor.Yellow + CivSettings.localize.localizedString("civGlobal_relation_Hostile")
					+ CivColor.White;
			break;
		case WAR:
			out += CivColor.Rose + CivSettings.localize.localizedString("civGlobal_relation_War") + CivColor.White;
			break;
		case PEACE:
			out += CivColor.LightGreen + CivSettings.localize.localizedString("civGlobal_relation_Peace")
					+ CivColor.White;
			break;
		case ALLY:
			out += CivColor.Green + CivSettings.localize.localizedString("civGlobal_relation_Allied") + CivColor.White;
			break;
		default:
			break;
		}
		CivMessage.global(CivSettings.localize.localizedString("var_civGlobal_relation_isNow", civ.getName(), out,
				otherCiv.getName()));
		CivGlobal.updateTagsBetween(civ, otherCiv);
	}

	private static void updateTagsBetween(Civilization civ, Civilization otherCiv) {
//		TaskMaster.asyncTask(new Runnable() {
//			@Override
//			public void run() {
//				Set<Player> civList = new HashSet<Player>();
//				Set<Player> otherCivList = new HashSet<Player>();
//
//				for (Player player : Bukkit.getOnlinePlayers()) {
//					Resident resident = CivGlobal.getResident(player);
//					if (resident == null || !resident.hasTown()) continue;
//					if (resident.getTown().getCiv() == civ) {
//						civList.add(player);
//					} else
//						if (resident.getTown().getCiv() == otherCiv) otherCivList.add(player);
//				}
//				TaskMaster.syncTask(new Runnable() {
//					@Override
//					public void run() {
//						if (CivSettings.hasITag) {
//							for (Player player : civList) {
//								if (!otherCivList.isEmpty()) iTag.getInstance().refreshPlayer(player, otherCivList);
//							}
//							for (Player player : otherCivList) {
//								if (!civList.isEmpty()) iTag.getInstance().refreshPlayer(player, civList);
//							}
//						}
//					}
//				});
//			}
//		}, 0);
	}

	public static String updateTagColor(Player namedPlayer, Player player) {
		Resident namedRes = CivGlobal.getResident(namedPlayer);
		Resident playerRes = CivGlobal.getResident(player);

		if (CivGlobal.isMutualOutlaw(namedRes, playerRes))
			return CivColor.Red + namedPlayer.getName();
		if (namedRes == null || !namedRes.hasTown())
			return namedPlayer.getName();
		if (playerRes == null || !playerRes.hasTown())
			return namedPlayer.getName();

		String color = CivColor.White;
		if (namedRes.getTown().getCiv() == playerRes.getTown().getCiv()) {
			color = CivColor.LightGreen;
		} else {
			Relation.Status status = playerRes.getTown().getCiv().getDiplomacyManager()
					.getRelationStatus(namedRes.getTown().getCiv());
			switch (status) {
			case PEACE:
				color = CivColor.LightBlue;
				break;
			case ALLY:
				color = CivColor.LightGreen;
				break;
			case HOSTILE:
				color = CivColor.Yellow;
				break;
			case WAR:
				color = CivColor.Rose;
				break;
			default:
				break;
			}
		}
		return color + namedPlayer.getName();
	}

	public static void checkForExpiredRelations() {
		Date now = new Date();
		ArrayList<Relation> deletedRelations = new ArrayList<Relation>();
		for (Civilization civ : CivGlobal.getCivs()) {
			for (Relation relation : civ.getDiplomacyManager().getRelations()) {
				if (relation.getExpireDate() != null && now.after(relation.getExpireDate()))
					deletedRelations.add(relation);
			}
		}
		for (Relation relation : deletedRelations) {
			// relation.getCiv().getDiplomacyManager().deleteRelation(relation);
			try {
				relation.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean willInstantBreak(Material type) {

		switch (type) {
		case BED_BLOCK:
		case BROWN_MUSHROOM:
		case CROPS:
		case DEAD_BUSH:
		case DIODE:
		case DIODE_BLOCK_OFF:
		case DIODE_BLOCK_ON:
		case FIRE:
		case FLOWER_POT:
		case FLOWER_POT_ITEM:
		case GLASS:
		case GRASS:
		case LEAVES:
		case LEVER:
		case LONG_GRASS:
		case MELON_STEM:
		case NETHER_STALK:
		case NETHER_WARTS:
		case PUMPKIN_STEM:
		case REDSTONE:
		case REDSTONE_TORCH_OFF:
		case REDSTONE_TORCH_ON:
		case REDSTONE_WIRE:
		case SAPLING:
		case SKULL:
		case SKULL_ITEM:
		case SNOW:
		case SUGAR_CANE_BLOCK:
		case THIN_GLASS:
		case TNT:
		case TORCH:
		case TRIPWIRE:
		case TRIPWIRE_HOOK:
		case VINE:
		case WATER_LILY:
		case YELLOW_FLOWER:
			return true;
		default:
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public static OfflinePlayer getFakeOfflinePlayer(String name) {
		return Bukkit.getOfflinePlayer(name);
	}

	public static Integer getScoreForCiv(Civilization civ) {
		for (Entry<Integer, Civilization> entry : civilizationScores.entrySet()) {
			if (civ == entry.getValue()) {
				return entry.getKey();
			}
		}
		return 0;
	}

	public static ArrayList<String> getNearbyPlayers(BlockCoord coord, double range) {
		ArrayList<String> playerNames = new ArrayList<String>();

		// TODO make it async....
		// for (PlayerLocation)

		return playerNames;
	}

	public static boolean isMutualOutlaw(Resident defenderResident, Resident attackerResident) {

		if (defenderResident == null || attackerResident == null) {
			return false;
		}

		if (defenderResident.hasTown() && defenderResident.getTown().isOutlaw(attackerResident.getName())) {
			return true;
		}

		if (attackerResident.hasTown() && attackerResident.getTown().isOutlaw(defenderResident.getName())) {
			return true;
		}

		return false;
	}

	public static boolean isOutlawHere(Resident resident, TownChunk tc) {
		if (tc == null) {
			return false;
		}

		if (tc.getTown() == null) {
			return false;
		}

		if (tc.getTown().isOutlaw(resident.getName())) {
			return true;
		}
		return false;
	}

	public static Date getTodaysSpawnRegenDate() {
		Calendar now = Calendar.getInstance();
		Calendar nextSpawn = Calendar.getInstance();

		int hourOfDay;
		try {
			hourOfDay = CivSettings.getInteger(CivSettings.civConfig, "global.regen_spawn_hour");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return null;
		}

		nextSpawn.set(Calendar.HOUR_OF_DAY, hourOfDay);
		nextSpawn.set(Calendar.MINUTE, 0);
		nextSpawn.set(Calendar.SECOND, 0);

		if (nextSpawn.after(now)) {
			return nextSpawn.getTime();
		}

		nextSpawn.add(Calendar.DATE, 1);
		nextSpawn.set(Calendar.HOUR_OF_DAY, hourOfDay);
		nextSpawn.set(Calendar.MINUTE, 0);
		nextSpawn.set(Calendar.SECOND, 0);

		return nextSpawn.getTime();
	}

	public static String getPhase() {
		try {
			return CivSettings.getStringBase("server_phase");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return "old";
		}
	}

	public static boolean isCasualMode() {
		try {
			String mode = CivSettings.getString(CivSettings.civConfig, "global.casual_mode");
			if (mode.equalsIgnoreCase("true")) {
				return true;
			} else {
				return false;
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return false;
		}
	}

	// --------------- Economy
	public static Economy getEconomy() {
		return econ == null ? (econ = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider()) : econ;
	}

	public static EconObject createEconObject(SQLObject holder) {
		if (useEconomy && holder instanceof Resident) {
			return new VaultEconObject(holder, ((Resident) holder).getUid());
		}
		return new EconObject(holder);
	}

	public static String getNameTagColor(final Civilization civ) {
		if (civ.isAdminCiv()) {
			return "§c";
		}
		switch (civ.getCurrentEra()) {
		case 0: {
			return "§f";
		}
		case 1: {
			return "§e";
		}
		case 2: {
			return "§d";
		}
		case 3: {
			return "§a";
		}
		case 4: {
			return "§6";
		}
		case 5: {
			return "§2";
		}
		case 6: {
			return "§b";
		}
		default: {
			return "§5";
		}
		}
	}

	public static void addCampAppearance(final Resident resident) {
		try {
			getPlayer(resident);
		} catch (CivException offline) {
			return;
		}
		if (resident.getCamp() == null) {
			return;
		}
		resident.getCamp();
	}

	public static String getFullNameTag(final Player var1) {
		final String full = TagManager.hash.get(var1.getName());
		return ((full != null) ? full : var1.getName()) + TagManager.reset;
	}

	public static boolean anybodyHasTag(final String tag) {
		for (final Civilization civ : getCivs()) {
			if (civ.getTag().equalsIgnoreCase(tag))
				return true;
		}
		return false;
	}

	public static void sessionAdd(final String key, final String value) {
		getSessionDatabase().add(key, value, 0, 0, 0);
	}

	public static void sessionUpdate(final String key, final String value, final int requestId) {
		getSessionDatabase().update(requestId, key, value);
	}

	public static long getTeleportCooldown(final String desc, final Player user) {
		long cooldown = 0L;
		final String key = "teleportCooldown_" + desc + "_" + user.getUniqueId();
		final ArrayList<SessionEntry> entries = getSessionDatabase().lookup(key);
		if (entries == null || entries.size() < 1) {
			return cooldown;
		}
		final SessionEntry cd = entries.get(0);
		cooldown = Long.parseLong(cd.value);
		return cooldown;
	}

	public static void setTeleportCooldown(final String desc, final int minutes, final Player user) {
		final String key = "teleportCooldown_" + desc + "_" + user.getUniqueId();
		final String value = Calendar.getInstance().getTimeInMillis() + 60000 * minutes + "";
		final ArrayList<SessionEntry> entries = getSessionDatabase().lookup(key);
		if (entries == null || entries.size() < 1) {
			sessionAdd(key, value);
			return;
		}
		sessionUpdate(key, value, entries.get(0).request_id);
	}

	public static boolean isChatDisAllowed(Player player) {
		// TODO Auto-generated method stub
		return false;
	}

	public static String getTimeString() {
		String time = "";
		if (War.isWarTime()) {
			time = time + CivColor.RedBold + "\u0412\u043e\u0439\u043d\u0430! ";
		}
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy ");
		dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
		time = time + CivColor.GoldBold + dateFormat.format(Calendar.getInstance().getTime());
		dateFormat.applyPattern("HH:mm:ss '\u041c\u0421\u041a'");
		time = time + CivColor.YellowBold + dateFormat.format(Calendar.getInstance().getTime());
		return time;
	}

	public static String getDynmapLink(final String server) {
		if ("Columba".equals(server)) {
			return "http://95.216.74.3:5551";
		}
		return "https://wiki.furnex.ru/index.php?title=Введение\u041a\u0430\u0440\u0442\u044b_\u0441\u0435\u0440\u0432\u0435\u0440\u043e\u0432";
	}
}
