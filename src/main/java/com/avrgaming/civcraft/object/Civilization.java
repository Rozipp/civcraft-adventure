package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.avrgaming.civcraft.construct.Buildable;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigTech;
//import com.avrgaming.civcraft.construct.caves.Cave;
//import com.avrgaming.civcraft.construct.caves.CaveStatus;
//import com.avrgaming.civcraft.construct.caves.CaveStatus.StatusType;
import com.avrgaming.civcraft.construct.constructs.WarCamp;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.construct.RespawnLocationHolder;
import com.avrgaming.civcraft.construct.wonders.Neuschwanstein;
import com.avrgaming.civcraft.construct.wonders.StockExchange;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.TagManager;

@Getter
@Setter
public class Civilization extends SQLObject {

	private Map<String, ConfigTech> techs = new ConcurrentHashMap<>();

	private int color;
	private int daysInDebt = 0;
	private int currentEra = 0;
	private double incomeTaxRate;
	private double sciencePercentage;
	private ConfigTech researchTech = null;
	private double researchProgress = 0.0;
	private String tag;

	private EconObject treasury;
	private int capitolId;
	public CivGroupManager GM;

	private Set<Town> towns = new HashSet<>();
	private ConfigGovernment government;

	private double baseBeakers = 1.0;

	public static final int HEX_COLOR_MAX = 16777215;
	public static final int HEX_COLOR_TOLERANCE = 40;

	/* Store information to display about last upkeep paid. */
	public HashMap<String, Double> lastUpkeepPaidMap = new HashMap<>();

	/* Store information about last tick's taxes */
	public HashMap<String, Double> lastTaxesPaidMap = new HashMap<>();

	/* Used to prevent spam of tech % complete message. */
	private int lastTechPercentage = 0;

	private DiplomacyManager diplomacyManager = new DiplomacyManager(this);

	private boolean adminCiv = false;
	private boolean conquered = false;

	private Date conquer_date = null;
	private Date created_date = null;
	public boolean scoutDebug = false;
	public String scoutDebugPlayer = null;

	public String messageOfTheDay = "";

	private LinkedList<WarCamp> warCamps = new LinkedList<>();
	public long lastWarCampCreated = 0;

	public Object civGuiLeaders;

	private ConfigTech techQueue = null;
	int currentMission = 1;
	boolean missionActive = false;
	String missionProgress = "0:0";

	// private HashMap<Integer, CaveStatus> caveStatuses = new HashMap<Integer, CaveStatus>();
	// private Set<Cave> disputedCave = new HashSet<Cave>();
	// private Set<Cave> takedCave = new HashSet<Cave>();

	public Civilization() {
		this.GM = new CivGroupManager(this);
		this.government = CivSettings.governments.get("gov_tribalism");
		this.color = CivColor.pickCivColor();
		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(0, false);
		this.loadSettings();
	}

	public Civilization(ResultSet rs) throws SQLException, InvalidNameException {
		this.load(rs);
		loadSettings();
	}

	public void loadSettings() {
		try {
			this.baseBeakers = CivSettings.getDouble(CivSettings.civConfig, "civ.base_beaker_rate");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public static String TABLE_NAME = "CIVILIZATIONS";

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" //
					+ "`id` int(11) unsigned NOT NULL auto_increment," //
					+ "`name` VARCHAR(64) NOT NULL," //
					+ "`capitolId` int(11) DEFAULT 0,"//
					+ "`debt` float NOT NULL DEFAULT '0',"//
					+ "`coins` double DEFAULT 0,"//
					+ "`daysInDebt` int NOT NULL DEFAULT '0',"//
					+ "`techs` mediumtext DEFAULT NULL,"//
					+ "`techQueue` mediumtext DEFAULT NULL,"//
					+ "`motd` mediumtext DEFAULT NULL,"//
					+ "`researchTech` mediumtext DEFAULT NULL,"//
					+ "`researchProgress` float NOT NULL DEFAULT 0,"//
					+ "`researched` mediumtext DEFAULT NULL, "//
					+ "`government_id` mediumtext DEFAULT NULL,"//
					+ "`color` int(11) DEFAULT 0,"//
					+ "`income_tax_rate` float NOT NULL DEFAULT 0,"//
					+ "`science_percentage` float NOT NULL DEFAULT 0,"//
					+ "`leaderGroupName` mediumtext DEFAULT NULL,"//
					+ "`advisersGroupName` mediumtext DEFAULT NULL,"//
					+ "`lastUpkeepTick` mediumtext DEFAULT NULL,"//
					+ "`lastTaxesTick` mediumtext DEFAULT NULL,"//
					+ "`adminCiv` boolean DEFAULT false,"//
					+ "`conquered` boolean DEFAULT false,"//
					+ "`conquered_date` long,"//
					+ "`created_date` long,"//
					+ "`ownerGroupName` mediumtext DEFAULT NULL," // названниегруппы основателя цивилизации
					+ "`cave_statuses` mediumtext DEFAULT NULL,"//
					+ "`ownerName` mediumtext DEFAULT NULL," // ник основателя цивилизации
					+ "`tag` mediumtext," // tэг цивилизации
					+ "`currentMission` int(11) DEFAULT 0,"//
					+ "`missionActive` boolean DEFAULT false,"//
					+ "`missionProgress` mediumtext,"//
					+ "UNIQUE KEY (`name`), " + "PRIMARY KEY (`id`)" + ")";

			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
			SQL.makeCol("cave_statuses", "mediumtext", TABLE_NAME);
			// SQL.makeCol("conquered_date", "long", TABLE_NAME);
			// SQL.makeCol("created_date", "long", TABLE_NAME);
			// SQL.makeCol("motd", "mediumtext", TABLE_NAME);
		}
	}

