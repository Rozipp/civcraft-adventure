/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.components.AttributeBase;
import com.avrgaming.civcraft.components.AttributeRate;
import com.avrgaming.civcraft.components.AttributeWarUnhappiness;
import com.avrgaming.civcraft.components.AttributeWarUnpkeep;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigHappinessState;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Cityhall;
import com.avrgaming.civcraft.structure.Mine;
import com.avrgaming.civcraft.structure.MineHouse;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Temple;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncUpdateTags;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitInventory;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.TagManager;
import com.avrgaming.civcraft.war.War;

@Getter
@Setter
public class Town extends SQLObject {

	/* Time it takes before a new attribute is calculated Otherwise its loaded from the cache. */
	public static final int ATTR_TIMEOUT_SECONDS = 5;

	ConcurrentHashMap<String, Resident> residents = new ConcurrentHashMap<String, Resident>();
	private ConcurrentHashMap<String, Resident> fakeResidents = new ConcurrentHashMap<String, Resident>();

	private ConcurrentHashMap<ChunkCoord, TownChunk> townChunks = new ConcurrentHashMap<ChunkCoord, TownChunk>();
	private ConcurrentHashMap<ChunkCoord, CultureChunk> cultureChunks = new ConcurrentHashMap<ChunkCoord, CultureChunk>();

	public UnitInventory unitInventory = new UnitInventory(this);

	private int level;
	private Civilization civ;
	private Civilization motherCiv;
	private int daysInDebt;

	/* Hammers */
	private double baseHammers = 1.0;
	private double extraHammers;

	/* Culture */
	private int culture;

	/* Beakers */
	private double unusedBeakers;

	/* Happiness Stuff */
	private double baseHappy = 0.0;
	private double baseUnhappy = 0.0;
	private double baseGrowth = 0.0;

	public ArrayList<TownChunk> savedEdgeBlocks = new ArrayList<TownChunk>();
	public HashSet<Town> townTouchList = new HashSet<Town>();

	public TownGroupManager GM;
	public TownBuildableManager SM;
	private BuffManager buffManager = new BuffManager();

	private EconObject treasury;
	private ConcurrentHashMap<String, ConfigTownUpgrade> upgrades = new ConcurrentHashMap<String, ConfigTownUpgrade>();

	/* This gets populated periodically from a synchronous timer so it will be accessible from async tasks. */
	private ConcurrentHashMap<String, BonusGoodie> bonusGoodies = new ConcurrentHashMap<String, BonusGoodie>();

	private boolean pvp = false;

	public boolean leaderWantsToDisband = false;
	public boolean mayorWantsToDisband = false;
	public HashSet<String> outlaws = new HashSet<String>();

	public boolean claimed = false;
	public boolean defeated = false;

	private RandomEvent activeEvent;

	/* Last time someone used /build refreshblocks, make sure they can do it only so often. */
	private Date lastBuildableRefresh = null;
	private Date createdDate;

	public String tradeGoods = "";
	private long conqueredDate = 0L;

	class AttrCache {
		public Date lastUpdate;
		public AttrSource sources;
	}

	public HashMap<String, AttrCache> attributeCache = new HashMap<String, AttrCache>();

