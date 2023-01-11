/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.object;

import com.avrgaming.gpl.InventorySerializer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.structures.TeslaTower;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.SpeedEnchantment;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.guiinventory.Trade;
import com.avrgaming.civcraft.interactive.InteractiveResponse;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.global.perks.Perk;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;

@Getter
@Setter
public class Resident extends SQLObject {

	private Town town = null;
	private Camp camp = null;
	private boolean campChat = false;
	private boolean townChat = false;
	private boolean civChat = false;
	private boolean adminChat = false;
	private boolean combatInfo = false;
	private boolean showScout = true;
	private boolean showTown = true;
	private boolean showCiv = true;
	private boolean showMap = false;
	private boolean showInfo = false;
	private String itemMode = "all";
	@Deprecated
	private int languageCode = 1033;

	/** Town to chat in besides your own. /ad tc <town> */
	private Town townChatOverride = null;
	/** Civ to chat in besides your own. /ad cc <civ> */
	private Civilization civChatOverride = null;
	/** Ignore plot permission. /ad perm */
	private boolean permOverride = false;
	/** Ignore constructBlock. /ad perm */
	private boolean sBPermOverride = false;

	private String timezone;

	/** Когда можна будет делать рефреш постройки */
	private long nextRefresh;

	private long registered;
	private long lastOnline;
	private boolean givenKit;
	private EconObject treasury;
	private boolean muted;
	private Date muteExpires = null;

	private boolean interactiveMode = false;
	private InteractiveResponse interactiveResponse = null;
	/* XXX This buildable is used as place to store which buildable we're working on when interacting with GUI items. We want to be able to pass
	 * the buildable object to the GUI's action function, but there isn't a good way to do this ATM. If we had a way to send arbitary objects it
	 * would be better. Could we store it here on the resident object? */
	public CallbackInterface pendingCallback;

	private int unitObjectId = 0;
	private int lastUnitObjectId = 0;
	private boolean performingMission = false;
	public Resident lastAttacker = null;
	public long lastAttackTime;

	private Town selectedTown = null;

	/** Если нужно повтороное нажатие на табличку, то эта переменная служит памятью последнего нажатия */
	public ConstructSign constructSignConfirm = null;

	private String savedInventory = null;
	private boolean isProtected = false;

	public ConcurrentHashMap<BlockCoord, SimpleBlock> previewUndo = new ConcurrentHashMap<BlockCoord, SimpleBlock>();
	public LinkedHashMap<String, Perk> perks = new LinkedHashMap<String, Perk>();
	private Date lastKilledTime = null;
	private String lastIP = "";
	public UUID uuid;
	private double walkingModifier = UnitStatic.normal_speed;

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
	public boolean isRefresh;

