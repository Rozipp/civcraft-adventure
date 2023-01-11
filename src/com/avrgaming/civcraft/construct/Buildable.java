package com.avrgaming.civcraft.construct;

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.components.ProfesionalComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.titles.Title;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.*;
import com.avrgaming.civcraft.util.SimpleBlock.SimpleType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

@Setter
@Getter
public abstract class Buildable extends Construct {

	private int hammersCompleted = 0;
	private long nextProgressBuild = Long.MAX_VALUE;

	private boolean complete = false;
	private boolean enabled = true;
	private HashMap<String, String> variables = new HashMap<>();

	// --------------------- SQL DataBase
	public static String TABLE_NAME = "BUILDABLE";

	public Buildable(String id, Town town) {
		super(id, town);
	}

	public static Buildable _newBuildable(String id, Town town) {
		ConfigConstructInfo cci = CivSettings.constructs.get(id);
		String className = cci.getClassName();
		Buildable buildable = null;
		try {
			Class<?>[] parTypes = {String.class, Town.class};
			Constructor<?> constructor = Class.forName(className).getConstructor(parTypes);
			Object[] argList = {id, town};
			buildable = (Buildable) constructor.newInstance(argList);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			CivLog.error("-----Structure class '" + className + "' creation error-----");
			e.printStackTrace();
			if (cci.type == ConfigConstructInfo.ConstructType.Structure) buildable = new Structure(id, town);
			else if (cci.type == ConfigConstructInfo.ConstructType.Title) buildable = new Title(id, town);
			else if (cci.type == ConfigConstructInfo.ConstructType.Wonder) buildable = new Wonder(id, town);
		}
		return buildable;
	}

	public static Buildable newBuildable(ResultSet rs) throws CivException, SQLException {
		String id = rs.getString("type_id");
		Town town = CivGlobal.getTown(rs.getInt("town_id"));
		Buildable buildable = _newBuildable(id, town);
		buildable.load(rs);
		buildable.postBuild();
		return buildable;
	}

	public static Buildable newBuildable(Player player, Location location, String id, Town town, boolean checkPerm) throws CivException {
		Buildable buildable = _newBuildable(id, town);
		buildable.initDefaultTemplate(location);
		if (checkPerm) {
			if (town != null) town.BM.checkIsTownCanBuildBuildable(buildable);
			buildable.checkBlockPermissionsAndRestrictions(player);
		}
		return buildable;
	}

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" //
					+ "`id` int(11) unsigned NOT NULL auto_increment," //
					+ "`type_id` mediumtext NOT NULL," //
					+ "`town_id` int(11) DEFAULT NULL," //
					+ "`complete` bool NOT NULL DEFAULT '0'," //
					+ "`hammersCompleted` int(11) DEFAULT NULL, " //
					+ "`cornerBlockHash` mediumtext DEFAULT NULL," //
					+ "`template_name` mediumtext DEFAULT NULL, " //
					+ "`hitpoints` int(11) DEFAULT '100'," //
					+ "`variables` mediumtext DEFAULT NULL, " //
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
		this.setInfo(CivSettings.constructs.get(rs.getString("type_id")));
		this.setSQLOwner(CivGlobal.getTown(rs.getInt("town_id")));
		if (this.getTownOwner() == null) {
			this.deleteWithUndo();
			throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for structure " + this.getDisplayName() + " ID:" + this.getId());
		}
		this.corner = new BlockCoord(rs.getString("cornerBlockHash"));
		this.setHitpoints(rs.getInt("hitpoints"));
		this.setTemplate(Template.getTemplate(rs.getString("template_name")));
		this.setComplete(rs.getBoolean("complete"));
		this.setHammersCompleted(rs.getInt("hammersCompleted"));
		String variablesMap = rs.getString("variables");
		variables = new HashMap<>();
		if (variablesMap != null && !variables.isEmpty())
			for (String ss : variablesMap.split(",")) {
				String[] s = ss.split("=");
				variables.put(s[0], s[1]);
			}
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
		hashmap.put("variables", this.variables.toString().replace("{", "").replace("}", "").replace(" ", ""));
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

	public void startBuildTask() {
		this.getTownOwner().BM.addBuildableInprogress(this);
	}

