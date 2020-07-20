/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.interactive.BuildCallback;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;

public class BuildCommand extends CommandBase implements TabCompleter {

	@Override
	public void init() {

		command = "/build";
		displayName = CivSettings.localize.localizedString("cmd_build_Desc");
		cs.sendUnknownToDefault = true;

		cs.add("list", "l", CivSettings.localize.localizedString("cmd_build_listDesc"));
		cs.add("progress", "p", CivSettings.localize.localizedString("cmd_build_progressDesc"));
		cs.add("repairnearest", CivSettings.localize.localizedString("cmd_build_repairnearestDesc"));
		cs.add("demolish", CivSettings.localize.localizedString("cmd_build_demolishDesc"));
		cs.add("demolishnearest", CivSettings.localize.localizedString("cmd_build_demolishnearestDesc"));
		cs.add("refreshnearest", CivSettings.localize.localizedString("cmd_build_refreshnearestDesc"));
		cs.add("validatenearest", CivSettings.localize.localizedString("cmd_build_validateNearestDesc"));
		cs.add("calc", CivSettings.localize.localizedString("cmd_build_calc_Desc"));
		String info = CivSettings.localize.localizedString("cmd_build_help1") + " " + CivSettings.localize.localizedString("cmd_build_help2");
		info = info.replace("[", "§a<");
		info = info.replace("]", ">§f");
		info = info.replace("(", "§2(");
		info = info.replace(")", ")§f");
		CivMessage.send(this.sender, (String) ("§e" + this.command + "§f" + ": " + info));
	}

