package com.avrgaming.civcraft.construct;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.construct.constructs.Template;
import com.avrgaming.civcraft.construct.constructvalidation.StructureValidator;
import com.avrgaming.civcraft.construct.structures.BuildableStatic;
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
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildTemplateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Construct extends SQLObject {

	protected BlockCoord corner;
	private Location centerLocation;
	private ConfigConstructInfo info;
	private int hitpoints;
	private Template template;
	private SQLObject SQLOwner;

	public String invalidLayerMessage = "";
	public boolean validated = false;
	protected boolean valid = false;
	public HashMap<Integer, ConstructLayer> layerValidPercentages = new HashMap<>();

	protected Map<BlockCoord, ConstructSign> constructSigns = new ConcurrentHashMap<>();
	private Map<String, ArrayList<ConstructChest>> constructChests = new ConcurrentHashMap<>();
	protected Map<BlockCoord, Boolean> constructBlocks = new ConcurrentHashMap<>();

	public ArrayList<Component> attachedComponents = new ArrayList<>();

	public Construct(String id, SQLObject owner) throws CivException {
		ConfigConstructInfo cInfo = CivSettings.constructs.get(id);
		this.setInfo(cInfo);
		this.setSQLOwner(owner);
		loadSettings();
	}

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
		return info.template_y_shift;
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
		return info.destroyable;
	}

	public boolean isAvailable() {
		if (this.getTown() == null || this.getTown().getId() == 0) return true;
		return info.isAvailable(this.getTown());
	}

	public int getLimit() {
		return info.limit;
	}

	public boolean isStrategic() {
		return info.strategic;
	}

	public boolean isIgnoreFloating() {
		return info.ignore_floating;
	}

	public Component getComponent(String name) {
		for (Component comp : this.attachedComponents) {
			if (name.equals(comp.getName())) return comp;
		}
		return null;
	}

	public void setTemplate(Template tpl) {
		if (this.getCorner() != null) {
			if (tpl == null)
				this.setCenterLocation(this.getCorner().getLocation());
			else
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
		return BuildableStatic.repositionCenterStatic(center, this.getTemplateYShift(), tpl);
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

		for (ChunkCoord chunkCoord : this.getChunksCoords()) {
			TownChunk tc = CivGlobal.getTownChunk(chunkCoord);
			if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
				// Make sure we have permission to destroy any block in this area.
				throw new CivException(CivSettings.localize.localizedString("cannotBuild_needPermissions"));
			}
			if (CivGlobal.getBuildableAt(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("buildable_structureExistsHere"));
			if (CivGlobal.getFarmChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
			if (!CivGlobal.getConstructsFromChunk(chunkCoord).isEmpty()) throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
		}

		int yTotal = 0;
		int yCount = 0;
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
				}
			}
		}
		double highestAverageBlock = (double) yTotal / (double) yCount;
		double floorLevel = getCorner().getY() - getTemplateYShift() - highestAverageBlock;
		if (floorLevel > 15 || floorLevel < -15) {
			throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		}
	}

	public void repairFromTemplate() {
		Template tpl = this.getTemplate();
		tpl.buildTemplate(getCorner());
		tpl.buildAirBlocks(getCorner());
		postBuild();
	}

	// ------------ abstract metods
	public void onDailyUpdate() {
	}

	public void onHourlyUpdate(CivAsyncTask task) {
	}

	public void onCivtickUpdate(CivAsyncTask task) {
	}

	public void onSecondUpdate(CivAsyncTask task) {
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
		this.getTemplate().buildAirBlocks(corner);
		this.postBuild();
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
		/* Override in children */
	}

	public void onDemolish() throws CivException {
	}

	// ---------------- load
	@SuppressWarnings("deprecation")
	public void loadSettings() {
		/* Build and register all of the components. */
		if (!info.components.isEmpty()) {
			for (HashMap<String, String> compInfo : info.components) {
				String className = "com.avrgaming.civcraft.components." + compInfo.get("name");
				try {
					Class<?> someClass = Class.forName(className);
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

		if (!this.attachedComponents.isEmpty()) {
			Construct construct = this;
			TaskMaster.syncTask(new Runnable() {
				// Посколько compClass.createComponent() создаеться в новом потоке, то нам надо подождать 2 секунды, перед загрузкой информации из ДБ
				@Override
				public void run() {
					for (Component comp : construct.attachedComponents) {
						comp.onLoad();
					}
				}
			}, 40);
		}
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
					for (SimpleBlock sb : tpl.blocks.get(y)) {
						if (sb.getType() == CivData.AIR) continue;
						if (sb.specialType == SimpleBlock.Type.COMMAND) continue;
						sbs.add(new SimpleBlock(corner, sb));

						BlockCoord bc = corner.getRelative(sb.getX(), y, sb.getZ());
						construct.addConstructBlock(new BlockCoord(bc), (y != 0));
					}
				}
			}
		}, 20);
	}

	public void postBuild() {
		CivLog.debug("postBuild");
		bindBlocks();
		if (this.isActive()) postBuildSyncTask();
	}

	public void postBuildSyncTask() {
		Construct constr = this;
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				constr.processCommandSigns();
				constr.onPostBuild();
			}
		}, TimeTools.toTicks(1));
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
				break;
			case "/chest":
				ConstructChest structChest = CivGlobal.getConstructChest(absCoord);
				if (structChest == null) structChest = new ConstructChest(absCoord, this);
				structChest.setChestId(sb.keyvalues.get("id"));
				this.addChest(structChest);

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
		}
	}

	public void addConstructBlock(BlockCoord coord, boolean damageable) {
		CivGlobal.addConstructBlock(coord, this, damageable);
		// all we really need is it's key, we'll put in true to make sure this
		// structureBlocks collection is not abused.
		this.constructBlocks.put(coord, true);
	}

	// ------------------- delete
	public void delete(boolean isUndo) {

	}

	@Override
	public void delete() {
		this.setDeleted(true);
		for (Component comp : this.attachedComponents) {
			comp.destroyComponent();
		}
		synchronized (constructChests) {
			for (String chestId : constructChests.keySet()) {
				for (ConstructChest chest : constructChests.get(chestId))
					chest.delete();
			}
			constructChests = null;
		}

		synchronized (this.constructSigns) {
			Set<BlockCoord> deleteObject = this.constructSigns.keySet();
			for (BlockCoord bcoord : deleteObject) {
				this.constructSigns.get(bcoord).delete();
				this.constructSigns.remove(bcoord);
			}
		}
	}

	public void deleteWithUndo() {
		try {
			this.undoFromTemplate();
		} catch (IOException | CivException e1) {
			e1.printStackTrace();
			this.fancyDestroyConstructBlocks();
		}
		this.delete();
	}

	public void deleteWithFancy() {
		this.fancyDestroyConstructBlocks();
		this.delete();
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
				BuildTemplateTask btt = new BuildTemplateTask(tpl, constr.getCorner(), true);
				TaskMaster.asyncTask(btt, 0);
				while (!BuildTemplateTask.isFinished(btt)) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				constr.unbindConstructBlocks();
				Template.deleteFilePath(templatePath);
			}
		}, 20);
	}

	public void fancyDestroyConstructBlocks() {
		String templatePath = Template.getUndoFilePath(this.getCorner().toString());
		Template.deleteFilePath(templatePath);
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				for (BlockCoord coord : constructBlocks.keySet()) {
					CivGlobal.removeConstructBlock(coord);

					if (ItemManager.getTypeId(coord.getBlock()) == CivData.AIR) continue;
					if (ItemManager.getTypeId(coord.getBlock()) == CivData.CHEST) continue;
					if (ItemManager.getTypeId(coord.getBlock()) == CivData.SIGN) continue;
					if (ItemManager.getTypeId(coord.getBlock()) == CivData.WALL_SIGN) continue;
					if (CivSettings.alwaysCrumble.contains(ItemManager.getTypeId(coord.getBlock()))) {
						ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
						continue;
					}

					double nextrand = CivCraft.civRandom.nextDouble();

					// Each block has a 0.1% chance of launching an explosion effect
					if (nextrand <= 0.002) {
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
					// Each block has a 10% chance of starting a fire
					if (nextrand <= 0.05) {
						ItemManager.setTypeId(coord.getBlock(), CivData.FIRE);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}
					// Each block has a 30% chance to turn into gravel
					if (nextrand <= 0.2) {
						ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}
					// Each block has a 70% chance to turn into Air
					if (nextrand <= 0.8) {
						ItemManager.setTypeId(coord.getBlock(), CivData.AIR);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}
				}
			}
		}, 100);
	}

	public void unbindConstructBlocks() {
		for (BlockCoord coord : this.constructBlocks.keySet()) {
			CivGlobal.removeConstructBlock(coord);
		}
	}

	// ------------- ConstructSign
	public void addConstructSign(ConstructSign sign) {
		this.constructSigns.put(sign.getCoord(), sign);
		CivGlobal.addConstructSign(sign);
	}

	public Collection<ConstructSign> getSigns() {
		return this.constructSigns.values();
	}

	public ConstructSign getSign(BlockCoord coord) {
		return this.constructSigns.get(coord);
	}

	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
		// Children override
	}

	// ------------ ConstructChest
	public void addChest(ConstructChest chest) {
		ArrayList<ConstructChest> chests = constructChests.get(chest.getChestId());
		if (chests == null) chests = new ArrayList<ConstructChest>();
		chests.add(chest);
		constructChests.put(chest.getChestId(), chests);
		// if (getTown() != null) getTown().addChest();
		CivGlobal.addConstructChest(chest);
	}

	public ArrayList<ConstructChest> getChestsById(String id) {
		ArrayList<ConstructChest> chests = constructChests.get(id);
		if (chests == null)
			return new ArrayList<ConstructChest>();
		else
			return chests;
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

	public abstract void onDamage(int amount, Player player, ConstructDamageBlock hit);

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
		World world = this.getCorner().getWorld();
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
		return !isDestroyed();
	}

	public boolean isDestroyed() {
		return (this.getMaxHitPoints() != 0) && (hitpoints == 0);
	}

	public boolean isCanRestoreFromTemplate() {
		return true;
	}

	public void validateAsyncTask(Player player) throws CivException {
		TaskMaster.asyncTask(new StructureValidator(player, this, null), 0);
	}

	public ArrayList<ChunkCoord> getChunksCoords() {
		ArrayList<ChunkCoord> ccs = new ArrayList<>();
		Template tpl = this.getTemplate();
		ChunkCoord cCorner = this.getCorner().getChunkCoord();
		int size_cx = ChunkCoord.castSizeInChunkSize(tpl.size_x);
		int size_cz = ChunkCoord.castSizeInChunkSize(tpl.size_z);
		CivLog.debug("size_cx = " + size_cx + "   size_cz = " + size_cz);
		for (int dx = 0; dx < size_cx; dx++) {
			for (int dz = 0; dz < size_cz; dz++) {
				ChunkCoord ccc = cCorner.getRelative(dx, dz);
				ccs.add(ccc);
			}
		}
		return ccs;
	}

}
