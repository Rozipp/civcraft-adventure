package com.avrgaming.civcraft.village;

import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigVillageLonghouseLevel;
import com.avrgaming.civcraft.config.ConfigVillageUpgrade;
import com.avrgaming.civcraft.config.ConfigTransmuterRecipe;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.InvalidObjectException;
import com.avrgaming.civcraft.items.BaseCustomMaterial;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.items.components.Tagged;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.road.RoadBlock;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PostBuildSyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
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
import java.util.List;
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

/** @author User */
@Getter
@Setter
public class Village extends Buildable {
	public static HashMap<String, ConfigTransmuterRecipe> enableTransmuterRecipes = new HashMap<>();
	public static final double SHIFT_OUT = 2.0D;
	public static final String SUBDIR = "village";

	private int hitpoints;
	private int firepoints;
	private BlockCoord corner;
	private HashMap<String, Resident> members = new HashMap<String, Resident>();
	private boolean undoable = false;

	private String ownerName;
	/* Village blocks on this structure. */
	public HashMap<BlockCoord, VillageBlock> villageBlocks = new HashMap<BlockCoord, VillageBlock>();
	public HashMap<Integer, BlockCoord> firepitBlocks = new HashMap<Integer, BlockCoord>();
	public HashSet<BlockCoord> fireFurnaceBlocks = new HashSet<BlockCoord>();
	private Integer coal_per_firepoint;
	private Integer maxFirePoints;

	/* Transmuter */
	/** уровни пристроек в селе */
	private HashMap<String, Integer> annexLevel = new HashMap<>();
	/** служит замком для рецептов трасмутера, а так же испольжуеться во время проверки купленых рецептов */
	public HashMap<String, ReentrantLock> locks = new HashMap<>();

	/* Locations that exhibit vanilla growth */
	public HashSet<BlockCoord> growthLocations = new HashSet<BlockCoord>();

	/* Longhouse Stuff. */
	private ConsumeLevelComponent consumeComponent;

	/* Doors we protect. */
//	public HashSet<BlockCoord> doors = new HashSet<BlockCoord>();
	/* Control blocks */
	public HashMap<BlockCoord, ControlPoint> controlBlocks = new HashMap<BlockCoord, ControlPoint>();

	private Date nextRaidDate;
	private int raidLength;

	private HashMap<String, ConfigVillageUpgrade> upgrades = new HashMap<String, ConfigVillageUpgrade>();

	public static void newVillage(Resident resident, Player player, String name) {
		try {
			Village existVillage = CivGlobal.getVillage(name);
			if (existVillage != null) {
				throw new CivException("(" + name + ") " + CivSettings.localize.localizedString("village_nameTaken"));
			}
			resident.clearInteractiveMode();
			ItemStack stack = player.getInventory().getItemInMainHand();
			BaseCustomMaterial craftMat = CustomMaterial.getBaseCustomMaterial(stack);
			if (craftMat == null || !craftMat.hasComponent("FoundVillage")) {
				throw new CivException(CivSettings.localize.localizedString("village_missingItem"));
			}
			Village village = new Village(resident, name, player.getLocation());
			village.buildVillage(player, player.getLocation());
			village.setUndoable(true);
			CivGlobal.addVillage(village);
			village.save();
			CivMessage.sendSuccess((CommandSender) player, CivSettings.localize.localizedString("village_createSuccess"));
			player.getInventory().setItemInMainHand(null);

			TagManager.editNameTag(player);
		} catch (CivException var6) {
			CivMessage.sendError(player, var6.getMessage());
		}
	}

	public Village(Resident owner, String name, Location corner) throws CivException {
		this.ownerName = owner.getUid().toString();
		this.corner = new BlockCoord(corner);
		try {
			this.setName(name);
		} catch (InvalidNameException var6) {
			throw new CivException("Плохое имя для лагеря, выберите другое.");
		}
		this.nextRaidDate = new Date();
		this.nextRaidDate.setTime(this.nextRaidDate.getTime() + 86400000L);
		try {
			this.firepoints = CivSettings.getInteger(CivSettings.villageConfig, "village.firepoints");
			this.hitpoints = CivSettings.getInteger(CivSettings.villageConfig, "village.hitpoints");
		} catch (InvalidConfiguration var5) {
			var5.printStackTrace();
		}
		this.loadSettings();
	}