	@Override
	public String getName() {
		return this.getDisplayName();
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		// Make sure we are building this building inside of culture.
		if (!(this instanceof Cityhall)) {
			for (ChunkCoord chunkCoord : getChunksCoords()) {
				CultureChunk cc = CivGlobal.getCultureChunk(chunkCoord);
				if (cc == null || cc.getTown().getCiv() != this.getCivOwner()) throw new CivException(CivSettings.localize.localizedString("buildable_notInCulture"));
			}
		}

		if (getTownOwner() != null && this.isTileImprovement()) {
			int maxTileImprovements = getTownOwner().getMaxTileImprovements();
			if (getTownOwner().getTileImprovementCount() >= maxTileImprovements) throw new CivException(CivSettings.localize.localizedString("buildable_errorTILimit"));
		}
		if (getInfo().require_floor==null || getInfo().require_floor.isEmpty()) super.checkBlockPermissionsAndRestrictions(player);
	}

	// ------------- Build Task
	public abstract void validCanProgressBuild() throws CivException;

	public void abortBuild() {
		// Remove build task from town..
		getTownOwner().BM.removeBuildableInprogress(this);
		// remove wonder from town.
		deleteWithUndo();
	}

	@Override
	public void build(Player player) throws CivException {
		Structure struct = (this instanceof Structure) ? (Structure) this : null;
		Wonder wonder = (this instanceof Wonder) ? (Wonder) this : null;

		getTownOwner().BM.checkIsTownCanBuildBuildable(this);
		this.checkBlockPermissionsAndRestrictions(player);

		try {
			getTemplate().saveUndoTemplate(corner.toString(), corner);
		} catch (IOException e) {
			e.printStackTrace();
		}
		CivGlobal.getResident(player).undoPreview();
		this.getTemplate().buildScaffolding(corner);

		for (ChunkCoord cc : this.getChunksCoords()) {
			TownChunk tc = CivGlobal.getTownChunk(cc);
			if (tc == null) TownChunk.autoClaim(getTownOwner(), cc);
		}

		try {
			if (struct != null) struct.runOnBuild(corner.getChunkCoord());
		} catch (CivException e1) {
			e1.printStackTrace();
		}
		this.startBuildTask();
		this.save();

		getTownOwner().getTreasury().withdraw(this.getCost());

		getTownOwner().BM.addBuildable(this);
		if (struct != null) {
			CivMessage.sendTown(getTownOwner(), CivColor.Yellow + CivSettings.localize.localizedString("var_town_buildStructure_success", this.getDisplayName()));
		}
		if (wonder != null) {
			CivMessage.sendTown(getTownOwner(), CivColor.Yellow + CivSettings.localize.localizedString("var_town_buildwonder_success", this.getDisplayName(), player.getName(), getTownOwner().getName()));
			CivMessage.global(CivSettings.localize.localizedString("var_wonder_startedByCiv", getCivOwner().getName(), this.getDisplayName(), getTownOwner().getName(), player.getName()));
		}
		getTownOwner().save();
	}

	public int getHammerCost() {
		return getInfo().hammer_cost;
	}

	public int getTotalBlock() {
		return this.getTemplate().getTotalBlocks();
		// return this.getTemplate().size_x * this.getTemplate().size_y * this.getTemplate().size_z;
	}

	public int converHammerToBlock(int hammer) {
		return getTotalBlock() * hammer / getHammerCost();
	}

	public int getNeadHammersToComplit() {
		return getHammerCost() - getHammersCompleted();
	}

	private int percent_complete = 0;

