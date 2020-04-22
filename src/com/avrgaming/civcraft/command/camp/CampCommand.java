/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.camp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.JoinCampResponse;
import com.avrgaming.civcraft.questions.Question;
import com.avrgaming.civcraft.threading.sync.TeleportPlayerTask;
import com.avrgaming.civcraft.util.CivColor;

public class CampCommand extends CommandBase {
	public static final long INVITE_TIMEOUT = 30000; //30 seconds
	private Date lastBuildableRefresh = null;

	@Override
	public void init() {
		this.command = "/camp";
		this.displayName = CivSettings.localize.localizedString("Camp");

		this.cs.add("undo", CivSettings.localize.localizedString("cmd_camp_undoDesc"));
		this.cs.add("add", CivSettings.localize.localizedString("cmd_camp_addDesc"));
		this.cs.add("remove", CivSettings.localize.localizedString("cmd_camp_removeDesc"));
		this.cs.add("leave", CivSettings.localize.localizedString("cmd_camp_leaveDesc"));
		this.cs.add("setowner", CivSettings.localize.localizedString("cmd_camp_setownerDesc"));
		this.cs.add("info", CivSettings.localize.localizedString("cmd_camp_infoDesc"));
		this.cs.add("disband", CivSettings.localize.localizedString("cmd_camp_disbandDesc"));
		this.cs.add("upgrade", CivSettings.localize.localizedString("cmd_camp_upgradeDesc"));
		this.cs.add("refresh", CivSettings.localize.localizedString("cmd_camp_refreshDesc"));
		this.cs.add("location", CivSettings.localize.localizedString("cmd_camp_locationDesc"));
		this.cs.add("teleport", CivSettings.localize.localizedString("cmd_camp_teleportDesc"));
		this.cs.add("chat", CivSettings.localize.localizedString("cmd_camp_chatDesc"));
	}

	public void chat_cmd() {
		Bukkit.dispatchCommand(this.sender, "vcc");
	}

	public void teleport_cmd() throws CivException {
		final Resident resident = this.getResident();
		@SuppressWarnings("unused")
		final Player sender = this.getPlayer();
		CivGlobal.dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
		if (!resident.hasCamp()) {
			throw new CivException("§c" + CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}
		final Camp camp = resident.getCamp();
		final long nextTeleport = resident.getNextTeleport();
		final long timeNow = Calendar.getInstance().getTimeInMillis();
		if (nextTeleport > timeNow) {
			throw new CivException("§c" + CivSettings.localize.localizedString("cmd_camp_teleport_cooldown", CivGlobal.dateFormat.format(nextTeleport)));
		}
		final Location toTeleport = camp.getCenterLocation();
		final TeleportPlayerTask teleportPlayerTask = new TeleportPlayerTask(resident, this.getPlayer(), toTeleport, resident.getCamp());
		teleportPlayerTask.run(true);
	}

	public void location_cmd() throws CivException {
		Resident resident = getResident();

		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}
		Camp camp = resident.getCamp();

		if (camp != null) {
			CivMessage.send(sender, "");
			CivMessage.send(sender, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("cmd_camp_locationSuccess") + " "
					+ CivColor.LightPurple + camp.getCorner());
			CivMessage.send(sender, "");
		}
	}

	public void refresh_cmd() throws CivException {
		Resident resident = getResident();
		Date now = new Date();
		int buildable_refresh_cooldown;
		try {
			buildable_refresh_cooldown = CivSettings.getInteger(CivSettings.townConfig, "town.buildable_refresh_cooldown");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
		}

		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}

		Camp camp = resident.getCamp();
		if (camp.getSQLOwner() != resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_refreshNotOwner"));
		}

		if (camp.isDestroyed()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_refreshDestroyed"));
		}

		if (this.lastBuildableRefresh != null) {
			if (now.getTime() < this.lastBuildableRefresh.getTime() + (buildable_refresh_cooldown * 60 * 1000)) {
				throw new CivException(CivSettings.localize.localizedString("var_town_refresh_wait1", buildable_refresh_cooldown));
			}
		}

