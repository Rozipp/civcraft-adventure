package com.avrgaming.civcraft.structure;

import java.text.DecimalFormat;
import java.util.LinkedList;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.wonders.Neuschwanstein;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.TimeTools;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class Buildable extends Construct {
	protected BlockCoord mobSpawnerCoord;

	public int blocksCompleted = 0;
	public int savedBlockCount = 0;

	private boolean complete = false;
	private boolean enabled = true;

	public Structure replaceStructure;

	// private String invalidReason = "";

	protected void startBuildTask() {
		if (this instanceof Structure)
			this.getTown().setCurrentStructureInProgress(this);
		else
			this.getTown().setCurrentWonderInProgress(this);
		BuildAsyncTask task = new BuildAsyncTask(this);
		this.getTown().build_tasks.add(task);
		TaskMaster.asyncTask(task, TimeTools.toTicks(5));
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {

		Block centerBlock = this.getCorner().getBlock();
		int regionX = this.getTemplate().getSize_x();
		int regionY = this.getTemplate().getSize_y();
		int regionZ = this.getTemplate().getSize_z();

		boolean foundTradeGood = false;
		TradeOutpost tradeOutpost = null;
		boolean ignoreBorders = this.isAllowOutsideTown();
		if (this instanceof TradeOutpost) tradeOutpost = (TradeOutpost) this;

		// Make sure we are building this building inside of culture.
		if (this.isTownHall()) {
			/* Structure is a town hall, auto-claim the borders. */
			ignoreBorders = true;
		} else {
			CultureChunk cc = CivGlobal.getCultureChunk(centerBlock.getLocation());
			if (cc == null || cc.getTown().getCiv() != this.getCiv()) {
				throw new CivException(CivSettings.localize.localizedString("buildable_notInCulture"));
			}
		}

		// FIXME Перененсти в водные постройки
		if (this.getConfigId().equals("s_shipyard") || this.getConfigId().equals("s_arrowship") || this.getConfigId().equals("s_scoutship") || this.getConfigId().equals("s_cannonship") || this.getConfigId().equals("ti_tradeship")
				|| this.getConfigId().equals("w_grand_ship_ingermanland")) {
			if (!centerBlock.getBiome().equals(Biome.OCEAN) && !centerBlock.getBiome().equals(Biome.BEACHES) && !centerBlock.getBiome().equals(Biome.STONE_BEACH) && !centerBlock.getBiome().equals(Biome.COLD_BEACH)
					&& !centerBlock.getBiome().equals(Biome.DEEP_OCEAN) && !centerBlock.getBiome().equals(Biome.RIVER) && !centerBlock.getBiome().equals(Biome.FROZEN_OCEAN) && !centerBlock.getBiome().equals(Biome.FROZEN_RIVER)) {
				throw new CivException(CivSettings.localize.localizedString("var_buildable_notEnoughWater", this.getDisplayName()));
			}
		}

		Structure struct = CivGlobal.getStructure(new BlockCoord(centerBlock));
		if (struct != null) throw new CivException(CivSettings.localize.localizedString("buildable_structureExistsHere"));
		if (!player.isOp()) BuildableStatic.validateDistanceFromSpawn(centerBlock.getLocation());

		if (getTown() != null && this.isTileImprovement()) {
			ignoreBorders = true;
			ConfigTownLevel level = CivSettings.townLevels.get(getTown().getLevel());

			Integer maxTileImprovements = level.tile_improvements;
			if (getTown().getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) maxTileImprovements *= 2;
			if (getTown().getTileImprovementCount() >= maxTileImprovements) throw new CivException(CivSettings.localize.localizedString("buildable_errorTILimit"));

			ChunkCoord coord = new ChunkCoord(centerBlock.getLocation());
			for (Structure s : getTown().getStructures()) {
				if (!s.isTileImprovement()) continue;
				ChunkCoord sCoord = new ChunkCoord(s.getCorner());
				if (sCoord.equals(coord)) {
					throw new CivException(CivSettings.localize.localizedString("buildable_errorTIHere"));
				}
			}

		}

		if (centerBlock.getLocation().getY() >= 255) throw new CivException(CivSettings.localize.localizedString("buildable_errorTooHigh"));
		if (centerBlock.getLocation().getY() <= 7) throw new CivException(CivSettings.localize.localizedString("buildable_errorTooLow"));
		if (centerBlock.getLocation().getY() < CivGlobal.minBuildHeight) throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) throw new CivException(CivSettings.localize.localizedString("buildable_errorHeightLimit"));

		LinkedList<RoadBlock> deletedRoadBlocks = new LinkedList<RoadBlock>();

		for (int x = 0; x < regionX; x++) {
			for (int y = 0; y < regionY; y++) {
				for (int z = 0; z < regionZ; z++) {
					Block b = centerBlock.getRelative(x, y, z);

					if (ItemManager.getTypeId(b) == CivData.CHEST) throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));

					TownChunk tc = CivGlobal.getTownChunk(b.getLocation());
					if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
						// Make sure we have permission to destroy any block in this area.
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_needPermissions") + " " + b.getX() + "," + b.getY() + "," + b.getZ());
					}

					BlockCoord coord = new BlockCoord(b);
					ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());

					if (tradeOutpost == null) {
						// not building a trade outpost, prevent protected blocks from being destroyed.
						ProtectedBlock pb = CivGlobal.getProtectedBlock(coord);
						if (pb != null) {}
					} else {
						if (CivGlobal.getTradeGood(coord) != null) {
							// Make sure we encompass entire trade good.
							if ((y + 3) < regionY) {
								foundTradeGood = true;
								tradeOutpost.setTradeGoodCoord(coord);
							}
						}
					}

					if (CivGlobal.getConstructBlock(coord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
					if (CivGlobal.getFarmChunk(new ChunkCoord(coord.getLocation())) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
					if (CivGlobal.getWallChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));
					if (CivGlobal.getConstructFromChunk(coord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));

					RoadBlock rb = CivGlobal.getRoadBlock(coord);
					if (rb != null) {
						deletedRoadBlocks.add(rb);
					}

					BorderData border = Config.Border(b.getWorld().getName());
					if (border != null) {
						if (!border.insideBorder(b.getLocation().getX(), b.getLocation().getZ(), Config.ShapeRound())) {
							throw new CivException(CivSettings.localize.localizedString("cannotBuild_outsideBorder"));
						}
					}
				}
			}
		}

		if (tradeOutpost != null && !foundTradeGood) throw new CivException(CivSettings.localize.localizedString("buildable_errorNotOnTradeGood"));

		/* Delete any road blocks we happen to come across. */
		for (RoadBlock rb : deletedRoadBlocks) {
			rb.getRoad().deleteRoadBlock(rb);
		}
	}

	// ------------- Build Task
	public double getHammerCost() {
		double rate = 1;
		rate -= this.getTown().getBuffManager().getEffectiveDouble(Buff.RUSH);
		rate -= this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
		rate -= this.getTown().getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");
		return rate * getInfo().hammer_cost;
	}

	public int getTotalBlock() {
		// return this.getTemplate().getTotalBlocks();
		return this.getTemplate().size_x * this.getTemplate().size_y * this.getTemplate().size_z;
	};

	public double getBlocksPerHammer() {
		// no hammer cost should be instant...
		if (this.getHammerCost() == 0) return this.getTotalBlock();
		return this.getTotalBlock() / this.getHammerCost();
	}

	/** Время ожидания между установкой блоков. Если менше 1000 мс то никак не влияет на процес строительства */
	public int getTimeLag() {
		// buildTime is in hours, we need to return milliseconds. We should return the
		// number of milliseconds to wait between each block placement.
		double millisecondsPerBlock = 3600 * 1000 / getBlocksPerHour();

		if (millisecondsPerBlock < 500) // Clip millisecondsPerBlock to 500 milliseconds.
			millisecondsPerBlock = 500;
		return (int) millisecondsPerBlock;
	}

	/** скорость установки блоков */
	public double getBlocksPerHour() {
		return getTown().getHammers().total * this.getTotalBlock() / this.getHammerCost();
	}

	public double getHammersCompleted() {
		return getHammerCost() * getBlocksCompleted() / getTotalBlock();
	}

	/** Количество блоков которое нужно установить за такт. Такт = 500 мс */
	public int getBlocksPerTick() {
		double blocks = getBlocksPerHour() / 7200;
		if (blocks < 1) blocks = 1;
		return (int) blocks;
	}

	public void setBlocksCompleted(int builtBlockCount) {
		this.blocksCompleted = builtBlockCount;
		this.savedBlockCount = builtBlockCount;
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

	public void updateSignText() {
	}

	public void onDemolish() throws CivException {
	}

	public void onGoodieFrame() { // Override children
	}

	public void validateAsyncTask(Player player) throws CivException {
		TaskMaster.asyncTask(new StructureValidator(player, this, null), 0);
	}

	/* SessionDB helpers */
	@Override
	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDatabase().add(key, value, this.getCiv().getId(), this.getTown().getId(), this.getId());
	}

	// --------------- Damage
	public void onDamage(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit) {
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

		if ((this instanceof TradeOutpost || this instanceof FishingBoat) && player != null) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_cannotBeBroken", "§6" + hit.getOwner().getDisplayName() + "§c"));
			return;
		}

		Construct constrOwner = hit.getOwner();
		if (!((constrOwner instanceof Buildable) && ((Buildable) constrOwner).isComplete() || (hit.getOwner() instanceof Wonder))) {
			if (player != null) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_underConstruction", hit.getOwner().getDisplayName()));
			}
			return;
		}
		if (this instanceof Neuschwanstein && player != null) {
			if (this.getTown().hasStructure("s_capitol")) {
				final Capitol capitol = (Capitol) this.getTown().getStructureByType("s_capitol");
				boolean allDestroyed = true;
				for (final ControlPoint c : capitol.controlPoints.values()) {
					if (c.getInfo().equalsIgnoreCase("Neuschwanstein") && !c.isDestroyed()) {
						allDestroyed = false;
						break;
					}
				}
				if (!allDestroyed) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_cannotAttackNeu", this.getTown().getName()));
					return;
				}
			} else {
				final Townhall townHall = (Townhall) this.getTown().getStructureByType("s_townhall");
				boolean allDestroyed = true;
				for (final ControlPoint c : townHall.controlPoints.values()) {
					if (c.getInfo().equalsIgnoreCase("Neuschwanstein") && !c.isDestroyed()) {
						allDestroyed = false;
						break;
					}
				}
				if (!allDestroyed) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_cannotAttackNeu", this.getTown().getName()));
					return;
				}
			}
		}
		if ((hit.getOwner().getDamagePercentage() % 10) == 0) {
			wasTenPercent = true;
		}

		this.damage(amount);

		world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
		world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

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
		if (this.getCiv().getCapitol() != null && this.getCiv().getCapitol().getBuffManager().hasBuff("level5_extraTowerHPTown")) {
			++regenRate;
		}
		if (regenRate != 0) {
			if ((this.getHitpoints() != this.getMaxHitPoints()) && (this.getHitpoints() != 0)) {
				this.setHitpoints(this.getHitpoints() + regenRate);
				if (this.getHitpoints() > this.getMaxHitPoints()) this.setHitpoints(this.getMaxHitPoints());
			}
		}
	}

	public boolean isTownHall() {
		return (this instanceof Townhall);
	}

	public void markInvalid() {
		this.valid = isPartOfAdminCiv();
		if (!this.valid) this.getTown().invalidStructures.add(this);
	}

	public void setValid(boolean b) {
		this.valid = this.isPartOfAdminCiv() || b;
	}

	@Override
	public boolean isValid() {
		return isPartOfAdminCiv() || valid;
	}

	@Override
	public boolean isActive() {
		return this.isComplete() && (this.isTownHall() || !isDestroyed()) && isEnabled();
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