	public void progressBuild(int depositHammer) {
		int completedBlock = converHammerToBlock(getHammersCompleted());
		hammersCompleted += depositHammer;
		int endBlock = converHammerToBlock(getHammersCompleted());
		updateBuildProgess();
		if (completedBlock == endBlock) return;
		if (endBlock >= getTotalBlock()) endBlock = getTotalBlock();

		try {
			Queue<SimpleBlock> sbs = new LinkedList<>();
			for (int count = completedBlock; count < endBlock; count++) {
				SimpleBlock sbnext = this.getTemplate().getNextBlockBuild(count);
				if (sbnext == null) throw new CivException("В темплатете не нашел блок под номером " + count + ". Всего блоков " + getTotalBlock());
				SimpleBlock sb = new SimpleBlock(getCorner(), sbnext);

				if (!Template.isAttachable(sb.getMaterial())) sbs.add(sb);
				if (sb.getType() != CivData.AIR && sb.specialType != SimpleType.COMMAND) addConstructBlock(new BlockCoord(sb), sb.y != 0);
			}

			// Add all of the blocks from this tick to the sync task.
			if (getTownOwner().BM.isBuildableInprogress(this)) {
				SyncBuildUpdateTask.queueSimpleBlock(sbs);
				sbs.clear();
			} else {
				abortBuild();
				return;
			}

			if (getHammersCompleted() >= getHammerCost()) {
				finished();
				return;
			}

			int nextPercentComplete = Math.floorDiv(getHammersCompleted() * 100, getHammerCost());
			if (nextPercentComplete > this.percent_complete) {
				this.percent_complete = nextPercentComplete;
				if ((this.percent_complete / 10) != (nextPercentComplete / 10)) {
					if (this instanceof Wonder)
						CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_progressWonder", getDisplayName(), getTownOwner().getName(), nextPercentComplete, getCivOwner().getName()));
					else
						CivMessage.sendTown(getTownOwner(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildAsync_progressOther", getDisplayName(), nextPercentComplete));
				}
			}
		} catch (Exception e) {
			CivLog.exception("BuildAsyncTask town:" + getTownOwner() + " struct:" + getDisplayName() + " template:" + getTemplate().filepath, e);
		}
	}

	public void finished() {
		setComplete(true);
		setEnabled(true);
		updateBuildProgess();
		save();
		getTownOwner().BM.removeBuildableInprogress(this);
		getTownOwner().save();

		TaskMaster.syncTask(() -> {
			this.getTemplate().buildTemplate(getCorner());
			this.getTemplate().buildAirBlocks(getCorner());
		});

		postBuild();
		onComplete();
	}

	public void setNextProgressBuild(int minute) {
		this.nextProgressBuild = System.currentTimeMillis() + TimeTools.minuteToMinisec(minute);
	}

	public boolean isNextProgressBuild() {
		return this.nextProgressBuild > System.currentTimeMillis();
	}

	// ------------ abstract metods
	@Override
	public void processUndo() throws CivException {
	}

	public void updateBuildProgess(){
		if (this.getId() != 0) {
			HashMap<String, Object> struct_hm = new HashMap<>();
			struct_hm.put("id", this.getId());
			struct_hm.put("type_id", this.getConfigId());
			struct_hm.put("complete", this.isComplete());
			struct_hm.put("hammersCompleted", this.getHammersCompleted());
			SQL.updateNamedObjectAsync(this, struct_hm, TABLE_NAME);
		}
	}

	public abstract void onComplete();

	public void onTechUpdate() {
	}

	public boolean showOnDynmap() {
		return true;
	}

	/* SessionDB helpers */
	@Override
	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDatabase().add(key, value, this.getCivOwner().getId(), this.getTownOwner().getId(), this.getId());
	}

	// --------------- Damage
	@Override
	public void onDamage(int amount, Player player, ConstructDamageBlock hit) {
		if (!this.getCivOwner().getDiplomacyManager().isAtWar()) {
			return;
		}
		boolean wasTenPercent = false;
		if (hit.getOwner().isDestroyed()) {
			if (player != null) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_alreadyDestroyed", hit.getOwner().getDisplayName()));
			}
			return;
		}

		Construct constrOwner = hit.getOwner();
		if (!((constrOwner instanceof Buildable) && ((Buildable) constrOwner).isComplete() || (hit.getOwner() instanceof Wonder))) {
			if (player != null) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_underConstruction", hit.getOwner().getDisplayName()));
			}
			return;
		}
		// if (this instanceof Neuschwanstein && player != null) {
		// if (this.getTown().hasStructure("s_capitol")) {
		// final Capitol capitol = (Capitol) this.getTown().getStructureByType("s_capitol");
		// boolean allDestroyed = true;
		// for (final ControlPoint c : capitol.controlPoints.values()) {
		// if (c.getInfo().equalsIgnoreCase("Neuschwanstein") && !c.isDestroyed()) {
		// allDestroyed = false;
		// break;
		// }
		// }
		// if (!allDestroyed) {
		// CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_cannotAttackNeu", this.getTown().getName()));
		// return;
		// }
		// } else {
		// final Townhall townHall = (Townhall) this.getTown().getStructureByType("s_townhall");
		// boolean allDestroyed = true;
		// for (final ControlPoint c : townHall.controlPoints.values()) {
		// if (c.getInfo().equalsIgnoreCase("Neuschwanstein") && !c.isDestroyed()) {
		// allDestroyed = false;
		// break;
		// }
		// }
		// if (!allDestroyed) {
		// CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_cannotAttackNeu", this.getTown().getName()));
		// return;
		// }
		// }
		// }
		if ((hit.getOwner().getDamagePercentage() % 10) == 0) {
			wasTenPercent = true;
		}