	public Village(ResultSet rs) throws SQLException, InvalidNameException, InvalidObjectException, CivException {
		this.load(rs);
		this.loadSettings();
	}

	public void loadSettings() {
		try {
			this.coal_per_firepoint = CivSettings.getInteger(CivSettings.villageConfig, "village.coal_per_firepoint");
			this.maxFirePoints = CivSettings.getInteger(CivSettings.villageConfig, "village.firepoints");
			this.raidLength = CivSettings.getInteger(CivSettings.villageConfig, "village.raid_length");
			this.consumeComponent = new ConsumeLevelComponent();
			this.consumeComponent.setBuildable(this);
			this.hitpoints = CivSettings.getInteger(CivSettings.villageConfig, "village.hitpoints");
			for (ConfigVillageLonghouseLevel lvl : CivSettings.longhouseLevels.values()) {
				consumeComponent.addLevel(lvl.level, lvl.count);
				consumeComponent.setConsumes(lvl.level, lvl.consumes);
			}
			this.consumeComponent.onLoad();
		} catch (InvalidConfiguration var7) {
			var7.printStackTrace();
		}
	}

	public static final String TABLE_NAME = "VILLAGES";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" + "`id` int(11) unsigned NOT NULL auto_increment,"
					+ "`name` VARCHAR(64) NOT NULL," + "`owner_name` mediumtext NOT NULL," + "`firepoints` int(11) DEFAULT 0," + "`next_raid_date` long,"
					+ "`corner` mediumtext," + "`upgrades` mediumtext," + "`template_name` mediumtext," + "PRIMARY KEY (`id`)" + ")";
			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info("TABLE_NAME table OK!");
		}
	}

	public void load(ResultSet rs) throws SQLException, InvalidNameException, InvalidObjectException, CivException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.ownerName = rs.getString("owner_name");
		if (this.ownerName == null) {
			CivLog.error("COULD NOT FIND OWNER FOR VILLAGE ID:" + this.getId());
		}
		this.corner = new BlockCoord(rs.getString("corner"));
		this.nextRaidDate = new Date(rs.getLong("next_raid_date"));
		this.setTemplateName(rs.getString("template_name"));

		this.firepoints = rs.getInt("firepoints");

		this.loadUpgradeString(rs.getString("upgrades"));
		this.bindVillageBlocks();
	}

	public void save() {
		SQLUpdate.add(this);
	}

	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("name", this.getName());
		hashmap.put("owner_name", this.getOwner().getUid().toString());
		hashmap.put("firepoints", this.firepoints);
		hashmap.put("corner", this.corner.toString());
		hashmap.put("next_raid_date", this.nextRaidDate.getTime());
		hashmap.put("upgrades", this.getUpgradeSaveString());
		hashmap.put("template_name", this.getTemplateName());
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

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

	public void loadUpgradeString(String upgrades) {
		String[] split = upgrades.split(",");
		for (String id : split) {
			if (id == null || id.equalsIgnoreCase("")) {
				continue;
			}
			id = id.trim();
			ConfigVillageUpgrade upgrade = CivSettings.villageUpgrades.get(id);
			if (upgrade == null) {
				CivLog.warning("Unknown upgrade id " + id + " during load.");
				continue;
			}
			this.upgrades.put(id, upgrade);
			if (annexLevel.getOrDefault(upgrade.annex, 0) < upgrade.level) annexLevel.put(upgrade.annex, upgrade.level);
			for (String s : upgrade.transmuter_recipe) {
				this.locks.put(s, new ReentrantLock());
			}
			//upgrade.processAction(this);
		}
	}

	public String getUpgradeSaveString() {
		String out = "";
		for (ConfigVillageUpgrade upgrade : this.upgrades.values()) {
			out += upgrade.id + ",";
		}
		return out;
	}

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
		this.undoFromTemplate();
		try {
			this.delete();
		} catch (SQLException var2) {
			var2.printStackTrace();
		}
	}

	public void undo() {
		SoundManager.playSound("villageDestruction", this.getCenterLocation());
		this.undoFromTemplate();
		try {
			this.delete();
		} catch (SQLException var2) {
			var2.printStackTrace();
		}
	}

	public void undoFromTemplate() {
		Template undo_tpl = new Template();
		try {
			undo_tpl.initUndoTemplate(this.getCorner().toString(), SUBDIR);
			undo_tpl.buildUndoTemplate(undo_tpl, this.getCorner().getBlock());
			undo_tpl.deleteUndoTemplate(this.getCorner().toString(), SUBDIR);
		} catch (CivException | IOException var3) {
			var3.printStackTrace();
		}

	}

	public void buildVillage(Player player, Location center) throws CivException {
		String templateFile;
		try {
			templateFile = CivSettings.getString(CivSettings.villageConfig, "village.template");
		} catch (InvalidConfiguration var11) {
			var11.printStackTrace();
			return;
		}

		Resident resident = CivGlobal.getResident(player);
		Template tpl;
		if (resident.desiredTemplate == null) {
			try {
				String templatePath = Template.getTemplateFilePath(templateFile, Template.getDirection(center), Template.TemplateType.STRUCTURE, "default");
				tpl = Template.getTemplate(templatePath, center);
			} catch (IOException var9) {
				var9.printStackTrace();
				return;
			} catch (CivException var10) {
				var10.printStackTrace();
				return;
			}
		} else {
			tpl = resident.desiredTemplate;
			resident.desiredTemplate = null;
		}

		this.corner.setFromLocation(this.repositionCenter(center, tpl.dir(), (double) tpl.size_x, (double) tpl.size_z));

		this.setTotalBlockCount(tpl.size_x * tpl.size_y * tpl.size_z);
		// Save the template x,y,z for later. This lets us know our own dimensions.
		// this is saved in the db so it remains valid even if the template changes.
		this.setTemplateName(tpl.getFilepath());
		this.setTemplateX(tpl.size_x);
		this.setTemplateY(tpl.size_y);
		this.setTemplateZ(tpl.size_z);
//		this.setTemplateAABB(new BlockCoord(cornerLoc), tpl);	

		this.checkBlockPermissionsAndRestrictions(player, this.corner.getBlock(), tpl.size_x, tpl.size_y, tpl.size_z);

		try {
			tpl.saveUndoTemplate(this.getCorner().toString(), SUBDIR, this.getCorner().getLocation());
		} catch (IOException var8) {
			var8.printStackTrace();
		}

		this.buildVillageFromTemplate(tpl, this.corner);
		TaskMaster.syncTask(new PostBuildSyncTask(tpl, this));

		try {
			this.saveNow();
		} catch (SQLException var7) {
			var7.printStackTrace();
			throw new CivException("Internal SQL Error.");
		}

		SoundManager.playSound("villageCreation", center);
		this.addMember(resident);
		resident.save();
	}
	
	@Override
	public void processValidateCommandBlockRelative(Template tpl) {
		/* Use the location's of the command blocks in the template and the buildable's corner to find their real positions. Then perform any special building
		 * we may want to do at those locations. */
		/* These block coords do not point to a location in the world, just a location in the template. */
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
						this.addVillageBlock(absCoord);
					} else {
						ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.AIR));
						this.removeVillageBlock(absCoord);
					}
					break;
				case "/growth" :
					if (sb.keyvalues.containsKey("level")) level = Integer.parseInt(sb.keyvalues.get("level"));
					annex = sb.keyvalues.get("annex");
					if (annexLevel.getOrDefault(annex, 0) >= level) {
						this.growthLocations.add(absCoord);
						CivGlobal.vanillaGrowthLocations.add(absCoord);
						Block b = absCoord.getBlock();
						if (ItemManager.getTypeId(b) != CivData.FARMLAND) ItemManager.setTypeId(b, CivData.FARMLAND);
						this.addVillageBlock(absCoord, true);
						this.addVillageBlock(new BlockCoord(absCoord.getBlock().getRelative(0, 1, 0)), true);
					} else {
						this.addVillageBlock(absCoord);
						this.addVillageBlock(new BlockCoord(absCoord.getBlock().getRelative(0, 1, 0)));
					}
					break;
				case "/chest" :
					if (sb.keyvalues.containsKey("level")) level = Integer.parseInt(sb.keyvalues.get("level"));
					annex = sb.keyvalues.get("annex");
					chestId = sb.keyvalues.get("id");
					//пример 21:7:21,63:12,/chest,id:SecondSawmillResult,level:2,annex:sawmill,
					if (annexLevel.getOrDefault(annex, 0) >= level) {
						StructureChest structChest = CivGlobal.getStructureChest(absCoord);
						if (structChest == null) structChest = new StructureChest(absCoord, this);
						structChest.setChestId(chestId);
						this.addStructureChest(structChest);
						CivGlobal.addStructureChest(structChest);

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
					this.addVillageBlock(absCoord);
					break;
				case "/firepit" :
					this.firepitBlocks.put(Integer.valueOf(sb.keyvalues.get("id")), absCoord);
					this.addVillageBlock(absCoord);
					break;
				case "/fire" :
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.FIRE));
					break;
				case "/firefurnace" :
					this.fireFurnaceBlocks.add(absCoord);
					byte data = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.FURNACE));
					ItemManager.setData(absCoord.getBlock(), data);
					this.addVillageBlock(absCoord);
					break;

