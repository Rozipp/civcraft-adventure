/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveBuildCommand;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.wonders.GrandShipIngermanland;
import com.avrgaming.civcraft.structure.wonders.Neuschwanstein;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.TemplateStatic;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.threading.tasks.BuildUndoTask;
import com.avrgaming.civcraft.threading.tasks.UpdateTechBar;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.civcraft.village.Village;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.global.perks.Perk;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Buildable extends SQLObject {

	protected BlockCoord mobSpawnerCoord;

	private Town town;
	private BlockCoord corner;
	private ConfigBuildableInfo info = new ConfigBuildableInfo(); //Blank buildable info for buildables which do not have configs.
	private int hitpoints;

	public int builtBlockCount = 0;
	public int savedBlockCount = 0;
	private int totalBlockCount = 0;
	private boolean complete = false;
	protected boolean autoClaim = false;
	private boolean enabled = true;

	private String templateName;

	// Number of blocks to shift the structure away from us when built.
	public static final double SHIFT_OUT = 0;
	public static final int MIN_DISTANCE = 7;

	private Map<BlockCoord, StructureSign> structureSigns = new ConcurrentHashMap<BlockCoord, StructureSign>();
	private Map<BlockCoord, StructureChest> structureChests = new ConcurrentHashMap<BlockCoord, StructureChest>();
	/** служит замком для рецептов трасмутера, а так же испольжуеться во время проверки купленых рецептов */
	public HashMap<String, ReentrantLock> locks = new HashMap<>();

	/* Used to keep track of which blocks belong to this buildable so they can be removed when the buildable is removed. */
	protected Map<BlockCoord, Boolean> structureBlocks = new ConcurrentHashMap<BlockCoord, Boolean>();
	private Location centerLocation;

	// XXX this is a bad hack to get the townchunks to load in the proper order when saving asynchronously
	public ArrayList<TownChunk> townChunksToSave = new ArrayList<TownChunk>();
	public ArrayList<Component> attachedComponents = new ArrayList<Component>();

	private boolean valid = true;
	public static double validPercentRequirement = 0.8;
	public static HashSet<Buildable> invalidBuildables = new HashSet<Buildable>();
	public HashMap<Integer, BuildableLayer> layerValidPercentages = new HashMap<Integer, BuildableLayer>();
	public boolean validated = false;
	private String invalidReason = "";

	public static final double DEFAULT_HAMMERRATE = 1.0;
	public String invalidLayerMessage = "";

	public Civilization getCiv() {
		if (this.getTown() == null) return null;
		return this.getTown().getCiv();
	}

	public String getHash() {
		return corner.toString();
	}
	public String getConfigId() {
		return info.id;
	}
	public String getTemplateBaseName() {
		return info.template_base_name;
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

	public double getHammerCost() {
		double rate = 1;
		rate -= this.getTown().getBuffManager().getEffectiveDouble(Buff.RUSH);
		rate -= this.getTown().getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
		rate -= this.getTown().getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");

		return rate * info.hammer_cost;
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
	public boolean isActive() {
		return this.isComplete() && (this.isTownHall() || !isDestroyed()) && isEnabled();
	}
	public void setBuiltBlockCount(int builtBlockCount) {
		this.builtBlockCount = builtBlockCount;
		this.savedBlockCount = builtBlockCount;
	}
	public boolean isDestroyed() {
		return (hitpoints == 0) && (this.getMaxHitPoints() != 0);
	}
	public boolean isDestroyable() {
		return (info.destroyable != null) && (info.destroyable == true);
	}
	public double getBlocksPerHammer() {
		// no hammer cost should be instant...
		if (this.getHammerCost() == 0) return this.totalBlockCount;
		return this.totalBlockCount / this.getHammerCost();
	}
	public Map<BlockCoord, Boolean> getStructureBlocks() {
		return this.structureBlocks;
	}
	public boolean isAvailable() {
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
	public boolean hasTemplate() {
		return info.has_template;
	}
	public String getReplaceStructure() {
		return info.replace_structure;
	}

	public void onHourlyUpdate(CivAsyncTask task) {
	}
	public void onMinuteUpdate() {
	}
	public void onSecondUpdate() {
	}
	public abstract void processUndo() throws CivException;
	public abstract void updateBuildProgess();
	public abstract void build(Player player, Location centerLoc, Template tpl) throws Exception;
	public abstract String getDynmapDescription();
	public abstract String getMarkerIconName();
	protected abstract void runOnBuild(Location centerLoc, Template tpl) throws CivException;

	public void bindBuildableBlocks() {
		// Called mostly on a reload, determines which blocks should be protected based on the corner
		// location and the template's size. We need to verify that each block is a part of the template.
		// We might be able to restore broken/missing structures from here in the future.
		if (this instanceof Structure) {
			Structure struct = (Structure) this;
			TaskMaster.syncTask(new Runnable() {
				@Override
				public void run() {
					try {
						struct.onLoad();
					} catch (Exception e) {
						CivLog.error("-----ON LOAD EXCEPTION-----");
						if (struct != null) {
							CivLog.error("Structure:" + struct.getDisplayName());
							if (struct.getTown() != null) CivLog.error("Town:" + struct.getTown().getName());
						}
						CivLog.error(e.getMessage());
						e.printStackTrace();
					}
				}
			}, 2000);
		}
		try {
			Template tpl;
			try {
				if (!this.hasTemplate()) throw new CivException("Not has template");
				if (this.getTemplateName() == null) {
					CivLog.warning("structure:" + this.getDisplayName() + " did not have a template name set but says it needs one!");
					new CivException("Not has template");
				}
				tpl = TemplateStatic.getTemplate(this.getTemplateName(), null);
			} catch (CivException | IOException e) {
				e.printStackTrace();
				this.centerLocation = corner.getLocation();
				return;
			}
			this.centerLocation = corner.getLocation().add(tpl.size_x / 2, tpl.size_y / 2, tpl.size_z / 2);

			if (isDestroyable()) return;
			Buildable buildable = this;
			TaskMaster.syncTask(new Runnable() {
				@Override
				public void run() {
					for (int y = 0; y < tpl.size_y; y++) {
						for (int z = 0; z < tpl.size_z; z++) {
							for (int x = 0; x < tpl.size_x; x++) {
								int relx = getCorner().getX() + x;
								int rely = getCorner().getY() + y;
								int relz = getCorner().getZ() + z;
								BlockCoord coord = new BlockCoord(buildable.getCorner().getWorldname(), (relx), (rely), (relz));
								if (tpl.blocks[x][y][z].getType() == CivData.AIR) continue;
								if (tpl.blocks[x][y][z].specialType == SimpleBlock.Type.COMMAND) continue;
								if (buildable instanceof Village)
									((Village) buildable).addVillageBlock(coord);
								else
									buildable.addStructureBlock(coord, (y != 0));
							}
						}
					}

					/* Re-run the post build on the command blocks we found. */
					if (buildable.isPartOfAdminCiv())
						buildable.processValidateCommandBlockRelative(tpl);
					else
						if (buildable.isActive()) buildable.processCommandSigns(tpl);
				}
			}, 100);
		} catch (Exception e) {
			CivLog.error("-----ON LOAD EXCEPTION-----");
			if (this != null) {
				CivLog.error("Structure:" + this.getDisplayName());
				if (this.getTown() != null) CivLog.error("Town:" + this.getTown().getName());
			}
			CivLog.error(e.getMessage());
			e.printStackTrace();
		}
	}

	public void buildPlayerPreview(Player player, Location centerLoc) throws CivException, IOException {
		/* Look for any custom template perks and ask the player if they want to use them. */
		Resident resident = CivGlobal.getResident(player);
		ArrayList<Perk> perkList = this.getTown().getTemplatePerks(this, resident, this.info);
		ArrayList<Perk> personalUnboundPerks = resident.getUnboundTemplatePerks(perkList, this.info);
		if (perkList.size() != 0 || personalUnboundPerks.size() != 0) {
			/* Store the pending buildable. */
			resident.pendingBuildable = this;

			/* Build an inventory full of templates to select. */
			Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9);
			ItemStack infoRec = LoreGuiItem.build(CivSettings.localize.localizedString("buildable_lore_default") + " " + this.getDisplayName(),
					ItemManager.getMaterialId(Material.WRITTEN_BOOK), 0, CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
			inv.addItem(infoRec);

			for (Perk perk : perkList) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data, CivColor.Gold + "<Click To Build>",
						CivColor.Gray + "Provided by: " + CivColor.LightBlue + perk.provider);
				infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getIdent());
				inv.addItem(infoRec);
			}

			for (Perk perk : personalUnboundPerks) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), CivData.BEDROCK, perk.configPerk.data,
						CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"),
						CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound"),
						CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound2"),
						CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound3"),
						CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound4"),
						CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_unbound5"));
				infoRec = LoreGuiItem.setAction(infoRec, "ActivatePerk");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getIdent());

			}

			/* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
			player.openInventory(inv);
			return;
		}

		Template tpl = new Template();
		try {
			tpl.initTemplate(centerLoc, this);
		} catch (CivException | IOException e) {
			e.printStackTrace();
			throw e;
		}

		buildPlayerPreview(player, centerLoc, tpl);
	}

	public void buildPlayerPreview(Player player, Location centerLoc, Template tpl) throws CivException, IOException {
		centerLoc = repositionCenter(centerLoc, tpl.getDirection(), tpl.size_x, tpl.size_z);
		if (this.getReplaceStructure() != null) {
			Location loc = centerLoc.clone();
			Structure replaceStructure = this.getTown().getStructureByType(this.getReplaceStructure());
			if (replaceStructure == null) throw new CivException("не найдено здание " + this.getReplaceStructure() + " для замены");

			BlockCoord bc = replaceStructure.getCorner();
			centerLoc = new Location(loc.getWorld(), bc.getX(), bc.getY() + replaceStructure.getTemplateYShift(), bc.getZ());
			centerLoc.setDirection(loc.getDirection());
		}

		tpl.buildPreviewScaffolding(centerLoc, player);

		this.setCorner(new BlockCoord(centerLoc));

		CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildable_preview_heading"));
		CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt1"));
		CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt2"));
		Resident resident = CivGlobal.getResident(player);

//		if (!War.isWarTime() && CivSettings.showPreview) {
//			resident.startPreviewTask(tpl, centerLoc.getBlock(), player.getUniqueId());
//		}

		/* Run validation on position. */
		//validate(player, this, tpl, centerLoc, null);
		this.templateName = tpl.getFilepath();
		TaskMaster.asyncTask(new StructureValidator(player, this), 0);
		resident.setInteractiveMode(new InteractiveBuildCommand(this.getTown(), this, centerLoc, tpl));
	}

	/* This function is called before we build structures that do not have a town yet. This includes Capitols, Camps, and Town Halls. */

	public static void buildVerifyStatic(Player player, ConfigBuildableInfo info, Location centerLoc, CallbackInterface callback) throws CivException {

		Resident resident = CivGlobal.getResident(player);
		/* Look for any custom template perks and ask the player if they want to use them. */
		LinkedList<Perk> perkList = resident.getPersonalTemplatePerks(info);
		if (perkList.size() != 0) {

			/* Store the pending buildable. */
			resident.pendingBuildableInfo = info;
			resident.pendingCallback = callback;

			/* Build an inventory full of templates to select. */
			Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9);
			ItemStack infoRec = LoreGuiItem.build("Default " + info.displayName, ItemManager.getMaterialId(Material.WRITTEN_BOOK), 0,
					CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithDefaultPersonalTemplate");
			inv.addItem(infoRec);

			for (Perk perk : perkList) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data,
						CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"),
						CivColor.Gray + CivSettings.localize.localizedString("loreGui_template_providedBy") + " " + CivColor.LightBlue
								+ CivSettings.localize.localizedString("loreGui_template_Yourself"));
				infoRec = LoreGuiItem.setAction(infoRec, "BuildWithPersonalTemplate");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getIdent());
				inv.addItem(infoRec);
				player.openInventory(inv);
			}
			/* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
			return;
		}

		String path = TemplateStatic.getTemplateFilePath(info.template_base_name, TemplateStatic.getDirection(player.getLocation()), "structures", "default");

		Template tpl;
		try {
			tpl = TemplateStatic.getTemplate(path, player.getLocation());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		centerLoc = repositionCenterStatic(centerLoc, info, tpl.getDirection(), tpl.size_x, tpl.size_z);
		//validate(player, null, tpl, centerLoc, callback);
		TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, callback), 0);
	}

	public void undoFromTemplate() throws IOException, CivException {
		for (BuildAsyncTask task : this.getTown().build_tasks) {
			if (task.buildable == this) {
				task.abort();
			}
		}
		String filepath = "templates/undo/" + this.getTown().getName() + "/" + this.getCorner().toString();
		File f = new File(filepath);
		if (!f.exists()) {
			throw new CivException(
					CivSettings.localize.localizedString("internalIOException") + " " + CivSettings.localize.localizedString("FileNotFound") + " " + filepath);
		}
		BuildUndoTask task = new BuildUndoTask(filepath, this.getCorner().toString(), this.getCorner(), 0, this.getTown().getName());

		this.town.undo_tasks.add(task);
		BukkitObjects.scheduleAsyncDelayedTask(task, 0);
	}

	public void unbindStructureBlocks() {
		for (BlockCoord coord : this.structureBlocks.keySet()) {
			CivGlobal.removeStructureBlock(coord);
		}

	}

	/* XXX this is called only on structures which do not have towns yet. For Example Capitols, Camps and Town Halls. */
	public static Location repositionCenterStatic(Location center, ConfigBuildableInfo info, String dir, double x_size, double z_size) throws CivException {
		Location loc = center.clone();

		// Reposition tile improvements
		if (info.tile_improvement) {
			// just put the center at 0,0 of this chunk?
			loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
		} else {
			int xc = (int) (x_size - 1) / 16;
			int zc = (int) (z_size - 1) / 16;
			loc = loc.getChunk().getBlock(0, loc.getBlockY(), 0).getLocation();
			switch (dir.toLowerCase()) {
				case "west" :
					loc.setX(loc.getX() - xc * 16);
					loc.setZ(loc.getZ() - (zc + 1) / 2 * 16);
					break;
				case "north" :
					loc.setX(loc.getX() - xc / 2 * 16);
					loc.setZ(loc.getZ() - zc * 16);
					break;
				case "east" :
					loc.setX(loc.getX());
					loc.setZ(loc.getZ() - zc / 2 * 16);
					break;
				case "south" :
					loc.setX(loc.getX() - (xc + 1) / 2 * 16);
					loc.setZ(loc.getZ());
					break;
				default :
					break;
			}
		}
		if (info.templateYShift != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(loc.getY() + info.templateYShift);
			if (loc.getY() < 1) throw new CivException(CivSettings.localize.localizedString("buildable_TooCloseToBedrock"));
		}

		return loc;
	}

	protected Location repositionCenter(Location center, String dir, double x_size, double z_size) throws CivException {
		return repositionCenterStatic(center, this.getInfo(), dir, x_size, z_size);
	}

	public void resumeBuildFromTemplate() throws Exception {
		Template tpl;

		Location corner = getCorner().getLocation();

		try {
			tpl = new Template();
			tpl.resumeTemplate(this.getTemplateName(), this);
		} catch (Exception e) {
			throw e;
		}

		this.totalBlockCount = tpl.size_x * tpl.size_y * tpl.size_z;

		if (this instanceof Wonder) {
			this.getTown().setCurrentWonderInProgress(this);
		} else {
			this.getTown().setCurrentStructureInProgress(this);
		}

		this.startBuildTask(tpl, corner);
	}

	public static void validateDistanceFromSpawn(Location loc) throws CivException {
		/* Check distance from spawn. */
		double requiredDistance;
		try {
			requiredDistance = CivSettings.getDouble(CivSettings.civConfig, "global.distance_from_spawn");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		for (Civilization civ : CivGlobal.getAdminCivs()) {
			Location townHallLoc = civ.getCapitolTownHallLocation();
			if (townHallLoc == null) {
				continue;
			}

			double distance = townHallLoc.distance(loc);
			if (distance < requiredDistance) {
				throw new CivException(CivSettings.localize.localizedString("var_buildable_toocloseToSpawn1", requiredDistance));
			}

		}
	}

	protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ, Location origin)
			throws CivException {

		boolean foundTradeGood = false;
		TradeOutpost tradeOutpost = null;
		boolean ignoreBorders = false;
		boolean autoClaim = this.autoClaim;

		if (this instanceof TradeOutpost) {
			tradeOutpost = (TradeOutpost) this;
		}

		//Make sure we are building this building inside of culture.
		if (!isTownHall()) {
			CultureChunk cc = CivGlobal.getCultureChunk(centerBlock.getLocation());
			if (cc == null || cc.getTown().getCiv() != this.town.getCiv()) {
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
				CivMessage.sendError(player, CivSettings.localize.localizedString("internalException"));
				e.printStackTrace();
				return;
			}

			for (Town town : CivGlobal.getTowns()) {
				TownHall townhall = town.getTownHall();
				if (townhall == null) {
					continue;
				}

				double dist = townhall.getCenterLocation().distanceSquared(centerBlock.getLocation());
				if (dist < minDistance * minDistance) {
					DecimalFormat df = new DecimalFormat();
					CivMessage.sendError(player,
							CivSettings.localize.localizedString("var_settler_errorTooClose", town.getName(), df.format(Math.sqrt(dist)), minDistance));
					return;
				}
			}
		}

		if (this.getConfigId().equals("s_shipyard") || this.getConfigId().equals("s_arrowship") || this.getConfigId().equals("s_scoutship")
				|| this.getConfigId().equals("s_cannonship") || this.getConfigId().equals("ti_tradeship")
				|| this.getConfigId().equals("w_grand_ship_ingermanland")) {
			if (!centerBlock.getBiome().equals(Biome.OCEAN) && !centerBlock.getBiome().equals(Biome.BEACHES)
					&& !centerBlock.getBiome().equals(Biome.STONE_BEACH) && !centerBlock.getBiome().equals(Biome.COLD_BEACH)
					&& !centerBlock.getBiome().equals(Biome.DEEP_OCEAN) && !centerBlock.getBiome().equals(Biome.RIVER)
					&& !centerBlock.getBiome().equals(Biome.FROZEN_OCEAN) && !centerBlock.getBiome().equals(Biome.FROZEN_RIVER)) {
				throw new CivException(CivSettings.localize.localizedString("var_buildable_notEnoughWater", this.getDisplayName()));
			}
		}

		Structure struct = CivGlobal.getStructure(new BlockCoord(centerBlock));
		if (struct != null) {
			throw new CivException(CivSettings.localize.localizedString("buildable_structureExistsHere"));
		}

		ignoreBorders = this.isAllowOutsideTown();

		if (!player.isOp()) {
			validateDistanceFromSpawn(centerBlock.getLocation());
		}

		if (this.isTileImprovement()) {
			ignoreBorders = true;
			ConfigTownLevel level = CivSettings.townLevels.get(getTown().getLevel());

			Integer maxTileImprovements = level.tile_improvements;
			if (town.getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
				maxTileImprovements *= 2;
			}
			if (getTown().getTileImprovementCount() >= maxTileImprovements) {
				throw new CivException(CivSettings.localize.localizedString("buildable_errorTILimit"));
			}

			ChunkCoord coord = new ChunkCoord(centerBlock.getLocation());
			for (Structure s : getTown().getStructures()) {
				if (!s.isTileImprovement()) {
					continue;
				}
				ChunkCoord sCoord = new ChunkCoord(s.getCorner());
				if (sCoord.equals(coord)) {
					throw new CivException(CivSettings.localize.localizedString("buildable_errorTIHere"));
				}
			}

		}

		TownChunk centertc = CivGlobal.getTownChunk(origin);
		if (centertc == null && ignoreBorders == false) {
			throw new CivException(CivSettings.localize.localizedString("buildable_errorNotInTown"));
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

		/* Check that we're not overlapping with another structure's template outline. */
		/* XXX this needs to check actual blocks, not outlines cause thats more annoying than actual problems caused by building into each other. */
		//		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		//		while(iter.hasNext()) {
		//			Entry<BlockCoord, Structure> entry = iter.next();
		//			Structure s = entry.getValue();
		//			
		//			if (s.templateBoundingBox != null) {
		//				if (s.templateBoundingBox.overlaps(this.templateBoundingBox)) {
		//					throw new CivException("Cannot build structure here as it would overlap with a "+s.getDisplayName());
		//				}
		//			}
		//		}

		onCheck();

		LinkedList<RoadBlock> deletedRoadBlocks = new LinkedList<RoadBlock>();
		ArrayList<ChunkCoord> claimCoords = new ArrayList<ChunkCoord>();
		for (int x = 0; x < regionX; x++) {
			for (int y = 0; y < regionY; y++) {
				for (int z = 0; z < regionZ; z++) {
					Block b = centerBlock.getRelative(x, y, z);

					if (ItemManager.getTypeId(b) == CivData.CHEST) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));
					}

					TownChunk tc = CivGlobal.getTownChunk(b.getLocation());
					if (tc == null && autoClaim == true) {
						claimCoords.add(new ChunkCoord(b.getLocation()));
					}

					if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
						// Make sure we have permission to destroy any block in this area.
						throw new CivException(
								CivSettings.localize.localizedString("cannotBuild_needPermissions") + " " + b.getX() + "," + b.getY() + "," + b.getZ());
					}

					BlockCoord coord = new BlockCoord(b);
					ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());

					if (tradeOutpost == null) {
						//not building a trade outpost, prevent protected blocks from being destroyed.
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

					if (CivGlobal.getStructureBlock(coord) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
					}

					if (CivGlobal.getFarmChunk(new ChunkCoord(coord.getLocation())) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
					}

					if (CivGlobal.getWallChunk(chunkCoord) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));
					}

					if (CivGlobal.getVillageBlock(coord) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
					}

					if (CivGlobal.getBuildablesAt(coord) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureHere"));
					}

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

		if (tradeOutpost != null) {
			if (!foundTradeGood) {
				throw new CivException(CivSettings.localize.localizedString("buildable_errorNotOnTradeGood"));
			}
		}

		for (ChunkCoord c : claimCoords) {
			try {
				//XXX These will be added to the array list of objects to save in town.buildStructure();
				this.townChunksToSave.add(TownChunk.townHallClaim(this.getTown(), c));
			} catch (Exception e) {}
		}

		/* Delete any road blocks we happen to come across. */
		for (RoadBlock rb : deletedRoadBlocks) {
			rb.getRoad().deleteRoadBlock(rb);
		}
	}

	public void onCheck() throws CivException {
		/* Override in children */
	}

	public synchronized void buildRepairTemplate(Template tpl, Block centerBlock) {
		HashMap<Chunk, Chunk> chunkUpdates = new HashMap<Chunk, Chunk>();

		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block b = centerBlock.getRelative(x, y, z);
					if (tpl.blocks[x][y][z].specialType == Type.COMMAND) {
						ItemManager.setTypeIdAndData(b, CivData.AIR, (byte) 0, false);
					} else {
						ItemManager.setTypeIdAndData(b, tpl.blocks[x][y][z].getType(), (byte) tpl.blocks[x][y][z].getData(), false);
					}

					chunkUpdates.put(b.getChunk(), b.getChunk());

					if (ItemManager.getTypeId(b) == CivData.WALL_SIGN || ItemManager.getTypeId(b) == CivData.SIGN) {
						Sign s2 = (Sign) b.getState();
						s2.setLine(0, tpl.blocks[x][y][z].message[0]);
						s2.setLine(1, tpl.blocks[x][y][z].message[1]);
						s2.setLine(2, tpl.blocks[x][y][z].message[2]);
						s2.setLine(3, tpl.blocks[x][y][z].message[3]);
						s2.update();
					}
				}
			}
		}
	}

	protected void startBuildTask(Template tpl, Location center) {
		//CivBuildTask task = new CivBuildTask(TownyUniverse.getPlugin(), this, tpl, 
		//	this.getBuildSpeed(), this.getBlocksPerTick(), center.getBlock());
		if (this instanceof Structure) {
			this.getTown().setCurrentStructureInProgress(this);
		} else {
			this.getTown().setCurrentWonderInProgress(this);
		}
		BuildAsyncTask task = new BuildAsyncTask(this, tpl, this.getBlocksPerTick(), center.getBlock());

		this.town.build_tasks.add(task);
		TaskMaster.asyncTask(task, 10);
	}

	public int getBuildSpeed() {
		// buildTime is in hours, we need to return milliseconds.
		// We should return the number of milliseconds to wait between each block placement.
		double hoursPerBlock = (this.getHammerCost() / this.town.getHammers().total) / this.totalBlockCount;
		double millisecondsPerBlock = hoursPerBlock * 60 * 60 * 1000;
		// Clip millisecondsPerBlock to 500 milliseconds.
		if (millisecondsPerBlock < 500) {
			millisecondsPerBlock = 500;
		}

		return (int) millisecondsPerBlock;
	}

	public double getBuiltHammers() {
		double hoursPerBlock = (this.getHammerCost() / DEFAULT_HAMMERRATE) / this.totalBlockCount;
		return this.builtBlockCount * hoursPerBlock;
	}

	public int getBlocksPerTick() {
		// We do not want the blocks to be placed faster than 500 milliseconds.
		// So in order to deal with speeds that are faster than that, we will
		// increase the number of blocks given per tick. 
		double hoursPerBlock = (this.getHammerCost() / this.town.getHammers().total) / this.totalBlockCount;
		double millisecondsPerBlock = hoursPerBlock * 60 * 60 * 1000;

		// Dont let this get lower than 1 just in case to prevent any crazyiness...
		//if (millisecondsPerBlock < 1)
		//millisecondsPerBlock = 1;

		double blocks = (500 / millisecondsPerBlock);
		if (blocks < 1) blocks = 1;
		return (int) blocks;
	}

	public void addStructureSign(StructureSign s) {
		this.structureSigns.put(s.getCoord(), s);
	}

	public Collection<StructureSign> getSigns() {
		return this.structureSigns.values();
	}

	public StructureSign getSign(BlockCoord coord) {
		return this.structureSigns.get(coord);
	}

	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
		CivLog.info("No Sign action for this buildable?:" + this.getDisplayName());
	}

	public void addStructureChest(StructureChest chest) {
		this.structureChests.put(chest.getCoord(), chest);
	}

	public ArrayList<StructureChest> getAllChestsById(String id) {
		ArrayList<StructureChest> chests = new ArrayList<StructureChest>();
		for (StructureChest chest : this.structureChests.values()) {
			if (chest.getChestId().equalsIgnoreCase(id)) chests.add(chest);
		}
		return chests;
	}

	public ArrayList<StructureChest> getAllChestsById(String[] ids) {
		final ArrayList<StructureChest> chests = new ArrayList<StructureChest>();
		for (final StructureChest chest : this.structureChests.values()) {
			for (String i : ids) {
				if (chest.getChestId() == i && chest != null) {
					chests.add(chest);
				}
			}
		}
		return chests;
	}

	public Collection<StructureChest> getChests() {
		return this.structureChests.values();
	}
	public Map<BlockCoord, StructureChest> getAllChests() {
		return this.structureChests;
	}

	public void addStructureBlock(BlockCoord coord, boolean damageable) {
		//CivLog.debug("Added structure block:"+this);
		CivGlobal.addStructureBlock(coord, this, damageable);

		// all we really need is it's key, we'll put in true
		// to make sure this structureBlocks collection isnt
		// abused.
		this.structureBlocks.put(coord, true);

	}

	/* SessionDB helpers */
	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDB().add(key, value, this.getCiv().getId(), this.getTown().getId(), this.getId());
	}

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
		//can be overriden in subclasses.
		CivMessage.global(CivSettings.localize.localizedString("var_buildable_destroyedAlert", this.getDisplayName(), this.getTown().getName()));
		this.hitpoints = 0;
		this.fancyDestroyStructureBlocks();
		this.save();
	}

	public void onDamage(int amount, World world, Player player, BlockCoord coord, BuildableDamageBlock hit) {
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

		if (!hit.getOwner().isComplete() && !(hit.getOwner() instanceof Wonder)) {
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
				final TownHall townHall = (TownHall) this.getTown().getStructureByType("s_townhall");
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
				CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess",
						hit.getOwner().getDisplayName(), (hit.getOwner().hitpoints + "/" + hit.getOwner().getMaxHitPoints())));
			}
		}
	}

	public void onDamageNotification(Player player, BuildableDamageBlock hit) {
		CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(),
				(hit.getOwner().getDamagePercentage() + "%")));

		CivMessage.sendTown(hit.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildable_underAttackAlert",
				hit.getOwner().getDisplayName(), hit.getOwner().getCorner(), hit.getOwner().getDamagePercentage()));
	}

	public void fancyDestroyStructureBlocks() {

		class SyncTask implements Runnable {

			@Override
			public void run() {
				for (BlockCoord coord : structureBlocks.keySet()) {

					for (final StructureChest structureChests : structureChests.values())
						CivGlobal.removeStructureChest(structureChests);
					for (final BlockCoord blockCoord : getStructureBlocks().keySet())
						CivGlobal.removeStructureBlock(blockCoord);
					for (final StructureSign structureSign : structureSigns.values())
						CivGlobal.removeStructureSign(structureSign);

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
						FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED)
								.withTrail().withFlicker().build();
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

	public abstract void onComplete();
	public abstract void onLoad() throws CivException;
	public abstract void onUnload();

	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
	}

	public void onTechUpdate() {
	}

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

	/* Plays a fire effect on all of the structure blocks for this structure. */
	public void flashStructureBlocks() {
		World world = null;
		for (BlockCoord coord : structureBlocks.keySet()) {
			if (world == null) world = coord.getLocation().getWorld();
			world.playEffect(coord.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		}
	}

	public boolean showOnDynmap() {
		return true;
	}
	public void onDailyEvent() {
	}

	public void onPreBuild(Location centerLoc) throws CivException {
	}
	public void updateSignText() {
	}
	public void repairFromTemplate() throws IOException, CivException {
		Template tpl = TemplateStatic.getTemplate(this.getTemplateName(), null);
		this.buildRepairTemplate(tpl, this.getCorner().getBlock());
		this.postBuildSyncTask(tpl, 10);
	}

	public boolean isPartOfAdminCiv() {
		return (this.getCiv().isAdminCiv());
	}

	public boolean isTownHall() {
		return (this instanceof TownHall);
	}

	public void markInvalid() {
		if (this.getCiv().isAdminCiv()) {
			this.valid = true;
		} else {
			this.valid = false;
			this.getTown().invalidStructures.add(this);
		}
	}

	public boolean isValid() {
		if (this.getCiv().isAdminCiv()) return true;
		return valid;
	}

	public static int getBlockIDFromSnapshotMap(HashMap<ChunkCoord, ChunkSnapshot> snapshots, int absX, int absY, int absZ, String worldName)
			throws CivException {

		int chunkX = ChunkCoord.castToChunkX(absX);
		int chunkZ = ChunkCoord.castToChunkZ(absZ);

		int blockChunkX = absX % 16;
		int blockChunkZ = absZ % 16;

		if (blockChunkX < 0) blockChunkX += 16;
		if (blockChunkZ < 0) blockChunkZ += 16;

		ChunkCoord coord = new ChunkCoord(worldName, chunkX, chunkZ);

		ChunkSnapshot snapshot = snapshots.get(coord);
		if (snapshot == null)
			throw new CivException("Snapshot for chunk " + chunkX + ", " + chunkZ + " in " + worldName + " not found for abs:" + absX + "," + absZ);

		return ItemManager.getBlockTypeId(snapshot, blockChunkX, absY, blockChunkZ);
	}

	public static double getReinforcementRequirementForLevel(int level) {
		if (level > 10) return Buildable.validPercentRequirement * 0.3;
		if (level > 40) return Buildable.validPercentRequirement * 0.1;

		return Buildable.validPercentRequirement;
	}

	public void validate(Player player) throws CivException {
		TaskMaster.asyncTask(new StructureValidator(player, this), 0);
	}

	public void setValid(boolean b) {
		this.valid = (this.getCiv().isAdminCiv()) ? true : b;
	}

	public void onGoodieFromFrame() {
	}

	public void onGoodieToFrame() {
	}

	@Override
	public void delete() throws SQLException {
		this.setEnabled(false);
		for (Component comp : this.attachedComponents) {
			comp.destroyComponent();
		}
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

	public void loadSettings() {
	}

	public void onDemolish() throws CivException {
	}

	public static int getReinforcementValue(int typeId) {
		switch (typeId) {
			case CivData.WATER :
			case CivData.WATER_RUNNING :
			case CivData.LAVA :
			case CivData.LAVA_RUNNING :
			case CivData.AIR :
			case CivData.COBWEB :
				return 0;
			case CivData.IRON_BLOCK :
				return 4;
			case CivData.STONE_BRICK :
				return 3;
			case CivData.STONE :
				return 2;
			default :
				return 1;
		}
	}

	public boolean canRestoreFromTemplate() {
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
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupport", this.getDisplayName(),
				(centerLoc.getBlockX() + "," + centerLoc.getBlockY() + "," + centerLoc.getBlockZ())));
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildable_cannotSupportDamage",
				df.format(invalid_hourly_penalty * 100), (this.hitpoints + "/" + this.getMaxHitPoints())));
		CivMessage.sendTown(this.getTown(), CivColor.Rose + this.invalidLayerMessage);
		CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("buildable_validationPrompt"));
		this.save();

	}

	public void processValidateCommandBlockRelative(Template tpl) {
		/* Use the location's of the command blocks in the template and the buildable's corner to find their real positions. Then perform any special building
		 * we may want to do at those locations. */
		/* These block coords do not point to a location in the world, just a location in the template. */
		for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
			SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
			StructureSign structSign;
			Block block;
			BlockCoord absCoord = new BlockCoord(this.getCorner().getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

			/* Signs and chests should already be handled, look for more exotic things. */
			switch (sb.command) {
				case "/tradeoutpost" :
					/* Builds the trade outpost tower at this location. */
					if (this instanceof TradeOutpost) {
						TradeOutpost outpost = (TradeOutpost) this;
						outpost.setTradeOutpostTower(absCoord);
						try {
							outpost.build_trade_outpost_tower();
						} catch (CivException e) {
							e.printStackTrace();
						}
					}
					break;
				case "/techbar" :
					if (this instanceof TownHall) {
						TownHall townhall = (TownHall) this;
						int index = Integer.valueOf(sb.keyvalues.get("id"));
						townhall.addTechBarBlock(absCoord, index);
					}
					break;
				case "/techname" :
					if (this instanceof TownHall) {
						TownHall townhall = (TownHall) this;
						townhall.setTechnameSign(absCoord);
						townhall.setTechnameSignData((byte) sb.getData());

					}
					break;
				case "/techdata" :
					if (this instanceof TownHall) {
						TownHall townhall = (TownHall) this;
						townhall.setTechdataSign(absCoord);
						townhall.setTechdataSignData((byte) sb.getData());
					}
					break;
				case "/itemframe" :
					String strvalue = sb.keyvalues.get("id");
					if (strvalue != null) {
						int index = Integer.valueOf(strvalue);
						if (this instanceof TownHall) {
							TownHall townhall = (TownHall) this;
							townhall.createGoodieItemFrame(absCoord, index, sb.getData());
							townhall.addStructureBlock(absCoord, false);
						}
					}
					break;
				case "/respawn" :
					if (this instanceof TownHall) {
						TownHall townhall = (TownHall) this;
						townhall.setRespawnPoint(absCoord);
					}
					break;
				case "/revive" :
					if (this instanceof TownHall) {
						TownHall townhall = (TownHall) this;
						townhall.setRevivePoint(absCoord);
					}
					break;
				case "/control" :
					if (this instanceof TownHall) {
						TownHall townhall = (TownHall) this;
						townhall.createControlPoint(absCoord, "");
					}
					if (!(this instanceof Neuschwanstein)) {
						break;
					}
					if (this.getTown().hasStructure("s_capitol")) {
						final Capitol capitol = (Capitol) this.getTown().getStructureByType("s_capitol");
						capitol.createControlPoint(absCoord, "Neuschwanstein");
						break;
					}
					if (this.getTown().hasStructure("s_townhall")) {
						final TownHall townHall = (TownHall) this.getTown().getStructureByType("s_townhall");
						townHall.createControlPoint(absCoord, "Neuschwanstein");
						break;
					}
					break;
				case "/towerfire" :
					this.setTurretLocation(absCoord);
					break;
				case "/arrowfire" :
					if (this instanceof GrandShipIngermanland) {
						GrandShipIngermanland arrowtower = (GrandShipIngermanland) this;
						arrowtower.setArrowLocation(absCoord);
					}
					break;
				case "/cannonfire" :
					if (this instanceof GrandShipIngermanland) {
						GrandShipIngermanland cannontower = (GrandShipIngermanland) this;
						cannontower.setCannonLocation(absCoord);
					}
					break;
				case "/sign" :
					structSign = CivGlobal.getStructureSign(absCoord);
					if (structSign == null) {
						structSign = new StructureSign(absCoord, this);
					}
					block = absCoord.getBlock();
					ItemManager.setTypeId(block, sb.getType());
					ItemManager.setData(block, sb.getData());

					structSign.setDirection(ItemManager.getData(block.getState()));
					for (String key : sb.keyvalues.keySet()) {
						structSign.setType(key);
						structSign.setAction(sb.keyvalues.get(key));
						break;
					}

					structSign.setOwner(this);
					this.addStructureSign(structSign);
					CivGlobal.addStructureSign(structSign);

					break;
				case "/chest" :
					StructureChest structChest = CivGlobal.getStructureChest(absCoord);
					if (structChest == null) {
						structChest = new StructureChest(absCoord, this);
					}
					structChest.setChestId(sb.keyvalues.get("id"));
					this.addStructureChest(structChest);
					CivGlobal.addStructureChest(structChest);

					/* Convert sign data to chest data. */
					block = absCoord.getBlock();
					if (ItemManager.getTypeId(block) != CivData.CHEST) {
						byte chestData = CivData.convertSignDataToChestData((byte) sb.getData());
						ItemManager.setTypeId(block, CivData.CHEST);
						ItemManager.setData(block, chestData, true);
					}
//XXX походу фикс фурнекса по фиксу поворота сундуков после перезагрузки
// из за етого из сундуков выпадают предметы после перезагрузки
					Chest chest = (Chest) block.getState();
					MaterialData data = chest.getData();
//					ItemManager.setData(data, chestData);
					chest.setData(data);
					chest.update();

					break;
			}

			this.onPostBuild(absCoord, sb);
		}
	}
	public void setTurretLocation(BlockCoord absCoord) {
	}

	public void postBuildSyncTask(Template tpl, long delay) {
		Buildable buildable = this;
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				buildable.processCommandSigns(tpl);
			}
		}, delay);
	}

	public void processCommandSigns(Template tpl) {
		for (BlockCoord bc : tpl.doorRelativeLocations) {
			SimpleBlock sb = tpl.blocks[bc.getX()][bc.getY()][bc.getZ()];
			BlockCoord absCoord = new BlockCoord(this.getCorner().getBlock().getRelative(bc.getX(), bc.getY(), bc.getZ()));

			Block block = absCoord.getBlock();
			if (ItemManager.getTypeId(block) != sb.getType()) {
				if (this.getCiv() != null && this.getCiv().isAdminCiv()) {//TODO если цива админская не ставим двери
					ItemManager.setTypeIdAndData(block, CivData.AIR, (byte) 0, false);
				} else {
					ItemManager.setTypeIdAndData(block, sb.getType(), (byte) sb.getData(), false);
				}
			}
		}

		for (BlockCoord bc : tpl.attachableLocations) {
			SimpleBlock sb = tpl.blocks[bc.getX()][bc.getY()][bc.getZ()];
			BlockCoord absCoord = new BlockCoord(this.getCorner().getBlock().getRelative(bc.getX(), bc.getY(), bc.getZ()));

			Block block = absCoord.getBlock();

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

		this.processValidateCommandBlockRelative(tpl);

		/* Run the tech bar task now in order to protect the blocks */
		if (this instanceof TownHall) (new UpdateTechBar(this.getCiv())).run();
		if (this instanceof Village) ((Village) this).updateFirepit();
		if (this instanceof Structure) this.updateSignText();
	}

	public double modifyChance(Double chance) {
		return chance;
	}
	public ArrayList<String> getTransmuterRecipe() {
		return new ArrayList<String>();
	}
	public void rebiuldTransmuterRecipe() {
		this.locks.clear();
		for (String s : this.getTransmuterRecipe()) {
			if (CivSettings.transmuterRecipes.containsKey(s))
				this.locks.put(s, new ReentrantLock());
			else
				CivLog.error("not Found Transmuter Recipe - " + s);
		}
	}

}
