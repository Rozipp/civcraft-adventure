/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command.admin;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.ReportChestsTask;
import com.avrgaming.civcraft.command.town.TownInfoCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.ConfigRandomEvent;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class AdminTownCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad town";
		displayName = CivSettings.localize.localizedString("adcmd_town_name");

		cs.add("disband", CivSettings.localize.localizedString("adcmd_town_disbandDesc"));
		cs.add("claim", CivSettings.localize.localizedString("adcmd_town_claimDesc"));
		cs.add("unclaim", CivSettings.localize.localizedString("adcmd_town_unclaimDesc"));
		cs.add("hammerrate", CivSettings.localize.localizedString("adcmd_town_hammerrateDesc"));
		cs.add("addmayor", CivSettings.localize.localizedString("adcmd_town_addmayorDesc"));
		cs.add("addassistant", CivSettings.localize.localizedString("adcmd_town_addAssistantDesc"));
		cs.add("rmmayor", CivSettings.localize.localizedString("adcmd_town_rmmayorDesc"));
		cs.add("rmassistant", CivSettings.localize.localizedString("adcmd_town_rmassistantDesc"));
		cs.add("tp", CivSettings.localize.localizedString("adcmd_town_tpDesc"));
		cs.add("culture", CivSettings.localize.localizedString("adcmd_town_cultureDesc"));
		cs.add("info", CivSettings.localize.localizedString("adcmd_town_infoDesc"));
		cs.add("setciv", CivSettings.localize.localizedString("adcmd_town_setcivDesc"));
		cs.add("select", CivSettings.localize.localizedString("adcmd_town_selectDesc"));
		cs.add("claimradius", CivSettings.localize.localizedString("adcmd_town_claimradiusDesc"));
		cs.add("chestreport", CivSettings.localize.localizedString("adcmd_town_chestReportDesc"));
		cs.add("rebuildgroups", CivSettings.localize.localizedString("adcmd_town_rebuildgroupsDesc"));
		cs.add("capture", CivSettings.localize.localizedString("adcmd_town_captureDesc"));
		cs.add("setmotherciv", CivSettings.localize.localizedString("adcmd_town_setmothercivDesc"));
		cs.add("sethappy", CivSettings.localize.localizedString("adcmd_town_sethappyDesc"));
		cs.add("setunhappy", CivSettings.localize.localizedString("adcmd_town_setunhappyDesc"));
		cs.add("event", CivSettings.localize.localizedString("adcmd_town_eventDesc"));
		cs.add("rename", CivSettings.localize.localizedString("adcmd_town_renameDesc"));
		cs.add("eventcancel", CivSettings.localize.localizedString("adcmd_town_eventcancelDesc"));
		cs.add("tradegoods", CivSettings.localize.localizedString("adcmd_town_tradegoodsDesc"));
	}

	public void tradegoods_cmd() throws CivException {
		String result = "";
		for (Town town : CivGlobal.getTowns()) {
			String tradegoods = town.tradeGoods;
			if (town == null || tradegoods.isEmpty()) continue;
			result = result + "§2" + town.getName() + ": ";
			for (String good : tradegoods.split(", ")) {
				ConfigTradeGood configTradeGood = CivSettings.goods.get(good);
				if (configTradeGood == null) continue;
				result = result + "§6" + configTradeGood.name + ", ";
			}
		}
		if (result.equalsIgnoreCase("")) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_town_tradegoods_noTradeGoods"));
		}
		CivMessage.send((Object) this.sender, "§a" + CivSettings.localize.localizedString("adcmd_town_tradegoods_result", result));
	}

	public void eventcancel_cmd() throws CivException {
		Town town = this.getNamedTown(1);
		if (town.getActiveEvent() == null) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_town_eventcancel_noEvent", "§6" + town.getName() + CivColor.Red));
		}
		try {
			CivMessage.sendSuccess(this.sender, CivSettings.localize.localizedString("adcmd_town_eventcancel_succusess", "§b" + town.getName() + CivColor.Red, "§6" + town.getActiveEvent().configRandomEvent.name));
			CivMessage.sendTown(town, CivSettings.localize.localizedString("adcmd_town_eventcancel_succusessTown", "§a" + town.getActiveEvent().configRandomEvent.name + CivColor.RESET, ((Player) this.sender).getDisplayName()));
			town.getActiveEvent().delete();
			town.setActiveEvent(null);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void rename_cmd() throws CivException, InvalidNameException {
		Town town = getNamedTown(1);
		String name = getNamedString(2, CivSettings.localize.localizedString("EnterTownName"));

		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_town_renameUnderscores"));
		}

		town.rename(name);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_renameSuccess"));
	}

	public void event_cmd() throws CivException {
		Town town = getNamedTown(1);

		if (args.length < 3) {
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_town_eventHeading"));
			String out = "";
			for (ConfigRandomEvent configEvent : CivSettings.randomEvents.values()) {
				out += configEvent.id + ",";
			}
			CivMessage.send(sender, out);
			return;
		}

		ConfigRandomEvent event = CivSettings.randomEvents.get(args[2]);
		RandomEvent randEvent = new RandomEvent(event);
		randEvent.start(town);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_eventRenameSuccess") + " " + event.name);
	}

	public void setunhappy_cmd() throws CivException {
		Town town = getNamedTown(1);
		double happy = getNamedDouble(2);

		town.setBaseUnhappy(happy);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_setunhappySuccess", happy));

	}

	public void sethappy_cmd() throws CivException {
		Town town = getNamedTown(1);
		double happy = getNamedDouble(2);

		town.setBaseHappiness(happy);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_sethappySuccess", happy));

	}

	public void setmotherciv_cmd() throws CivException {
		Town town = getNamedTown(1);
		Civilization civ = getNamedCiv(2);

		town.setMotherCiv(civ);
		town.save();

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_setMotherCivSuccess", town.getName(), civ.getName()));
	}

	public void capture_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Town town = getNamedTown(2);

		town.onDefeat(civ);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_captureSuccess", town.getName(), civ.getName()));
	}

	public void rebuildgroups_cmd() throws CivException {
		Town town = getNamedTown(1);

		if (town.getDefaultGroup() == null) {
			PermissionGroup residents;
			try {
				residents = new PermissionGroup(town, "residents");
				town.setDefaultGroup(residents);
				try {
					residents.saveNow();
					town.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (InvalidNameException e1) {
				e1.printStackTrace();
			}
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_rebuildgroupsResidents"));
		}

		if (town.getAssistantGroup() == null) {
			PermissionGroup assistant;
			try {
				assistant = new PermissionGroup(town, "assistants");

				town.setAssistantGroup(assistant);
				try {
					assistant.saveNow();
					town.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (InvalidNameException e) {
				e.printStackTrace();
			}
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_rebuildgroupsAssistants"));
		}

		if (town.getMayorGroup() == null) {
			PermissionGroup mayor;
			try {
				mayor = new PermissionGroup(town, "mayors");
				town.setMayorGroup(mayor);
				try {
					mayor.saveNow();
					town.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (InvalidNameException e) {
				e.printStackTrace();
			}
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_rebuildgroupsMayors"));
		}

	}

	public void chestreport_cmd() throws CivException {
		Town town = getNamedTown(1);

		Queue<ChunkCoord> coords = new LinkedList<ChunkCoord>();
		for (TownChunk tc : town.getTownChunks()) {
			ChunkCoord coord = tc.getChunkCoord();
			coords.add(coord);
		}

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_town_chestReportStart") + " " + town.getName());
		CivMessage.send(sender, CivSettings.localize.localizedString("adcmd_ReportStarted"));
		TaskMaster.syncTask(new ReportChestsTask(sender, coords), 0);

	}

	public static int claimradius(Town town, Location loc, Integer radius) {
		ChunkCoord coord = new ChunkCoord(loc);

		int count = 0;
		for (int x = -radius; x < radius; x++) {
			for (int z = -radius; z < radius; z++) {
				try {
					ChunkCoord next = new ChunkCoord(coord.getWorldname(), coord.getX() + x, coord.getZ() + z);
					TownChunk.autoClaim(town, next);
					count++;
				} catch (CivException e) {
					// ignore errors...
				}
			}
		}

		town.save();
		return count;
	}

	public void claimradius_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer radius = getNamedInteger(1);

		int count = claimradius(town, getPlayer().getLocation(), radius);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_claimradiusSuccess", count));
	}

	public void select_cmd() throws CivException {
		Resident resident = getResident();
		Town selectTown = getNamedTown(1);

		if (resident.getSelectedTown() == null) {
			if (resident.getTown() == selectTown) {
				throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_selectAlreadySelected", selectTown.getName()));
			}
		}

		if (resident.getSelectedTown() == selectTown) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_selectAlreadySelected", selectTown.getName()));
		}

		resident.setSelectedTown(selectTown);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_selectSuccess", selectTown.getName()));
	}

	public void setciv_cmd() throws CivException {
		Town town = getNamedTown(1);
		Civilization civ = getNamedCiv(2);

		if (town.getCiv() == civ) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_setcivErrorInCiv", civ.getName()));
		}

		if (town.isCapitol()) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_town_setcivErrorCapitol"));
		}

		town.changeCiv(civ);
		CivGlobal.processCulture();
		CivMessage.global(CivSettings.localize.localizedString("var_adcmd_town_setcivSuccess1", town.getName(), civ.getName()));

	}

	public void info_cmd() throws CivException {
		Town town = getNamedTown(1);

		TownInfoCommand cmd = new TownInfoCommand();
		cmd.senderTownOverride = town;
		cmd.senderCivOverride = town.getCiv();
		cmd.onCommand(sender, null, "info", this.stripArgs(args, 2));
	}

	public void culture_cmd() throws CivException {
		Town town = getNamedTown(1);
		Integer culture = getNamedInteger(2);

		town.addAccumulatedCulture(culture);
		town.save();

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_var_adcmd_town_cultureSuccess", town.getName(), culture));
	}

	public void tp_cmd() throws CivException {
		Town town = getNamedTown(1);

		Townhall townhall = town.getTownHall();

		if (sender instanceof Player) {
			if (townhall != null && townhall.isComplete()) {
				BlockCoord bcoord = townhall.getRandomRevivePoint();
				((Player) sender).teleport(bcoord.getLocation());
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_tpSuccess", town.getName()));
				return;
			} else {
				if (town.getTownChunks().size() > 0) {
					ChunkCoord coord = town.getTownChunks().iterator().next().getChunkCoord();

					Location loc = new Location(Bukkit.getWorld(coord.getWorldname()), (coord.getX() << 4), 100, (coord.getZ() << 4));
					((Player) sender).teleport(loc);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_tpSuccess", town.getName()));
					return;
				}
			}

			throw new CivException(CivSettings.localize.localizedString("adcmd_town_tpError"));
		}

	}

	public void rmassistant_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);

		if (!town.getAssistantGroup().hasMember(resident)) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_rmassistantNotInTown", resident.getName(), town.getName()));
		}

		town.getAssistantGroup().removeMember(resident);
		try {
			town.getAssistantGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_rmassistantSuccess", resident.getName(), town.getName()));

	}

	public void rmmayor_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);

		if (!town.getMayorGroup().hasMember(resident)) {
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_town_rmmayorNotInTown", resident.getName(), town.getName()));

		}

		town.getMayorGroup().removeMember(resident);
		try {
			town.getMayorGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_rmmayorSuccess", resident.getName(), town.getName()));

	}

	public void addassistant_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);

		town.getAssistantGroup().addMember(resident);
		try {
			town.getAssistantGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_addAssistantSuccess", resident.getName(), town.getName()));

	}

	public void addmayor_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);

		town.getMayorGroup().addMember(resident);
		try {
			town.getMayorGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_addmayorSuccess", resident.getName(), town.getName()));

	}

	public void disband_cmd() throws CivException {
		Town town = getNamedTown(1);

		if (town.isCapitol()) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_town_disbandError"));
		}

		CivMessage.sendTown(town, CivSettings.localize.localizedString("adcmd_town_disbandBroadcast"));
		town.delete();

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_town_disbandSuccess"));
	}

	public void hammerrate_cmd() throws CivException {
		if (args.length < 3) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_town_hammerratePrompt"));
		}

		Town town = getNamedTown(1);

		try {
			town.setHammerRate(Double.valueOf(args[2]));
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_town_hammerrateSuccess", args[1], args[2]));
		} catch (NumberFormatException e) {
			throw new CivException(args[2] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
		}

		town.save();
	}

	public void unclaim_cmd() throws CivException {
		Town town = getNamedTown(1);
		Player player = getPlayer();

		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc != null) {

			tc.getTown().removeTownChunk(tc);
			CivGlobal.removeTownChunk(tc);
			try {
				tc.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			town.save();

			CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_adcmd_town_unclaimSuccess", town.getName()));
		} else {
			CivMessage.sendError(sender, CivSettings.localize.localizedString("adcmd_town_unclaimErrorNotOwned"));
		}

	}

	public void claim_cmd() throws CivException {
		Town town = getNamedTown(1);
		Player player = getPlayer();

		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc == null) {
			tc = new TownChunk(town, player.getLocation());
			CivGlobal.addTownChunk(tc);
			try {
				town.addTownChunk(tc);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			}

			tc.save();
			town.save();

			CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_adcmd_town_claimSucess", town.getName()));
		} else {
			CivMessage.sendError(sender, CivSettings.localize.localizedString("var_adcmd_town_claimErrorOwned", town.getName()));
		}

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
		// Admin permission checked in parent.
	}

}