	@Override
	public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		CivLog.debug(arg0.toString());
		CivLog.debug(arg1.toString());
		CivLog.debug(arg2.toString());
		CivLog.debug(arg3.toString());
		String[] s = {"list", "progress","repairnearest", "demolish", "demolishnearest", "refreshnearest",
				"validatenearest", "calc"};
		List<String> l = new ArrayList<>();
		for (String ce : s) {
			if (arg3[0].isEmpty() || ce.startsWith(arg3[0])) l.add(ce);
		}
		return l;
	}

	public void calc_cmd() throws CivException {
		Town town = this.getSelectedTown();
		Buildable b = town.BM.getBuildableInprogress();
		if (b != null) {
			CivGlobal.dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
			long milisec = (long) TimeTools.civtickMiliSecond * b.getNeadHammersToComplit() / town.PM.calcHammerPerCivtick();
			long end = System.currentTimeMillis() + milisec;
			String messageSender = "§a" + CivSettings.localize.localizedString("cmd_build_calc_result", "§2" + b.getDisplayName() + "§a", "§c" + CivGlobal.dateFormat.format(end) + "§a");
			String messageTown = CivSettings.localize.localizedString("cmd_build_calc_result", "§2" + b.getDisplayName() + CivColor.RESET, "§c" + CivGlobal.dateFormat.format(end) + CivColor.RESET);
			CivMessage.send(sender, (String) messageSender);
			CivMessage.sendTown(town, messageTown);
		} else {
			throw new CivException(CivSettings.localize.localizedString("cmd_build_notBuilding"));
		}
	}

	public void validatenearest_cmd() throws CivException {
		Player player = getPlayer();
		Resident resident = getResident();
		Buildable buildable = CivGlobal.getNearestBuildable(player.getLocation());

		if (buildable.getTown() != resident.getTown()) throw new CivException(CivSettings.localize.localizedString("cmd_build_validateNearestYourTownOnly"));
		if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("cmd_build_validatenearestNotDuringWar"));
		if (buildable.isIgnoreFloating()) throw new CivException(CivSettings.localize.localizedString("var_cmd_build_validateNearestExempt", buildable.getDisplayName()));

		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_cmd_build_validateNearestSuccess", buildable.getDisplayName(), buildable.getCenterLocation().toVector()));
		buildable.validateAsyncTask(player);
	}

	public void refreshnearest_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("town_refresh_errorNotMayor"));
		town.BM.refreshNearestBuildable(resident);
	}

	public void repairnearest_cmd() throws CivException {
		Town town = getSelectedTown();
		Player player = getPlayer();

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

	public void demolishnearest_cmd() throws CivException {
		Town town = getSelectedTown();
		Player player = getPlayer();

		Structure nearest = (Structure) CivGlobal.getConstructFromChunk(new ChunkCoord(player.getLocation()));
		if (nearest == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_Invalid"));

		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
			CivMessage.send(player, CivColor.LightGreen
					+ CivSettings.localize.localizedString("var_cmd_build_demolishNearestConfirmPrompt", CivColor.Yellow + nearest.getDisplayName() + CivColor.LightGreen, CivColor.Yellow + nearest.getCorner() + CivColor.LightGreen));
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("cmd_build_demolishNearestConfirmPrompt2"));

			nearest.flashConstructBlocks();
			return;
		}

		town.BM.demolish(nearest, false);
		CivMessage.sendSuccess(player, nearest.getDisplayName() + " at " + nearest.getCorner() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
	}

	public void demolish_cmd() throws CivException {
		Town town = getSelectedTown();

		if (args.length < 2) {
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_demolishHeader"));
			for (Structure struct : town.BM.getStructures()) {
				CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_build_demolish", struct.getDisplayName(), CivColor.Yellow + struct.getCorner().toString() + CivColor.White));
			}
			return;
		}

		try {
			BlockCoord coord = new BlockCoord(args[1]);
			Structure struct = town.BM.getStructure(coord);
			if (struct == null) {
				CivMessage.send(sender, CivColor.Rose + " " + CivSettings.localize.localizedString("NoStructureAt") + " " + args[1]);
				return;
			}
			struct.getTown().BM.demolish(struct, false);
			CivMessage.sendTown(struct.getTown(), struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			CivMessage.sendError(sender, CivSettings.localize.localizedString("cmd_build_demolishFormatError"));
		}
	}

	public void progress_cmd() throws CivException {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_undoHeader"));
		Town town = getSelectedTown();
		for (Buildable b : town.BM.getBuildablePoolInProgress()) {
			DecimalFormat df = new DecimalFormat();

			CivMessage.send(sender, CivColor.LightPurple + b.getDisplayName() + ": " + CivColor.Yellow + b.getPercent_complete() + "% (" + df.format(b.getHammersCompleted()) + "/" + b.getHammerCost() + ")");

			// CivMessage.send(sender, CivColor.LightPurple+b.getDisplayName()+" "+CivColor.Yellow+"("+
			// b.builtBlockCount+" / "+b.getTotalBlockCount()+")");
		}

	}

	public void list_available_structures() throws CivException {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_listHeader"));
		Town town = getSelectedTown();
		for (ConfigBuildableInfo sinfo : CivSettings.structures.values()) {
			if (sinfo.isAvailable(town)) {
				String leftString = "";
				if (sinfo.limit == 0) {
					leftString = CivSettings.localize.localizedString("Unlimited");
				} else {
					leftString = "" + (sinfo.limit - town.BM.getBuildableByIdCount(sinfo.id));
				}

				CivMessage.send(sender, CivColor.LightPurple + sinfo.displayName + " " + CivColor.Yellow + CivSettings.localize.localizedString("Cost") + " " + sinfo.cost + " " + CivSettings.localize.localizedString("Upkeep") + " "
						+ sinfo.upkeep + " " + CivSettings.localize.localizedString("Hammers") + " " + sinfo.hammer_cost + " " + CivSettings.localize.localizedString("Remaining") + " " + leftString);
			}
		}
	}

	public void list_available_wonders() throws CivException {
		CivMessage.sendHeading(sender, CivSettings.localize.localizedString("cmd_build_listWondersHeader"));
		Town town = getSelectedTown();
		for (ConfigBuildableInfo sinfo : CivSettings.wonders.values()) {
			if (sinfo.isAvailable(town)) {
				String leftString = "";
				if (sinfo.limit == 0) {
					leftString = CivSettings.localize.localizedString("Unlimited");
				} else {
					leftString = "" + (sinfo.limit - (town.BM.getBuildableByIdCount(sinfo.id)));
				}

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
	}

	public void list_cmd() throws CivException {
		this.list_available_structures();
		this.list_available_wonders();
	}

	@Override
	public void doDefaultAction() throws CivException {
		if (this.args.length == 0) {
			this.showHelp();
			GuiInventory.openGuiInventory(getPlayer(), "Structure", "false");
			return;
		}
		String fullArgs = "";
		for (String arg : args) {
			fullArgs += arg + " ";
		}
		fullArgs = fullArgs.trim();

		buildByName(fullArgs);
	}

	private void buildByName(String fullArgs) throws CivException {
		ConfigBuildableInfo sinfo = CivSettings.getBuildableInfoByName(fullArgs);
		if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + fullArgs);
		getResident().setPendingCallback(new BuildCallback(getPlayer(), sinfo, getSelectedTown()));
	}

	@Override
	public void showHelp() {
		showBasicHelp();
		CivMessage.send(sender, CivColor.LightPurple + command + " " + CivColor.Yellow + CivSettings.localize.localizedString("cmd_build_help1") + " " + CivColor.LightGray + CivSettings.localize.localizedString("cmd_build_help2"));
	}

	@Override
	public void permissionCheck() throws CivException {
		validMayorAssistantLeader();
	}

}
