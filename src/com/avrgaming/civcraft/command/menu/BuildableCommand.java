/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.menu;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.Validators;
import com.avrgaming.civcraft.command.taber.AbstractTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.config.ConfigConstructInfo.ConstructType;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.interactive.BuildCallback;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class BuildableCommand extends MenuAbstractCommand {

	public BuildableCommand(String perentComman) {
		super(perentComman);
		displayName = CivSettings.localize.localizedString("cmd_build_Desc");
		this.addValidator(Validators.validHasTown);

		add(new CustomCommand("list").withAliases("l").withDescription(CivSettings.localize.localizedString("cmd_build_listDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_listHeader"));
				Town town = Commander.getSelectedTown(sender);
				CivMessage.send(sender, CivSettings.localize.localizedString("Cost") + "C, " + CivSettings.localize.localizedString("Upkeep") + "U, " + CivSettings.localize.localizedString("Hammers") + "H, "
						+ CivSettings.localize.localizedString("Remaining") + "L");
				for (ConfigConstructInfo sinfo : CivSettings.constructs.values()) {
					if (sinfo.type != ConstructType.Structure) continue;
					if (sinfo.isAvailable(town)) {
						String s = "";
						s = CivColor.addTabToString(s + CivColor.LightPurple, sinfo.displayName, 22);
						s = CivColor.addTabToString(s + CivColor.Yellow, "C: " + sinfo.cost, 12);
						s = CivColor.addTabToString(s + CivColor.Yellow, "U: " + sinfo.upkeep, 12);
						s = CivColor.addTabToString(s + CivColor.Yellow, "H: " + sinfo.hammer_cost, 12);
						s = s + CivColor.Yellow + "L: " + ((sinfo.limit == 0) ? CivSettings.localize.localizedString("Unlimited") : "" + (sinfo.limit - town.BM.getBuildableByIdCount(sinfo.id)));
						CivMessage.send(sender, s);
					}
				}
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_listWondersHeader"));

				for (ConfigConstructInfo sinfo : CivSettings.constructs.values()) {
					if (sinfo.type != ConstructType.Wonder) continue;
					if (!sinfo.isAvailable(town)) continue;
					String leftString = (sinfo.limit == 0) ? CivSettings.localize.localizedString("Unlimited") : "" + (sinfo.limit - (town.BM.getBuildableByIdCount(sinfo.id)));

					if (Wonder.isWonderAvailable(sinfo.id)) {
						double rate = 1.0;
						rate -= town.getBuffManager().getEffectiveDouble("buff_rush");
						rate -= town.getBuffManager().getEffectiveDouble("buff_grandcanyon_rush");
						rate -= town.getBuffManager().getEffectiveDouble("buff_mother_tree_tile_improvement_cost");
						CivMessage.send(sender, CivColor.LightPurple + sinfo.displayName + " " + CivColor.Yellow + CivSettings.localize.localizedString("Cost") + " " + sinfo.cost + " " + CivSettings.localize.localizedString("Upkeep") + " "
								+ sinfo.upkeep + " " + CivSettings.localize.localizedString("Hammers") + " " + sinfo.hammer_cost * rate + " " + CivSettings.localize.localizedString("Remaining") + " " + leftString);
					} else {
						Wonder wonder = CivGlobal.getWonderByConfigId(sinfo.id);
						CivMessage.send(sender, CivColor.LightGray + sinfo.displayName + " Cost: " + sinfo.cost + " - "
								+ CivSettings.localize.localizedString("var_cmd_build_listWonderAlreadyBuild", wonder.getTown().getName(), wonder.getTown().getCiv().getName()));
					}
				}
			}
		}));
		add(new CustomCommand("build").withAliases("b").withDescription(CivSettings.localize.localizedString("cmd_build_help1") + " " + CivSettings.localize.localizedString("cmd_build_help2"))
				.withValidator(Validators.validMayorAssistantLeader).withTabCompleter(new AbstractTaber() {
					@Override
					public List<String> getTabList(CommandSender sender, String arg) throws CivException {
						List<String> l = new ArrayList<>();
						Town town = Commander.getSelectedTown(sender);
						for (ConfigConstructInfo sinfo : CivSettings.constructs.values()) {
							if (sinfo.displayName.toLowerCase().startsWith(arg) && sinfo.isAvailable(town)) {
								if (sinfo.type == ConstructType.Structure) l.add(sinfo.displayName.replace(" ", "_"));
								if (sinfo.type == ConstructType.Wonder) {
									if (Wonder.isWonderAvailable(sinfo.id)) l.add(sinfo.displayName.replace(" ", "_"));
								}
							}
						}
						return l;
					}
				}).withExecutor(new CustonExecutor() {
					@Override
					public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
						if (args.length < 1) {
							GuiInventory.openGuiInventory(Commander.getPlayer(sender), "Structure", "false");
							return;
						}
						String fullArgs = Commander.combineArgs(args);
						fullArgs = fullArgs.replace("_", " ");
						buildByName(sender, fullArgs);
					}
				}));
		add(new CustomCommand("progress").withAliases("p").withDescription(CivSettings.localize.localizedString("cmd_build_progressDesc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_undoHeader"));
				Town town = Commander.getSelectedTown(sender);
				for (Buildable b : town.BM.getBuildablePoolInProgress()) {
					DecimalFormat df = new DecimalFormat();
					CivMessage.send(sender, CivColor.LightPurple + b.getDisplayName() + ": " + CivColor.Yellow + b.getPercent_complete() + "% (" + df.format(b.getHammersCompleted()) + "/" + b.getHammerCost() + ")");
					// CivMessage.send(sender, CivColor.LightPurple+b.getDisplayName()+" "+CivColor.Yellow+"("+
					// b.builtBlockCount+" / "+b.getTotalBlockCount()+")");
				}
			}
		}));
		add(new CustomCommand("repairnearest").withDescription(CivSettings.localize.localizedString("cmd_build_repairnearestDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Player player = Commander.getPlayer(sender);
				if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_build_repairNotDuringWar"));
				Structure nearest = (Structure) CivGlobal.getConstructFromChunk(new ChunkCoord(player.getLocation()));
				if (nearest == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_Invalid"));
				if (!nearest.isDestroyed()) throw new CivException(CivSettings.localize.localizedString("var_cmd_build_repairNotDestroyed", nearest.getDisplayName(), nearest.getCorner()));
				if (!town.getCiv().hasTechnologys(nearest.getRequiredTechnology())) throw new CivException(CivSettings.localize.localizedString("var_cmd_build_repairMissingTech", nearest.getDisplayName(), nearest.getCorner()));
				if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_cmd_build_repairConfirmPrompt", CivColor.Yellow + nearest.getDisplayName() + CivColor.LightGreen,
							CivColor.Yellow + nearest.getCorner() + CivColor.LightGreen, CivColor.Yellow + nearest.getRepairCost() + CivColor.LightGreen, CivColor.Yellow + CivSettings.CURRENCY_NAME + CivColor.LightGreen));
					CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("cmd_build_repairConfirmPrompt2"));
					return;
				}
				nearest.repairStructure();
				CivMessage.sendSuccess(player, nearest.getDisplayName() + " " + CivSettings.localize.localizedString("Repaired"));
			}
		}));
		add(new CustomCommand("demolish").withDescription(CivSettings.localize.localizedString("cmd_build_demolishDesc")).withValidator(Validators.validMayor).withTabCompleter(new AbstractTaber() {
			@Override
			public List<String> getTabList(CommandSender sender, String arg) throws CivException {
				List<String> l = new ArrayList<>();
				Town town = Commander.getSelectedTown(sender);
				for (Structure struct : town.BM.getStructures()) {
					String s = struct.getDisplayName().replace(" ", "_") + ":" + struct.getCorner().toString();
					if (s.toLowerCase().startsWith(arg)) l.add(s);
				}
				return l;
			}
		}).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				if (args.length < 1) {
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_demolishHeader"));
					for (Structure struct : town.BM.getStructures()) {
						CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_build_demolish", struct.getDisplayName(), CivColor.Yellow + struct.getCorner().toString() + CivColor.White));
					}
					return;
				}
				try {
					String[] split = args[0].split(":");
					String arg = (split.length > 1) ? split[1] : split[0];
					BlockCoord coord = new BlockCoord(arg);
					Structure struct = town.BM.getStructure(coord);
					if (struct == null) {
						CivMessage.send(sender, CivColor.Rose + " " + CivSettings.localize.localizedString("NoStructureAt") + " " + arg);
						return;
					}
					struct.getTown().BM.demolish(struct, false);
					CivMessage.sendTown(struct.getTown(), struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
				} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
					CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_build_demolishFormatError"));
				}
			}
		}));
		add(new CustomCommand("demolishnearest").withDescription(CivSettings.localize.localizedString("cmd_build_demolishnearestDesc")).withValidator(Validators.validMayor).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Player player = Commander.getPlayer(sender);
				Structure nearest = (Structure) CivGlobal.getConstructFromChunk(new ChunkCoord(player.getLocation()));
				if (nearest == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_Invalid"));
				if (args.length < 1 || !args[0].equalsIgnoreCase("yes")) {
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_cmd_build_demolishNearestConfirmPrompt", CivColor.Yellow + nearest.getDisplayName() + CivColor.LightGreen,
							CivColor.Yellow + nearest.getCorner() + CivColor.LightGreen));
					CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("cmd_build_demolishNearestConfirmPrompt2"));
					nearest.flashConstructBlocks();
					return;
				}
				town.BM.demolish(nearest, false);
				CivMessage.sendSuccess(player, nearest.getDisplayName() + " at " + nearest.getCorner() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
			}
		}));
		add(new CustomCommand("refreshnearest").withDescription(CivSettings.localize.localizedString("cmd_build_refreshnearestDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getSelectedTown(sender);
				Resident resident = Commander.getResident(sender);
				if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorNotMayor"));
				town.BM.refreshNearestBuildable(resident);
			}
		}));
		add(new CustomCommand("validatenearest").withDescription(CivSettings.localize.localizedString("cmd_build_validateNearestDesc")).withValidator(Validators.validMayorAssistantLeader).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Resident resident = Commander.getResident(sender);
				Buildable buildable = CivGlobal.getNearestBuildable(player.getLocation());
				if (buildable.getTown() != resident.getTown()) throw new CivException(CivSettings.localize.localizedString("cmd_build_validateNearestYourTownOnly"));
				if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_build_validatenearestNotDuringWar"));
				if (buildable.isIgnoreFloating()) throw new CivException(CivSettings.localize.localizedString("var_cmd_build_validateNearestExempt", buildable.getDisplayName()));
				CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_build_validateNearestSuccess", buildable.getDisplayName(), buildable.getCenterLocation().toVector()));
				buildable.validateAsyncTask(player);
			}
		}));
		add(new CustomCommand("calc").withDescription(CivSettings.localize.localizedString("cmd_build_calc_Desc")).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				// Town town = Commander.getSelectedTown(sender);
				// Buildable b = town.BM.getBuildableInprogress();
				// if (b != null) {
				// CivGlobal.dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
				// long milisec = (long) TimeTools.civtickMiliSecond * b.getNeadHammersToComplit() / town.PM.calcHammerPerCivtick();
				// long end = System.currentTimeMillis() + milisec;
				// String messageSender = "§a" + CivSettings.localize.localizedString("cmd_build_calc_result", "§2" + b.getDisplayName() + "§a", "§c" +
				// CivGlobal.dateFormat.format(end) + "§a");
				// String messageTown = CivSettings.localize.localizedString("cmd_build_calc_result", "§2" + b.getDisplayName() + CivColor.RESET, "§c" +
				// CivGlobal.dateFormat.format(end) + CivColor.RESET);
				// CivMessage.send(sender, (String) messageSender);
				// CivMessage.sendTown(town, messageTown);
				// } else {
				// throw new CivException(CivSettings.localize.localizedString("cmd_build_notBuilding"));
				// }
			}
		}));
	}

	private void buildByName(CommandSender sender, String fullArgs) throws CivException {
		ConfigConstructInfo sinfo = CivSettings.getConstructInfoByName(fullArgs);
		if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + fullArgs);
		Commander.getResident(sender).setPendingCallback(new BuildCallback(Commander.getPlayer(sender), sinfo, Commander.getSelectedTown(sender)));
	}

}
