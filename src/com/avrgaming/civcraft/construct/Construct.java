package com.avrgaming.civcraft.construct;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structure.RoadBlock;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildTemplateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Construct extends SQLObject {

	protected BlockCoord corner;
	private Location centerLocation;
	private ConfigBuildableInfo info = new ConfigBuildableInfo();
	private int hitpoints;
	private boolean enabled = true;
	private Template template;
	private SQLObject SQLOwner;

	public String invalidLayerMessage = "";
	public boolean validated = false;
	public boolean valid = false;
	public HashMap<Integer, ConstructLayer> layerValidPercentages = new HashMap<Integer, ConstructLayer>();

	private Map<BlockCoord, ConstructSign> сonstructSigns = new ConcurrentHashMap<BlockCoord, ConstructSign>();
	private Map<BlockCoord, ConstructChest> сonstructChests = new ConcurrentHashMap<BlockCoord, ConstructChest>();
	protected Map<BlockCoord, Boolean> constructBlocks = new ConcurrentHashMap<BlockCoord, Boolean>();

	public ArrayList<Component> attachedComponents = new ArrayList<Component>();

	public String getHash() {
		return corner.toString();
	}

	public Resident getOwnerResident() {
		if (SQLOwner == null) return null;
		if (SQLOwner instanceof Resident) return (Resident) SQLOwner;
		return null;
	}

	public Construct getOwnerConstruct() {
		if (SQLOwner == null) return null;
		if (SQLOwner instanceof Construct) return (Construct) SQLOwner;
		return null;
	}

	public Town getTown() {
		if (SQLOwner == null) return null;
		if (SQLOwner instanceof Construct) return ((Construct) SQLOwner).getTown();
		if (SQLOwner instanceof Town) return (Town) SQLOwner;
		if (SQLOwner instanceof Resident) return ((Resident) SQLOwner).getTown();
		return null;
	}

	public Civilization getCiv() {
		if (SQLOwner == null) return null;
		if (SQLOwner instanceof Construct) return ((Construct) SQLOwner).getCiv();
		if (SQLOwner instanceof Town) return ((Town) SQLOwner).getCiv();
		if (SQLOwner instanceof Civilization) return (Civilization) SQLOwner;
		if (SQLOwner instanceof Resident) return ((Resident) SQLOwner).getCiv();
		return null;
	}

	public void setCorner(Location loc) {
		this.corner = new BlockCoord(loc);
		if (getTemplate() != null)
			this.setCenterLocation(loc.add(getTemplate().size_x / 2, getTemplate().size_y / 2, getTemplate().size_z / 2));
		else
			this.setCenterLocation(loc);
	}

	// ---------------- get ConfigBuildableInfo
	public String getConfigId() {
		return info.id;
	}

	public String getDisplayName() {
		return info.displayName;
	}

	public int getMaxHitPoints() {
		return info.max_hitpoints;
	}

	public double getCost() {
		return info.cost;
	}

	public int getRegenRate() {
		if (this.info.regenRate == null) return 0;
		return info.regenRate;
	}

	public double getUpkeepCost() {
		return info.upkeep;
	}

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
		if (info.points == null) return 0;
		return info.points;
	}

	public boolean allowDemolish() {
		return info.allow_demolish;
	}

	public boolean isTileImprovement() {
		return info.tile_improvement;
	}

	public boolean isDestroyable() {
		return (info.destroyable != null) && info.destroyable;
	}

	public boolean isAvailable() {
		if (this.getTown() == null || this.getTown().getId() == 0) return true;
		return info.isAvailable(this.getTown());
	}

	public int getLimit() {
		return info.limit;
	}

	public boolean isAllowOutsideTown() {
		return (info.allow_outside_town != null) && (info.allow_outside_town == true);
	}

	public boolean isStrategic() {
		return info.strategic;
	}

	public boolean isIgnoreFloating() {
		return info.ignore_floating;
	}

	protected List<HashMap<String, String>> getComponentInfoList() {
		return info.components;
	}

	public Component getComponent(String name) {
		for (Component comp : this.attachedComponents) {
			if (comp.getName().equals(name)) return comp;
		}
		return null;
	}

	public void setTemplate(Template tpl) {
		if (this.getCorner() != null) {
			if (tpl == null) {
				this.setCenterLocation(this.getCorner().getLocation());
			} else
				this.setCenterLocation(this.getCorner().getLocation().add(tpl.size_x / 2, tpl.size_y / 2, tpl.size_z / 2));
		}
		this.template = tpl;
	}

	public Location getCenterLocation() {
		if (this.centerLocation != null)
			return this.centerLocation;
		else
			return this.getCorner().getLocation();
	}

	// ------------- Build ----------------------
	public void initDefaultTemplate(Location location) throws CivException {
		String tplPath = Template.getTemplateFilePath(location, getInfo(), null);
		Template tpl = Template.getTemplate(tplPath);
		if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
		this.setTemplate(tpl);
		this.setCorner(this.repositionCenter(location, tpl));
		this.setHitpoints(getMaxHitPoints());
	}

	public Location repositionCenter(Location center, Template tpl) throws CivException {
		return BuildableStatic.repositionCenterStatic(center, this.getInfo().templateYShift, tpl);
	}

	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		int regionX = this.getTemplate().getSize_x();
		int regionY = this.getTemplate().getSize_y();
		int regionZ = this.getTemplate().getSize_z();

		if (!player.isOp()) BuildableStatic.validateDistanceFromSpawn(this.getCenterLocation());
		if (getCorner().getY() >= 255) throw new CivException(CivSettings.localize.localizedString("buildable_errorTooHigh"));
		if (getCorner().getY() <= 7) throw new CivException(CivSettings.localize.localizedString("buildable_errorTooLow"));
		if (getCorner().getY() < CivGlobal.minBuildHeight) throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		if ((regionY + getCorner().getY()) >= 255) throw new CivException(CivSettings.localize.localizedString("buildable_errorHeightLimit"));

		for (ChunkCoord chunkCoord : BuildableStatic.getChunkCoords(this)) {
			TownChunk tc = CivGlobal.getTownChunk(chunkCoord);
			if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
				// Make sure we have permission to destroy any block in this area.
				throw new CivException(CivSettings.localize.localizedString("cannotBuild_needPermissions"));
			}
			if (CivGlobal.getBuildableAt(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("buildable_structureExistsHere"));
			if (CivGlobal.getFarmChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
			if (CivGlobal.getWallChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));
			if (CivGlobal.getConstructFromChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
		}

		int yTotal = 0;
		int yCount = 0;
		LinkedList<RoadBlock> deletedRoadBlocks = new LinkedList<RoadBlock>();
		for (int x = 0; x < regionX; x++) {
			for (int y = 0; y < regionY; y++) {
				for (int z = 0; z < regionZ; z++) {
					Block block = getCorner().getBlockRelative(x, y, z);
					if (ItemManager.getTypeId(block) == CivData.CHEST) throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));

					BorderData border = Config.Border(block.getWorld().getName());
					if (border != null && !border.insideBorder(block.getX(), block.getZ(), Config.ShapeRound())) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_outsideBorder"));
					}

					yTotal += block.getWorld().getHighestBlockYAt(block.getX(), block.getZ());
					++yCount;

					BlockCoord coord = new BlockCoord(block);
					if (CivGlobal.getConstructBlock(coord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));

					RoadBlock rb = CivGlobal.getRoadBlock(coord);
					if (rb != null) deletedRoadBlocks.add(rb);
				}
			}
		}
		double highestAverageBlock = (double) yTotal / (double) yCount;
		if (getCorner().getY() > (highestAverageBlock + 15) || getCorner().getY() < (highestAverageBlock - 15)) {
			throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		}

		/* Delete any road blocks we happen to come across. */
		for (RoadBlock rb : deletedRoadBlocks) {
			rb.getRoad().deleteRoadBlock(rb);
		}
	}

	public void repairFromTemplate(){
		Template tpl = this.getTemplate();
		HashMap<Chunk, Chunk> chunkUpdates = new HashMap<Chunk, Chunk>();
		Block centerBlock = this.getCorner().getBlock();

		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block b = centerBlock.getRelative(x, y, z);
					SimpleBlock sb = tpl.blocks[x][y][z];
					if (sb.specialType == Type.COMMAND)
						ItemManager.setTypeIdAndData(b, CivData.AIR, (byte) 0, false);
					else
						ItemManager.setTypeIdAndData(b, sb.getType(), (byte) sb.getData(), false);

					chunkUpdates.put(b.getChunk(), b.getChunk());

					if (ItemManager.getTypeId(b) == CivData.WALL_SIGN || ItemManager.getTypeId(b) == CivData.SIGN) {
						Sign s2 = (Sign) b.getState();
						s2.setLine(0, sb.message[0]);
						s2.setLine(1, sb.message[1]);
						s2.setLine(2, sb.message[2]);
						s2.setLine(3, sb.message[3]);
						s2.update();
					}
				}
			}
		}
	}

	// ------------ abstract metods
	public void onDailyUpdate() {
	}

	public void onHourlyUpdate(CivAsyncTask task) {
	}

	public void onMinuteUpdate() {
	}

	public void onSecondUpdate() {
	}

	public abstract void processUndo() throws CivException;

	public void build(Player player) throws CivException {
		this.checkBlockPermissionsAndRestrictions(player);
		try {
			this.getTemplate().saveUndoTemplate(this.getCorner().toString(), this.getCorner());
		} catch (IOException var8) {
			var8.printStackTrace();
		}
		this.getTemplate().buildTemplate(corner);
		this.bindBlocks();
		try {
			this.saveNow();
		} catch (SQLException var7) {
			var7.printStackTrace();
			throw new CivException("Internal SQL Error.");
		}
	}

	public abstract String getDynmapDescription();

	public abstract String getMarkerIconName();

	public abstract void onLoad() throws CivException;

	public abstract void onUnload();

	public boolean showOnDynmap() {
		return false;
	}

	public void updateSignText() {
	}

	public void onDemolish() throws CivException {
	}

	// ---------------- load
	@SuppressWarnings("deprecation")
	public void loadSettings() {
		/* Build and register all of the components. */
		if (this.getComponentInfoList() != null) {
			for (HashMap<String, String> compInfo : this.getComponentInfoList()) {
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
			comp.onLoad();
		}
	}

	public ConsumeLevelComponent getConsumeComponent() {
		ConsumeLevelComponent consumeComp = null;
		if (consumeComp == null) {
			consumeComp = (ConsumeLevelComponent) this.getComponent(ConsumeLevelComponent.class.getSimpleName());
		}
		return consumeComp;
	}

	public void bindBlocks() {
		/* Called mostly on a reload, determines which blocks should be protected based on the corner location and the template's size. We need to
		 * verify that each block is a part of the template. We might be able to restore broken/missing structures from here in the future. */
		Template tpl = this.getTemplate();
		if (tpl == null) return;
		if (isDestroyable()) return;
		Construct construct = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
				BlockCoord corner = construct.getCorner();
				for (int y = 0; y < tpl.size_y; y++) {
					for (int z = 0; z < tpl.size_z; z++) {
						for (int x = 0; x < tpl.size_x; x++) {
							SimpleBlock sb = tpl.blocks[x][y][z];
							if (sb.getType() == CivData.AIR) continue;
							if (sb.specialType == SimpleBlock.Type.COMMAND) continue;
							sbs.add(new SimpleBlock(corner, sb));

							BlockCoord bc = new BlockCoord(corner.getWorldname(), corner.getX() + x, corner.getY() + y, corner.getZ() + z);
							construct.addConstructBlock(new BlockCoord(bc), (y != 0));
						}
					}
				}
				/* Re-run the post build on the command blocks we found. */
				if (construct.isActive()) construct.postBuildSyncTask();
			}
		}, 100);
	}

	public void postBuildSyncTask() {
		Construct constr = this;
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				constr.processCommandSigns();
				constr.onPostBuild();
			}
		}, 10);
	}

	public abstract void onPostBuild();

	public void processCommandSigns() {
		Template tpl = this.getTemplate();
		if (!this.isPartOfAdminCiv()) {// если цива админская не ставим двери
			for (SimpleBlock sb : tpl.doorRelativeLocations) {
				Block block = this.getCorner().getBlock().getRelative(sb.getX(), sb.getY(), sb.getZ());
				if (ItemManager.getTypeId(block) != sb.getType()) {
					ItemManager.setTypeIdAndData(block, sb.getType(), (byte) sb.getData(), false);
				}
			}
		}
		for (SimpleBlock sb : tpl.attachableLocations) {
			Block block = this.getCorner().getBlock().getRelative(sb.getX(), sb.getY(), sb.getZ());
			if (ItemManager.getTypeId(block) != sb.getType()) {
				ItemManager.setTypeIdAndData(block, sb.getType(), (byte) sb.getData(), false);
				if (sb.getType() == CivData.WALL_SIGN || sb.getType() == CivData.SIGN) {
					Sign s2 = (Sign) block.getState();
					s2.setLine(0, sb.message[0]);
					s2.setLine(1, sb.message[1]);
					s2.setLine(2, sb.message[2]);
					s2.setLine(3, sb.message[3]);
					s2.update();
				}
			}
		}
		this.processValidateCommandBlockRelative();
		this.updateSignText();
	}

	/** Обрабатывает командные блоки в темплатете которые не вошли в список общих */
	public abstract void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb);

	public void processValidateCommandBlockRelative() {
		/* Use the location's of the command blocks in the template and the buildable's corner to find their real positions. Then perform any
		 * special building we may want to do at those locations. */
		/* These block coords do not point to a location in the world, just a location in the template. */
		Template tpl = this.getTemplate();
		for (SimpleBlock sb : tpl.commandBlockRelativeLocations) {
			Block block;
			BlockCoord absCoord = new BlockCoord(this.getCorner().getBlock().getRelative(sb.getX(), sb.getY(), sb.getZ()));

			/* Signs and chests should already be handled, look for more exotic things. */
			switch (sb.command) {
			case "/sign":
				ConstructSign structSign = CivGlobal.getConstructSign(absCoord);
				if (structSign == null) structSign = new ConstructSign(absCoord, this);
				block = absCoord.getBlock();
				ItemManager.setTypeId(block, sb.getType());
				ItemManager.setData(block, sb.getData());

				structSign.setDirection(ItemManager.getData(block.getState()));
				for (String key : sb.keyvalues.keySet()) {
					structSign.setType(key);
					structSign.setAction(sb.keyvalues.get(key));
				}
				structSign.setOwner(this);
				this.addConstructSign(structSign);
				CivGlobal.addConstructSign(structSign);
				break;
			case "/chest":
				ConstructChest structChest = CivGlobal.getConstructChest(absCoord);
				if (structChest == null) structChest = new ConstructChest(absCoord, this);
				structChest.setChestId(sb.keyvalues.get("id"));
				this.addChest(structChest);
				CivGlobal.addConstructChest(structChest);

				/* Convert sign data to chest data. */
				block = absCoord.getBlock();
				if (ItemManager.getTypeId(block) != CivData.CHEST) {
					byte chestData = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setTypeId(block, CivData.CHEST);
					ItemManager.setData(block, chestData, true);
				}
				Chest chest = (Chest) block.getState();
				MaterialData data = chest.getData();
				chest.setData(data);
				chest.update();
				break;
			default:
				this.commandBlockRelatives(absCoord, sb);
				break;
			}

			// switch (sb.command) {
			// case "/tradeoutpost" :
			// /* Builds the trade outpost tower at this location. */
			// if (this instanceof TradeOutpost) {
			// TradeOutpost outpost = (TradeOutpost) this;
			// outpost.setTradeOutpostTower(absCoord);
			// try {
			// outpost.build_trade_outpost_tower();
			// } catch (CivException e) {
			// e.printStackTrace();
			// }
			// }
			// break;
			// case "/control" :
			// if (!(this instanceof Neuschwanstein)) {
			// break;
			// }
			// if (this.getTown().hasStructure("s_capitol")) {
			// final Capitol capitol = (Capitol) this.getTown().getStructureByType("s_capitol");
			// capitol.createControlPoint(absCoord, "Neuschwanstein");
			// break;
			// }
			// if (this.getTown().hasStructure("s_townhall")) {
			// final TownHall townHall = (TownHall) this.getTown().getStructureByType("s_townhall");
			// townHall.createControlPoint(absCoord, "Neuschwanstein");
			// break;
			// }
			// break;
			// case "/towerfire" :
			// this.setTurretLocation(absCoord);
			// break;
			// case "/arrowfire" :
			// if (this instanceof GrandShipIngermanland) {
			// GrandShipIngermanland arrowtower = (GrandShipIngermanland) this;
			// arrowtower.setArrowLocation(absCoord);
			// }
			// break;
			// case "/cannonfire" :
			// if (this instanceof GrandShipIngermanland) {
			// GrandShipIngermanland cannontower = (GrandShipIngermanland) this;
			// cannontower.setCannonLocation(absCoord);
			// }
			// break;
			// }
		}
	}

	public void addConstructBlock(BlockCoord coord, boolean damageable) {
		CivGlobal.addConstructBlock(coord, this, damageable);
		// all we really need is it's key, we'll put in true to make sure this
		// structureBlocks collection is not abused.
		this.constructBlocks.put(coord, true);
	}

	// ------------------- delete
	@Override
	public void delete() {
		this.unbindConstructBlocks();
		this.setEnabled(false);
		for (Component comp : this.attachedComponents) {
			comp.destroyComponent();
		}
		Construct constr = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				for (ConstructChest chest : constr.getChests())
					chest.delete();
				for (ConstructSign sign : constr.getSigns()) {
					sign.delete();
				}
			}
		}, 10);
	}

	public void undoFromTemplate() throws IOException, CivException {
		String templatePath = Template.getUndoFilePath(this.getCorner().toString());
		File f = new File(templatePath);
		if (!f.exists()) {
			throw new CivException(CivSettings.localize.localizedString("internalIOException") + " " + CivSettings.localize.localizedString("FileNotFound") + " " + templatePath);
		}
		Construct constr = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				Template tpl;
				try {
					tpl = new Template(templatePath);
				} catch (IOException | CivException e) {
					e.printStackTrace();
					return;
				}
				BuildTemplateTask btt = new BuildTemplateTask(tpl, constr.getCorner());
				TaskMaster.asyncTask(btt, 0);
				while (!BuildTemplateTask.isFinished(btt)) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Template.deleteFilePath(templatePath);
			}
		}, 10);
	}

	public void unbindConstructBlocks() {
		for (BlockCoord coord : this.constructBlocks.keySet()) {
			CivGlobal.removeConstructBlock(coord);
		}
	}

	public void removeContructBlock(BlockCoord coord) {
		CivGlobal.removeConstructBlock(coord);
		// all we really need is it's key, we'll put in true to make sure this
		// structureBlocks collection is not abused.
		this.constructBlocks.remove(coord);
	}

	// ------------- ConstructSign
	public void addConstructSign(ConstructSign s) {
		this.сonstructSigns.put(s.getCoord(), s);
	}

	public Collection<ConstructSign> getSigns() {
		return this.сonstructSigns.values();
	}

	public ConstructSign getSign(BlockCoord coord) {
		return this.сonstructSigns.get(coord);
	}

	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
		CivLog.info("No Sign action for this buildable?:" + this.getDisplayName());
	}

	// ------------ ConstructChest
	public void addChest(ConstructChest chest) {
		this.сonstructChests.put(chest.getCoord(), chest);
	}

	public ArrayList<ConstructChest> getAllChestsById(String id) {
		ArrayList<ConstructChest> chests = new ArrayList<ConstructChest>();
		for (ConstructChest chest : this.сonstructChests.values()) {
			if (chest.getChestId().equalsIgnoreCase(id)) chests.add(chest);
		}
		return chests;
	}

	public ArrayList<ConstructChest> getAllChestsById(String[] ids) {
		final ArrayList<ConstructChest> chests = new ArrayList<ConstructChest>();
		for (final ConstructChest chest : this.сonstructChests.values()) {
			for (String i : ids) {
				if (chest.getChestId() == i && chest != null) chests.add(chest);
			}
		}
		return chests;
	}

	public Collection<ConstructChest> getChests() {
		return this.сonstructChests.values();
	}

	public Map<BlockCoord, ConstructChest> getAllChests() {
		return this.сonstructChests;
	}

	// --------------- Damage
	public int getDamagePercentage() {
		double percentage = (double) hitpoints / (double) this.getMaxHitPoints();
		percentage *= 100;
		return (int) percentage;
	}

	public void damage(int amount) {
		if (hitpoints == 0) return;
		hitpoints -= amount;

		if (hitpoints <= 0) {
			hitpoints = 0;
			onDestroy();
		}
	}

	public void onDestroy() {
		// can be overriden in subclasses.
		// CivMessage.global(CivSettings.localize.localizedString("var_buildable_destroyedAlert", this.getDisplayName(), this.getTown().getName()));
		this.hitpoints = 0;
		this.fancyDestroyConstructBlocks();
		this.save();
	}

	public void fancyDestroyConstructBlocks() {
		class SyncTask implements Runnable {
			@Override
			public void run() {
				for (BlockCoord coord : constructBlocks.keySet()) {

					for (ConstructChest chest : сonstructChests.values())
						CivGlobal.removeConstructChest(chest);
					for (final BlockCoord blockCoord : getConstructBlocks().keySet())
						CivGlobal.removeConstructBlock(blockCoord);
					for (final ConstructSign sign : сonstructSigns.values())
						CivGlobal.removeConstructSign(sign);

					if (ItemManager.getTypeId(coord.getBlock()) == CivData.AIR) continue;
					if (ItemManager.getTypeId(coord.getBlock()) == CivData.CHEST) continue;
					if (ItemManager.getTypeId(coord.getBlock()) == CivData.SIGN) continue;
					if (ItemManager.getTypeId(coord.getBlock()) == CivData.WALL_SIGN) continue;
					if (CivSettings.alwaysCrumble.contains(ItemManager.getTypeId(coord.getBlock()))) {
						ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
						continue;
					}

					Random rand = new Random();

					// Each block has a 70% chance to turn into Air
					if (rand.nextInt(100) <= 70) {
						ItemManager.setTypeId(coord.getBlock(), CivData.AIR);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}

					// Each block has a 30% chance to turn into gravel
					if (rand.nextInt(100) <= 30) {
						ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}

					// Each block has a 10% chance of starting a fire
					if (rand.nextInt(100) <= 10) {
						ItemManager.setTypeId(coord.getBlock(), CivData.FIRE);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}

					// Each block has a 0.1% chance of launching an explosion effect
					if (rand.nextInt(1000) <= 1) {
						FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED).withTrail().withFlicker().build();
						FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
						for (int i = 0; i < 3; i++) {
							try {
								fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		TaskMaster.syncTask(new SyncTask());
	}

	public abstract void onDamage(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit);

	public abstract void onDamageNotification(Player player, ConstructDamageBlock hit);

	public void processRegen() {
		if (!this.isActive()) {
			/* Do not regen non activate structures. */
			return;
		}

		int regenRate = this.getRegenRate();
		if (regenRate != 0) {
			if ((this.getHitpoints() != this.getMaxHitPoints()) && (this.getHitpoints() != 0)) {
				this.setHitpoints(this.getHitpoints() + regenRate);
				if (this.getHitpoints() > this.getMaxHitPoints()) this.setHitpoints(this.getMaxHitPoints());
			}
		}
	}

	/* Plays a fire effect on all of the structure blocks for this structure. */
	public void flashConstructBlocks() {
		World world = Bukkit.getWorld(this.getCorner().getWorldname());
		for (BlockCoord coord : constructBlocks.keySet()) {
			if (CivCraft.civRandom.nextDouble() < 0.3) world.playEffect(coord.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		}
	}

	/* SessionDB helpers */
	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDatabase().add(key, value, 0, 0, this.getId());
	}

	public boolean isPartOfAdminCiv() {
		return (this.getCiv() != null) && this.getCiv().isAdminCiv();
	}

	public boolean isActive() {
		return !isDestroyed() && isEnabled();
	}

	public boolean isDestroyed() {
		return (this.getMaxHitPoints() != 0) && (hitpoints == 0);
	}

	public boolean isCanRestoreFromTemplate() {
		return true;
	}

	public double modifyTransmuterChance(Double chance) {
		return chance;
	}

}
