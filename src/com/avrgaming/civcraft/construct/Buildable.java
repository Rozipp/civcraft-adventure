package com.avrgaming.civcraft.construct;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.InvalidObjectException;
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
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.util.SimpleBlock.Type;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class Buildable extends Construct {

	private int hammersCompleted = 0;
	private long nextProgressBuild = Long.MAX_VALUE;

	private boolean complete = false;
	private boolean enabled = true;

	public Buildable(ResultSet rs) throws SQLException, CivException {
		super(rs.getString("type_id"), CivGlobal.getTown(rs.getInt("town_id")));
		try {
			this.load(rs);
		} catch (InvalidNameException | InvalidObjectException e) {
			e.printStackTrace();
		}
		this.bindBlocks();
	}

	public Buildable(String id, Town town) throws CivException {
		super(id, town);
	}

	public void startBuildTask() {
		this.getTown().BM.addBuildableInprogress(this);
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		// Make sure we are building this building inside of culture.
		if (!(this instanceof Cityhall)) {
			for (ChunkCoord chunkCoord : getChunksCoords()) {
				CultureChunk cc = CivGlobal.getCultureChunk(chunkCoord);
				if (cc == null || cc.getTown().getCiv() != this.getCiv()) throw new CivException(CivSettings.localize.localizedString("buildable_notInCulture"));
			}
		}

		if (getTown() != null && this.isTileImprovement()) {
			Integer maxTileImprovements = getTown().getMaxTileImprovements();
			if (getTown().getTileImprovementCount() >= maxTileImprovements) throw new CivException(CivSettings.localize.localizedString("buildable_errorTILimit"));
		}
		super.checkBlockPermissionsAndRestrictions(player);
	}

	// ------------- Build Task
	public abstract void validCanProgressBuild() throws CivException;

	public void abortBuild() {
		// Remove build task from town..
		getTown().BM.removeBuildableInprogress(this);
		// remove wonder from town.
		deleteWithUndo();
	}

	@Override
	public void build(Player player) throws CivException {
		Structure struct = (this instanceof Structure) ? (Structure) this : null;
		Wonder wonder = (this instanceof Wonder) ? (Wonder) this : null;

		getTown().BM.checkIsTownCanBuildBuildable(this);
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
			if (tc == null) TownChunk.autoClaim(getTown(), cc);
		}

		try {
			if (struct != null) struct.runOnBuild(corner.getChunkCoord());
		} catch (CivException e1) {
			e1.printStackTrace();
		}
		this.startBuildTask();
		this.save();

		getTown().getTreasury().withdraw(this.getCost());

		if (struct != null) {
			CivMessage.sendTown(getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_town_buildStructure_success", this.getDisplayName()));
			getTown().BM.addStructure(struct);
		}
		if (wonder != null) {
			getTown().BM.addWonder(wonder);
			CivMessage.sendTown(getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_town_buildwonder_success", this.getDisplayName(), player.getName(), getTown().getName()));
			CivMessage.global(CivSettings.localize.localizedString("var_wonder_startedByCiv", getCiv().getName(), this.getDisplayName(), getTown().getName(), player.getName()));
		}
		getTown().save();
	}

	public int getHammerCost() {
		return getInfo().hammer_cost;
	}

	public int getTotalBlock() {
		return this.getTemplate().getTotalBlocks();
		// return this.getTemplate().size_x * this.getTemplate().size_y * this.getTemplate().size_z;
	};

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
			Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
			for (int count = completedBlock; count < endBlock; count++) {
				SimpleBlock sbnext = this.getTemplate().getNextBlockBuild(count);
				if (sbnext == null) throw new CivException("В темплатете не нашел блок под номером " + count + ". Всего блоков " + getTotalBlock());
				SimpleBlock sb = new SimpleBlock(getCorner(), sbnext);

				if (!Template.isAttachable(sb.getMaterial())) sbs.add(sb);
				if (sb.getType() != CivData.AIR && sb.specialType != Type.COMMAND) addConstructBlock(new BlockCoord(sb), sb.y != 0);
			}

			// Add all of the blocks from this tick to the sync task.
			if (getTown().BM.isBuildableInprogress(this)) {
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
						CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_progressWonder", getDisplayName(), getTown().getName(), nextPercentComplete, getCiv().getName()));
					else
						CivMessage.sendTown(getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildAsync_progressOther", getDisplayName(), nextPercentComplete));
				}
			}
		} catch (Exception e) {
			CivLog.exception("BuildAsyncTask town:" + getTown() + " struct:" + getDisplayName() + " template:" + getTemplate().filepath, e);
		}
	}

	public void finished() {
		setComplete(true);
		updateBuildProgess();
		save();
		getTown().BM.removeBuildableInprogress(this);
		getTown().save();

		Template tpl = this.getTemplate();
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				tpl.buildTemplate(getCorner());
				tpl.buildAirBlocks(getCorner());
			}
		});
		
		postBuildSyncTask();
		onComplete();
		return;
	}

	public void setNextProgressBuild(int minute) {
		this.nextProgressBuild = System.currentTimeMillis() + TimeTools.minuteToMinisec(minute);
	}

	public boolean isNextProgressBuild() {
		return this.nextProgressBuild > System.currentTimeMillis();
	}

	// ------------ abstract metods
	public abstract void processUndo() throws CivException;

	public abstract void updateBuildProgess();

	public abstract void onComplete();

	public void onTechUpdate() {
	}

	public boolean showOnDynmap() {
		return true;
	}

	public void onDemolish() throws CivException {
	}

	public void onGoodieFrame() { // Override children
	}

	/* SessionDB helpers */
	@Override
	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDatabase().add(key, value, this.getCiv().getId(), this.getTown().getId(), this.getId());
	}

	// --------------- Damage
	@Override
	public void onDamage(int amount, Player player, ConstructDamageBlock hit) {
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
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
		regenRate += this.getTown().getBuffManager().getEffectiveInt("buff_chichen_itza_regen_rate");
		regenRate += this.getTown().getBuffManager().getEffectiveInt("buff_statue_of_zeus_struct_regen");
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
		if (!this.valid) this.getTown().BM.addInvalideBuildable(this);
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
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupport", this.getDisplayName(), (centerLoc.getBlockX() + "," + centerLoc.getBlockY() + "," + centerLoc.getBlockZ())));
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupportDamage", df.format(invalid_hourly_penalty * 100), (this.getHitpoints() + "/" + this.getMaxHitPoints())));
		CivMessage.sendTown(this.getTown(), CivColor.Rose + this.invalidLayerMessage);
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("buildable_validationPrompt"));
		this.save();
	}

}