	public static final String TABLE_NAME = "TOWNS";

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" //
					+ "`id` int(11) unsigned NOT NULL auto_increment,"//
					+ "`name` VARCHAR(64) NOT NULL,"//
					+ "`civ_id` int(11) NOT NULL DEFAULT 0," //
					+ "`master_civ_id` int(11) NOT NULL DEFAULT 0," //
					+ "`mother_civ_id` int(11) NOT NULL DEFAULT 0,"//
					+ "`defaultGroupName` mediumtext DEFAULT NULL,"//
					+ "`mayorGroupName` mediumtext DEFAULT NULL,"//
					+ "`assistantGroupName` mediumtext DEFAULT NULL,"//
					+ "`upgrades` mediumtext DEFAULT NULL,"//
					+ "`level` int(11) DEFAULT 1,"//
					+ "`debt` double DEFAULT 0,"//
					+ "`coins` double DEFAULT 0," //
					+ "`daysInDebt` int(11) DEFAULT 0," //
					+ "`extra_hammers` double DEFAULT 0,"//
					+ "`culture` int(11) DEFAULT 0," //
					+ "`created_date` long,"//
					+ "`outlaws` mediumtext DEFAULT NULL,"//
					+ "`dbg_civ_name` mediumtext DEFAULT NULL," //
					+ "`tradeGoods` mediumtext DEFAULT NULL,"//
					+ "`conquered_date` mediumtext," //
					+ "UNIQUE KEY (`name`), " //
					+ "PRIMARY KEY (`id`)" + ")";
			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}

	public Town(Civilization civ) {
		this.setLevel(1);
		this.setCiv(civ);
		this.setDaysInDebt(0);
		this.setHammerRate(1.0);
		this.setExtraHammers(0);
		this.setAccumulatedCulture(0);
		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(0, false);
		this.setCreatedDate(new Date());
		this.GM = new TownGroupManager(this, null, null, null);
		loadSettings();
	}

	public Town(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.load(rs);
		loadSettings();
	}

	@Override
	public boolean equals(final Object another) {
		return (another instanceof Town) && ((Town) another).getId() == this.getId();
	}
	// ----------------- load save

	public void loadSettings() {
		this.SM = new TownBuildableManager(this);
		try {
			this.baseHammers = CivSettings.getDouble(CivSettings.townConfig, "town.base_hammer_rate");
			this.setBaseGrowth(CivSettings.getDouble(CivSettings.townConfig, "town.base_growth_rate"));
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.setLevel(rs.getInt("level"));
		this.setCiv(CivGlobal.getCivFromId(rs.getInt("civ_id")));

		GM = new TownGroupManager(this, rs.getString("defaultGroupName"), rs.getString("mayorGroupName"), rs.getString("assistantGroupName"));

		Integer motherCivId = rs.getInt("mother_civ_id");
		if (motherCivId != null && motherCivId != 0) {
			Civilization mother = CivGlobal.getConqueredCivFromId(motherCivId);
			if (mother == null) mother = CivGlobal.getCivFromId(motherCivId);

			if (mother == null)
				CivLog.warning("Unable to find a mother civ with ID:" + motherCivId + "!");
			else
				setMotherCiv(mother);
		}

		if (this.getCiv() == null) {
			CivLog.error("TOWN:" + this.getName() + " WITHOUT A CIV, id was:" + rs.getInt("civ_id"));
			// this.delete();
			CivGlobal.orphanTowns.add(this);
			throw new CivException("Failed to load town, bad data.");
		}
		this.setDaysInDebt(rs.getInt("daysInDebt"));
		this.setUpgradesFromString(rs.getString("upgrades"));

		// this.setHomeChunk(rs.getInt("homechunk_id"));
		this.setExtraHammers(rs.getDouble("extra_hammers"));
		this.setAccumulatedCulture(rs.getInt("culture"));
		this.conqueredDate = rs.getLong("conquered_date");

		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(rs.getDouble("coins"), false);
		this.setDebt(rs.getDouble("debt"));

		if (rs.getString("tradeGoods") != null)
			this.tradeGoods = rs.getString("tradeGoods");
		else
			this.tradeGoods = "";

		String outlawRaw = rs.getString("outlaws");
		if (outlawRaw != null) {
			String[] outlaws = outlawRaw.split(",");

			for (String outlaw : outlaws) {
				this.outlaws.add(outlaw);
			}
		}

		Long ctime = rs.getLong("created_date");
		if (ctime == null || ctime == 0)
			this.setCreatedDate(new Date(0)); // Forever in the past.
		else
			this.setCreatedDate(new Date(ctime));

		this.getCiv().addTown(this);
		this.processTradeLoad();
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		hashmap.put("name", this.getName());
		hashmap.put("civ_id", this.getCiv().getId());

		if (this.motherCiv != null) {
			hashmap.put("mother_civ_id", this.motherCiv.getId());
		} else {
			hashmap.put("mother_civ_id", 0);
		}

		hashmap.put("defaultGroupName", this.GM.defaultGroupName);
		hashmap.put("mayorGroupName", this.GM.mayorGroupName);
		hashmap.put("assistantGroupName", this.GM.assistantGroupName);
		hashmap.put("level", this.getLevel());
		hashmap.put("debt", this.getTreasury().getDebt());
		hashmap.put("daysInDebt", this.getDaysInDebt());
		hashmap.put("extra_hammers", this.getExtraHammers());
		hashmap.put("culture", this.getAccumulatedCulture());
		hashmap.put("upgrades", this.getUpgradesString());
		hashmap.put("coins", this.getTreasury().getBalance());
		hashmap.put("dbg_civ_name", this.getCiv().getName());
		hashmap.put("conquered_date", this.conqueredDate);
		hashmap.put("tradeGoods", this.tradeGoods);

		if (this.getCreatedDate() != null) {
			hashmap.put("created_date", this.getCreatedDate().getTime());
		} else {
			hashmap.put("created_date", null);
		}

		String outlaws = "";
		for (String outlaw : this.outlaws) {
			outlaws += outlaw + ",";
		}
		hashmap.put("outlaws", outlaws);

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() {
		try {
			/* Remove all of our residents from town. */
			for (Resident resident : this.residents.values()) {
				resident.setTown(null);
				/* Also forgive their debt, nobody to pay it to. */
				resident.getTreasury().setDebt(0);
				TagManager.editNameTag(resident);
				resident.saveNow();
			}

			/* Remove all our Groups */
			GM.delete();

			/* Remove all structures in the town. */
			for (Structure struct : this.SM.getStructures()) {
				struct.deleteWithUndo();
			}

			for (Wonder wonder : SM.getWonders()) {
				wonder.deleteWithUndo();
			}

			/* Remove all town chunks. */
			if (this.getTownChunks() != null) {
				for (TownChunk tc : this.getTownChunks()) {
					tc.delete();
				}
			}

			if (this.cultureChunks != null) {
				for (CultureChunk cc : this.cultureChunks.values()) {
					CivGlobal.removeCultureChunk(cc);
				}
			}
			this.cultureChunks = null;

			getCiv().removeTown(this);
			CivGlobal.removeTown(this);
			SQL.deleteNamedObject(this, TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ----------- create town

	public void checkCanCreatedTown(Resident resident, Structure cityhall) throws CivException {
		if (resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("town_found_errorIncamp"));
		if (resident.getTown() != null && resident.getTown().GM.isOneMayor(resident)) throw new CivException(CivSettings.localize.localizedString("var_town_found_errorIsMayor", resident.getTown().getName()));

		if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("town_found_errorAtWar"));
		if (getCiv() != null) {
			if (!getCiv().getTreasury().hasEnough(getCiv().getNextTownCost())) throw new CivException("Для основания города в казне цивилизации должно быть " + getCiv().getNextTownCost() + " коинов");
		}
		double minDistanceFriendSqr;
		double minDistanceEnemySqr;
		double min_distanceSqr;
		try {
			minDistanceFriendSqr = Math.pow(CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance"), 2);
			minDistanceEnemySqr = Math.pow(CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance_enemy"), 2);
			min_distanceSqr = Math.pow(CivSettings.getInteger(CivSettings.civConfig, "civ.min_distance"), 2);
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalException"));
		}

		for (Town town : CivGlobal.getTowns()) {
			Location loc = town.getLocation();
			if (loc == null) continue;
			if (!loc.getWorld().equals(this.getLocation().getWorld())) continue;

			double distSqr = loc.distanceSquared(cityhall.getCenterLocation());
			double minDistanceSqr;
			if (town.getCiv().equals(getCiv())) {
				minDistanceSqr = minDistanceFriendSqr;
			} else
				minDistanceSqr = minDistanceEnemySqr;

			if (distSqr < minDistanceSqr) {
				DecimalFormat df = new DecimalFormat("###.##");
				throw new CivException(CivSettings.localize.localizedString("var_town_found_errorTooClose", town.getName(), df.format(Math.sqrt(distSqr)), Math.sqrt(minDistanceSqr)));
			}
			// TODO не придумал как зделать и города, и столици в одной проверке
			if (distSqr <= min_distanceSqr) {
				DecimalFormat df = new DecimalFormat();
				throw new CivException(CivSettings.localize.localizedString("var_civ_found_errorTooClose1", town.getCiv().getName(), df.format(Math.sqrt(distSqr)), Math.sqrt(min_distanceSqr)));
			}
		}
	}

	public void createTown(Resident resident, Structure cityhall) throws CivException {
		Player player = CivGlobal.getPlayer(resident.getName());
		this.checkCanCreatedTown(resident, cityhall);
		try {
			int cost = getCiv().getNextTownCost();

			// build TownHall
			try {
				this.getTreasury().deposit(cityhall.getCost());
				cityhall.setSQLOwner(this);
				cityhall.build(player);
			} catch (CivException e) {
				getCiv().removeTown(this);
				this.delete();
				throw e;
			}
			
			GM.init();
			try {
				if (resident.getTown() != null) {
					CivMessage.sendTown(resident.getTown(), CivSettings.localize.localizedString("var_town_found_leftTown", resident.getName()));
					resident.getTown().removeResident(resident);
				}
				this.addResident(resident);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("town_found_residentError"));
			}
			GM.addMayor(resident);
			
			CivGlobal.addTown(this);
			getCiv().addTown(this);
			getCiv().getTreasury().deposit(cost);

			CivGlobal.processCulture();
			this.saveNow();
			return;
		} catch (SQLException e2) {
			e2.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}
	}

	public void rename(String name) throws CivException, InvalidNameException {

		Town other = CivGlobal.getTown(name);
		if (other != null) throw new CivException(CivSettings.localize.localizedString("town_rename_errorExists"));

		String oldName = this.getName();
		CivGlobal.removeTown(this);

		this.setName(name);
		this.save();

		CivGlobal.addTown(this);
		CivMessage.global(CivSettings.localize.localizedString("var_town_rename_success1", oldName, this.getName()));
	}

	// --------------- gettters

	public int getMaxTileImprovements() {
		ConfigTownLevel level = CivSettings.townLevels.get(this.getLevel());
		Integer maxTileImprovements = level.tile_improvements;
		if (this.getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) maxTileImprovements *= 2;
		return maxTileImprovements;
	}

	private Location location = null;

	public Location getLocation() {
		if (location == null) {
			Cityhall cityhall = getCityhall();
			if (cityhall == null) return null;
			location = cityhall.getCenterLocation();
		}
		return location;
	}

	private Cityhall cityhall = null;

	public Cityhall getCityhall() {
		if (cityhall == null) cityhall = (Cityhall) this.SM.getFirstStructureById("s_cityhall");
		return cityhall;
	}

	public int getMaxPlots() {
		ConfigTownLevel lvl = CivSettings.townLevels.get(this.level);
		return lvl.plots;
	}

	public String getLevelTitle() {
		ConfigTownLevel clevel = CivSettings.townLevels.get(this.level);
		if (clevel == null) {
			return "Unknown";
		} else {
			return clevel.title;
		}
	}

	public String getPvpString() {
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
			if (pvp)
				return CivColor.Red + "[PvP]";
			else
				return CivColor.Green + "[No PvP]";
		} else
			return CivColor.Red + "[WAR-PvP]";
	}

	public String getDynmapDescription() {
		String out = "";
		try {
			out += "<h3><b>" + this.getName() + "</b> (<i>" + this.getCiv().getName() + "</i>)</h3>";
			out += "<b>" + CivSettings.localize.localizedString("Mayors") + " " + this.GM.getMayorGroup().getMembersString() + "</b>";
		} catch (Exception e) {
			CivLog.error("Town: " + this.getName());
			e.printStackTrace();
		}

		return out;
	}

	public int getTileImprovementCount() {
		int count = 0;
		for (Structure struct : SM.getStructures()) {
			if (struct.isTileImprovement()) count++;
		}
		return count;
	}

	public int getScore() {
		int points = 0;

		// Count Structures
		for (Structure struct : this.SM.getStructures()) {
			points += struct.getPoints();
		}

		// Count Wonders
		for (Wonder wonder : this.SM.getWonders()) {
			points += wonder.getPoints();
		}

		// Count residents, town chunks, and culture chunks.
		// also coins.
		try {
			double perResident = CivSettings.getInteger(CivSettings.scoreConfig, "town_scores.resident");
			points += perResident * this.getResidents().size();

			double perTownChunk = CivSettings.getInteger(CivSettings.scoreConfig, "town_scores.town_chunk");
			points += perTownChunk * this.getTownChunks().size();

			double perCultureChunk = CivSettings.getInteger(CivSettings.scoreConfig, "town_scores.culture_chunk");
			if (this.cultureChunks != null) {
				points += perCultureChunk * this.cultureChunks.size();
			} else {
				CivLog.warning("Town " + this.getName() + " has no culture chunks??");
			}

			double coins_per_point = CivSettings.getInteger(CivSettings.scoreConfig, "coins_per_point");
			points += (int) (this.getTreasury().getBalance() / coins_per_point);

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}

		return points;
	}
	// -------------------- Upgrades

	private void setUpgradesFromString(String upgradeString) {
		String[] split = upgradeString.split(",");
		for (String str : split) {
			if (str == null || str.equals("")) continue;
			ConfigTownUpgrade upgrade = CivSettings.townUpgrades.get(str);
			if (upgrade == null) {
				CivLog.warning("Unknown town upgrade:" + str + " in town " + this.getName());
				continue;
			}
			this.upgrades.put(str, upgrade);
		}
	}

	private String getUpgradesString() {
		String out = "";
		for (ConfigTownUpgrade upgrade : upgrades.values()) {
			out += upgrade.id + ",";
		}
		return out;
	}

	public void addUpgrade(ConfigTownUpgrade upgrade) {
		try {
			upgrade.processAction(this);
		} catch (CivException e) {}
		this.upgrades.put(upgrade.id, upgrade);
		this.save();
	}

	public boolean hasUpgrade(String require_upgrade) {
		if (require_upgrade == null || require_upgrade.equals("")) return true;
		return upgrades.containsKey(require_upgrade);
	}

	public ConfigTownUpgrade getUpgrade(String id) {
		return upgrades.get(id);
	}

	public void removeUpgrade(ConfigTownUpgrade upgrade) {
		this.upgrades.remove(upgrade.id);
	}

	public void purchaseUpgrade(ConfigTownUpgrade upgrade) throws CivException {
		if (!this.hasUpgrade(upgrade.require_upgrade)) throw new CivException(CivSettings.localize.localizedString("town_missingUpgrades"));
		if (!this.getTreasury().hasEnough(upgrade.cost)) throw new CivException(CivSettings.localize.localizedString("var_town_missingFunds", upgrade.cost, CivSettings.CURRENCY_NAME));
		if (!this.SM.hasStructure(upgrade.require_structure)) throw new CivException(CivSettings.localize.localizedString("town_missingStructures"));
		if (upgrade.id.equalsIgnoreCase("upgrade_stock_exchange_level_6") && !this.SM.canUpgradeStock(upgrade.id))
			throw new CivException("§c" + CivSettings.localize.localizedString("var_upgradeStockExchange_nogoodCondition", "http://wiki.minetexas.com/index.php/Stock_Exchange"));
		if (!this.SM.hasWonder(upgrade.require_wonder)) throw new CivException(CivSettings.localize.localizedString("town_missingWonders"));

		this.getTreasury().withdraw(upgrade.cost);

		try {
			upgrade.processAction(this);
		} catch (CivException e) {
			// Something went wrong purchasing the upgrade, refund and throw again.
			this.getTreasury().deposit(upgrade.cost);
			throw e;
		}

		this.upgrades.put(upgrade.id, upgrade);
		this.save();
	}

	public void loadUpgrades() throws CivException {
		for (ConfigTownUpgrade upgrade : this.upgrades.values()) {
			try {
				upgrade.processAction(this);
			} catch (CivException e) {
				// Ignore any exceptions here?
				CivLog.warning("Loading upgrade generated exception:" + e.getMessage());
			}
		}
	}

	public ConcurrentHashMap<String, ConfigTownUpgrade> getUpgrades() {
		return upgrades;
	}

	// ----------------- residents

	public void addResident(Resident res) throws AlreadyRegisteredException {
		String key = res.getName().toLowerCase();

		if (residents.containsKey(key)) throw new AlreadyRegisteredException(res.getName() + " already a member of town " + this.getName());
		res.setTown(this);

		residents.put(key, res);
		try {
			GM.addDefault(res);
		} catch (CivException e) {}
	}

	public int getResidentCount() {
		return residents.size();
	}

	public Collection<Resident> getResidents() {
		return residents.values();
	}

	public boolean hasResident(String name) {
		return residents.containsKey(name.toLowerCase());
	}

	public boolean hasResident(Resident res) {
		return hasResident(res.getName());
	}

	public void kickResident(Resident resident) {
		/* Clear resident's debt and remove from town. */
		resident.setRejoinCooldown(this);
		removeResident(resident);
	}

	public void removeResident(Resident resident) {
		/* Repo all this resident's plots. */
		for (TownChunk tc : this.getTownChunks()) {
			if (tc.perms.getOwner() == resident) {
				tc.perms.setOwner(null);
				tc.perms.replaceGroups(GM.getDefaultGroup());
				tc.perms.resetPerms();
				tc.save();
			}
		}
		this.residents.remove(resident.getName().toLowerCase());

		resident.setTown(null);
		GM.removeAllGroup(resident);
		this.save();
	}

	public void validateResidentSelect(Resident resident) throws CivException {
		if (!this.hasResident(resident) && !this.getCiv().GM.isLeaderOrAdviser(resident)) {
			throw new CivException(CivSettings.localize.localizedString("town_validateSelect_error2"));
		}
	}

	public void rebuildNameTag() {
		TagManager.editNameTag(this);
	}

	public void addFakeResident(Resident fake) {
		this.fakeResidents.put(fake.getName(), fake);
	}

	public boolean areMayorsInactive() {

		int mayor_inactive_days;
		try {
			mayor_inactive_days = CivSettings.getInteger(CivSettings.townConfig, "town.mayor_inactive_days");
			for (Resident resident : this.GM.getMayors()) {
				if (!resident.isInactiveForDays(mayor_inactive_days)) return false;
			}

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public Collection<Resident> getOnlineResidents() {
		LinkedList<Resident> residents = new LinkedList<Resident>();
		for (Resident resident : this.getResidents()) {
			try {
				CivGlobal.getPlayer(resident);
				residents.add(resident);
			} catch (CivException e) {
				// player offline
			}
		}

		for (Resident resident : this.fakeResidents.values()) {
			residents.add(resident);
		}

		return residents;
	}

	// --------------------- townChunks

	public void addTownChunk(TownChunk tc) throws AlreadyRegisteredException {
		if (townChunks.containsKey(tc.getChunkCoord())) {
			throw new AlreadyRegisteredException("TownChunk at " + tc.getChunkCoord() + " already registered to town " + this.getName());
		}
		townChunks.put(tc.getChunkCoord(), tc);
	}

	public Collection<TownChunk> getTownChunks() {
		return this.townChunks.values();
	}

	public void removeTownChunk(TownChunk tc) {
		this.townChunks.remove(tc.getChunkCoord());
	}

	// --------------- Wonder

	// ------------- Structure

	public void markLastBuildableRefeshAsNow() {
		this.lastBuildableRefresh = new Date();
	}

	// ------------------------- Culture

	public void addAccumulatedCulture(double generated) {
		ConfigCultureLevel clc = CivSettings.cultureLevels.get(this.getCultureLevel());

		this.culture += generated;
		this.save();
		if (this.getCultureLevel() != CivSettings.getMaxCultureLevel()) {
			if (this.culture >= clc.amount) {
				CivGlobal.processCulture();
				CivMessage.sendCiv(this.civ, CivSettings.localize.localizedString("var_town_bordersExpanded", this.getName()));
			}
		}
		return;
	}

	public int getAccumulatedCulture() {
		return culture;
	}

	public void setAccumulatedCulture(int culture) {
		this.culture = culture;
	}

	public AttrSource getCultureRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();

		double newRate = getGovernment().culture_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());
		newRate = rate * state.culture_rate;
		rates.put("Happiness", newRate - rate);
		rate = newRate;

		double structures = 0;
		if (this.getBuffManager().hasBuff("buff_art_appreciation")) structures += this.getBuffManager().getEffectiveDouble("buff_art_appreciation");
		rates.put("Great Works", structures);
		rate += structures;

		double additional = 0;
		if (this.getBuffManager().hasBuff("buff_fine_art")) additional += this.getBuffManager().getEffectiveDouble(Buff.FINE_ART);
		if (this.getBuffManager().hasBuff("buff_pyramid_culture")) additional += this.getBuffManager().getEffectiveDouble("buff_pyramid_culture");
		if (this.getBuffManager().hasBuff("buff_neuschwanstein_culture")) additional += this.getBuffManager().getEffectiveDouble("buff_neuschwanstein_culture");

		if (this.getBuffManager().hasBuff("buff_globe_theatre_culture_from_towns")) {
			int townCount = 0;
			for (Civilization civ : CivGlobal.getCivs()) {
				townCount += civ.getTownCount();
			}
			double culturePercentPerTown = Double.valueOf(CivSettings.buffs.get("buff_globe_theatre_culture_from_towns").value);

			double bonus = culturePercentPerTown * townCount;
			additional += bonus;
		}

		rates.put("Wonders/Goodies", additional);
		rate += additional;

		return new AttrSource(rates, rate, null);
	}

	public AttrSource getCulture() {
		AttrCache cache = this.attributeCache.get("CULTURE");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}

		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();
		double goodieCulture = 0.0;
		if (this.getBuffManager().hasBuff("buff_advanced_touring")) goodieCulture += 200.0;
		sources.put("Goodies", goodieCulture);
		total += goodieCulture;
		/* Grab beakers generated from structures with components. */
		double fromStructures = 0;
		for (Structure struct : this.SM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("CULTURE")) fromStructures += as.getGenerated();
				}
			}
			if (struct instanceof Temple) {
				Temple temple = (Temple) struct;
				fromStructures += temple.getCultureGenerated();
			}
		}

		if (this.getBuffManager().hasBuff("buff_globe_theatre_culture_from_towns")) {
			int townCount = 0;
			for (Civilization civ : CivGlobal.getCivs()) {
				townCount += civ.getTownCount();
			}
			double culturePerTown = Double.valueOf(CivSettings.buffs.get("buff_globe_theatre_culture_from_towns").value);
			double bonus = culturePerTown * townCount;
			CivMessage.sendTown(this, CivColor.LightGreen + CivSettings.localize.localizedString("var_town_GlobeTheatreCulture", CivColor.Yellow + bonus + CivColor.LightGreen, townCount));

			fromStructures += bonus;
		}

		total += fromStructures;
		sources.put("Structures", fromStructures);

		AttrSource rate = this.getCultureRate();
		total *= rate.total;

		if (total < 0) total = 0;

		AttrSource as = new AttrSource(sources, total, rate);
		cache.sources = as;
		this.attributeCache.put("CULTURE", cache);
		return as;
	}

	public void removeCultureChunk(ChunkCoord coord) {
		this.cultureChunks.remove(coord);
	}

	public void removeCultureChunk(CultureChunk cc) {
		this.cultureChunks.remove(cc.getChunkCoord());
	}

	public void addCultureChunk(CultureChunk cc) {
		this.cultureChunks.put(cc.getChunkCoord(), cc);
	}

	public int getCultureLevel() {

		/* Get the first level */
		int bestLevel = 0;
		ConfigCultureLevel level = CivSettings.cultureLevels.get(0);

		while (this.culture >= level.amount) {
			level = CivSettings.cultureLevels.get(bestLevel + 1);
			if (level == null) {
				level = CivSettings.cultureLevels.get(bestLevel);
				break;
			}
			bestLevel++;
		}

		return level.level;
	}

	public Collection<CultureChunk> getCultureChunks() {
		return this.cultureChunks.values();
	}

	public Object getCultureChunk(ChunkCoord coord) {
		return this.cultureChunks.get(coord);
	}

	public void trimCultureChunks(HashSet<ChunkCoord> expanded) {
		LinkedList<ChunkCoord> removedKeys = new LinkedList<ChunkCoord>();
		for (ChunkCoord coord : this.cultureChunks.keySet()) {
			if (!expanded.contains(coord)) removedKeys.add(coord);
		}

		for (ChunkCoord coord : removedKeys) {
			CultureChunk cc = CivGlobal.getCultureChunk(coord);
			CivGlobal.removeCultureChunk(cc);
			this.cultureChunks.remove(coord);
		}
	}

	public ChunkCoord getTownCultureOrigin() {
		/* Culture now only eminates from the town hall. */
		Location townLocation = this.getLocation();
		return (townLocation == null) ? this.getTownChunks().iterator().next().getChunkCoord()// if no town hall, pick a 'random' town chunk'
				: new ChunkCoord(townLocation);// Grab town chunk from town hall location.
	}

	// ----------------- hammers

	public double getExtraHammers() {
		return extraHammers;
	}

	public void setExtraHammers(double extraHammers) {
		this.extraHammers = extraHammers;
	}

	public AttrSource getHammerRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();
		ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());

		/* Happiness */
		double newRate = rate * state.hammer_rate;
		rates.put("Happiness", newRate - rate);
		rate = newRate;

		/* Government */
		newRate = rate * getGovernment().hammer_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		double randomRate = RandomEvent.getHammerRate(this);
		newRate = rate * randomRate;
		rates.put("Random Events", newRate - rate);
		rate = newRate;
		for (final Town town : this.civ.getTowns()) {
			if (town.getBuffManager().hasBuff("buff_spoil")) {
				newRate *= 1.1;
			}
		}
		rate = newRate;
		/* Captured Town Penalty */
		if (this.motherCiv != null) {
			try {
				newRate = rate * CivSettings.getDouble(CivSettings.warConfig, "war.captured_penalty");
				rates.put("Captured Penalty", newRate - rate);
				rate = newRate;

			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
		}
		return new AttrSource(rates, rate, null);
	}

	public AttrSource getHammers() {
		double total = 0;

		AttrCache cache = this.attributeCache.get("HAMMERS");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}

		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Wonders and Goodies. */
		double wonderGoodies = this.getBuffManager().getEffectiveInt(Buff.CONSTRUCTION);
		wonderGoodies += this.getBuffManager().getEffectiveDouble("buff_grandcanyon_hammers");
		sources.put("Wonders/Goodies", wonderGoodies);
		total += wonderGoodies;

		if (this.hasScroll()) {
			total += 500.0;
			sources.put("Scroll of Hammers (Until " + this.getScrollTill() + ")", 500.0);
		}

		double cultureHammers = this.getHammersFromCulture();
		sources.put("Culture Biomes", cultureHammers);
		total += cultureHammers;
		/* Grab hammers generated from structures with components. */
		double structures = 0;
		double mines = 0;
		int countMine = 0;
		double mineHouse = 0;
		for (Structure struct : this.SM.getStructures()) {
			if (struct instanceof Mine) {
				countMine++;
				Mine mine = (Mine) struct;
				mines += mine.getBonusHammers();
			}
			if (struct instanceof MineHouse) {
				mineHouse = 25;
			}
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("HAMMERS")) {
						structures += as.getGenerated();
					}
				}
			}
		}
		mines = mineHouse * countMine;

		total += mines;
		sources.put("Mines", mines);

		total += structures;
		sources.put("Structures", structures);

		sources.put("Base Hammers", this.baseHammers);
		total += this.baseHammers;

		AttrSource rate = getHammerRate();
		total *= rate.total;

		if (total < this.baseHammers) {
			total = this.baseHammers;
		}

		AttrSource as = new AttrSource(sources, total, rate);
		cache.sources = as;
		this.attributeCache.put("HAMMERS", cache);
		return as;
	}

	public void setHammerRate(double hammerRate) {
		this.baseHammers = hammerRate;
	}

	public void giveExtraHammers(double extra) {
		if (SM.buildTaskSize() == 0) {
			// Nothing is building, store the extra hammers for when a structure starts
			// building.
			extraHammers = extra;
		} else {
			// Currently building structures ... divide them evenly between
			double hammers_per_task = extra / SM.buildTaskSize();
			double leftovers = 0.0;

			for (BuildAsyncTask task : SM.getBuildTasks()) {
				leftovers += task.setExtraHammers(hammers_per_task);
			}

			extraHammers = leftovers;
		}
		this.save();
	}

	public Double getHammersFromCulture() {
		double hammers = 0;
		for (CultureChunk cc : this.cultureChunks.values()) {
			hammers += cc.getHammers();
		}
		return hammers;
	}
	// --------------- EconObject

	public EconObject getTreasury() {
		return treasury;
	}

	public void setTreasury(EconObject treasury) {
		this.treasury = treasury;
	}

	public void depositDirect(double amount) {
		this.treasury.deposit(amount);
	}

	public void depositTaxed(double amount) {
		double taxAmount = amount * this.getDepositCiv().getIncomeTaxRate();
		amount -= taxAmount;

		if (this.getMotherCiv() != null) {
			double capturedPenalty;
			try {
				capturedPenalty = CivSettings.getDouble(CivSettings.warConfig, "war.captured_penalty");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}

			double capturePayment = amount * capturedPenalty;
			CivMessage.sendTown(this, CivColor.Yellow + CivSettings.localize.localizedString("var_town_capturePenalty1", (amount - capturePayment), CivSettings.CURRENCY_NAME, this.getCiv().getName()));
			amount = capturePayment;
		}

		this.treasury.deposit(amount);
		this.getDepositCiv().taxPayment(this, taxAmount);
	}

	public void withdraw(double amount) {
		this.treasury.withdraw(amount);
	}

	public boolean inDebt() {
		return this.treasury.inDebt();
	}

	public double getDebt() {
		return this.treasury.getDebt();
	}

	public void setDebt(double amount) {
		this.treasury.setDebt(amount);
	}

	public double getBalance() {
		return this.treasury.getBalance();
	}

	public boolean hasEnough(double amount) {
		return this.treasury.hasEnough(amount);
	}

	public void depositFromResident(Double amount, Resident resident) throws CivException {
		if (!resident.getTreasury().hasEnough(amount)) {
			throw new CivException(CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", (amount + " " + CivSettings.CURRENCY_NAME)));
		}

		if (this.inDebt()) {
			if (this.getDebt() > amount) {
				this.getTreasury().setDebt(this.getTreasury().getDebt() - amount);
				resident.getTreasury().withdraw(amount);
			} else {
				double leftAmount = amount - this.getTreasury().getDebt();
				this.getTreasury().setDebt(0);
				this.getTreasury().deposit(leftAmount);
				resident.getTreasury().withdraw(amount);
			}

			if (this.getTreasury().inDebt() == false) {
				this.daysInDebt = 0;
				CivMessage.global(CivSettings.localize.localizedString("town_ruin_nolongerInDebt", this.getName()));
			}
		} else {
			this.getTreasury().deposit(amount);
			resident.getTreasury().withdraw(amount);
		}

		this.save();
	}
	// -------------- Upkeep

	public double payUpkeep() throws InvalidConfiguration {
		double upkeep = 0;
		if (this.getCiv().isAdminCiv()) return 0;
		upkeep += this.getBaseUpkeep();
		upkeep += this.getStructureUpkeep();
		upkeep *= this.getBonusUpkeep();
		upkeep *= getGovernment().upkeep_rate;

		if (this.getBuffManager().hasBuff("buff_colossus_reduce_upkeep")) {
			upkeep = upkeep - (upkeep * this.getBuffManager().getEffectiveDouble("buff_colossus_reduce_upkeep"));
		}

		// TODO подозрительный текст из фурнекса влияет на колос
		if (this.getBuffManager().hasBuff("debuff_colossus_leech_upkeep")) {
			double rate = this.getBuffManager().getEffectiveDouble("debuff_colossus_leech_upkeep");
			double amount = upkeep * rate;

			Wonder colossus = CivGlobal.getWonderByConfigId("w_colossus");
			if (colossus != null) {
				colossus.getTown().getTreasury().deposit(amount);
			} else {
				CivLog.warning("Unable to find Colossus wonder but debuff for leech upkeep was present!");
				// Colossus is "null", doesn't exist, we remove the buff in case of duplication
				this.getBuffManager().removeBuff("debuff_colossus_leech_upkeep");
			}
		}

		if (this.getTreasury().hasEnough(upkeep)) {
			this.getTreasury().withdraw(upkeep);
		} else {
			/* Couldn't pay the bills. Add to this town's debt, civ may pay it later. */
			double diff = upkeep - this.getTreasury().getBalance();

			if (this.isCapitol()) {
				/* Capitol towns cannot be in debt, must pass debt on to civ. */
				if (this.getCiv().getTreasury().hasEnough(diff)) {
					this.getCiv().getTreasury().withdraw(diff);
				} else {
					diff -= this.getCiv().getTreasury().getBalance();
					this.getCiv().getTreasury().setBalance(0);
					this.getCiv().getTreasury().setDebt(this.getCiv().getTreasury().getDebt() + diff);
					this.getCiv().save();
				}
			} else {
				this.getTreasury().setDebt(this.getTreasury().getDebt() + diff);
			}
			this.getTreasury().withdraw(this.getTreasury().getBalance());
		}
		return upkeep;
	}

	public double getBaseUpkeep() {
		ConfigTownLevel level = CivSettings.townLevels.get(this.level);
		return level.upkeep;
	}

	public double getStructureUpkeep() {
		double upkeep = 0;

		for (Structure struct : SM.getStructures()) {
			upkeep += struct.getUpkeepCost();
		}
		return upkeep;
	}

	public double getTotalUpkeep() throws InvalidConfiguration {
		return this.getBaseUpkeep() + this.getStructureUpkeep();
	}
	// ---------------- Technology

	public boolean hasTechnology(String require_tech) {
		return this.getCiv().hasTechnology(require_tech);
	}

	public void onTechUpdate() {
		for (Structure struct : this.SM.getStructures()) {
			try {
				if (struct.isActive()) struct.onTechUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		for (Wonder wonder : this.SM.getWonders()) {
			try {
				if (wonder.isActive()) wonder.onTechUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// ----------------- BonusGood

	public void setBonusGoodies(ConcurrentHashMap<String, BonusGoodie> bonusGoodies) {
		this.bonusGoodies = bonusGoodies;
	}

	public Collection<BonusGoodie> getBonusGoodies() {
		return this.bonusGoodies.values();
	}

	/* public HashSet<BonusGoodie> getEffectiveBonusGoodies() { HashSet<BonusGoodie> returnList = new HashSet<BonusGoodie>(); for (BonusGoodie
	 * goodie : getBonusGoodies()) { //CivLog.debug("hash:"+goodie.hashCode()); if (!goodie.isStackable()) { boolean skip = false; for
	 * (BonusGoodie existing : returnList) { if (existing.getDisplayName().equals(goodie.getDisplayName())) { skip = true; break; } } if (skip)
	 * { continue; } } returnList.add(goodie); } return returnList; } */
	public void removeGoodie(BonusGoodie goodie) {
		this.bonusGoodies.remove(goodie.getOutpost().getCorner().toString());
		for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
			String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;
			buffManager.removeBuff(key);
		}
		if (goodie.getFrame() != null) {
			goodie.getFrame().clearItem();
		}
	}

	public void clearBonusGoods() {
		this.bonusGoodies.clear();
	}

	public void depositTradeGood(final String id) throws CivException {
		if (StringUtils.isBlank(this.tradeGoods)) {
			this.tradeGoods = id + ", ";
		} else {
			if (this.tradeGoods.split(", ").length >= 8) {
				throw new CivException(CivSettings.localize.localizedString("var_virtualTG_townFullGoods", "§6" + this.getName() + "§c"));
			}
			this.tradeGoods = this.tradeGoods + id + ", ";
		}
		final ConfigTradeGood configTradeGood = CivSettings.goods.get(id);
		for (final ConfigBuff configBuff : configTradeGood.buffs.values()) {
			final String key = "tradegood:" + this.tradeGoods.split(", ").length + ":" + configBuff.id;
			if (this.buffManager.hasBuffKey(key)) continue;
			try {
				this.buffManager.addBuff(key, configBuff.id, configTradeGood.name);
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
		this.SM.onBonusGoodieUpdate();
	}

	public int getTradeGoodCount(final String id) {
		if (!this.tradeGoods.contains(id)) {
			return 0;
		}
		int count = 0;
		for (final String good : this.tradeGoods.split(", ")) {
			if (good.equals(id)) {
				++count;
			}
		}
		return count;
	}

	public void processTradeLoad() {
		if (StringUtils.isBlank(this.tradeGoods)) {
			return;
		}
		int iterate = 1;
		for (final String id : this.tradeGoods.split(", ")) {
			final ConfigTradeGood configTradeGood = CivSettings.goods.get(id);
			for (final ConfigBuff configBuff : configTradeGood.buffs.values()) {
				final String key = "tradegood:" + iterate + ":" + configBuff.id;
				if (this.buffManager.hasBuffKey(key)) {
					continue;
				}
				try {
					this.buffManager.addBuff(key, configBuff.id, configTradeGood.name);
					++iterate;
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
		}
		for (final Structure structure : this.SM.getStructures()) {
			structure.onBonusGoodieUpdate();
		}
	}

	public boolean hasTradeGood(final String id) {
		return this.tradeGoods.contains(id);
	}

	public void withdrawTradeGood(final String id) throws CivException {
		if (!this.hasTradeGood(id)) {
			throw new CivException(CivSettings.localize.localizedString("var_virtualTG_townHasNoGood", "§6" + this.getName() + "§c", "§6" + CivSettings.goods.get(id).name + "§c"));
		}
		final String[] goods = this.tradeGoods.split(", ");
		final ArrayList<String> newGoods = new ArrayList<String>();
		boolean withdrawed = false;
		for (int i = 0; i < goods.length; ++i) {
			final String goodID = goods[i];
			if (!withdrawed && goodID.equals(id)) {
				withdrawed = true;
			} else {
				newGoods.add(goodID);
			}
		}
		String goodies = "";
		for (final String goodie : newGoods) {
			goodies = goodies + goodie + ", ";
		}
		this.tradeGoods = goodies;
		final ArrayList<String> keysToRemove = new ArrayList<String>();
		for (final Buff buff : this.buffManager.getAllBuffs()) {
			if (buff.getKey().contains("tradegood")) {
				keysToRemove.add(buff.getKey());
			}
		}
		for (final String key : keysToRemove) {
			this.buffManager.removeBuff(key);
		}
		this.processTradeLoad();
	}

	// -------------- get rate

	public AttrSource getGrowthRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();

		double newRate = rate * getGovernment().growth_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		if (this.getCiv().hasTechnology("tech_fertilizer")) {
			double techRate = 0.3;
			rates.put("Technology", techRate);
			rate += techRate;
		}

		/* Wonders and Goodies. */
		double additional = this.getBuffManager().getEffectiveDouble(Buff.GROWTH_RATE);
		additional += this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_growth");

		additional += this.getBuffManager().getEffectiveDouble("buff_mother_tree_growth");

		double additionalGrapes = this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_additional_growth");
		int grapeCount = 0;
		for (BonusGoodie goodie : this.getBonusGoodies()) {
			if (goodie.getDisplayName().equalsIgnoreCase("grapes")) {
				grapeCount++;
			}
		}

		additional += (additionalGrapes * grapeCount);
		rates.put("Wonders/Goodies", additional);
		rate += additional;

		return new AttrSource(rates, rate, null);
	}

	public AttrSource getGrowth() {
		AttrCache cache = this.attributeCache.get("GROWTH");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}

		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Grab any growth from culture. */
		double cultureSource = 0;
		for (CultureChunk cc : this.cultureChunks.values()) {
			try {
				cultureSource += cc.getGrowth();
			} catch (NullPointerException e) {
				CivLog.error(this.getName() + " - Culture Chunks: " + cc);
				e.printStackTrace();
			}

		}

		sources.put("Culture Biomes", cultureSource);
		total += cultureSource;

		double grapeCount = 0.0;
		for (final String goodID : this.tradeGoods.split(", ")) {
			if (CivSettings.goods.get(goodID) != null) {
				for (final ConfigBuff configBuff : CivSettings.goods.get(goodID).buffs.values()) {
					if (configBuff.id.equals("buff_growth")) {
						grapeCount += 150.0;
					}
				}
			}
		}
		total += grapeCount;
		sources.put("Goodies", grapeCount);

		/* Grab any growth from structures. */
		double structures = 0;
		for (Structure struct : this.SM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("GROWTH")) {
						double h = as.getGenerated();
						structures += h;
					}
				}
			}
		}

		total += structures;
		sources.put("Structures", structures);

		boolean hasBurj = false;
		for (final Town town : this.getCiv().getTowns()) {
			if (town.SM.hasWonder("w_burj")) {
				hasBurj = true;
				break;
			}
		}
		if (hasBurj) {
			sources.put("Wonders", 1000.0);
			total += 1000.0;
		}

		sources.put("Base Growth", baseGrowth);
		total += baseGrowth;

		AttrSource rate = this.getGrowthRate();
		total *= rate.total;

		if (total < 0) {
			total = 0;
		}

		AttrSource as = new AttrSource(sources, total, rate);
		cache.sources = as;
		this.attributeCache.put("GROWTH", cache);
		return as;
	}

	public double getCottageRate() {
		double rate = getGovernment().cottage_rate;

		double additional = rate * this.getBuffManager().getEffectiveDouble(Buff.COTTAGE_RATE);
		rate += additional;

		/* Adjust for happiness state. */
		rate *= this.getHappinessState().coin_rate;
		return rate;
	}

	public double getTempleRate() {
		double rate = 1.0;
		return rate;
	}

	public double getTradeRate() {
		double rate = getGovernment().trade_rate;

		/* Grab changes from any rate components. */
		double fromStructures = 0.0;
		for (Structure struct : this.SM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeRate) {
					AttributeRate as = (AttributeRate) comp;
					if (as.getString("attribute").equalsIgnoreCase("TRADE")) {
						fromStructures += as.getGenerated();
					}
				}
			}
		}
		/* XXX TODO convert this into a 'source' rate so it can be displayed properly. */
		rate += fromStructures;

		double additional = rate * this.getBuffManager().getEffectiveDouble(Buff.TRADE);
		rate += additional;

		/* Adjust for happiness state. */
		rate *= this.getHappinessState().coin_rate;
		return rate;
	}

	public AttrSource getBeakerRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();

		ConfigHappinessState state = this.getHappinessState();
		double newRate = rate * state.beaker_rate;
		rates.put("Happiness", newRate - rate);
		rate = newRate;

		newRate = rate * getGovernment().beaker_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;

		/* Additional rate increases from buffs. */
		/* Great Library buff is made to not stack with Science_Rate */
		double additional = rate * getBuffManager().getEffectiveDouble(Buff.SCIENCE_RATE);
		additional += rate * getBuffManager().getEffectiveDouble("buff_greatlibrary_extra_beakers");
		rate += additional;
		rates.put("Goodies/Wonders", additional);

		return new AttrSource(rates, rate, null);
	}

	public AttrSource getBeakers() {
		AttrCache cache = this.attributeCache.get("BEAKERS");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}

		double beakers = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Grab beakers generated from culture. */
		double fromCulture = 0;
		for (CultureChunk cc : this.cultureChunks.values()) {
			fromCulture += cc.getBeakers();
		}
		sources.put("Culture Biomes", fromCulture);
		beakers += fromCulture;

		/* Grab beakers generated from structures with components. */
		double fromStructures = this.SM.getBeakersFromStructure();
		beakers += fromStructures;
		sources.put("Structures", fromStructures);

		/* Grab any extra beakers from buffs. */
		double wondersTrade = 0;
		// No more flat bonuses here, leaving it in case of new buffs
		if (this.getBuffManager().hasBuff("buff_advanced_mixing")) {
			wondersTrade += 150.0;
		}
		beakers += wondersTrade;
		sources.put("Goodies/Wonders", wondersTrade);

		/* Make sure we never give out negative beakers. */
		beakers = Math.max(beakers, 0);
		AttrSource rates = getBeakerRate();

		beakers = beakers * rates.total;

		if (beakers < 0) {
			beakers = 0;
		}

		AttrSource as = new AttrSource(sources, beakers, null);
		cache.sources = as;
		this.attributeCache.put("BEAKERS", cache);
		return as;
	}

	/* Gets the basic amount of happiness for a town. */
	public AttrSource getHappiness() {
		HashMap<String, Double> sources = new HashMap<String, Double>();
		double total = 0;

		AttrCache cache = this.attributeCache.get("HAPPINESS");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}

		/* Add happiness from town level. */
		double townlevel = CivSettings.townHappinessLevels.get(this.getLevel()).happiness;
		total += townlevel;
		sources.put("Base Happiness", townlevel);

		/* Grab any sources from buffs. */
		double goodiesWonders = this.buffManager.getEffectiveDouble("buff_hedonism");
		goodiesWonders += this.buffManager.getEffectiveDouble("buff_globe_theatre_happiness_to_towns");
		goodiesWonders += this.buffManager.getEffectiveDouble("buff_colosseum_happiness_to_towns");
		goodiesWonders += this.buffManager.getEffectiveDouble("buff_colosseum_happiness_for_town");
		sources.put("Goodies/Wonders", goodiesWonders);
		total += goodiesWonders;

		/* Grab happiness from the number of trade goods socketed. */
		int tradeGoods = this.bonusGoodies.size();
		if (tradeGoods > 0) {
			sources.put("Trade Goods", (double) tradeGoods);
		}
		total += tradeGoods;

		/* Add in base happiness if it exists. */
		if (this.baseHappy != 0) {
			sources.put("Base Happiness", this.baseHappy);
			total += baseHappy;
		}

		/* Grab beakers generated from culture. */
		double fromCulture = 0;
		for (CultureChunk cc : this.cultureChunks.values()) {
			fromCulture += cc.getHappiness();
		}
		sources.put("Culture Biomes", fromCulture);
		total += fromCulture;

		/* Grab happiness generated from structures with components. */
		double structures = 0;
		for (Structure struct : this.SM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("HAPPINESS")) {
						structures += as.getGenerated();
					}
				}
			}
		}
		total += structures;
		sources.put("Structures", structures);

		if (total < 0) {
			total = 0;
		}

		double randomEvent = RandomEvent.getHappiness(this);
		total += randomEvent;
		sources.put("Random Events", randomEvent);

		// TODO Governments

		AttrSource as = new AttrSource(sources, total, null);
		cache.sources = as;
		this.attributeCache.put("HAPPINESS", cache);
		return as;
	}

	/* Gets the basic amount of happiness for a town. */
	public AttrSource getUnhappiness() {

		AttrCache cache = this.attributeCache.get("UNHAPPINESS");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS * 1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}

		HashMap<String, Double> sources = new HashMap<String, Double>();

		/* Get the unhappiness from the civ. */
		double total = this.getCiv().getCivWideUnhappiness(sources);

		/* Get unhappiness from residents. */
		double per_resident;
		try {
			per_resident = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_resident");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return null;
		}
		// HashSet<Resident> UnResidents = new HashSet<Resident>();
		// HashSet<Resident> NonResidents = new HashSet<Resident>();
		// for (PermissionGroup group : this.getGroups()) {
		// for (Resident res : group.getMemberList()) {
		// if (res.getCiv() != null) {
		// if (res.getCiv() != this.getCiv()) {
		// NonResidents.add(res);
		// }
		// } else {
		// UnResidents.add(res);
		// }
		// }
		// }

		double happy_resident = per_resident * this.getResidents().size();
		// double happy_Nonresident = (per_resident * 0.25) * NonResidents.size();
		// double happy_Unresident = per_resident * UnResidents.size();
		sources.put("Residents", (happy_resident));// + happy_Nonresident + happy_Unresident));
		total += happy_resident;// + happy_Nonresident + happy_Unresident;

		/* Try to reduce war unhappiness via the component. */
		if (sources.containsKey("War")) {
			for (Structure struct : this.SM.getStructures()) {
				for (Component comp : struct.attachedComponents) {
					if (!comp.isActive()) continue;

					if (comp instanceof AttributeWarUnhappiness) {
						AttributeWarUnhappiness warunhappyComp = (AttributeWarUnhappiness) comp;
						double value = sources.get("War"); // Negative if a reduction
						value += warunhappyComp.value;

						if (value < 0) value = 0;

						sources.put("War", value);
					}
				}
			}
		}

		/* Add in base unhappiness if it exists. */
		if (this.baseUnhappy != 0) {
			sources.put("Base Unhappiness", this.baseUnhappy);
			total += this.baseUnhappy;
		}

		/* Grab unhappiness generated from structures with components. */
		double structures = 0;
		for (Structure struct : this.SM.getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("UNHAPPINESS")) {
						structures += as.getGenerated();
					}
				}
			}
		}
		total += structures;
		sources.put("Structures", structures);

		/* Grab unhappiness from Random events. */
		double randomEvent = RandomEvent.getUnhappiness(this);
		total += randomEvent;
		if (randomEvent > 0) {
			sources.put("Random Events", randomEvent);
		}

		// TODO Spy Missions
		// TODO Governments

		if (total < 0) {
			total = 0;
		}

		AttrSource as = new AttrSource(sources, total, null);
		cache.sources = as;
		this.attributeCache.put("UNHAPPINESS", cache);
		return as;
	}

	/* Gets the rate at which we will modify other stats based on the happiness level. */
	public double getHappinessModifier() {
		return 1.0;
	}

	public double getHappinessPercentage() {
		double total_happiness = getHappiness().total;
		double total_unhappiness = getUnhappiness().total;

		double total = total_happiness + total_unhappiness;
		return total_happiness / total;
	}

	public ConfigHappinessState getHappinessState() {
		return CivSettings.getHappinessState(this.getHappinessPercentage());
	}

	public void setBaseHappiness(double happy) {
		this.baseHappy = happy;
	}

	public void setBaseUnhappy(double happy) {
		this.baseUnhappy = happy;
	}

	public double getBaseGrowth() {
		return baseGrowth;
	}

	public void setBaseGrowth(double baseGrowth) {
		this.baseGrowth = baseGrowth;
	}

	public double getUnusedBeakers() {
		return unusedBeakers;
	}

	public void setUnusedBeakers(double unusedBeakers) {
		this.unusedBeakers = unusedBeakers;
	}

	public void addUnusedBeakers(double more) {
		this.unusedBeakers += more;
	}

	public double getBonusCottageRate() {
		double rate = 1.0;
		for (final String goodID : this.tradeGoods.split(", ")) {
			if (CivSettings.goods.get(goodID) != null) {
				for (final ConfigBuff configBuff : CivSettings.goods.get(goodID).buffs.values()) {
					if (configBuff.id.equals("buff_demand")) {
						rate += 0.05;
					}
				}
			}
		}
		return rate;
	}

	public double getBonusUpkeep() {
		if (this.getCiv().getCapitol() != null && this.getCiv().getCapitol().getBuffManager().hasBuff("level8_noWarUpkeep")) {
			return 1.0;
		}
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
			return 1.0;
		}
		boolean enemyHasNotre = false;
		for (final Relation relation : this.getCiv().getDiplomacyManager().getRelations()) {
			if (relation.getStatus().equals(Relation.Status.WAR)) {
				if (relation.isDeleted()) {
					continue;
				}
				if (this.getCiv() != relation.getAggressor()) {
					continue;
				}
				for (final Town town : relation.getOtherCiv().getTowns()) {
					if (town.SM.hasWonder("w_notre_dame")) {
						enemyHasNotre = true;
						break;
					}
				}
			}
		}
		double total = 1.0;
		for (final Relation relation2 : this.getCiv().getDiplomacyManager().getRelations()) {
			if (relation2.getStatus().equals(Relation.Status.WAR)) {
				if (relation2.isDeleted()) {
					continue;
				}
				if (this.getCiv() != relation2.getAggressor()) {
					continue;
				}
				total = 2.5;
				break;
			}
		}
		if (enemyHasNotre) {
			total = 5.0;
		}
		for (final Structure structure : this.SM.getStructures()) {
			for (final Component comp : structure.attachedComponents) {
				if (comp instanceof AttributeWarUnpkeep) {
					total -= ((AttributeWarUnpkeep) comp).value;
				}
			}
		}
		return (total < 1.0) ? 1.0 : total;
	}

	// ------------- outlaw

	public boolean isOutlaw(Resident res) {
		return this.outlaws.contains(res.getUuid().toString());
	}

	public boolean isOutlaw(String name) {
		Resident res = CivGlobal.getResident(name);
		return this.outlaws.contains(res.getUuid().toString());
	}

	public void addOutlaw(String name) {
		Resident res = CivGlobal.getResident(name);
		this.outlaws.add(res.getUuid().toString());
		TaskMaster.syncTask(new SyncUpdateTags(res.getUuid().toString(), this.residents.values()));
	}

	public void removeOutlaw(String name) {
		Resident res = CivGlobal.getResident(name);
		this.outlaws.remove(res.getUuid().toString());
		TaskMaster.syncTask(new SyncUpdateTags(res.getUuid().toString(), this.residents.values()));
	}

	// ---------- InDebt

	public void incrementDaysInDebt() {
		daysInDebt++;

		if (daysInDebt >= CivSettings.TOWN_DEBT_GRACE_DAYS) {
			if (daysInDebt >= CivSettings.TOWN_DEBT_SELL_DAYS) {
				this.delete();
				CivMessage.global(CivSettings.localize.localizedString("var_town_ruin1", this.getName()));
				return;
			}
		}

		CivMessage.global(CivSettings.localize.localizedString("var_town_inDebt", this.getName()) + getDaysLeftWarning());
	}

	public String getDaysLeftWarning() {

		if (daysInDebt < CivSettings.TOWN_DEBT_GRACE_DAYS) {
			return " " + CivSettings.localize.localizedString("var_town_inDebt_daysTilSale", (CivSettings.TOWN_DEBT_GRACE_DAYS - daysInDebt));
		}

		if (daysInDebt < CivSettings.TOWN_DEBT_SELL_DAYS) {
			return " " + CivSettings.localize.localizedString("var_town_inDebt_daysTilDelete", this.getName(), CivSettings.TOWN_DEBT_SELL_DAYS - daysInDebt);
		}

		return "";
	}

	public int getDaysInDebt() {
		return daysInDebt;
	}

	public void setDaysInDebt(int daysInDebt) {
		this.daysInDebt = daysInDebt;
	}

	public boolean isForSale() {
		if (this.getCiv().isTownsForSale()) return true;
		if (!this.inDebt()) return false;
		if (daysInDebt >= CivSettings.TOWN_DEBT_GRACE_DAYS) return true;
		return false;
	}

	public double getForSalePrice() {
		int points = this.getScore();
		try {
			double coins_per_point = CivSettings.getDouble(CivSettings.scoreConfig, "coins_per_point");
			return coins_per_point * points;
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return 0.0;
		}
	}

	// ------------ diplomati

	public void onDefeat(Civilization attackingCiv) {
		/* We've been defeated. If we don't have our mother civilization set, this means this is the first time this town has been conquered. */
		if (this.getMotherCiv() == null) {
			/* Save our motherland in case we ever get liberated. */
			this.setMotherCiv(this.civ);
		} else {
			/* If we've been liberated by our motherland, set things right. */
			if (this.getMotherCiv() == attackingCiv) {
				this.setMotherCiv(null);
			}
		}

		this.changeCiv(attackingCiv);
		this.save();
	}

	public void changeCiv(Civilization newCiv) {
		/* Remove this town from its old civ. */
		Civilization oldCiv = this.civ;
		oldCiv.removeTown(this);
		oldCiv.save();

		/* Add this town to the new civ. */
		newCiv.addTown(this);
		newCiv.save();

		/* Remove any outlaws which are in our new civ. */
		LinkedList<String> removeUs = new LinkedList<String>();
		for (String outlaw : this.outlaws) {
			if (outlaw.length() >= 2) {
				Resident resident = CivGlobal.getResidentViaUUID(UUID.fromString(outlaw));
				if (newCiv.hasResident(resident)) {
					removeUs.add(outlaw);
				}
			}
		}

		for (String outlaw : removeUs) {
			this.outlaws.remove(outlaw);
		}

		this.setCiv(newCiv);
		CivGlobal.processCulture();

		this.save();
	}

	public Civilization getDepositCiv() {
		// Get the civilization we are going to deposit taxes to.
		return this.getCiv();
	}

	public Civilization getMotherCiv() {
		return motherCiv;
	}

	public void setMotherCiv(Civilization motherCiv) {
		this.motherCiv = motherCiv;
	}

	public void capitulate() {
		if (this.getMotherCiv() == null) return;

		if (this.getId() == this.getMotherCiv().getCapitolId()) {
			this.getMotherCiv().capitulate();
			return;
		}

		/* Town is capitulating, no longer need a mother civ. */
		this.setMotherCiv(null);
		this.save();
		CivMessage.global(CivSettings.localize.localizedString("var_town_capitulate1", this.getName(), this.getCiv().getName()));
	}

	public void validateGift() throws CivException {
		try {
			int min_gift_age = CivSettings.getInteger(CivSettings.civConfig, "civ.min_gift_age");

			if (!DateUtil.isAfterDays(getCreatedDate(), min_gift_age)) {
				throw new CivException(CivSettings.localize.localizedString("var_town_gift_errorAge1", this.getName(), min_gift_age));
			}
		} catch (InvalidConfiguration e) {
			throw new CivException("Configuration error.");
		}
	}

	public int getGiftCost() {
		int gift_cost;
		try {
			gift_cost = CivSettings.getInteger(CivSettings.civConfig, "civ.gift_cost_per_town");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return 0;
		}
		return gift_cost;
	}

	// ---------------- units

	/** проверка, можно ли строить юнита в этом городе */
	public ArrayList<ConfigUnit> getAvailableUnits() {
		ArrayList<ConfigUnit> unitList = new ArrayList<ConfigUnit>();

		for (ConfigUnit unit : UnitStatic.configUnits.values()) {
			if (unit.isAvailable(this)) unitList.add(unit);
		}
		return unitList;
	}

	private static String lastMessage = null;

	public boolean processSpyExposure(Resident resident) {
		// double exposure = resident.getSpyExposure();
		// double percent = exposure / Resident.MAX_SPY_EXPOSURE;
		boolean failed = false;

		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e1) {
			e1.printStackTrace();
			return failed;
		}

		String message = "";
		// try {
		// if (percent >= CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_failure")) {
		// failed = true;
		// CivMessage.sendTown(this, CivColor.Yellow + CivColor.BOLD + CivSettings.localize.localizedString("town_spy_thwarted"));
		// return failed;
		// }
		// if (percent > CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_warning")) {
		message += CivSettings.localize.localizedString("town_spy_currently") + " ";
		// }
		// if (percent > CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_location")) {
		message += CivSettings.localize.localizedString("var_town_spy_location", (player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ())) + " ";
		// }
		// if (percent > CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_name")) {
		message += CivSettings.localize.localizedString("var_town_spy_perpetrator", resident.getName());
		// }
		if (message.length() > 0) {
			if (lastMessage == null || !lastMessage.equals(message)) {
				CivMessage.sendTown(this, CivColor.Yellow + CivColor.BOLD + message);
				lastMessage = message;
			}
		}
		// } catch (InvalidConfiguration e) {
		// e.printStackTrace();
		// }

		return failed;
	}

	public int getArtifactTypeCount(final String id) {
		return 0;
	}

	// TODO Scroll --------------

	public boolean hasScroll() {
		final String key = "scrollHammers_" + this.getId();
		final ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
		if (entries == null || entries.size() < 1) {
			return false;
		}
		final SessionEntry cd = entries.get(0);
		final long till = Long.parseLong(cd.value);
		if (till > Calendar.getInstance().getTimeInMillis()) {
			return true;
		}
		this.removeScroll();
		return false;
	}

	public String getScrollTill() {
		final String key = "scrollHammers_" + this.getId();
		final ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
		if (entries == null || entries.size() < 1) {
			return null;
		}
		final SessionEntry cd = entries.get(0);
		long till = Long.parseLong(cd.value);
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		return sdf.format(till);
	}

	public void addScroll(final long time) {
		final String key = "scrollHammers_" + this.getId();
		final String value = Calendar.getInstance().getTimeInMillis() + time + "";
		final ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
		if (entries == null || entries.size() < 1) {
			CivGlobal.getSessionDatabase().add(key, value, 0, 0, 0);
			return;
		}
		CivGlobal.getSessionDatabase().update(entries.get(0).request_id, key, value);
	}

	public void removeScroll() {
		final String key = "scrollHammers_" + this.getId();
		CivGlobal.getSessionDatabase().delete_all(key);
		CivMessage.sendTown(this, CivSettings.localize.localizedString("var_scrollEnded"));
	}

	// ----------- other

	public ConfigGovernment getGovernment() {
		if (this.getCiv().getGovernment().id.equals("gov_anarchy")) {
			if (this.motherCiv != null && !this.motherCiv.getGovernment().id.equals("gov_anarchy")) {
				return this.motherCiv.getGovernment();
			}

			if (this.motherCiv != null) {
				return CivSettings.governments.get("gov_tribalism");
			}
		}

		return this.getCiv().getGovernment();
	}

	public boolean isValid() {
		return getCityhall() != null;
	}

	public boolean isActive() {
		if (getCityhall() == null) return false;
		return getCityhall().isActive();
	}

	public boolean isCapitol() {
		return this.getCiv().getCapitolId() == this.getId();
	}

}
