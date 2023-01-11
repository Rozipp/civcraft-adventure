package com.avrgaming.civcraft.main;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.*;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.construct.constructs.WarCamp;
import com.avrgaming.civcraft.construct.farm.FarmChunk;
import com.avrgaming.civcraft.construct.farm.FarmPreCachePopulateTimer;
import com.avrgaming.civcraft.construct.structures.Market;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.titles.Title;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.sessiondb.SessionDatabase;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.CultureProcessAsyncTask;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.TagManager;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

//import com.avrgaming.civcraft.construct.caves.Cave;

public class CivGlobal {

	public static double LIGHTHOUSE_WATER_PLAYER_SPEED = 1.5;
	public static double LIGHTHOUSE_WATER_BOAT_SPEED = 1.1;

	private static boolean useEconomy;
	// XXX economy public static Economy econ;

	public static SimpleDateFormat dateFormat;

	private static final Map<String, Resident> residents = new ConcurrentHashMap<>();
	private static final Map<UUID, Resident> residentsViaUUID = new ConcurrentHashMap<>();
	private static final Map<Integer, UnitObject> unitObjects = new ConcurrentHashMap<>();

	private static final Map<Integer, Town> towns = new ConcurrentHashMap<>();
	private static final Map<Integer, Civilization> civs = new ConcurrentHashMap<>();
	private static final Map<Integer, Coalition> coalitions = new ConcurrentHashMap<>();
	private static final Map<String, Civilization> conqueredCivs = new ConcurrentHashMap<>();
	private static final Map<String, Civilization> adminCivs = new ConcurrentHashMap<>();
	private static final Map<ChunkCoord, TownChunk> townChunks = new ConcurrentHashMap<>();
	private static final Map<ChunkCoord, CultureChunk> cultureChunks = new ConcurrentHashMap<>();

	private static final Map<BlockCoord, Title> titles = new ConcurrentHashMap<>();
	private static final Map<BlockCoord, Structure> structures = new ConcurrentHashMap<>();
	private static final Map<BlockCoord, Wonder> wonders = new ConcurrentHashMap<>();
	private static final Map<BlockCoord, ConstructBlock> constructBlocks = new ConcurrentHashMap<>();
	private static final Map<ChunkCoord, HashSet<Construct>> constructsInChunk = new ConcurrentHashMap<>();
	private static final Map<BlockCoord, ConstructSign> constructSigns = new ConcurrentHashMap<>();
	private static final Map<BlockCoord, ConstructChest> constructChests = new ConcurrentHashMap<>();
	private static final Map<ChunkCoord, FarmChunk> farmChunks = new ConcurrentHashMap<>();
	private static final Map<String, Camp> camps = new ConcurrentHashMap<>();
	private static final Map<BlockCoord, Market> markets = new ConcurrentHashMap<>();
	public static HashSet<String> researchedTechs = new HashSet<>();
	private static final Map<Integer, Report> reports = new HashMap<>();

	public static void addReport(final Report report) {
		CivGlobal.reports.put(report.getId(), report);
	}

	public static Map<Integer, Boolean> CivColorInUse = new ConcurrentHashMap<>();

	// TODO fix the duplicate score issue...
	public static TreeMap<Integer, Civilization> civilizationScores = new TreeMap<>();
	public static TreeMap<Integer, Town> townScores = new TreeMap<>();

	public static HashMap<String, Date> playerFirstLoginMap = new HashMap<>();
	public static HashSet<String> banWords = new HashSet<>();

	// public static Scoreboard globalBoard;

	public static Integer maxPlayers = -1;
	public static String fullMessage = "";

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

	public static ArrayList<Town> orphanTowns = new ArrayList<>();
	public static ArrayList<Civilization> orphanCivs = new ArrayList<>();

	public static boolean checkForBooks = true;
	public static boolean debugDateBypass = false;
	public static boolean endWorld = false;

