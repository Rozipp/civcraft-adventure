/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.object;

import gpl.AttributeUtil;
import gpl.InventorySerializer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.interactive.InteractiveResponse;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.RoadBlock;
import com.avrgaming.civcraft.structure.Structure;
// import com.avrgaming.civcraft.structure.TeslaShip;
import com.avrgaming.civcraft.structure.TeslaTower;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildPreviewAsyncTask;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomPersonalTemplate;
import com.avrgaming.global.perks.components.CustomTemplate;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;

@Getter
@Setter
public class Resident extends SQLObject {

	private Town town = null;
	private Camp camp = null;
	private boolean townChat = false;
	private boolean civChat = false;
	private boolean adminChat = false;
	private boolean combatInfo = false;
	private boolean titleAPI = true;
	@Deprecated
	private boolean preview;
	@Deprecated
	private int languageCode = 1033;
	@Deprecated
	public static HashSet<String> allchatters = new HashSet<String>();

	/** Town to chat in besides your own. /ad tc <town> */
	private Town townChatOverride = null;
	/** Civ to chat in besides your own. /ad cc <civ> */
	private Civilization civChatOverride = null;
	private boolean permOverride = false;
	private boolean sbperm = false;
	private boolean controlBlockInstantBreak = false;
	private int townID = 0;
	private int campID = 0;
	private boolean dontSaveTown = false;
	private String timezone;

	private boolean canUseRename;
	private long nextRefresh;

	private long registered;
	private long lastOnline;
	private int daysTilEvict;
	private boolean givenKit;
	private ConcurrentHashMap<String, Integer> friends = new ConcurrentHashMap<String, Integer>();
	private EconObject treasury;
	private boolean muted;
	private Date muteExpires = null;

	private boolean interactiveMode = false;
	private InteractiveResponse interactiveResponse = null;
	private BuildPreviewAsyncTask previewTask = null;

	private int unitObjectId = 0;
	private int lastUnitObjectId = 0;
	private boolean performingMission = false;

	private Town selectedTown = null;

	private Scoreboard scoreboard = null;
	public String desiredCivName;
	public String desiredCapitolName;
	public String desiredTownName;
	public String desiredTag;
	public Location desiredTownLocation = null;
	public Template desiredTemplate = null;
	/**
	 * Если нужно повтороное нажатие на табличку, то эта переменная служит памятью
	 * последнего нажатия
	 */
	public ConstructSign constructSignConfirm = null;

	public boolean allchat = false;

	/*
	 * XXX This buildable is used as place to store which buildable we're working on
	 * when interacting with GUI items. We want to be able to pass the buildable
	 * object to the GUI's action function, but there isn't a good way to do this
	 * ATM. If we had a way to send arbitary objects it would be better. Could we
	 * store it here on the resident object?
	 */
	public Buildable pendingBuildable;
	public ConfigBuildableInfo pendingBuildableInfo;
	public CallbackInterface pendingCallback;

	private boolean showScout = true;
	private boolean showTown = true;
	private boolean showCiv = true;
	private boolean showMap = false;
	private boolean showInfo = false;
	private String itemMode = "all";
	private String savedInventory = null;
	private boolean isProtected = false;

	public ConcurrentHashMap<BlockCoord, SimpleBlock> previewUndo = null;
	public LinkedHashMap<String, Perk> perks = new LinkedHashMap<String, Perk>();
	private Date lastKilledTime = null;
	private String lastIP = "";
	private UUID uid;
	private double walkingModifier = UnitStatic.normal_speed;
	private boolean onRoad = false;
	public String debugTown;

	private String savedPrefix;
	private String prefix;
	public boolean isTeleporting = false;
	private long nextTeleport;
	private long poisonImmune = 0L;
	private long levitateImmune = 0L;
	private long nextPLCDamage = 0L;
	private String reportResult;
	private boolean reportChecked;

	public static int POISON_DURATION = 5;
	public static int LEVITATE_DURATION = 3;

	private String desiredReportPlayerName;
	private boolean campChat;
	public boolean isRefresh;

	public Resident(UUID uid, String name) throws InvalidNameException {
		this.setName(name);
		this.uid = uid;
		this.treasury = CivGlobal.createEconObject(this);
		this.setGivenKit(false);
		setTimezoneToServerDefault();
		loadSettings();
	}

	public Resident(ResultSet rs) throws SQLException, InvalidNameException {
		this.load(rs);
		this.setGivenKit(true);
		loadSettings();
	}

	public void loadSettings() {
	}

	public static final String TABLE_NAME = "RESIDENTS";

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" + //
					"`id` int(11) unsigned NOT NULL auto_increment," + //
					"`name` VARCHAR(64) NOT NULL," + //
					"`uuid` VARCHAR(256) NOT NULL DEFAULT 'UNKNOWN'," + //
					"`currentName` VARCHAR(64) DEFAULT NULL," + //
					"`town_id` int(11)," + //
					"`lastOnline` BIGINT NOT NULL," + //
					"`registered` BIGINT NOT NULL," + //
					"`friends` mediumtext," + //
					"`debt` double DEFAULT 0," + //
					"`coins` double DEFAULT 0," + //
					"`daysTilEvict` mediumint DEFAULT NULL," + //
					"`camp_id` int(11)," + //
					"`nextTeleport` BIGINT NOT NULL DEFAULT '0'," + //
					"`nextRefresh` BIGINT NOT NULL DEFAULT '0'," + //
					"`timezone` mediumtext," + //
					"`last_unit_object_id` int(11) NOT NULL DEFAULT '0'," + //
					"`savedInventory` mediumtext DEFAULT NULL," + //
					"`isProtected` bool NOT NULL DEFAULT '0'," + //
					"`flags` mediumtext DEFAULT NULL," + //
					"`last_ip` mediumtext DEFAULT NULL," + //
					"`debug_town` mediumtext DEFAULT NULL," + //
					"`debug_civ` mediumtext DEFAULT NuLL," + //
					"`language_id` int(11) DEFAULT '1033'," + //
					"`reportResult` mediumtext," + //
					"`reportChecked` boolean DEFAULT false," + //
					"UNIQUE KEY (`name`), " + "PRIMARY KEY (`id`)" + ")";

			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.townID = rs.getInt("town_id");
		this.campID = rs.getInt("camp_id");
		this.lastIP = rs.getString("last_ip");
		this.debugTown = rs.getString("debug_town");

