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
package com.avrgaming.civcraft.command.plot;

import java.text.SimpleDateFormat;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.farm.FarmChunk;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class PlotCommand extends CommandBase {

	@Override
	public void init() {
		command = "/plot";
		displayName = CivSettings.localize.localizedString("cmd_plot_Name");

		cs.add("info", "i", CivSettings.localize.localizedString("cmd_plot_infoDesc"));
		cs.add("toggle", CivSettings.localize.localizedString("cmd_plot_toggleDesc"));
		cs.add("perm", CivSettings.localize.localizedString("cmd_plot_permDesc"));
		cs.add("addgroup", CivSettings.localize.localizedString("cmd_plot_addgroupDesc"));
		cs.add("setowner", CivSettings.localize.localizedString("cmd_plot_setowner"));
		cs.add("farminfo", CivSettings.localize.localizedString("cmd_plot_farminfoDesc"));
		cs.add("removegroup", CivSettings.localize.localizedString("cmd_plot_removegroupDesc"));
		cs.add("cleargroups", CivSettings.localize.localizedString("cmd_plot_cleargroupsDesc"));
	}

	public void farminfo_cmd() throws CivException {
		Player player = getPlayer();

		ChunkCoord coord = new ChunkCoord(player.getLocation());
		FarmChunk fc = CivGlobal.getFarmChunk(coord);

		if (fc == null) throw new CivException(CivSettings.localize.localizedString("cmd_plot_notFarm"));
		if (fc.getConstruct().isActive() == false) throw new CivException(CivSettings.localize.localizedString("cmd_plot_farmNotDone"));

		String dateString = CivSettings.localize.localizedString("Never");

		if (fc.getLastGrowDate() != null) {
			SimpleDateFormat sdf = CivGlobal.dateFormat;
			dateString = sdf.format(fc.getLastGrowDate());
		}

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_plot_farmInfoHeading"));
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmLastGrowTime") + " " + CivColor.LightGreen + dateString);
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmLastGrowVolume") + " " + CivColor.LightGreen + fc.getLastGrowTickCount());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmunloaded") + " " + CivColor.LightGreen + fc.getMissedGrowthTicksStat());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmRate") + " " + CivColor.LightGreen + df.format(fc.getLastEffectiveGrowthRate() * 100) + "%");

		String success = "no";
		if (fc.getLastRandomInt() < fc.getLastChanceForLast()) {
			success = "yes";
		}

		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmExtraRate") + " " + CivColor.LightGreen + fc.getLastChanceForLast() + " vs " + CivColor.LightGreen + fc.getLastRandomInt() + " "
				+ CivSettings.localize.localizedString("cmd_plot_farmsuccessToo") + " " + CivColor.LightGreen + success);

		String out = "";
		for (BlockCoord bcoord : fc.getLastGrownCrops()) {
			out += bcoord.toString() + ", ";
		}

		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmCropsGrown") + " " + CivColor.LightGreen + out);

	}

	public void setowner_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		validPlotOwner();

		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_plot_setownerPrompt"));
		}

		if (args[1].equalsIgnoreCase("none")) {
			tc.perms.setOwner(null);
			tc.save();
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_plot_setownerNone"));
			return;
		}

		Resident resident = getNamedResident(1);

		if (resident.getTown() != tc.getTown()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_plot_setownerNotRes"));
		}

		tc.perms.setOwner(resident);
		tc.save();

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_setownerSuccess", args[1]));

	}

	public void removegroup_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		validPlotOwner();

		if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_plot_removegroupPrompt"));
		if (args[1].equalsIgnoreCase("none")) throw new CivException(CivSettings.localize.localizedString("cmd_plot_removegroupNone"));

		PermissionGroup grp = tc.getTown().GM.getGroup(args[1]);
		if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_plot_removegroupInvalid", args[1]));

		tc.perms.removeGroup(grp);
		tc.save();

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_removegroupSuccess", grp.getName()));
	}

	public void cleargroups_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		validPlotOwner();

		tc.perms.clearGroups();
		tc.perms.addGroup(tc.getTown().GM.getDefaultGroup());
		tc.save();
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_plot_cleargroupsSuccess"));
		return;
	}

	public void addgroup_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		validPlotOwner();

		if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("cmd_plot_addgroupPrompt"));
		if (args[1].equalsIgnoreCase("none")) throw new CivException(CivSettings.localize.localizedString("cmd_plot_addgroupNone"));

		PermissionGroup grp = tc.getTown().GM.getGroup(args[1]);
		if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_plot_removegroupInvalid", args[1]));

		tc.perms.addGroup(grp);
		tc.save();

		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_addgroupSuccess", grp.getName()));
	}

	public void toggle_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		this.validPlotOwner();

		if (args.length < 2) {
			throw new CivException(CivSettings.localize.localizedString("cmd_plot_togglePrompt"));
		}

		if (args[1].equalsIgnoreCase("mobs")) {
			if (tc.perms.isMobs()) {
				tc.perms.setMobs(false);
			} else {
				tc.perms.setMobs(true);
			}

			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_toggleMobs", tc.perms.isMobs()));

		} else
			if (args[1].equalsIgnoreCase("fire")) {
				if (tc.perms.isFire()) {
					tc.perms.setFire(false);
				} else {
					tc.perms.setFire(true);
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_toggleFire", tc.perms.isFire()));
			}
		tc.save();
	}

	public void perm_cmd() throws CivException {
		PlotPermCommand cmd = new PlotPermCommand();
		cmd.onCommand(sender, null, "perm", this.stripArgs(args, 1));
	}

	private void showCurrentPermissions(TownChunk tc) {
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermBuild") + " " + CivColor.LightGreen + tc.perms.getBuildString());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermDestroy") + " " + CivColor.LightGreen + tc.perms.getDestroyString());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermInteract") + " " + CivColor.LightGreen + tc.perms.getInteractString());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermItemUse") + " " + CivColor.LightGreen + tc.perms.getItemUseString());
	}

	private void showPermOwnership(TownChunk tc) {
		String out = CivColor.Green + CivSettings.localize.localizedString("Town") + " " + CivColor.LightGreen + tc.getTown().getName();
		out += CivColor.Green + " " + CivSettings.localize.localizedString("Owner") + " " + CivColor.LightGreen;
		if (tc.perms.getOwner() != null) {
			out += tc.perms.getOwner().getName();
		} else {
			out += CivSettings.localize.localizedString("none");
		}

		out += CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_group_listGroup") + " " + CivColor.LightGreen;
		if (tc.perms.getGroups().size() != 0) {
			out += tc.perms.getGroupString();
		} else {
			out += CivSettings.localize.localizedString("none");
		}

		CivMessage.send(sender, out);
	}

	/* private void showPermCmdHelp() { CivMessage.send(sender, CivColor.LightGray+"/plot perm set <type> <groupType> [on|off] ");
	 * CivMessage.send(sender, CivColor.LightGray+"    types: [build|destroy|interact|itemuse|reset]"); CivMessage.send(sender,
	 * CivColor.LightGray+"    groupType: [owner|group|others]"); } */

	public void info_cmd() throws CivException {
		Player player = getPlayer();
		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc == null) throw new CivException(CivSettings.localize.localizedString("cmd_plot_infoNotOwned"));

		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_plot_infoHeading"));
		showPermOwnership(tc);
		showCurrentPermissions(tc);
		showToggles(tc);
		showPriceInfo(tc);
	}

	private void showToggles(TownChunk tc) {
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showMobs") + " " + CivColor.LightGreen + tc.perms.isMobs() + " " + CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showFire")
				+ " " + CivColor.LightGreen + tc.perms.isFire());
	}

	private void showPriceInfo(TownChunk tc) {
		String out = "";
		if (tc.isForSale()) {
			out += CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_plot_showPrice", tc.getPrice(), CivSettings.CURRENCY_NAME);
		}
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Value") + " " + CivColor.LightGreen + tc.getValue() + out);
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() {
		return;
	}

	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
		// info_cmd();
		// CivMessage.send(sender, CivColor.LightGray+"Subcommands available: See /plot help");
	}

}