	public static int highestCivEra = 0;

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
			CivMessage.globalTitle(CivColor.Green + localizedEraString(highestCivEra), CivColor.LightGreen + CivSettings.localize.localizedString("var_announce_newEraCiv", civ.getName()));

		}
	}

	public static void loadGlobals() throws SQLException, CivException {
		/* Don't use CivSettings.getBoolean() to prevent error when using old config Must be loaded before residents are loaded */
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
		loadBuildable();
//		loadCaves();
		loadRandomEvents();
		loadUnitObjects();
		loadReports();
		EventTimer.loadGlobalEvents();
		EndGameCondition.init();
		War.init();

		CivLog.heading("--- Done <3 ---");

		/* Load in upgrades after all of our objects are loaded, resolves dependencies */
		processUpgrades();
		processCulture();

		/* Check for orphan civs now */
		for (Civilization civ : civs.values()) {
			if (civ.getCapitol() == null) orphanCivs.add(civ);
		}

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

		loadCompleted = true;
	}

	private static void processUpgrades() {
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

//	private static void loadCaves() throws SQLException {
//		Connection context = null;
//		ResultSet rs = null;
//		PreparedStatement ps = null;
//
//		try {
//			context = SQL.getGameConnection();
//			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Cave.TABLE_NAME);
//			rs = ps.executeQuery();
//
//			while (rs.next()) {
//				Cave cave;
//				try {
//					cave = new Cave(rs);
//					caves.put(cave.getCornerEntrance().getChunkCoord(), cave);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//
//			CivLog.info("Loaded " + caves.size() + " Caves");
//		} finally {
//			SQL.close(rs, ps, context);
//		}
//	}

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
					towns.put(town.getId(), town);
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
					CivGlobal.addConstruct(new Camp(rs));
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

	public static void loadBuildable() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Buildable.TABLE_NAME);
			rs = ps.executeQuery();

			while (rs.next()) {
				try {
					CivGlobal.addConstruct(Buildable.newBuildable(rs));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			CivLog.info("Loaded " + structures.size() + " Structures");
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
		Player player = Bukkit.getPlayer(resident.getUuid());
		if (player == null) throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noPlayer", resident.getName()));
		return player;
	}

	public static Player getPlayer(UUID uuid) throws CivException {
		Player player = Bukkit.getPlayer(uuid);
		if (player == null) throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noPlayer", uuid.toString()));
		return player;
	}

	public static Player getPlayer(String name) throws CivException {
		Resident res = CivGlobal.getResident(name);
		if (res == null) throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noResident", name));
		return getPlayer(res);
	}

	// ---------- Resident
	public static void addResident(Resident res) {
		residents.put(res.getName(), res);
		residentsViaUUID.put(res.getUuid(), res);
	}

	public static void removeResident(Resident res) {
		residents.remove(res.getName());
		residentsViaUUID.remove(res.getUuid());
	}

	public static Resident getResident(String name) {
		return residents.get(name);
	}

	public static Resident getResident(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if (player != null)
			return getResident(player);
		else
			return null;
	}

	public static Resident getResident(Player player) {
		return getResident(player.getName());
	}

	public static Resident getResidentViaUUID(UUID uuid) {
		return residentsViaUUID.get(uuid);
	}

	/** make lookup via ID faster(use hashtable) */
	public static Resident getResidentFromId(int id) {
		for (Resident resident : residents.values()) {
			if (resident.getId() == id) return resident;
		}
		return null;
	}

	public static Collection<Resident> getResidents() {
		return residents.values();
	}

	// --------------- Town
	public static void addTown(Town town) {
		towns.put(town.getId(), town);
	}

	public static void removeTown(Town town) {
		towns.remove(town.getId());
	}

	public static Town getTownFromName(String name) {
		if (name == null || name.isEmpty()) return null;
		for (Town town : towns.values())
			if (town.getName().equalsIgnoreCase(name)) return town;
		return null;
	}

	public static Collection<Town> getTowns() {
		return towns.values();
	}

	public static Town getTown(int id) {
		return towns.get(id);
	}

	// -------------- TownChunk
	public static void addTownChunk(TownChunk tc) {
		townChunks.put(tc.getChunkCoord(), tc);
	}

	public static void removeTownChunk(TownChunk tc) {
		if (tc.getChunkCoord() != null) townChunks.remove(tc.getChunkCoord());
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
		civs.put(civ.getId(), civ);
		if (civ.isAdminCiv()) addAdminCiv(civ);
	}

	public static void removeCiv(Civilization civ) {
		civs.remove(civ.getId());
		// TODO протестить надо
		int cid = civ.getDiplomacyManager().getCoalitionId();
		if (cid != 0) coalitions.get(cid).removeCiv(cid);
		if (civ.isAdminCiv()) removeAdminCiv(civ);
	}

	public static Civilization getCivFromName(String name) {
		if (name == null || name.isEmpty()) return null;
		for (Civilization civ : civs.values())
			if (civ.getName().equalsIgnoreCase(name)) return civ;
		return null;
	}

	public static Civilization getCiv(int id) {
		return civs.get(id);
	}

	public static Collection<Civilization> getCivs() {
		return civs.values();
	}

	/* Gets a TreeMap of the civilizations sorted based on the distance to the provided town. Ignores the civilization the town belongs to. */
	public static TreeMap<Double, Civilization> findNearestCivilizations(Town town) {
		Location townLoc = town.getLocation();
		TreeMap<Double, Civilization> returnMap = new TreeMap<>();
		if (townLoc == null) return returnMap;
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ == town.getCiv()) continue;
			// Get shortest distance of any of this civ's towns.
			double shortestDistance = Double.MAX_VALUE;
			for (Town t : civ.getTowns()) {
				Location tempTownLoc = t.getLocation();
				if (tempTownLoc == null) continue;
				double tmpDistance = tempTownLoc.distanceSquared(townLoc);
				if (tmpDistance < shortestDistance) shortestDistance = tmpDistance;
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
			if (civ.getId() == id) return civ;
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

	// ---------------- Buildable
	public static void addConstruct(Construct construct) {
		if (construct instanceof Structure)
			structures.put(construct.getCorner(), (Structure) construct);
		else if (construct instanceof Title)
			titles.put(construct.getCorner(), (Title) construct);
		else if (construct instanceof Wonder)
			wonders.put(construct.getCorner(), (Wonder) construct);
		else if (construct instanceof Camp)
			camps.put(construct.getName().toLowerCase(), (Camp) construct);
	}

	public static void removeConstruct(Construct construct) {
		if (construct instanceof Structure)
			structures.remove(construct.getCorner());
		else if (construct instanceof Title)
			titles.remove(construct.getCorner());
		else if (construct instanceof Wonder)
			wonders.remove(construct.getCorner());
		else if (construct instanceof Camp)
			camps.remove(construct.getName().toLowerCase());
	}

	public static Buildable getNearestBuildable(Location location) {
		Buildable nearest = null;
		double lowest_distance = Double.MAX_VALUE;
		for (Structure struct : structures.values()) {
			double distance = struct.getCenterLocation().distance(location);
			if (distance < lowest_distance) {
				lowest_distance = distance;
				nearest = struct;
			}
		}
		for (Title title : titles.values()) {
			double distance = title.getCenterLocation().distance(location);
			if (distance < lowest_distance) {
				lowest_distance = distance;
				nearest = title;
			}
		}
		for (Wonder wonder : wonders.values()) {
			double distance = wonder.getCenterLocation().distance(location);
			if (distance < lowest_distance) {
				lowest_distance = distance;
				nearest = wonder;
			}
		}
		return nearest;
	}

	// ---------------- Structure

	public static Collection<Title> getTitles() {
		return titles.values();
	}

	// ---------------- Structure

	public static Collection<Structure> getStructures() {
		return structures.values();
	}

	public static Iterator<Entry<BlockCoord, Structure>> getStructureIterator() {
		return structures.entrySet().iterator();
	}

	// --------------- Wonder
	public static Collection<Wonder> getWonders() {
		return wonders.values();
	}

	public static Wonder getWonderByConfigId(String id) {
		for (Wonder wonder : wonders.values()) {
			if (wonder.getConfigId().equals(id)) return wonder;
		}
		return null;
	}

	// ------------ Camp
	public static Camp getCamp(String name) {
		return camps.get(name.toLowerCase());
	}

	public static Camp getCampFromId(int campID) {
		for (Camp camp : camps.values()) {
			if (camp.getId() == campID) return camp;
		}
		return null;
	}

	public static Collection<Camp> getCamps() {
		return camps.values();
	}


	// ---------------- ConstructBlock
	public static void addConstructBlock(BlockCoord coord, Construct owner, boolean damageable) {
		ConstructBlock sb = new ConstructBlock(coord, owner);
		sb.setDamageable(damageable);
		constructBlocks.put(coord, sb);

		ChunkCoord cc = new ChunkCoord(coord);
		HashSet<Construct> constructs = constructsInChunk.get(cc);
		if (constructs == null) constructs = new HashSet<>();
		if (constructs.contains(owner)) return;
		constructs.add(owner);
		constructsInChunk.put(cc, constructs);
	}

	public static void removeConstructBlock(BlockCoord coord) {
		ConstructBlock bb = constructBlocks.get(coord);
		if (bb == null) return;
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

	public static Set<Construct> getConstructsFromChunk(ChunkCoord cc) {
		if (!constructsInChunk.containsKey(cc)) return new HashSet<>();
		return constructsInChunk.get(cc);
	}

	public static Construct getConstructFromChunk(ChunkCoord cc) {
		if (!constructsInChunk.containsKey(cc)) return null;
		return constructsInChunk.get(cc).stream().iterator().next();
	}

	public static Set<Construct> getConstructsFromChunk(BlockCoord coord) {
		return getConstructsFromChunk(new ChunkCoord(coord));
	}

	public static Camp getCampAt(ChunkCoord cc) {
		for (Construct constr : getConstructsFromChunk(cc))
			if (constr instanceof Camp) return (Camp) constr;
		return null;
	}

	public static Buildable getBuildableAt(ChunkCoord cc) {
		for (Construct constr : getConstructsFromChunk(cc))
			if (constr instanceof Buildable) return (Buildable) constr;
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

	// ------------ FarmChunk
	public static void addFarmChunk(ChunkCoord coord, FarmChunk fc) {
		farmChunks.put(coord, fc);
		FarmPreCachePopulateTimer.queueFarmChunk(fc);
	}

	public static FarmChunk getFarmChunk(ChunkCoord coord) {
		return farmChunks.get(coord);
	}

	public static boolean farmChunkValid(FarmChunk fc) {
		return farmChunks.containsKey(fc.getCCoord());
	}

	public static Collection<FarmChunk> getFarmChunks() {
		return farmChunks.values();
	}

	public static void removeFarmChunk(ChunkCoord coord) {
		FarmChunk fc = getFarmChunk(coord);
		if (fc != null) FarmPreCachePopulateTimer.dequeueFarmChunk(fc);
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

	// ------------ Cave
//	public static void addCave(Cave cave) {
//		caves.put(cave.getCornerEntrance().getChunkCoord(), cave);
//	}
//
//	public static void removeCave(ChunkCoord ccoord) {
//		caves.remove(ccoord);
//	}
//
//	public static Cave getCave(ChunkCoord ccoord) {
//		return caves.get(ccoord);
//	}
//
//	public static Cave getCaveFromId(int id) {
//		for (Cave cave : caves.values()) {
//			if (cave.getMaterialId() == id) return cave;
//		}
//		return null;
//	}
//
//	public static Collection<Cave> getCaves() {
//		return caves.values();
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

	// --------------- Coalition
	public static void addCoalition(Coalition coal) {
		coalitions.put(coal.getId(), coal);
		Coalition.message("Цивилизация " + coal.getCreator().getName() + " объявила о создании коалиции под названием " + coal.getName());
	}

	public static Coalition getCoalition(String name) {
		for (Coalition c : coalitions.values()) {
			if (c.getName().equalsIgnoreCase(name)) return c;
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
			if (report.isClosed() && report.getCloseTime() == closeTime) return report;
		}
		return null;
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
			if (CivSettings.getStringBase(flagname).equalsIgnoreCase("true")) return true;
		} catch (InvalidConfiguration ignored) {}
		return false;
	}

	public static boolean hasTimeElapsed(SessionEntry se, double seconds) {
		long now = System.currentTimeMillis();
		int secondsBetween = getSecondsBetween(se.time, now);

		// First determine the time between two events.
		return !(secondsBetween < seconds);
	}

	public static Date getNextUpkeepDate() {

		EventTimer daily = EventTimer.timers.get("daily");
		return daily.getNext().getTime();

		// int upkeepHour;
		// try {
		// upkeepHour = CivSettings.getInteger(CivSettings.civConfig, "global.daily_upkeep_hour");
		// } catch (InvalidConfiguration e) {
		// e.printStackTrace();
		// return null;
		// }
		//
		// Calendar c = Calendar.getInstance();
		// Date now = c.getTime();
		//
		// c.set(Calendar.HOUR_OF_DAY, upkeepHour);
		// c.set(Calendar.MINUTE, 0);
		// c.set(Calendar.SECOND, 0);
		//
		// if (now.after(c.getTime())) {
		// c.add(Calendar.DATE, 1);
		// }
		//
		// return c.getTime();
	}

	public static Date getNextHourlyTickDate() {
		EventTimer hourly = EventTimer.timers.get("hourly");
		return hourly.getNext().getTime();
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
			if (e.getUniqueId().equals(id)) return e;
		}
		return null;
	}

	public static Date getNextRandomEventTime() {
		EventTimer repo = EventTimer.timers.get("random");
		return repo.getNext().getTime();
	}

	public static void setAggressor(Civilization civ, Civilization otherCiv, Civilization aggressor) {
		civ.getDiplomacyManager().setAggressor(aggressor, otherCiv);
		otherCiv.getDiplomacyManager().setAggressor(aggressor, civ);
	}

	public static void setRelation(Civilization civ, Civilization otherCiv, Status status) {
		if (civ.getId() == otherCiv.getId()) return;
		civ.getDiplomacyManager().setRelation(otherCiv, status, null);
		otherCiv.getDiplomacyManager().setRelation(civ, status, null);
		String out = "";
		switch (status) {
		case NEUTRAL:
			out += CivColor.LightGray + CivSettings.localize.localizedString("civGlobal_relation_Neutral") + CivColor.White;
			break;
		case HOSTILE:
			out += CivColor.Yellow + CivSettings.localize.localizedString("civGlobal_relation_Hostile") + CivColor.White;
			break;
		case WAR:
			out += CivColor.Rose + CivSettings.localize.localizedString("civGlobal_relation_War") + CivColor.White;
			break;
		case PEACE:
			out += CivColor.LightGreen + CivSettings.localize.localizedString("civGlobal_relation_Peace") + CivColor.White;
			break;
		case ALLY:
			out += CivColor.Green + CivSettings.localize.localizedString("civGlobal_relation_Allied") + CivColor.White;
			break;
		default:
			break;
		}
		CivMessage.global(CivSettings.localize.localizedString("var_civGlobal_relation_isNow", civ.getName(), out, otherCiv.getName()));
		CivGlobal.updateTagsBetween(civ, otherCiv);
	}

	private static void updateTagsBetween(Civilization civ, Civilization otherCiv) {
		// TaskMaster.asyncTask(new Runnable() {
		// @Override
		// public void run() {
		// Set<Player> civList = new HashSet<Player>();
		// Set<Player> otherCivList = new HashSet<Player>();
		//
		// for (Player player : Bukkit.getOnlinePlayers()) {
		// Resident resident = CivGlobal.getResident(player);
		// if (resident == null || !resident.hasTown()) continue;
		// if (resident.getTown().getCiv() == civ) {
		// civList.add(player);
		// } else
		// if (resident.getTown().getCiv() == otherCiv) otherCivList.add(player);
		// }
		// TaskMaster.syncTask(new Runnable() {
		// @Override
		// public void run() {
		// if (CivSettings.hasITag) {
		// for (Player player : civList) {
		// if (!otherCivList.isEmpty()) iTag.getInstance().refreshPlayer(player, otherCivList);
		// }
		// for (Player player : otherCivList) {
		// if (!civList.isEmpty()) iTag.getInstance().refreshPlayer(player, civList);
		// }
		// }
		// }
		// });
		// }
		// }, 0);
	}

	public static String updateTagColor(Player namedPlayer, Player player) {
		Resident namedRes = CivGlobal.getResident(namedPlayer);
		Resident playerRes = CivGlobal.getResident(player);

		if (CivGlobal.isMutualOutlaw(namedRes, playerRes)) return CivColor.Red + namedPlayer.getName();
		if (namedRes == null || !namedRes.hasTown()) return namedPlayer.getName();
		if (playerRes == null || !playerRes.hasTown()) return namedPlayer.getName();

		String color = CivColor.White;
		if (namedRes.getTown().getCiv() == playerRes.getTown().getCiv()) {
			color = CivColor.LightGreen;
		} else {
			Relation.Status status = playerRes.getTown().getCiv().getDiplomacyManager().getRelationStatus(namedRes.getTown().getCiv());
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
		ArrayList<Relation> deletedRelations = new ArrayList<>();
		for (Civilization civ : CivGlobal.getCivs()) {
			for (Relation relation : civ.getDiplomacyManager().getRelations()) {
				if (relation.getExpireDate() != null && now.after(relation.getExpireDate())) deletedRelations.add(relation);
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

	public static Integer getScoreForCiv(Civilization civ) {
		for (Entry<Integer, Civilization> entry : civilizationScores.entrySet()) {
			if (civ == entry.getValue()) {
				return entry.getKey();
			}
		}
		return 0;
	}

	public static ArrayList<String> getNearbyPlayers(BlockCoord coord, double range) {
		ArrayList<String> playerNames = new ArrayList<>();

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

		return tc.getTown().isOutlaw(resident.getName());
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
			return mode.equalsIgnoreCase("true");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return false;
		}
	}

	// --------------- Economy
	// XXX economy
	// public static Economy getEconomy() {
	// return econ == null ? (econ = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider()) : econ;
	// }

	public static EconObject createEconObject(SQLObject holder) {
		if (useEconomy && holder instanceof Resident) {
			// XXX economy return new VaultEconObject(holder, ((Resident) holder).getUid());
			return new EconObject(holder);
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

	public static String getFullNameTag(final Player var1) {
		final String full = TagManager.hash.get(var1.getName());
		return ((full != null) ? full : var1.getName()) + TagManager.reset;
	}

	public static boolean anybodyHasTag(final String tag) {
		for (final Civilization civ : getCivs()) {
			if (civ.getTag().equalsIgnoreCase(tag)) return true;
		}
		return false;
	}

	public static void sessionAdd(final String key, final String value) {
		getSessionDatabase().add(key, value, 0, 0, 0);
	}

	public static void sessionUpdate(final String key, final String value, final int requestId) {
		getSessionDatabase().update(requestId, key, value);
	}

	public static long getTeleportCooldown(final String desc, final Resident res) {
		long cooldown = 0L;
		final String key = "teleportCooldown_" + desc + "_" + res.getUuid();
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

	public static String getTimeString() {
		String time = "";
		if (War.isWarTime()) {
			time = time + CivColor.RedBold + CivSettings.localize.localizedString("WAR") + "! ";
		}
		final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy ");
		dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
		time = time + CivColor.GoldBold + dateFormat.format(Calendar.getInstance().getTime());
		dateFormat.applyPattern("HH:mm:ss 'МСК'");
		time = time + CivColor.YellowBold + dateFormat.format(Calendar.getInstance().getTime());
		return time;
	}

	public static String getDynmapLink(final String server) {
		if ("Columba".equals(server)) {
			return "FIXME"; // FIXME
		}
		return "FIXME";
	}
}