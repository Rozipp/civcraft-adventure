/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.village;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.JoinVillageResponse;
import com.avrgaming.civcraft.threading.sync.TeleportPlayerTask;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.village.Village;

public class VillageCommand extends CommandBase {
	public static final long INVITE_TIMEOUT = 30000; //30 seconds
	private Date lastBuildableRefresh = null;

	@Override
	public void init() {
		this.command = "/village";
		this.displayName = CivSettings.localize.localizedString("Village");

		this.cs.add("undo", CivSettings.localize.localizedString("cmd_village_undoDesc"));
		this.cs.add("add", CivSettings.localize.localizedString("cmd_village_addDesc"));
		this.cs.add("remove", CivSettings.localize.localizedString("cmd_village_removeDesc"));
		this.cs.add("leave", CivSettings.localize.localizedString("cmd_village_leaveDesc"));
		this.cs.add("setowner", CivSettings.localize.localizedString("cmd_village_setownerDesc"));
		this.cs.add("info", CivSettings.localize.localizedString("cmd_village_infoDesc"));
		this.cs.add("disband", CivSettings.localize.localizedString("cmd_village_disbandDesc"));
		this.cs.add("upgrade", CivSettings.localize.localizedString("cmd_village_upgradeDesc"));
		this.cs.add("refresh", CivSettings.localize.localizedString("cmd_village_refreshDesc"));
		this.cs.add("location", CivSettings.localize.localizedString("cmd_village_locationDesc"));
		this.cs.add("teleport", CivSettings.localize.localizedString("cmd_village_teleportDesc"));
		this.cs.add("chat", CivSettings.localize.localizedString("cmd_village_chatDesc"));
	}

	public void chat_cmd() {
		Bukkit.dispatchCommand(this.sender, "vcc");
	}

	public void teleport_cmd() throws CivException {
		final Resident resident = this.getResident();
		@SuppressWarnings("unused")
		final Player sender = this.getPlayer();
		CivGlobal.dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
		if (!resident.hasVillage()) {
			throw new CivException("§c" + CivSettings.localize.localizedString("cmd_villageBase_NotInvillage"));
		}
		final Village village = resident.getVillage();
		final long nextTeleport = resident.getNextTeleport();
		final long timeNow = Calendar.getInstance().getTimeInMillis();
		if (nextTeleport > timeNow) {
			throw new CivException("§c" + CivSettings.localize.localizedString("cmd_village_teleport_cooldown", CivGlobal.dateFormat.format(nextTeleport)));
		}
		final Location toTeleport = village.getCenterLocation();
		final TeleportPlayerTask teleportPlayerTask = new TeleportPlayerTask(resident, this.getPlayer(), toTeleport, resident.getVillage());
		teleportPlayerTask.run(true);
	}

	public void location_cmd() throws CivException {
		Resident resident = getResident();

		if (!resident.hasVillage()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_villageBase_NotInvillage"));
		}
		Village village = resident.getVillage();

		if (village != null) {
			CivMessage.send(sender, "");
			CivMessage.send(sender, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("cmd_village_locationSuccess") + " "
					+ CivColor.LightPurple + village.getCorner());
			CivMessage.send(sender, "");
		}
	}

	public void refresh_cmd() throws CivException {
		Resident resident = getResident();

		if (!resident.hasVillage()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_villageBase_NotInvillage"));
		}

		Village village = resident.getVillage();
		if (village.getOwner() != resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_refreshNotOwner"));
		}

		if (village.isDestroyed()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_refreshDestroyed"));
		}

		if (this.lastBuildableRefresh != null) {
			Date now = new Date();
			int buildable_refresh_cooldown;
			try {
				buildable_refresh_cooldown = CivSettings.getInteger(CivSettings.townConfig, "town.buildable_refresh_cooldown");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
			}

			if (now.getTime() < this.lastBuildableRefresh.getTime() + (buildable_refresh_cooldown * 60 * 1000)) {
				throw new CivException(CivSettings.localize.localizedString("var_town_refresh_wait1", buildable_refresh_cooldown));
			}
		}

		try {
			village.repairFromTemplate();
		} catch (IOException e) {} catch (CivException e) {
			e.printStackTrace();
		}
		village.reprocessCommandSigns();
		CivMessage.send(sender, CivSettings.localize.localizedString("cmd_village_refreshSuccess"));
	}

