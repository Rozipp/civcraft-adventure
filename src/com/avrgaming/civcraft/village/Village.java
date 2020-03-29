package com.avrgaming.civcraft.village;

import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigVillageLonghouseLevel;
import com.avrgaming.civcraft.config.ConfigVillageUpgrade;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.components.Tagged;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.ConstructDamageBlock;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.ConstructBlock;
import com.avrgaming.civcraft.object.ConstructChest;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structure.RoadBlock;
import com.avrgaming.civcraft.structure.Construct;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TagManager;
import gpl.AttributeUtil;
import lombok.Getter;
import lombok.Setter;
import ua.rozipp.sound.SoundManager;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class Village extends Construct {
	public static final String TABLE_NAME = "VILLAGES";
	public static final double SHIFT_OUT = 2.0D;
	public static final String SUBDIR = "village";
	private static Integer coal_per_firepoint;
	private static Integer maxFirePoints;
	private static Integer maxHitPoints;
	private static int raidLength;

	private HashMap<String, Resident> members = new HashMap<String, Resident>();
	/** можно ли отменить установку лагеря */
	private boolean undoable = false;

	private String ownerName;
	private int firepoints;
	public HashMap<Integer, BlockCoord> firepitBlocks = new HashMap<Integer, BlockCoord>();
	public HashSet<BlockCoord> fireFurnaceBlocks = new HashSet<BlockCoord>();

	/* Transmuter */
	/** уровни пристроек в селе */
	private HashMap<String, Integer> annexLevel = new HashMap<>();

	/** Locations that exhibit vanilla growth */
	public HashSet<BlockCoord> growthLocations = new HashSet<BlockCoord>();

	/* Longhouse Stuff. */
	private ConsumeLevelComponent consumeComponent;
	public ReentrantLock lockLonghouse = new ReentrantLock();

	/* Doors we protect. */
//	public HashSet<BlockCoord> doors = new HashSet<BlockCoord>();
	/* Control blocks */
	public HashMap<BlockCoord, ControlPoint> controlBlocks = new HashMap<BlockCoord, ControlPoint>();
	private Date nextRaidDate;

	private HashMap<String, ConfigVillageUpgrade> upgrades = new HashMap<String, ConfigVillageUpgrade>();

	//-------------constructor
	public Village(Resident owner, String name, Location corner) throws CivException {
		this.ownerName = owner.getUid().toString();
		this.setSQLOwner(owner);
		this.corner = new BlockCoord(corner);
		try {
			this.setName(name);
		} catch (InvalidNameException var6) {
			throw new CivException("Плохое имя для деревни, выберите другое.");
		}
		this.nextRaidDate = new Date();
		this.nextRaidDate.setTime(this.nextRaidDate.getTime() + 86400000L);
		this.hitpoints = Village.maxHitPoints;
		this.loadSettings();
	}
	public Village(ResultSet rs) throws SQLException, CivException {
		this.load(rs);
		this.loadSettings();
	}

	//----------- dataBase
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" //
					+ "`id` int(11) unsigned NOT NULL auto_increment,"//
					+ "`name` VARCHAR(64) NOT NULL," //
					+ "`owner_name` mediumtext NOT NULL," //
					+ "`firepoints` int(11) DEFAULT 0," //
					+ "`next_raid_date` long,"//
					+ "`corner` mediumtext," //
					+ "`upgrades` mediumtext," //
					+ "`template_name` mediumtext," //
					+ "PRIMARY KEY (`id`)" + ")";
			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info("TABLE_NAME table OK!");
		}
	}
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("name", this.getName());
		hashmap.put("owner_name", this.getOwnerResident().getUid().toString());
		hashmap.put("firepoints", this.firepoints);
		hashmap.put("corner", this.corner.toString());
		hashmap.put("next_raid_date", this.nextRaidDate.getTime());
		hashmap.put("upgrades", this.getUpgradeSaveString());
		hashmap.put("template_name", this.getTemplate().getFilepath());
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}
	@Override
	public void load(ResultSet rs) throws SQLException, CivException {
		this.setId(rs.getInt("id"));
		try {
			this.setName(rs.getString("name"));
		} catch (InvalidNameException e) {
			throw new CivException("Плохое имя для лагеря id = " + getId());
		}
		this.ownerName = rs.getString("owner_name");
		if (this.ownerName == null) CivLog.error("COULD NOT FIND OWNER FOR VILLAGE ID:" + this.getId());
		this.corner = new BlockCoord(rs.getString("corner"));
		this.nextRaidDate = new Date(rs.getLong("next_raid_date"));
		this.setTemplate(Template.getTemplate(rs.getString("template_name")));
		this.firepoints = rs.getInt("firepoints");
		this.loadUpgradeString(rs.getString("upgrades"));
		this.bindBlocks();
	}
	public void loadUpgradeString(String upgrades) {
		String[] split = upgrades.split(",");
		for (String id : split) {
			if (id == null || id.equalsIgnoreCase("")) continue;
			id = id.trim();
			ConfigVillageUpgrade upgrade = CivSettings.villageUpgrades.get(id);
			if (upgrade == null) {
				CivLog.warning("Unknown upgrade id " + id + " during load.");
				continue;
			}
			this.upgrades.put(id, upgrade);
			if (annexLevel.getOrDefault(upgrade.annex, 0) < upgrade.level) annexLevel.put(upgrade.annex, upgrade.level);
			transmuterLocks = new HashMap<>();
			if (upgrade.transmuter_recipe != null) {
				for (String s : upgrade.transmuter_recipe) {
					this.transmuterLocks.put(s, new ReentrantLock());
				}
			}
			//upgrade.processAction(this);
		}
	}
	public void loadSettings() {
		this.hitpoints = Village.maxHitPoints;
		this.consumeComponent = new ConsumeLevelComponent();
		this.consumeComponent.setConstruct(this);
		for (ConfigVillageLonghouseLevel lvl : CivSettings.longhouseLevels.values()) {
			consumeComponent.addLevel(lvl.level, lvl.count);
			consumeComponent.setConsumes(lvl.level, lvl.consumes);
		}
		this.consumeComponent.onLoad();
	}
	public static void loadStaticSettings() {
		try {
			Village.coal_per_firepoint = CivSettings.getInteger(CivSettings.villageConfig, "village.coal_per_firepoint");
			Village.maxFirePoints = CivSettings.getInteger(CivSettings.villageConfig, "village.firepoints");
			Village.maxHitPoints = CivSettings.getInteger(CivSettings.villageConfig, "village.hitpoints");
			Village.raidLength = CivSettings.getInteger(CivSettings.villageConfig, "village.raid_length");
		} catch (InvalidConfiguration var7) {
			var7.printStackTrace();
		}
	}
	public String getUpgradeSaveString() {
		String out = "";
		for (ConfigVillageUpgrade upgrade : this.upgrades.values()) {
			out += upgrade.id + ",";
		}
		return out;
	}
	@Override
	public void delete() throws SQLException {
		TagManager.editNameTag(this);
		for (Resident resident : this.members.values()) {
			resident.setVillage((Village) null);
			resident.save();
		}
		this.unbindVillageBlocks();
		SQL.deleteNamedObject(this, TABLE_NAME);
		CivGlobal.removeVillage(this.getName());
	}

	//----------- get info
	public String getDisplayName() {
		return CivSettings.localize.localizedString("Village");
	}

	//----------- build
	public static void newVillage(Resident resident, Player player, String name) {
		TaskMaster.syncTask(new Runnable() {
			public void run() {
				try {
					Village existVillage = CivGlobal.getVillage(name);
					if (existVillage != null) throw new CivException("(" + name + ") " + CivSettings.localize.localizedString("village_nameTaken"));
					CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(player.getInventory().getItemInMainHand());
					if (craftMat == null || !craftMat.hasComponent("FoundVillage"))
						throw new CivException(CivSettings.localize.localizedString("village_missingItem"));

					Village village = new Village(resident, name, player.getLocation());
					village.build(player);
					village.setUndoable(true);
					CivGlobal.addVillage(village);
					village.save();
					CivMessage.sendSuccess((CommandSender) player, CivSettings.localize.localizedString("village_createSuccess"));
					player.getInventory().setItemInMainHand(new ItemStack(Material.AIR)); //TODO не правильно
					resident.clearInteractiveMode();
					TagManager.editNameTag(player);
				} catch (Exception var6) {
					CivMessage.sendError(player, var6.getMessage());
					var6.printStackTrace();
				}
			}
		});
	}
	@Override
	public void build(Player player) throws CivException {
		Location loc = player.getLocation();
		Resident resident = CivGlobal.getResident(player);
		Template tpl;
		if (resident.desiredTemplate != null) {
			tpl = resident.desiredTemplate;
			resident.desiredTemplate = null;
		} else {
			String templatePath = Template.getTemplateFilePath(SUBDIR, Template.getDirection(loc), "default");
			tpl = Template.getTemplate(templatePath);
			if (tpl == null) throw new CivException("Темплатет отсутствует");
		}
		this.setTemplate(tpl);
		this.corner.setFromLocation(this.repositionCenter(loc, tpl.getDirection(), (double) tpl.size_x, (double) tpl.size_z));
		this.setCenterLocation(this.getCorner().getLocation().add(tpl.size_x / 2, tpl.size_y / 2, tpl.size_z / 2));
		// Save the template x,y,z for later. This lets us know our own dimensions.
		// this is saved in the db so it remains valid even if the template changes.

		this.checkBlockPermissionsAndRestrictions(player);

		try {
			tpl.saveUndoTemplate(this.getCorner().toString(), this.getCorner());
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
		this.addMember(resident);
		resident.save();
		SoundManager.playSound("villageCreation", loc);
	}
	protected Location repositionCenter(Location center, String dir, double x_size, double z_size) throws CivException {
		Location loc = new Location(center.getWorld(), center.getX(), center.getY(), center.getZ(), center.getYaw(), center.getPitch());
		if (dir.equalsIgnoreCase("east")) {
			loc.setZ(loc.getZ() - z_size / 2);
			loc.setX(loc.getX() + SHIFT_OUT);
			return loc;
		}
		if (dir.equalsIgnoreCase("west")) {
			loc.setZ(loc.getZ() - z_size / 2);
			loc.setX(loc.getX() - (SHIFT_OUT + x_size));
			return loc;
		}
		if (dir.equalsIgnoreCase("north")) {
			loc.setX(loc.getX() - x_size / 2);
			loc.setZ(loc.getZ() - (SHIFT_OUT + z_size));
			return loc;
		}
		if (dir.equalsIgnoreCase("south")) {
			loc.setX(loc.getX() - x_size / 2);
			loc.setZ(loc.getZ() + SHIFT_OUT);
			return loc;
		}
		return loc;

	}
	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		Block block = this.getCorner().getBlock();
		int sizeX = this.getTemplate().getSize_x();
		int sizeY = this.getTemplate().getSize_y();
		int sizeZ = this.getTemplate().getSize_z();
		
		ChunkCoord ccoord = new ChunkCoord(block.getLocation());
		if (CivGlobal.getCultureChunk(ccoord) != null) throw new CivException(CivSettings.localize.localizedString("village_checkInCivError"));
		if (block.getY() >= 200.0D) throw new CivException(CivSettings.localize.localizedString("village_checkTooHigh"));
		if (sizeY + block.getY() >= 255) throw new CivException(CivSettings.localize.localizedString("village_checkWayTooHigh"));
		if (block.getY() < CivGlobal.minBuildHeight)
			throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		if (!player.isOp()) BuildableStatic.validateDistanceFromSpawn(block.getLocation());

		int yTotal = 0;
		int yCount = 0;
		LinkedList<RoadBlock> deletedRoadBlocks = new LinkedList<RoadBlock>();

		for (int x = 0; x < sizeX; x++) {
			for (int y = 0; y < sizeY; y++) {
				for (int z = 0; z < sizeZ; z++) {
					Block b = block.getRelative(x, y, z);
					if (ItemManager.getTypeId(b) == CivData.CHEST) throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));

					BlockCoord coord = new BlockCoord(b);
					ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());
					TownChunk tc = CivGlobal.getTownChunk(chunkCoord);
					if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
						throw new CivException(
								CivSettings.localize.localizedString("cannotBuild_needPermissions") + " " + b.getX() + "," + b.getY() + "," + b.getZ());
					}
					if (CivGlobal.getProtectedBlock(coord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_protectedInWay"));
					if (CivGlobal.getConstructBlock(coord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
					if (CivGlobal.getFarmChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
					if (CivGlobal.getWallChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));

					yTotal += b.getWorld().getHighestBlockYAt(block.getX() + x, block.getZ() + z);
					++yCount;
					RoadBlock rb = CivGlobal.getRoadBlock(coord);
					if (CivGlobal.getRoadBlock(coord) != null) {
						/* XXX Special case. Since road blocks can be built in wilderness we don't want people griefing with them. Building a structure over a
						 * road block should always succeed. */
						deletedRoadBlocks.add(rb);
					}
				}
			}
		}

		/* Delete any roads that we're building over. */
		for (RoadBlock roadBlock : deletedRoadBlocks) {
			roadBlock.getRoad().deleteRoadBlock(roadBlock);
		}

		double highestAverageBlock = (double) yTotal / (double) yCount;

		if (((block.getY() > (highestAverageBlock + 10)) || (block.getY() < (highestAverageBlock - 10)))) {
			throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		}

	}
	public void unbindVillageBlocks() {
		for (BlockCoord bcoord : this.constructBlocks.keySet()) {
			CivGlobal.removeConstructBlock(bcoord);
		}
	}

	//------------ delete
	public void destroy() {
		this.fancyVillageBlockDestory();
		try {
			this.delete();
		} catch (SQLException var2) {
			var2.printStackTrace();
		}
	}
	public void disband() {
		SoundManager.playSound("villageDestruction", this.getCenterLocation());
		try {
			this.undoFromTemplate();
		} catch (IOException | CivException e) {
			this.fancyDestroyStructureBlocks();
		}
		try {
			this.delete();
		} catch (SQLException var2) {
			var2.printStackTrace();
		}
	}
	public void undo() {
		SoundManager.playSound("villageDestruction", this.getCenterLocation());
		try {
			this.undoFromTemplate();
		} catch (IOException | CivException e) {
			this.fancyDestroyStructureBlocks();
		}
		try {
			this.delete();
		} catch (SQLException var2) {
			var2.printStackTrace();
		}
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
	}
	@Override
	public void processValidateCommandBlockRelative() {
		/* Use the location's of the command blocks in the template and the buildable's corner to find their real positions. Then perform any special building
		 * we may want to do at those locations. */
		/* These block coords do not point to a location in the world, just a location in the template. */
		Template tpl = this.getTemplate();
		for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
			SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
			BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));
			int level = 0;
			String annex = null;
			String chestId = null;
			switch (sb.command) {
				case "/sign" :
					if (sb.keyvalues.containsKey("level")) level = Integer.parseInt(sb.keyvalues.get("level"));
					annex = sb.keyvalues.get("annex");
					if (annexLevel.getOrDefault(annex, 0) < level) {
						ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.SIGN_POST));
						ItemManager.setData(absCoord.getBlock(), sb.getData());
						Sign sign = (Sign) absCoord.getBlock().getState();
						sign.setLine(0, CivSettings.localize.localizedString("village_growthUpgradeSign", level));
						sign.setLine(1, CivSettings.localize.localizedString("upgradeUsing_SignText"));
						sign.setLine(2, "/village upgrade");
						sign.setLine(3, "");
						sign.update();
						this.addConstructBlock(absCoord, false);
					} else {
						ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.AIR));
						this.removeBuildableBlock(absCoord);
					}
					break;
				case "/growth" :
					if (sb.keyvalues.containsKey("level")) level = Integer.parseInt(sb.keyvalues.get("level"));
					annex = sb.keyvalues.get("annex");
					Block b = absCoord.getBlock();
					if (annexLevel.getOrDefault(annex, 0) >= level) {
						this.growthLocations.add(absCoord);
						CivGlobal.vanillaGrowthLocations.add(absCoord);
						if (ItemManager.getTypeId(b) != CivData.FARMLAND) ItemManager.setTypeId(b, CivData.FARMLAND);
						this.addConstructBlock(absCoord, false);
					} else {
						ItemManager.setTypeId(b, CivData.COBBLESTONE);
						this.addConstructBlock(absCoord, false);
					}
					break;
				case "/chest" :
					if (sb.keyvalues.containsKey("level")) level = Integer.parseInt(sb.keyvalues.get("level"));
					annex = sb.keyvalues.get("annex");
					chestId = sb.keyvalues.get("id");
					//пример 21:7:21,63:12,/chest,id:SecondSawmillResult,level:2,annex:sawmill,
					if (annexLevel.getOrDefault(annex, 0) >= level) {
						ConstructChest structChest = CivGlobal.getConstructChest(absCoord);
						if (structChest == null) structChest = new ConstructChest(absCoord, this);
						structChest.setChestId(chestId);
						this.addConstructChest(structChest);
						CivGlobal.addConstructChest(structChest);

						ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.CHEST));
						byte data2 = CivData.convertSignDataToChestData((byte) sb.getData());
						ItemManager.setData(absCoord.getBlock(), data2);

					} else {
						try {
							ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.SIGN_POST));
							ItemManager.setData(absCoord.getBlock(), sb.getData());
							Sign sign = (Sign) absCoord.getBlock().getState();
							sign.setLine(0, CivSettings.localize.localizedString("village_" + annex + "UpgradeSign", level));
							sign.setLine(1, CivSettings.localize.localizedString("upgradeUsing_SignText"));
							sign.setLine(2, "/village upgrade");
							sign.setLine(3, "");
							sign.update();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					this.addConstructBlock(absCoord, false);
					break;
				case "/firepit" :
					this.firepitBlocks.put(Integer.valueOf(sb.keyvalues.get("id")), absCoord);
					this.addConstructBlock(absCoord, false);
					break;
				case "/fire" :
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.FIRE));
					break;
				case "/firefurnace" :
					this.fireFurnaceBlocks.add(absCoord);
					byte data = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.FURNACE));
					ItemManager.setData(absCoord.getBlock(), data);
					this.addConstructBlock(absCoord, false);
					break;
				case "/control" :
					this.createControlPoint(absCoord, "");
					break;
				case "/literal" :
					/* Unrecognized command... treat as a literal sign. */
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
					ItemManager.setData(absCoord.getBlock(), sb.getData());

					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, sb.message[0]);
					sign.setLine(1, sb.message[1]);
					sign.setLine(2, sb.message[2]);
					sign.setLine(3, sb.message[3]);
					sign.update();
					this.addConstructBlock(absCoord, false);
					break;
			}
		}
		this.updateFirepit();
	}

	public void updateFirepit() {
		try {
			int maxFirePoints = CivSettings.getInteger(CivSettings.villageConfig, "village.firepoints");
			int totalFireBlocks = this.firepitBlocks.size();
			double percentLeft = (double) this.firepoints / (double) maxFirePoints;
			int litFires = (int) (percentLeft * (double) totalFireBlocks);

			for (int i = 0; i < totalFireBlocks; ++i) {
				BlockCoord next = this.firepitBlocks.get(i);
				if (next == null) {
					CivLog.warning("Couldn't find firepit id:" + i);
					continue;
				}
				if (i < litFires) {
					ItemManager.setTypeId(next.getBlock(), CivData.FIRE);
				} else {
					ItemManager.setTypeId(next.getBlock(), CivData.AIR);
				}
			}
		} catch (InvalidConfiguration var8) {
			var8.printStackTrace();
		}

	}

	public void processFirepoints() {
		MultiInventory mInv = new MultiInventory();
		for (BlockCoord bcoord : this.fireFurnaceBlocks) {
			Furnace furnace = (Furnace) bcoord.getBlock().getState();
			mInv.addInventory((Inventory) furnace.getInventory());
		}
		if (mInv.contains(null, CivData.COAL, (short) 0, Village.coal_per_firepoint)) {
			try {
				mInv.removeItem(CivData.COAL, Village.coal_per_firepoint, true);
			} catch (CivException var4) {
				var4.printStackTrace();
			}

			this.firepoints++;
			if (this.firepoints > Village.maxFirePoints) {
				this.firepoints = maxFirePoints;
			}
		} else {
			this.firepoints--;
			CivMessage.sendVillage(this, "§e" + CivSettings.localize.localizedString("var_village_villagefireDown", this.firepoints));
			final double percentLeft = this.firepoints / (double) Village.maxFirePoints;
			if (percentLeft < 0.3) {
				CivMessage.sendVillage(this, "§e" + ChatColor.BOLD + CivSettings.localize.localizedString("village_villagefire30percent"));
			}
			if (this.firepoints < 0) {
				this.destroy();
			}
		}
		this.save();
		this.updateFirepit();
	}

	public void processLonghouse(CivAsyncTask task) {
		int level = annexLevel.getOrDefault("longhouse", 0);
		if (level == 0) return;
		MultiInventory mInv = new MultiInventory();

		ArrayList<ConstructChest> chests = this.getAllChestsById("longhouse1");
		// Make sure the chunk is loaded and add it to the inventory.
		for (ConstructChest c : chests) {
			Block b = c.getCoord().getBlock();
			Chest chest = (Chest) b.getState();
			mInv.addInventory(chest.getBlockInventory());
		}
		if (mInv.getInventoryCount() == 0) {
			CivMessage.sendVillage(this, "§c" + CivSettings.localize.localizedString("village_longhouseNoChest"));
			return;
		}
		this.consumeComponent.setSource(mInv);
		Result result = this.consumeComponent.processConsumption(true);
		this.consumeComponent.onSave();
		switch (result) {
			case STARVE :
				CivMessage.sendVillage(this,
						CivColor.LightGreen + CivSettings.localize.localizedString("var_village_yourLonghouseDown",
								(CivColor.Rose + CivSettings.localize.localizedString("var_village_longhouseStarved", consumeComponent.getCountString())
										+ CivColor.LightGreen),
								CivSettings.CURRENCY_NAME));
				return;
			case LEVELDOWN :
				CivMessage.sendVillage(this,
						CivColor.LightGreen + CivSettings.localize.localizedString("var_village_yourLonghouseDown",
								(CivColor.Rose + CivSettings.localize.localizedString("village_longhouseStavedAndLeveledDown") + CivColor.LightGreen),
								CivSettings.CURRENCY_NAME));
				return;
			case STAGNATE :
				CivMessage.sendVillage(this,
						CivColor.LightGreen + CivSettings.localize.localizedString("var_village_yourLonghouseDown",
								(CivColor.Yellow + CivSettings.localize.localizedString("village_longhouseStagnated") + CivColor.LightGreen),
								CivSettings.CURRENCY_NAME));
				return;
			case UNKNOWN :
				CivMessage.sendVillage(this,
						CivColor.LightGreen + CivSettings.localize.localizedString("var_village_yourLonghouseDown",
								(CivColor.Purple + CivSettings.localize.localizedString("village_longhouseSomethingUnknown") + CivColor.LightGreen),
								CivSettings.CURRENCY_NAME));
				return;
			default :
				break;
		}

		ConfigVillageLonghouseLevel lvl = null;
		if (result == Result.LEVELUP) {
			lvl = CivSettings.longhouseLevels.get(consumeComponent.getLevel() - 1);
		} else {
			lvl = CivSettings.longhouseLevels.get(consumeComponent.getLevel());
		}

		double total_coins = lvl.coins;
		this.getOwnerResident().getTreasury().deposit(total_coins);

		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial("mat_token_of_leadership");
		if (craftMat != null) {
			ItemStack token = CraftableCustomMaterial.spawn(craftMat);

			Tagged tag = (Tagged) craftMat.getComponent("Tagged");
			Resident res = CivGlobal.getResident(this.getOwnerName());

			token = tag.addTag(token, res.getUid().toString());

			AttributeUtil attrs = new AttributeUtil(token);
			attrs.addLore(CivColor.LightGray + res.getName());
			token = attrs.getStack();

			mInv.addItems(token, true);
		}

		String stateMessage = "";
		switch (result) {
			case GROW :
				stateMessage = CivColor.Green
						+ CivSettings.localize.localizedString("var_village_longhouseGrew", consumeComponent.getCountString() + CivColor.LightGreen);
				break;
			case LEVELUP :
				stateMessage = CivColor.Green + CivSettings.localize.localizedString("village_longhouselvlUp") + CivColor.LightGreen;
				break;
			case MAXED :
				stateMessage = CivColor.Green
						+ CivSettings.localize.localizedString("var_village_longhouseIsMaxed", consumeComponent.getCountString() + CivColor.LightGreen);
				break;
			default :
				break;
		}

		CivMessage.sendVillage(this,
				CivColor.LightGreen + CivSettings.localize.localizedString("var_village_yourLonghouse", stateMessage, total_coins, CivSettings.CURRENCY_NAME));

		if (level == 2) {
			MultiInventory mInv2 = new MultiInventory();
			ArrayList<ConstructChest> chests2 = this.getAllChestsById("longhouse2");
			// Make sure the chunk is loaded and add it to the inventory.
			for (ConstructChest c : chests2) {
				Block b = c.getCoord().getBlock();
				Chest chest = (Chest) b.getState();
				mInv2.addInventory(chest.getBlockInventory());
			}

			try {
				mInv2.removeItem(CivData.BREAD, CivSettings.villageConfig.getInt("village.bread_per_hour_for_second_longhouse"), true);
			} catch (CivException e) {
				return;
			}
			CraftableCustomMaterial craftMat2 = CraftableCustomMaterial.getCraftableCustomMaterial("mat_token_of_leadership");
			if (craftMat2 != null) {
				ItemStack token = CraftableCustomMaterial.spawn(craftMat2);

				Tagged tag = (Tagged) craftMat2.getComponent("Tagged");
				Resident res = CivGlobal.getResident(this.getOwnerName());

				token = tag.addTag(token, res.getUid().toString());

				AttributeUtil attrs = new AttributeUtil(token);
				attrs.addLore(CivColor.LightGray + res.getName());
				token = attrs.getStack();

				mInv2.addItems(token, true);
			}
		}
	}

	public void addMember(Resident resident) {
		this.members.put(resident.getName(), resident);
		resident.setVillage(this);
		resident.save();
	}
	public void removeMember(Resident resident) {
		this.members.remove(resident.getName());
		resident.setVillage((Village) null);
		resident.save();
	}
	public Resident getMember(String name) {
		return (Resident) this.members.get(name);
	}
	public boolean hasMember(String name) {
		return this.members.containsKey(name);
	}

