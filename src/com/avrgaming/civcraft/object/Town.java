/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.object;

import java.io.IOException;
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
import java.util.List;
import java.util.Set;
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
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigHappinessState;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.interactive.InteractiveBuildableRefresh;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Bank;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.structure.Cottage;
import com.avrgaming.civcraft.structure.Mine;
import com.avrgaming.civcraft.structure.MineHouse;
import com.avrgaming.civcraft.structure.Quarry;
import com.avrgaming.civcraft.structure.Road;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Temple;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.structure.TradeOutpost;
import com.avrgaming.civcraft.structure.TradeShip;
import com.avrgaming.civcraft.structure.Wall;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncUpdateTags;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.threading.tasks.BuildTemplateTask;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitInventory;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.TagManager;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomTemplate;

@Getter
@Setter
public class Town extends SQLObject {

	private ConcurrentHashMap<String, Resident> residents = new ConcurrentHashMap<String, Resident>();
	private ConcurrentHashMap<String, Resident> fakeResidents = new ConcurrentHashMap<String, Resident>();

	private ConcurrentHashMap<ChunkCoord, TownChunk> townChunks = new ConcurrentHashMap<ChunkCoord, TownChunk>();
	private ConcurrentHashMap<ChunkCoord, TownChunk> outposts = new ConcurrentHashMap<ChunkCoord, TownChunk>();
	private ConcurrentHashMap<ChunkCoord, CultureChunk> cultureChunks = new ConcurrentHashMap<ChunkCoord, CultureChunk>();

	private ConcurrentHashMap<BlockCoord, Wonder> wonders = new ConcurrentHashMap<BlockCoord, Wonder>();
	private ConcurrentHashMap<BlockCoord, Structure> structures = new ConcurrentHashMap<BlockCoord, Structure>();
	private ConcurrentHashMap<BlockCoord, Buildable> disabledBuildables = new ConcurrentHashMap<BlockCoord, Buildable>();

	public UnitInventory unitInventory = new UnitInventory(this);

	private int level;
	private double taxRate;
	private double flatTax;
	private Civilization civ;
	private Civilization motherCiv;
	private int daysInDebt;

	/* Hammers */
	private double baseHammers = 1.0;
	private double extraHammers;
	public Buildable currentStructureInProgress;
	public Buildable currentWonderInProgress;

	/* Culture */
	private int culture;

	private PermissionGroup defaultGroup;
	private PermissionGroup mayorGroup;
	private PermissionGroup assistantGroup;

	/* Beakers */
	private double unusedBeakers;

	// These are used to resolve reverse references after the database loads.
	private String defaultGroupName;
	private String mayorGroupName;
	private String assistantGroupName;

	public ArrayList<TownChunk> savedEdgeBlocks = new ArrayList<TownChunk>();
	public HashSet<Town> townTouchList = new HashSet<Town>();

	private ConcurrentHashMap<String, PermissionGroup> groups = new ConcurrentHashMap<String, PermissionGroup>();
	private EconObject treasury;
	private ConcurrentHashMap<String, ConfigTownUpgrade> upgrades = new ConcurrentHashMap<String, ConfigTownUpgrade>();

	/* This gets populated periodically from a synchronous timer so it will be accessible from async tasks. */
	private ConcurrentHashMap<String, BonusGoodie> bonusGoodies = new ConcurrentHashMap<String, BonusGoodie>();

	private BuffManager buffManager = new BuffManager();

	private boolean pvp = false;

	public ArrayList<BuildAsyncTask> build_tasks = new ArrayList<BuildAsyncTask>();
	public Buildable lastBuildableBuilt = null;

	public boolean leaderWantsToDisband = false;
	public boolean mayorWantsToDisband = false;
	public HashSet<String> outlaws = new HashSet<String>();

	public boolean claimed = false;
	public boolean defeated = false;
	public LinkedList<Buildable> invalidStructures = new LinkedList<Buildable>();

	/* XXX kind of a hacky way to save the bank's level information between build undo calls */
	public int saved_bank_level = 1;
	public int saved_store_level = 1;
	public int saved_library_level = 1;
	public int saved_trommel_level = 1;
	public int saved_tradeship_upgrade_levels = 1;
	public int saved_grocer_levels = 1;
	public int saved_alch_levels = 1;
	public int saved_quarry_level = 1;
	public int saved_fish_hatchery_level = 1;
	public double saved_bank_interest_amount = 0;
	public int saved_stock_exchange_level = 1;

	/* Happiness Stuff */
	private double baseHappy = 0.0;
	private double baseUnhappy = 0.0;

	private RandomEvent activeEvent;

	/* Last time someone used /build refreshblocks, make sure they can do it only so often. */
	private Date lastBuildableRefresh = null;
	private Date createdDate;

	public String tradeGoods = "";
	private long conqueredDate = 0L;

	/* Time it takes before a new attribute is calculated Otherwise its loaded from the cache. */
	public static final int ATTR_TIMEOUT_SECONDS = 5;

	class AttrCache {
		public Date lastUpdate;
		public AttrSource sources;
	}

	public HashMap<String, AttrCache> attributeCache = new HashMap<String, AttrCache>();

	private double baseGrowth = 0.0;

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
					+ "`flat_tax` double NOT NULL DEFAULT '0',"//
					+ "`tax_rate` double DEFAULT 0,"//
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

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.setLevel(rs.getInt("level"));
		this.setCiv(CivGlobal.getCivFromId(rs.getInt("civ_id")));

		Integer motherCivId = rs.getInt("mother_civ_id");
		if (motherCivId != null && motherCivId != 0) {
			Civilization mother = CivGlobal.getConqueredCivFromId(motherCivId);
			if (mother == null) {
				mother = CivGlobal.getCivFromId(motherCivId);
			}

			if (mother == null) {
				CivLog.warning("Unable to find a mother civ with ID:" + motherCivId + "!");
			} else {
				setMotherCiv(mother);
			}
		}

		if (this.getCiv() == null) {
			CivLog.error("TOWN:" + this.getName() + " WITHOUT A CIV, id was:" + rs.getInt("civ_id"));
			// this.delete();
			CivGlobal.orphanTowns.add(this);
			throw new CivException("Failed to load town, bad data.");
		}
		this.setDaysInDebt(rs.getInt("daysInDebt"));
		this.setFlatTax(rs.getDouble("flat_tax"));
		this.setTaxRate(rs.getDouble("tax_rate"));
		this.setUpgradesFromString(rs.getString("upgrades"));

		// this.setHomeChunk(rs.getInt("homechunk_id"));
		this.setExtraHammers(rs.getDouble("extra_hammers"));
		this.setAccumulatedCulture(rs.getInt("culture"));
		this.conqueredDate = rs.getLong("conquered_date");

		defaultGroupName = "residents";
		mayorGroupName = "mayors";
		assistantGroupName = "assistants";

		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(rs.getDouble("coins"), false);
		this.setDebt(rs.getDouble("debt"));
		if (rs.getString("tradeGoods") != null) {
			this.tradeGoods = rs.getString("tradeGoods");
		} else {
			this.tradeGoods = "";
		}

		String outlawRaw = rs.getString("outlaws");
		if (outlawRaw != null) {
			String[] outlaws = outlawRaw.split(",");

			for (String outlaw : outlaws) {
				this.outlaws.add(outlaw);
			}
		}

		Long ctime = rs.getLong("created_date");
		if (ctime == null || ctime == 0) {
			this.setCreatedDate(new Date(0)); // Forever in the past.
		} else {
			this.setCreatedDate(new Date(ctime));
		}

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

		hashmap.put("defaultGroupName", this.getDefaultGroupName());
		hashmap.put("mayorGroupName", this.getMayorGroupName());
		hashmap.put("assistantGroupName", this.getAssistantGroupName());
		hashmap.put("level", this.getLevel());
		hashmap.put("debt", this.getTreasury().getDebt());
		hashmap.put("daysInDebt", this.getDaysInDebt());
		hashmap.put("flat_tax", this.getFlatTax());
		hashmap.put("tax_rate", this.getTaxRate());
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
			/* Remove all our Groups */
			for (PermissionGroup grp : this.groups.values()) {
				grp.delete();
			}