	public void upgrade_cmd() {
		VillageUpgradeCommand cmd = new VillageUpgradeCommand();
		cmd.onCommand(sender, null, "village", this.stripArgs(args, 1));
	}

	public void info_cmd() throws CivException {
		Village village = this.getCurrentVillage();
		SimpleDateFormat sdf = CivGlobal.dateFormat;

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_village_infoHeading", village.getName()));
		HashMap<String, String> info = new HashMap<String, String>();
		info.put(CivSettings.localize.localizedString("Owner"), village.getOwnerName());
		info.put(CivSettings.localize.localizedString("Members"), "" + village.getMembers().size());
		info.put(CivSettings.localize.localizedString("NextRaid"), "" + sdf.format(village.getNextRaidDate()));
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));

		info.clear();
		info.put(CivSettings.localize.localizedString("cmd_village_infoFireLeft"), "" + village.getFirepoints());
		info.put(CivSettings.localize.localizedString("cmd_village_infoLonghouseLevel"),
				"" + village.getLonghouseLevel() + "" + village.getLonghouseCountString());
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));

		info.clear();
		info.put(CivSettings.localize.localizedString("Members"), village.getMembersString());
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));
	}

	public void remove_cmd() throws CivException {
		this.validVillageOwner();
		Village village = getCurrentVillage();
		Resident resident = getNamedResident(1);

		if (!resident.hasVillage() || resident.getVillage() != village) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_village_removeNotInvillage", resident.getName()));
		}

		if (resident.getVillage().getOwner() == resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_removeErrorOwner"));
		}

		village.removeMember(resident);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_village_removeSuccess", resident.getName()));
	}

	public void add_cmd() throws CivException {
		this.validVillageOwner();
		Village village = this.getCurrentVillage();
		Resident resident = getNamedResident(1);
		Player player = getPlayer();

		if (resident.hasVillage()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_addInvillage"));
		}

		if (resident.hasTown()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_addInTown"));
		}

		JoinVillageResponse join = new JoinVillageResponse();
		join.village = village;
		join.resident = resident;
		join.sender = player;

		CivGlobal.questionPlayer(player, CivGlobal.getPlayer(resident),
				CivSettings.localize.localizedString("var_cmd_village_addInvite", player.getName(), village.getName()), INVITE_TIMEOUT, join);

		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_village_addSuccess", resident.getName()));
	}

	public void setowner_cmd() throws CivException {
		this.validVillageOwner();
		Village village = getCurrentVillage();
		Resident newLeader = getNamedResident(1);

		if (!village.hasMember(newLeader.getName())) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_village_removeNotInvillage", newLeader.getName()));
		}

		village.setOwner(newLeader);
		village.save();

		Player player = CivGlobal.getPlayer(newLeader);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_village_setownerMsg", village.getName()));
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_village_setownerSuccess", newLeader.getName()));

	}

	public void leave_cmd() throws CivException {
		Resident resident = getResident();

		if (!resident.hasVillage()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_villageBase_NotInvillage"));
		}

		Village village = resident.getVillage();
		if (village.getOwner() == resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_leaveOwner"));
		}

		village.removeMember(resident);
		village.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_village_leaveSuccess", village.getName()));
	}

	public void new_cmd() throws CivException {

	}

	public void disband_cmd() throws CivException {
		Resident resident = getResident();
		this.validVillageOwner();
		Village village = this.getCurrentVillage();

		if (!resident.hasVillage()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_villageBase_NotInvillage"));
		}

		village.disband();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_village_disbandSuccess"));
	}

	public void undo_cmd() throws CivException {
		Resident resident = getResident();

		if (!resident.hasVillage()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_villageBase_NotInvillage"));
		}

		Village village = resident.getVillage();
		if (village.getOwner() != resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_undoNotOwner"));
		}

		if (!village.isUndoable()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_undoTooLate"));
		}

		CraftableCustomMaterial villageMat = CraftableCustomMaterial.getCraftableCustomMaterial("mat_found_village");
		if (villageMat == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_village_undoError"));
		}

		ItemStack newStack = CraftableCustomMaterial.spawn(villageMat);
		Player player = CivGlobal.getPlayer(resident);
		HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(newStack);
		for (ItemStack stack : leftovers.values()) {
			player.getWorld().dropItem(player.getLocation(), stack);
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("cmd_village_undoFullInven"));
		}

		village.undo();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_village_undoSuccess"));

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
	}

}