	@Override
	public void delete() {
		this.capitolId = 0;
		try {
			/* First delete all of our groups. */
			this.GM.delete();

			/* Delete all of our towns. */
			Iterator<Town> it = getTowns().iterator();
			while (it.hasNext()) {
				Town t = it.next();
				t.delete();
			}

			/* Delete all relationships with other civs. */
			this.diplomacyManager.deleteAllRelations();

			SQL.deleteNamedObject(this, TABLE_NAME);
			CivGlobal.removeCiv(this);
			if (this.isConquered()) CivGlobal.removeConqueredCiv(this);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void createCiv(Player player, Town town, Buildable buildable) throws CivException {
		Resident resident = CivGlobal.getResident(player);
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(player.getInventory().getItemInMainHand());
		if (craftMat == null || !craftMat.hasComponent("FoundCivilization")) throw new CivException(CivSettings.localize.localizedString("civ_found_notItem"));
		try {
			try {
				this.saveNow();
			} catch (SQLException e) {
				CivLog.error("Caught exception:" + e.getMessage() + " error code:" + e.getErrorCode());
				if (e.getMessage().contains("Duplicate entry")) {
					SQL.deleteByName(getName(), TABLE_NAME);
					throw new CivException(CivSettings.localize.localizedString("civ_found_databaseException"));
				}
			}

			/* Save this civ in the db and hashtable. */
			try {
				town.setCiv(this);
				town.createTown(resident, buildable);
				this.setCapitolId(town.getId());
			} catch (CivException e) {
				e.printStackTrace();
				this.delete();
				throw e;
			}

			// Create permission groups for civs.
			this.GM.init();
			this.GM.addLeader(resident);

			CivGlobal.addCiv(this);
			CivMessage.globalTitle(CivSettings.localize.localizedString("var_civ_found_successTitle", this.getName()), CivSettings.localize.localizedString("var_civ_found_successSubTitle", town.getName(), player.getName()));
			this.saveNow();
			player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
			TagManager.editNameTag(player);
		} catch (SQLException e) {
			this.delete();
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}
	}

	public void checkCanCreatedCiv(Player player) throws CivException {
		ItemStack stack = player.getInventory().getItemInMainHand();
		/* Verify we have the correct item somewhere in our inventory. */
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat == null || !craftMat.hasComponent("FoundCivilization")) throw new CivException(CivSettings.localize.localizedString("civ_found_notItem"));
		// if (this.getCapitol() != null) throw new CivException(CivSettings.localize.localizedString("var_civ_found_townExists",
		// this.getCapitol().getName()));
		// if (CivGlobal.anybodyHasTag(tag)) throw new CivException(CivSettings.localize.localizedString("var_civ_found_tagExists", tag));
	}

	public void onCivtickUpdate() {
		processBeakers();
		for (Town town : getTowns()) {
			town.getCityhall().updateResearchSign();
		}
	}

	// -------------------- load save

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));

		this.capitolId = rs.getInt("capitolId");
		this.tag = rs.getString("tag");
		this.GM = new CivGroupManager(this, rs);

		this.daysInDebt = rs.getInt("daysInDebt");
		this.color = rs.getInt("color");
		this.setResearchTech(CivSettings.techs.get(rs.getString("researchTech")));
		this.setResearchProgress(rs.getDouble("researchProgress"));
		this.setGovernment(rs.getString("government_id"));
		this.loadKeyValueString(rs.getString("lastUpkeepTick"), this.lastUpkeepPaidMap);
		this.loadKeyValueString(rs.getString("lastTaxesTick"), this.lastTaxesPaidMap);
		this.setSciencePercentage(rs.getDouble("science_percentage"));
		this.setTechQueue(CivSettings.techs.get(rs.getString("techQueue")));
		double taxes = rs.getDouble("income_tax_rate");
		if (taxes > this.government.maximum_tax_rate) {
			taxes = this.government.maximum_tax_rate;
		}

		this.setIncomeTaxRate(taxes);
		this.loadResearchedTechs(rs.getString("researched"));
		this.adminCiv = rs.getBoolean("adminCiv");
		this.conquered = rs.getBoolean("conquered");
		Long ctime = rs.getLong("conquered_date");
		this.conquer_date = ctime == 0 ? null : new Date(ctime);

		String motd = rs.getString("motd");
		this.messageOfTheDay = (motd == null || motd.equals("")) ? null : motd;

		ctime = rs.getLong("created_date");
		this.created_date = new Date(ctime);

		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(rs.getDouble("coins"), false);
		this.getTreasury().setDebt(rs.getDouble("debt"));

		for (ConfigTech tech : this.getTechs()) {
			if (tech.era > this.getCurrentEra()) this.setCurrentEra(tech.era);
		}
		this.currentMission = rs.getInt("currentMission");
		this.missionActive = rs.getBoolean("missionActive");
		this.missionProgress = rs.getString("missionProgress");

		// this.loadCaveStatus(rs.getString("cave_statuses"));
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<>();
		hashmap.put("name", this.getName());
		hashmap.put("tag", this.tag);
		hashmap.put("capitolId", this.capitolId);
		hashmap.put("leaderGroupName", this.GM.leadersGroupName);
		hashmap.put("advisersGroupName", this.GM.advisersGroupName);
		hashmap.put("debt", this.getTreasury().getDebt());
		hashmap.put("coins", this.getTreasury().getBalance());
		hashmap.put("daysInDebt", this.daysInDebt);
		hashmap.put("income_tax_rate", this.getIncomeTaxRate());
		hashmap.put("science_percentage", this.getSciencePercentage());
		hashmap.put("color", this.getColor());
		hashmap.put("techQueue", (this.getTechQueue() != null) ? this.getTechQueue().id : null);
		hashmap.put("researchTech", (this.getResearchTech() != null) ? this.getResearchTech().id : null);
		hashmap.put("researchProgress", this.getResearchProgress());
		hashmap.put("government_id", this.getGovernment().id);
		hashmap.put("lastUpkeepTick", this.saveKeyValueString(this.lastUpkeepPaidMap));
		hashmap.put("lastTaxesTick", this.saveKeyValueString(this.lastTaxesPaidMap));
		hashmap.put("researched", this.saveResearchedTechs());
		hashmap.put("adminCiv", this.adminCiv);
		hashmap.put("conquered", this.conquered);

		hashmap.put("currentMission", this.currentMission);
		hashmap.put("missionActive", this.missionActive);
		hashmap.put("missionProgress", this.missionProgress);
		hashmap.put("conquered_date", (this.conquer_date != null) ? this.conquer_date.getTime() : null);
		hashmap.put("motd", this.messageOfTheDay);
		hashmap.put("created_date", (this.created_date != null) ? this.created_date.getTime() : null);

		// hashmap.put("cave_statuses", saveCaveStatus());

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	private void loadResearchedTechs(String techstring) {
		if (techstring == null || techstring.equals("")) return;
		for (String tech : techstring.split(",")) {
			ConfigTech t = CivSettings.techs.get(tech);
			if (t != null) {
				CivGlobal.researchedTechs.add(t.id.toLowerCase());
				this.techs.put(tech, t);
			}
		}
	}

	private Object saveResearchedTechs() {
		String out = "";
		for (ConfigTech tech : this.techs.values()) {
			out += tech.id + ",";
		}
		return out;
	}

	private void loadKeyValueString(String string, HashMap<String, Double> map) {
		for (String keyvalue : string.split(";")) {
			try {
				map.put(keyvalue.split(":")[0], Double.valueOf(keyvalue.split(":")[1]));
			} catch (ArrayIndexOutOfBoundsException e) {/* forget it then. */}
		}
	}

	private String saveKeyValueString(HashMap<String, Double> map) {
		String out = "";
		for (String key : map.keySet()) {
			double value = map.get(key);
			out += key + ":" + value + ";";
		}
		return out;
	}

	// ------------------ Technology and Era

	public void addTech(ConfigTech t) {
		if (t.era > this.getCurrentEra()) this.setCurrentEra(t.era);
		CivGlobal.researchedTechs.add(t.id);
		techs.put(t.id, t);
		for (Town town : this.getTowns()) {
			town.onTechUpdate();
		}
	}

	public void removeTech(String configId) {
		techs.remove(configId);
		for (Town town : this.getTowns()) {
			town.onTechUpdate();
		}
	}

	public boolean hasTechnologys(String require_tech) {
		if (require_tech != null) {
			for (String str : require_tech.split(",")) {
				CivLog.debug("hasTechnologys " + str);
				if (!hasTech(str)) return false;
			}
		}
		return true;
	}

	private boolean hasTech(String configId) {
		if (configId == null || configId.equals("")) return true;
		return techs.containsKey(configId);
	}

	public void setCurrentEra(int currentEra) {
		this.currentEra = currentEra;

		if (this.currentEra > CivGlobal.highestCivEra && !this.isAdminCiv()) {
			CivGlobal.setCurrentEra(this.currentEra, this);
		}
	}

	public void processTech(double beakers) {
		if (beakers == 0) return;
		setResearchProgress(getResearchProgress() + beakers);

		if (getResearchProgress() >= getResearchTech().getAdjustedBeakerCost(this)) {
			CivMessage.sendCiv(this, CivSettings.localize.localizedString("var_civ_research_Discovery", getResearchTech().name));
			CivMessage.sendCiv(this, getResearchTech().lore);
			CivMessage.sendCiv(this, getResearchTech().open_lore);
			if ("tech_enlightenment".equals(this.getResearchTech().id)) {
				CivMessage.global(CivSettings.localize.localizedString("engliment_sucusses", this.getName()));
			}
			this.addTech(this.getResearchTech());
			this.setResearchProgress(0);
			this.setResearchTech(null);

			this.save();
			TagManager.editNameTag(this);
			if (this.getTechQueue() != null) {
				final ConfigTech tech = this.getTechQueue();
				if (!tech.isAvailable(this)) {
					CivMessage.sendTechError(this, CivSettings.localize.localizedString("civ_research_queueErrorCodeOne", tech.name));
					return;
				}
				if (this.hasTech(tech.id)) {
					CivMessage.sendTechError(this, CivSettings.localize.localizedString("civ_research_queueErrorCodeTwo", tech.name));
					return;
				}
				if (!this.getTreasury().hasEnough(tech.getAdjustedTechCost(this))) {
					CivMessage.sendTechError(this, CivSettings.localize.localizedString("civ_research_queueErrorCodeThree", tech.name, tech.getAdjustedTechCost(this) - this.getTreasury().getBalance(),
							CivMessage.plurals((int) (tech.getAdjustedTechCost(this) - this.getTreasury().getBalance()), "монета", "монеты", "монет")));
					return;
				}
				if (this.getResearchTech() != null) {
					CivMessage.sendTechError(this, CivSettings.localize.localizedString("civ_research_queueErrorCodeFour", tech.name, this.getResearchTech().name));
					return;
				}
				this.setResearchTech(tech);
				this.setResearchProgress(0.0);
				this.setTechQueue(null);
				CivMessage.sendCiv(this, "§e" + CivSettings.localize.localizedString("civ_research_queuePrefix") + CivSettings.localize.localizedString("civ_research_queueSucussesStarted", tech.name));
				this.getTreasury().withdraw(tech.getAdjustedTechCost(this));
				this.save();
			}
			return;
		}

		int percentageComplete = (int) ((getResearchProgress() / this.getResearchTech().getAdjustedBeakerCost(this)) * 100);
		if ((percentageComplete % 10) == 0) {
			if (percentageComplete != lastTechPercentage) {
				CivMessage.sendCiv(this, CivSettings.localize.localizedString("var_civ_research_currentProgress", getResearchTech().name, percentageComplete));
				lastTechPercentage = percentageComplete;
				if ("tech_enlightenment".equals(this.getResearchTech().id)) {
					CivMessage.global(CivSettings.localize.localizedString("var_engliment_research", this.getName(), percentageComplete));
				}
			}

		}
		this.save();
	}

	public void startTechnologyResearch(ConfigTech tech) throws CivException {
		if (this.getResearchTech() != null) throw new CivException(CivSettings.localize.localizedString("var_civ_research_switchAlert1", this.getResearchTech().name));
		double cost = tech.getAdjustedTechCost(this);

		if (!this.getTreasury().hasEnough(cost)) throw new CivException(CivSettings.localize.localizedString("var_civ_research_notEnoughMoney", cost, CivSettings.CURRENCY_NAME));
		if (this.hasTech(tech.id)) throw new CivException(CivSettings.localize.localizedString("civ_research_alreadyDone"));
		if (!tech.isAvailable(this)) throw new CivException(CivSettings.localize.localizedString("civ_research_missingRequirements"));

		this.setResearchTech(tech);
		this.setResearchProgress(0.0);

		this.getTreasury().withdraw(cost);
	}

	public double getSciencePercentage() {
		return sciencePercentage;
	}

	public void setSciencePercentage(double sciencePercentage) {
		if (sciencePercentage > 1.0) sciencePercentage = 1.0;
		this.sciencePercentage = sciencePercentage;
	}

	public Collection<ConfigTech> getTechs() {
		return this.techs.values();
	}

	// --------------- Town

	public void addTown(Town town) {
		towns.add(town);
	}

	public Town getTown(String name) {
		for (Town town : towns) {
			if (town.getName().equalsIgnoreCase(name)) return town;
		}
		return null;
	}

	public void removeTown(Town town) {
		towns.remove(town);
	}

	public int getTownCount() {
		return towns.size();
	}

	public Collection<Town> getTowns() {
		return towns;
	}

	public void regenControlBlocks() {
		for (Town t : getTowns()) {
			t.getCityhall().regenControlBlocks();
		}
	}

	public int getNextTownCost() {
		return 10000 * (this.getTownCount()); // TODO пересчитать сколько надо для построй следующего города
	}

	public ArrayList<RespawnLocationHolder> getAvailableRespawnables() {
		ArrayList<RespawnLocationHolder> respawns = new ArrayList<>();
		for (Town town : this.getTowns()) {
			Cityhall cityhall = town.getCityhall();
			if (cityhall != null && cityhall.isActive() && !town.defeated) respawns.add(cityhall); /* Do not respawn at defeated towns. */
			if (town.BM.hasWonder("w_neuschwanstein")) respawns.add((Neuschwanstein) town.BM.getWonderById("w_neuschwanstein"));
		}

		for (WarCamp camp : this.warCamps) {
			if (camp.isTeleportReal()) respawns.add(camp);
		}
		return respawns;
	}

	// ----------------- war

	public double getWarUpkeep() {
		double upkeep = 0;
		boolean doublePenalty = false;

		/* calculate war upkeep from being an aggressor. */
		for (Relation relation : this.getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Status.WAR) {
				if (relation.getAggressor() == this) {
					double thisWarUpkeep = 0;
					int scoreDiff = CivGlobal.getScoreForCiv(this) - CivGlobal.getScoreForCiv(relation.getOtherCiv());
					try {
						thisWarUpkeep += CivSettings.getDouble(CivSettings.warConfig, "war.upkeep_per_war");
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
						return 0;
					}
					if (scoreDiff > 0) {
						double war_penalty;
						try {
							war_penalty = CivSettings.getDouble(CivSettings.warConfig, "war.upkeep_per_war_multiplier");
						} catch (InvalidConfiguration e) {
							e.printStackTrace();
							return 0;
						}
						thisWarUpkeep += (scoreDiff) * war_penalty;
					}

					/* Try to find notredame in ourenemies buff list or their allies list. */
					ArrayList<Civilization> allies = new ArrayList<>();
					allies.add(relation.getOtherCiv());
					for (Relation relation2 : relation.getOtherCiv().getDiplomacyManager().getRelations()) {
						if (relation2.getStatus() == Status.ALLY) {
							allies.add(relation2.getOtherCiv());
						}
					}

					for (Civilization civ : allies) {
						for (Town t : civ.getTowns()) {
							if (t.getBuffManager().hasBuff("buff_notre_dame_extra_war_penalty")) {
								doublePenalty = true;
								break;
							}
						}
					}

					if (doublePenalty) thisWarUpkeep *= 2;
					upkeep += thisWarUpkeep;
				}
			}
		}
		return upkeep;
	}

	public double getWarUnhappiness() {
		double happy = 0;
		/* calculate war upkeep from being an aggressor. */
		for (Relation relation : this.getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Status.WAR) {
				if (relation.getAggressor() == this) {
					double thisWarUpkeep = 0;
					int scoreDiff = CivGlobal.getScoreForCiv(this) - CivGlobal.getScoreForCiv(relation.getOtherCiv());
					try {
						thisWarUpkeep += CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_war");
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
						return 0;
					}
					if (scoreDiff > 0) {
						double war_penalty;
						try {
							war_penalty = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_war_score");
							double addedFromPoints = (scoreDiff) * war_penalty;
							addedFromPoints = Math.min(CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_war_score_max"), addedFromPoints);
							thisWarUpkeep += addedFromPoints;
						} catch (InvalidConfiguration e) {
							e.printStackTrace();
							return 0;
						}
					}
					happy += thisWarUpkeep;
				}
			}
		}
		return happy;
	}

	public void onDefeat(Civilization attackingCiv) {
		/* The entire civilization has been defeated. We need to give our towns to the attacker. Meanwhile our civilization will become dormant. We
		 * will NOT remember who the attacker was, if we revolt we will declare war on anyone who owns our remaining towns. We will hand over all of
		 * our native towns, as well as any conquered towns we might have. Those towns when they revolt will revolt against whomever owns them. */

		for (Town town : this.getTowns()) {
			town.onDefeat(attackingCiv);
		}

		/* Remove any old relationships this civ may have had. */
		LinkedList<Relation> deletedRelations = new LinkedList<>();
		deletedRelations.addAll(this.getDiplomacyManager().getRelations());
		for (Relation relation : deletedRelations) {
			try {
				if (relation.getStatus() == Relation.Status.WAR) {
					relation.setStatus(Relation.Status.NEUTRAL);
				}
				relation.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		/* Remove ourselves from the main global civ list and into a special conquered list. */
		CivGlobal.removeCiv(this);
		CivGlobal.addConqueredCiv(this);
		this.conquered = true;
		this.conquer_date = new Date();
		this.save();
	}

	public void addWarCamp(WarCamp camp) {
		this.warCamps.add(camp);
	}

	public void onWarEnd() {
		for (WarCamp camp : this.warCamps) {
			camp.delete();
		}
		for (Town town : towns) {
			Cityhall cityhall = town.getCityhall();
			if (cityhall != null) {
				cityhall.setHitpoints(cityhall.getMaxHitPoints());
				cityhall.save();
			}
		}
	}

	public void clearAggressiveWars() {
		/* If this civ is the aggressor in any wars. Cancel the, this happens when civs go into debt. */
		LinkedList<Relation> removeUs = new LinkedList<>();
		for (Relation relation : this.getDiplomacyManager().getRelations()) {
			if (relation.getStatus().equals(Relation.Status.WAR) && relation.getAggressor() == this) removeUs.add(relation);
		}

		for (Relation relation : removeUs) {
			this.getDiplomacyManager().deleteRelation(relation);
			CivMessage.global(CivSettings.localize.localizedString("var_civ_debt_endWar", this.getName(), relation.getOtherCiv().getName()));
		}

	}

	// ------------- capitol

	public Location getCapitolLocation() {
		Town capitol = this.getCapitol();
		if (capitol == null) return null;
		return capitol.getLocation();
	}

	public Cityhall getCapitolCityHall() {
		Town capitol = this.getCapitol();
		if (capitol == null) return null;
		return capitol.getCityhall();
	}

	public void updateReviveSigns() {
		this.getCapitolCityHall().updateRespawnSigns();
	}

	public Town getCapitol() {
		return CivGlobal.getTown(capitolId);
	}

	public boolean isValid() {
		if (capitolId == 0) return false;
		Town capitol = CivGlobal.getTown(capitolId);
		if (capitol == null) return false;
		return capitol.isValid();
	}

	// -------------------- Upkeep Debt

	public double payUpkeep() throws CivException {
		double upkeep = 0;
		this.lastUpkeepPaidMap.clear();
		if (this.isAdminCiv()) return 0;
		if (this.getCapitol() == null) throw new CivException("Civilization found with no capitol!");

		for (Town t : this.getTowns()) {
			/* Calculate upkeep from extra towns, obviously ignore the capitol itself. */
			if (!t.isCapitol()) {
				try {
					/* Base upkeep per town. */
					upkeep += CivSettings.getDoubleCiv("civ.town_upkeep");
					lastUpkeepPaidMap.put(t.getName() + ",base", upkeep);
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
				}
			}
		}

		upkeep += this.getWarUpkeep();

		if (this.getCapitol() != null) upkeep += this.getCapitol().getBonusUpkeep();
		if (this.getTreasury().hasEnough(upkeep)) {
			/* Have plenty on our coffers, pay the lot and clear all of these towns' debt. */
			this.getTreasury().withdraw(upkeep);
		} else {
			/* Doh! We dont have enough money to pay for our upkeep, go into debt. */
			double diff = upkeep - this.getTreasury().getBalance();
			this.getTreasury().setDebt(this.getTreasury().getDebt() + diff);
			this.getTreasury().withdraw(this.getTreasury().getBalance());
		}
		return upkeep;
	}

	public void warnDebt() {
		CivMessage.global(CivSettings.localize.localizedString("var_civ_debtAnnounce", this.getName(), this.getTreasury().getDebt(), CivSettings.CURRENCY_NAME));
	}

	public void incrementDaysInDebt() {
		daysInDebt++;
		if (daysInDebt >= CivSettings.CIV_DEBT_GRACE_DAYS) {
			if (daysInDebt >= CivSettings.CIV_DEBT_SELL_DAYS) {
				if (daysInDebt >= CivSettings.CIV_DEBT_TOWN_SELL_DAYS) {
					CivMessage.global(CivSettings.localize.localizedString("var_civ_fellIntoRuin", this.getName()));
					this.delete();
					return;
				}
			}
		}
		// warn sell..
		CivMessage.global(CivSettings.localize.localizedString("var_civ_debtGlobalAnnounce", this.getName()) + " " + getDaysLeftWarning());
		this.save();
	}

	public String getDaysLeftWarning() {
		if (daysInDebt < CivSettings.CIV_DEBT_GRACE_DAYS) return CivSettings.localize.localizedString("var_civ_daystillSaleAnnounce", (CivSettings.CIV_DEBT_GRACE_DAYS - daysInDebt));
		if (daysInDebt < CivSettings.CIV_DEBT_SELL_DAYS) return CivSettings.localize.localizedString("var_civ_isForSale1", this.getName(), (CivSettings.CIV_DEBT_SELL_DAYS - daysInDebt));
		if (daysInDebt < CivSettings.CIV_DEBT_TOWN_SELL_DAYS) return CivSettings.localize.localizedString("var_civ_isForSale2", this.getName(), (CivSettings.CIV_DEBT_TOWN_SELL_DAYS - daysInDebt));
		return "";
	}

	public String getUpkeepPaid(Town town, String type) {
		String out = "";
		if (lastUpkeepPaidMap.containsKey(town.getName() + "," + type))
			out += lastUpkeepPaidMap.get(town.getName() + "," + type);
		else
			out += "0";
		return out;
	}

	public void taxPayment(Town town, double amount) {
		Double townPaid = this.lastTaxesPaidMap.get(town.getName());
		if (townPaid == null)
			townPaid = amount;
		else
			townPaid += amount;
		this.lastTaxesPaidMap.put(town.getName(), townPaid);
		double beakerAmount = amount * this.sciencePercentage;
		amount -= beakerAmount;
		this.getTreasury().deposit(amount);
		this.save();
		double coins_per_beaker;
		try {
			coins_per_beaker = CivSettings.getDouble(CivSettings.civConfig, "civ.coins_per_beaker");
			for (Town t : this.getTowns()) {
				if (t.getBuffManager().hasBuff("buff_greatlibrary_double_tax_beakers")) coins_per_beaker /= 2;
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		double totalBeakers = 0.1 * Math.floor(10 * beakerAmount / coins_per_beaker);
		if (totalBeakers == 0) return;
		if (this.researchTech != null) this.processTech(totalBeakers);
	}

	// ------------ Sale and buy

	public boolean isTownsForSale() {
		return daysInDebt >= CivSettings.CIV_DEBT_SELL_DAYS;
	}

	public boolean isForSale() {
		if (this.getTownCount() == 0) return false;
		return daysInDebt >= CivSettings.CIV_DEBT_GRACE_DAYS;
	}

	public double getForSalePriceFromCivOnly() {
		int effectivePoints;
		effectivePoints = this.getTechScore();
		double coins_per_point;
		try {
			coins_per_point = CivSettings.getDouble(CivSettings.scoreConfig, "coins_per_point");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return 0;
		}
		return coins_per_point * effectivePoints;
	}

	public double getTotalSalePrice() {
		double price = getForSalePriceFromCivOnly();
		for (Town town : this.getTowns()) {
			price += town.getForSalePrice();
		}
		return price;
	}

	public void buyCiv(Civilization civ) throws CivException {
		if (!this.getTreasury().hasEnough(civ.getTotalSalePrice())) throw new CivException(CivSettings.localize.localizedString("civ_buy_notEnough") + " " + CivSettings.CURRENCY_NAME);
		this.getTreasury().withdraw(civ.getTotalSalePrice());
		this.mergeInCiv(civ);
	}

	public void buyTown(Town town) throws CivException {
		if (!this.getTreasury().hasEnough(town.getForSalePrice())) throw new CivException(CivSettings.localize.localizedString("civ_buy_notEnough") + " " + CivSettings.CURRENCY_NAME);

		this.getTreasury().withdraw(town.getForSalePrice());
		town.changeCiv(this);
		town.setMotherCiv(null);
		town.setDebt(0);
		town.setDaysInDebt(0);
		town.save();
		CivGlobal.processCulture();
		CivMessage.global(CivSettings.localize.localizedString("var_civ_buyTown_Success1", this.getName(), this.getName()));

	}

	// --------------- beakers

	public void processBeakers() {
		if (capitolId == 0) {
			CivMessage.sendCiv(this, CivSettings.localize.localizedString("var_beaker_noCapitol", this.getName()));
			return;
		}
		if (!getCapitol().isValid()) {
			CivMessage.sendCiv(this, CivSettings.localize.localizedString("beaker_noCapitolHall"));
			return;
		}

		if (this.getResearchTech() == null) {
			this.getTreasury().deposit(getBeakersCivtick());
			return;
		}

		this.processTech(this.getBeakersCivtick());
	}

	public double getBeakersCivtick() {
		double total = 0;
		for (Town town : this.getTowns()) {
			total += town.SM.getAttr(StorageType.BEAKERS).total;
		}
		total += baseBeakers;
		return total;
	}

	// --------------- Government

	public void setGovernment(String gov_id) {
		this.government = CivSettings.governments.get(gov_id);
		if (this.getSciencePercentage() > this.government.maximum_tax_rate) this.setSciencePercentage(this.government.maximum_tax_rate);
	}

	public void changeGovernment(Civilization civ, ConfigGovernment gov, boolean force) throws CivException {
		if (civ.getGovernment() == gov && !force) throw new CivException(CivSettings.localize.localizedString("var_civ_gov_already", gov.displayName));
		if (civ.getGovernment().id.equals("gov_anarchy")) throw new CivException(CivSettings.localize.localizedString("civ_gov_errorAnarchy"));

		boolean noanarchy = false;
		for (Town t : this.getTowns()) {
			if (t.getBuffManager().hasBuff("buff_notre_dame_no_anarchy")) {
				noanarchy = true;
				break;
			}
		}

		if (!noanarchy) {
			String key = "changegov_" + this.getId();
			String value = gov.id;

			sessionAdd(key, value);

			// Set the town's government to anarchy in the meantime
			civ.setGovernment("gov_anarchy");
			CivMessage.global(CivSettings.localize.localizedString("var_civ_gov_anachyAlert", this.getName()));
		} else {
			civ.setGovernment(gov.id);
			CivMessage.global(CivSettings.localize.localizedString("var_civ_gov_success", civ.getName(), CivSettings.governments.get(gov.id).displayName));
		}
		civ.save();
	}

	// --------------- diplomaty

	public void mergeInCiv(Civilization oldciv) {
		if (oldciv == this) return;
		/* Grab each town underneath and add it to us. */
		for (Town town : oldciv.getTowns()) {
			town.changeCiv(this);
			town.setDebt(0);
			town.setDaysInDebt(0);
			town.save();
		}

		if (oldciv.towns.size() > 0) {
			CivLog.error("CIV SOMEHOW STILL HAS TOWNS AFTER WE GAVE THEM ALL AWAY WTFWTFWTFWTF.");
			this.towns.clear();
		}
		oldciv.delete();
		CivGlobal.processCulture();
	}

	public double getRevolutionFee() {
		try {
			double base_coins = CivSettings.getDouble(CivSettings.warConfig, "revolution.base_cost");
			double coins_per_town = CivSettings.getDouble(CivSettings.warConfig, "revolution.coins_per_town");
			double coins_per_point = CivSettings.getDouble(CivSettings.warConfig, "revolution.coins_per_point");
			double max_fee = CivSettings.getDouble(CivSettings.warConfig, "revolution.maximum_fee");

			double total_coins = base_coins;

			double motherCivPoints = this.getTechScore();
			for (Town t : CivGlobal.getTowns()) {
				if (t.getMotherCiv() == this) {
					motherCivPoints += t.getScore();
					total_coins += coins_per_town;
				}
			}

			total_coins += motherCivPoints * coins_per_point;
			if (total_coins > max_fee) total_coins = max_fee;
			return total_coins;
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return Double.MAX_VALUE;
		}
	}

	public void setConquered(boolean b) {
		this.conquered = b;
	}

	public void capitulate() {
		for (Town t : CivGlobal.getTowns()) {
			if (t.getMotherCiv() == this) {
				t.setMotherCiv(null);
				t.save();
			}
		}
		this.delete();
		CivMessage.global(CivSettings.localize.localizedString("var_civ_capitulate_Success1", this.getName()));
	}

	public int getMergeCost() {
		int total = 0;
		for (Town town : this.towns) {
			total += town.getGiftCost();
		}
		return total;
	}

	public void validateGift() throws CivException {
		try {
			int min_gift_age = CivSettings.getInteger(CivSettings.civConfig, "civ.min_gift_age");
			if (!DateUtil.isAfterDays(created_date, min_gift_age)) throw new CivException(CivSettings.localize.localizedString("var_civ_gift_tooyoung1", this.getName(), min_gift_age));
		} catch (InvalidConfiguration e) {
			throw new CivException(CivSettings.localize.localizedString("internalException"));
		}
	}

	// ---------------- resident

	public boolean hasResident(Resident resident) {
		if (resident == null) return false;
		for (Town t : this.getTowns()) {
			if (t.hasResident(resident)) return true;
		}
		return false;
	}

	public Collection<Resident> getOnlineResidents() {
		LinkedList<Resident> residents = new LinkedList<>();
		for (Town t : this.getTowns()) {
			residents.addAll(t.getOnlineResidents());
		}
		return residents;
	}

	public void repositionPlayers(String reason) {
		if (!this.getDiplomacyManager().isAtWar()) return;
		for (Town t : this.getTowns()) {
			Cityhall cityhall = t.getCityhall();
			if (cityhall == null) {
				CivLog.error("Town hall was null for " + t.getName() + " when trying to reposition players.");
				continue;
			}

			// if (!townhall.isComplete()) {
			// CivLog.error("Town hall was not completed before war time. Unable to
			// reposition players.");
			// continue;
			// }

			for (Resident resident : t.getResidents()) {
				// if (townhall.isActive()) {

				try {
					Player player = CivGlobal.getPlayer(resident);
					ChunkCoord coord = new ChunkCoord(player.getLocation());
					CultureChunk cc = CivGlobal.getCultureChunk(coord);
					if (cc != null && cc.getCiv() != this && cc.getCiv().getDiplomacyManager().atWarWith(this)) {
						CivMessage.send(player, CivColor.Purple + reason);
						BlockCoord revive = cityhall.getRandomRevivePoint();
						player.teleport(revive.getLocation());
					}

				} catch (CivException e) {
					// player not online....
				}
				// } else {
				// // use player spawn point instead.
				// Player player;
				// try {
				// player = CivGlobal.getPlayer(resident);
				//
				// if (player.getBedSpawnLocation() != null) {
				// player.teleport(player.getBedSpawnLocation());
				// CivMessage.send(player, CivColor.Purple+reason);
				// } else {
				// player.gets
				// }
				// } catch (CivException e) {
				// // player not online
				// }
				// }
			}
		}
	}

	public void depositFromResident(Resident resident, Double amount) throws CivException, SQLException {
		if (!resident.getTreasury().hasEnough(amount)) throw new CivException(CivSettings.localize.localizedString("var_civ_deposit_NotEnough", CivSettings.CURRENCY_NAME));

		if (this.getTreasury().inDebt()) {
			if (this.getTreasury().getDebt() >= amount) {
				this.getTreasury().setDebt(this.getTreasury().getDebt() - amount);
				resident.getTreasury().withdraw(amount);
			} else
				if (this.getTreasury().getDebt() < amount) {
					double leftAmount = amount - this.getTreasury().getDebt();
					this.getTreasury().setDebt(0);
					this.getTreasury().deposit(leftAmount);
					resident.getTreasury().withdraw(amount);
				}

			if (!this.getTreasury().inDebt()) {
				this.daysInDebt = 0;
				CivMessage.global(CivSettings.localize.localizedString("var_civ_deposit_cleardebt", this.getName()));
			}
		} else {
			this.getTreasury().deposit(amount);
			resident.getTreasury().withdraw(amount);
		}
		this.save();
	}

	public void rebuildTag() {
		TagManager.editNameTag(this);
	}

	public String MOTD() {
		return this.messageOfTheDay;
	}

	// ------------------- endGame winers and score

	public int getTechScore() {
		int points = 0;
		for (ConfigTech t : this.getTechs()) {
			points += t.points;
		}
		return points;
	}

	public int getScore() {
		int points = 0;
		for (Town t : this.getTowns()) {
			points += t.getScore();
		}
		points += getTechScore();
		return points;
	}

	public void declareAsWinner(EndGameCondition end) {
		String out = CivSettings.localize.localizedString("var_civ_victory_end1", this.getName(), end.getVictoryName());
		CivGlobal.getSessionDatabase().add("endgame:winningCiv", out, 0, 0, 0);
		CivMessage.global(out);
	}

	public void winConditionWarning(EndGameCondition end, int daysLeft) {
		CivMessage.global(CivSettings.localize.localizedString("var_civ_victory_end2", this.getName(), end.getVictoryName(), daysLeft));
	}

	public double getPercentageConquered() {
		int totalCivs = CivGlobal.getCivs().size() + CivGlobal.getConqueredCivs().size();
		int conqueredCivs = 1; /* Your civ already counts */

		for (Civilization civ : CivGlobal.getConqueredCivs()) {
			Town capitol = civ.getCapitol();
			if (capitol == null) {
				/* Invalid civ? */
				totalCivs--;
				continue;
			}
			if (capitol.getCiv() == this) conqueredCivs++;
		}

		return (double) conqueredCivs / (double) totalCivs;
	}

	// --------------- CurrentMission

	public int getCurrentMission() {
		return this.currentMission;
	}

	public void setCurrentMission(int newMission) {
		this.currentMission = newMission;
	}

	public boolean getMissionActive() {
		return this.missionActive;
	}

	public void setMissionActive(boolean missionActive) {
		this.missionActive = missionActive;
	}

	public void updateMissionProgress(double beakers, double hammers) {
		this.missionProgress = beakers + ":" + hammers;
	}

	public void restoreMissionProgress() {
		this.missionProgress = "";
	}

	public String getMissionProgress() {
		return this.missionProgress;
	}

	// -------------- buildable

	public boolean hasWonder(final String id) {
		for (final Town town : this.getTowns()) {
			if (town.BM.hasWonder(id)) return true;
		}
		return false;
	}

	public Town getStatueTown() {
		for (final Town town : this.getTowns()) {
			if (town.BM.hasWonder("w_statue_of_zeus")) return town;
		}
		return null;
	}

	public double getTotalAnarchyTime() {
		double memberHours = 0.0;
		boolean hasNotreDame = false;
		boolean noanarchy = false;
		for (final Town t : this.getTowns()) {
			final double residentHours = t.getResidentCount();
			double modifier = 1.0;
			if (t.getBuffManager().hasBuff("buff_reduced_anarchy")) modifier -= t.getBuffManager().getEffectiveDouble("buff_reduced_anarchy");
			if (t.getBuffManager().hasBuff("buff_noanarchy")) noanarchy = true;
			if (t.BM.hasWonder("w_notre_dame")) hasNotreDame = true;
			memberHours += residentHours * modifier;
		}
		double maxAnarchy = CivSettings.getIntegerGovernment("anarchy_duration");
		if (noanarchy) maxAnarchy = CivSettings.getIntegerGovernment("notre_dame_max_anarchy");
		double anarchyHours = Math.min(memberHours, maxAnarchy);
		if (hasNotreDame) anarchyHours /= 2.0;

		return (double) Math.round(anarchyHours);
	}

	public int getStockExchangeLevel() {
		StockExchange stockExchange = null;
		for (Town town : this.getTowns()) {
			for (Wonder wonder : town.BM.getWonders()) {
				if (wonder.isActive() && !this.isConquered() && wonder.getConfigId().equalsIgnoreCase("w_stock_exchange")) {
					stockExchange = (StockExchange) wonder;
				}
			}
			if (stockExchange != null) break;
		}
		if (stockExchange == null) return 0;
		return stockExchange.getLevel();
	}

	// -------------------- Cave

	// public void showCaveStatus(Player player, StatusType statusType) {
	// if (statusType == null) {
	// CivMessage.sendHeading(player, "Все пещеры");
	// } else
	// CivMessage.sendHeading(player, statusType + " пещеры");
	// for (CaveStatus cs : this.caveStatuses.values()) {
	// if (statusType != null && cs.statusType != statusType) continue;
	// String cc;
	// switch (cs.statusType) {
	// case founded:
	// cc = CivColor.Gray + "(founded) ";
	// break;
	// case available:
	// cc = CivColor.Green + "(available)";
	// break;
	// case captured:
	// cc = CivColor.LightBlue + "(captured) ";
	// break;
	// case lost:
	// cc = CivColor.Red + "(lost) ";
	// break;
	// case updated:
	// cc = CivColor.Navy + "(updated) ";
	// break;
	// case used:
	// cc = CivColor.White + "(used) ";
	// break;
	// default:
	// cc = "";
	// break;
	// }
	// CivMessage.send(player, cc + cs.getCave().getCornerEntrance().toString() + " : " + cs.getCave().getDisplayName());
	// }
	// }
	//
	// private String saveCaveStatus() {
	// String res = "";
	// for (CaveStatus cstatus : caveStatuses.values()) {
	// res = res + cstatus.caveId + ":";
	// res = res + cstatus.statusType.toString() + ":";
	// res = res + cstatus.date.getTime() + ":";
	// res = res + cstatus.activatorId + ",";
	// }
	// return res;
	// }
	//
	// private void loadCaveStatus(String string) {
	// if (string == null || string.isEmpty()) return;
	// String[] csSpl = string.split(",");
	// for (int k = 0; k < csSpl.length; k++) {
	// String[] propertySpl = csSpl[k].split(":");
	// int id = Integer.parseInt(propertySpl[0]);
	//
	// StatusType statusType = StatusType.valueOf(propertySpl[1]);
	// Date date = new Date(Long.parseLong(propertySpl[2]));
	// int activator = Integer.parseInt(propertySpl[3]);
	//
	// CaveStatus cs = new CaveStatus(statusType, id, date, activator);
	// this.caveStatuses.put(id, cs);
	// }
	// }
	//
	// public void addDisputedCave(Cave cave) {
	// disputedCave.add(cave);
	// }
	//
	// public void addTakedCave(Cave cave) {
	// takedCave.add(cave);
	// }
	//
	// public CaveStatus getCaveStatus(Cave cave) {
	// return this.caveStatuses.get(cave.getMaterialId());
	// }
	//
	// public void addCaveStatus(Cave cave, CaveStatus caveStatus) {
	// this.caveStatuses.put(cave.getMaterialId(), caveStatus);
	// this.save();
	// }
	//
	// public void removeCave(Cave cave, Civilization newCiv) {
	// CaveStatus cs = this.getCaveStatus(cave);
	// if (cs == null) return;
	// cs.editCaveStatusLost(newCiv);
	// this.caveStatuses.put(cave.getMaterialId(), cs);
	// this.save();
	// }
	//
	// public void addCave(Cave cave) {
	// CaveStatus cs = this.getCaveStatus(cave);
	// StatusType statusType = StatusType.captured;
	// Date date = new Date();
	// if (cs == null)
	// cs = new CaveStatus(StatusType.captured, cave.getMaterialId(), date, this.getMaterialId());
	// else {
	// cs.statusType = statusType;
	// cs.date = date;
	// cs.activatorId = this.getMaterialId();
	// }
	// this.caveStatuses.put(cave.getMaterialId(), cs);
	// this.save();
	// }

	// -------------- other

	public String getIncomeTaxRateString() {
		return (this.incomeTaxRate * 100) + "%";
	}

	public void setAdminCiv(boolean adminCiv) {
		this.adminCiv = adminCiv;
		if (adminCiv)
			CivGlobal.addAdminCiv(this);
		else
			CivGlobal.removeAdminCiv(this);
		this.save();
	}

	public double getDistanceUpkeepAtLocation(Location capitolTownHallLoc, Location townHallLoc, boolean touching) throws InvalidConfiguration {
		double town_distance_base_upkeep = CivSettings.getDoubleCiv("civ.town_distance_base_upkeep");
		double distance_multiplier_touching = CivSettings.getDoubleCiv("civ.town_distance_multiplier");
		double distance_multiplier_not_touching = CivSettings.getDoubleCiv("civ.town_distance_multiplier_outside_culture");
		double maxDistanceUpkeep = CivSettings.getDoubleCiv("civ.town_distance_upkeep_max");

		double distance = capitolTownHallLoc.distance(townHallLoc);
		double distanceUpkeep;
		if (touching) {
			distanceUpkeep = town_distance_base_upkeep * (Math.pow(distance, distance_multiplier_touching));
		} else {
			distanceUpkeep = town_distance_base_upkeep * (Math.pow(distance, distance_multiplier_not_touching));
		}

		if (distanceUpkeep > maxDistanceUpkeep) {
			distanceUpkeep = maxDistanceUpkeep;
		}

		distanceUpkeep = Math.round(distanceUpkeep);
		return distanceUpkeep;
	}

	public double getDistanceHappiness(Location capitolTownHallLoc, Location townHallLoc, boolean touching) throws InvalidConfiguration {
		double town_distance_base_happy = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_base");
		double distance_multiplier_touching = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_multiplier");
		double distance_multiplier_not_touching = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_multiplier_outside_culture");
		double maxDistanceHappiness = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_max");
		double distance = capitolTownHallLoc.distance(townHallLoc);
		double distance_happy;

		distance_happy = touching ? //
				town_distance_base_happy * (Math.pow(distance, distance_multiplier_touching)) : //
				town_distance_base_happy * (Math.pow(distance, distance_multiplier_not_touching));

		if (distance_happy > maxDistanceHappiness) distance_happy = maxDistanceHappiness;

		distance_happy = Math.round(distance_happy);
		return distance_happy;
	}

	public String getCultureDescriptionString() {
		String out = "";
		out += "<b>" + this.getName() + "</b>";
		return out;
	}

	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDatabase().add(key, value, this.getId(), 0, 0);
	}

	public void sessionDeleteAll(String key) {
		CivGlobal.getSessionDatabase().delete_all(key);
	}

	/* populates the sources with happiness sources. */
	public double getCivWideUnhappiness(HashMap<String, Double> sources) {
		double total = 0;
		try {
			/* Get happiness per town. */
			double per_town = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_town");
			double per_captured_town = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_captured_town");

			double happy_town = 0;
			double happy_captured_town = 0;
			for (Town town : this.getTowns()) {
				if (town.getMotherCiv() == null) {
					if (!town.isCapitol()) {
						happy_town += per_town;
					}
				} else {
					happy_captured_town += per_captured_town;
				}
			}

			total += happy_town;
			sources.put("Towns", happy_town);

			boolean civHasColossus = false;
			for (final Town town2 : this.getTowns()) {
				for (final Wonder wonder : town2.BM.getWonders()) {
					if (wonder.getConfigId().equalsIgnoreCase("w_colossus") && wonder.isComplete() && wonder.isActive()) {
						civHasColossus = true;
					}
				}
			}
			if (civHasColossus) happy_captured_town = 0.0;
			total += happy_captured_town;
			sources.put("Captured Towns", happy_captured_town);

			/* Get unhappiness from wars. */
			double war_happy = this.getWarUnhappiness();
			total += war_happy;
			sources.put("War", war_happy);

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}

		return total;
	}

	public void rename(String name) throws CivException, InvalidNameException {
		Civilization other = CivGlobal.getCivFromName(name);
		if (other != null) throw new CivException(CivSettings.localize.localizedString("civ_rename_errorExists"));

		other = CivGlobal.getConqueredCiv(name);
		if (other != null) throw new CivException(CivSettings.localize.localizedString("civ_rename_errorExists"));

		if (this.conquered)
			CivGlobal.removeConqueredCiv(this);
		else
			CivGlobal.removeCiv(this);

		String oldName = this.getName();
		this.setName(name);
		this.save();

		if (this.conquered)
			CivGlobal.addConqueredCiv(this);
		else
			CivGlobal.addCiv(this);

		CivMessage.global(CivSettings.localize.localizedString("var_civ_rename_success1", oldName, this.getName()));
	}

	public ItemStack getRandomLeaderSkull(String message) {
		Random rand = new Random();
		int i = rand.nextInt(this.GM.getLeaders().size());
		int count = 0;
		Resident resident = this.GM.getLeader();

		for (Resident res : this.GM.getLeaders()) {
			if (count == i) {
				resident = res;
				break;
			}
		}

		String leader = (resident == null) ? "" : resident.getName();
		return ItemManager.spawnPlayerHead(leader, message + " (" + leader + ")");
	}

}