		this.damage(amount);

		hit.getWorld().playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
		hit.getWorld().playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

		if ((hit.getOwner().getDamagePercentage() % 10) == 0 && !wasTenPercent) {
			if (player != null) {
				onDamageNotification(player, hit);
			}
		}

		if (player != null) {
			Resident resident = CivGlobal.getResident(player);
			if (resident.isCombatInfo()) {
				CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(), (hit.getOwner().getHitpoints() + "/" + hit.getOwner().getMaxHitPoints())));
			}
		}
	}

	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
		CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(), (hit.getOwner().getDamagePercentage() + "%")));

		CivMessage.sendTown(hit.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildable_underAttackAlert", hit.getOwner().getDisplayName(), hit.getOwner().getCorner(), hit.getOwner().getDamagePercentage()));
	}

	@Override
	public void processRegen() {
		if (this.validated && !this.isValid()) {
			/* Do not regen invalid structures. */
			return;
		}

		int regenRate = this.getRegenRate();
		regenRate += this.getTownOwner().getBuffManager().getEffectiveInt("buff_chichen_itza_regen_rate");
		regenRate += this.getTownOwner().getBuffManager().getEffectiveInt("buff_statue_of_zeus_struct_regen");
		if (regenRate != 0) {
			if ((this.getHitpoints() != this.getMaxHitPoints()) && (this.getHitpoints() != 0)) {
				this.setHitpoints(this.getHitpoints() + regenRate);
				if (this.getHitpoints() > this.getMaxHitPoints()) this.setHitpoints(this.getMaxHitPoints());
			}
		}
	}

	@Override
	public void setValid(boolean b) {
		this.valid = this.isPartOfAdminCiv() || b;
		if (!this.valid) this.getTownOwner().BM.addInvalideBuildable(this);
	}

	@Override
	public boolean isValid() {
		return isPartOfAdminCiv() || valid;
	}

	@Override
	public boolean isActive() {
		return this.isComplete() && (this instanceof Cityhall || super.isActive());
	}

	public boolean isCanRestoreFromTemplate() {
		return true;
	}

	public void onInvalidPunish() {
		Location centerLoc = this.getCenterLocation();
		double invalid_hourly_penalty;
		try {
			invalid_hourly_penalty = CivSettings.getDouble(CivSettings.warConfig, "war.invalid_hourly_penalty");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		int damage = (int) (this.getMaxHitPoints() * invalid_hourly_penalty);
		if (damage <= 0) damage = 10;

		this.damage(damage);

		DecimalFormat df = new DecimalFormat("###");
		CivMessage.sendTown(this.getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupport", this.getDisplayName(), (centerLoc.getBlockX() + "," + centerLoc.getBlockY() + "," + centerLoc.getBlockZ())));
		CivMessage.sendTown(this.getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupportDamage", df.format(invalid_hourly_penalty * 100), (this.getHitpoints() + "/" + this.getMaxHitPoints())));
		CivMessage.sendTown(this.getTownOwner(), CivColor.Rose + this.invalidLayerMessage);
		CivMessage.sendTown(this.getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("buildable_validationPrompt"));
		this.save();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof Buildable) {
			Buildable buildable = (Buildable) obj;
			return buildable.getId() == this.getId() || buildable.getCorner() == this.getCorner();
		}
		return false;
	}

	private ProfesionalComponent profesionalComponent;
	private NonMemberFeeComponent nonMemberFeeComponent;
	private ConsumeLevelComponent consumeLevelComponent = null;
	public ProfesionalComponent getProfesionalComponent() {
		if (profesionalComponent == null) {
			Component comp = getComponent("ProfesionalComponent");
			if (comp != null) profesionalComponent = (ProfesionalComponent) comp;
		}
		return profesionalComponent;
	}
	public NonMemberFeeComponent getNonMemberFeeComponent() {
		if (nonMemberFeeComponent == null) {
			Component comp = getComponent("NonMemberFeeComponent");
			if (comp != null) nonMemberFeeComponent = (NonMemberFeeComponent) comp;
		}
		return nonMemberFeeComponent;
	}
	public ConsumeLevelComponent getConsumeComponent() {
		if (consumeLevelComponent == null) {
			Component comp = getComponent("ConsumeLevelComponent");
			consumeLevelComponent = (ConsumeLevelComponent) comp;
		}
		return consumeLevelComponent;
	}
}