		try {
			camp.repairFromTemplate();
		} catch (IOException e) {} catch (CivException e) {
			e.printStackTrace();
		}
		camp.processCommandSigns();
		CivMessage.send(sender, CivSettings.localize.localizedString("cmd_camp_refreshSuccess"));
		resident.setNextRefresh(now.getTime() + (buildable_refresh_cooldown * 60 * 1000));
	}

	public void upgrade_cmd() {
		CampUpgradeCommand cmd = new CampUpgradeCommand();
		cmd.onCommand(sender, null, "camp", this.stripArgs(args, 1));
	}

	public void info_cmd() throws CivException {
		Camp camp = this.getCurrentCamp();
		SimpleDateFormat sdf = CivGlobal.dateFormat;

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_camp_infoHeading", camp.getName()));
		HashMap<String, String> info = new HashMap<String, String>();
		info.put(CivSettings.localize.localizedString("Owner"), camp.getOwnerName());
		info.put(CivSettings.localize.localizedString("Members"), "" + camp.getMembers().size());
		info.put(CivSettings.localize.localizedString("NextRaid"), "" + sdf.format(camp.getNextRaidDate()));
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));

		info.clear();
		info.put(CivSettings.localize.localizedString("cmd_camp_infoFireLeft"), "" + camp.getFirepoints());
		info.put(CivSettings.localize.localizedString("cmd_camp_infoLonghouseLevel"),
				"" + camp.getLonghouseLevel() + "" + camp.getLonghouseCountString());
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));

		info.clear();
		info.put(CivSettings.localize.localizedString("Members"), camp.getMembersString());
		CivMessage.send(sender, this.makeInfoString(info, CivColor.Green, CivColor.LightGreen));
	}

	public void remove_cmd() throws CivException {
		this.validCampOwner();
		Camp camp = getCurrentCamp();
		Resident resident = getNamedResident(1);

		if (!resident.hasCamp() || resident.getCamp() != camp) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_camp_removeNotIncamp", resident.getName()));
		}

		if (resident.getCamp().getSQLOwner() == resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_removeErrorOwner"));
		}

		camp.removeMember(resident);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_removeSuccess", resident.getName()));
	}

	public void add_cmd() throws CivException {
		this.validCampOwner();
		Camp camp = this.getCurrentCamp();
		Resident resident = getNamedResident(1);
		Player player = getPlayer();

		if (resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_addIncamp"));
		}

		if (resident.hasTown()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_addInTown"));
		}

		JoinCampResponse join = new JoinCampResponse();
		join.camp = camp;
		join.resident = resident;
		join.sender = player;

		Question.questionPlayer(player, CivGlobal.getPlayer(resident),
				CivSettings.localize.localizedString("var_cmd_camp_addInvite", player.getName(), camp.getName()), INVITE_TIMEOUT, join);

		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_camp_addSuccess", resident.getName()));
	}

	public void setowner_cmd() throws CivException {
		this.validCampOwner();
		Camp camp = getCurrentCamp();
		Resident newLeader = getNamedResident(1);

		if (!camp.hasMember(newLeader.getName())) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_camp_removeNotIncamp", newLeader.getName()));
		}

		camp.setSQLOwner(newLeader);
		camp.save();

		Player player = CivGlobal.getPlayer(newLeader);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_camp_setownerMsg", camp.getName()));
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_setownerSuccess", newLeader.getName()));

	}

	public void leave_cmd() throws CivException {
		Resident resident = getResident();

		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}

		Camp camp = resident.getCamp();
		if (camp.getSQLOwner() == resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_leaveOwner"));
		}

		camp.removeMember(resident);
		camp.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_camp_leaveSuccess", camp.getName()));
	}

	public void new_cmd() throws CivException {

	}

	public void disband_cmd() throws CivException {
		Resident resident = getResident();
		this.validCampOwner();
		Camp camp = this.getCurrentCamp();

		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}

		camp.disband();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_camp_disbandSuccess"));
	}

	public void undo_cmd() throws CivException {
		Resident resident = getResident();

		if (!resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		}

		Camp camp = resident.getCamp();
		if (camp.getSQLOwner() != resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoNotOwner"));
		}

		if (!camp.isUndoable()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoTooLate"));
		}

		CraftableCustomMaterial campMat = CraftableCustomMaterial.getCraftableCustomMaterial("mat_found_camp");
		if (campMat == null) {
			throw new CivException(CivSettings.localize.localizedString("cmd_camp_undoError"));
		}

		ItemStack newStack = CraftableCustomMaterial.spawn(campMat);
		Player player = CivGlobal.getPlayer(resident);
		HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(newStack);
		for (ItemStack stack : leftovers.values()) {
			player.getWorld().dropItem(player.getLocation(), stack);
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("cmd_camp_undoFullInven"));
		}

		camp.undo();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_camp_undoSuccess"));

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
