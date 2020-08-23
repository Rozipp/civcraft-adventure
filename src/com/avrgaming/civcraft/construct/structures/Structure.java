package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;
import lombok.Getter;
import lombok.Setter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

@Getter
@Setter
public class Structure extends Buildable {

	public Structure(String id, Town town) {
		super(id, town);
	}

	@Override
	public void load(ResultSet rs) throws CivException, SQLException {
		this.setId(rs.getInt("id"));
		this.setInfo(CivSettings.constructs.get(rs.getString("type_id")));
		this.setSQLOwner(CivGlobal.getTown(rs.getInt("town_id")));

		this.corner = new BlockCoord(rs.getString("cornerBlockHash"));
		this.setHitpoints(rs.getInt("hitpoints"));

		this.setTemplate(Template.getTemplate(rs.getString("template_name")));

		if (this.getTownOwner() == null) {
			this.deleteWithUndo();
			throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for structure " + this.getDisplayName() + " ID:" + this.getId());
		}

		this.setComplete(rs.getBoolean("complete"));
		this.setHammersCompleted(rs.getInt("hammersCompleted"));
		this.getTownOwner().BM.addBuildable(this);

		if (!this.isComplete()) {
			try {
				this.startBuildTask();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
			TaskMaster.syncTask(() -> {
				try {
					this.onLoad();
				} catch (Exception e) {
					CivLog.error(e.getMessage());
					e.printStackTrace();
				}
			}, 2000);
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
		super.delete();

		if (this.getTownOwner() != null) this.getTownOwner().BM.removeBuildable(this);
		CivGlobal.removeConstruct(this);

		try {
			SQL.deleteNamedObject(this, TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// -------------------build

	public void runOnBuild(ChunkCoord cChunk) throws CivException {
		/* Override in children */
	}

	public void repairStructureForFree() {
		setHitpoints(getMaxHitPoints());
		repairFromTemplate();
		save();
	}

	public void repairStructure() throws CivException {
		if (this instanceof Cityhall) throw new CivException(CivSettings.localize.localizedString("structure_repair_notCaporHall"));
		double cost = getRepairCost();
		if (!getTownOwner().getTreasury().hasEnough(cost)) throw new CivException(CivSettings.localize.localizedString("var_structure_repair_tooPoor", getTownOwner().getName(), cost, CivSettings.CURRENCY_NAME, getDisplayName()));
		repairStructureForFree();
		getTownOwner().getTreasury().withdraw(cost);
		CivMessage.sendTown(getTownOwner(), CivColor.Yellow + CivSettings.localize.localizedString("var_structure_repair_success", getTownOwner().getName(), getDisplayName(), getCorner()));
	}

	@Override
	public void processUndo() throws CivException {
		if (this instanceof Cityhall) throw new CivException(CivSettings.localize.localizedString("structure_move_notCaporHall"));
		deleteWithUndo();

		CivMessage.sendTown(getTownOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_structure_undo_success", getDisplayName()));
		double refund = this.getCost();
		this.getTownOwner().depositDirect(refund);
		CivMessage.sendTown(getTownOwner(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTownOwner().getName(), refund, CivSettings.CURRENCY_NAME));
	}

	// --------------- structure const

	@Override
	@Deprecated
	public String getName() {
		return this.getDisplayName();
	}

	public double getRepairCost() {
		return (int) this.getCost() *0.5;
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "bighouse";
	}

	@Override
	public boolean isActive() {
		return super.isActive();
	}
	
	public boolean isWork() {
		return super.isActive() && (getProfesionalComponent() == null || getProfesionalComponent().isWork);
	}
	
	@Override
	public void onComplete() {
		/* Override in children */
	}

	@Override
	public void onLoad() {
		/* Override in children */
	}

	@Override
	public void onUnload() {
		/* Override in children */
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		/* Override in children */
	}

	@Override
	public void onPostBuild() {
		/* Override in children */
	}

	@Override
	public void validCanProgressBuild() throws CivException {
		if (!getTownOwner().isValid()) {
			this.setNextProgressBuild(10);
			throw new CivException("Город " + getTownOwner().getName() + " неактивен");
		}
	}

	public void finished() {
		CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_completed", getTownOwner().getName(), "§2" + getDisplayName() + CivColor.RESET));
		super.finished();
	}
}
