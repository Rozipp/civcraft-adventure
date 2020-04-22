/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildTemplateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Structure extends Buildable {
	private int level = 1;

	public static String TABLE_NAME = "STRUCTURES";

	public Structure(Location center, String id, Town town) throws CivException {
		this.setInfo(CivSettings.structures.get(id));
		this.setSQLOwner(town);
		this.setCorner(new BlockCoord(center));
		this.setHitpoints(getInfo().max_hitpoints);

		// Disallow duplicate structures with the same hash.
		//		Structure struct = CivGlobal.getStructure(this.getCorner());
		//		if (struct != null) {
		//			throw new CivException(CivSettings.localize.localizedString("structure_alreadyExistsHere"));
		//		}
	}

	public Structure(ResultSet rs) throws SQLException, CivException {
		this.load(rs);
	}

	private static Structure newStructure(ResultSet rs, Location center, String id, Town town) throws CivException, SQLException {
		Structure struct;
		if (rs != null)
			id = rs.getString("type_id");
		String[] splitId = id.split("_");
		String name = "com.avrgaming.civcraft.structure.";
		int length = splitId.length;
		for (int i = 1; i < length; i++) {
			name = name + splitId[i].substring(0, 1).toUpperCase() + splitId[i].substring(1).toLowerCase();
		}
		try {
			Class<?> cls = null;
			cls = Class.forName(name);
			Constructor<?> cntr;
			if (rs == null) {
				Class<?> partypes[] = { Location.class, String.class, Town.class };
				cntr = cls.getConstructor(partypes);
				Object arglist[] = { center, id, town };
				struct = (Structure) cntr.newInstance(arglist);
			} else {
				Class<?> partypes[] = { ResultSet.class };
				cntr = cls.getConstructor(partypes);
				Object arglist[] = { rs };
				struct = (Structure) cntr.newInstance(arglist);
			}
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			CivLog.error("-----Structure class '" + name + "' creation error-----");
			e.printStackTrace();
			// This structure is generic, just create a structure type. 
			// TODO should ANY structure be generic?
			if (rs == null)
				struct = new Structure(center, id, town);
			else
				struct = new Structure(rs);
		}
		struct.loadSettings();
		return struct;
	}

	public static Structure newStructure(ResultSet rs) throws CivException, SQLException {
		return newStructure(rs, null, null, null);
	}

	public static Structure newStructure(Location center, String id, Town town) throws CivException {
		try {
			return newStructure(null, center, id, town);
		} catch (SQLException e) {
			throw new CivException("SQLException");
		}
	}

	public void loadSettings() {
		/* Build and register all of the components. */
		List<HashMap<String, String>> compInfoList = this.getComponentInfoList();
		if (compInfoList != null) {
			for (HashMap<String, String> compInfo : compInfoList) {
				String className = "com.avrgaming.civcraft.components." + compInfo.get("name");
				Class<?> someClass;
				try {
					someClass = Class.forName(className);
					Component compClass = (Component) someClass.newInstance();
					compClass.setName(compInfo.get("name"));
					for (String key : compInfo.keySet()) {
						compClass.setAttribute(key, compInfo.get(key));
					}
					compClass.createComponent(this, false);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		for (Component comp : this.attachedComponents) {
			comp.onSave();
		}
	}

	//--------------------- SQL DataBase
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" // 
					+ "`id` int(11) unsigned NOT NULL auto_increment," //
					+ "`type_id` mediumtext NOT NULL," //
					+ "`town_id` int(11) DEFAULT NULL," //
					+ "`complete` bool NOT NULL DEFAULT '0'," //
					+ "`builtBlockCount` int(11) DEFAULT NULL, " //
					+ "`cornerBlockHash` mediumtext DEFAULT NULL," //
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
	public void load(ResultSet rs) throws CivException, SQLException {
		this.setId(rs.getInt("id"));
		this.setInfo(CivSettings.structures.get(rs.getString("type_id")));
		this.setSQLOwner(CivGlobal.getTownFromId(rs.getInt("town_id")));

		if (this.getTown() == null) {
			this.delete();
			throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for structure " + this.getDisplayName() + " ID:" + this.getId());
		}
		this.setCorner(new BlockCoord(rs.getString("cornerBlockHash")));
		this.setHitpoints(rs.getInt("hitpoints"));

		this.setTemplate(Template.getTemplate(rs.getString("template_name")));

		this.setComplete(rs.getBoolean("complete"));
		this.setBlocksCompleted(rs.getInt("builtBlockCount"));
		this.getTown().addStructure(this);

		Structure struct = this;
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				try {
					struct.onLoad();
				} catch (Exception e) {
					CivLog.error(e.getMessage());
					e.printStackTrace();
				}
			}
		}, 2000);
		if (!this.isComplete()) {
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
		hashmap.put("builtBlockCount", this.getBlocksCompleted());
		hashmap.put("cornerBlockHash", this.getCorner().toString());
		hashmap.put("hitpoints", this.getHitpoints());
		hashmap.put("template_name", this.getTemplate().getFilepath());

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		super.delete();

		if (this.getTown() != null) {
			try {
				this.undoFromTemplate();
			} catch (IOException | CivException e1) {
				e1.printStackTrace();
				this.fancyDestroyConstructBlocks();
			}

			CivGlobal.removeStructure(this);
			this.getTown().removeStructure(this);
			this.unbindConstructBlocks();
		}
		this.setEnabled(false);
		SQL.deleteNamedObject(this, TABLE_NAME);
	}

	/** @deprecated */
	public void deleteSkipUndo() throws SQLException {
		super.delete();
		CivGlobal.removeStructure(this);
		this.getTown().removeStructure(this);
		this.unbindConstructBlocks();
		this.setEnabled(false);
		SQL.deleteNamedObject(this, TABLE_NAME);
	}

	//-------------------build
	@Override
	public void updateBuildProgess() {
		if (this.getId() != 0) {
			HashMap<String, Object> struct_hm = new HashMap<String, Object>();
			struct_hm.put("id", this.getId());
			struct_hm.put("type_id", this.getConfigId());
			struct_hm.put("complete", this.isComplete());
			struct_hm.put("builtBlockCount", this.savedBlockCount);

			SQL.updateNamedObjectAsync(this, struct_hm, TABLE_NAME);
		}
	}

	public void build(Player player) {
		// Before we place the blocks, give our build function a chance to work on it
		CivLog.debug("build log: structure.build ");
		try {
			this.runOnBuild(this.getCorner().getChunkCoord());
		} catch (CivException e1) {
			e1.printStackTrace();
		}

		// Setup undo information
		getTown().lastBuildableBuilt = this;
		Template tpl = this.getTemplate();
		try {
			tpl.saveUndoTemplate(this.getCorner().toString(), this.getCorner());
		} catch (CivException | IOException e) {
			e.printStackTrace();
		}
		tpl.buildScaffolding(this.getCorner());

		CivGlobal.getResident(player).undoPreview();
		this.startBuildTask();

		CivGlobal.addStructure(this);
		this.getTown().addStructure(this);
	}

	protected void runOnBuild(ChunkCoord cChunk) throws CivException {
		/* Override in children */
	}

	public void repairStructureForFree() throws CivException {
		setHitpoints(getMaxHitPoints());
		try {
			repairFromTemplate();
		} catch (CivException | IOException e) {
			throw new CivException(CivSettings.localize.localizedString("internalIOException"));
		}
		bindBlocks();
		save();
	}

	public void repairStructure() throws CivException {
		if (this instanceof Townhall)
			throw new CivException(CivSettings.localize.localizedString("structure_repair_notCaporHall"));
		double cost = getRepairCost();
		if (!getTown().getTreasury().hasEnough(cost))
			throw new CivException(CivSettings.localize.localizedString("var_structure_repair_tooPoor", getTown().getName(), cost, CivSettings.CURRENCY_NAME, getDisplayName()));
		repairStructureForFree();
		getTown().getTreasury().withdraw(cost);
		CivMessage.sendTown(getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_structure_repair_success", getTown().getName(), getDisplayName(), getCorner()));
	}

	@Override
	public void processUndo() throws CivException {
		if (isTownHall()) {
			throw new CivException(CivSettings.localize.localizedString("structure_move_notCaporHall"));
		}
		try {
			delete();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}
		CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_structure_undo_success", getDisplayName()));
		double refund = this.getCost();
		this.getTown().depositDirect(refund);
		CivMessage.sendTown(getTown(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTown().getName(), refund, CivSettings.CURRENCY_NAME));
	}

	// --------------- structure const
	@Override
	@Deprecated
	public String getName() {
		return this.getDisplayName();
	}

	public double getRepairCost() {
		return (int) this.getCost() / 2;
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "bighouse";
	}

	//-------------- Override in children
	@Override
	public void onCheckBlockPAR() throws CivException {
		/* Override in children */
	}

	public void updateSignText() {
		/* Override in children */
	}

	public void onBonusGoodieUpdate() {
		/* Override in children */
	}

	public void onMarkerPlacement(Player player, Location next, ArrayList<Location> locs) throws CivException {
		/* Override in children */
	}

	@Override
	public void onComplete() {
		/* Override in children */
	}

	@Override
	public void onLoad() throws CivException {
		/* Override in children */
	}

	@Override
	public void onUnload() {
		/* Override in children */
	}

	@Override
	public void setTurretLocation(BlockCoord absCoord) {
		/* Override in children */
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		/* Override in children */
	}

	

	
}