	public Resident(UUID uuid, String name) throws InvalidNameException {
		this.setName(name);
		this.uuid = uuid;
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
					"`coins` double DEFAULT 0," + //
					"`camp_id` int(11)," + //
					"`nextTeleport` BIGINT NOT NULL DEFAULT '0'," + //
					"`nextRefresh` BIGINT NOT NULL DEFAULT '0'," + //
					"`timezone` mediumtext," + //
					"`last_unit_object_id` int(11) NOT NULL DEFAULT '0'," + //
					"`savedInventory` mediumtext DEFAULT NULL," + //
					"`isProtected` bool NOT NULL DEFAULT '0'," + //
					"`flags` mediumtext DEFAULT NULL," + //
					"`last_ip` mediumtext DEFAULT NULL," + //
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

	// --------------------Load save begin
	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		int townID = rs.getInt("town_id");
		int campID = rs.getInt("camp_id");
		this.lastIP = rs.getString("last_ip");

		this.uuid = (rs.getString("uuid").equalsIgnoreCase("UNKNOWN")) ? null : UUID.fromString(rs.getString("uuid"));

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

		if (this.getTimezone() == null) this.setTimezoneToServerDefault();

		if (townID != 0) {
			this.setTown(CivGlobal.getTown(townID));
			if (this.town == null) {
				CivLog.error("COULD NOT FIND TOWN(" + townID + ") FOR RESIDENT(" + this.getId() + ") Name:" + this.getName());
				/* When a town fails to load, we wont be able to find it above. However this can cause a cascade effect where because we couldn't find the
				 * town above, we save this resident's town as NULL which wipes their town information from the database when the resident gets saved. Just
				 * to make sure this doesn't happen the boolean below guards resident saves. There ought to be a better way... */
				try {
					if (CivSettings.getStringBase("cleanupDatabase").equalsIgnoreCase("true")) this.saveNow();
				} catch (InvalidConfiguration e) {}
			}
		}

		if (campID != 0) {
			this.setCamp(CivGlobal.getCampFromId(campID));
			if (this.camp == null) {
				CivLog.error("COULD NOT FIND CAMP(" + campID + ") FOR RESIDENT(" + this.getId() + ") Name:" + this.getName());
			} else {
				camp.addMember(this);
				if (camp.getOwnerUuid().equals(this.getUuid())) camp.setSQLOwner(this);
			}
		}

		if (this.getTown() != null) {
			this.getTown().residents.put(this.getName().toLowerCase(), this);
		}

		this.setLastOnline(rs.getLong("lastOnline"));
		this.setRegistered(rs.getLong("registered"));
		this.nextRefresh = rs.getLong("nextRefresh");
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		hashmap.put("name", this.getName());
		hashmap.put("uuid", this.getUuid().toString());
		hashmap.put("town_id", (getTown() != null ? getTown().getId() : 0));
		hashmap.put("camp_id", (getCamp() != null ? getCamp().getId() : 0));
		hashmap.put("lastOnline", this.getLastOnline());
		hashmap.put("registered", this.getRegistered());
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

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	private void setTimezoneToServerDefault() {
		this.timezone = EventTimer.getCalendarInServerTimeZone().getTimeZone().getID();
	}

	public String getFlagSaveString() {
		String flagString = "";

		if (this.isShowMap()) flagString += "map,";
		if (this.isShowTown()) flagString += "showtown,";
		if (this.isShowCiv()) flagString += "showciv,";
		if (this.isShowScout()) flagString += "showscout,";
		if (this.isShowInfo()) flagString += "info,";
		if (this.combatInfo) flagString += "combatinfo,";
		if (this.itemMode.equals("rare")) {
			flagString += "itemModeRare,";
		} else
			if (this.itemMode.equals("none")) {
				flagString += "itemModeNone,";
			}

		return flagString;
	}

	public void loadFlagSaveString(String str) {
		if (str == null) return;
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
			case "itemmoderare":
				this.itemMode = "rare";
				break;
			case "itemmodenone":
				this.itemMode = "none";
				break;
			}
		}
	}

	// ---------------------- load save end

	@Override
	public void delete() throws SQLException {
		SQL.deleteByName(this.getName(), TABLE_NAME);
	}

	// -------------- show info

	public String getGroupsString() {
		ArrayList<PermissionGroup> groups = new ArrayList<PermissionGroup>();
		for (Town t : CivGlobal.getTowns()) {
			for (PermissionGroup grp : t.GM.getAllGroups()) {
				groups.add(grp);
			}
		}
		for (Civilization civ : CivGlobal.getCivs()) {
			groups.addAll(civ.GM.getProtectedGroups());
		}

		String out = "";
		for (PermissionGroup grp : groups) {
			if (grp.hasMember(this)) {
				if (grp.getTown() != null) {
					if (grp.getTown().GM.getDefaultGroup() == grp) continue;
					if (grp.getTown().GM.getMayorGroup() == grp) out += CivColor.LightPurple;
					if (grp.getTown().GM.getAssistantGroup() == grp) out += CivColor.LightPurpleItalic;
					if (!grp.getTown().GM.isProtectedGroup(grp)) out += CivColor.White;
					out += grp.getName() + "(town:" + grp.getTown().getName() + ")";
				}
				if (grp.getCiv() != null) {
					if (grp.getCiv().GM.getLeaderGroup() == grp) out += CivColor.Gold;
					if (grp.getCiv().GM.getAdviserGroup() == grp) out += CivColor.GoldItalic;
					if (!grp.getCiv().GM.isProtectedGroup(grp)) out += CivColor.Gray;
					out += grp.getName() + "(civ:" + grp.getCiv().getName() + ")";
				}
				out += ", ";
			}
		}
		return out;
	}

	public String getCivName() {
		return getCiv() == null ? "" : getCiv().getName();
	}

	public String getTownName() {
		return getTown() == null ? "" : getTown().getName();
	}

	public String getCampName() {
		return getCamp() == null ? "" : getCamp().getName();
	}

	// ------------- getters
	public Civilization getCiv() {
		if (this.getTown() == null) return null;
		return this.getTown().getCiv();
	}

	public Town getSelectedTown() {
		if (this.getTown() == null) return null;
		if (this.selectedTown != null) {
			try {
				this.selectedTown.validateResidentSelect(this);
			} catch (CivException e) {
				CivMessage.send(this, "§e" + CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid", this.getSelectedTown().getName(), this.getTown().getName()));
				this.selectedTown = this.getTown();
				return this.getTown();
			}
			return this.selectedTown;
		}
		return this.getTown();
	}

	// ---------------- boolean

	public boolean hasTown() {
		return town != null;
	}

	public boolean hasCamp() {
		return (this.camp != null);
	}

	public boolean hasTechForItem(ItemStack stack) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat == null) return true;
		if (craftMat.getConfigMaterial().required_tech == null) return true;
		if (!this.hasTown()) return false;

		/* Parse technoloies */
		String[] split = craftMat.getConfigMaterial().required_tech.split(",");
		for (String tech : split) {
			tech = tech.replace(" ", "");
			if (!this.getCiv().hasTechnologys(tech)) return false;
		}
		return true;
	}

	public boolean canDamageControlBlock() {
		return (!this.hasTown()) || (this.getCiv().isValid());
	}

	public boolean isInactiveForDays(int days) {
		Calendar now = Calendar.getInstance();
		Calendar expire = Calendar.getInstance();
		expire.setTimeInMillis(this.getLastOnline());
		expire.add(Calendar.DATE, days);
		return now.after(expire);
	}

	// -------------- interactive callback

	public void setInteractiveMode(InteractiveResponse interactive) {
		this.interactiveMode = true;
		this.interactiveResponse = interactive;
	}

	public void clearInteractiveMode() {
		this.interactiveMode = false;
		this.interactiveResponse = null;
		this.pendingCallback = null;
	}

	public InteractiveResponse getInteractiveResponse() {
		return this.interactiveResponse;
	}

	// -------------------- validateJoinTown begin
	public void loadPerks(final Player player) {
		class AsyncTask implements Runnable {
			Resident resident;

			public AsyncTask(Resident resident) {
				this.resident = resident;
			}

			@Override
			public void run() {
				try {
					String perkMessage = "";
					if (CivSettings.getString(CivSettings.perkConfig, "system.free_perks").equalsIgnoreCase("true")) {
						perkMessage = CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg1") + " ";
					} else
						if (CivSettings.getString(CivSettings.perkConfig, "system.free_admin_perks").equalsIgnoreCase("true")) {
							if (player.hasPermission(CivSettings.MINI_ADMIN) || player.hasPermission(CivSettings.FREE_PERKS)) {
								perkMessage = CivSettings.localize.localizedString("PlayerLoginAsync_perksMsg1") + ": ";
								perkMessage += "Weather" + ", ";
							}
						}

					for (ConfigPerk p : CivSettings.templates.values()) {
						if (player.hasPermission("civ.perk." + p.simple_name)) {
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
			Civilization oldCiv = CivGlobal.getCiv(Integer.valueOf(entries.get(0).value));
			if (oldCiv == null) {
				/* Hmm old civ is gone. */
				cleanupCooldown();
				return;
			}

			// if (oldCiv == town.getCiv()) {
			// /* We're rejoining the old civ, allow it. */
			// return;
			// }

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
			throw new CivException(CivSettings.localize.localizedString("var_resident_cannotJoinCivJustLeft1", cooldownHours));
		}
	}

	// -------------------- inventory

	public void saveInventory() {
		try {
			Player player = CivGlobal.getPlayer(this);
			String serial = InventorySerializer.InventoryToString(player.getInventory());
			this.setSavedInventory(serial);
			this.save();
		} catch (CivException e) {}
	}

	public void clearInventory() {
		try {
			Player player = CivGlobal.getPlayer(this);
			player.getInventory().clear();
			player.getInventory().setArmorContents(new ItemStack[4]);
		} catch (CivException e) {}
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

	// ------------------ Immune

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

	// --------------- unit

	public void setUnitObjectId(int uId) {
		if (uId > 0) {
			this.lastUnitObjectId = uId;
			this.save();
		}
		this.unitObjectId = uId;
	}

	public boolean isUnitActive() {
		return this.unitObjectId > 0;
	}

	// ------------------- other

	public GuiInventory startTradeWith(Resident tradeResident) {
		try {
			Player player = CivGlobal.getPlayer(this);
			if (player.isDead()) throw new CivException(CivSettings.localize.localizedString("resident_tradeErrorPlayerDead"));
			GuiInventory gi = new Trade(player, tradeResident.getName());
			gi.openInventory(player);
			return gi;
		} catch (CivException e) {
			try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("possibleCheaters.txt", true)))) {
				out.println("trade:" + this.getName() + " WITH " + tradeResident.getName() + " and was dead");
			} catch (IOException e1) {
				// exception handling left as an exercise for the reader
			}
			CivMessage.sendError(this, CivSettings.localize.localizedString("resident_tradeCouldNotTrade") + " " + e.getMessage());
			CivMessage.sendError(tradeResident, CivSettings.localize.localizedString("resident_tradeCouldNotTrade") + " " + e.getMessage());
			return null;
		}
	}

	public void toggleItemMode() {
		if (this.itemMode.equals("all")) {
			this.itemMode = "rare";
			CivMessage.send(this, CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemRare"));
		} else
			if (this.itemMode.equals("rare")) {
				this.itemMode = "none";
				CivMessage.send(this, CivColor.LightGreen + CivSettings.localize.localizedString("resident_toggleItemNone"));
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
				Cityhall cityhall = this.getTown().getCityhall();
				if (cityhall != null) {
					BlockCoord coord = cityhall.getRandomRevivePoint();
					player.teleport(coord.getLocation());
				}
			} else {
				player.teleport(CivCraft.mainWorld.getSpawnLocation());
			}
		} catch (CivException e) {
			return;
		}
	}

	public void calculateWalkingModifier(Player player) {
		Double percentModifer = 0.0;
		ItemStack[] stacks = player.getInventory().getArmorContents();
		for (ItemStack is : stacks) {
			if (is == null) continue;
			if (UnitStatic.isWearingAnyIron(is.getType())) percentModifer = percentModifer + UnitStatic.T1_metal_speed;
			if (UnitStatic.isWearingAnyChain(is.getType())) percentModifer = percentModifer + UnitStatic.T2_metal_speed;
			if (UnitStatic.isWearingAnyGold(is.getType())) percentModifer = percentModifer + UnitStatic.T3_metal_speed;
			if (UnitStatic.isWearingAnyDiamond(is.getType())) percentModifer = percentModifer + UnitStatic.T4_metal_speed;
			if (Enchantments.hasEnchantment(is, EnchantmentCustom.Speed)) {
				percentModifer = percentModifer + SpeedEnchantment.getModifitedSpeed(Enchantments.getLevelEnchantment(is, EnchantmentCustom.Speed));
			}
		}
		this.walkingModifier = UnitStatic.normal_speed * (1.0 + percentModifer);
	}

	public void lightningStrike(final boolean repeat, final Town source) {
		Player player;
		try {
			player = CivGlobal.getPlayer(this);
		} catch (CivException e) {
			return;
		}

		int dmg = 7;
		Structure tesla = source.BM.getFirstStructureById("s_teslatower");
		if (tesla != null) {
			dmg = ((TeslaTower) tesla).getDamage();
		}

		final LivingEntity target = (LivingEntity) player;
		if (target.getHealth() - dmg > 0.0) {
			target.setHealth(target.getHealth() - dmg);
			target.damage(0.5);
		} else {
			target.setHealth(0.1);
			target.damage(1.0);
		}
		target.setFireTicks(60);
		if (repeat) Bukkit.getScheduler().runTaskLater(CivCraft.getPlugin(), () -> this.lightningStrike(false, source), 30L);
	}

	public void undoPreview() {
		Player player;
		try {
			player = CivGlobal.getPlayer(this);
		} catch (CivException e) {
			e.printStackTrace();
			return;
		}

		for (BlockCoord coord : this.previewUndo.keySet()) {
			SimpleBlock sb = this.previewUndo.get(coord);
			ItemManager.sendBlockChange(player, coord.getLocation(), sb.getType(), sb.getData());
		}
		this.previewUndo.clear();
	}

	public void showPlayerLoginWarnings(Player player) {
		/* Notify Resident of any invalid structures. */
		if (this.getTown() != null) {
			for (Buildable buildable : this.getTown().BM.getInvalideBuildables()) {
				CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_resident_structInvalidAlert1", buildable.getDisplayName(), buildable.getCorner()) + " "
						+ CivSettings.localize.localizedString("resident_structInvalidAlert2") + " " + buildable.getInvalidLayerMessage());
			}

			/* Show any event messages. */
			if (this.getTown().getActiveEvent() != null) {
				CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_resident_eventNotice1", this.getTown().getActiveEvent().configRandomEvent.name));
			}
		}
	}

	public void sendActionBar(String msg) {
		Bukkit.getPlayerExact(this.getName()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', msg)).create());
	}

	@SuppressWarnings("deprecation")
	public boolean takeItemInHand(int itemId, int itemData, int amount) throws CivException {
		Player player = CivGlobal.getPlayer(this);
		Inventory inv = player.getInventory();

		if (!inv.contains(itemId)) return false;
		if ((player.getInventory().getItemInMainHand().getTypeId() != itemId) && (player.getInventory().getItemInMainHand().getTypeId() != itemData)) return false;

		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack.getAmount() < amount) {
			return false;
		} else
			if (stack.getAmount() == amount)
				inv.removeItem(stack);
			else
				stack.setAmount(stack.getAmount() - amount);

		player.updateInventory();
		return true;
	}

	@SuppressWarnings("deprecation")
	public boolean takeItem(int itemId, int itemData, int amount) throws CivException {
		Player player = CivGlobal.getPlayer(this);
		Inventory inv = player.getInventory();

		if (!inv.contains(itemId)) return false;

		HashMap<Integer, ? extends ItemStack> stacks;
		stacks = inv.all(itemId);

		for (ItemStack stack : stacks.values()) {
			if (stack.getData().getData() != (byte) itemData) continue;
			if (stack.getAmount() <= 0) continue;
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
		if (!this.getTreasury().hasEnough(price)) throw new CivException(CivSettings.localize.localizedString("resident_notEnoughMoney") + " " + CivSettings.CURRENCY_NAME);

		boolean completed = true;
		int bought = 0;
		bought = giveItem(id, data, amount);
		if (bought != amount) {
			this.getTreasury().withdraw(price);
			takeItem(id, data, bought);
			completed = false;
		} else
			this.getTreasury().withdraw(price);

		if (completed)
			return true;
		else
			throw new CivException(CivSettings.localize.localizedString("resident_buyInvenFull"));
	}

}