		this.uid = (rs.getString("uuid").equalsIgnoreCase("UNKNOWN")) ? null : UUID.fromString(rs.getString("uuid"));

		this.treasury = CivGlobal.createEconObject(this);
		this.getTreasury().setBalance(rs.getDouble("coins"), false);
		this.setTimezone(rs.getString("timezone"));
		this.loadFlagSaveString(rs.getString("flags"));
		this.lastUnitObjectId = rs.getInt("last_unit_object_id");
		this.savedInventory = rs.getString("savedInventory");
		this.isProtected = rs.getBoolean("isProtected");
		this.languageCode = rs.getInt("language_id");
		this.nextTeleport = rs.getLong("nextTeleport");
		this.reportResult = rs.getString("reportResult");
		this.reportChecked = rs.getBoolean("reportChecked");

		if (this.getTimezone() == null) {
			this.setTimezoneToServerDefault();
		}

		if (this.townID != 0) {
			this.setTown(CivGlobal.getTownFromId(this.townID));
			if (this.town == null) {
				CivLog.error("COULD NOT FIND TOWN(" + this.townID + ") FOR RESIDENT(" + this.getId() + ") Name:"
						+ this.getName());
				/*
				 * When a town fails to load, we wont be able to find it above. However this can
				 * cause a cascade effect where because we couldn't find the town above, we save
				 * this resident's town as NULL which wipes their town information from the
				 * database when the resident gets saved. Just to make sure this doesn't happen
				 * the boolean below guards resident saves. There ought to be a better way...
				 */
				try {
					if (CivSettings.getStringBase("cleanupDatabase").equalsIgnoreCase("true"))
						this.saveNow();
					else
						this.dontSaveTown = true;
				} catch (InvalidConfiguration e) {
					this.dontSaveTown = true;
				}
				return;
			}
		}

		if (this.campID != 0) {
			this.setCamp(CivGlobal.getCampFromId(this.campID));
			if (this.camp == null) {
				CivLog.error("COULD NOT FIND CAMP(" + this.campID + ") FOR RESIDENT(" + this.getId() + ") Name:"
						+ this.getName());
			} else {
				camp.addMember(this);
			}
		}

		if (this.getTown() != null) {
			try {
				this.getTown().addResident(this);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			}
		}

