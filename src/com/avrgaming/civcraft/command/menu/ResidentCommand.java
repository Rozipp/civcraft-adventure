/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.menu;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.SelectorAbstractCommand;
import com.avrgaming.civcraft.command.taber.AllResidentTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class ResidentCommand extends MenuAbstractCommand {

	public ResidentCommand(String perentComman) {
		super(perentComman);
		this.setAliases("res");
		displayName = CivSettings.localize.localizedString("cmd_res_Name");

		add(new CustomCommand("info").withAliases("i").withDescription(CivSettings.localize.localizedString("cmd_res_infoDesc")) //
				.withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						ResidentCommand.show(sender, Commander.getResident(sender));
					}
				}));
		add(new SelectorAbstractCommand("toggle", //
				new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						Resident resident = Commander.getResident(sender);
						String arg = args[0];
						boolean result;
						switch (arg.toLowerCase()) {
						case "map":
							resident.setShowMap(!resident.isShowMap());
							result = resident.isShowMap();
							break;
						case "showtown":
							resident.setShowTown(!resident.isShowTown());
							result = resident.isShowTown();
							break;
						case "showciv":
							resident.setShowCiv(!resident.isShowCiv());
							result = resident.isShowCiv();
							break;
						case "showscout":
							resident.setShowScout(!resident.isShowScout());
							result = resident.isShowScout();
							break;
						case "info":
							resident.setShowInfo(!resident.isShowInfo());
							result = resident.isShowInfo();
							break;
						case "combatinfo":
							resident.setCombatInfo(!resident.isCombatInfo());
							result = resident.isCombatInfo();
							break;
						case "itemdrops":
							resident.toggleItemMode();
							return;
						default:
							throw new CivException(CivSettings.localize.localizedString("cmd_unkownFlag") + " " + arg);
						}
						resident.save();
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_toggled") + " " + arg + " -> " + result);
					}
				}) {

			@Override
			public void initSubCommands() {
				add("map", CivSettings.localize.localizedString("cmd_res_toggle_mapDesc"));
				add("info", CivSettings.localize.localizedString("cmd_res_toggle_infoDesc"));
				add("showtown", CivSettings.localize.localizedString("cmd_res_toggle_showtownDesc"));
				add("showciv", CivSettings.localize.localizedString("cmd_res_toggle_showcivDesc"));
				add("showscout", CivSettings.localize.localizedString("cmd_res_toggle_showscoutDesc"));
				add("combatinfo", CivSettings.localize.localizedString("cmd_res_toggle_combatinfoDesc"));
				add("itemdrops", CivSettings.localize.localizedString("cmd_res_toggle_itemdropsDesc"));
				add("titles", CivSettings.localize.localizedString("cmd_res_toggle_titleAPIDesc"));
			}
		});
		add(new CustomCommand("show").withDescription(CivSettings.localize.localizedString("cmd_res_showDesc")).withTabCompleter(new AllResidentTaber()).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_res_showPrompt"));
				ResidentCommand.show(sender, Commander.getNamedResident(args, 0));
			}
		}));
		add(new CustomCommand("resetspawn").withDescription(CivSettings.localize.localizedString("cmd_res_resetspawnDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Location spawn = player.getWorld().getSpawnLocation();
				player.setBedSpawnLocation(spawn, true);
				CivMessage.sendSuccess(player, CivSettings.localize.localizedString("cmd_res_resetspawnSuccess"));
			}
		}));
		add(new CustomCommand("book").withAliases("b").withDescription(CivSettings.localize.localizedString("cmd_res_bookDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);

				/* Determine if he already has the book. */
				for (ItemStack stack : player.getInventory().getContents()) {
					if (stack == null) continue;
					CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
					if (craftMat == null) continue;
					if (craftMat.getConfigId().equals("mat_tutorial_book")) {
						throw new CivException(CivSettings.localize.localizedString("cmd_res_bookHaveOne"));
					}
				}

				CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial("mat_tutorial_book");
				ItemStack helpBook = CraftableCustomMaterial.spawn(craftMat);

				HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(helpBook);
				if (leftovers != null && leftovers.size() >= 1) throw new CivException(CivSettings.localize.localizedString("cmd_res_bookInvenFull"));
				CivMessage.sendSuccess(player, CivSettings.localize.localizedString("cmd_res_bookSuccess"));
			}
		}));

		add(new TimezoneCustonCommand());
		add(new CustomCommand("pvptimer").withDescription(CivSettings.localize.localizedString("cmd_res_pvptimerDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (!resident.isProtected()) CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_res_pvptimerNotActive"));
				resident.setProtected(false);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_pvptimerSuccess"));
			}
		}));
		add(new CustomCommand("outlawed").withDescription(CivSettings.localize.localizedString("cmd_res_outlawedDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				StringBuilder outlaws = new StringBuilder();
				for (Town town : CivGlobal.getTowns()) {
					if (!town.isOutlaw(resident)) continue;
					outlaws.append(CivColor.Red).append(town.getName()).append(" [").append(town.getCiv().getName()).append("] ").append("\n");
				}
				if (outlaws.toString().equals("")) {
					throw new CivException(CivSettings.localize.localizedString("cmd_res_outlawed_noOne"));
				}
				CivMessage.send(sender, CivSettings.localize.localizedString("cmd_res_outlawed_list", outlaws.toString()));
			}
		}));
	}

	public static void show(CommandSender sender, Resident resident) {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("var_Resident", resident.getName()));
		Date lastOnline = new Date(resident.getLastOnline());
		SimpleDateFormat sdf = CivGlobal.dateFormat;
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showLastOnline") + " " + CivColor.LightGreen + sdf.format(lastOnline));
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Town") + " " + CivColor.LightGreen + (resident.getTown() == null ? "none" : resident.getTown().getName()));
		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Camp") + " " + CivColor.LightGreen + (resident.getCamp() == null ? "none" : resident.getCamp().getName()));

		if (sender.getName().equalsIgnoreCase(resident.getName()) || sender.isOp()) {
			CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showTreasure") + " " + CivColor.LightGreen + resident.getTreasury().getBalance() + " " + CivColor.Green);
			if (resident.hasTown()) {
				if (resident.getSelectedTown() != null) {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showSelected") + " " + CivColor.LightGreen + resident.getSelectedTown().getName());
				} else {
					CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("cmd_res_showSelected") + " " + CivColor.LightGreen + resident.getTown().getName());
				}
			}
		}

		CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("Groups") + " " + resident.getGroupsString());
	}

	private class TimezoneCustonCommand extends CustomCommand {

		public TimezoneCustonCommand() {
			super("timezone");
			this.setDescription(CivSettings.localize.localizedString("cmd_res_timezoneDesc"));
			this.setExecutor(new CustonExecutor() {
				@Override
				public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
					Resident resident = Commander.getResident(sender);
					if (args.length < 1) {
						CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_res_timezoneCurrent") + " " + resident.getTimezone());
						return;
					}
					if (args[0].equalsIgnoreCase("list")) {
						CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_res_timezoneHeading"));
						String out = "";
						for (String zone : TimeZone.getAvailableIDs()) {
							out += zone + ", ";
						}
						CivMessage.send(sender, out);
						return;
					}

					TimeZone timezone = TimeZone.getTimeZone(args[0]);

					if (timezone.getID().equals("GMT") && !args[0].equalsIgnoreCase("GMT")) {
						CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("var_cmd_res_timezonenotFound1", args[0]));
						CivMessage.send(sender, CivColor.LightGray + CivSettings.localize.localizedString("cmd_res_timezoneNotFound3"));
					}

					resident.setTimezone(timezone.getID());
					resident.save();
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_res_timezoneSuccess", timezone.getID()));
				}
			});
		}
	}
}
