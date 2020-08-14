package com.avrgaming.civcraft.construct.constructs;

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.TransmuterComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCampUpgrade;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructBlock;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.construct.farm.FarmChunk;
import com.avrgaming.civcraft.construct.structures.BuildableStatic;
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
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TagManager;

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public class Camp extends Construct {
	private static int[][] offset = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 0 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };

	public static final String TABLE_NAME = "CAMPS";
	public static final double SHIFT_OUT = 2.0D;
	public static final String SUBDIR = "camp";
	public static double growthCampTotal = 1000.0;
	private static Integer coal_per_firepoint;
	private static Integer maxFirePoints;
	private static int raidLength;

	private HashMap<String, Resident> members = new HashMap<String, Resident>();
	public HashSet<BlockCoord> memberProtectionBlocks = new HashSet<>();
	/** можно ли отменить установку лагеря */
	private boolean undoable = false;

	private UUID ownerUuid;
	private int firepoints;
	public HashMap<Integer, BlockCoord> firepitBlocks = new HashMap<Integer, BlockCoord>();
	public HashSet<BlockCoord> fireFurnaceBlocks = new HashSet<BlockCoord>();

	/** уровни пристроек в селе */
	private HashMap<String, Integer> annexLevel = new HashMap<>();

	/** Locations that exhibit vanilla growth */
	public HashSet<BlockCoord> growthLocations = new HashSet<BlockCoord>();

	/* Longhouse Stuff. */
	public ReentrantLock lockLonghouse = new ReentrantLock();

	/* Control blocks */
	public HashMap<BlockCoord, ControlPoint> controlBlocks = new HashMap<BlockCoord, ControlPoint>();
	private Date nextRaidDate;

	private HashMap<String, ConfigCampUpgrade> upgrades = new HashMap<String, ConfigCampUpgrade>();
	public FarmChunk farmChunk;

	public Date lastBuildableRefresh;

	// -------------constructor
	public Camp(Resident resident) throws CivException {
		super("c_camp", resident);
		this.ownerUuid = resident.getUuid();
		try {
			this.setName(resident.getName());
		} catch (InvalidNameException var6) {}
		this.nextRaidDate = new Date();
		this.nextRaidDate.setTime(this.nextRaidDate.getTime() + 86400000L);
		this.firepoints = 2;
		this.loadSettings();
	}

	public Camp(ResultSet rs) throws SQLException, CivException {
		super("c_camp", null);
		this.load(rs);
		this.loadSettings();
		this.postBuild();
	}

	public static Camp newCamp(Player player, Location location) throws CivException {
		Resident resident = CivGlobal.getResident(player);
		if (resident.hasTown()) throw new CivException(CivSettings.localize.localizedString("buildcamp_hasTown"));
		if (resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("buildcamp_hascamp"));

		Camp camp = new Camp(resident);
		camp.initDefaultTemplate(location);
		camp.checkBlockPermissionsAndRestrictions(player);
		return camp;
	}

	// ----------- dataBase
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
		hashmap.put("owner_name", this.getOwnerResident().getUuid().toString());
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
		this.ownerUuid = UUID.fromString(rs.getString("owner_name"));
		if (this.ownerUuid == null) CivLog.error("COULD NOT FIND OWNER FOR CAMP ID:" + this.getId());
		this.corner = new BlockCoord(rs.getString("corner"));
		this.nextRaidDate = new Date(rs.getLong("next_raid_date"));
		this.setTemplate(Template.getTemplate(rs.getString("template_name")));
		this.firepoints = rs.getInt("firepoints");
		this.loadUpgradeString(rs.getString("upgrades"));
	}

	public void loadUpgradeString(String upgrades) {
		String[] split = upgrades.split(",");
		for (String id : split) {
			if (id == null || id.equalsIgnoreCase("")) continue;
			id = id.trim();
			ConfigCampUpgrade upgrade = CivSettings.campUpgrades.get(id);
			if (upgrade == null) {
				CivLog.warning("Unknown upgrade id " + id + " during load.");
				continue;
			}
			this.upgrades.put(id, upgrade);
			if (annexLevel.getOrDefault(upgrade.annex, 0) < upgrade.level) annexLevel.put(upgrade.annex, upgrade.level);
		}
	}

	public void loadSettings() {
		this.setHitpoints(getMaxHitPoints());
		super.loadSettings();
	}

	public static void loadStaticSettings() {
		try {
			Camp.growthCampTotal = CivSettings.getInteger(CivSettings.campConfig, "camp.growth");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		try {
			Camp.coal_per_firepoint = CivSettings.getInteger(CivSettings.campConfig, "camp.coal_per_firepoint");
			Camp.maxFirePoints = CivSettings.getInteger(CivSettings.campConfig, "camp.firepoints");
			Camp.raidLength = CivSettings.getInteger(CivSettings.campConfig, "camp.raid_length");
		} catch (InvalidConfiguration var7) {
			var7.printStackTrace();
		}
	}

	public String getUpgradeSaveString() {
		String out = "";
		for (ConfigCampUpgrade upgrade : this.upgrades.values()) {
			out += upgrade.id + ",";
		}
		return out;
	}

	@Override
	public void delete() {
		if (this.farmChunk != null) farmChunk.delete();

		for (Resident resident : this.members.values()) {
			resident.setCamp((Camp) null);
			resident.save();
		}
		this.members.clear();
		TagManager.editNameTag(this);

		this.unbindConstructBlocks();

		try {
			SQL.deleteNamedObject(this, TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		CivGlobal.removeCamp(this.getName());
	}

	// ----------- get info
	public String getDisplayName() {
		return CivSettings.localize.localizedString("Camp");
	}

	// ----------- build
	public void createCamp(Player player) throws CivException {
		Resident resident = CivGlobal.getResident(player);

		this.setUndoable(true);
		this.build(player);
		this.addMember(resident);
		CivGlobal.addCamp(this);
		this.save();
		SoundManager.playSound("campCreation", this.getCenterLocation());
		CivMessage.sendSuccess((CommandSender) player, CivSettings.localize.localizedString("camp_createSuccess"));
		player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
		resident.clearInteractiveMode();
		TagManager.editNameTag(player);
	}

	@Override
	public Location repositionCenter(Location center, Template tpl) throws CivException {
		return BuildableStatic.repositionCenterStatic(center, this.getTemplateYShift(), tpl, true);
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		for (ChunkCoord chunkCoord : this.getChunksCoords()) {
			if (CivGlobal.getCultureChunk(chunkCoord) != null) throw new CivException(CivSettings.localize.localizedString("camp_checkInCivError"));
		}
		super.checkBlockPermissionsAndRestrictions(player);
	}

	// ------------ delete
	public void destroy() {
		this.fancyDestroyConstructBlocks();
		this.delete();
	}

	public void disband() {
		SoundManager.playSound("campDestruction", this.getCenterLocation());
		try {
			this.undoFromTemplate();
		} catch (IOException | CivException e) {
			this.fancyDestroyConstructBlocks();
		}
		this.delete();
	}

	public void undo() {
		SoundManager.playSound("campDestruction", this.getCenterLocation());
		try {
			this.undoFromTemplate();
		} catch (IOException | CivException e) {
			this.fancyDestroyConstructBlocks();
		}
		this.delete();
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
	}

	@Override
	public void processValidateCommandBlockRelative() {
		/* Use the location's of the command blocks in the template and the buildable's corner to find their real positions. Then perform any
		 * special building we may want to do at those locations. */
		/* These block coords do not point to a location in the world, just a location in the template. */
		Template tpl = this.getTemplate();
		for (SimpleBlock sb : tpl.commandBlockRelativeLocations) {
			BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(sb.getX(), sb.getY(), sb.getZ()));
			int level = 0;
			String annex = null;
			String chestId = null;
			ConstructSign structSign = null;
			switch (sb.command) {
			case "/gardensign":
				if (annexLevel.getOrDefault("growth", 0) == 0) {
					structSign = CivGlobal.getConstructSign(absCoord);
					if (structSign == null) structSign = new ConstructSign(absCoord, this);
					ItemManager.setTypeIdAndData(absCoord.getBlock(), sb.getType(), sb.getData(), true);

					structSign.setDirection(ItemManager.getData(absCoord.getBlock().getState()));
					structSign.setOwner(this);
					structSign.setText(new String[] { "Сад виключен", "Нажми сюда или введи", "/camp upgrade", "что бы влючить" });
					structSign.setAction("gardenupgrade");
					structSign.update();
					this.addConstructSign(structSign);
				} else {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.AIR));
				}
				break;
			case "/sign":
				if (sb.keyvalues.containsKey("level")) level = Integer.parseInt(sb.keyvalues.get("level"));
				annex = sb.keyvalues.get("annex");
				if (annexLevel.getOrDefault(annex, 0) < level) {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.SIGN_POST));
					ItemManager.setData(absCoord.getBlock(), sb.getData());
					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, CivSettings.localize.localizedString("camp_growthUpgradeSign", level));
					sign.setLine(1, CivSettings.localize.localizedString("upgradeUsing_SignText"));
					sign.setLine(2, "/camp upgrade");
					sign.setLine(3, "");
					sign.update();
					this.addConstructBlock(absCoord, false);
				} else {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.AIR));
				}
				break;
			case "/growth":
				level = (sb.keyvalues.containsKey("level")) ? Integer.parseInt(sb.keyvalues.get("level")) : 1;
				annex = "growth";
				Block b = absCoord.getBlock();
				if (annexLevel.getOrDefault(annex, 0) >= level) {
					this.growthLocations.add(absCoord.getRelative(0, 1, 0));
					for (int i = 0; i < Camp.offset.length; i++)
						for (int j = 1; j < 5; j++) {
							this.memberProtectionBlocks.add(new BlockCoord(absCoord.getRelative(Camp.offset[i][0], j, Camp.offset[i][1])));
						}

					if (ItemManager.getTypeId(b) != CivData.FARMLAND) ItemManager.setTypeId(b, CivData.FARMLAND);
					this.addConstructBlock(absCoord, false);
				} else {
					ItemManager.setTypeId(b, CivData.COBBLESTONE);
					this.addConstructBlock(absCoord, false);
				}
				break;
			case "/sifter": // /chest,id:sifterresult,level:1,annex:sifter, для совместимости
				level = 1;
				annex = "sifter";
				if (annexLevel.getOrDefault(annex, 0) >= level) {
					Integer idchest = Integer.valueOf(sb.keyvalues.get("id"));
					switch (idchest) {
					case 0:
						chestId = "siftersource";
						break;
					case 1:
						chestId = "sifterresult";
						break;
					default:
						CivLog.warning("Unknown ID for sifter in camp:" + idchest);
						chestId = "sifternull";
						break;
					}
					ConstructChest structChest = CivGlobal.getConstructChest(absCoord);
					if (structChest == null) structChest = new ConstructChest(absCoord, this);
					structChest.setChestId(chestId);
					this.addChest(structChest);

					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.CHEST));
					byte data2 = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setData(absCoord.getBlock(), data2);
				} else {
					structSign = CivGlobal.getConstructSign(absCoord);
					if (structSign == null) structSign = new ConstructSign(absCoord, this);
					ItemManager.setTypeIdAndData(absCoord.getBlock(), sb.getType(), sb.getData(), true);
					structSign.setDirection(ItemManager.getData(absCoord.getBlock().getState()));
					structSign.setOwner(this);
					structSign.setText(new String[] { "Дробилка виключена", "Нажми сюда или введи", "/camp upgrade", "что бы влючить" });
					structSign.setAction("sifterupgrade");
					structSign.update();
					this.addConstructSign(structSign);
				}
				this.addConstructBlock(absCoord, false);
				break;
			case "/foodinput": // /chest,id:longhouse1,level:1,annex:longhouse, для совместимости
				level = 1;
				annex = "longhouse";
				chestId = "longhouse1";
				// пример 21:7:21,63:12,/chest,id:SecondSawmillResult,level:2,annex:sawmill,
				if (annexLevel.getOrDefault(annex, 0) >= level) {
					ConstructChest structChest = CivGlobal.getConstructChest(absCoord);
					if (structChest == null) structChest = new ConstructChest(absCoord, this);
					structChest.setChestId(chestId);
					this.addChest(structChest);

					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.CHEST));
					byte data2 = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setData(absCoord.getBlock(), data2);
				} else {
					structSign = CivGlobal.getConstructSign(absCoord);
					if (structSign == null) structSign = new ConstructSign(absCoord, this);
					ItemManager.setTypeIdAndData(absCoord.getBlock(), sb.getType(), sb.getData(), true);
					structSign.setDirection(ItemManager.getData(absCoord.getBlock().getState()));
					structSign.setOwner(this);
					structSign.setText(new String[] { "Большой дом виключен", "Нажми сюда или введи", "/camp upgrade", "что бы влючить" });
					structSign.setAction("longhouseupgrade");
					structSign.update();
					this.addConstructSign(structSign);
				}
				this.addConstructBlock(absCoord, false);
				break;
			case "/chest":
				if (sb.keyvalues.containsKey("level")) level = Integer.parseInt(sb.keyvalues.get("level"));
				annex = sb.keyvalues.get("annex");
				chestId = sb.keyvalues.get("id");
				// пример 21:7:21,63:12,/chest,id:SecondSawmillResult,level:2,annex:sawmill,
				if (annexLevel.getOrDefault(annex, 0) >= level) {
					ConstructChest structChest = CivGlobal.getConstructChest(absCoord);
					if (structChest == null) structChest = new ConstructChest(absCoord, this);
					structChest.setChestId(chestId);
					this.addChest(structChest);
					CivGlobal.addConstructChest(structChest);

					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.CHEST));
					byte data2 = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setData(absCoord.getBlock(), data2);

				} else {
					try {
						ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.SIGN_POST));
						ItemManager.setData(absCoord.getBlock(), sb.getData());
						Sign sign = (Sign) absCoord.getBlock().getState();
						sign.setLine(0, CivSettings.localize.localizedString("camp_" + annex + "UpgradeSign", level));
						sign.setLine(1, CivSettings.localize.localizedString("upgradeUsing_SignText"));
						sign.setLine(2, "/camp upgrade");
						sign.setLine(3, "");
						sign.update();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				this.addConstructBlock(absCoord, false);
				break;
			case "/firepit":
				this.firepitBlocks.put(Integer.valueOf(sb.keyvalues.get("id")), absCoord);
				this.addConstructBlock(absCoord, false);
				break;
			case "/fire":
				ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.FIRE));
				break;
			case "/firefurnace":
				this.fireFurnaceBlocks.add(absCoord);
				byte data = CivData.convertSignDataToChestData((byte) sb.getData());
				ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.FURNACE));
				ItemManager.setData(absCoord.getBlock(), data);
				this.addConstructBlock(absCoord, false);
				break;
			case "/door":
				Block doorBlock = absCoord.getBlock();
				Block doorBlock2 = absCoord.getBlock().getRelative(0, 1, 0);

				byte topData = 0x8;
				byte bottomData = 0x0;
				byte doorDirection = CivData.convertSignDataToDoorDirectionData((byte) sb.getData());
				bottomData |= doorDirection;

				ItemManager.setTypeIdAndData(doorBlock, ItemManager.getMaterialId(Material.WOODEN_DOOR), bottomData, false);
				ItemManager.setTypeIdAndData(doorBlock2, ItemManager.getMaterialId(Material.WOODEN_DOOR), topData, false);

				this.addConstructBlock(new BlockCoord(doorBlock), false);
				this.addConstructBlock(new BlockCoord(doorBlock2), false);
				break;
			case "/control":
				this.createControlPoint(absCoord, "");
				break;
			case "/literal":
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
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		// int special_id = Integer.valueOf(sign.getAction());
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;
		if (!this.getOwnerResident().equals(resident)) {
			CivMessage.sendError(player, "Вы не хозяин поселения");
			return;
		}

		switch (sign.getAction()) {
		case "gardenupgrade":
			processSignActionUpgrade(player, "v_up_garden1", sign);
			break;
		case "sifterupgrade":
			processSignActionUpgrade(player, "v_up_sifter1", sign);
			break;
		case "longhouseupgrade":
			processSignActionUpgrade(player, "v_up_longhouse1", sign);
			break;
		}
	}

	private void processSignActionUpgrade(Player player, String upgradeId, ConstructSign sign) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;
		ConfigCampUpgrade upgrade = CivSettings.campUpgrades.get(upgradeId);
		if (upgrade == null) {
			CivLog.error(CivSettings.localize.localizedString("var_cmd_camp_upgrade_buyInvalid", upgradeId));
			return;
		}
		if (this.hasUpgrade(upgrade.id)) {
			CivLog.error(CivSettings.localize.localizedString("cmd_camp_upgrade_buyOwned"));
			return;
		}

		if (!upgrade.isAvailable(this)) return;

		if (resident.getConstructSignConfirm() != null && resident.getConstructSignConfirm().equals(sign)) {
			try {
				this.purchaseUpgrade(upgrade);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
				return;
			}
			CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_camp_upgrade_buySuccess", CivColor.GreenBold + upgrade.name));
		} else {
			CivMessage.sendSuccess(player, "Для покупки улучшения " + CivColor.GreenBold + upgrade.name + CivColor.LightGreen + " у вас должно быть " + CivColor.YellowBold + upgrade.cost + CivColor.LightGreen + " монет.");
			CivMessage.sendSuccess(player, "Повторно нажмите на табличку для подтверждения покупки");
			resident.setConstructSignConfirm(sign);
		}
	}

	public void updateFirepit() {
		try {
			int maxFirePoints = CivSettings.getInteger(CivSettings.campConfig, "camp.firepoints");
			int totalFireBlocks = this.firepitBlocks.size();
			double percentLeft = (double) this.firepoints / (double) (maxFirePoints + 1);
			int litFires = (int) (percentLeft * (double) (totalFireBlocks + 1));

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
		this.firepoints--;

		int coutAddFirePoints = 0;
		while (mInv.contains(null, CivData.COAL, (short) 0, Camp.coal_per_firepoint) && this.firepoints < Camp.maxFirePoints) {
			try {
				mInv.removeItem(CivData.COAL, Camp.coal_per_firepoint, true);
				this.firepoints++;
				coutAddFirePoints++;
			} catch (CivException var4) {
				var4.printStackTrace();
			}
		}
		if (this.firepoints < 0) {
			this.destroy();
			return;
		}

		final double percentLeft = this.firepoints / (double) Camp.maxFirePoints;

		if (coutAddFirePoints > 0) {
			if (percentLeft != 1) CivMessage.sendCamp(this, "§e" + "Костер в вашем лагере разгорелся. " + this.firepoints + " часов горения осталось.");
		} else
			CivMessage.sendCamp(this, "§e" + CivSettings.localize.localizedString("var_camp_campfireDown", this.firepoints));

		if (percentLeft < 0.3) {
			CivMessage.sendCamp(this, "§e" + ChatColor.RED + ChatColor.BOLD + CivSettings.localize.localizedString("camp_campfire30percent"));
		}
		this.save();
		this.updateFirepit();
	}

	public void processLonghouse(CivAsyncTask task) {
		int level = annexLevel.getOrDefault("longhouse", 0);
		if (level == 0) return;
		MultiInventory mInv = new MultiInventory();

		ArrayList<ConstructChest> chests = this.getChestsById("longhouse1");
		// Make sure the chunk is loaded and add it to the inventory.
		for (ConstructChest c : chests) {
			Block b = c.getCoord().getBlock();
			Chest chest = (Chest) b.getState();
			mInv.addInventory(chest.getBlockInventory());
		}
		if (mInv.getInventoryCount() == 0) {
			CivMessage.sendCamp(this, "§c" + CivSettings.localize.localizedString("camp_longhouseNoChest"));
			return;
		}
		getConsumeLevelComponent().setMultiInventory(mInv);
		Result result = getConsumeLevelComponent().processConsumption(false);
		getConsumeLevelComponent().onSave();
		switch (result) {
		case STARVE:
			CivMessage.sendCamp(this, CivColor.LightGreen + CivSettings.localize.localizedString("var_camp_yourLonghouseDown",
					(CivColor.Rose + CivSettings.localize.localizedString("var_camp_longhouseStarved", getConsumeLevelComponent().getCountString()) + CivColor.LightGreen), CivSettings.CURRENCY_NAME));
			return;
		case LEVELDOWN:
			CivMessage.sendCamp(this, CivColor.LightGreen
					+ CivSettings.localize.localizedString("var_camp_yourLonghouseDown", (CivColor.Rose + CivSettings.localize.localizedString("camp_longhouseStavedAndLeveledDown") + CivColor.LightGreen), CivSettings.CURRENCY_NAME));
			return;
		case STAGNATE:
			CivMessage.sendCamp(this, CivColor.LightGreen
					+ CivSettings.localize.localizedString("var_camp_yourLonghouseDown", (CivColor.Yellow + CivSettings.localize.localizedString("camp_longhouseStagnated") + CivColor.LightGreen), CivSettings.CURRENCY_NAME));
			return;
		case UNKNOWN:
			CivMessage.sendCamp(this, CivColor.LightGreen
					+ CivSettings.localize.localizedString("var_camp_yourLonghouseDown", (CivColor.Purple + CivSettings.localize.localizedString("camp_longhouseSomethingUnknown") + CivColor.LightGreen), CivSettings.CURRENCY_NAME));
			return;
		default:
			break;
		}

		Double coins = null;
		if (result == Result.LEVELUP) {
			coins = getConsumeLevelComponent().getConsumeLevelStorageResult(getConsumeLevelComponent().getLevel() - 1);
		} else {
			coins = getConsumeLevelComponent().getConsumeLevelStorageResult(getConsumeLevelComponent().getLevel());
		}

		this.getOwnerResident().getTreasury().deposit(coins);

		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial("mat_token_of_leadership");
		if (craftMat != null) {
			ItemStack token = CraftableCustomMaterial.spawn(craftMat);

			Tagged tag = (Tagged) craftMat.getComponent("Tagged");
			Resident res = CivGlobal.getResident(this.getOwnerName());

			token = tag.addTag(token, res.getName());
			mInv.addItemStack(token, true);
		}

		String stateMessage = "";
		switch (result) {
		case GROW:
			stateMessage = CivColor.Green + CivSettings.localize.localizedString("var_camp_longhouseGrew", getConsumeLevelComponent().getCountString() + CivColor.LightGreen);
			break;
		case LEVELUP:
			stateMessage = CivColor.Green + CivSettings.localize.localizedString("camp_longhouselvlUp") + CivColor.LightGreen;
			break;
		case MAXED:
			stateMessage = CivColor.Green + CivSettings.localize.localizedString("var_camp_longhouseIsMaxed", getConsumeLevelComponent().getCountString() + CivColor.LightGreen);
			break;
		default:
			break;
		}

		CivMessage.sendCamp(this, CivColor.LightGreen + CivSettings.localize.localizedString("var_camp_yourLonghouse", stateMessage, coins, CivSettings.CURRENCY_NAME));

		if (level == 2) {
			// MultiInventory mInv2 = new MultiInventory();
			// ArrayList<ConstructChest> chests2 = this.getAllChestsById("longhouse2");
			// // Make sure the chunk is loaded and add it to the inventory.
			// for (ConstructChest c : chests2) {
			// Block b = c.getCoord().getBlock();
			// Chest chest = (Chest) b.getState();
			// mInv2.addInventory(chest.getBlockInventory());
			// }
			//
			// try {
			// mInv2.removeItem(CivData.BREAD,
			// CivSettings.campConfig.getInt("camp.bread_per_hour_for_second_longhouse"), true);
			// } catch (CivException e) {
			// return;
			// }
			// CraftableCustomMaterial craftMat2 = CraftableCustomMaterial
			// .getCraftableCustomMaterial("mat_token_of_leadership");
			// if (craftMat2 != null) {
			// ItemStack token = CraftableCustomMaterial.spawn(craftMat2);
			//
			// Tagged tag = (Tagged) craftMat2.getComponent("Tagged");
			// Resident res = CivGlobal.getResident(this.getOwnerName());
			//
			// token = tag.addTag(token, res.getUid().toString());
			//
			// AttributeUtil attrs = new AttributeUtil(token);
			// attrs.addLore(CivColor.LightGray + res.getName());
			// token = attrs.getStack();
			//
			// mInv2.addItems(token, true);
			// }
		}
	}

	public void addMember(Resident resident) {
		this.members.put(resident.getName(), resident);
		resident.setCamp(this);
		resident.save();
	}

	public void removeMember(Resident resident) {
		this.members.remove(resident.getName());
		resident.setCamp((Camp) null);
		resident.save();
	}

	public Resident getMember(String name) {
		return (Resident) this.members.get(name);
	}

	public boolean hasMember(String name) {
		return this.members.containsKey(name);
	}

	// public Resident getOwner() {
	// return CivGlobal.getResidentViaUUID(UUID.fromString(this.ownerName));
	// }
	// public void setOwner(Resident owner) {
	// this.ownerName = owner.getUid().toString();
	// }

//	public void fancyCampBlockDestory() {
//		for (BlockCoord coord : this.getConstructBlocks().keySet()) {
//			
//			if (ItemManager.getTypeId(coord.getBlock()) == CivData.CHEST) continue;
//			if (CivSettings.alwaysCrumble.contains(ItemManager.getTypeId(coord.getBlock()))) {
//				ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
//				continue;
//			}
//
//			double nextrand = CivCraft.civRandom.nextDouble();
//			// Each block has a 1% chance of launching an explosion effect
//			if (nextrand <= 0.002) {
//				FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED).withTrail().withFlicker().build();
//				FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
//				try {
//					fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
//				} catch (Exception var8) {
//					var8.printStackTrace();
//				}
//			}
//			if (nextrand <= 0.05) {
//				ItemManager.setTypeId(coord.getBlock(), 51);
//				continue; // Each block has a 5% chance of starting a fire
//			}
//			if (nextrand <= 0.2) {
//				ItemManager.setTypeId((Block) coord.getBlock(), 13);
//				continue; // Each block has a 20% chance to turn into gravel
//			}
//			if (nextrand <= 0.8) {
//				ItemManager.setTypeId(coord.getBlock(), 0);
//				continue; // Each block has a 80% chance of clear
//			}
//		}
//	}

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

		int campControlHitpoints;
		try {
			campControlHitpoints = CivSettings.getInteger(CivSettings.warConfig, "war.control_block_hitpoints_camp");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			campControlHitpoints = 100;
		}

		BlockCoord coord = new BlockCoord(b);
		this.controlBlocks.put(coord, new ControlPoint(coord, this, campControlHitpoints, info));
	}

	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDatabase().add(key, value, 0, 0, 0);
	}

	// XXX TODO make sure these all work...
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
		Resident res = CivGlobal.getResidentViaUUID(this.ownerUuid);
		return res.getName();
	}

	public int getLonghouseLevel() {
		if (getConsumeLevelComponent() == null) return 1;
		return getConsumeLevelComponent().getLevel();
	}

	public String getLonghouseCountString() {
		return getConsumeLevelComponent().getCountString();
	}

	public String getMembersString() {
		String out = "";
		for (Resident resident : members.values()) {
			out += resident.getName() + " ";
		}
		return out;
	}

	public void onControlBlockHit(ControlPoint cp, Player player) {
		cp.getWorld().playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2F, 1.0F);
		cp.getWorld().playEffect(cp.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		CivMessage.send((Object) player, (String) ("§7" + CivSettings.localize.localizedString("camp_hitControlBlock") + "(" + cp.getHitpoints() + " / " + cp.getMaxHitpoints() + ")"));
		CivMessage.sendCamp(this, "§e" + CivSettings.localize.localizedString("camp_controlBlockUnderAttack"));
	}

	public void onControlBlockDestroy(ControlPoint cp, Player player) {
		World world = cp.getCoord().getWorld();
		ItemManager.setTypeId((Block) cp.getCoord().getLocation().getBlock(), 0);
		world.playSound(cp.getCoord().getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0F, -1.0F);
		world.playSound(cp.getCoord().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.0F);
		FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.YELLOW).withColor(Color.RED).withTrail().withFlicker().build();
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
			CivMessage.sendCamp(this, "§c" + CivSettings.localize.localizedString("camp_destroyed"));
			CivMessage.global(CivSettings.localize.localizedString("camp_destroyedBy", this.getName(), CivGlobal.getFullNameTag(player), this.getOwnerName()));
			this.destroy();
		} else {
			CivMessage.sendCamp(this, "§c" + CivSettings.localize.localizedString("camp_controlBlockDestroyed"));
		}

	}

	@Override
	public void onDamage(int amount, Player player, ConstructDamageBlock hit) {
		ControlPoint cp = this.controlBlocks.get(hit.getCoord());
		if (cp != null) {
			Date now = new Date();
			Resident resident = CivGlobal.getResident(player);
			if (resident.isProtected()) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("camp_protected"));
				return;
			}

			if (now.after(this.getNextRaidDate())) {
				if (!cp.isDestroyed()) {
					cp.damage(amount);
					if (cp.isDestroyed()) {
						this.onControlBlockDestroy(cp, player);
					} else {
						this.onControlBlockHit(cp, player);
					}
				} else {
					CivMessage.send((Object) player, (String) ("§c" + CivSettings.localize.localizedString("camp_controlBlockAlreadyDestroyed")));
				}
			} else {
				SimpleDateFormat sdf = CivGlobal.dateFormat;
				CivMessage.send((Object) player, (String) ("§c" + CivSettings.localize.localizedString("camp_protectedUntil") + " " + sdf.format(this.getNextRaidDate())));
			}
		}
	}

	public void setNextRaidDate(Date next) {
		this.nextRaidDate = next;
		this.save();
	}

	public Date getNextRaidDate() {
		Date raidEnd = new Date(this.nextRaidDate.getTime());
		raidEnd.setTime(this.nextRaidDate.getTime() + (long) (3600000 * Camp.raidLength));
		Date now = new Date();
		if (now.getTime() > raidEnd.getTime()) {
			this.nextRaidDate.setTime(this.nextRaidDate.getTime() + 86400000L);
		}

		return this.nextRaidDate;
	}

	public Collection<ConfigCampUpgrade> getUpgrades() {
		return this.upgrades.values();
	}

	public boolean hasUpgrade(String require_upgrade) {
		return this.upgrades.containsKey(require_upgrade);
	}

	public void purchaseUpgrade(ConfigCampUpgrade upgrade) throws CivException {
		Resident owner = this.getOwnerResident();
		if (!owner.getTreasury().hasEnough(upgrade.cost)) {
			throw new CivException(CivSettings.localize.localizedString("var_camp_ownerMissingCost", upgrade.cost, CivSettings.CURRENCY_NAME));
		}

		this.upgrades.put(upgrade.id, upgrade);
		if (annexLevel.getOrDefault(upgrade.annex, 0) < upgrade.level) annexLevel.put(upgrade.annex, upgrade.level);
		this.postBuildSyncTask();
		CivMessage.sendCamp(this, CivSettings.localize.localizedString("camp_upgrade_" + upgrade.annex + upgrade.level));
		owner.getTreasury().withdraw(upgrade.cost);
		this.save();
	}

	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
		CivMessage.send(player, "TODO Нужно чтото написать");
	}

	private TransmuterComponent sifterTransmuter;

	public TransmuterComponent getSifterTransmuter() {
		if (sifterTransmuter == null) {
			for (Component comp : this.attachedComponents) {
				if (comp instanceof TransmuterComponent) {
					TransmuterComponent tc = (TransmuterComponent) comp;
					String annex = tc.getString("annex");
					if (annex != null && annex.equalsIgnoreCase("sifter")) {
						sifterTransmuter = tc;
						break;
					}
				}
			}
		}
		return sifterTransmuter;
	}

	private ConsumeLevelComponent consumeLevelComponent = null;

	public ConsumeLevelComponent getConsumeLevelComponent() {
		if (consumeLevelComponent == null) {
			for (Component comp : this.attachedComponents) {
				if (comp instanceof ConsumeLevelComponent) {
					consumeLevelComponent = (ConsumeLevelComponent) comp;
					break;
				}
			}
		}
		return consumeLevelComponent;
	}

	@Override
	public void onPostBuild() {
		for (String key : annexLevel.keySet()) {
			if (key.equals("longhouse")) continue;
			if (key.equals("sifter")) getSifterTransmuter().setLevel(annexLevel.get(key));
		}

		if (farmChunk != null) {
			farmChunk.delete();
			farmChunk = null;
		}

		for (BlockCoord farmBlock : this.growthLocations) {
			if (farmChunk == null) farmChunk = new FarmChunk(farmBlock.getChunkCoord(), this);
			farmChunk.staticCropLocationCache.add(farmBlock);
		}
	}

}