/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure.wonders;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigWonderBuff;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Wonder extends Buildable {

	public static String TABLE_NAME = "WONDERS";
	private ConfigWonderBuff wonderBuffs = null;

	public Wonder(ResultSet rs) throws SQLException, CivException {
		this.load(rs);
		if (this.getHitpoints() == 0) this.delete();
	}

	public static Wonder newWonder(ResultSet rs) throws CivException, SQLException {
		return _newWonder(rs.getString("type_id"), null, rs);
	}

	public Wonder(String id, Town town) throws CivException {
		this.setInfo(CivSettings.wonders.get(id));
		this.setSQLOwner(town);
	}

	public void loadSettings() {
		wonderBuffs = CivSettings.wonderBuffs.get(this.getConfigId());

		if (this.isComplete() && this.isActive()) {
			this.addWonderBuffsToTown();
		}
	}

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" //
					+ "`id` int(11) unsigned NOT NULL auto_increment,"//
					+ "`type_id` mediumtext NOT NULL,"//
					+ "`town_id` int(11) DEFAULT NULL,"//
					+ "`complete` bool NOT NULL DEFAULT '0',"//
					+ "`hammersCompleted` int(11) DEFAULT NULL, "//
					+ "`cornerBlockHash` mediumtext DEFAULT NULL,"//
					+ "`template_name` mediumtext DEFAULT NULL, " //
					+ "`hitpoints` int(11) DEFAULT '100'," //
					+ "PRIMARY KEY (`id`)" + ")";

			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, CivException {
		this.setId(rs.getInt("id"));
		this.setInfo(CivSettings.wonders.get(rs.getString("type_id")));
		this.setSQLOwner(CivGlobal.getTown(rs.getInt("town_id")));
		if (this.getTown() == null) {
			// CivLog.warning("Coudln't find town ID:"+rs.getInt("town_id")+ " for wonder
			// "+this.getDisplayName()+" ID:"+this.getId());
			throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for wonder " + this.getDisplayName() + " ID:" + this.getId());
		}

		this.corner = new BlockCoord(rs.getString("cornerBlockHash"));
		this.setHitpoints(rs.getInt("hitpoints"));
		this.setTemplate(Template.getTemplate(rs.getString("template_name")));
		this.setComplete(rs.getBoolean("complete"));
		this.setHammersCompleted(rs.getInt("hammersCompleted"));

		this.getTown().BM.addWonder(this);

		this.startWonderOnLoad();

		if (this.isComplete() == false) {
			try {
				this.startBuildTask();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.bindBlocks();
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("type_id", this.getConfigId());
		hashmap.put("town_id", this.getTown().getId());
		hashmap.put("complete", this.isComplete());
		hashmap.put("hammersCompleted", this.getHammersCompleted());
		hashmap.put("cornerBlockHash", this.getCorner().toString());
		hashmap.put("hitpoints", this.getHitpoints());
		hashmap.put("template_name", this.getTemplate().getFilepath());
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() {
		if (this.getTown() != null) {
			if (this.wonderBuffs != null) {
				for (ConfigBuff buff : this.wonderBuffs.buffs) {
					this.getTown().getBuffManager().removeBuff(buff.id);
				}
			}
		}
		super.delete();

		if (this.getTown() != null) this.getTown().BM.removeWonder(this);
		CivGlobal.removeWonder(this);

		try {
			SQL.deleteNamedObject(this, TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateBuildProgess() {
		if (this.getId() != 0) {
			HashMap<String, Object> struct_hm = new HashMap<String, Object>();
			struct_hm.put("id", this.getId());
			struct_hm.put("type_id", this.getConfigId());
			struct_hm.put("complete", this.isComplete());
			struct_hm.put("hammersCompleted", this.getHammersCompleted());
			SQL.updateNamedObjectAsync(this, struct_hm, TABLE_NAME);
		}
	}

	public static boolean isWonderAvailable(String configId) {
		if (CivGlobal.isCasualMode()) {
			return true;
		}

		for (Wonder wonder : CivGlobal.getWonders()) {
			if (wonder.getConfigId().equals(configId)) {
				if (wonder.getConfigId().equals("w_colosseum") || wonder.getConfigId().equals("w_battledome")) {
					return true;
				}
				if (wonder.isComplete()) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void processUndo() throws CivException {
		try {
			this.undoFromTemplate();
		} catch (IOException e1) {
			e1.printStackTrace();
			CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("wonder_undo_error"));
			this.fancyDestroyConstructBlocks();
		}

		CivMessage.global(CivSettings.localize.localizedString("var_wonder_undo_broadcast", (CivColor.LightGreen + this.getDisplayName() + CivColor.White), this.getTown().getName(), this.getTown().getCiv().getName()));

		double refund = this.getCost();
		this.getTown().depositDirect(refund);
		CivMessage.sendTown(getTown(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTown().getName(), refund, CivSettings.CURRENCY_NAME));

		this.deleteWithUndo();
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "beer";
	}

	public void onDestroy() {
		if (!CivGlobal.isCasualMode()) {
			// can be overriden in subclasses.
			CivMessage.global(CivSettings.localize.localizedString("var_wonder_destroyed", this.getDisplayName(), this.getTown().getName()));
			this.deleteWithFancy();
		}
	}

	public static Wonder newWonder(Player player, Location location, String id, Town town) throws CivException {
		Wonder wonder;
		try {
			wonder = _newWonder(id, town, null);
		} catch (SQLException e) {
			throw new CivException("SQLException");
		}
		wonder.initDefaultTemplate(location);
		town.BM.checkIsTownCanBuildBuildable(wonder);
		wonder.checkBlockPermissionsAndRestrictions(player);
		return wonder;
	}

	public static Wonder _newWonder(String id, Town town, ResultSet rs) throws CivException, SQLException {
		Wonder wonder;
		switch (id) {
		case "w_pyramid":
			if (rs == null) {
				wonder = new TheGreatPyramid(id, town);
			} else {
				wonder = new TheGreatPyramid(rs);
			}
			break;
		case "w_greatlibrary":
			if (rs == null) {
				wonder = new GreatLibrary(id, town);
			} else {
				wonder = new GreatLibrary(rs);
			}
			break;
		case "w_oracle":
			if (rs == null) {
				wonder = new Oracle(id, town);
			} else {
				wonder = new Oracle(rs);
			}
			break;
		case "w_hanginggardens":
			if (rs == null) {
				wonder = new TheHangingGardens(id, town);
			} else {
				wonder = new TheHangingGardens(rs);
			}
			break;
		case "w_colossus":
			if (rs == null) {
				wonder = new TheColossus(id, town);
			} else {
				wonder = new TheColossus(rs);
			}
			break;
		case "w_notre_dame":
			if (rs == null) {
				wonder = new NotreDame(id, town);
			} else {
				wonder = new NotreDame(rs);
			}
			break;
		case "w_chichen_itza":
			if (rs == null) {
				wonder = new ChichenItza(id, town);
			} else {
				wonder = new ChichenItza(rs);
			}
			break;
		case "w_council_of_eight":
			if (rs == null) {
				wonder = new CouncilOfEight(id, town);
			} else {
				wonder = new CouncilOfEight(rs);
			}
			break;
		case "w_colosseum":
			if (rs == null) {
				wonder = new Colosseum(id, town);
			} else {
				wonder = new Colosseum(rs);
			}
			break;
		case "w_globe_theatre":
			if (rs == null) {
				wonder = new GlobeTheatre(id, town);
			} else {
				wonder = new GlobeTheatre(rs);
			}
			break;
		case "w_great_lighthouse":
			if (rs == null) {
				wonder = new GreatLighthouse(id, town);
			} else {
				wonder = new GreatLighthouse(rs);
			}
			break;
		case "w_mother_tree":
			if (rs == null) {
				wonder = new MotherTree(id, town);
			} else {
				wonder = new MotherTree(rs);
			}
			break;
		case "w_grand_ship_ingermanland":
			if (rs == null) {
				wonder = new GrandShipIngermanland(id, town);
			} else {
				wonder = new GrandShipIngermanland(rs);
			}
			break;
		case "w_battledome":
			if (rs == null) {
				wonder = new Battledome(id, town);
			} else {
				wonder = new Battledome(rs);
			}
			break;
		case "w_stock_exchange":
			if (rs == null) {
				wonder = new StockExchange(id, town);
				break;
			}
			wonder = new StockExchange(rs);
			break;
		case "w_burj":
			if (rs == null) {
				wonder = new Burj(id, town);
				break;
			}
			wonder = new Burj(rs);
			break;
		case "w_grandcanyon":
			if (rs == null) {
				wonder = new GrandCanyon(id, town);
				break;
			}
			wonder = new GrandCanyon(rs);
			break;
		case "w_statue_of_zeus":
			if (rs == null) {
				wonder = new StatueOfZeus(id, town);
				break;
			}
			wonder = new StatueOfZeus(rs);
			break;
		case "w_space_shuttle":
			if (rs == null) {
				wonder = new SpaceShuttle(id, town);
				break;
			}
			wonder = new SpaceShuttle(rs);
			break;
		case "w_moscow_state_uni":
			if (rs == null) {
				wonder = new MoscowStateUni(id, town);
				break;
			}
			wonder = new MoscowStateUni(rs);
			break;
		case "w_neuschwanstein":
			if (rs == null) {
				wonder = new Neuschwanstein(id, town);
				break;
			}
			wonder = new Neuschwanstein(rs);
			break;

		default:
			throw new CivException(CivSettings.localize.localizedString("wonder_unknwon_type") + " " + id);
		}

		wonder.loadSettings();
		return wonder;
	}

	public void addWonderBuffsToTown() {

		if (this.wonderBuffs == null) return;
		for (ConfigBuff buff : this.wonderBuffs.buffs) {
			try {
				this.getTown().getBuffManager().addBuff("wonder:" + this.getDisplayName() + ":" + this.getCorner() + ":" + buff.id, buff.id, this.getDisplayName());
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onComplete() {
		addWonderBuffsToTown();
	}

	public ConfigWonderBuff getWonderBuffs() {
		return wonderBuffs;
	}

	public void setWonderBuffs(ConfigWonderBuff wonderBuffs) {
		this.wonderBuffs = wonderBuffs;
	}

	@Override
	public void onLoad() {
	}

	@Override
	public void onUnload() {
	}

	private void startWonderOnLoad() {
		Wonder wonder = this;
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				try {
					wonder.onLoad();
				} catch (Exception e) {
					CivLog.error(e.getMessage());
					e.printStackTrace();
				}
			}
		}, 2000);
	}

	protected void addBuffToTown(Town town, String id) {
		try {
			town.getBuffManager().addBuff(id, id, this.getDisplayName() + " in " + this.getTown().getName());
		} catch (CivException e) {
			e.printStackTrace();
		}
	}

	protected void addBuffToCiv(Civilization civ, String id) {
		for (Town t : civ.getTowns()) {
			addBuffToTown(t, id);
		}
	}

	protected void removeBuffFromTown(Town town, String id) {
		town.getBuffManager().removeBuff(id);
	}

	protected void removeBuffFromCiv(Civilization civ, String id) {
		for (Town t : civ.getTowns()) {
			removeBuffFromTown(t, id);
		}
	}

	protected void removeBuffs() {
	}

	protected void addBuffs() {
	}

	public void processCoinsFromCulture() {
		int cultureCount = 0;
		for (Town t : this.getCiv().getTowns()) {
			cultureCount += t.getCultureChunks().size();
		}

		double coinsPerCulture = Double.valueOf(CivSettings.buffs.get("buff_colossus_coins_from_culture").value);

		double total = coinsPerCulture * cultureCount;
		this.getCiv().getTreasury().deposit(total);

		CivMessage.sendCiv(this.getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_colossus_generatedCoins", (CivColor.Yellow + total + CivColor.LightGreen), CivSettings.CURRENCY_NAME, cultureCount));
	}

	public void processCoinsFromColosseum() {
		int townCount = 0;
		for (Civilization civ : CivGlobal.getCivs()) {
			townCount += civ.getTownCount();
		}
		double coinsPerTown = Double.valueOf(CivSettings.buffs.get("buff_colosseum_coins_from_towns").value);

		double total = coinsPerTown * townCount;
		this.getCiv().getTreasury().deposit(total);

		CivMessage.sendCiv(this.getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_colosseum_generatedCoins", (CivColor.Yellow + total + CivColor.LightGreen), CivSettings.CURRENCY_NAME, townCount));
	}

	public void processCoinsFromNeuschwanstein() {
		int castleCount = 0;
		for (Civilization civ : CivGlobal.getCivs()) {
			for (Town town : civ.getTowns()) {
				if (town.BM.hasStructure("s_castle")) {
					++castleCount;
				}
			}
		}
		double coinsPerTown = 2000.0;
		double total = coinsPerTown * castleCount;
		this.getCiv().getTreasury().deposit(total);
		CivMessage.sendCiv(this.getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_neuschwanstein_generatedCoins", "§e" + total + "§a", CivSettings.CURRENCY_NAME, castleCount, "§b" + this.getTown().getName()));
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
	}

	@Override
	public void onPostBuild() {
		// TODO Автоматически созданная заглушка метода

	}

	@Override
	public void validCanProgressBuild() throws CivException {
		if (getTown().getMotherCiv() != null) {
			this.setNextProgressBuild(30); // 30 min notify.
			throw new CivException(CivSettings.localize.localizedString("var_buildAsync_wonderHaltedConquered", getTown().getCiv().getName()));
		}
		Buildable inProgress = getTown().BM.getBuildablePoolInProgress().get(0);
		if (inProgress != null && inProgress != this) {
			this.setNextProgressBuild(1);
			throw new CivException(CivSettings.localize.localizedString("var_buildAsync_wonderHaltedOtherConstruction", inProgress.getDisplayName()));
		}
		if (!getTown().isValid()) {
			this.setNextProgressBuild(10);
			throw new CivException(CivSettings.localize.localizedString("buildAsync_wonderHaltedNoTownHall"));
		}

		if (checkOtherWonderAlreadyBuilt()) {
			abortBuild();
			return; // wonder aborted via function above, no need to abort again.
		}
		if (isDestroyed()) {
			CivMessage.sendTown(getTown(), CivSettings.localize.localizedString("var_buildAsync_destroyed", getDisplayName()));
			abortBuild();
			return;
		}
		if (getTown().getMotherCiv() != null) {
			// Can't build wonder while we're conquered.
			// TODO continue;
		}
	}

	public boolean checkOtherWonderAlreadyBuilt() {
		if (isComplete()) return false; // We are completed, other wonders are not already built.
		return (!Wonder.isWonderAvailable(getConfigId()));
	}

	public void finished() {
		if (this.checkOtherWonderAlreadyBuilt()) {
			CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildAsync_wonderFarAway", getDisplayName()));

			// Refund the town half the cost of the wonder.
			double refund = (int) (getCost() / 2);
			getTown().depositDirect(refund);

			CivMessage.sendTown(getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildAsync_wonderRefund", refund, CivSettings.CURRENCY_NAME));
			abortBuild();
			return;
		}
		CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_completedWonder", CivColor.Red + getCiv().getName() + CivColor.RESET, "§6" + getTown().getName() + CivColor.RESET, "§a" + getDisplayName() + CivColor.RESET));
		super.finished();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof Wonder) {
			Wonder struct = (Wonder) obj;
			if (struct.getId() == this.getId()) return true;
		}
		return false;
	}

}