		this.setLastOnline(rs.getLong("lastOnline"));
		this.setRegistered(rs.getLong("registered"));
		this.setDaysTilEvict(rs.getInt("daysTilEvict"));
		this.nextRefresh = rs.getLong("nextRefresh");
		this.getTreasury().setDebt(rs.getDouble("debt"));
		this.loadFriendsFromSaveString(rs.getString("friends"));
	}

	private void setTimezoneToServerDefault() {
		this.timezone = EventTimer.getCalendarInServerTimeZone().getTimeZone().getID();
	}

	public String getFlagSaveString() {
		String flagString = "";

		if (this.isShowMap()) {
			flagString += "map,";
		}

		if (this.isShowTown()) {
			flagString += "showtown,";
		}

		if (this.isShowCiv()) {
			flagString += "showciv,";
		}

		if (this.isShowScout()) {
			flagString += "showscout,";
		}

		if (this.isShowInfo()) {
			flagString += "info,";
		}

		if (this.combatInfo) {
			flagString += "combatinfo,";
		}
		if (this.isTitleAPI()) {
			flagString += "titleapi,";
		}

		if (this.itemMode.equals("rare")) {
			flagString += "itemModeRare,";
		} else if (this.itemMode.equals("none")) {
			flagString += "itemModeNone,";
		}

		return flagString;
	}

	public void loadFlagSaveString(String str) {
		if (str == null) {
			return;
		}

		String[] split = str.split(",");

		for (String s : split) {
			switch (s.toLowerCase()) {
			case "map":
				this.setShowMap(true);
				break;
			case "showtown":
				this.setShowTown(true);
				break;
			case "showciv":
				this.setShowCiv(true);
				break;
			case "showscout":
				this.setShowScout(true);
				break;
			case "info":
				this.setShowInfo(true);
				break;
			case "combatinfo":
				this.setCombatInfo(true);
				break;
			case "titleapi":
				if (CivSettings.hasTitleAPI) {
					this.setTitleAPI(true);
				} else {
					this.setTitleAPI(false);
				}
				break;
			case "itemmoderare":
				this.itemMode = "rare";
				break;
			case "itemmodenone":
				this.itemMode = "none";
				break;
			}
		}
	}

	@Override
	public void saveNow() throws SQLException {

		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		hashmap.put("name", this.getName());
		hashmap.put("uuid", this.getUid().toString());
		if (this.getTown() != null) {
			hashmap.put("town_id", this.getTown().getId());
		} else {
			if (!dontSaveTown) {
				hashmap.put("town_id", null);
			}
		}

		if (this.getCamp() != null) {
			hashmap.put("camp_id", this.getCamp().getId());
		} else {
			hashmap.put("camp_id", null);
		}

		hashmap.put("lastOnline", this.getLastOnline());
		hashmap.put("registered", this.getRegistered());
		hashmap.put("debt", this.getTreasury().getDebt());
		hashmap.put("daysTilEvict", this.getDaysTilEvict());
		hashmap.put("friends", this.getFriendsSaveString());
		hashmap.put("coins", this.getTreasury().getBalance());
		hashmap.put("timezone", this.getTimezone());
		hashmap.put("flags", this.getFlagSaveString());
		hashmap.put("last_ip", this.getLastIP());
		hashmap.put("last_unit_object_id", this.getLastUnitObjectId());
		hashmap.put("savedInventory", this.savedInventory);
		hashmap.put("isProtected", this.isProtected);
		hashmap.put("language_id", this.languageCode);
		hashmap.put("nextTeleport", this.nextTeleport);
		hashmap.put("nextRefresh", this.nextRefresh);

		if (this.getTown() != null) {
			hashmap.put("debug_town", this.getTown().getName());

			if (this.getTown().getCiv() != null) {
				hashmap.put("debug_civ", this.getCiv().getName());
			}
		}

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	public String getTownString() {
		if (town == null) {
			return "none";
		}
		return this.getTown().getName();
	}

	public boolean hasTown() {
		return town != null;
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteByName(this.getName(), TABLE_NAME);
	}

	public EconObject getTreasury() {
		return treasury;
	}

	public void setTreasury(EconObject treasury) {
		this.treasury = treasury;
	}

	public void onEnterDebt() {
		this.daysTilEvict = CivSettings.GRACE_DAYS;
	}

	public void warnDebt() {
		Player player;
		try {
			player = CivGlobal.getPlayer(this);
			CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_resident_debtmsg",
					this.getTreasury().getDebt(), CivSettings.CURRENCY_NAME));
			CivMessage.send(player, CivColor.LightGray
					+ CivSettings.localize.localizedString("var_resident_debtEvictAlert1", this.daysTilEvict));
		} catch (CivException e) {
			// Player is not online.
		}
	}

	public void decrementGraceCounters() {
		this.daysTilEvict--;
		if (this.daysTilEvict == 0) {
			this.getTown().removeResident(this);

			try {
				CivMessage.send(CivGlobal.getPlayer(this),
						CivColor.Yellow + CivSettings.localize.localizedString("resident_evictedAlert"));
			} catch (CivException e) {
				// Resident not online.
			}
			return;
		}

		if (this.getTreasury().inDebt()) {
			warnDebt();
		} else {
			warnEvict();
		}

		this.save();
	}

	public double getPropertyTaxOwed() {
		double total = 0;

		if (this.getTown() == null) {
			return total;
		}

		for (TownChunk tc : this.getTown().getTownChunks()) {
			if (tc.perms.getOwner() == this) {
				double tax = tc.getValue() * this.getTown().getTaxRate();
				total += tax;
			}
		}
		return total;
	}

	public boolean isLandOwner() {
		if (this.getTown() == null)
			return false;

		for (TownChunk tc : this.getTown().getTownChunks()) {
			if (tc.perms.getOwner() == this) {
				return true;
			}
		}

		return false;
	}

	public double getFlatTaxOwed() {
		if (this.getTown() == null)
			return 0;

		return this.getTown().getFlatTax();
	}

	public boolean isTaxExempt() {
		return this.getTown().isInGroup("mayors", this) || this.getTown().isInGroup("assistants", this);
	}

	public void payOffDebt() {
		this.getTreasury().payTo(this.getTown().getTreasury(), this.getTreasury().getDebt());
		this.getTreasury().setDebt(0);
		this.daysTilEvict = -1;
		this.save();
	}

	public void addFriend(Resident resident) {
		friends.put(resident.getName(), 1);
	}

	public boolean isFriend(Resident resident) {
		return friends.containsKey(resident.getName());
	}

	public Collection<String> getFriends() {
		return friends.keySet();
	}

	private String getFriendsSaveString() {
		String out = "";
		for (String name : friends.keySet()) {
			out += name + ",";
		}
		return out;
	}

	public void sendActionBar(String msg) {
		Bukkit.getPlayerExact(this.getName()).spigot().sendMessage(ChatMessageType.ACTION_BAR,
				new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', msg)).create());
	}

	private void loadFriendsFromSaveString(String string) {
		String[] split = string.split(",");

		for (String str : split) {
			friends.put(str, 1);
		}
	}

	public void removeFriend(Resident friendToAdd) {
		friends.remove(friendToAdd.getName());
	}

	public String getGroupsString() {
		String out = "";

		for (PermissionGroup grp : CivGlobal.getPermissionGroups()) {
			if (grp.hasMember(this)) {
				if (grp.getTown() != null) {
					if (grp.isProtectedGroup()) {
						out += CivColor.LightPurple;
					} else {
						out += CivColor.White;
					}
					out += grp.getName() + "(" + grp.getTown().getName() + ")";

				} else if (grp.getCiv() != null) {
					out += CivColor.Gold + grp.getName() + "(" + grp.getCiv().getName() + ")";
				}

				out += ", ";
			}
		}

		return out;
	}

	public void warnEvict() {
		try {
			CivMessage.send(CivGlobal.getPlayer(this), CivColor.Yellow
					+ CivSettings.localize.localizedString("var_resident_evictionNotice1", this.getDaysTilEvict()));
		} catch (CivException e) {
			// player offline.
		}
	}

	@SuppressWarnings("deprecation")
	public int takeItemsInHand(int itemId, int itemData) throws CivException {
		Player player = CivGlobal.getPlayer(this);
		Inventory inv = player.getInventory();
		if (!inv.contains(itemId)) {
			return 0;
		}

		if ((player.getInventory().getItemInMainHand().getTypeId() != itemId)
				&& (player.getInventory().getItemInMainHand().getTypeId() != itemData)) {
			return 0;
		}

		ItemStack stack = player.getInventory().getItemInMainHand();
		int count = stack.getAmount();
		inv.removeItem(stack);

		player.updateInventory();
		return count;
	}

	@SuppressWarnings("deprecation")
	public boolean takeItemInHand(int itemId, int itemData, int amount) throws CivException {
		Player player = CivGlobal.getPlayer(this);
		Inventory inv = player.getInventory();

		if (!inv.contains(itemId)) {
			return false;
		}

		if ((player.getInventory().getItemInMainHand().getTypeId() != itemId)
				&& (player.getInventory().getItemInMainHand().getTypeId() != itemData)) {
			return false;
		}

		ItemStack stack = player.getInventory().getItemInMainHand();

		if (stack.getAmount() < amount) {
			return false;
		} else if (stack.getAmount() == amount) {
			inv.removeItem(stack);
		} else {
			stack.setAmount(stack.getAmount() - amount);
		}

		player.updateInventory();
		return true;
	}

	@SuppressWarnings("deprecation")
	public boolean takeItem(int itemId, int itemData, int amount) throws CivException {
		Player player = CivGlobal.getPlayer(this);
		Inventory inv = player.getInventory();

		if (!inv.contains(itemId)) {
			return false;
		}

		HashMap<Integer, ? extends ItemStack> stacks;
		stacks = inv.all(itemId);

		for (ItemStack stack : stacks.values()) {
			if (stack.getData().getData() != (byte) itemData) {
				continue;
			}

			if (stack.getAmount() <= 0)
				continue;

			if (stack.getAmount() < amount) {
				amount -= stack.getAmount();
				stack.setAmount(0);
				inv.removeItem(stack);
				continue;
			} else {
				stack.setAmount(stack.getAmount() - amount);
				break;
			}
		}

		player.updateInventory();
		return true;
	}

	@SuppressWarnings("deprecation")
	public int giveItem(int itemId, short damage, int amount) throws CivException {
		Player player = CivGlobal.getPlayer(this);
		Inventory inv = player.getInventory();
		ItemStack stack = new ItemStack(itemId, amount, damage);
		HashMap<Integer, ItemStack> leftovers = null;
		leftovers = inv.addItem(stack);

		int leftoverAmount = 0;
		for (ItemStack i : leftovers.values()) {
			leftoverAmount += i.getAmount();
		}
		player.updateInventory();
		return amount - leftoverAmount;
	}

	public boolean buyItem(String itemName, int id, byte data, double price, int amount) throws CivException {

		if (!this.getTreasury().hasEnough(price)) {
			throw new CivException(
					CivSettings.localize.localizedString("resident_notEnoughMoney") + " " + CivSettings.CURRENCY_NAME);
		}

		boolean completed = true;
		int bought = 0;
		bought = giveItem(id, data, amount);
		if (bought != amount) {
			this.getTreasury().withdraw(price);
			takeItem(id, data, bought);
			completed = false;
		} else {
			this.getTreasury().withdraw(price);
		}

		if (completed) {
			return true;
		} else {
			throw new CivException(CivSettings.localize.localizedString("resident_buyInvenFull"));
		}
	}

	public Civilization getCiv() {
		if (this.getTown() == null) {
			return null;
		}
		return this.getTown().getCiv();
	}

	@SuppressWarnings("deprecation")
	public void setScoreboardName(String name, String key) {
		if (this.scoreboard == null) {
			this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
			Team team = this.scoreboard.registerNewTeam("team");
			team.addPlayer(CivGlobal.getFakeOfflinePlayer(key));
			team.setDisplayName(name);
		} else {
			Team team = this.scoreboard.getTeam("team");
			team.setDisplayName(name);
		}

	}

	@SuppressWarnings("deprecation")
	public void setScoreboardValue(String name, String key, int value) {
		if (this.scoreboard == null) {
			return;
		}

		Objective obj = scoreboard.getObjective("obj:" + key);
		if (obj == null) {
			obj = scoreboard.registerNewObjective(name, "dummy");
			obj.setDisplaySlot(DisplaySlot.SIDEBAR);
			Score score = obj.getScore(CivGlobal.getFakeOfflinePlayer(key));
			score.setScore(value);
		} else {
			Score score = obj.getScore(CivGlobal.getFakeOfflinePlayer(key));
			score.setScore(value);
		}
	}

	public void showScoreboard() {
		if (this.scoreboard != null) {
			Player player;
			try {
				player = CivGlobal.getPlayer(this);
				player.setScoreboard(this.scoreboard);
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
	}

	public void hideScoreboard() {
		Player player;
		try {
			player = CivGlobal.getPlayer(this);
			player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		} catch (CivException e) {
			e.printStackTrace();
		}
	}

	public boolean isSBPermOverride() {
		return sbperm;
	}

	public void setSBPermOverride(boolean b) {
		sbperm = b;
	}

	public void setInteractiveMode(InteractiveResponse interactive) {
		this.interactiveMode = true;
		this.interactiveResponse = interactive;
	}

	public void clearInteractiveMode() {
		this.interactiveMode = false;
		this.interactiveResponse = null;
	}

	public InteractiveResponse getInteractiveResponse() {
		return this.interactiveResponse;
	}

	public boolean hasCamp() {
		return (this.camp != null);
	}

	public String getCampString() {
		if (this.camp == null) {
			return "none";
		}
		return this.camp.getName();
	}

	public void showWarnings(Player player) {
		/* Notify Resident of any invalid structures. */
		if (this.getTown() != null) {
			for (Buildable struct : this.getTown().invalidStructures) {
				CivMessage.send(player,
						CivColor.Yellow + ChatColor.BOLD
								+ CivSettings.localize.localizedString("var_resident_structInvalidAlert1",
										struct.getDisplayName(), struct.getCorner())
								+ " " + CivSettings.localize.localizedString("resident_structInvalidAlert2") + " "
								+ struct.getInvalidReason());
			}

			/* Show any event messages. */
			if (this.getTown().getActiveEvent() != null) {
				CivMessage.send(player,
						CivColor.Yellow + CivSettings.localize.localizedString("var_resident_eventNotice1",
								this.getTown().getActiveEvent().configRandomEvent.name));
			}
		}

	}

	public void startPreviewTask(Template tpl, Block block, UUID uuid) {
		this.previewTask = new BuildPreviewAsyncTask(tpl, block, uuid);
		TaskMaster.asyncTask(previewTask, 0);
	}

	public void undoPreview() {
		if (this.previewUndo == null) {
			this.previewUndo = new ConcurrentHashMap<BlockCoord, SimpleBlock>();
			return;
		}

		if (this.previewTask != null) {
			previewTask.lock.lock();
			try {
				previewTask.aborted = true;
			} finally {
				previewTask.lock.unlock();
			}
		}

		try {
			Player player = CivGlobal.getPlayer(this);
			Resident resident = this;
			TaskMaster.asyncTask(new Runnable() {
				@Override
				public void run() {
					for (BlockCoord coord : resident.previewUndo.keySet()) {
//						int count = 0;
						SimpleBlock sb = resident.previewUndo.get(coord);
						ItemManager.sendBlockChange(player, coord.getLocation(), sb.getType(), sb.getData());
//						count++;
//						if (count > 50) {
//							count = 0;
//							try {
//								Thread.sleep(1000);
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
//						}
					}
					resident.previewUndo.clear();
					resident.previewUndo = new ConcurrentHashMap<BlockCoord, SimpleBlock>();
				}
			}, 0);
		} catch (CivException e) {
			return;// Fall down and return.
		}
	}

	public void onRoadTest(BlockCoord coord, Player player) {
		/* Test the block beneath us for a road, if so, set the road flag. */
		BlockCoord feet = new BlockCoord(coord);
		feet.setY(feet.getY() - 1);
		RoadBlock rb = CivGlobal.getRoadBlock(feet);

		onRoad = rb != null;
	}

	public void giveTemplate(String name) {
		int perkCount;
		try {
			perkCount = CivSettings.getInteger(CivSettings.perkConfig, "system.free_perk_count");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		for (ConfigPerk p : CivSettings.perks.values()) {
			Perk perk = new Perk(p);

			if (perk.getConfigId().startsWith(("tpl_" + name).toLowerCase())
					|| perk.getConfigId().startsWith(("template_" + name).toLowerCase())) {
				perk.count = perkCount;
				this.perks.put(perk.getConfigId(), perk);
			}
		}

	}

	public void giveAllFreePerks() {
		int perkCount;
		try {
			perkCount = CivSettings.getInteger(CivSettings.perkConfig, "system.free_perk_count");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		for (ConfigPerk p : CivSettings.perks.values()) {
			Perk perk = new Perk(p);

			if (perk.getConfigId().startsWith("perk_")) {
				perk.count = perkCount;
				this.perks.put(perk.getConfigId(), perk);
			}
		}

	}

	public void loadPerks(final Player player) {
		class AsyncTask implements Runnable {
			Resident resident;

			public AsyncTask(Resident resident) {
				this.resident = resident;
			}

			@Override
			public void run() {
//				try {
//					resident.perks.clear();
//					Player player = CivGlobal.getPlayer(resident);
//					try {
//						CivGlobal.perkManager.loadPerksForResident(resident);
//					} catch (SQLException e) {
//						CivMessage.sendError(player, CivSettings.localize.localizedString("resident_couldnotloadperks"));
//						e.printStackTrace();
//						return;
//					} catch (NotVerifiedException e) {
//						return;
//					}
//				} catch (CivException e1) {
//					return;
//				}
				try {

					String perkMessage = "";
					if (CivSettings.getString(CivSettings.perkConfig, "system.free_perks").equalsIgnoreCase("true")) {
						resident.giveAllFreePerks();
						perkMessage = CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg1") + " ";
					} else if (CivSettings.getString(CivSettings.perkConfig, "system.free_admin_perks")
							.equalsIgnoreCase("true")) {
						if (player.hasPermission(CivSettings.MINI_ADMIN)
								|| player.hasPermission(CivSettings.FREE_PERKS)) {
							resident.giveAllFreePerks();
							perkMessage = CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg1") + ": ";
							perkMessage += "Weather" + ", ";
						}
					}

					for (ConfigPerk p : CivSettings.templates.values()) {
						if (player.hasPermission("civ.perk." + p.simple_name)) {
							resident.giveTemplate(p.simple_name);
							perkMessage += p.display_name + ", ";
						}
					}

					perkMessage += CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg2");

					CivMessage.send(resident, CivColor.LightGreen + perkMessage);
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
				}
			}
		}

		TaskMaster.asyncTask(new AsyncTask(this), 0);
	}

	public void setRejoinCooldown(Town town) {
		String value = "" + town.getCiv().getId();
		String key = getCooldownKey();
		CivGlobal.getSessionDatabase().add(key, value, 0, 0, 0);
	}

	public String getCooldownKey() {
		return "cooldown:" + this.getName();
	}

	public void cleanupCooldown() {
		CivGlobal.getSessionDatabase().delete_all(getCooldownKey());
	}

	public void validateJoinTown(Town town) throws CivException {
		if (this.hasTown() && this.getCiv() == town.getCiv()) {
			/* allow players to join the same civ, no probs */
			return;
		}

		long cooldownTime;
		int cooldownHours;
		try {
			cooldownHours = CivSettings.getInteger(CivSettings.civConfig, "global.join_civ_cooldown");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		cooldownTime = cooldownHours * 60 * 60 * 1000; /* convert hours to milliseconds. */

		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getCooldownKey());
		if (entries.size() > 0) {
			Civilization oldCiv = CivGlobal.getCivFromId(Integer.valueOf(entries.get(0).value));
			if (oldCiv == null) {
				/* Hmm old civ is gone. */
				cleanupCooldown();
				return;
			}

//			if (oldCiv == town.getCiv()) {
//				/* We're rejoining the old civ, allow it. */
//				return;
//			}

			/* Check if cooldown is expired. */
			Date now = new Date();
			if (now.getTime() > (entries.get(0).time + cooldownTime)) {
				/* Entry is expired, allow cooldown and cleanup. */
				cleanupCooldown();
				return;
			}
			// XXX from furnex throw new
			// CivException(CivSettings.localize.localizedString("var_resident_cannotJoinCivJustLeft1",
			// "§e" + CivGlobal.dateFormat.format(entries.get(0).time + cooldownTime) +
			// "§c", "§6" + this.getName() + "§c", "§a" + town.getName() + "§c", "§2" +
			// CivGlobal.dateFormat.format(entries.get(0).time) + "§c"));
			throw new CivException(
					CivSettings.localize.localizedString("var_resident_cannotJoinCivJustLeft1", cooldownHours));
		}
	}

	public LinkedList<Perk> getPersonalTemplatePerks(ConfigBuildableInfo info) {
		LinkedList<Perk> templates = new LinkedList<Perk>();

		for (Perk perk : this.perks.values()) {
			CustomPersonalTemplate customTemplate = (CustomPersonalTemplate) perk
					.getComponent("CustomPersonalTemplate");
			if (customTemplate == null) {
				continue;
			}

			if (customTemplate.getString("id").equals(info.id)) {
				templates.add(perk);
			}
		}
		return templates;
	}

	public ArrayList<Perk> getUnboundTemplatePerks(ArrayList<Perk> alreadyBoundPerkList, ConfigBuildableInfo info) {
		ArrayList<Perk> unboundPerks = new ArrayList<Perk>();
		for (Perk ourPerk : perks.values()) {

			if (!ourPerk.getConfigId().contains("template")) {
				CustomTemplate customTemplate = (CustomTemplate) ourPerk.getComponent("CustomTemplate");
				if (customTemplate == null) {
					continue;
				}

				if (!customTemplate.getString("template").equals(info.template_name)) {
					/* Not the correct template. */
					continue;
				}

				boolean has = false;
				for (Perk perk : alreadyBoundPerkList) {
					if (perk.getConfigId().equals(ourPerk.getConfigId())) {
						/* Perk is already bound in this town, do not display for binding. */
						has = true;
						continue;
					}
				}

				if (!has)
					unboundPerks.add(ourPerk);
			}
		}

		return unboundPerks;
	}

	public void setControlBlockInstantBreak(boolean controlBlockInstantBreak) {
		this.controlBlockInstantBreak = controlBlockInstantBreak;
	}

	public boolean isInactiveForDays(int days) {
		Calendar now = Calendar.getInstance();
		Calendar expire = Calendar.getInstance();
		expire.setTimeInMillis(this.getLastOnline());

		expire.add(Calendar.DATE, days);

		if (now.after(expire)) {
			return true;
		}

		return false;
	}

	public Inventory startTradeWith(Resident resident) {
		try {
			Player player = CivGlobal.getPlayer(this);
			if (player.isDead()) {
				throw new CivException(CivSettings.localize.localizedString("resident_tradeErrorPlayerDead"));
			}
			Inventory inv = Bukkit.createInventory(player, 9 * 5, this.getName() + " : " + resident.getName());

			/* Set up top and bottom layer with buttons. */

			/* Top part which is for the other resident. */
			ItemStack signStack = LoreGuiItem.build("", CivData.WOOL, CivData.DATA_WOOL_WHITE, "");
			int start = 0;
			for (int i = start; i < (9 + start); i++) {
				if ((i - start) == 8) {
					ItemStack guiStack = LoreGuiItem.build(resident.getName() + " Confirm", CivData.WOOL,
							CivData.DATA_WOOL_RED,
							CivColor.LightGreen + CivSettings.localize.localizedString("var_resident_tradeWait1",
									CivColor.LightBlue + resident.getName()),
							CivColor.LightGray + " " + CivSettings.localize.localizedString("resident_tradeWait2"));
					inv.setItem(i, guiStack);
				} else if ((i - start) == 7) {
					ItemStack guiStack = LoreGuiItem.build(
							CivSettings.CURRENCY_NAME + " "
									+ CivSettings.localize.localizedString("resident_tradeOffered"),
							ItemManager.getMaterialId(Material.NETHER_BRICK_ITEM), 0,
							CivColor.Yellow + "0 " + CivSettings.CURRENCY_NAME);
					inv.setItem(i, guiStack);
				} else {
					inv.setItem(i, signStack);
				}
			}

			start = 4 * 9;
			for (int i = start; i < (9 + start); i++) {
				if ((i - start) == 8) {
					ItemStack guiStack = LoreGuiItem.build(
							CivSettings.localize.localizedString("resident_tradeYourConfirm"), CivData.WOOL,
							CivData.DATA_WOOL_RED,
							CivColor.Gold + CivSettings.localize.localizedString("resident_tradeClicktoConfirm"));
					inv.setItem(i, guiStack);

				} else if ((i - start) == 0) {
					ItemStack guiStack = LoreGuiItem.build(
							CivSettings.localize.localizedString("resident_tradeRemove") + " "
									+ CivSettings.CURRENCY_NAME,
							ItemManager.getMaterialId(Material.NETHER_BRICK_ITEM), 0,
							CivColor.Gold + CivSettings.localize.localizedString("resident_tradeRemove100") + " "
									+ CivSettings.CURRENCY_NAME,
							CivColor.Gold + CivSettings.localize.localizedString("resident_tradeRemove1000") + " "
									+ CivSettings.CURRENCY_NAME);
					inv.setItem(i, guiStack);
				} else if ((i - start) == 1) {
					ItemStack guiStack = LoreGuiItem.build(
							CivSettings.localize.localizedString("resident_tradeAdd") + " " + CivSettings.CURRENCY_NAME,
							ItemManager.getMaterialId(Material.GOLD_INGOT), 0,
							CivColor.Gold + CivSettings.localize.localizedString("resident_tradeAdd100") + " "
									+ CivSettings.CURRENCY_NAME,
							CivColor.Gold + CivSettings.localize.localizedString("resident_tradeAdd1000") + " "
									+ CivSettings.CURRENCY_NAME);
					inv.setItem(i, guiStack);
				} else if ((i - start) == 7) {
					ItemStack guiStack = LoreGuiItem.build(
							CivSettings.CURRENCY_NAME + " "
									+ CivSettings.localize.localizedString("resident_tradeOffered"),
							ItemManager.getMaterialId(Material.NETHER_BRICK_ITEM), 0,
							CivColor.Yellow + "0 " + CivSettings.CURRENCY_NAME);
					inv.setItem(i, guiStack);
				} else {
					inv.setItem(i, signStack);
				}
			}

			/* Set up middle divider. */
			start = 2 * 9;
			for (int i = start; i < (9 + start); i++) {
				inv.setItem(i, signStack);
			}

			player.openInventory(inv);
			return inv;
		} catch (CivException e) {
			try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("possibleCheaters.txt", true)))) {
				out.println("trade:" + this.getName() + " WITH " + resident.getName() + " and was dead");
			} catch (IOException e1) {
				// exception handling left as an exercise for the reader
			}

			CivMessage.sendError(this,
					CivSettings.localize.localizedString("resident_tradeCouldNotTrade") + " " + e.getMessage());
			CivMessage.sendError(resident,
					CivSettings.localize.localizedString("resident_tradeCouldNotTrade") + " " + e.getMessage());
			return null;
		}

	}

	public boolean hasTechForItem(ItemStack stack) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat == null) {
			return true;
		}

		if (craftMat.getConfigMaterial().required_tech == null) {
			return true;
		}

		if (!this.hasTown()) {
			return false;
		}

		/* Parse technoloies */
		String[] split = craftMat.getConfigMaterial().required_tech.split(",");
		for (String tech : split) {
			tech = tech.replace(" ", "");
			if (!this.getCiv().hasTechnology(tech)) {
				return false;
			}
		}

		return true;
	}

	public void toggleItemMode() {
		if (this.itemMode.equals("all")) {
			this.itemMode = "rare";
			CivMessage.send(this,
					CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemRare"));
		} else if (this.itemMode.equals("rare")) {
			this.itemMode = "none";
			CivMessage.send(this,
					CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemNone"));
		} else {
			this.itemMode = "all";
			CivMessage.send(this, CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemAll"));
		}
		this.save();
	}

	public void teleportHome() {
		try {
			Player player = CivGlobal.getPlayer(this);
			if (this.hasTown()) {
				Townhall townhall = this.getTown().getTownHall();
				if (townhall != null) {
					BlockCoord coord = townhall.getRandomRevivePoint();
					player.teleport(coord.getLocation());
				}
			} else {
				World world = Bukkit.getWorld("world");
				player.teleport(world.getSpawnLocation());
			}
		} catch (CivException e) {
			return;
		}
	}

	public boolean canDamageControlBlock() {
		return (!this.hasTown()) || (this.getCiv().getCapitolStructure().isValid());
	}

	public void saveInventory() {
		try {
			Player player = CivGlobal.getPlayer(this);
			String serial = InventorySerializer.InventoryToString(player.getInventory());
			this.setSavedInventory(serial);
			this.save();
		} catch (CivException e) {
		}
	}

	public void clearInventory() {
		try {
			Player player = CivGlobal.getPlayer(this);
			player.getInventory().clear();
			player.getInventory().setArmorContents(new ItemStack[4]);
		} catch (CivException e) {
		}
	}

	public void restoreInventory() {
		if (this.savedInventory == null) {
			return;
		}

		try {
			Player player = CivGlobal.getPlayer(this);
			clearInventory();
			InventorySerializer.StringToInventory(player.getInventory(), this.savedInventory);
			this.setSavedInventory(null);
			this.save();
		} catch (CivException e) {
			// Player offline??
			e.printStackTrace();
			this.setSavedInventory(null);
			this.save();
		}
	}

	public void calculateWalkingModifier(Player player) {
		Double sumValue = 0.0;
		ItemStack[] stacks = player.getInventory().getArmorContents();
		for (ItemStack is : stacks) {
			if (is == null)
				continue;
			if (UnitStatic.isWearingAnyIron(is.getType()))
				sumValue = sumValue + UnitStatic.T1_metal_speed;
			if (UnitStatic.isWearingAnyChain(is.getType()))
				sumValue = sumValue + UnitStatic.T2_metal_speed;
			if (UnitStatic.isWearingAnyGold(is.getType()))
				sumValue = sumValue + UnitStatic.T3_metal_speed;
			if (UnitStatic.isWearingAnyDiamond(is.getType()))
				sumValue = sumValue + UnitStatic.T4_metal_speed;
			AttributeUtil attrs = new AttributeUtil(is);
			if (attrs.hasEnhancement("LoreEnhancementSpeed")) {
				sumValue = sumValue + Double.valueOf(attrs.getEnhancementData("LoreEnhancementSpeed", "value"));
			}
		}
		this.walkingModifier = UnitStatic.normal_speed * (1.0 + sumValue);
	}

	public static String getNameTagColor(final Civilization civ) {
		if (civ.isAdminCiv()) {
			return "§c";
		}
		switch (civ.getCurrentEra()) {
		case 0:
			return "§f";
		case 1:
			return "§e";
		case 2:
			return "§d";
		case 3:
			return "§a";
		case 4:
			return "§6";
		case 5:
			return "§2";
		case 6:
			return "§b";
		default:
			return "§5";
		}
	}

	@Deprecated
	public void setLanguageCode(int code) {
		// TODO: Need to validate if language code is supported.
		this.languageCode = code;
	}

	public static String plurals(final int count, final String... pluralForms) {
		final int i = (count % 10 == 1 && count % 100 != 11) ? 0
				: ((count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) ? 1 : 2);
		return pluralForms[i];
	}

	public Town getSelectedTown(final Player player) {
		final Resident resident = CivGlobal.getResident(player);
		if (resident == null || resident.getTown() == null) {
			return null;
		}
		if (resident.getSelectedTown() != null) {
			try {
				resident.getSelectedTown().validateResidentSelect(resident);
			} catch (CivException e) {
				CivMessage.send(player, "§e" + CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid",
						resident.getSelectedTown().getName(), resident.getTown().getName()));
				resident.setSelectedTown(resident.getTown());
				return resident.getTown();
			}
			return resident.getSelectedTown();
		}
		return resident.getTown();
	}

	public boolean isPoisonImmune() {
		return Calendar.getInstance().after(poisonImmune);
	}

	public void addPosionImmune() {
		poisonImmune = Calendar.getInstance().getTimeInMillis() + 1000 * Resident.POISON_DURATION;
	}

	public boolean isLevitateImmune() {
		return Calendar.getInstance().after(levitateImmune);
	}

	public void addLevitateImmune() {
		levitateImmune = Calendar.getInstance().getTimeInMillis() + 1000 * (Resident.LEVITATE_DURATION + 3);
	}

	public void addPLCImmune(final int seconds) {
		nextPLCDamage = System.currentTimeMillis() + TimeTools.toTicks(seconds);
	}

	public boolean isPLCImmuned() {
		return nextPLCDamage > System.currentTimeMillis();
	}

	public void lightningStrike(final boolean repeat, final Town source) {
		Player player;
		try {
			player = CivGlobal.getPlayer(this);
		} catch (CivException e) {
			return;
		}
		if (player == null) {
			return;
		}
		int dmg = 7;
		Structure tesla = source.getStructureByType("s_teslatower");
		if (tesla != null) {
			dmg = ((TeslaTower) tesla).getDamage();
		}
//		tesla = source.getStructureByType("s_teslaship");
//		if (tesla != null) {
//			dmg = ((TeslaShip) tesla).getDamage();
//		}
		final LivingEntity target = (LivingEntity) player;
		if (target.getHealth() - dmg > 0.0) {
			target.setHealth(target.getHealth() - dmg);
			target.damage(0.5);
		} else {
			target.setHealth(0.1);
			target.damage(1.0);
		}
		target.setFireTicks(60);
		if (repeat) {
			Bukkit.getScheduler().runTaskLater(CivCraft.getPlugin(), () -> this.lightningStrike(false, source), 30L);
		}
	}

	public void setUnitObjectId(int uId) {
		if (uId > 0) {
			CivGlobal.getUnitObject(uId).setLastResident(this);
			this.unitObjectId = uId;
			this.lastUnitObjectId = uId;
			this.save();
		} else
			this.unitObjectId = uId;
	}

	public boolean isUnitActive() {
		return this.unitObjectId > 0;
	}
}
