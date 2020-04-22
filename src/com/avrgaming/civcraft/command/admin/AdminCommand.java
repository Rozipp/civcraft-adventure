/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.admin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.ReportChestsTask;
import com.avrgaming.civcraft.command.ReportPlayerInventoryTask;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.event.GoodieRepoEvent;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.listener.WorldListener;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.ConfigUnit;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.Schematic;
import com.avrgaming.sls.SLSManager;

public class AdminCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad";
		displayName = CivSettings.localize.localizedString("adcmd_Name");

		cs.add("perm", CivSettings.localize.localizedString("adcmd_permDesc"));
		cs.add("sbperm", CivSettings.localize.localizedString("adcmd_adpermDesc"));
		cs.add("cbinstantbreak", CivSettings.localize.localizedString("adcmd_cbinstantbreakDesc"));
		cs.add("mob", CivSettings.localize.localizedString("cmd_mob_managament"));
		cs.add("unit", "Работа с юнитами");
		cs.add("recover", CivSettings.localize.localizedString("adcmd_recoverDesc"));
		cs.add("server", CivSettings.localize.localizedString("adcmd_serverDesc"));
		cs.add("spawnunit", CivSettings.localize.localizedString("adcmd_spawnUnitDesc"));

		cs.add("chestreport", CivSettings.localize.localizedString("adcmd_chestReportDesc"));
		cs.add("playerreport", CivSettings.localize.localizedString("adcmd_playerreportDesc"));

		cs.add("civ", CivSettings.localize.localizedString("adcmd_civDesc"));
		cs.add("town", CivSettings.localize.localizedString("adcmd_townDesc"));
		cs.add("war", CivSettings.localize.localizedString("adcmd_warDesc"));
		cs.add("lag", CivSettings.localize.localizedString("adcmd_lagdesc"));
		cs.add("camp", CivSettings.localize.localizedString("adcmd_campDesc"));
		cs.add("chat", CivSettings.localize.localizedString("adcmd_chatDesc"));
		cs.add("res", CivSettings.localize.localizedString("adcmd_resDesc"));
		cs.add("build", CivSettings.localize.localizedString("adcmd_buildDesc"));
		cs.add("items", CivSettings.localize.localizedString("adcmd_itemsDesc"));
		cs.add("item", CivSettings.localize.localizedString("adcmd_itemDesc"));
		cs.add("timer", CivSettings.localize.localizedString("adcmd_timerDesc"));
		cs.add("road", CivSettings.localize.localizedString("adcmd_roadDesc"));
		cs.add("clearendgame", CivSettings.localize.localizedString("adcmd_clearEndGameDesc"));
		cs.add("endworld", CivSettings.localize.localizedString("adcmd_endworldDesc"));
		cs.add("perk", CivSettings.localize.localizedString("adcmd_perkDesc"));
		cs.add("reloadgov", CivSettings.localize.localizedString("adcmd_reloadgovDesc"));
		cs.add("heartbeat", CivSettings.localize.localizedString("adcmd_heartbeatDesc"));
		cs.add("goodierepo", CivSettings.localize.localizedString("cmd_servak"));
		cs.add("clearchat", CivSettings.localize.localizedString("clearchat"));
		cs.add("newspaper", CivSettings.localize.localizedString("adcmd_newspaper"));
		cs.add("startMission", CivSettings.localize.localizedString("adcmd_startMission"));
		cs.add("replenish", CivSettings.localize.localizedString("adcmd_replenish"));
		cs.add("count", CivSettings.localize.localizedString("adcmd_count"));
		cs.add("globalwar", CivSettings.localize.localizedString("adcmd_globalWar"));
		cs.add("gc", CivSettings.localize.localizedString("cmd_gc"));
		cs.add("pasteruin", CivSettings.localize.localizedString("cmd_pasteruin"));
		cs.add("report", CivSettings.localize.localizedString("adcmd_report"));
	}

	public void report_cmd() {
		final AdminReportCommand cmd = new AdminReportCommand();
		cmd.onCommand(this.sender, null, "report", this.stripArgs(this.args, 1));
	}

	public void pasteruin_cmd() throws CivException {
		final Schematic schem = WorldListener.schematics.get(CivCraft.civRandom.nextInt(WorldListener.schematics.size() - 1));
		schem.paste(((Player) this.sender).getLocation());
		CivMessage.sendSuccess(this.sender, CivSettings.localize.localizedString("cmd_pasteruin_success"));
	}

	public void gc_cmd() {
		final long start = System.nanoTime();
		System.gc();
		CivMessage.sendSuccess(this.sender, CivColor.LightGreenBold + CivSettings.localize.localizedString("cmd_gc_result",
				CivColor.GoldBold + this.formatFloat((System.nanoTime() - start) / 1000000L, 2) + CivColor.LightGreenBold));
	}

	public String formatFloat(float num, final int pr) {
		final StringBuilder sb = new StringBuilder();
		sb.append((int) num);
		sb.append('.');
		for (int i = 0; i < pr; ++i) {
			num *= 10.0f;
			sb.append((int) num % 10);
		}
		return sb.toString();
	}

	public void count_cmd() {
		CivMessage.send(this.sender, CivColor.RoseBold + "Total Residents: " + CivGlobal.getResidents().size());
		CivMessage.send(this.sender, CivColor.RoseBold + "Total Camps: " + CivGlobal.getCamps().size());
		CivMessage.send(this.sender, CivColor.RoseBold + "Total Towns: " + CivGlobal.getTowns().size());
		CivMessage.send(this.sender, CivColor.RoseBold + "Total Civs: " + CivGlobal.getCivs().size());
	}

	public void globalwar_cmd() {
		for (final Civilization civ : CivGlobal.getCivs()) {
			for (final Civilization civ2 : CivGlobal.getCivs()) {
				if (!civ.getDiplomacyManager().atWarWith(civ2)) {
					CivGlobal.setRelation(civ, civ2, Relation.Status.WAR);
					CivGlobal.setAggressor(civ, civ2, civ);
					CivGlobal.setAggressor(civ2, civ, civ);
				}
			}
		}
		Bukkit.dispatchCommand(this.sender, "ad war start");
	}

	public void replenish_cmd() {
		for (final Civilization civ : CivGlobal.getCivs()) {
			if (civ.tradeGoods.isEmpty()) {
				civ.tradeGoods = "";
				try {
					civ.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			for (final Town town : civ.getTowns()) {
				if (!civ.tradeGoods.isEmpty()) {
					town.tradeGoods = "";
					try {
						town.saveNow();
					} catch (SQLException e2) {
						e2.printStackTrace();
					}
					final ArrayList<String> keysToRemove = new ArrayList<String>();
					for (final Buff buff : town.getBuffManager().getAllBuffs()) {
						if (buff.getKey().contains("tradegood")) {
							keysToRemove.add(buff.getKey());
						}
					}
					for (final String key : keysToRemove) {
						town.getBuffManager().removeBuff(key);
					}
				}
			}
		}
		for (final BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
			try {
				goodie.replenish();
			} catch (Exception e3) {
				e3.printStackTrace();
			}
		}
		CivMessage.global(CivSettings.localize.localizedString("goodieRepoBroadcast"));
		CivMessage.globalTitle(CivSettings.localize.localizedString("goodieRepoBroadcast"), "");
	}

	public void newspaper_cmd() throws IOException, InvalidConfigurationException {
		CivSettings.reloadNewspaperConfigFiles();
		CivMessage.send(this.sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_newspaper_done"));
		CivMessage.global(CivSettings.localize.localizedString("adcmd_newspaper_broadcast"));
	}

	public void clearchat_cmd() throws CivException {
		for (int i = 0; i < 200; ++i) {
			Bukkit.broadcastMessage("");
		}
		final Player player = this.getPlayer();
		CivMessage.global(CivSettings.localize.localizedString("chatcleared", player.getName()));
	}

	public void goodierepo_cmd() {
		CivLog.info("TimerEvent: GoodieRepo -------------------------------------");
		GoodieRepoEvent.repoProcess();
		CivMessage.globalTitle(CivSettings.localize.localizedString("goodieRepoBroadcast"), "");
	}

	public void reloadgov_cmd() throws FileNotFoundException, IOException, InvalidConfigurationException, InvalidConfiguration {
		CivSettings.reloadGovConfigFiles();
		for (Civilization civ : CivGlobal.getCivs()) {
			ConfigGovernment gov = civ.getGovernment();

			civ.setGovernment(gov.id);
		}
		CivMessage.send(sender, CivColor.Gold + CivSettings.localize.localizedString("adcmd_reloadgovSuccess"));
	}

	public void heartbeat_cmd() {
		SLSManager.sendHeartbeat();
	}

	public void unit_cmd() {
		AdminUnitCommand cmd = new AdminUnitCommand();
		cmd.onCommand(sender, null, "unit", this.stripArgs(args, 1));
	}
	
	public void mob_cmd() {
		AdminMobCommand cmd = new AdminMobCommand();
		cmd.onCommand(sender, null, "mob", this.stripArgs(args, 1));
	}

	public void perk_cmd() {
		AdminPerkCommand cmd = new AdminPerkCommand();
		cmd.onCommand(sender, null, "perk", this.stripArgs(args, 1));
	}

	public void endworld_cmd() {
		CivGlobal.endWorld = !CivGlobal.endWorld;
		if (CivGlobal.endWorld) {
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOn"));
		} else {
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_endworldOff"));
		}
	}

	public void clearendgame_cmd() throws CivException {
		String key = getNamedString(1, "enter key.");
		Civilization civ = getNamedCiv(2);

		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
		if (entries.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_clearEndGameNoKey"));
		}

		for (SessionEntry entry : entries) {
			if (EndGameCondition.getCivFromSessionData(entry.value) == civ) {
				CivGlobal.getSessionDatabase().delete(entry.request_id, entry.key);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_clearEndGameSuccess", civ.getName()));
			}
		}
	}

	public void cbinstantbreak_cmd() throws CivException {
		Resident resident = getResident();

		resident.setControlBlockInstantBreak(!resident.isControlBlockInstantBreak());
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_cbinstantbreakSuccess") + resident.isControlBlockInstantBreak());
	}

	public static Inventory spawnInventory = null;
	public void items_cmd() throws CivException {
		Player player = getPlayer();

		if (spawnInventory == null) {
			spawnInventory = Bukkit.createInventory(player, LoreGuiItem.MAX_INV_SIZE, CivSettings.localize.localizedString("adcmd_itemsHeader"));

			/* Build the Category Inventory. */
			for (ConfigMaterialCategory cat : ConfigMaterialCategory.getCategories()) {
				int identifier;
				if (cat.name.contains("Fish")) {
					identifier = ItemManager.getMaterialId(Material.RAW_FISH);
				} else
					if (cat.name.contains("Catalyst")) {
						identifier = ItemManager.getMaterialId(Material.BOOK);
					} else
						if (cat.name.contains("Gear")) {
							identifier = ItemManager.getMaterialId(Material.IRON_SWORD);
						} else
							if (cat.name.contains("Materials")) {
								identifier = ItemManager.getMaterialId(Material.WOOD_STEP);
							} else
								if (cat.name.contains("Tools")) {
									identifier = ItemManager.getMaterialId(Material.IRON_SPADE);
								} else
									if (cat.name.contains("Eggs")) {
										identifier = ItemManager.getMaterialId(Material.MONSTER_EGG);
									} else {
										identifier = ItemManager.getMaterialId(Material.WRITTEN_BOOK);
									}
				ItemStack infoRec = LoreGuiItem.build(cat.name, identifier, 0, CivColor.LightBlue + cat.materials.size() + " Items",
						CivColor.Gold + "<Click To Open>");
				infoRec = LoreGuiItem.setAction(infoRec, "OpenInventory");
				infoRec = LoreGuiItem.setActionData(infoRec, "invType", "showGuiInv");
				infoRec = LoreGuiItem.setActionData(infoRec, "invName", cat.name + " Spawn");
				spawnInventory.addItem(infoRec);

				/* Build a new GUI Inventory. */
				Inventory inv = Bukkit.createInventory(player, LoreGuiItem.MAX_INV_SIZE, cat.name + " Spawn");
				for (ConfigMaterial mat : cat.materials.values()) {
					CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(mat.id);
					if (craftMat == null) continue;
					ItemStack stack = CustomMaterial.spawn(craftMat);
					stack = LoreGuiItem.asGuiItem(stack);
					stack = LoreGuiItem.setAction(stack, "SpawnItem");
					inv.addItem(stack);
					LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
				}
			}

		}

		player.openInventory(spawnInventory);
	}

	public void road_cmd() {
		AdminRoadCommand cmd = new AdminRoadCommand();
		cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
	}

	public void item_cmd() {
		AdminItemCommand cmd = new AdminItemCommand();
		cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
	}

	public void timer_cmd() {
		AdminTimerCommand cmd = new AdminTimerCommand();
		cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
	}

	public void camp_cmd() {
		AdminCampCommand cmd = new AdminCampCommand();
		cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
	}

	public void playerreport_cmd() {

		LinkedList<OfflinePlayer> offplayers = new LinkedList<OfflinePlayer>();
		for (OfflinePlayer offplayer : Bukkit.getOfflinePlayers()) {
			offplayers.add(offplayer);
		}

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_playerreportHeader"));
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
		TaskMaster.syncTask(new ReportPlayerInventoryTask(sender, offplayers), 0);
	}

	public void chestreport_cmd() throws CivException {
		Integer radius = getNamedInteger(1);
		Player player = getPlayer();

		LinkedList<ChunkCoord> coords = new LinkedList<ChunkCoord>();
		for (int x = -radius; x < radius; x++) {
			for (int z = -radius; z < radius; z++) {
				ChunkCoord coord = new ChunkCoord(player.getLocation());
				coord.setX(coord.getX() + x);
				coord.setZ(coord.getZ() + z);

				coords.add(coord);
			}
		}

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_chestReportHeader"));
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
		TaskMaster.syncTask(new ReportChestsTask(sender, coords), 0);
	}

	public void spawnunit_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_spawnUnitPrompt"));
		}

		ConfigUnit unit = UnitStatic.configUnits.get(args[1]);
		if (unit == null) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_spawnUnitInvalid", args[1]));
		}

		Player player = getPlayer();
		Town town = getNamedTown(2);

//		if (args.length > 2) {
//			try {
//				player = CivGlobal.getPlayer(args[2]);
//			} catch (CivException e) {
//				throw new CivException("Player "+args[2]+" is not online.");
//			}
//		} else {
//			player = getPlayer();
//		}

		Class<?> c;
		try {
			c = Class.forName(unit.class_name);
			Method m = c.getMethod("spawn", Inventory.class, Town.class);
			m.invoke(null, player.getInventory(), town);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new CivException(e.getMessage());
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_spawnUnitSuccess", unit.name));
	}

	public void server_cmd() {
		CivMessage.send(sender, Bukkit.getServerName());
	}

	public void recover_cmd() {
		AdminRecoverCommand cmd = new AdminRecoverCommand();
		cmd.onCommand(sender, null, "recover", this.stripArgs(args, 1));
	}

	public void town_cmd() {
		AdminTownCommand cmd = new AdminTownCommand();
		cmd.onCommand(sender, null, "town", this.stripArgs(args, 1));
	}

	public void civ_cmd() {
		AdminCivCommand cmd = new AdminCivCommand();
		cmd.onCommand(sender, null, "civ", this.stripArgs(args, 1));
	}

	public void setfullmessage_cmd() {
		if (args.length < 2) {
			CivMessage.send(sender, CivSettings.localize.localizedString("Current") + CivGlobal.fullMessage);
			return;
		}

		synchronized (CivGlobal.maxPlayers) {
			CivGlobal.fullMessage = args[1];
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("SetTo") + args[1]);

	}

	public void res_cmd() {
		AdminResCommand cmd = new AdminResCommand();
		cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
	}

	public void chat_cmd() {
		AdminChatCommand cmd = new AdminChatCommand();
		cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
	}

	public void war_cmd() {
		AdminWarCommand cmd = new AdminWarCommand();
		cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
	}

	public void lag_cmd() {
		AdminLagCommand cmd = new AdminLagCommand();
		cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
	}

	public void build_cmd() {
		AdminBuildCommand cmd = new AdminBuildCommand();
		cmd.onCommand(sender, null, "war", this.stripArgs(args, 1));
	}

	public void perm_cmd() throws CivException {
		Resident resident = getResident();

		if (resident.isPermOverride()) {
			resident.setPermOverride(false);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_permOff"));
			return;
		}

		resident.setPermOverride(true);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_permOn"));

	}

	public void sbperm_cmd() throws CivException {
		Resident resident = getResident();
		if (resident.isSBPermOverride()) {
			resident.setSBPermOverride(false);
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_sbpermOff"));
			return;
		}

		resident.setSBPermOverride(true);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_sbpermOn"));
	}

	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {

		if (sender instanceof Player) {
			if (((Player) sender).hasPermission(CivSettings.MINI_ADMIN)) {
				return;
			}
		}

		if (sender.isOp() == false) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_NotAdmin"));
		}
	}

	@Override
	public void doLogging() {
		CivLog.adminlog(sender.getName(), "/ad " + this.combineArgs(args));
	}

}
