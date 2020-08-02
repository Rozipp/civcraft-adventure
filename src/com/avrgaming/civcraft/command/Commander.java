package com.avrgaming.civcraft.command;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CustomCommand.CustonExecutor;
import com.avrgaming.civcraft.command.menu.BuildableCommand;
import com.avrgaming.civcraft.command.menu.CampCommand;
import com.avrgaming.civcraft.command.menu.CivCommand;
import com.avrgaming.civcraft.command.menu.EconCommand;
import com.avrgaming.civcraft.command.menu.PlotCommand;
import com.avrgaming.civcraft.command.menu.ResidentCommand;
import com.avrgaming.civcraft.command.menu.TownCommand;
import com.avrgaming.civcraft.command.report.DonateCommand;
import com.avrgaming.civcraft.command.report.ReportCommand;
import com.avrgaming.civcraft.commandold.AdminCommand;
import com.avrgaming.civcraft.commandold.DebugCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class Commander {

	private static void addMenu(String string_cmd, CommandExecutor comm) {
		CivCraft.getPlugin().getCommand(string_cmd).setExecutor(comm);
	}

	public static void initCommands() {
		// Init commands
		CommanderRegistration.register(new ResidentCommand("resident"));
		CommanderRegistration.register(new EconCommand("econ"));
		CommanderRegistration.register(new CampCommand("camp"));
		CommanderRegistration.register(new CivCommand("civ"));
		CommanderRegistration.register(new TownCommand("town"));
		CommanderRegistration.register(new BuildableCommand("buildable"));
		CommanderRegistration.register(new PlotCommand("plot"));
		CommanderRegistration.register(new AcceptCommand("accept"));
		CommanderRegistration.register(new DenyCommand("deny"));
		CommanderRegistration.register(new CustomCommand("vcc").withValidator(Validators.validHasCamp).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Camp camp = Commander.getCurrentCamp(sender);
				if (args.length == 0) {
					resident.setCampChat(!resident.isCampChat());
					resident.setCivChat(false);
					resident.setTownChat(false);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_campchat_modeSet") + " " + resident.isCampChat());
					return;
				}
				CivMessage.sendCampChat(camp, resident, "<%s> %s", Commander.combineArgs(args));
			}
		}));
		CommanderRegistration.register(new CustomCommand("cc").withValidator(Validators.validHasTown).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (args.length == 0) {
					resident.setCampChat(false);
					resident.setCivChat(!resident.isCivChat());
					resident.setTownChat(false);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civchat_modeSet") + " " + resident.isCivChat());
					return;
				}
				CivMessage.sendCivChat(resident.getTown().getCiv(), resident, "<%s> %s", Commander.combineArgs(args));
			}
		}));
		CommanderRegistration.register(new CustomCommand("tc").withValidator(Validators.validHasCamp).withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (args.length == 0) {
					resident.setCampChat(false);
					resident.setTownChat(!resident.isTownChat());
					resident.setCivChat(false);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_town_chat_mode") + " " + resident.isTownChat());
					return;
				}
				CivMessage.sendTownChat(resident.getTown(), resident, "<%s> %s", Commander.combineArgs(args));
			}
		}));
		CommanderRegistration.register(new CustomCommand("gc").withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				if (args.length == 0) {
					resident.setCampChat(false);
					resident.setCivChat(false);
					resident.setTownChat(false);
					CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_gc_enabled"));
					return;
				}
				CivMessage.sendChat(resident, "<%s> %s", Commander.combineArgs(args));
			}
		}));
		CommanderRegistration.register(new CustomCommand("pay").withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getResident(sender);
				Resident payTo = Commander.getNamedResident(args, 0);
				if (resident == payTo) throw new CivException(CivSettings.localize.localizedString("cmd_pay_yourself"));
				if (args.length < 1) throw new CivException(CivSettings.localize.localizedString("cmd_pay_prompt"));
				Double amount;
				try {
					amount = Double.valueOf(args[1]);
					if (!resident.getTreasury().hasEnough(amount)) throw new CivException(CivSettings.localize.localizedString("var_cmd_pay_InsufficentFunds", CivSettings.CURRENCY_NAME));
				} catch (NumberFormatException e) {
					throw new CivException(CivSettings.localize.localizedString("EnterNumber"));
				}
				if (amount < 1) throw new CivException(CivSettings.localize.localizedString("cmd_pay_WholeNumbers"));
				amount = Math.floor(amount);
				resident.getTreasury().withdraw(amount);
				payTo.getTreasury().deposit(amount);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_pay_PaidSuccess", amount, CivSettings.CURRENCY_NAME, payTo.getName()));
				CivMessage.sendSuccess(payTo, CivSettings.localize.localizedString("var_cmd_pay_PaidReceiverSuccess", resident.getName(), amount, CivSettings.CURRENCY_NAME));
			}
		}));
		CommanderRegistration.register(new CustomCommand("here").withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ChunkCoord coord = new ChunkCoord(player.getLocation());
				CultureChunk cc = CivGlobal.getCultureChunk(coord);
				if (cc != null)
					CivMessage.send(sender, CivColor.LightPurple + CivSettings.localize.localizedString("var_cmd_here_inCivAndTown", CivColor.Yellow + cc.getCiv().getName() + CivColor.LightPurple, CivColor.Yellow + cc.getTown().getName()));

				TownChunk tc = CivGlobal.getTownChunk(coord);
				if (tc != null) CivMessage.send(sender, CivColor.Green + CivSettings.localize.localizedString("var_cmd_here_inTown", CivColor.LightGreen + tc.getTown().getName()));
				if (cc == null && tc == null) CivMessage.send(sender, CivColor.Yellow + CivSettings.localize.localizedString("cmd_here_wilderness"));
			}
		}));
		CommanderRegistration.register(new TradeCommand("trade"));
		CommanderRegistration.register(new CustomCommand("kill").withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Commander.getPlayer(sender).setHealth(0);
				CivMessage.send(sender, CivColor.Yellow + CivColor.BOLD + CivSettings.localize.localizedString("cmd_kill_Mesage"));
			}
		}));
		CommanderRegistration.register(new CustomCommand("enderchest").withAliases("echest", "eechest", "eenderchest", "endersee", "eendersee", "ec", "eec").withExecutor(new CustonExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (!sender.hasPermission("civcraft.enderchest") && !sender.isOp() && !sender.hasPermission("civcraft.ec") && !PermissionGroup.hasGroup(sender.getName(), "ultra") && !PermissionGroup.hasGroup(sender.getName(), "deluxe"))
					throw new CivException("§c" + CivSettings.localize.localizedString("cmd_enderchest_NoPermissions"));
				Player player = Commander.getPlayer(sender);
				if (!player.getWorld().equals(CivCraft.mainWorld)) throw new CivException("§c" + CivSettings.localize.localizedString("cmd_enderchest_inArena"));
				player.openInventory(player.getEnderChest());
			}
		}));
		// CommanderRegistration.register(new CustomCommand("map").withExecutor(new CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// if (args.length < 1)
		// CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_map_mapLink", "http://95.216.74.3:5551"));
		// else {
		// String combineArgs = Commander.combineArgs(args);
		// CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_map_mapLinkAddon", "http://95.216.74.3:5551" +
		// combineArgs.toString().replace(" ", "_"), combineArgs));
		// }
		// }
		// }));
		// CommanderRegistration.register(new CustomCommand("wiki").withExecutor(new CustonExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// if (args.length < 1)
		// CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_wiki_wikiLink",
		// "https://wiki.furnex.ru/index.php?title=Введение"));
		// else {
		// String combineArgs = Commander.combineArgs(args);
		// CivMessage.send(sender, "§a" + CivSettings.localize.localizedString("cmd_wiki_wikiLinkAddon",
		// "https://wiki.furnex.ru/index.php?title=Введение" + combineArgs.toString().replace(" ", "_"), combineArgs.toString()));
		// }
		// }
		// }));
		addMenu("ad", new AdminCommand());
		addMenu("dbg", new DebugCommand());
		addMenu("report", new ReportCommand());
		addMenu("donate", new DonateCommand());
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
