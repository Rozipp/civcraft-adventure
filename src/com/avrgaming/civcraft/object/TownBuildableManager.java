package com.avrgaming.civcraft.object;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.components.AttributeBase;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveBuildableRefresh;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Bank;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Cottage;
import com.avrgaming.civcraft.structure.Quarry;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TradeShip;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildTemplateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TownBuildableManager {

	private Town town = null;

	private ConcurrentHashMap<BlockCoord, Wonder> wonders = new ConcurrentHashMap<BlockCoord, Wonder>();
	private ConcurrentHashMap<BlockCoord, Structure> structures = new ConcurrentHashMap<BlockCoord, Structure>();
	private LinkedList<Buildable> disabledBuildables = new LinkedList<>();
	private LinkedList<Buildable> invalideBuildables = new LinkedList<>();

	private LinkedList<Buildable> buildablePoolInProgress = new LinkedList<>();

	private Buildable lastBuildableBuilt = null;

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

	public TownBuildableManager(Town town) {
		this.town = town;
	}

	// ------------ wonders

	public void addWonder(Wonder wonder) {
		this.wonders.put(wonder.getCorner(), wonder);
		CivGlobal.addWonder(wonder);
	}

	public boolean hasWonder(final String wonder_id) {
		if (wonder_id == null || wonder_id.equals("")) return false;
		Wonder foundwonder = null;
		for (final Wonder wonder : this.wonders.values()) {
			if (wonder.getConfigId().equalsIgnoreCase(wonder_id)) {
				foundwonder = wonder;
				break;
			}
		}
		return foundwonder != null && foundwonder.isActive();
	}

	public void removeWonder(Wonder wonder) {
		if (!wonder.isComplete()) this.removeBuildableInprogress(wonder);
		if (buildablePoolInProgress.contains(wonder)) buildablePoolInProgress.remove(wonder);
		this.wonders.remove(wonder.getCorner());
	}

	public Collection<Wonder> getWonders() {
		return this.wonders.values();
	}

	// -------------Structure

	public void addStructure(Structure struct) {
		this.structures.put(struct.getCorner(), struct);

		if (isStructureAddable(struct)) {
			this.disabledBuildables.remove(struct);
			struct.setEnabled(true);
		} else {
			this.disabledBuildables.add(struct);
			struct.setEnabled(false);
		}
	}

	public boolean isStructureAddable(Structure struct) {
		if (struct.isTileImprovement()) {
			Integer maxTileImprovements = town.getMaxTileImprovements();
			return town.getTileImprovementCount() <= maxTileImprovements;
		}
		return (struct.getLimit() == 0) || (this.getBuildableByIdCount(struct.getConfigId()) <= struct.getLimit());
	}

	public boolean hasStructure(String structure_id) {
		if (structure_id == null || structure_id.equals("")) return true;
		Structure foundstruct = null;
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(structure_id)) {
				foundstruct = struct;
				break;
			}
		}
		return foundstruct != null && foundstruct.isActive();
	}

	public Collection<Structure> getStructures() {
		return this.structures.values();
	}

	public void removeStructure(Structure structure) {
		if (!structure.isComplete()) this.removeBuildableInprogress(structure);
		this.structures.remove(structure.getCorner());
		this.invalideBuildables.remove(structure);
		this.disabledBuildables.remove(structure);
	}

	// ------------ build

	public void checkIsTownCanBuildBuildable(Buildable buildable) throws CivException {
		if (!town.hasUpgrade(buildable.getRequiredUpgrade())) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingUpgrade") + " §6" + CivSettings.getUpgradeById(buildable.getRequiredUpgrade()).name);
		if (!town.hasTechnology(buildable.getRequiredTechnology())) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingTech") + " §6" + CivSettings.getTechById(buildable.getRequiredTechnology()).name);
		if (!buildablePoolInProgress.isEmpty()) {
			Buildable inProgress = buildablePoolInProgress.get(0);
			if (inProgress instanceof Structure)
				throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + ". " + CivSettings.localize.localizedString("town_buildwonder_errorOneAtATime"));
			if (inProgress instanceof Wonder)
				throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName()) + " " + CivSettings.localize.localizedString("town_buildwonder_errorOneWonderAtaTime"));
		}

		double cost = buildable.getCost();
		if (!town.getTreasury().hasEnough(cost)) throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorTooPoor", buildable.getDisplayName(), cost, CivSettings.CURRENCY_NAME));

		if (buildable instanceof Structure) {
			if (!buildable.isAvailable()) throw new CivException(CivSettings.localize.localizedString("town_structure_errorNotAvaliable"));
			if (buildable.getLimit() != 0 && getBuildableByIdCount(buildable.getConfigId()) >= buildable.getLimit())
				throw new CivException(CivSettings.localize.localizedString("var_town_structure_errorLimitMet", buildable.getLimit(), buildable.getDisplayName()));
		} else {
			if (this.wonders.size() >= 2) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorLimit2"));
			if (!buildable.getCorner().inMainWorld()) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_NotOverworld"));
			if (!buildable.isAvailable()) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorNotAvailable"));
			if (!Wonder.isWonderAvailable(buildable.getConfigId())) throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorBuiltElsewhere"));
		}

		ArrayList<ChunkCoord> chunks = buildable.getChunksCoords();
		int needClaim = 0;
		for (ChunkCoord cc : chunks) {
			TownChunk tc = CivGlobal.getTownChunk(cc);
			if (tc == null)
				needClaim++;
			else
				if (!tc.getTown().equals(town)) throw new CivException("Один из чанков, которые займёт зданием, пренадлежит городу " + tc.getTown().getName());
		}
		if (town.getTownChunks().size() + needClaim > town.getMaxPlots())
			throw new CivException("Для постройки здания требуеться заприватить " + chunks.size() + " плотов. В вашем городе занято " + town.getTownChunks().size() + " из " + town.getMaxPlots()
					+ ". Освободите плоты командой /plot unclaim, или улучшите город командой /t upgrade buy");
	}

	/** @deprecated */
	public void startBuildStructure(Player player, Structure struct) {
		try {
			struct.runOnBuild(struct.getCorner().getChunkCoord());
		} catch (CivException e1) {
			e1.printStackTrace();
		}
		struct.startBuildTask();
		struct.save();

		// Go through and add any town chunks that were claimed to this list of saved objects.
		town.getTreasury().withdraw(struct.getCost());

		town.save();
	}

	/** @deprecated */
	public void rebuildStructure(Player player, Structure struct) throws CivException {
		if (struct.getReplaceStructure() == null) {
			struct.build(player);
			return;
		}

		Structure replaceStruct = this.getFirstStructureById(struct.getInfo().replace_structure);
		if (replaceStruct == null) throw new CivException("Заменяемое здание " + struct.getReplaceStructure() + " не найдено");

		try {
			CivMessage.sendTown(replaceStruct.getTown(), CivColor.Yellow + "В городе начался снос постройки " + replaceStruct.getDisplayName());
			TaskMaster.asyncTask(new Runnable() {
				@Override
				public void run() {
					try {
						String templatePath = Template.getUndoFilePath(replaceStruct.getCorner().toString());
						Template tpl = new Template(templatePath);
						BuildTemplateTask btt = new BuildTemplateTask(tpl, replaceStruct.getCorner(), true);
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

					replaceStruct.delete();
					replaceStruct.getTown().BM.startBuildStructure(player, struct);
				}
			}, 10);
		} catch (Exception e) {
			e.printStackTrace();
			CivMessage.sendError(player, e.getMessage());
		}
	}

	// -------------- wonder_stock_exchange

	public boolean canBuildStock(final Player player) {
		int bankCount = 0;
		int tradeShipCount = 0;
		int cottageCount = 0;
		int quarryCount = 0;
		for (final Town t : town.getCiv().getTowns()) {
			for (final Structure structure : t.BM.getStructures()) {
				if (structure instanceof Bank && ((Bank) structure).getLevel() == 10) ++bankCount;
				if (structure instanceof TradeShip && ((TradeShip) structure).getLevel() >= 7) ++tradeShipCount;
				if (structure instanceof Cottage && ((Cottage) structure).getLevel() >= 5) ++cottageCount;
				if (structure instanceof Quarry && ((Quarry) structure).getLevel() >= 3) ++quarryCount;
			}
		}
		boolean bankCountCondition = false;
		boolean tradeShipCountCondition = false;
		boolean cottageCountCondition = false;
		boolean quarryCountCondition = false;
		if (bankCount >= 3) bankCountCondition = true;
		if (tradeShipCount >= 3) tradeShipCountCondition = true;
		if (cottageCount >= 15) cottageCountCondition = true;
		if (quarryCount >= 3) quarryCountCondition = true;

		CivMessage.sendCiv(town.getCiv(), (bankCountCondition ? "§a" : "§c") + "Number of lvl 10 Banks: " + "§e" + (bankCountCondition ? "Done " : "Incomplete ") + bankCount + "/3");
		CivMessage.sendCiv(town.getCiv(), (tradeShipCountCondition ? "§a" : "§c") + "Number of lvl 7 Trade Ships: " + "§e" + (tradeShipCountCondition ? "Done " : "Incomplete ") + tradeShipCount + "/3");
		CivMessage.sendCiv(town.getCiv(), (cottageCountCondition ? "§a" : "§c") + "Number of lvl 5 Cottages: " + "§e" + (cottageCountCondition ? "Done " : "Incomplete ") + cottageCount + "/15");
		CivMessage.sendCiv(town.getCiv(), (quarryCountCondition ? "§a" : "§c") + "Number of lvl 3 Quarries: " + "§e" + (quarryCountCondition ? "Done " : "Incomplete ") + quarryCount + "/3");
		return bankCountCondition && tradeShipCountCondition && cottageCountCondition && quarryCountCondition;
	}

	public boolean canUpgradeStock(final String upgradeName) {
		int bankCount = 0;
		int tradeShipCount = 0;
		int cottageCount = 0;
		int quarryCount = 0;
		for (final Town town : town.getCiv().getTowns()) {
			for (final Structure structure : town.BM.getStructures()) {
				if (structure instanceof Bank && ((Bank) structure).getLevel() == 10) ++bankCount;
				if (structure instanceof TradeShip && ((TradeShip) structure).getLevel() >= 8) ++tradeShipCount;
				if (structure instanceof Cottage && ((Cottage) structure).getLevel() >= 6) ++cottageCount;
				if (structure instanceof Quarry && ((Quarry) structure).getLevel() >= 4) ++quarryCount;
			}
		}
		boolean bankCountCondition = false;
		boolean tradeShipCountCondition = false;
		boolean cottageCountCondition = false;
		boolean quarryCountCondition = false;
		if (bankCount >= 3) bankCountCondition = true;
		if (tradeShipCount >= 3) tradeShipCountCondition = true;
		if (cottageCount >= 20) cottageCountCondition = true;
		if (quarryCount >= 3) quarryCountCondition = true;

		CivMessage.sendCiv(town.getCiv(), (bankCountCondition ? "§a" : "§c") + "Number of lvl 10 Banks: " + "§e" + (bankCountCondition ? "Done " : "Incomplete ") + bankCount + "/3");
		CivMessage.sendCiv(town.getCiv(), (tradeShipCountCondition ? "§a" : "§c") + "Number of lvl 8 Trade Ships: " + "§e" + (tradeShipCountCondition ? "Done " : "Incomplete ") + tradeShipCount + "/3");
		CivMessage.sendCiv(town.getCiv(), (cottageCountCondition ? "§a" : "§c") + "Number of lvl 6 Cottages: " + "§e" + (cottageCountCondition ? "Done " : "Incomplete ") + cottageCount + "/20");
		CivMessage.sendCiv(town.getCiv(), (quarryCountCondition ? "§a" : "§c") + "Number of lvl 4 Quarries: " + "§e" + (quarryCountCondition ? "Done " : "Incomplete ") + quarryCount + "/3");

		return bankCountCondition && tradeShipCountCondition && cottageCountCondition && quarryCountCondition;
	}

	// -------------------- process

	public void demolish(Structure struct, boolean isAdmin) throws CivException {
		if (!struct.allowDemolish() && !isAdmin) throw new CivException(CivSettings.localize.localizedString("town_demolish_Cannot"));
		struct.onDemolish();
		struct.deleteWithUndo();
	}

	public void refreshNearestBuildable(Resident resident) throws CivException {
		if (town.getLastBuildableRefresh() != null) {
			Date now = new Date();
			int buildable_refresh_cooldown;
			try {
				buildable_refresh_cooldown = CivSettings.getInteger(CivSettings.townConfig, "town.buildable_refresh_cooldown");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
			}

			if (now.getTime() < town.getLastBuildableRefresh().getTime() + (buildable_refresh_cooldown * 60 * 1000)) {
				throw new CivException(CivSettings.localize.localizedString("var_town_refresh_wait1", buildable_refresh_cooldown));
			}
		}

		Player player = CivGlobal.getPlayer(resident);
		Buildable buildable = CivGlobal.getNearestBuildable(player.getLocation());
		if (buildable == null) throw new CivException(CivSettings.localize.localizedString("town_refresh_couldNotFind"));
		if (!buildable.isActive()) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorInProfress"));
		if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWar"));
		if (!buildable.getTown().equals(town)) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWrongTown"));
		resident.setInteractiveMode(new InteractiveBuildableRefresh(buildable, resident.getName()));
	}

	public void processStructureFlipping(HashMap<ChunkCoord, Structure> centerCoords) {

		for (CultureChunk cc : town.getCultureChunks()) {
			Structure struct = centerCoords.get(cc.getChunkCoord());
			if (struct == null) continue;
			if (struct.getCiv() == cc.getCiv()) continue;

			/* There is a structure at this location that doesnt belong to us! Grab it! */
			struct.getTown().BM.removeStructure(struct);
			this.addStructure(struct);
			struct.setSQLOwner(town);
			struct.save();
		}
	}

	public Collection<Buildable> getDisabledBuildables() {
		return this.disabledBuildables;
	}

	public void addInvalideBuildable(Buildable buildable) {
		this.invalideBuildables.add(buildable);
	}

	public double getBeakersFromStructure() {
		double fromStructures = 0;
		for (Structure struct : getStructures()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase) comp;
					if (as.getString("attribute").equalsIgnoreCase("BEAKERS")) fromStructures += as.getGenerated();
				}
			}
		}
		return fromStructures;
	}

	// ----------- found

	public Wonder getWonderById(final String id) {
		for (final Wonder wonder : this.wonders.values()) {
			if (wonder.getConfigId().equalsIgnoreCase(id)) return wonder;
		}
		return null;
	}

	public Wonder getWonderByName(String name) {
		Wonder found = null;
		for (final Wonder wonder : this.wonders.values()) {
			if (wonder.getDisplayName().toLowerCase().startsWith(name.toLowerCase())) {
				if (found == null)
					found = wonder;
				else {
					found = null;
					break;
				}
			}
		}
		return null;
	}

	public Structure getFirstStructureById(String id) {
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(id)) return struct;
		}
		return null;
	}

	public List<Structure> getAllStructuresById(String id) {
		List<Structure> structures = new LinkedList<>();
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(id)) structures.add(struct);
		}
		return structures;
	}

	public int getBuildableByIdCount(String id) {
		int count = 0;
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(id)) count++;
		}
		for (Wonder wonder : this.wonders.values()) {
			if (wonder.getConfigId().equalsIgnoreCase(id)) count++;
		}
		return count;
	}

	public Structure getStructure(BlockCoord coord) {
		return structures.get(coord);
	}

	public Buildable getNearestStrucutreOrWonder(Location location) {
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

	/** @deprecated */
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

	public void onHourlyUpdate(CivAsyncTask task) {
		// Loop through each structure, if it has an update function call it in another async process
		for (Structure struct : getStructures()) {
			if (!struct.isActive()) continue;
			struct.onHourlyUpdate(task);
		}
		for (Wonder wonder : getWonders()) {
			if (!wonder.isActive()) continue;
			wonder.onHourlyUpdate(task);
		}
	}

	public void onCivtickUpdate(CivAsyncTask task) {
		for (Structure struct : CivGlobal.getStructures()) {
			if (!struct.isActive()) continue;
			struct.onCivtickUpdate();
		}
		for (Wonder wonder : CivGlobal.getWonders()) {
			wonder.onCivtickUpdate();
		}

		town.PM.markAllWorkerNotWork();
		if (!buildablePoolInProgress.isEmpty()) {
			int hammers = 0;
			try {
				Buildable buildable = buildablePoolInProgress.get(0);
				if (buildable.isNextProgressBuild()) {
					buildable.validCanProgressBuild();
					int neadHammers = Math.min(buildable.getNeadHammersToComplit(), town.SM.getHammers());
					hammers = town.PM.progressBuildGetHammers(neadHammers);
					buildable.progressBuild(hammers);
				}
			} catch (CivException e) {
				CivMessage.sendTown(town, e.getMessage());
			}
			town.SM.withdrawHammers(hammers);
		}
	}

	public void onSecondUpdate(CivAsyncTask task) {
		for (Structure struct : CivGlobal.getStructures()) {
			if (!struct.isActive()) continue;
			struct.onSecondUpdate();
		}
		for (Wonder wonder : CivGlobal.getWonders()) {
			wonder.onSecondUpdate();
		}
	}

	// ---------- build task

	public void addBuildableInprogress(Buildable build) {
		List<Buildable> remove = new ArrayList<Buildable>();

		if (build instanceof Wonder) {
			for (Buildable b : buildablePoolInProgress) {
				if (b instanceof Wonder) {
					remove.add(b);
					buildablePoolInProgress.remove(b);
				}
			}
		}
		this.buildablePoolInProgress.add(build);
		if (!remove.isEmpty()) {
			for (Buildable b : remove) {
				if (b instanceof Wonder) {
					buildablePoolInProgress.add(b);
				}
			}
		}
	}

	public Buildable getBuildableInprogress() {
		if (buildablePoolInProgress.isEmpty()) return null;
		return buildablePoolInProgress.get(0);
	}

	public boolean isBuildableInprogress(Buildable buildable) {
		if (buildable == null) return false;
		return buildable.equals(getBuildableInprogress());
	}

	public void removeBuildableInprogress(Buildable buildable) {
		buildablePoolInProgress.remove(buildable);
	}

}