//	public Resident getOwner() {
//		return CivGlobal.getResidentViaUUID(UUID.fromString(this.ownerName));
//	}
//	public void setOwner(Resident owner) {
//		this.ownerName = owner.getUid().toString();
//	}

	public void fancyVillageBlockDestory() {
		for (BlockCoord coord : this.getConstructBlocks().keySet()) {
			if (CivGlobal.getConstructChest(coord) != null) continue;
			if (CivGlobal.getConstructSign(coord) != null) continue;
			if (ItemManager.getTypeId(coord.getBlock()) == CivData.CHEST) continue;
			if (ItemManager.getTypeId(coord.getBlock()) == CivData.SIGN) continue;
			if (ItemManager.getTypeId(coord.getBlock()) == CivData.WALL_SIGN) continue;

			if (CivSettings.alwaysCrumble.contains(ItemManager.getTypeId(coord.getBlock()))) {
				ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
				continue;
			}
			Random rand = new Random();

			// Each block has a 10% chance to turn into gravel
			if (rand.nextInt(100) <= 10) {
				ItemManager.setTypeId((Block) coord.getBlock(), 13);
				continue;
			}

			// Each block has a 50% chance of starting a fire
			if (rand.nextInt(100) <= 50) {
				ItemManager.setTypeId((Block) coord.getBlock(), 51);
				continue;
			}

			// Each block has a 1% chance of launching an explosion effect
			if (rand.nextInt(100) <= 1) {
				FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED)
						.withTrail().withFlicker().build();
				FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();

				for (int i = 0; i < 3; ++i) {
					try {
						fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
					} catch (Exception var8) {
						var8.printStackTrace();
					}
				}
			}
		}
	}

	public void createControlPoint(BlockCoord absCoord, String info) {
		Location centerLoc = absCoord.getLocation();

		/* Build the bedrock tower. */
		Block b = centerLoc.getBlock();
		ItemManager.setTypeIdAndData(b, CivData.FENCE, 0, true);

		ConstructBlock sb = new ConstructBlock(new BlockCoord(b), this);
		this.addConstructBlock(sb.getCoord(), true);

		/* Build the control block. */
		b = centerLoc.getBlock().getRelative(0, 1, 0);
		ItemManager.setTypeId(b, CivData.OBSIDIAN);
		sb = new ConstructBlock(new BlockCoord(b), this);
		this.addConstructBlock(sb.getCoord(), true);

		int villageControlHitpoints;
		try {
			villageControlHitpoints = CivSettings.getInteger(CivSettings.warConfig, "war.control_block_hitpoints_village");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			villageControlHitpoints = 100;
		}

		BlockCoord coord = new BlockCoord(b);
		this.controlBlocks.put(coord, new ControlPoint(coord, this, villageControlHitpoints, info));
	}

	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDatabase().add(key, value, 0, 0, 0);
	}

	//XXX TODO make sure these all work...
	@Override
	public void processUndo() throws CivException {
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return null;
	}

	@Override
	public void onLoad() {
	}

	@Override
	public void onUnload() {
	}

	public Collection<Resident> getMembers() {
		return this.members.values();
	}

	public String getOwnerName() {
		Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(this.ownerName));
		return res.getName();
	}

	public int getLonghouseLevel() {
		return this.consumeComponent.getLevel();
	}

	public String getLonghouseCountString() {
		return this.consumeComponent.getCountString();
	}

	public String getMembersString() {
		String out = "";
		for (Resident resident : members.values()) {
			out += resident.getName() + " ";
		}
		return out;
	}

	public void onControlBlockHit(ControlPoint cp, World world, Player player) {
		world.playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2F, 1.0F);
		world.playEffect(cp.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		CivMessage.send((Object) player, (String) ("§7" + CivSettings.localize.localizedString("village_hitControlBlock") + "(" + cp.getHitpoints() + " / "
				+ cp.getMaxHitpoints() + ")"));
		CivMessage.sendVillage(this, "§e" + CivSettings.localize.localizedString("village_controlBlockUnderAttack"));
	}

	public void onControlBlockDestroy(ControlPoint cp, World world, Player player) {
		ItemManager.setTypeId((Block) cp.getCoord().getLocation().getBlock(), 0);
		world.playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, -1.0F);
		world.playSound(cp.getCoord().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
		FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.YELLOW).withColor(Color.RED).withTrail()
				.withFlicker().build();
		FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();

		for (int i = 0; i < 3; ++i) {
			try {
				fePlayer.playFirework(world, cp.getCoord().getLocation(), effect);
			} catch (Exception var9) {
				var9.printStackTrace();
			}
		}

		boolean allDestroyed = true;
		for (ControlPoint c : this.controlBlocks.values()) {
			if (c.isDestroyed() == false) {
				allDestroyed = false;
				break;
			}
		}

		if (allDestroyed) {
			CivMessage.sendVillage(this, "§c" + CivSettings.localize.localizedString("village_destroyed"));
			CivMessage
					.global(CivSettings.localize.localizedString("village_destroyedBy", this.getName(), CivGlobal.getFullNameTag(player), this.getOwnerName()));
			this.destroy();
		} else {
			CivMessage.sendVillage(this, "§c" + CivSettings.localize.localizedString("village_controlBlockDestroyed"));
		}

	}

	@Override
	public void onDamage(int amount, World world, Player player, BlockCoord hit, ConstructDamageBlock hit2) {
		ControlPoint cp = (ControlPoint) this.controlBlocks.get(hit);
		if (cp != null) {
			Date now = new Date();
			Resident resident = CivGlobal.getResident(player);
			if (resident.isProtected()) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("village_protected"));
				return;
			}

			if (now.after(this.getNextRaidDate())) {
				if (!cp.isDestroyed()) {
					cp.damage(amount);
					if (cp.isDestroyed()) {
						this.onControlBlockDestroy(cp, world, player);
					} else {
						this.onControlBlockHit(cp, world, player);
					}
				} else {
					CivMessage.send((Object) player, (String) ("§c" + CivSettings.localize.localizedString("village_controlBlockAlreadyDestroyed")));
				}
			} else {
				SimpleDateFormat sdf = CivGlobal.dateFormat;
				CivMessage.send((Object) player,
						(String) ("§c" + CivSettings.localize.localizedString("village_protectedUntil") + " " + sdf.format(this.getNextRaidDate())));
			}
		}
	}

	public void onSecondUpdate() {
		for (String ctrId : this.transmuterLocks.keySet()) {
			if (!this.transmuterLocks.get(ctrId).isLocked())
				TaskMaster.asyncTask("village-" + this.getCorner() + ";tr-" + ctrId, new TransmuterAsyncTask(this, CivSettings.transmuterRecipes.get(ctrId)),
						0);
		}
	}

	public void setNextRaidDate(Date next) {
		this.nextRaidDate = next;
		this.save();
	}

	public Date getNextRaidDate() {
		Date raidEnd = new Date(this.nextRaidDate.getTime());
		raidEnd.setTime(this.nextRaidDate.getTime() + (long) (3600000 * Village.raidLength));
		Date now = new Date();
		if (now.getTime() > raidEnd.getTime()) {
			this.nextRaidDate.setTime(this.nextRaidDate.getTime() + 86400000L);
		}

		return this.nextRaidDate;
	}

	public Collection<ConfigVillageUpgrade> getUpgrades() {
		return this.upgrades.values();
	}

	public boolean hasUpgrade(String require_upgrade) {
		return this.upgrades.containsKey(require_upgrade);
	}

	public void purchaseUpgrade(ConfigVillageUpgrade upgrade) throws CivException {
		Resident owner = this.getOwnerResident();
		if (!owner.getTreasury().hasEnough(upgrade.cost)) {
			throw new CivException(CivSettings.localize.localizedString("var_village_ownerMissingCost", upgrade.cost, CivSettings.CURRENCY_NAME));
		}

		this.upgrades.put(upgrade.id, upgrade);
		if (annexLevel.getOrDefault(upgrade.annex, 0) < upgrade.level) annexLevel.put(upgrade.annex, upgrade.level);
		if (upgrade.transmuter_recipe != null) {
			for (String s : upgrade.transmuter_recipe)
				this.transmuterLocks.put(s, new ReentrantLock());
		}
		upgrade.processAction(this);
		this.processCommandSigns();
		owner.getTreasury().withdraw(upgrade.cost);
		this.save();
	}

	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
		CivMessage.send(player, "TODO Нужно чтото написать");
	}
}