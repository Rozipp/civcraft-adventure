package com.avrgaming.civcraft.comm;

import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.comm.Resident.ResidentCommand;
import com.avrgaming.civcraft.command.AcceptCommand;
import com.avrgaming.civcraft.command.BuildCommand;
import com.avrgaming.civcraft.command.CampChatCommand;
import com.avrgaming.civcraft.command.DenyCommand;
import com.avrgaming.civcraft.command.EconCommand;
import com.avrgaming.civcraft.command.EnderChestCommand;
import com.avrgaming.civcraft.command.HereCommand;
import com.avrgaming.civcraft.command.KillCommand;
import com.avrgaming.civcraft.command.MapCommand;
import com.avrgaming.civcraft.command.PayCommand;
import com.avrgaming.civcraft.command.ReportCommand;
import com.avrgaming.civcraft.command.SelectCommand;
import com.avrgaming.civcraft.command.TradeCommand;
import com.avrgaming.civcraft.command.WikiCommand;
import com.avrgaming.civcraft.command.admin.AdminCommand;
import com.avrgaming.civcraft.command.camp.CampCommand;
import com.avrgaming.civcraft.command.civ.CivChatCommand;
import com.avrgaming.civcraft.command.civ.CivCommand;
import com.avrgaming.civcraft.command.debug.DebugCommand;
import com.avrgaming.civcraft.command.market.MarketCommand;
import com.avrgaming.civcraft.command.plot.PlotCommand;
import com.avrgaming.civcraft.command.town.TownChatCommand;
import com.avrgaming.civcraft.command.town.TownCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.donate.Donate;
import com.github.goblom.bukkitlibraries.command.CommandRegistrationFactory;

public class Commander {

	private static void addMenu(String string_cmd, CommandExecutor comm) {
		CivCraft.getPlugin().getCommand(string_cmd).setExecutor(comm);
	}

