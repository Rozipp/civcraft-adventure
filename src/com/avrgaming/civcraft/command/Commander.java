package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.admin.AdminCommand;
import com.avrgaming.civcraft.command.debug.DebugCommand;
import com.avrgaming.civcraft.command.menu.BuildCommand;
import com.avrgaming.civcraft.command.menu.CampCommand;
import com.avrgaming.civcraft.command.menu.CivCommand;
import com.avrgaming.civcraft.command.menu.EconCommand;
import com.avrgaming.civcraft.command.menu.PlotCommand;
import com.avrgaming.civcraft.command.menu.ResidentCommand;
import com.avrgaming.civcraft.command.menu.TownCommand;
import com.avrgaming.civcraft.command.newcommands.AcceptCommand;
import com.avrgaming.civcraft.command.newcommands.DenyCommand;
import com.avrgaming.civcraft.command.oldcommands.CampChatCommand;
import com.avrgaming.civcraft.command.oldcommands.CivChatCommand;
import com.avrgaming.civcraft.command.oldcommands.EnderChestCommand;
import com.avrgaming.civcraft.command.oldcommands.HereCommand;
import com.avrgaming.civcraft.command.oldcommands.KillCommand;
import com.avrgaming.civcraft.command.oldcommands.MapCommand;
import com.avrgaming.civcraft.command.oldcommands.PayCommand;
import com.avrgaming.civcraft.command.oldcommands.ReportCommand;
import com.avrgaming.civcraft.command.oldcommands.SelectCommand;
import com.avrgaming.civcraft.command.oldcommands.TownChatCommand;
import com.avrgaming.civcraft.command.oldcommands.TradeCommand;
import com.avrgaming.civcraft.command.oldcommands.WikiCommand;
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
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.donate.Donate;

public class Commander {

	private static void addMenu(String string_cmd, CommandExecutor comm) {
		CivCraft.getPlugin().getCommand(string_cmd).setExecutor(comm);
	}

	public static void initCommands() {
		// Init commands
		CommanderRegistration.register(new ResidentCommand());
		CommanderRegistration.register(new EconCommand());
		CommanderRegistration.register(new CampCommand());
		CommanderRegistration.register(new CivCommand());
//		CommanderRegistration.register(new CivMarketCommand());
		CommanderRegistration.register(new TownCommand());
		CommanderRegistration.register(new BuildCommand());
		CommanderRegistration.register(new PlotCommand());
		CommanderRegistration.register(new AcceptCommand());
		CommanderRegistration.register(new DenyCommand());
		addMenu("ad", new AdminCommand());
		addMenu("dbg", new DebugCommand());
		addMenu("vcc", new CampChatCommand());
		addMenu("cc", new CivChatCommand());
		addMenu("tc", new TownChatCommand());
		// addMenu("gc", new GlobalChatCommand());
		addMenu("pay", new PayCommand());
		addMenu("select", new SelectCommand());
		addMenu("here", new HereCommand());
		addMenu("report", new ReportCommand());
		addMenu("trade", new TradeCommand());
		addMenu("kill", new KillCommand());
		addMenu("enderchest", new EnderChestCommand());
		addMenu("map", new MapCommand());
		addMenu("wiki", new WikiCommand());
		addMenu("donate", new Donate());
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

	// --------------------- get args

	public static Integer getNamedInteger(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
		try {
			return Integer.valueOf(args[index]);
		} catch (NumberFormatException e) {
			throw new CivException(args[index] + " " + CivSettings.localize.localizedString("cmd_enterNumerError2"));
		}
	}

	public static Double getNamedDouble(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("cmd_enterNumber"));
		try {
			return Double.valueOf(args[index]);
		} catch (NumberFormatException e) {
			throw new CivException(args[index] + " " + CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
	}

	public static String getNamedString(String[] args, int index, String message) throws CivException {
		if (args.length < (index + 1)) throw new CivException(message);
		return args[index];
	}

	public static Resident getNamedResident(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterResidentName"));
		String name = args[index].toLowerCase().replace("%", "(\\w*)");
		for (Resident resident : CivGlobal.getResidents()) {
			String str = resident.getName().toLowerCase();
			try {
				if (str.equalsIgnoreCase(name)) return resident;
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
	}

	public static Civilization getNamedCiv(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
		String name = args[index].toLowerCase().replace("%", "(\\w*)");
		for (Civilization civ : CivGlobal.getCivs()) {
			String str = civ.getName().toLowerCase();
			try {
				if (str.equalsIgnoreCase(name)) return civ;
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " '" + args[index] + "'");
	}

	public static Town getNamedTown(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterTownName"));
		String name = args[index].toLowerCase().replace("%", "(\\w*)");
		for (Town town : CivGlobal.getTowns()) {
			String str = town.getName().toLowerCase();
			try {
				if (str.equalsIgnoreCase(name)) return town;
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
	}


	public static PermissionGroup getNamedPermissionGroup(Town town, String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterGroupName"));
		PermissionGroup grp = town.GM.getGroup(args[index]);
		if (grp == null) throw new CivException(CivSettings.localize.localizedString("var_cmd_NameNoResults", args[index], town.getName()));
		return grp;
	}

	public static Camp getNamedCamp(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EntercampName"));
		String name = args[index].toLowerCase().replace("%", "(\\w*)");
		for (Camp camp : CivGlobal.getCamps()) {
			String str = camp.getName().toLowerCase();
			try {
				if (str.equalsIgnoreCase(name)) return camp;
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults"));
	}

	@SuppressWarnings("deprecation")
	public static OfflinePlayer getNamedOfflinePlayer(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterPlayerName"));
		OfflinePlayer offplayer = Bukkit.getOfflinePlayer(args[index]);
		if (offplayer == null) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " " + args[index]);
		return offplayer;
	}

	/** @deprecated */
	public static Civilization getNamedCapturedCiv(String[] args, int index) throws CivException {
		if (args.length < (index + 1)) throw new CivException(CivSettings.localize.localizedString("EnterCivName"));
		String name = args[index].toLowerCase().replace("%", "(\\w*)");
		ArrayList<Civilization> potentialMatches = new ArrayList<Civilization>();
		for (Civilization civ : CivGlobal.getConqueredCivs()) {
			String str = civ.getName().toLowerCase();
			try {
				if (str.matches(name)) potentialMatches.add(civ);
			} catch (Exception e) {
				throw new CivException(CivSettings.localize.localizedString("cmd_invalidPattern"));
			}
		}
		if (potentialMatches.size() == 0) throw new CivException(CivSettings.localize.localizedString("cmd_NameNoResults") + " '" + args[index] + "'");
		return potentialMatches.get(0);
	}
}