//				case "/door" :
//					this.doors.add(absCoord);
//					Block doorBlock = absCoord.getBlock();
//					Block doorBlock2 = absCoord.getBlock().getRelative(0, 1, 0);
//
//					byte topData = 0x8;
//					byte bottomData = 0x0;
//					byte doorDirection = CivData.convertSignDataToDoorDirectionData((byte) sb.getData());
//					bottomData |= doorDirection;
//
//					ItemManager.setTypeIdAndData(doorBlock, ItemManager.getMaterialId(Material.WOODEN_DOOR), bottomData, false);
//					ItemManager.setTypeIdAndData(doorBlock2, ItemManager.getMaterialId(Material.WOODEN_DOOR), topData, false);
//
//					this.addVillageBlock(new BlockCoord(doorBlock));
//					this.addVillageBlock(new BlockCoord(doorBlock2));
//					break;
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
					this.addVillageBlock(absCoord);
					break;
			}
		}
	}

	public void reprocessCommandSigns() {
		Template tpl;
		try {
			tpl = Template.getTemplate(this.getTemplateName(), (Location) null);
		} catch (CivException | IOException var3) {
			var3.printStackTrace();
			return;
		}

		this.processCommandSigns(tpl);
	}

	@Override
	public void processCommandSigns(Template tpl) {
		for (BlockCoord relativeCoord : tpl.doorRelativeLocations) {
			SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
			BlockCoord absCoord = new BlockCoord(this.getCorner().getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

			Block block = absCoord.getBlock();
			if (ItemManager.getTypeId(block) != sb.getType()) {
				ItemManager.setTypeIdAndData(block, sb.getType(), (byte) sb.getData(), false);
			}
			this.addVillageBlock(absCoord);
		}

		for (BlockCoord relativeCoord : tpl.attachableLocations) {
			SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
			BlockCoord absCoord = new BlockCoord(this.getCorner().getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));

			Block block = absCoord.getBlock();
			if (ItemManager.getTypeId(block) != sb.getType()) {
				ItemManager.setTypeIdAndData(block, sb.getType(), (byte) sb.getData(), false);
			}
			this.addVillageBlock(absCoord);
		}
		

		updateFirepit();
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
		if (mInv.contains(null, CivData.COAL, (short) 0, this.coal_per_firepoint)) {
			try {
				mInv.removeItem(CivData.COAL, this.coal_per_firepoint, true);
			} catch (CivException var4) {
				var4.printStackTrace();
			}

			this.firepoints++;
			if (this.firepoints > this.maxFirePoints) {
				this.firepoints = this.maxFirePoints;
			}
		} else {
			this.firepoints--;
			CivMessage.sendVillage(this, "§e" + CivSettings.localize.localizedString("var_village_villagefireDown", this.firepoints));
			final double percentLeft = this.firepoints / (double) this.maxFirePoints;
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

		ArrayList<StructureChest> chests = this.getAllChestsById("longhouse1");
		// Make sure the chunk is loaded and add it to the inventory.
		for (StructureChest c : chests) {
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
		this.getOwner().getTreasury().deposit(total_coins);

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
			ArrayList<StructureChest> chests2 = this.getAllChestsById("longhouse2");
			// Make sure the chunk is loaded and add it to the inventory.
			for (StructureChest c : chests2) {
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

	private void buildVillageFromTemplate(Template tpl, BlockCoord corner) {
		Block cornerBlock = corner.getBlock();

		for (int x = 0; x < tpl.size_x; ++x) {
			for (int y = 0; y < tpl.size_y; ++y) {
				for (int z = 0; z < tpl.size_z; ++z) {
					Block nextBlock = cornerBlock.getRelative(x, y, z);
					SimpleBlock sb = tpl.blocks[x][y][z];
					if (sb.specialType == SimpleBlock.Type.COMMAND) {
						continue;
					}
					if (sb.specialType == SimpleBlock.Type.LITERAL) {
						// Adding a command block for literal sign placement
						sb.command = "/literal";
						tpl.commandBlockRelativeLocations.add(new BlockCoord(cornerBlock.getWorld().getName(), x, y, z));
						continue;
					}
					if (sb.getType() == CivData.WOOD_DOOR || sb.getType() == CivData.IRON_DOOR || sb.getType() == CivData.SPRUCE_DOOR
							|| sb.getType() == CivData.BIRCH_DOOR || sb.getType() == CivData.JUNGLE_DOOR || sb.getType() == CivData.ACACIA_DOOR
							|| sb.getType() == CivData.DARK_OAK_DOOR || Template.isAttachable(sb.getType())) {
						// dont build doors, save it for post sync build.

					} else
						try {
							if (ItemManager.getTypeId(nextBlock) != sb.getType()) {
								ItemManager.setTypeId(nextBlock, sb.getType());
								ItemManager.setData(nextBlock, sb.getData());
							}
						} catch (Exception var9) {
							CivLog.error(var9.getMessage());
						}

					if (sb.getType() != CivData.AIR) {
						if (sb.specialType != Type.COMMAND) {
							if (ItemManager.getTypeId(nextBlock) != CivData.AIR) {
								this.addVillageBlock(new BlockCoord(nextBlock.getLocation()));
							}
						}
					}
				}
			}
		}
	}

	private void bindVillageBlocks() {
		// Called mostly on a reload, determines which blocks should be protected based on the corner
		// location and the template's size. We need to verify that each block is a part of the template.

		Template tpl;
		try {
			tpl = Template.getTemplate(this.getTemplateName(), (Location) null);
		} catch (IOException var9) {
			var9.printStackTrace();
			return;
		} catch (CivException var10) {
			var10.printStackTrace();
			return;
		}

		for (int y = 0; y < tpl.size_y; y++) {
			for (int z = 0; z < tpl.size_z; z++) {
				for (int x = 0; x < tpl.size_x; x++) {
					int relx = this.getCorner().getX() + x;
					int rely = this.getCorner().getY() + y;
					int relz = this.getCorner().getZ() + z;
					BlockCoord coord = new BlockCoord(this.getCorner().getWorldname(), relx, rely, relz);
					if (tpl.blocks[x][y][z].getType() == CivData.AIR) continue;
					if (tpl.blocks[x][y][z].specialType == SimpleBlock.Type.COMMAND) continue;
					this.addVillageBlock(coord);
				}
			}
		}
		this.processCommandSigns(tpl);
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

	protected void checkBlockPermissionsAndRestrictions(Player player, Block block, int sizeX, int sizeY, int sizeZ) throws CivException {
		ChunkCoord ccoord = new ChunkCoord(block.getLocation());
		if (CivGlobal.getCultureChunk(ccoord) != null) throw new CivException(CivSettings.localize.localizedString("village_checkInCivError"));
		if (player.getLocation().getY() >= 200.0D) throw new CivException(CivSettings.localize.localizedString("village_checkTooHigh"));
		if (sizeY + block.getLocation().getBlockY() >= 255) throw new CivException(CivSettings.localize.localizedString("village_checkWayTooHigh"));
		if (player.getLocation().getY() < CivGlobal.minBuildHeight)
			throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		if (!player.isOp()) Buildable.validateDistanceFromSpawn(block.getLocation());

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
					if (CivGlobal.getStructureBlock(coord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
					if (CivGlobal.getFarmChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_farmInWay"));
					if (CivGlobal.getWallChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_wallInWay"));
					if (CivGlobal.getVillageBlock(coord) != null) throw new CivException(CivSettings.localize.localizedString("cannotBuild_villageinWay"));

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
		for (BlockCoord bcoord : this.villageBlocks.keySet()) {
			CivGlobal.removeVillageBlock(bcoord);
			ChunkCoord coord = new ChunkCoord(bcoord);
			CivGlobal.removeVillageChunk(coord);
		}
	}

	private void addVillageBlock(BlockCoord coord) {
		this.addVillageBlock(coord, false);
	}

	private void addVillageBlock(BlockCoord coord, boolean friendlyBreakable) {
		VillageBlock cb = new VillageBlock(coord, this, friendlyBreakable);
		this.villageBlocks.put(coord, cb);
		CivGlobal.addVillageBlock(cb);
	}

	private void removeVillageBlock(BlockCoord absCoord) {
		this.villageBlocks.remove(absCoord);
		CivGlobal.removeVillageBlock(absCoord);
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

	public Resident getOwner() {
		return CivGlobal.getResidentViaUUID(UUID.fromString(this.ownerName));
	}

	public void setOwner(Resident owner) {
		this.ownerName = owner.getUid().toString();
	}

	public void fancyVillageBlockDestory() {
		for (BlockCoord coord : this.villageBlocks.keySet()) {
			if (CivGlobal.getStructureChest(coord) != null) continue;
			if (CivGlobal.getStructureSign(coord) != null) continue;
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

		StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
		this.addVillageBlock(sb.getCoord());

		/* Build the control block. */
		b = centerLoc.getBlock().getRelative(0, 1, 0);
		ItemManager.setTypeId(b, CivData.OBSIDIAN);
		sb = new StructureBlock(new BlockCoord(b), this);
		this.addVillageBlock(sb.getCoord());

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

	public String getDisplayName() {
		return CivSettings.localize.localizedString("Village");
	}

	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDB().add(key, value, 0, 0, 0);
	}

	//XXX TODO make sure these all work...
	@Override
	public void processUndo() throws CivException {
	}

	@Override
	public void updateBuildProgess() {
	}

	@Override
	public void build(Player player, Location centerLoc, Template tpl) throws Exception {
	}

	@Override
	protected void runOnBuild(Location centerLoc, Template tpl) throws CivException {
		return;
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
	public void onComplete() {
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
			CivMessage.global(
					CivSettings.localize.localizedString("village_destroyedBy", this.getName(), CivGlobal.getFullNameTag(player), this.getOwner().getName()));
			this.destroy();
		} else {
			CivMessage.sendVillage(this, "§c" + CivSettings.localize.localizedString("village_controlBlockDestroyed"));
		}

	}

	@Override
	public void onDamage(int amount, World world, Player player, BlockCoord hit, BuildableDamageBlock hit2) {
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

	public void setNextRaidDate(Date next) {
		this.nextRaidDate = next;
		this.save();
	}

	public Date getNextRaidDate() {
		Date raidEnd = new Date(this.nextRaidDate.getTime());
		raidEnd.setTime(this.nextRaidDate.getTime() + (long) (3600000 * this.raidLength));
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
		Resident owner = this.getOwner();
		if (!owner.getTreasury().hasEnough(upgrade.cost)) {
			throw new CivException(CivSettings.localize.localizedString("var_village_ownerMissingCost", upgrade.cost, CivSettings.CURRENCY_NAME));
		}

		this.upgrades.put(upgrade.id, upgrade);
		if (annexLevel.getOrDefault(upgrade.annex, 0) < upgrade.level) annexLevel.put(upgrade.annex, upgrade.level);
		for (String s : upgrade.transmuter_recipe)
			this.locks.put(s, new ReentrantLock());
		upgrade.processAction(this);
		this.reprocessCommandSigns();
		owner.getTreasury().withdraw(upgrade.cost);
		this.save();
	}

	public static void loadConfigTransmuterRecipes() {
		List<?> configLore = CivSettings.villageConfig.getList("transmuter_recipe");
		if (configLore != null) {
			for (Object obj : configLore) {
				if (obj instanceof String) {
					ConfigTransmuterRecipe ctr = CivSettings.transmuterRecipes.get((String) obj);
					if (ctr != null) enableTransmuterRecipes.put(ctr.id, ctr);
				}
			}
		}
	}
}