	public static void initCommands() {
		// Init commands
		addMenu("town", new TownCommand());
		// addMenu("resident", new ResidentCommand());
		CommanderRegistration.register(new ResidentCommand());
		addMenu("dbg", new DebugCommand());
		addMenu("plot", new PlotCommand());
		addMenu("accept", new AcceptCommand());
		addMenu("deny", new DenyCommand());
		addMenu("civ", new CivCommand());
		addMenu("tc", new TownChatCommand());
		addMenu("cc", new CivChatCommand());
		// addMenu("gc", new GlobalChatCommand());
		addMenu("ad", new AdminCommand());
		addMenu("econ", new EconCommand());
		addMenu("pay", new PayCommand());
		addMenu("build", new BuildCommand());
		addMenu("market", new MarketCommand());
		addMenu("select", new SelectCommand());
		addMenu("here", new HereCommand());
		addMenu("camp", new CampCommand());
		addMenu("report", new ReportCommand());
		addMenu("trade", new TradeCommand());
		addMenu("kill", new KillCommand());
		addMenu("enderchest", new EnderChestCommand());
		addMenu("map", new MapCommand());
		addMenu("wiki", new WikiCommand());
		addMenu("vcc", new CampChatCommand());
		addMenu("donate", new Donate());

		/******************************** Lets write a command Values: Command: /heal Aliases: /h Usage: /<command> <player> [amount] Permission:
		 * heal.use Permission Message: You do not have permission do /heal */
		CommandRegistrationFactory healCommand = CommandRegistrationFactory.buildCommand("resident");
		healCommand.withAliases("res");
		healCommand.withDescription("Resident menu");
		healCommand.withUsage("/<command> <submenu> ...");
		healCommand.withPermission("civ.resident");
		healCommand.withPermissionMessage("You do not have permission do do /resident");
		healCommand.withCommandExecutor(new CommandExecutor() {
			public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
				if (!(sender instanceof Player)) return true;
				if (args.length >= 1) {
					Player toHeal = Bukkit.getPlayer(args[0]);
					if (toHeal != null) {
						if (args.length >= 2) {
							try {
								toHeal.setHealth(Double.valueOf(args[1]));
								sender.sendMessage("You have healed " + toHeal.getName());
								toHeal.sendMessage("You have been healed!");
							} catch (NumberFormatException e) {
								sender.sendMessage("Health must be a number!");
							}
						} else {
							toHeal.setHealth(20.0D);
							sender.sendMessage("You have healed " + toHeal.getName());
							toHeal.sendMessage("You have been healed!");
						}
					} else
						sender.sendMessage("That player is not online!");
				} else {
					((Player) sender).setHealth(20.0D);
					sender.sendMessage("You have been healed!");
				}
				return true;
			}
		});
		healCommand.build();
	}

	
	// ----------------- arg utils

	public static String convertCommand(String comm) {
		String rus = "фисвуапршолдьтщзйкыегмцчня";
		String eng = "abcdefghijklmnopqrstuvwxyz";

		String res = comm;
		for (int i = 0; i < rus.length(); i++)
			res = res.replace(rus.charAt(i), eng.charAt(i));
		return res;
	}

	public static String[] stripArgs(String[] someArgs, int amount) {
		if (amount >= someArgs.length) return new String[0];
		String[] argsLeft = new String[someArgs.length - amount];
		for (int i = 0; i < argsLeft.length; i++) {
			argsLeft[i] = someArgs[i + amount];
		}
		return argsLeft;
	}

	public static String combineArgs(String[] someArgs) {
		String combined = "";
		for (String str : someArgs) {
			combined += str + " ";
		}
		return combined.trim();
	}

	public static String makeInfoString(HashMap<String, String> kvs, String lowColor, String highColor) {
		String out = "";
		for (String key : kvs.keySet()) {
			out += lowColor + key + ": " + highColor + kvs.get(key) + " ";
		}
		return out;
	}

	// -------------- CommandSender

	public static Player getPlayer(CommandSender sender) throws CivException {
		if (sender instanceof Player) return (Player) sender;
		throw new CivException(CivSettings.localize.localizedString("cmd_MustBePlayer"));
	}

	public static Resident getResident(CommandSender sender) throws CivException {
		Player player = getPlayer(sender);
		Resident res = CivGlobal.getResident(player);
		if (res == null) throw new CivException(CivSettings.localize.localizedString("var_Resident_CouldNotBeFound", player.getName()));
		return res;
	}

	public static Town getSelectedTown(CommandSender sender) throws CivException {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			Resident res = CivGlobal.getResident(player);
			if (res != null && res.getTown() != null) {
				if (res.getSelectedTown() != null) {
					try {
						res.getSelectedTown().validateResidentSelect(res);
					} catch (CivException e) {
						CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("var_cmd_townDeselectedInvalid", res.getSelectedTown().getName(), res.getTown().getName()));
						res.setSelectedTown(res.getTown());
						return res.getTown();
					}
					return res.getSelectedTown();
				} else
					return res.getTown();
			}
		}
		throw new CivException(CivSettings.localize.localizedString("cmd_notPartOfTown"));
	}

	public static Civilization getSenderCiv(CommandSender sender) throws CivException {
		Resident resident = getResident(sender);
		if (resident.getTown() == null) throw new CivException(CivSettings.localize.localizedString("cmd_getSenderCivNoCiv"));
		if (resident.getTown().getCiv() == null) {
			// This should never happen but....
			throw new CivException(CivSettings.localize.localizedString("cmd_getSenderCivNoCiv"));
		}
		return resident.getTown().getCiv();
	}

	public static Camp getCurrentCamp(CommandSender sender) throws CivException {
		Resident resident = getResident(sender);
		if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		return resident.getCamp();
	}

	public static TownChunk getStandingTownChunk(CommandSender sender) throws CivException {
		Player player = getPlayer(sender);
		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc == null) throw new CivException(CivSettings.localize.localizedString("cmd_plotNotOwned"));
		return tc;
	}

	// -------------- valid

	public static void validMayor(CommandSender sender) throws CivException {
		Resident resident = getResident(sender);
		Town town = getSelectedTown(sender);
		if (!town.GM.isMayor(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_MustBeMayor"));
	}

	public static void validMayorAssistantLeader(CommandSender sender) throws CivException {
		Resident resident = getResident(sender);
		Town town = getSelectedTown(sender);
		Civilization civ;

		/* If we're using a selected town that isn't ours validate based on the mother civ. */
		if (town.getMotherCiv() != null)
			civ = town.getMotherCiv();
		else
			civ = getSenderCiv(sender);

		if (!town.GM.isMayorOrAssistant(resident) && !civ.GM.isLeader(resident)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherTownOrCivRank"));
	}

	public static void validLeaderAdvisor(CommandSender sender) throws CivException {
		Resident res = getResident(sender);
		Civilization civ = getSenderCiv(sender);
		if (!civ.GM.isLeaderOrAdviser(res)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank"));
	}

	public static void validLeader(CommandSender sender) throws CivException {
		Resident res = getResident(sender);
		Civilization civ = getSenderCiv(sender);
		if (!civ.GM.isLeader(res)) throw new CivException(CivSettings.localize.localizedString("cmd_NeedHigherCivRank2"));
	}

	public static void validPlotOwner(CommandSender sender) throws CivException {
		Resident resident = getResident(sender);
		TownChunk tc = getStandingTownChunk(sender);

		if (tc.perms.getOwner() == null) {
			validMayorAssistantLeader(sender);
			if (tc.getTown() != resident.getTown()) throw new CivException(CivSettings.localize.localizedString("cmd_validPlotOwnerFalse"));
		} else {
			if (resident != tc.perms.getOwner()) throw new CivException(CivSettings.localize.localizedString("cmd_validPlotOwnerFalse2"));
		}
	}

	public static void validCampOwner(CommandSender sender) throws CivException {
		Resident resident = getResident(sender);
		if (!resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotIncamp"));
		if (resident.getCamp().getSQLOwner() != resident) throw new CivException(CivSettings.localize.localizedString("cmd_campBase_NotOwner") + " (" + resident.getCamp().getOwnerName() + ")");
	}

	// --------------------- get args
	//
	// protected Double getNamedDouble(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
	// try {
	// return Double.valueOf(args[index]);
	// } catch (NumberFormatException e) {
	// throw new CivException(args[index] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
	// }
	// }
	//
	// protected Integer getNamedInteger(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
	// try {
	// return Integer.valueOf(args[index]);
	// } catch (NumberFormatException e) {
	// throw new CivException(args[index] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
	// }
	// }
	//
	// protected Resident getNamedResident(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterResidentName"));
	// String name = args[index].toLowerCase();
	// name = name.replace("%", "(\\w*)");
	// ArrayList<Resident> potentialMatches = new ArrayList<Resident>();
	// for (Resident resident : CivGlobal.getResidents()) {
	// String str = resident.getName().toLowerCase();
	// try {
	// if (str.matches(name)) potentialMatches.add(resident);
	// } catch (Exception e) {
	// throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
	// }
	// // if (potentialMatches.size() > MATCH_LIMIT) {
	// // throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
	// // }
	// }
	// if (potentialMatches.size() == 0) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
	//
	// if (potentialMatches.size() != 1) {
	// CivMessage.send(sender, CivColor.LightPurple + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
	// CivMessage.send(sender, " ");
	// String out = "";
	// for (Resident resident : potentialMatches) {
	// out += resident.getName() + ", ";
	// }
	//
	// CivMessage.send(sender, CivColor.LightBlue + ChatColor.ITALIC + out);
	// throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
	// }
	// return potentialMatches.get(0);
	// }
	//
	// protected Civilization getNamedCiv(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
	//
	// String name = args[index].toLowerCase();
	// name = name.replace("%", "(\\w*)");
	//
	// ArrayList<Civilization> potentialMatches = new ArrayList<Civilization>();
	// for (Civilization civ : CivGlobal.getCivs()) {
	// String str = civ.getName().toLowerCase();
	// try {
	// if (str.matches(name)) potentialMatches.add(civ);
	// } catch (Exception e) {
	// throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
	// }
	//
	// // if (potentialMatches.size() > MATCH_LIMIT) {
	// // throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
	// // }
	// }
	// if (potentialMatches.size() == 0) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " '" + args[index] +
	// "'");
	// if (potentialMatches.size() != 1) {
	// CivMessage.send(sender, CivColor.LightPurple + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
	// CivMessage.send(sender, " ");
	// String out = "";
	// for (Civilization civ : potentialMatches) {
	// out += civ.getName() + ", ";
	// }
	// CivMessage.send(sender, CivColor.LightBlue + ChatColor.ITALIC + out);
	// throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
	// }
	// return potentialMatches.get(0);
	// }
	//
	// protected Civilization getNamedCapturedCiv(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
	//
	// String name = args[index].toLowerCase();
	// name = name.replace("%", "(\\w*)");
	//
	// ArrayList<Civilization> potentialMatches = new ArrayList<Civilization>();
	// for (Civilization civ : CivGlobal.getConqueredCivs()) {
	// String str = civ.getName().toLowerCase();
	// try {
	// if (str.matches(name)) potentialMatches.add(civ);
	// } catch (Exception e) {
	// throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
	// }
	// // if (potentialMatches.size() > MATCH_LIMIT) {
	// // throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
	// // }
	// }
	//
	// if (potentialMatches.size() == 0) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " '" + args[index] +
	// "'");
	// if (potentialMatches.size() != 1) {
	// CivMessage.send(sender, CivColor.LightPurple + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
	// CivMessage.send(sender, " ");
	// String out = "";
	// for (Civilization civ : potentialMatches) {
	// out += civ.getName() + ", ";
	// }
	// CivMessage.send(sender, CivColor.LightBlue + ChatColor.ITALIC + out);
	// throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
	// }
	// return potentialMatches.get(0);
	// }
	//
	// protected Town getNamedTown(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterTownName"));
	//
	// String name = args[index].toLowerCase();
	// name = name.replace("%", "(\\w*)");
	//
	// ArrayList<Town> potentialMatches = new ArrayList<Town>();
	// for (Town town : CivGlobal.getTowns()) {
	// String str = town.getName().toLowerCase();
	// try {
	// if (str.matches(name)) potentialMatches.add(town);
	// } catch (Exception e) {
	// throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
	// }
	// // if (potentialMatches.size() > MATCH_LIMIT) {
	// // throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
	// // }
	// }
	// if (potentialMatches.size() == 0) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
	// if (potentialMatches.size() != 1) {
	// CivMessage.send(sender, CivColor.LightPurple + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
	// CivMessage.send(sender, " ");
	// String out = "";
	// for (Town town : potentialMatches) {
	// out += town.getName() + ", ";
	// }
	// CivMessage.send(sender, CivColor.LightBlue + ChatColor.ITALIC + out);
	// throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
	// }
	// return potentialMatches.get(0);
	// }
	//
	// public String getNamedString(int index, String message) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(message);
	// return args[index];
	// }
	//
	// @SuppressWarnings("deprecation")
	// protected OfflinePlayer getNamedOfflinePlayer(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterPlayerName"));
	// OfflinePlayer offplayer = Bukkit.getOfflinePlayer(args[index]);
	// if (offplayer == null) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " " + args[index]);
	// return offplayer;
	// }
	//
	// protected PermissionGroup getNamedPermissionGroup(Town town, int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterGroupName"));
	// PermissionGroup grp = town.GM.getGroup(args[index]);
	// if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_NameNoResults", args[index], town.getName()));
	// return grp;
	// }
	//
	// protected Camp getNamedCamp(int index) throws CivException {
	// if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EntercampName"));
	//
	// String name = args[index].toLowerCase();
	// name = name.replace("%", "(\\w*)");
	//
	// ArrayList<Camp> potentialMatches = new ArrayList<Camp>();
	// for (Camp camp : CivGlobal.getCamps()) {
	// String str = camp.getName().toLowerCase();
	// try {
	// if (str.matches(name)) potentialMatches.add(camp);
	// } catch (Exception e) {
	// throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
	// }
	// // if (potentialMatches.size() > MATCH_LIMIT) {
	// // throw new CivException(CivSettings.localize.localizedString("cmd_TooManyResults"));
	// // }
	// }
	// if (potentialMatches.size() == 0) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
	// if (potentialMatches.size() != 1) {
	// CivMessage.send(sender, CivColor.LightPurple + ChatColor.UNDERLINE + CivSettings.localize.localizedString("cmd_NameMoreThan1"));
	// CivMessage.send(sender, " ");
	// String out = "";
	// for (Camp camp : potentialMatches) {
	// out += camp.getName() + ", ";
	// }
	// CivMessage.send(sender, CivColor.LightBlue + ChatColor.ITALIC + out);
	// throw new CivException(CivSettings.localize.localizedString("cmd_NameMoreThan2"));
	// }
	// return potentialMatches.get(0);
	// }

}
