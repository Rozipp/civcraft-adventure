package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigWonderBuff;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class Wonder extends Buildable {

	private ConfigWonderBuff wonderBuffs = null;

	public Wonder(String id, Town town) {
		super(id, town);
	}

	public void loadSettings() {
		wonderBuffs = CivSettings.wonderBuffs.get(this.getConfigId());

		if (this.isComplete() && this.isActive()) {
			this.addWonderBuffsToTown();
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, CivException {
		this.setId(rs.getInt("id"));
		this.setInfo(CivSettings.constructs.get(rs.getString("type_id")));
		this.setSQLOwner(CivGlobal.getTown(rs.getInt("town_id")));
		if (this.getTownOwner() == null) {
			// CivLog.warning("Coudln't find town ID:"+rs.getInt("town_id")+ " for wonder
			// "+this.getDisplayName()+" ID:"+this.getId());
			throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for wonder " + this.getDisplayName() + " ID:" + this.getId());
		}

		this.corner = new BlockCoord(rs.getString("cornerBlockHash"));
		this.setHitpoints(rs.getInt("hitpoints"));
		this.setTemplate(Template.getTemplate(rs.getString("template_name")));
		this.setComplete(rs.getBoolean("complete"));
		this.setHammersCompleted(rs.getInt("hammersCompleted"));

		this.getTownOwner().BM.addBuildable(this);

		this.startWonderOnLoad();

		if (!this.isComplete()) {
			try {
				this.startBuildTask();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<>();
		hashmap.put("type_id", this.getConfigId());
		hashmap.put("town_id", this.getTownOwner().getId());
		hashmap.put("complete", this.isComplete());
		hashmap.put("hammersCompleted", this.getHammersCompleted());
		hashmap.put("cornerBlockHash", this.getCorner().toString());
		hashmap.put("hitpoints", this.getHitpoints());
		hashmap.put("template_name", this.getTemplate().getFilepath());
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() {
		if (this.getTownOwner() != null) {
			if (this.wonderBuffs != null) {
				for (ConfigBuff buff : this.wonderBuffs.buffs) {
					this.getTownOwner().getBuffManager().removeBuff(buff.id);
				}
			}
		}
		super.delete();

		if (this.getTownOwner() != null) this.getTownOwner().BM.removeBuildable(this);
		CivGlobal.removeConstruct(this);

		try {
			SQL.deleteNamedObject(this, TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
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
	public void processUndo() {
		try {
			this.undoFromTemplate();
		} catch (CivException e1) {
			e1.printStackTrace();
			CivMessage.sendTown(getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("wonder_undo_error"));
			this.fancyDestroyConstructBlocks();
		}

		CivMessage.global(CivSettings.localize.localizedString("var_wonder_undo_broadcast", (CivColor.LightGreen + this.getDisplayName() + CivColor.White), this.getTownOwner().getName(), this.getTownOwner().getCiv().getName()));

		double refund = this.getCost();
		this.getTownOwner().depositDirect(refund);
		CivMessage.sendTown(getTownOwner(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTownOwner().getName(), refund, CivSettings.CURRENCY_NAME));

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
			CivMessage.global(CivSettings.localize.localizedString("var_wonder_destroyed", this.getDisplayName(), this.getTownOwner().getName()));
			this.deleteWithFancy();
		}
	}

	public void addWonderBuffsToTown() {

		if (this.wonderBuffs == null) return;
		for (ConfigBuff buff : this.wonderBuffs.buffs) {
			try {
				this.getTownOwner().getBuffManager().addBuff("wonder:" + this.getDisplayName() + ":" + this.getCorner() + ":" + buff.id, buff.id, this.getDisplayName());
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
		TaskMaster.syncTask(() -> {
			try {
				wonder.onLoad();
			} catch (Exception e) {
				CivLog.error(e.getMessage());
				e.printStackTrace();
			}
		}, 2000);
	}

	protected void addBuffToTown(Town town, String id) {
		try {
			town.getBuffManager().addBuff(id, id, this.getDisplayName() + " in " + this.getTownOwner().getName());
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
		for (Town t : this.getCivOwner().getTowns()) {
			cultureCount += t.getCultureChunks().size();
		}

		double coinsPerCulture = Double.parseDouble(CivSettings.buffs.get("buff_colossus_coins_from_culture").value);

		double total = coinsPerCulture * cultureCount;
		this.getCivOwner().getTreasury().deposit(total);

		CivMessage.sendCiv(this.getCivOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_colossus_generatedCoins", (CivColor.Yellow + total + CivColor.LightGreen), CivSettings.CURRENCY_NAME, cultureCount));
	}

	public void processCoinsFromColosseum() {
		int townCount = 0;
		for (Civilization civ : CivGlobal.getCivs()) {
			townCount += civ.getTownCount();
		}
		double coinsPerTown = Double.parseDouble(CivSettings.buffs.get("buff_colosseum_coins_from_towns").value);

		double total = coinsPerTown * townCount;
		this.getCivOwner().getTreasury().deposit(total);

		CivMessage.sendCiv(this.getCivOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_colosseum_generatedCoins", (CivColor.Yellow + total + CivColor.LightGreen), CivSettings.CURRENCY_NAME, townCount));
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
		this.getCivOwner().getTreasury().deposit(total);
		CivMessage.sendCiv(this.getCivOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_neuschwanstein_generatedCoins", "§e" + total + "§a", CivSettings.CURRENCY_NAME, castleCount, "§b" + this.getTownOwner().getName()));
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
		if (getTownOwner().getMotherCiv() != null) {
			this.setNextProgressBuild(30); // 30 min notify.
			throw new CivException(CivSettings.localize.localizedString("var_buildAsync_wonderHaltedConquered", getTownOwner().getCiv().getName()));
		}
		Buildable inProgress = getTownOwner().BM.getBuildablePoolInProgress().get(0);
		if (inProgress != null && inProgress != this) {
			this.setNextProgressBuild(1);
			throw new CivException(CivSettings.localize.localizedString("var_buildAsync_wonderHaltedOtherConstruction", inProgress.getDisplayName()));
		}
		if (!getTownOwner().isValid()) {
			this.setNextProgressBuild(10);
			throw new CivException(CivSettings.localize.localizedString("buildAsync_wonderHaltedNoTownHall"));
		}

		if (checkOtherWonderAlreadyBuilt()) {
			abortBuild();
			return; // wonder aborted via function above, no need to abort again.
		}
		if (isDestroyed()) {
			CivMessage.sendTown(getTownOwner(), CivSettings.localize.localizedString("var_buildAsync_destroyed", getDisplayName()));
			abortBuild();
			return;
		}
		if (getTownOwner().getMotherCiv() != null) {
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
			CivMessage.sendTown(getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("var_buildAsync_wonderFarAway", getDisplayName()));

			// Refund the town half the cost of the wonder.
			double refund = (int) (getCost() / 2);
			getTownOwner().depositDirect(refund);

			CivMessage.sendTown(getTownOwner(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildAsync_wonderRefund", refund, CivSettings.CURRENCY_NAME));
			abortBuild();
			return;
		}
		CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_completedWonder", CivColor.Red + getCivOwner().getName() + CivColor.RESET, "§6" + getTownOwner().getName() + CivColor.RESET, "§a" + getDisplayName() + CivColor.RESET));
		super.finished();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof Wonder) {
			Wonder struct = (Wonder) obj;
			return struct.getId() == this.getId();
		}
		return false;
	}

}
