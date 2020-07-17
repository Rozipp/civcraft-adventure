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
package com.avrgaming.civcraft.main;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class CivMessage {

	/* Stores the player name and the hash code of the last message sent to prevent error spamming the player. */
	private static HashMap<String, Integer> lastMessageHashCode = new HashMap<String, Integer>();

	/* Indexed off of town names, contains a list of extra people who listen to town chats.(mostly for admins to listen to towns) */
	private static Map<Integer, ArrayList<Resident>> extraTownChatListeners = new ConcurrentHashMap<>();

	/* Indexed off of civ names, contains a list of extra people who listen to civ chats. (mostly for admins to list to civs) */
	private static Map<Integer, ArrayList<Resident>> extraCivChatListeners = new ConcurrentHashMap<>();

	private static Map<String, ArrayList<Resident>> extraCampChatListeners = new ConcurrentHashMap<>();

	public static void sendErrorNoRepeat(Object sender, String line) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			Integer hashcode = lastMessageHashCode.get(player.getName());
			if (hashcode != null && hashcode == line.hashCode()) {
				return;
			}

			lastMessageHashCode.put(player.getName(), line.hashCode());
		}

		send(sender, CivColor.Rose + line);
	}

	public static void sendError(Object sender, String line) {
		send(sender, CivColor.Rose + line);
	}

	/* Sends message to playerName(if online) AND console. */
	public static void console(String playerName, String line) {
		try {
			Player player = CivGlobal.getPlayer(playerName);
			send(player, line);
		} catch (CivException e) {}
		CivLog.info(line);
	}

	public static void sendTitle(Object sender, int fadeIn, int show, int fadeOut, String title, String subTitle) {
		send(sender, title);
		if (subTitle != "") {
			send(sender, subTitle);
		}
	}

	public static void sendTitle(Object sender, String title, String subTitle) {
		sendTitle(sender, 10, 40, 5, title, subTitle);
	}

	public static void send(Object sender, String line) {
		if ((sender instanceof Player)) {
			((Player) sender).sendMessage(line);
		} else
			if (sender instanceof CommandSender) {
				((CommandSender) sender).sendMessage(line);
			} else
				if (sender instanceof Resident) {
					try {
						CivGlobal.getPlayer(((Resident) sender)).sendMessage(line);
					} catch (CivException e) {
						// No player online
					}
				}
	}

	// public static void send(Object sender, String line, ItemStack item) {
	// if ((sender instanceof Player)) {
	// Player p = (Player) sender;
	// TextComponent msg = new TextComponent( line );
	// msg.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(itemTooltip(item)).create() ) );
	//
	// p.sendMessage(String.valueOf(msg));
	// } else if (sender instanceof CommandSender) {
	//
	// ((CommandSender) sender).sendMessage(line);
	// }
	// else if (sender instanceof Resident) {
	// try {
	// Player p = CivGlobal.getPlayer(((Resident) sender));
	// TextComponent msg = new TextComponent( line );
	// msg.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(itemTooltip(item)).create() ) );
	//
	// p.sendMessage(String.valueOf(msg));
	// } catch (CivException e) {
	// // No player online
	// }
	// }
	// }
	public static void send(Object sender, String[] lines) {
		boolean isPlayer = false;
		if (sender instanceof Player) isPlayer = true;

		for (String line : lines) {
			if (isPlayer) {
				((Player) sender).sendMessage(line);
			} else {
				((CommandSender) sender).sendMessage(line);
			}
		}
	}

	public static String buildTitle(String title) {
		String line = "-------------------------------------------------";
		String titleBracket = "[ " + CivColor.Yellow + title + CivColor.LightBlue + " ]";

		if (titleBracket.length() > line.length()) {
			return CivColor.LightBlue + "-" + titleBracket + "-";
		}

		int min = (line.length() / 2) - titleBracket.length() / 2;
		int max = (line.length() / 2) + titleBracket.length() / 2;

		String out = CivColor.LightBlue + line.substring(0, Math.max(0, min));
		out += titleBracket + line.substring(max);

		return out;
	}

	public static String buildSmallTitle(String title) {
		String line = CivColor.LightBlue + "------------------------------";

		String titleBracket = "[ " + title + " ]";

		int min = (line.length() / 2) - titleBracket.length() / 2;
		int max = (line.length() / 2) + titleBracket.length() / 2;

		String out = CivColor.LightBlue + line.substring(0, Math.max(0, min));
		out += titleBracket + line.substring(max);

		return out;
	}

	public static void sendSubHeading(CommandSender sender, String title) {
		send(sender, buildSmallTitle(title));
	}

	public static void sendHeading(Resident resident, String title) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
			sendHeading(player, title);
		} catch (CivException e) {}
	}

	public static void sendHeading(CommandSender sender, String title) {
		send(sender, buildTitle(title));
	}

	public static void sendSuccess(CommandSender sender, String message) {
		send(sender, CivColor.LightGreen + message);
	}

	public static void global(String string) {
		CivLog.info("[Global] " + string);
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(CivColor.LightBlue + CivSettings.localize.localizedString("civMsg_Globalprefix") + " " + CivColor.White + string);
		}
	}

	public static void globalTitle(String title, String subTitle) {
		CivLog.info("[GlobalTitle] " + title + " - " + subTitle);
		for (Player player : Bukkit.getOnlinePlayers()) {
			send(player, buildTitle(title));
			if (!subTitle.equals("")) send(player, subTitle);
		}
	}

	public static void globalHeading(String string) {
		CivLog.info("[GlobalHeading] " + string);
		for (Player player : Bukkit.getOnlinePlayers()) {
			send(player, buildTitle(string));
		}
	}

	public static void sendScout(Civilization civ, String string) {
		CivLog.info("[Scout:" + civ.getName() + "] " + string);
		for (Town t : civ.getTowns()) {
			for (Resident resident : t.getResidents()) {
				if (!resident.isShowScout()) {
					continue;
				}

				Player player;
				try {
					player = CivGlobal.getPlayer(resident);
					if (player != null) {
						CivMessage.send(player, CivColor.Purple + CivSettings.localize.localizedString("civMsg_ScoutPrefix") + " " + CivColor.White + string);
					}
				} catch (CivException e) {}
			}

		}
	}

	public static void sendTown(Town town, String string) {
		CivLog.info("[Town:" + town.getName() + "] " + string);

		for (Resident resident : town.getResidents()) {
			if (!resident.isShowTown()) {
				continue;
			}

			Player player;
			try {
				player = CivGlobal.getPlayer(resident);
				if (player != null) {
					CivMessage.send(player, CivColor.Gold + CivSettings.localize.localizedString("civMsg_Townprefix") + " " + CivColor.White + string);
				}
			} catch (CivException e) {}
		}
	}

	public static void sendCiv(Civilization civ, String string) {
		CivLog.info("[Civ:" + civ.getName() + "] " + string);
		for (Town t : civ.getTowns()) {
			for (Resident resident : t.getResidents()) {
				if (!resident.isShowCiv()) {
					continue;
				}

				Player player;
				try {
					player = CivGlobal.getPlayer(resident);
					if (player != null) {
						CivMessage.send(player, CivColor.LightPurple + CivSettings.localize.localizedString("civMsg_Civprefix") + " " + CivColor.White + string);
					}
				} catch (CivException e) {}
			}

		}
	}

	public static void send(CommandSender sender, List<String> outs) {
		for (String str : outs) {
			send(sender, str);
		}
	}

	public static void sendTownChat(Town town, Resident resident, String format, String message) {
		if (town == null) {
			try {
				Player player = CivGlobal.getPlayer(resident);
				player.sendMessage(CivColor.Rose + CivSettings.localize.localizedString("civMsg_tcNotInTown"));

			} catch (CivException e) {}
			return;
		}

		CivLog.info("[TC:" + town.getName() + "] " + resident.getName() + ": " + message);

		for (Resident r : town.getResidents()) {
			try {
				Player player = CivGlobal.getPlayer(r);
				String msg = CivColor.LightBlue + CivSettings.localize.localizedString("civMsg_tcPrefix") + CivColor.White + String.format(format, resident.getName(), message);
				player.sendMessage(msg);
			} catch (CivException e) {
				continue; /* player not online. */
			}
		}

		for (Resident res : getExtraTownChatListeners(town)) {
			try {
				Player player = CivGlobal.getPlayer(res);
				String msg = CivColor.LightBlue + CivSettings.localize.localizedString("civMsg_tcPrefix2") + town.getName() + "]" + CivColor.White + String.format(format, resident.getName(), message);
				player.sendMessage(msg);
			} catch (CivException e) {
				/* player not online. */
			}
		}
	}

	public static void sendCivChat(Civilization civ, Resident resident, String format, String message) {
		if (civ == null) {
			try {
				Player player = CivGlobal.getPlayer(resident);
				player.sendMessage(CivColor.Rose + CivSettings.localize.localizedString("civMsg_ccNotInCiv"));
			} catch (CivException e) {}
			return;
		}

		String townName = (resident.getTown() != null) ? resident.getTown().getName() : "";

		for (Town t : civ.getTowns()) {
			for (Resident r : t.getResidents()) {
				try {
					Player player = CivGlobal.getPlayer(r);
					String msg = CivColor.Gold + CivSettings.localize.localizedString("civMsg_ccPrefix1") + " " + townName + "]" + CivColor.White + String.format(format, resident.getName(), message);
					player.sendMessage(msg);
				} catch (CivException e) {
					continue; /* player not online. */
				}
			}
		}

		for (Resident res : getExtraCivChatListeners(civ)) {
			try {
				Player player = CivGlobal.getPlayer(res);
				String msg = CivColor.Gold + CivSettings.localize.localizedString("civMsg_ccPrefix2") + civ.getName() + " " + townName + "]" + CivColor.White + String.format(format, resident.getName(), message);
				player.sendMessage(msg);
			} catch (CivException e) {
				/* player not online. */
			}
		}

		return;
	}

	public static void sendChat(Resident resident, String format, String message) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			String msg = String.format(format, resident.getName(), message);
			player.sendMessage(msg);
		}
	}

	public static void addExtraTownChatListener(Town town, Resident res) {
		ArrayList<Resident> names = extraTownChatListeners.get(town.getId());
		if (names == null) names = new ArrayList<Resident>();
		if (names.contains(res)) return;
		names.add(res);
		extraTownChatListeners.put(town.getId(), names);
	}

	public static void removeExtraTownChatListener(Town town, Resident res) {
		ArrayList<Resident> names = extraTownChatListeners.get(town.getId());
		if (names == null) return;
		names.remove(res);
		extraTownChatListeners.put(town.getId(), names);
	}

	public static ArrayList<Resident> getExtraTownChatListeners(Town town) {
		ArrayList<Resident> names = extraTownChatListeners.get(town.getId());
		if (names == null) return new ArrayList<Resident>();
		return names;
	}

	public static void addExtraCivChatListener(Civilization civ, Resident res) {
		ArrayList<Resident> names = extraCivChatListeners.get(civ.getId());
		if (names == null) names = new ArrayList<Resident>();
		if (names.contains(res)) return;
		names.add(res);
		extraCivChatListeners.put(civ.getId(), names);
	}

	public static void removeExtraCivChatListener(Civilization civ, Resident res) {
		ArrayList<Resident> names = extraCivChatListeners.get(civ.getId());
		if (names == null) return;
		names.remove(res);
		extraCivChatListeners.put(civ.getId(), names);
	}

	public static ArrayList<Resident> getExtraCivChatListeners(Civilization civ) {
		ArrayList<Resident> names = extraCivChatListeners.get(civ.getId());
		if (names == null) return new ArrayList<Resident>();
		return names;
	}

	public static void addExtraCampChatListener(Camp camp, Resident res) {
		ArrayList<Resident> names = extraCampChatListeners.get(camp.getName().toLowerCase());
		if (names == null) names = new ArrayList<Resident>();
		if (names.contains(res)) return;
		names.add(res);
		extraCampChatListeners.put(camp.getName().toLowerCase(), names);
	}

	public static void removeExtraCampChatListener(Camp camp, Resident res) {
		ArrayList<Resident> names = extraCampChatListeners.get(camp.getName().toLowerCase());
		if (names == null) return;
		names.remove(res);
		extraCampChatListeners.put(camp.getName().toLowerCase(), names);
	}

	public static ArrayList<Resident> getExtraCampChatListeners(Camp camp) {
		ArrayList<Resident> names = extraCampChatListeners.get(camp.getName().toLowerCase());
		if (names == null) return new ArrayList<Resident>();
		return names;
	}

	public static void sendTownSound(Town town, Sound sound, float f, float g) {
		for (Resident resident : town.getResidents()) {
			Player player;
			try {
				player = CivGlobal.getPlayer(resident);

				player.playSound(player.getLocation(), sound, f, g);
			} catch (CivException e) {
				// player not online.
			}
		}

	}

	public static void sendAll(String str) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(str);
		}
	}

	public static void sendCamp(Camp camp, String message) {
		for (Resident resident : camp.getMembers()) {
			try {
				Player player = CivGlobal.getPlayer(resident);
				player.sendMessage(CivColor.Yellow + "[Camp] " + CivColor.Yellow + message);
				CivLog.info("[Camp:" + camp.getName() + "] " + message);

			} catch (CivException e) {
				// player not online.
			}
		}
	}

	public static void sendTownHeading(Town town, String string) {
		CivLog.info("[Town:" + town.getName() + "] " + string);
		for (Resident resident : town.getResidents()) {
			if (!resident.isShowTown()) {
				continue;
			}

			Player player;
			try {
				player = CivGlobal.getPlayer(resident);
				if (player != null) {
					CivMessage.sendHeading(player, string);
				}
			} catch (CivException e) {}
		}
	}

	public static void sendSuccess(Resident resident, String message) {
		try {
			Player player = CivGlobal.getPlayer(resident);
			sendSuccess(player, message);
		} catch (CivException e) {
			return;
		}
	}

	public static void sendTechError(final Civilization civ, final String string) {
		CivLog.info("[Civilization" + civ.getName() + "][technology learning ERROR] " + string);
		for (final Town t : civ.getTowns()) {
			for (final Resident resident : t.getResidents()) {
				if (!resident.isShowCiv()) {
					continue;
				}
				try {
					final Player player = CivGlobal.getPlayer(resident);
					if (player == null) {
						continue;
					}
					send(player, "§d[Civilization] " + CivColor.RoseItalic + "[technology learning ERROR] " + "§f" + string);
				} catch (CivException ex) {}
			}
		}
	}

	public static void sendCampChat(final Camp camp, final Resident resident, final String format, final String message) {
		if (camp == null) {
			try {
				final Player player = CivGlobal.getPlayer(resident);
				player.sendMessage("§c" + CivSettings.localize.localizedString("campMsg_ccNotIncamp"));
			} catch (CivException ex) {}
			return;
		}
		for (final Resident resident2 : camp.getMembers()) {
			try {
				final Player player2 = CivGlobal.getPlayer(resident2);
				final String msg = "§6" + CivSettings.localize.localizedString("campMsg_ccPrefix1") + " " + resident.getCamp().getName() + "]" + "§f" + String.format(format, resident.getName(), message);
				player2.sendMessage(msg);
			} catch (CivException ex2) {}
		}
		for (final Resident res : getExtraCampChatListeners(camp)) {
			try {
				final Player player2 = CivGlobal.getPlayer(res);
				final String msg = "§6" + CivSettings.localize.localizedString("camMsg_ccPrefix2") + camp.getName() + " " + resident.getCamp().getName() + "]" + "§f" + String.format(format, resident.getName(), message);
				player2.sendMessage(msg);
			} catch (CivException ex3) {}
		}
	}

	public static String pasteStackTrace(Player cause, Exception e) {
		CivGlobal.dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
		StringBuilder permissions = new StringBuilder("Пермы: ");
		Iterator<PermissionAttachmentInfo> var3 = cause.getEffectivePermissions().iterator();

		while (var3.hasNext()) {
			PermissionAttachmentInfo permission = (PermissionAttachmentInfo) var3.next();
			permissions.append(permission.getPermission()).append("\n");
		}

		String contents = "Игрок: " + cause.getName() + "\nВремя: " + CivGlobal.dateFormat.format(new Date()) + "\nСервер: " + Bukkit.getServerName() + "\nКарта: " + CivGlobal.getDynmapLink(Bukkit.getServerName()) + "\nЦивилизация: "
				+ CivGlobal.getResident(cause).getCivName() + "\nГород: " + CivGlobal.getResident(cause).getTownName() + "\nДеревня: " + CivGlobal.getResident(cause).getCampName() + "\n" + permissions
				+ "================================= Страктрейс ===================================\n\n" + ExceptionUtils.getStackTrace(e) + "\n=====================================================================\nПричина: "
				+ ExceptionUtils.getRootCauseMessage(e.getCause()) + "\n================================= Полный страктрейс причины ===================================\n\n" + ExceptionUtils.getFullStackTrace(e.getCause()) + "\n";
		String url = CivLog.paste(contents, "exc", "", "exc");
		Iterator<? extends Player> var5 = Bukkit.getOnlinePlayers().iterator();

		while (var5.hasNext()) {
			Player admin = (Player) var5.next();
			if (admin.isOp()) {
				send((Object) admin, (String) ("§c" + CivColor.UNDERLINE + "Новый Exception: " + url));
			}
		}

		url = url.replace(".exc", "");
		return url.replace("https://www.dropbox.com/home/HNM", "");
	}

	public static void sendTownNoRepeat(final Town town, final String line) {
		final Integer hashcode = CivMessage.lastMessageHashCode.get(town.getName());
		if (hashcode != null && hashcode == line.hashCode()) {
			return;
		}
		CivMessage.lastMessageHashCode.put(town.getName(), line.hashCode());
		sendTown(town, line);
	}

	public static String plurals(final int count, final String... pluralForms) {
		final int i = (count % 10 == 1 && count % 100 != 11) ? 0 : ((count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) ? 1 : 2);
		return pluralForms[i];
	}
}
