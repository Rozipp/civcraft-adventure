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
package com.avrgaming.civcraft.command.menu;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.command.taber.AllResidentTaber;
import com.avrgaming.civcraft.command.taber.GroupInTown;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.farm.FarmChunk;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.permission.PermissionNode;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class PlotCommand extends MenuAbstractCommand {

	public PlotCommand() {
		super("plot");
		displayName = CivSettings.localize.localizedString("cmd_plot_Name");

		add(new CustomCommand("info").withAliases("i").withDescription(CivSettings.localize.localizedString("cmd_plot_infoDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
				if (tc == null) throw new CivException(CivSettings.localize.localizedString("cmd_plot_infoNotOwned"));
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_plot_infoHeading"));
				showPermOwnership(sender, tc);
				showCurrentPermissions(sender, tc);
				showToggles(sender, tc);
				showPriceInfo(sender, tc);
			}
		}));
		add(new CustomCommand("toggle").withDescription(CivSettings.localize.localizedString("cmd_plot_toggleDesc")).withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				if ("mobs".toLowerCase().startsWith(arg.toLowerCase())) l.add("mobs");
				if ("fire".toLowerCase().startsWith(arg.toLowerCase())) l.add("fire");
				return l;
			}
		}).withValidator(Validators.validPlotOwner).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				TownChunk tc = Commander.getStandingTownChunk(sender);
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_plot_togglePrompt"));
				if (args[0].equalsIgnoreCase("mobs")) {
					if (tc.perms.isMobs())
						tc.perms.setMobs(false);
					else
						tc.perms.setMobs(true);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_toggleMobs", tc.perms.isMobs()));
				} else
					if (args[0].equalsIgnoreCase("fire")) {
						if (tc.perms.isFire())
							tc.perms.setFire(false);
						else
							tc.perms.setFire(true);
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_toggleFire", tc.perms.isFire()));
					}
				tc.save();
			}
		}));
		add(new CustomCommand("permset").withDescription(CivSettings.localize.localizedString("cmd_plot_permDesc") + "  " + CivSettings.localize.localizedString("cmd_plot_perm_setDesc")).withValidator(Validators.validPlotOwner)
				.withTabCompleter(new AbstractCashedTaber() {
					@Override
					protected List<String> newTabList(String arg) {
						List<String> l = new ArrayList<>();
						if ("build".toLowerCase().startsWith(arg.toLowerCase())) l.add("build");
						if ("destroy".toLowerCase().startsWith(arg.toLowerCase())) l.add("destroy");
						if ("interact".toLowerCase().startsWith(arg.toLowerCase())) l.add("interact");
						if ("itemuse".toLowerCase().startsWith(arg.toLowerCase())) l.add("itemuse");
						if ("reset".toLowerCase().startsWith(arg.toLowerCase())) l.add("reset");
						return l;
					}
				}).withTabCompleter(new AbstractCashedTaber() {
					@Override
					protected List<String> newTabList(String arg) {
						List<String> l = new ArrayList<>();
						if ("build".toLowerCase().startsWith(arg.toLowerCase())) l.add("build");
						if ("destroy".toLowerCase().startsWith(arg.toLowerCase())) l.add("destroy");
						if ("others".toLowerCase().startsWith(arg.toLowerCase())) l.add("others");
						return l;
					}
				}).withTabCompleter(new AbstractCashedTaber() {
					@Override
					protected List<String> newTabList(String arg) {
						List<String> l = new ArrayList<>();
						if ("on".toLowerCase().startsWith(arg.toLowerCase())) l.add("on");
						if ("yes".toLowerCase().startsWith(arg.toLowerCase())) l.add("yes");
						if ("off".toLowerCase().startsWith(arg.toLowerCase())) l.add("off");
						if ("no".toLowerCase().startsWith(arg.toLowerCase())) l.add("no");
						return l;
					}
				}).withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Player player = (Player) sender;
						TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
						if (tc == null) throw new CivException(CivSettings.localize.localizedString("cmd_plot_perm_setnotInTown"));
						if (args.length < 3) {
							showPermCmdHelp(sender);
							throw new CivException(CivSettings.localize.localizedString("cmd_plot_perm_setBadArg"));
						}
						PermissionNode node = null;
						switch (args[0].toLowerCase()) {
						case "build":
							node = tc.perms.build;
							break;
						case "destroy":
							node = tc.perms.destroy;
							break;
						case "interact":
							node = tc.perms.interact;
							break;
						case "itemuse":
							node = tc.perms.itemUse;
							break;
						case "reset":
							// TODO implement permissions reset.
							break;
						default:
							showPermCmdHelp(sender);
							throw new CivException(CivSettings.localize.localizedString("cmd_plot_perm_setBadArg"));
						}
						if (node == null) throw new CivException(CivSettings.localize.localizedString("cmd_plot_perm_setInternalError"));
						boolean on;
						if (args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("yes") || args[2].equalsIgnoreCase("1"))
							on = true;
						else
							if (args[2].equalsIgnoreCase("off") || args[2].equalsIgnoreCase("no") || args[2].equalsIgnoreCase("0"))
								on = false;
							else {
								showPermCmdHelp(sender);
								throw new CivException(CivSettings.localize.localizedString("cmd_plot_perm_setBadArg"));
							}
						switch (args[1].toLowerCase()) {
						case "owner":
							node.setPermitOwner(on);
							break;
						case "group":
							node.setPermitGroup(on);
							break;
						case "others":
							node.setPermitOthers(on);
						}
						tc.save();
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_perm_setSuccess", node.getType(), on, args[1]));
					}
				}));
		add(new CustomCommand("addgroup").withDescription(CivSettings.localize.localizedString("cmd_plot_addgroupDesc")).withValidator(Validators.validPlotOwner).withTabCompleter(new GroupInTown()).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				TownChunk tc = Commander.getStandingTownChunk(sender);
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_plot_addgroupPrompt"));
				if (args[0].equalsIgnoreCase("none")) throw new CivException(CivSettings.localize.localizedString("cmd_plot_addgroupNone"));
				PermissionGroup grp = tc.getTown().GM.getGroup(args[0]);
				if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_plot_removegroupInvalid", args[1]));
				tc.perms.addGroup(grp);
				tc.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_addgroupSuccess", grp.getName()));
			}
		}));
		add(new CustomCommand("setowner").withDescription(CivSettings.localize.localizedString("cmd_plot_setowner")).withTabCompleter(new AllResidentTaber()).withValidator(Validators.validPlotOwner).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				TownChunk tc = Commander.getStandingTownChunk(sender);
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_plot_setownerPrompt"));
				if (args[0].equalsIgnoreCase("none") || args[0].equalsIgnoreCase("-")) {
					tc.perms.setOwner(null);
					tc.save();
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_plot_setownerNone"));
					return;
				}
				Resident resident = Commander.getNamedResident(args, 0);
				if (resident.getTown() != tc.getTown()) throw new CivException(CivSettings.localize.localizedString("cmd_plot_setownerNotRes"));
				tc.perms.setOwner(resident);
				tc.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_setownerSuccess", args[1]));
			}
		}));
		add(new CustomCommand("farminfo").withDescription(CivSettings.localize.localizedString("cmd_plot_farminfoDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				DecimalFormat df = new DecimalFormat("#.#");
				Player player = Commander.getPlayer(sender);
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
				if (fc.getLastRandomInt() < fc.getLastChanceForLast()) success = "yes";
				CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmExtraRate") + " " + CivColor.LightGreen + fc.getLastChanceForLast() + " vs " + CivColor.LightGreen + fc.getLastRandomInt() + " "
						+ CivSettings.localize.localizedString("cmd_plot_farmsuccessToo") + " " + CivColor.LightGreen + success);
				String out = "";
				for (BlockCoord bcoord : fc.getLastGrownCrops()) {
					out += bcoord.toString() + ", ";
				}
				CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_farmCropsGrown") + " " + CivColor.LightGreen + out);
			}
		}));
		add(new CustomCommand("removegroup").withDescription(CivSettings.localize.localizedString("cmd_plot_removegroupDesc")).withTabCompleter(new GroupInTown()).withValidator(Validators.validPlotOwner)
				.withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						TownChunk tc = Commander.getStandingTownChunk(sender);
						if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_plot_removegroupPrompt"));
						if (args[0].equalsIgnoreCase("none")) throw new CivException(CivSettings.localize.localizedString("cmd_plot_removegroupNone"));
						PermissionGroup grp = tc.getTown().GM.getGroup(args[0]);
						if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_plot_removegroupInvalid", args[1]));
						tc.perms.removeGroup(grp);
						tc.save();
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_plot_removegroupSuccess", grp.getName()));
					}
				}));
		add(new CustomCommand("cleargroups").withDescription(CivSettings.localize.localizedString("cmd_plot_cleargroupsDesc")).withValidator(Validators.validPlotOwner).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				TownChunk tc = Commander.getStandingTownChunk(sender);
				tc.perms.clearGroups();
				tc.perms.addGroup(tc.getTown().GM.getDefaultGroup());
				tc.save();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_plot_cleargroupsSuccess"));
			}
		}));
	}

	private void showCurrentPermissions(CommandSender sender, TownChunk tc) {
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermBuild") + " " + CivColor.LightGreen + tc.perms.getBuildString());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermDestroy") + " " + CivColor.LightGreen + tc.perms.getDestroyString());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermInteract") + " " + CivColor.LightGreen + tc.perms.getInteractString());
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showPermItemUse") + " " + CivColor.LightGreen + tc.perms.getItemUseString());
	}

	private void showPermOwnership(CommandSender sender, TownChunk tc) {
		String out = CivColor.Green + CivSettings.localize.localizedString("Town") + " " + CivColor.LightGreen + tc.getTown().getName();
		out += CivColor.Green + " " + CivSettings.localize.localizedString("Owner") + " " + CivColor.LightGreen;
		if (tc.perms.getOwner() != null)
			out += tc.perms.getOwner().getName();
		else
			out += CivSettings.localize.localizedString("none");
		out += CivColor.Green + " " + CivSettings.localize.localizedString("cmd_civ_group_listGroup") + " " + CivColor.LightGreen;
		if (tc.perms.getGroups().size() != 0)
			out += tc.perms.getGroupString();
		else
			out += CivSettings.localize.localizedString("none");
		CivMessage.send(sender, out);
	}

	private void showToggles(CommandSender sender, TownChunk tc) {
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showMobs") + " " + CivColor.LightGreen + tc.perms.isMobs() + " " + CivColor.Green + CivSettings.localize.localizedString("cmd_plot_showFire")
				+ " " + CivColor.LightGreen + tc.perms.isFire());
	}

	private void showPriceInfo(CommandSender sender, TownChunk tc) {
		String out = "";
		if (tc.isForSale()) out += CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_plot_showPrice", tc.getPrice(), CivSettings.CURRENCY_NAME);
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Value") + " " + CivColor.LightGreen + tc.getValue() + out);
	}

	private void showPermCmdHelp(CommandSender sender) {
		CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_plot_perm_help1"));
		CivMessage.send(sender, CivColor.LightGray + "    " + CivSettings.localize.localizedString("cmd_plot_perm_help2"));
		CivMessage.send(sender, CivColor.LightGray + "    " + CivSettings.localize.localizedString("cmd_plot_perm_help3"));
	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		showBasicHelp(sender);
		CivMessage.send(sender, CivColor.LightGray + "/plot perm set <type> <groupType> [on|off] ");
		CivMessage.send(sender, CivColor.LightGray + "    types: [build|destroy|interact|itemuse|reset]");
		CivMessage.send(sender, CivColor.LightGray + "    groupType: [owner|group|others]");
	}
}