			/* Remove all of our residents from town. */
			for (Resident resident : this.residents.values()) {
				resident.setTown(null);
				/* Also forgive their debt, nobody to pay it to. */
				resident.getTreasury().setDebt(0);
				resident.saveNow();
			}

			TagManager.editNameTag(this);
			/* Remove all structures in the town. */
			if (this.structures != null) {
				for (Structure struct : this.structures.values()) {
					struct.delete();
				}
			}

			/* Remove all town chunks. */
			if (this.getTownChunks() != null) {
				for (TownChunk tc : this.getTownChunks()) {
					tc.delete();
				}
			}

			if (this.wonders != null) {
				for (Wonder wonder : wonders.values()) {
					wonder.unbindConstructBlocks();
					try {
						wonder.undoFromTemplate();
					} catch (IOException | CivException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						wonder.fancyDestroyConstructBlocks();
					}
					wonder.delete();
				}
			}

			if (this.cultureChunks != null) {
				for (CultureChunk cc : this.cultureChunks.values()) {
					CivGlobal.removeCultureChunk(cc);
				}
			}
			this.cultureChunks = null;

			/* Remove any related SessionDB entries */
			CivGlobal.getSessionDatabase().deleteAllForTown(this);

			getCiv().removeTown(this);
			CivGlobal.removeTown(this);
			SQL.deleteNamedObject(this, TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public Town(Civilization civ) {
		this.setLevel(1);
		this.setTaxRate(0.0);
		this.setFlatTax(0.0);
		this.setCiv(civ);
		this.setDaysInDebt(0);
		this.setHammerRate(1.0);
		this.setExtraHammers(0);
		this.setAccumulatedCulture(0);
		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(0, false);
		this.setCreatedDate(new Date());

		loadSettings();
	}

	public Town(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.load(rs);
		loadSettings();
	}

	public void loadSettings() {
		try {
			this.baseHammers = CivSettings.getDouble(CivSettings.townConfig, "town.base_hammer_rate");
			this.setBaseGrowth(CivSettings.getDouble(CivSettings.townConfig, "town.base_growth_rate"));

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

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

	public ConfigTownUpgrade getUpgrade(String id) {
		return upgrades.get(id);
	}

	public boolean isMayor(Resident res) {
		if (this.getMayorGroup().hasMember(res)) {
			return true;
		}
		return false;
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

	public void addResident(Resident res) throws AlreadyRegisteredException {
		String key = res.getName().toLowerCase();

		if (residents.containsKey(key)) {
			throw new AlreadyRegisteredException(res.getName() + " already a member of town " + this.getName());
		}

		res.setTown(this);

		residents.put(key, res);
		if (this.defaultGroup != null && !this.defaultGroup.hasMember(res)) {
			this.defaultGroup.addMember(res);
			this.defaultGroup.save();
		}
		// TODO
		// Player player = Bukkit.getPlayer(res.getUid());
		/* if (player != null && CivSettings.hasITag) { Bukkit.getScheduler().runTask(CivCraft.getPlugin(), () -> //XXX from furnex
		 * iTag.getInstance().refreshPlayer(player, new HashSet<>(Bukkit.getOnlinePlayers()))); } */
	}

	public void addTownChunk(TownChunk tc) throws AlreadyRegisteredException {

		if (townChunks.containsKey(tc.getChunkCoord())) {
			throw new AlreadyRegisteredException("TownChunk at " + tc.getChunkCoord() + " already registered to town " + this.getName());
		}
		townChunks.put(tc.getChunkCoord(), tc);
	}

	public Structure findStructureByName(String name) {
		for (Structure struct : structures.values()) {
			if (struct.getDisplayName().equalsIgnoreCase(name)) {
				return struct;
			}
		}
		return null;
	}

	public Structure findStructureByLocation(BlockCoord bc) {
		return structures.get(bc);
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {

		// TownHall townhall = this.getTownHall();
		// if (townhall != null) {
		// if (townhall.nextGoodieFramePoint.size() > 0 &&
		// townhall.nextGoodieFramePoint.size() > 0) {
		// townhall.createGoodieItemFrame(townhall.nextGoodieFramePoint.get(0), level,
		// townhall.nextGoodieFrameDirection.get(0));
		// }
		// }

		this.level = level;
	}

	public double getTaxRate() {
		return taxRate;
	}

	public void setTaxRate(double taxRate) {
		this.taxRate = taxRate;
	}

	public String getTaxRateString() {
		long rounded = Math.round(this.taxRate * 100);
		return "" + rounded + "%";
	}

	public double getFlatTax() {
		return flatTax;
	}

	public void setFlatTax(double flatTax) {
		this.flatTax = flatTax;
	}

	public Civilization getCiv() {
		return civ;
	}

	public void setCiv(Civilization civ) {
		this.civ = civ;
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
		if (this.getBuffManager().hasBuff("buff_art_appreciation")) {
			structures += this.getBuffManager().getEffectiveDouble("buff_art_appreciation");
		}
		rates.put("Great Works", structures);
		rate += structures;

		double additional = 0;
		if (this.getBuffManager().hasBuff("buff_fine_art")) {
			additional += this.getBuffManager().getEffectiveDouble(Buff.FINE_ART);
		}
		if (this.getBuffManager().hasBuff("buff_pyramid_culture")) {
			additional += this.getBuffManager().getEffectiveDouble("buff_pyramid_culture");
		}
		if (this.getBuffManager().hasBuff("buff_neuschwanstein_culture")) {
			additional += this.getBuffManager().getEffectiveDouble("buff_neuschwanstein_culture");
		}

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
		if (this.getBuffManager().hasBuff("buff_advanced_touring")) {
			goodieCulture += 200.0;
		}
		sources.put("Goodies", goodieCulture);
		total += goodieCulture;
		/* Grab beakers generated from structures with components. */
		double fromStructures = 0;
		for (Structure struct : this.structures.values()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("CULTURE")) {
						fromStructures += as.getGenerated();
					}
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

		if (total < 0) {
			total = 0;
		}

		AttrSource as = new AttrSource(sources, total, rate);
		cache.sources = as;
		this.attributeCache.put("CULTURE", cache);
		return as;
	}

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
		for (Structure struct : this.structures.values()) {
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

	public void checkCanCreatedTown(Resident resident, Structure structure) throws CivException {
		if (resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("town_found_errorIncamp"));
		if (resident.getTown() != null && resident.getTown().isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("var_town_found_errorIsMayor", resident.getTown().getName()));

		if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("town_found_errorAtWar"));
		if (getCiv() != null) {
			Double costTown = 10000.0 * (getCiv().getTownCount()); // TODO для основания города в казне цивилизации должно быть
			if (!getCiv().getTreasury().hasEnough(costTown)) throw new CivException("TODO для основания города в казне цивилизации должно быть " + costTown + " коинов");
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
			Location loc2 = town.getLocation();
			if (loc2 == null) continue;

			double distSqr = loc2.distanceSquared(structure.getCenterLocation());
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

	public void createTown(Resident resident, Structure structure) throws CivException {
		Player player = CivGlobal.getPlayer(resident.getName());
		try {
			this.saveNow();
			CivGlobal.addTown(this);
			getCiv().addTown(this);

			// Create permission groups for town.
			try {
				PermissionGroup residentsGroup = new PermissionGroup(this, "residents");
				this.setDefaultGroup(residentsGroup);
				try {
					if (resident.getTown() != null) {
						CivMessage.sendTown(resident.getTown(), CivSettings.localize.localizedString("var_town_found_leftTown", resident.getName()));
						resident.getTown().removeResident(resident);
					}
					this.addResident(resident);
					resident.saveNow();
				} catch (AlreadyRegisteredException e) {
					e.printStackTrace();
					throw new CivException(CivSettings.localize.localizedString("town_found_residentError"));
				}

				PermissionGroup mayorGroup = new PermissionGroup(this, "mayors");
				mayorGroup.addMember(resident);
				mayorGroup.saveNow();
				this.setMayorGroup(mayorGroup);

				PermissionGroup assistantGroup = new PermissionGroup(this, "assistants");
				assistantGroup.saveNow();
				this.setAssistantGroup(assistantGroup);
			} catch (InvalidNameException e2) {
				e2.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
			}

			// build TownHall
			try {
				this.getTreasury().deposit(structure.getCost());
				structure.setSQLOwner(this);
				this.buildStructure(player, structure);
			} catch (CivException e) {
				getCiv().removeTown(this);
				this.delete();
				throw e;
			}

			getCiv().getTreasury().deposit(10000.0 * (getCiv().getTownCount() - 1)); // TODO

			CivGlobal.processCulture();
			this.saveNow();
			return;
		} catch (SQLException e2) {
			e2.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}
	}

	public Location getLocation() {
		Townhall townhall = getTownHall();
		if (townhall == null) return null;
		return townhall.getCenterLocation();
	}

	public PermissionGroup getDefaultGroup() {
		return defaultGroup;
	}

	public void setDefaultGroup(PermissionGroup defaultGroup) {
		this.defaultGroup = defaultGroup;
		this.groups.put(defaultGroup.getName(), defaultGroup);
	}

	public Collection<PermissionGroup> getGroups() {
		return groups.values();
	}

	public PermissionGroup getGroup(String name) {
		return groups.get(name);
	}

	public PermissionGroup getGroupFromId(Integer id) {
		for (PermissionGroup grp : groups.values()) {
			if (grp.getId() == id) {
				return grp;
			}
		}
		return null;
	}

	public void addGroup(PermissionGroup grp) {

		if (grp.getName().equalsIgnoreCase(this.defaultGroupName)) {
			this.defaultGroup = grp;
		} else
			if (grp.getName().equalsIgnoreCase(this.mayorGroupName)) {
				this.mayorGroup = grp;
			} else
				if (grp.getName().equalsIgnoreCase(this.assistantGroupName)) {
					this.assistantGroup = grp;
				}

		groups.put(grp.getName(), grp);

	}

	public void removeGroup(PermissionGroup grp) {
		groups.remove(grp.getName());
	}

	public boolean hasGroupNamed(String name) {
		for (PermissionGroup grp : groups.values()) {
			if (grp.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public PermissionGroup getGroupByName(String name) {
		for (PermissionGroup grp : groups.values()) {
			if (grp.getName().equalsIgnoreCase(name)) {
				return grp;
			}
		}
		return null;
	}

	public String getDefaultGroupName() {
		// if (this.defaultGroup == null) {
		// return "none";
		// }
		// return this.defaultGroup.getName();
		return "residents";
	}

	public PermissionGroup getMayorGroup() {
		return mayorGroup;
	}

	public void setMayorGroup(PermissionGroup mayorGroup) {
		this.mayorGroup = mayorGroup;
		this.groups.put(mayorGroup.getName(), mayorGroup);

	}

	public PermissionGroup getAssistantGroup() {
		return assistantGroup;
	}

	public void setAssistantGroup(PermissionGroup assistantGroup) {
		this.assistantGroup = assistantGroup;
		this.groups.put(assistantGroup.getName(), assistantGroup);

	}

	public String getMayorGroupName() {
		return "mayors";
		// if (this.mayorGroup == null) {
		// return "none";
		// }
		// return this.mayorGroup.getName();
	}

	public String getAssistantGroupName() {
		// if (this.assistantGroup == null) {
		// return "none";
		// }
		// return this.assistantGroup.getName();
		return "assistants";
	}

	public boolean isProtectedGroup(PermissionGroup grp) {
		return grp.isProtectedGroup();
	}

	public boolean playerIsInGroupName(String groupName, Player player) {
		PermissionGroup grp = this.getGroupByName(groupName);
		if (grp == null) {
			return false;
		}

		Resident resident = CivGlobal.getResident(player);
		return grp.hasMember(resident);
	}

	public EconObject getTreasury() {
		return treasury;
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

	public void setTreasury(EconObject treasury) {
		this.treasury = treasury;
	}

	public String getLevelTitle() {
		ConfigTownLevel clevel = CivSettings.townLevels.get(this.level);
		if (clevel == null) {
			return "Unknown";
		} else {
			return clevel.title;
		}
	}

	public void addUpgrade(ConfigTownUpgrade upgrade) {

		try {
			upgrade.processAction(this);
		} catch (CivException e) {}
		this.upgrades.put(upgrade.id, upgrade);
		this.save();
	}

	public void purchaseUpgrade(ConfigTownUpgrade upgrade) throws CivException {
		if (!this.hasUpgrade(upgrade.require_upgrade)) {
			throw new CivException(CivSettings.localize.localizedString("town_missingUpgrades"));
		}

		if (!this.getTreasury().hasEnough(upgrade.cost)) {
			throw new CivException(CivSettings.localize.localizedString("var_town_missingFunds", upgrade.cost, CivSettings.CURRENCY_NAME));
		}

		if (!this.hasStructure(upgrade.require_structure)) {
			throw new CivException(CivSettings.localize.localizedString("town_missingStructures"));
		}

		if (upgrade.id.equalsIgnoreCase("upgrade_stock_exchange_level_6") && !this.canUpgradeStock(upgrade.id)) {
			throw new CivException("§c" + CivSettings.localize.localizedString("var_upgradeStockExchange_nogoodCondition", "http://wiki.minetexas.com/index.php/Stock_Exchange"));
		}

		if (!this.hasWonder(upgrade.require_wonder)) {
			throw new CivException(CivSettings.localize.localizedString("town_missingWonders"));
		}

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

	public boolean canBuildStock(final Player player) {
		int bankCount = 0;
		int tradeShipCount = 0;
		int cottageCount = 0;
		int quarryCount = 0;
		for (final Town t : this.getCiv().getTowns()) {
			for (final Structure structure : t.getStructures()) {
				if (structure instanceof Bank && ((Bank) structure).getLevel() == 10) {
					++bankCount;
				}
				if (structure instanceof TradeShip && ((TradeShip) structure).getLevel() >= 7) {
					++tradeShipCount;
				}
				if (structure instanceof Cottage && ((Cottage) structure).getLevel() >= 5) {
					++cottageCount;
				}
				if (structure instanceof Quarry && ((Quarry) structure).getLevel() >= 3) {
					++quarryCount;
				}
			}
		}
		boolean bankCountCondition = false;
		boolean tradeShipCountCondition = false;
		boolean cottageCountCondition = false;
		boolean quarryCountCondition = false;
		if (bankCount >= 3) {
			bankCountCondition = true;
		}
		if (tradeShipCount >= 3) {
			tradeShipCountCondition = true;
		}
		if (cottageCount >= 15) {
			cottageCountCondition = true;
		}
		if (quarryCount >= 3) {
			quarryCountCondition = true;
		}
		CivMessage.sendCiv(this.getCiv(), (bankCountCondition ? "§a" : "§c") + "Number of lvl 10 Banks: " + "§e" + (bankCountCondition ? "Done " : "Incomplete ") + bankCount + "/3");
		CivMessage.sendCiv(this.getCiv(), (tradeShipCountCondition ? "§a" : "§c") + "Number of lvl 7 Trade Ships: " + "§e" + (tradeShipCountCondition ? "Done " : "Incomplete ") + tradeShipCount + "/3");
		CivMessage.sendCiv(this.getCiv(), (cottageCountCondition ? "§a" : "§c") + "Number of lvl 5 Cottages: " + "§e" + (cottageCountCondition ? "Done " : "Incomplete ") + cottageCount + "/15");
		CivMessage.sendCiv(this.getCiv(), (quarryCountCondition ? "§a" : "§c") + "Number of lvl 3 Quarries: " + "§e" + (quarryCountCondition ? "Done " : "Incomplete ") + quarryCount + "/3");
		return bankCountCondition && tradeShipCountCondition && cottageCountCondition && quarryCountCondition;
	}

	public boolean canUpgradeStock(final String upgradeName) {
		int bankCount = 0;
		int tradeShipCount = 0;
		int cottageCount = 0;
		int quarryCount = 0;
		for (final Town town : this.getCiv().getTowns()) {
			for (final Structure structure : town.getStructures()) {
				if (structure instanceof Bank && ((Bank) structure).getLevel() == 10) {
					++bankCount;
				}
				if (structure instanceof TradeShip && ((TradeShip) structure).getLevel() >= 8) {
					++tradeShipCount;
				}
				if (structure instanceof Cottage && ((Cottage) structure).getLevel() >= 6) {
					++cottageCount;
				}
				if (structure instanceof Quarry && ((Quarry) structure).getLevel() >= 4) {
					++quarryCount;
				}
			}
		}
		boolean bankCountCondition = false;
		boolean tradeShipCountCondition = false;
		boolean cottageCountCondition = false;
		boolean quarryCountCondition = false;
		if (bankCount >= 3) {
			bankCountCondition = true;
		}
		if (tradeShipCount >= 3) {
			tradeShipCountCondition = true;
		}
		if (cottageCount >= 20) {
			cottageCountCondition = true;
		}
		if (quarryCount >= 3) {
			quarryCountCondition = true;
		}

		CivMessage.sendCiv(this.getCiv(), (bankCountCondition ? "§a" : "§c") + "Number of lvl 10 Banks: " + "§e" + (bankCountCondition ? "Done " : "Incomplete ") + bankCount + "/3");
		CivMessage.sendCiv(this.getCiv(), (tradeShipCountCondition ? "§a" : "§c") + "Number of lvl 8 Trade Ships: " + "§e" + (tradeShipCountCondition ? "Done " : "Incomplete ") + tradeShipCount + "/3");
		CivMessage.sendCiv(this.getCiv(), (cottageCountCondition ? "§a" : "§c") + "Number of lvl 6 Cottages: " + "§e" + (cottageCountCondition ? "Done " : "Incomplete ") + cottageCount + "/20");
		CivMessage.sendCiv(this.getCiv(), (quarryCountCondition ? "§a" : "§c") + "Number of lvl 4 Quarries: " + "§e" + (quarryCountCondition ? "Done " : "Incomplete ") + quarryCount + "/3");

		return bankCountCondition && tradeShipCountCondition && cottageCountCondition && quarryCountCondition;
	}

	public Structure findStructureByConfigId(String require_structure) {

		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equals(require_structure)) {
				return struct;
			}
		}

		return null;
	}

	public ConcurrentHashMap<String, ConfigTownUpgrade> getUpgrades() {
		return upgrades;
	}

	public boolean isPvp() {
		return pvp;
	}

	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}

	public String getPvpString() {
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
			if (pvp) {
				return CivColor.Red + "[PvP]";
			} else {
				return CivColor.Green + "[No PvP]";
			}
		} else {
			return CivColor.Red + "[WAR-PvP]";
		}
	}

	private void kickResident(Resident resident) {
		/* Repo all this resident's plots. */
		for (TownChunk tc : townChunks.values()) {
			if (tc.perms.getOwner() == resident) {
				tc.perms.setOwner(null);
				tc.perms.replaceGroups(defaultGroup);
				tc.perms.resetPerms();
				tc.save();
			}
		}

		/* Clear resident's debt and remove from town. */
		resident.getTreasury().setDebt(0);
		resident.setDaysTilEvict(0);
		resident.setTown(null);
		resident.setRejoinCooldown(this);

		this.residents.remove(resident.getName().toLowerCase());

		resident.save();
		this.save();
		// TODO
		// Player player = Bukkit.getPlayer(resident.getUid());
		/* if (player != null && CivSettings.hasITag) { Bukkit.getScheduler().runTask(CivCraft.getPlugin(), () ->
		 * iTag.getInstance().refreshPlayer(player, new HashSet<>(Bukkit.getOnlinePlayers()))); } */
	}

	public double collectPlotTax() {
		double total = 0;
		for (Resident resident : this.residents.values()) {
			if (!resident.hasTown()) {
				CivLog.warning("Resident in town list but doesnt have a town! Resident:" + resident.getName() + " town:" + this.getName());
				continue;
			}

			if (resident.isTaxExempt()) {
				continue;
			}
			double tax = resident.getPropertyTaxOwed();
			boolean wasInDebt = resident.getTreasury().inDebt();

			total += resident.getTreasury().payToCreditor(this.getTreasury(), tax);

			if (resident.getTreasury().inDebt() && !wasInDebt) {
				resident.onEnterDebt();
			}
		}

		return total;
	}

	public double collectFlatTax() {
		double total = 0;
		for (Resident resident : this.residents.values()) {
			if (!resident.hasTown()) {
				CivLog.warning("Resident in town list but doesnt have a town! Resident:" + resident.getName() + " town:" + this.getName());
				continue;
			}

			if (resident.isTaxExempt()) {
				continue;
			}
			boolean wasInDebt = resident.getTreasury().inDebt();

			total += resident.getTreasury().payToCreditor(this.getTreasury(), this.getFlatTax());

			if (resident.getTreasury().inDebt() && !wasInDebt) {
				resident.onEnterDebt();
			}
		}

		return total;
	}

	public Collection<TownChunk> getTownChunks() {
		return this.townChunks.values();
	}

	public boolean isInGroup(String name, Resident resident) {
		PermissionGroup grp = this.getGroupByName(name);
		if (grp != null) {
			if (grp.hasMember(resident)) {
				return true;
			}
		}
		return false;
	}

	public Townhall getTownHall() {
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equals("s_capitol") || struct.getConfigId().equals("s_townhall")) {
				return (Townhall) struct;
			}
		}
		return null;
	}

	public double payUpkeep() throws InvalidConfiguration {
		double upkeep = 0;
		if (this.getCiv().isAdminCiv()) {
			return 0;
		}
		upkeep += this.getBaseUpkeep();
		// upkeep += this.getSpreadUpkeep();
		upkeep += this.getStructureUpkeep();
		upkeep += this.getOutpostUpkeep();
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

		for (Structure struct : getStructures()) {
			upkeep += struct.getUpkeepCost();
		}
		return upkeep;
	}

	public void removeResident(Resident resident) {
		this.residents.remove(resident.getName().toLowerCase());

		/* Remove resident from any groups. */
		for (PermissionGroup group : groups.values()) {
			if (group.hasMember(resident)) {
				group.removeMember(resident);
				group.save();
			}
		}

		kickResident(resident);
	}

	public boolean canClaim() {

		if (getMaxPlots() <= townChunks.size()) {
			return false;
		}

		return true;
	}

	public int getMaxPlots() {
		ConfigTownLevel lvl = CivSettings.townLevels.get(this.level);
		return lvl.plots;
	}

	public boolean hasUpgrade(String require_upgrade) {
		if (require_upgrade == null || require_upgrade.equals("")) return true;

		return upgrades.containsKey(require_upgrade);
	}

	public boolean hasTechnology(String require_tech) {
		return this.getCiv().hasTechnology(require_tech);
	}

	public String getDynmapDescription() {
		String out = "";
		try {
			out += "<h3><b>" + this.getName() + "</b> (<i>" + this.getCiv().getName() + "</i>)</h3>";
			out += "<b>" + CivSettings.localize.localizedString("Mayors") + " " + this.getMayorGroup().getMembersString() + "</b>";
		} catch (Exception e) {
			CivLog.error("Town: " + this.getName());
			e.printStackTrace();
		}

		return out;
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

	public void removeWonder(Buildable buildable) {
		if (!buildable.isComplete()) {
			this.removeBuildTask(buildable);
		}

		if (currentWonderInProgress == buildable) {
			currentWonderInProgress = null;
		}

		this.wonders.remove(buildable.getCorner());
	}

	public void addWonder(Buildable buildable) {
		if (buildable instanceof Wonder) {
			this.wonders.put(buildable.getCorner(), (Wonder) buildable);
		}
	}

	public int getStructureTypeCount(String id) {
		int count = 0;
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(id)) {
				count++;
			}
		}
		for (Wonder wonder : this.wonders.values()) {
			if (wonder.getConfigId().equalsIgnoreCase(id)) {
				count++;
			}
		}
		return count;
	}

	public void giveExtraHammers(double extra) {
		if (build_tasks.size() == 0) {
			// Nothing is building, store the extra hammers for when a structure starts
			// building.
			extraHammers = extra;
		} else {
			// Currently building structures ... divide them evenly between
			double hammers_per_task = extra / build_tasks.size();
			double leftovers = 0.0;

			for (BuildAsyncTask task : build_tasks) {
				leftovers += task.setExtraHammers(hammers_per_task);
			}

			extraHammers = leftovers;
		}
		this.save();
	}

	public void buildWonder(Player player, Buildable buildable) throws CivException {
		Location center = buildable.getCorner().getLocation();
		if (this.wonders.size() >= 2) {
			throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorLimit2"));
		}

		if (!center.getWorld().getName().equals("world")) {
			throw new CivException(CivSettings.localize.localizedString("town_buildwonder_NotOverworld"));
		}

		Wonder wonder = (Wonder) buildable;// Wonder.newWonder(center, id, this);

		if (!this.hasUpgrade(wonder.getRequiredUpgrade())) {
			throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingUpgrade") + " §6" + CivSettings.getUpgradeById(wonder.getRequiredUpgrade()).name);
		}

		if (!this.hasTechnology(wonder.getRequiredTechnology())) {
			throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingTech") + " §6" + CivSettings.getTechById(wonder.getRequiredTechnology()).name);
		}

		if (!wonder.isAvailable()) {
			throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorNotAvailable"));
		}

		if (!Wonder.isWonderAvailable(wonder.getConfigId())) {
			throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorBuiltElsewhere"));
		}

		if (CivGlobal.isCasualMode()) {
			/* Check for a wonder already in this civ. */
			for (Town town : this.getCiv().getTowns()) {
				for (Wonder w : town.getWonders()) {
					if (w.getConfigId().equals(wonder.getConfigId())) {
						throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorLimit1Casual"));
					}
				}
			}
		}

		double cost = wonder.getCost();
		if (!this.getTreasury().hasEnough(cost)) {
			throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorTooPoor", wonder.getDisplayName(), cost, CivSettings.CURRENCY_NAME));
		}

		Buildable inProgress = getCurrentStructureInProgress();
		if (inProgress != null) {
			throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + " " + CivSettings.localize.localizedString("town_buildwonder_errorOneAtATime"));
		} else {
			inProgress = getCurrentWonderInProgress();
			if (inProgress != null) {
				throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + " " + CivSettings.localize.localizedString("town_buildwonder_errorOneWonderAtaTime"));
			}
		}

		try {
			wonder.build(player);
			if (this.getExtraHammers() > 0) {
				this.giveExtraHammers(this.getExtraHammers());
			}
		} catch (Exception e) {
			if (CivGlobal.isHaveTestFlag("debug")) {
				e.printStackTrace();
			}
			throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorGeneric", e.getMessage()));
		}

		wonders.put(wonder.getCorner(), wonder);

		this.getTreasury().withdraw(cost);
		CivMessage.sendTown(this, CivColor.Yellow + CivSettings.localize.localizedString("var_town_buildwonder_success", wonder.getDisplayName()));
		this.save();
	}

	public void checkIsTownCanBuildStructure(Buildable buildable) throws CivException {
		if (!this.hasUpgrade(buildable.getRequiredUpgrade())) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingUpgrade") + " §6" + CivSettings.getUpgradeById(buildable.getRequiredUpgrade()).name);
		if (!this.hasTechnology(buildable.getRequiredTechnology())) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingTech") + " §6" + CivSettings.getTechById(buildable.getRequiredTechnology()).name);
		if (!buildable.isAvailable()) throw new CivException(CivSettings.localize.localizedString("town_structure_errorNotAvaliable"));
		if (buildable.getLimit() != 0)
			if (getStructureTypeCount(buildable.getConfigId()) >= buildable.getLimit()) throw new CivException(CivSettings.localize.localizedString("var_town_structure_errorLimitMet", buildable.getLimit(), buildable.getDisplayName()));

		double cost = buildable.getCost();
		if (!this.getTreasury().hasEnough(cost)) throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorTooPoor", buildable.getDisplayName(), cost, CivSettings.CURRENCY_NAME));

		Buildable inProgress = getCurrentStructureInProgress();
		if (inProgress != null)
			throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + ". " + CivSettings.localize.localizedString("town_buildwonder_errorOneAtATime"));

		ArrayList<ChunkCoord> chunks = BuildableStatic.getChunkCoords(buildable);
		if (getTownChunks().size() + chunks.size() > getMaxPlots())
			throw new CivException("Для постройки здания требуеться заприватить " + chunks.size() + " плотов. В вашем городе занято " + this.getTownChunks().size() + " из " + this.getMaxPlots()
					+ ". Освободите плоты командой /plot unclaim, или улучшите город командой /t upgrade buy");

		for (ChunkCoord cc : chunks) {
			TownChunk tc = CivGlobal.getTownChunk(cc);
			if (tc != null && tc.getTown() != this) throw new CivException("Один из чанков, которые займёт зданием, пренадлежит городу " + tc.getTown());
		}
	}

	public void checkIsTownCanBuildWonder(Buildable buildable) throws CivException {

	}

	public void rebuildStructure(Player player, Structure struct) throws CivException {
		if (struct.getReplaceStructure() == null) {
			buildStructure(player, struct);
			return;
		}

		Structure replaceStruct = this.getStructureByType(struct.getInfo().replace_structure);
		if (replaceStruct == null) throw new CivException("Заменяемое здание " + struct.getReplaceStructure() + " не найдено");

		try {
			CivMessage.sendTown(replaceStruct.getTown(), CivColor.Yellow + "В городе начался снос постройки " + replaceStruct.getDisplayName());
			TaskMaster.asyncTask(new Runnable() {
				@Override
				public void run() {
					try {
						String templatePath = Template.getUndoFilePath(replaceStruct.getCorner().toString());
						Template tpl = new Template(templatePath);
						BuildTemplateTask btt = new BuildTemplateTask(tpl, replaceStruct.getCorner());
						TaskMaster.asyncTask(btt, 0);
						while (!BuildTemplateTask.isFinished(btt)) {
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						Template.deleteFilePath(templatePath);
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} catch (IOException | CivException e) {
						e.printStackTrace();
					}

					try {
						replaceStruct.deleteSkipUndo();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}

					try {
						replaceStruct.getTown().startBuildStructure(player, struct);
					} catch (CivException e) {
						e.printStackTrace();
					}
				}
			}, 10);
		} catch (Exception e) {
			e.printStackTrace();
			CivMessage.sendError(player, e.getMessage());
		}
	}

	public void buildStructure(Player player, Structure struct) throws CivException {
		if (struct.getReplaceStructure() != null) {
			rebuildStructure(player, struct);
			return;
		}
		checkIsTownCanBuildStructure(struct);
		struct.checkBlockPermissionsAndRestrictions(player);
		for (ChunkCoord cc : BuildableStatic.getChunkCoords(struct)) {
			TownChunk tc = CivGlobal.getTownChunk(cc);
			if (tc == null) {
				// XXX These will be added to the array list of objects to save in town.buildStructure();
				TownChunk.autoClaim(this, cc).save();
			}
		}
		this.startBuildStructure(player, struct);
	}

	public void startBuildStructure(Player player, Structure struct) throws CivException {
		try {
			struct.build(player);
		} catch (Exception e) {
			e.printStackTrace();
			CivMessage.sendError(player, e.getMessage());
		}
		struct.save();

		// Go through and add any town chunks that were claimed to this list of saved objects.
		try {
			if (this.getExtraHammers() > 0) this.giveExtraHammers(this.getExtraHammers());

			this.getTreasury().withdraw(struct.getCost());
			CivMessage.sendTown(this, CivColor.Yellow + CivSettings.localize.localizedString("var_town_buildStructure_success", struct.getDisplayName()));

			if (struct instanceof TradeOutpost) {
				TradeOutpost outpost = (TradeOutpost) struct;
				if (outpost.getGood() != null) outpost.getGood().save();
			}
			this.save();
		} catch (Exception e) {
			e.printStackTrace();
			CivMessage.sendError(player, e.getMessage());
			throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
		}
	}

	public boolean isStructureAddable(Structure struct) {
		int count = this.getStructureTypeCount(struct.getConfigId());

		if (struct.isTileImprovement()) {
			ConfigTownLevel level = CivSettings.townLevels.get(this.getLevel());
			Integer maxTileImprovements = level.tile_improvements;
			if (this.getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
				maxTileImprovements *= 2;
			}
			if (this.getTileImprovementCount() > maxTileImprovements) {
				return false;
			}
		} else
			if ((struct.getLimit() != 0) && (count > struct.getLimit())) {
				return false;
			}

		return true;
	}

	public void addStructure(Structure struct) {
		this.structures.put(struct.getCorner(), struct);

		if (!isStructureAddable(struct)) {
			this.disabledBuildables.put(struct.getCorner(), struct);
			struct.setEnabled(false);
		} else {
			this.disabledBuildables.remove(struct.getCorner());
			struct.setEnabled(true);
		}

	}

	public Wonder getWonderByType(final String id) {
		for (final Wonder wonder : this.wonders.values()) {
			if (wonder.getConfigId().equalsIgnoreCase(id)) {
				return wonder;
			}
		}
		return null;
	}

	public Structure getStructureByType(String id) {
		for (Structure struct : this.structures.values()) {
			if (id.equalsIgnoreCase(struct.getConfigId())) {
				return struct;
			}
		}
		return null;
	}

	public List<Structure> getStructuresByType(String id) {
		List<Structure> res = new LinkedList<>();
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(id)) {
				res.add(struct);
			}
		}
		return res;
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

	public Collection<Structure> getStructures() {
		return this.structures.values();
	}

	public void processUndoConstruct() throws CivException {
		if (this.lastBuildableBuilt == null) {
			throw new CivException(CivSettings.localize.localizedString("town_undo_cannotFind"));
		}

		if ((this.lastBuildableBuilt instanceof Wall) || (this.lastBuildableBuilt instanceof Road)) {
			this.lastBuildableBuilt.processUndo();
			this.structures.remove(this.lastBuildableBuilt.getCorner());
			this.removeBuildTask(lastBuildableBuilt);
			this.lastBuildableBuilt = null;
		}
		throw new CivException(CivSettings.localize.localizedString("town_undo_notRoadOrWall"));
	}

	private void removeBuildTask(Buildable lastBuildableBuilt) {
		for (BuildAsyncTask task : this.build_tasks) {
			if (task.buildable == lastBuildableBuilt) {
				this.build_tasks.remove(task);
				task.abort();
				return;
			}
		}
	}

	public Structure getStructure(BlockCoord coord) {
		return structures.get(coord);
	}

	public void demolish(Structure struct, boolean isAdmin) throws CivException {

		if (!struct.allowDemolish() && !isAdmin) {
			throw new CivException(CivSettings.localize.localizedString("town_demolish_Cannot"));
		}
		if ((struct instanceof TradeOutpost) && !CivGlobal.allowDemolishOutPost()) {
			throw new CivException(CivSettings.localize.localizedString("town_demolish_CannotNotNow"));
		}

		struct.onDemolish();
		struct.delete();
	}

	public boolean hasStructure(String require_structure) {
		if (require_structure == null || require_structure.equals("")) {
			return true;
		}

		Structure struct = this.findStructureByConfigId(require_structure);
		if (struct != null && struct.isActive()) {
			return true;
		}

		return false;
	}

	public boolean hasWonder(final String require_wonder) {
		if (require_wonder == null || require_wonder.equals("")) {
			return true;
		}

		Wonder wonder = this.findWonderByConfigId(require_wonder);
		return wonder != null && wonder.isActive();
	}

	public Wonder findWonderByConfigId(final String require_wonder) {
		for (Wonder wonder : this.wonders.values()) {
			if (wonder.getConfigId().equals(require_wonder)) {
				return wonder;
			}
		}
		return null;
	}

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
		for (Structure struct : this.structures.values()) {
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
			if (town.hasWonder("w_burj")) {
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

	public double getSpreadUpkeep() throws InvalidConfiguration {
		double total = 0.0;
		double grace_distance = CivSettings.getDoubleTown("town.upkeep_town_block_grace_distance");
		double base = CivSettings.getDoubleTown("town.upkeep_town_block_base");
		double falloff = CivSettings.getDoubleTown("town.upkeep_town_block_falloff");

		Structure townHall = this.getTownHall();
		if (townHall == null) {
			CivLog.error("No town hall for " + getName() + " while getting spread upkeep.");
			return 0.0;
		}

		ChunkCoord townHallChunk = new ChunkCoord(townHall.getCorner().getLocation());

		for (TownChunk tc : this.getTownChunks()) {
			if (tc.isOutpost()) {
				continue;
			}

			if (tc.getChunkCoord().equals(townHallChunk)) continue;

			double distance = tc.getChunkCoord().distance(townHallChunk);
			if (distance > grace_distance) {
				distance -= grace_distance;
				double upkeep = base * Math.pow(distance, falloff);

				total += upkeep;
			}

		}

		return Math.floor(total);
	}

	public double getTotalUpkeep() throws InvalidConfiguration {
		return this.getBaseUpkeep() + this.getStructureUpkeep() + this.getSpreadUpkeep() + this.getOutpostUpkeep();
	}

	public double getTradeRate() {
		double rate = getGovernment().trade_rate;

		/* Grab changes from any rate components. */
		double fromStructures = 0.0;
		for (Structure struct : this.structures.values()) {
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

	public int getTileImprovementCount() {
		int count = 0;
		for (Structure struct : getStructures()) {
			if (struct.isTileImprovement()) {
				count++;
			}
		}
		return count;
	}

	public void removeTownChunk(TownChunk tc) {
		if (tc.isOutpost()) {
			this.outposts.remove(tc.getChunkCoord());
		} else {
			this.townChunks.remove(tc.getChunkCoord());
		}
	}

	public Double getHammersFromCulture() {
		double hammers = 0;
		for (CultureChunk cc : this.cultureChunks.values()) {
			hammers += cc.getHammers();
		}
		return hammers;
	}

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

	public void removeUpgrade(ConfigTownUpgrade upgrade) {
		this.upgrades.remove(upgrade.id);
	}

	public Structure getNearestStrucutre(Location location) {
		Structure nearest = null;
		double lowest_distance = Double.MAX_VALUE;

		for (Structure struct : getStructures()) {
			double distance = struct.getCenterLocation().distanceSquared(location);
			if (distance < lowest_distance) {
				lowest_distance = distance;
				nearest = struct;
			}
		}

		return nearest;
	}

	public Buildable getNearestStrucutreOrWonderInprogress(Location location) {
		Buildable nearest = null;
		double lowest_distanceSqr = Double.MAX_VALUE;

		for (Structure struct : getStructures()) {
			double distanceSqr = struct.getCenterLocation().distanceSquared(location);
			if (distanceSqr < lowest_distanceSqr) {
				lowest_distanceSqr = distanceSqr;
				nearest = struct;
			}
		}

		for (Wonder wonder : getWonders()) {
			if (wonder.isComplete()) continue;

			double distanceSqr = wonder.getCenterLocation().distanceSquared(location);
			if (distanceSqr < lowest_distanceSqr) {
				lowest_distanceSqr = distanceSqr;
				nearest = wonder;
			}
		}

		return nearest;
	}

	public void removeStructure(Structure structure) {
		if (!structure.isComplete()) this.removeBuildTask(structure);
		if (currentStructureInProgress == structure) currentStructureInProgress = null;
		this.structures.remove(structure.getCorner());
		this.invalidStructures.remove(structure);
		this.disabledBuildables.remove(structure.getCorner());
	}

	public void repairStructure(Structure struct) throws CivException {
		struct.repairStructure();
	}

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

	public Civilization getDepositCiv() {
		// Get the civilization we are going to deposit taxes to.
		return this.getCiv();
	}

	public Collection<Wonder> getWonders() {
		return this.wonders.values();
	}

	public void onGoodiePlaceIntoFrame(ItemFrameStorage framestore, BonusGoodie goodie) {
		Townhall townhall = this.getTownHall();
		if (townhall == null) return;
		for (ItemFrameStorage fs : townhall.getGoodieFrames()) {
			if (fs == framestore) {
				this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
				for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
					String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;
					if (buffManager.hasBuffKey(key)) continue;
					try {
						buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
					} catch (CivException e) {
						e.printStackTrace();
					}
				}
			}
		}

		for (Structure struct : this.structures.values()) {
			struct.onGoodieFrame();
		}

		for (Wonder wonder : this.wonders.values()) {
			wonder.onGoodieFrame();
		}
	}

	public void loadGoodiePlaceIntoFrame(Townhall townhall, BonusGoodie goodie) {
		this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
		for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
			String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;

			if (buffManager.hasBuffKey(key)) {
				continue;
			}

			try {
				buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
	}

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

	public void onGoodieRemoveFromFrame(ItemFrameStorage framestore, BonusGoodie goodie) {
		Townhall townhall = this.getTownHall();
		if (townhall == null) return;
		for (ItemFrameStorage fs : townhall.getGoodieFrames()) {
			if (fs == framestore) removeGoodie(goodie);
		}
		for (Structure struct : this.structures.values()) {
			struct.onGoodieFrame();
		}
		for (Wonder wonder : this.wonders.values()) {
			wonder.onGoodieFrame();
		}
	}

	/** проверка, можно ли строить юнита в этом городе */
	public ArrayList<ConfigUnit> getAvailableUnits() {
		ArrayList<ConfigUnit> unitList = new ArrayList<ConfigUnit>();

		for (ConfigUnit unit : UnitStatic.configUnits.values()) {
			if (unit.isAvailable(this)) unitList.add(unit);
		}
		return unitList;
	}

	public void onTechUpdate() {
		try {
			for (Structure struct : this.structures.values()) {
				if (struct.isActive()) {
					struct.onTechUpdate();
				}
			}

			for (Wonder wonder : this.wonders.values()) {
				if (wonder.isActive()) {
					wonder.onTechUpdate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// continue in case some structure/wonder had an error.
		}
	}

	public Buildable getNearestBuildable(Location location) {
		Buildable nearest = null;
		double lowest_distanceSqr = Double.MAX_VALUE;

		for (Structure struct : getStructures()) {
			double distanceSqr = struct.getCenterLocation().distanceSquared(location);
			if (distanceSqr < lowest_distanceSqr) {
				lowest_distanceSqr = distanceSqr;
				nearest = struct;
			}
		}

		for (Wonder wonder : getWonders()) {
			double distanceSqr = wonder.getCenterLocation().distanceSquared(location);
			if (distanceSqr < lowest_distanceSqr) {
				lowest_distanceSqr = distanceSqr;
				nearest = wonder;
			}
		}

		return nearest;
	}

	public boolean isCapitol() {
		if (this.getCiv().getCapitolName().equals(this.getName())) {
			return true;
		}
		return false;
	}

	public boolean isForSale() {
		if (this.getCiv().isTownsForSale()) {
			return true;
		}

		if (!this.inDebt()) {
			return false;
		}

		if (daysInDebt >= CivSettings.TOWN_DEBT_GRACE_DAYS) {
			return true;
		}

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

	public int getScore() {
		int points = 0;

		// Count Structures
		for (Structure struct : this.getStructures()) {
			points += struct.getPoints();
		}

		// Count Wonders
		for (Wonder wonder : this.getWonders()) {
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

	public void addOutpostChunk(TownChunk tc) throws AlreadyRegisteredException {
		if (outposts.containsKey(tc.getChunkCoord())) {
			throw new AlreadyRegisteredException(CivSettings.localize.localizedString("var_town_outpost_alreadyRegistered", tc.getChunkCoord(), this.getName()));
		}
		outposts.put(tc.getChunkCoord(), tc);
	}

	public Collection<TownChunk> getOutpostChunks() {
		return outposts.values();
	}

	public double getOutpostUpkeep() {
		// double outpost_upkeep;
		// try {
		// outpost_upkeep = CivSettings.getDouble(CivSettings.townConfig, "town.outpost_upkeep");
		// } catch (InvalidConfiguration e) {
		// e.printStackTrace();
		// return 0.0;
		// }
		// return outpost_upkeep*outposts.size();
		return 0;
	}

	public boolean isOutlaw(Resident res) {
		return this.outlaws.contains(res.getUid().toString());
	}

	public boolean isOutlaw(String name) {
		Resident res = CivGlobal.getResident(name);
		return this.outlaws.contains(res.getUid().toString());
	}

	public void addOutlaw(String name) {
		Resident res = CivGlobal.getResident(name);
		this.outlaws.add(res.getUid().toString());
		TaskMaster.syncTask(new SyncUpdateTags(res.getUid().toString(), this.residents.values()));
	}

	public void removeOutlaw(String name) {
		Resident res = CivGlobal.getResident(name);
		this.outlaws.remove(res.getUid().toString());
		TaskMaster.syncTask(new SyncUpdateTags(res.getUid().toString(), this.residents.values()));
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

	public void validateResidentSelect(Resident resident) throws CivException {
		if (this.getMayorGroup() == null || this.getAssistantGroup() == null || this.getDefaultGroup() == null || this.getCiv().getLeaderGroup() == null || this.getAssistantGroup() == null) {
			throw new CivException(CivSettings.localize.localizedString("town_validateSelect_error1"));
		}

		if (!this.getMayorGroup().hasMember(resident) && !this.getAssistantGroup().hasMember(resident) && !this.getDefaultGroup().hasMember(resident) && !this.getCiv().getLeaderGroup().hasMember(resident)
				&& !this.getCiv().getAdviserGroup().hasMember(resident)) {
			throw new CivException(CivSettings.localize.localizedString("town_validateSelect_error2"));
		}
	}

	public boolean touchesCapitolCulture(HashSet<Town> closedSet) {
		if (this.isCapitol()) {
			return true;
		}

		closedSet.add(this);

		for (Town t : this.townTouchList) {
			if (closedSet.contains(t)) {
				continue;
			}

			if (t.getCiv() != this.getCiv()) {
				continue;
			}

			boolean touches = t.touchesCapitolCulture(closedSet);
			if (touches) {
				return true;
			}
		}

		return false;
	}

	public void rebuildNameTag() {
		TagManager.editNameTag(this);
	}

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

	public Civilization getMotherCiv() {
		return motherCiv;
	}

	public void setMotherCiv(Civilization motherCiv) {
		this.motherCiv = motherCiv;
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

	public void capitulate() {
		if (this.getMotherCiv() == null) {
			return;
		}

		if (this.getMotherCiv().getCapitolName().equals(this.getName())) {
			this.getMotherCiv().capitulate();
			return;
		}

		/* Town is capitulating, no longer need a mother civ. */
		this.setMotherCiv(null);
		this.save();
		CivMessage.global(CivSettings.localize.localizedString("var_town_capitulate1", this.getName(), this.getCiv().getName()));
	}

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

		// double education = 0.0;
		// for (Structure struct : this.structures.values()) {
		// for (Component comp : struct.attachedComponents) {
		// if (comp instanceof AttributeBase) {
		// AttributeBase as = (AttributeBase)comp;
		// if (as.getString("attribute").equalsIgnoreCase("BEAKERBOOST")) {
		// double boostPerRes = as.getGenerated();
		// int maxBoost = 0;
		//
		// if (struct instanceof University) {
		// maxBoost = 5;
		// }
		// else if (struct instanceof School || struct instanceof ResearchLab) {
		// maxBoost = 10;
		// }
		// int resCount = Math.min(this.getResidentCount(),maxBoost);
		// education += (boostPerRes * resCount);
		// }
		// }
		// }
		// }
		//
		// rate += education;
		// rates.put("Education", education);

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
		double fromStructures = 0;
		for (Structure struct : this.structures.values()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("BEAKERS")) {
						fromStructures += as.getGenerated();
					}
				}
			}
		}
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

		double education = 0.0;
		for (Structure struct : this.structures.values()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("BEAKERBOOST")) {
						double boostPerRes = as.getGenerated();
						int maxBoost = 5;
						int resCount = Math.min(this.getResidentCount(), maxBoost);
						education += (boostPerRes * resCount);
					}
				}
			}
		}
		double educationBeakers = (beakers * education);
		beakers += educationBeakers;
		sources.put("Education", educationBeakers);

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
		for (Structure struct : this.structures.values()) {
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
		HashSet<Resident> UnResidents = new HashSet<Resident>();
		HashSet<Resident> NonResidents = new HashSet<Resident>();
		for (PermissionGroup group : this.getGroups()) {
			for (Resident res : group.getMemberList()) {
				if (res.getCiv() != null) {
					if (res.getCiv() != this.getCiv()) {
						NonResidents.add(res);
					}
				} else {
					UnResidents.add(res);
				}
			}
		}

		double happy_resident = per_resident * this.getResidents().size();
		double happy_Nonresident = (per_resident * 0.25) * NonResidents.size();
		double happy_Unresident = per_resident * UnResidents.size();
		sources.put("Residents", (happy_resident + happy_Nonresident + happy_Unresident));
		total += happy_resident + happy_Nonresident + happy_Unresident;

		/* Try to reduce war unhappiness via the component. */
		if (sources.containsKey("War")) {
			for (Structure struct : this.structures.values()) {
				for (Component comp : struct.attachedComponents) {
					if (!comp.isActive()) {
						continue;
					}

					if (comp instanceof AttributeWarUnhappiness) {
						AttributeWarUnhappiness warunhappyComp = (AttributeWarUnhappiness) comp;
						double value = sources.get("War"); // Negative if a reduction
						value += warunhappyComp.value;

						if (value < 0) {
							value = 0;
						}

						sources.put("War", value);
					}
				}
			}
		}

		/* Get distance unhappiness from capitol. */
		if (this.getMotherCiv() == null && !this.isCapitol()) {
			double distance_unhappy = this.getCiv().getDistanceHappiness(this);
			total += distance_unhappy;
			sources.put("Distance To Capitol", distance_unhappy);
		}

		/* Add in base unhappiness if it exists. */
		if (this.baseUnhappy != 0) {
			sources.put("Base Unhappiness", this.baseUnhappy);
			total += this.baseUnhappy;
		}

		/* Grab unhappiness generated from structures with components. */
		double structures = 0;
		for (Structure struct : this.structures.values()) {
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

	public Buildable getCurrentStructureInProgress() {
		return currentStructureInProgress;
	}

	public void setCurrentStructureInProgress(Buildable currentStructureInProgress) {
		this.currentStructureInProgress = currentStructureInProgress;
	}

	public Buildable getCurrentWonderInProgress() {
		return currentWonderInProgress;
	}

	public void setCurrentWonderInProgress(Buildable currentWonderInProgress) {
		this.currentWonderInProgress = currentWonderInProgress;
	}

	public void addFakeResident(Resident fake) {
		this.fakeResidents.put(fake.getName(), fake);
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

	public Set<Perk> getTemplatePerks(Buildable buildable, Resident resident, ConfigBuildableInfo info) {
		Set<Perk> perks = CustomTemplate.getTemplatePerksForBuildable(this, info.template_name);
		perks.addAll(resident.getUnboundTemplatePerks(perks, info));
		return perks;
	}

	public RandomEvent getActiveEvent() {
		return activeEvent;
	}

	public void setActiveEvent(RandomEvent activeEvent) {
		this.activeEvent = activeEvent;
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

	public void markLastBuildableRefeshAsNow() {
		this.lastBuildableRefresh = new Date();
	}

	public void refreshNearestBuildable(Resident resident) throws CivException {
		if (!this.getMayorGroup().hasMember(resident)) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorNotMayor"));

		if (this.lastBuildableRefresh != null) {
			Date now = new Date();
			int buildable_refresh_cooldown;
			try {
				buildable_refresh_cooldown = CivSettings.getInteger(CivSettings.townConfig, "town.buildable_refresh_cooldown");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
			}

			if (now.getTime() < this.lastBuildableRefresh.getTime() + (buildable_refresh_cooldown * 60 * 1000)) {
				throw new CivException(CivSettings.localize.localizedString("var_town_refresh_wait1", buildable_refresh_cooldown));
			}
		}

		Player player = CivGlobal.getPlayer(resident);
		Buildable buildable = CivGlobal.getNearestBuildable(player.getLocation());
		if (buildable == null) throw new CivException(CivSettings.localize.localizedString("town_refresh_couldNotFind"));
		if (!buildable.isActive()) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorInProfress"));
		if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWar"));
		if (buildable.getTown() != this) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWrongTown"));
		resident.setInteractiveMode(new InteractiveBuildableRefresh(buildable, resident.getName()));
	}

	public boolean areMayorsInactive() {

		int mayor_inactive_days;
		try {
			mayor_inactive_days = CivSettings.getInteger(CivSettings.townConfig, "town.mayor_inactive_days");
			for (Resident resident : this.getMayorGroup().getMemberList()) {
				if (!resident.isInactiveForDays(mayor_inactive_days)) return false;
			}

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void rename(String name) throws CivException, InvalidNameException {

		Town other = CivGlobal.getTown(name);
		if (other != null) throw new CivException(CivSettings.localize.localizedString("town_rename_errorExists"));

		if (this.isCapitol()) {
			this.getCiv().setCapitolName(name);
			this.getCiv().save();
		}

		String oldName = this.getName();
		CivGlobal.removeTown(this);

		this.setName(name);
		this.save();

		CivGlobal.addTown(this);
		CivMessage.global(CivSettings.localize.localizedString("var_town_rename_success1", oldName, this.getName()));
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
		Townhall townhall = this.getTownHall();
		ChunkCoord coord;
		if (townhall == null) {
			/* if no town hall, pick a 'random' town chunk' */
			coord = this.getTownChunks().iterator().next().getChunkCoord();
		} else {
			/* Grab town chunk from town hall location. */
			coord = new ChunkCoord(townhall.getCenterLocation());
		}
		return coord;
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

	public void clearBonusGoods() {
		this.bonusGoodies.clear();
	}

	public void processStructureFlipping(HashMap<ChunkCoord, Structure> centerCoords) {

		for (CultureChunk cc : this.cultureChunks.values()) {
			Structure struct = centerCoords.get(cc.getChunkCoord());
			if (struct == null) continue;
			if (struct.getCiv() == cc.getCiv()) continue;

			/* There is a structure at this location that doesnt belong to us! Grab it! */
			struct.getTown().removeStructure(struct);
			this.addStructure(struct);
			struct.setSQLOwner(this);
			struct.save();
		}
	}

	public boolean hasDisabledStructures() {
		return this.disabledBuildables.size() > 0;
	}

	public Collection<Buildable> getDisabledBuildables() {
		return this.disabledBuildables.values();
	}

	public int getArtifactTypeCount(final String id) {
		return 0;
	}

	public boolean hasCapitol() {
		return this.hasStructure("s_capitol");
	}

	public Capitol getCapitol() {
		for (final Structure structure : this.structures.values()) {
			if (structure instanceof Capitol) return (Capitol) structure;
		}
		return null;
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
		for (final Structure structure : this.getStructures()) {
			structure.onBonusGoodieUpdate();
		}
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
		for (final Structure structure : this.getStructures()) {
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

	@Override
	public boolean equals(final Object another) {
		if (!(another instanceof Town)) {
			return false;
		}
		final Town anther = (Town) another;
		return anther.getName().equals(this.getName()) && anther.getId() == this.getId();
	}

	public int getXChance() {
		if (!this.hasTradeGood("good_titanium")) {
			return 0;
		}
		int x = 5;
		if (this.getTradeGoodCount("good_titanium") >= 2) {
			x = 10;
		}
		return x;
	}

	public int getReturnChance() {
		if (!this.hasTradeGood("good_titanium") && !this.hasTradeGood("good_poison_ivy")) {
			return 0;
		}
		int chance = 20;
		if (this.getTradeGoodCount("good_titanium") >= 2 || this.getTradeGoodCount("good_poison_ivy") >= 2) {
			chance = 30;
		}
		return chance;
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

	public boolean otherCivHasNotreDame() {
		boolean has = false;
		for (final Relation relation : this.getCiv().getDiplomacyManager().getRelations()) {
			if (relation.getStatus().equals(Relation.Status.WAR)) {
				if (relation.isDeleted()) {
					continue;
				}
				if (this.getCiv() != relation.getAggressor()) {
					continue;
				}
				for (final Town town : relation.getOtherCiv().getTowns()) {
					if (town.hasWonder("w_notre_dame")) {
						has = true;
						break;
					}
				}
			}
		}
		return has;
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
					if (town.hasWonder("w_notre_dame")) {
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
		for (final Structure structure : this.getStructures()) {
			for (final Component comp : structure.attachedComponents) {
				if (comp instanceof AttributeWarUnpkeep) {
					total -= ((AttributeWarUnpkeep) comp).value;
				}
			}
		}
		return (total < 1.0) ? 1.0 : total;
	}
}
