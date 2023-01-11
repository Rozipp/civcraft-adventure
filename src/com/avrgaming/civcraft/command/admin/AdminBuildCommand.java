/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Buildable;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.wonders.Wonder;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class AdminBuildCommand extends MenuAbstractCommand {

	public AdminBuildCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_build_Name");
		add(new CustomCommand("unbuild").withDescription(CivSettings.localize.localizedString("adcmd_build_unbuildDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("adcmd_build_unbuildPrompt"));
				Town town = Commander.getNamedTown(args, 0);
				if (args.length < 2) {
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_build_unbuildHeading"));
					for (Structure struct : town.BM.getStructures()) {
						CivMessage.send(sender,
								struct.getDisplayName() + ": " + CivColor.Yellow + struct.getId() + CivColor.White + " - " + CivSettings.localize.localizedString("Location") + " " + CivColor.Yellow + struct.getCorner().toString());
					}
					return;
				}
				String id = args[1];
				Connection context = null;
				ResultSet rs = null;
				PreparedStatement ps = null;
				Structure struct = null;
				try {
					context = SQL.getGameConnection();
					ps = context.prepareStatement("SELECT * FROM " + SQL.tb_prefix + Buildable.TABLE_NAME + " WHERE id = " + id);
					rs = ps.executeQuery();
					while (rs.next()) {
						try {
							struct = CivGlobal.getStructure(Buildable.newBuildable(rs).getCorner());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				} finally {
					SQL.close(rs, ps, context);
				}
				if (struct == null) {
					CivMessage.send(sender, CivColor.Rose + CivSettings.localize.localizedString("NoStructureAt") + " " + args[1]);
					return;
				}
				struct.getTownOwner().BM.demolish(struct, true);
				CivMessage.sendTown(struct.getTownOwner(), struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
			}
		}));
		add(new CustomCommand("demolish").withDescription(CivSettings.localize.localizedString("adcmd_build_demolishDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("adcmd_build_demolishPrompt"));
				Town town = Commander.getNamedTown(args, 0);
				if (args.length < 2) {
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_build_unbuildHeading"));
					for (Structure struct : town.BM.getStructures()) {
						CivMessage.send(sender, CivSettings.localize.localizedString("var_cmd_build_demolish", struct.getDisplayName(), CivColor.Yellow + struct.getCorner().toString() + CivColor.White));
					}
					return;
				}
				BlockCoord coord = new BlockCoord(args[1]);
				Structure struct = town.BM.getStructure(coord);
				if (struct == null) {
					CivMessage.send(sender, CivColor.Rose + CivSettings.localize.localizedString("NoStructureAt") + " " + args[2]);
					return;
				}
				struct.getTownOwner().BM.demolish(struct, true);
				CivMessage.sendTown(struct.getTownOwner(), struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_demolishComplete"));
			}
		}));
		add(new CustomCommand("repairnearest").withDescription(CivSettings.localize.localizedString("adcmd_build_repairDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Buildable nearest = CivGlobal.getNearestBuildable(player.getLocation());
				if (nearest == null) throw new CivException(CivSettings.localize.localizedString("adcmd_build_StructNotFound"));
				if (args.length < 1 || !args[0].equalsIgnoreCase("yes")) {
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_adcmd_build_repairConfirmPrompt", CivColor.Yellow + nearest.getDisplayName(), nearest.getCorner()));
					CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("adcmd_build_toConfirm"));
					return;
				}
				nearest.repairFromTemplate();
				CivMessage.sendSuccess(player, nearest.getDisplayName() + " " + CivSettings.localize.localizedString("Repaired"));
			}
		}));
		add(new CustomCommand("destroywonder").withDescription(CivSettings.localize.localizedString("adcmd_build_destroyWonderDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				if (args.length < 2) throw new CivException(CivSettings.localize.localizedString("adcmd_build_enterWonderID"));
				Wonder wonder = town.BM.getWonderByName(args[1]);
				if (wonder == null) throw new CivException(CivSettings.localize.localizedString("adcmd_build_wonderDoesNotExist") + " " + args[1]);
				wonder.deleteWithFancy();
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_build_destroyed"));
			}
		}));
		add(new CustomCommand("destroynearest").withDescription(CivSettings.localize.localizedString("adcmd_build_destroyNearestDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Player player = Commander.getPlayer(sender);
				Buildable struct = town.BM.getNearestStrucutreOrWonder(player.getLocation());
				if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
					CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_adcmd_build_wouldDestroy", struct.getDisplayName(), struct.getCorner()));
					return;
				}
				struct.onDestroy();
				CivMessage.send(player, struct.getDisplayName() + " " + CivSettings.localize.localizedString("adcmd_build_destroyed"));
			}
		}));
		add(new CustomCommand("validatenearest").withDescription(CivSettings.localize.localizedString("adcmd_build_valideateNearestDesc")).withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Town town = Commander.getNamedTown(args, 0);
				Buildable buildable = town.BM.getNearestBuildable(player.getLocation());
				if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
					CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_adcmd_build_wouldValidate", buildable.getDisplayName(), buildable.getCorner()));
					return;
				}
				buildable.validateAsyncTask(player);
			}
		}));
//		add(new CustomCommand("changenearest").withDescription(CivSettings.localize.localizedString("adcmd_build_changeNearestDesc")).withExecutor(new CustonExecutor() {
//			@Override
//			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
//				Player player = Commander.getPlayer(sender);
//				Town town = Commander.getNamedTown(args, 0);
//				Buildable buildable = town.BM.getNearestBuildable(player.getLocation());
//				if (args.length < 2) {
//					CivMessage.send(player, CivColor.Red + ChatColor.BOLD + CivSettings.localize.localizedString("adcmd_build_wouldChangeTheme_NoTheme"));
//					return;
//				}
//				if (args.length < 3 || !args[2].equalsIgnoreCase("yes")) {
//					CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("var_adcmd_build_wouldChangeTheme", buildable.getDisplayName(), buildable.getCorner()));
//					return;
//				}
//			}
//		}));
		add(new CustomCommand("validateall").withDescription(CivSettings.localize.localizedString("adcmd_build_validateAllDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				for (Structure struct : CivGlobal.getStructures()) {
					if (struct.isStrategic()) struct.validateAsyncTask(null);
				}
				for (Wonder wonder : CivGlobal.getWonders()) {
					if (wonder.isStrategic()) wonder.validateAsyncTask(null);
				}
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("adcmd_build_validateAll"));
			}
		}));

	}

	@Override
	public void doDefaultAction(CommandSender sender) throws CivException {
		showBasicHelp(sender);
	}

}
