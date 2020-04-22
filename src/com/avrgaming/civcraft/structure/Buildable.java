package com.avrgaming.civcraft.structure;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.wonders.Neuschwanstein;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.global.perks.Perk;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class Buildable extends Construct {
	protected BlockCoord mobSpawnerCoord;

	private ConfigBuildableInfo info = new ConfigBuildableInfo();
	public int blocksCompleted = 0;
	public int savedBlockCount = 0;

	private boolean complete = false;
	private boolean enabled = true;
	public String invalidLayerMessage = "";

	private boolean valid = true;
	public HashMap<Integer, BuildableLayer> layerValidPercentages = new HashMap<Integer, BuildableLayer>();
	public boolean validated = false;
	private String invalidReason = "";

	// ---------------- get ConfigBuildableInfo
	public String getConfigId() {
		return info.id;
	}

	@Override
	public String getDisplayName() {
		return info.displayName;
	}

	@Override
	public int getMaxHitPoints() {
		return info.max_hitpoints;
	}

	public double getCost() {
		return info.cost;
	}

	@Override
	public int getRegenRate() {
		if (this.info.regenRate == null)
			return 0;
		return info.regenRate;
	}

	@Override
	public double getUpkeepCost() {
		return info.upkeep;
	}

	@Override
	public int getTemplateYShift() {
		return info.templateYShift;
	}

	public String getRequiredUpgrade() {
		return info.require_upgrade;
	}

	public String getRequiredTechnology() {
		return info.require_tech;
	}

	public int getPoints() {
		if (info.points == null)
			return 0;
		return info.points;
	}

	@Override
	public boolean allowDemolish() {
		return info.allow_demolish;
	}

	public boolean isTileImprovement() {
		return info.tile_improvement;
	}

	@Override
	public boolean isDestroyable() {
		return (info.destroyable != null) && (info.destroyable == true);
	}

	public boolean isAvailable() {
		return info.isAvailable(this.getTown());
	}

	@Override
	public int getLimit() {
		return info.limit;
	}

	public boolean isAllowOutsideTown() {
		return (info.allow_outside_town != null) && (info.allow_outside_town == true);
	}

	@Override
	public boolean isStrategic() {
		return info.strategic;
	}

	public boolean isIgnoreFloating() {
		return info.ignore_floating;
	}

	public String getReplaceStructure() {
		return info.replace_structure;
	}

	protected List<HashMap<String, String>> getComponentInfoList() {
		return info.components;
	}

	// ------------- Build ----------------------
	public void newBiuldSetTemplate(Player player, Location centerLoc) throws CivException, IOException {
		// children override for wall and road
		/* Look for any custom template perks and ask the player if they want to use
		 * them. */
		Resident resident = CivGlobal.getResident(player);
		ArrayList<Perk> perkList = this.getTown().getTemplatePerks(this, resident, this.info);
		ArrayList<Perk> personalUnboundPerks = resident.getUnboundTemplatePerks(perkList, this.info);
		if (personalUnboundPerks.size() != 0 || perkList.size() != 0) {
			/* Store the pending buildable. */
			resident.pendingBuildable = this;

			/* Build an inventory full of templates to select. */
			Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9);
			ItemStack infoRec = LoreGuiItem.build(CivSettings.localize.localizedString("buildable_lore_default") + " " + this.getDisplayName(), ItemManager.getMaterialId(Material.WRITTEN_BOOK), 0, CivColor.Gold + CivSettings.localize
					.localizedString("loreGui_template_clickToBuild"));
			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
			inv.addItem(infoRec);

			for (Perk perk : perkList) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data, CivColor.Gold + "<Click To Build>", CivColor.Gray + "Provided by: " + CivColor.LightBlue + perk.provider);
				infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getConfigId());
				inv.addItem(infoRec);
			}
			for (Perk perk : personalUnboundPerks) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), CivData.BEDROCK, perk.configPerk.data, CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"), CivColor.Gray + CivSettings.localize
						.localizedString("loreGui_template_unbound"), CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound2"), CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound3"),
						CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound4"), CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound5"));
				infoRec = LoreGuiItem.setAction(infoRec, "ActivatePerk");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getConfigId());
			}
			/* We will resume by calling buildPlayerPreview with the template when a gui
			 * item is clicked. */
			player.openInventory(inv);
			return;
		}

		this.setTemplate(Template.getTemplate(Template.getTemplateFilePath(centerLoc, this.getInfo(), null)));
		BuildableStatic.buildPlayerPreview(player, centerLoc, this);
	}

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
	public Location repositionCenter(Location center, Template tpl) throws CivException {
		return BuildableStatic.repositionCenterStatic(center, this.getInfo().templateYShift, tpl);
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {

		Block centerBlock = this.getCorner().getBlock();
		int regionX = this.getTemplate().getSize_x();
		int regionY = this.getTemplate().getSize_y();
		int regionZ = this.getTemplate().getSize_z();

		boolean foundTradeGood = false;
		TradeOutpost tradeOutpost = null;
		boolean ignoreBorders = false;

		if (this instanceof TradeOutpost)
			tradeOutpost = (TradeOutpost) this;

		// Make sure we are building this building inside of culture.
		if (!this.info.id.equalsIgnoreCase("s_capitol") && !this.info.id.equalsIgnoreCase("s_townhall")) {
			CultureChunk cc = CivGlobal.getCultureChunk(centerBlock.getLocation());
			if (cc == null || cc.getTown().getCiv() != this.getCiv()) {
				CivLog.debug("Строим здание " + this.getDisplayName() + " не в пределах культуры");
				throw new CivException(CivSettings.localize.localizedString("buildable_notInCulture"));
			}
		} else {
			/* Structure is a town hall, auto-claim the borders. */
			ignoreBorders = true;
		}

		if (isTownHall()) {
			double minDistance;
			try {
				minDistance = CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance");
			} catch (InvalidConfiguration e) {
				throw new CivException(CivSettings.localize.localizedString("internalException"));
			}

			for (Town town : CivGlobal.getTowns()) {
				Townhall townhall = town.getTownHall();
				if (townhall == null)
					continue;

				double dist = townhall.getCenterLocation().distanceSquared(centerBlock.getLocation());
				if (dist < minDistance * minDistance) {
					DecimalFormat df = new DecimalFormat();
					throw new CivException(CivSettings.localize.localizedString("var_settler_errorTooClose", town.getName(), df.format(Math.sqrt(dist)), minDistance));
				}
			}
		}

		if (this.getConfigId().equals("s_shipyard") || this.getConfigId().equals("s_arrowship") || this.getConfigId().equals("s_scoutship") || this.getConfigId().equals("s_cannonship") || this.getConfigId().equals("ti_tradeship") || this
				.getConfigId().equals("w_grand_ship_ingermanland")) {
			if (!centerBlock.getBiome().equals(Biome.OCEAN) && !centerBlock.getBiome().equals(Biome.BEACHES) && !centerBlock.getBiome().equals(Biome.STONE_BEACH) && !centerBlock.getBiome().equals(Biome.COLD_BEACH) && !centerBlock.getBiome()
					.equals(Biome.DEEP_OCEAN) && !centerBlock.getBiome().equals(Biome.RIVER) && !centerBlock.getBiome().equals(Biome.FROZEN_OCEAN) && !centerBlock.getBiome().equals(Biome.FROZEN_RIVER)) {
				throw new CivException(CivSettings.localize.localizedString("var_buildable_notEnoughWater", this.getDisplayName()));
			}
		}

		Structure struct = CivGlobal.getStructure(new BlockCoord(centerBlock));
		if (struct != null) {
			throw new CivException(CivSettings.localize.localizedString("buildable_structureExistsHere"));
		}

		ignoreBorders = this.isAllowOutsideTown();

		if (!player.isOp())
			BuildableStatic.validateDistanceFromSpawn(centerBlock.getLocation());

		if (this.isTileImprovement()) {
			ignoreBorders = true;
			ConfigTownLevel level = CivSettings.townLevels.get(getTown().getLevel());

			Integer maxTileImprovements = level.tile_improvements;
			if (getTown().getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
				maxTileImprovements *= 2;
			}
			if (getTown().getTileImprovementCount() >= maxTileImprovements) {
				throw new CivException(CivSettings.localize.localizedString("buildable_errorTILimit"));
			}

			ChunkCoord coord = new ChunkCoord(centerBlock.getLocation());
			for (Structure s : getTown().getStructures()) {
				if (!s.isTileImprovement())
					continue;
				ChunkCoord sCoord = new ChunkCoord(s.getCorner());
				if (sCoord.equals(coord)) {
					throw new CivException(CivSettings.localize.localizedString("buildable_errorTIHere"));
				}
			}

		}

		if (centerBlock.getLocation().getY() >= 255) {
			throw new CivException(CivSettings.localize.localizedString("buildable_errorTooHigh"));
		}

		if (centerBlock.getLocation().getY() <= 7) {
			throw new CivException(CivSettings.localize.localizedString("buildable_errorTooLow"));
		}

		if (centerBlock.getLocation().getY() < CivGlobal.minBuildHeight) {
			throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		}

		if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
			throw new CivException(CivSettings.localize.localizedString("buildable_errorHeightLimit"));
		}

		/* Check that we're not overlapping with another structure's template
		 * outline. */
		/* XXX this needs to check actual blocks, not outlines cause thats more annoying
		 * than actual problems caused by building into each other. */
		// Iterator<Entry<BlockCoord, Structure>> iter =
		// CivGlobal.getStructureIterator();
		// while(iter.hasNext()) {
		// Entry<BlockCoord, Structure> entry = iter.next();
		// Structure s = entry.getValue();
		//
		// if (s.templateBoundingBox != null) {
		// if (s.templateBoundingBox.overlaps(this.templateBoundingBox)) {
		// throw new CivException("Cannot build structure here as it would overlap with
		// a "+s.getDisplayName());
		// }
		// }
		// }

		onCheckBlockPAR();

		LinkedList<RoadBlock> deletedRoadBlocks = new LinkedList<RoadBlock>();

		for (int x = 0; x < regionX; x++) {
			for (int y = 0; y < regionY; y++) {
				for (int z = 0; z < regionZ; z++) {
					Block b = centerBlock.getRelative(x, y, z);

					if (ItemManager.getTypeId(b) == CivData.CHEST)
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));

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
						if (pb != null) {
						}
					} else {
						if (CivGlobal.getTradeGood(coord) != null) {
							// Make sure we encompass entire trade good.
							if ((y + 3) < regionY) {
								foundTradeGood = true;
								tradeOutpost.setTradeGoodCoord(coord);
							}
						}
					}

					if (CivGlobal.getConstructBlock(coord) != null)
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
					if (CivGlobal.getFarmChunk(new ChunkCoord(coord.getLocation())) != null)
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
					if (CivGlobal.getWallChunk(chunkCoord) != null)
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));
					if (CivGlobal.getConstructFromChunk(coord) != null)
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));

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

		if (tradeOutpost != null && !foundTradeGood)
			throw new CivException(CivSettings.localize.localizedString("buildable_errorNotOnTradeGood"));

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
		return rate * info.hammer_cost;
	}

	public int getTotalBlock() {
		return this.getTemplate().getTotalBlocks();// tpl.size_x * tpl.size_y * tpl.size_z);
	};

	public double getBlocksPerHammer() {
		// no hammer cost should be instant...
		if (this.getHammerCost() == 0)
			return this.getTotalBlock();
		return this.getTotalBlock() / this.getHammerCost();
	}

	/**
	 * Время ожидания между установкой блоков. Если менше 1000 мс то никак не влияет
	 * на процес строительства
	 */
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
		if (blocks < 1)
			blocks = 1;
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

	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
	}

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
		TaskMaster.asyncTask(new StructureValidator(player, this), 0);
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
				if (this.getHitpoints() > this.getMaxHitPoints())
					this.setHitpoints(this.getMaxHitPoints());
			}
		}
	}

	public boolean isTownHall() {
		return (this instanceof Townhall);
	}

	public void markInvalid() {
		this.valid = isPartOfAdminCiv();
		if (!this.valid)
			this.getTown().invalidStructures.add(this);
	}

	public void setValid(boolean b) {
		this.valid = this.isPartOfAdminCiv() || b;
	}

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
		if (damage <= 0)
			damage = 10;

		this.damage(damage);

		DecimalFormat df = new DecimalFormat("###");
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupport", this.getDisplayName(), (centerLoc.getBlockX() + "," + centerLoc.getBlockY() + "," + centerLoc.getBlockZ())));
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupportDamage", df.format(invalid_hourly_penalty * 100), (this.getHitpoints() + "/" + this.getMaxHitPoints())));
		CivMessage.sendTown(this.getTown(), CivColor.Rose + this.invalidLayerMessage);
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("buildable_validationPrompt"));
		this.save();
	}
}
