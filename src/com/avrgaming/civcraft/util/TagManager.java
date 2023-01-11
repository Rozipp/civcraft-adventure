package com.avrgaming.civcraft.util;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
//import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.permission.PermissionGroup;

import org.apache.commons.lang.*;
import java.util.regex.*;
import java.util.*;

public class TagManager {
	private static final Pattern CLEANUP_REDUNDANT_COLORS_REGEX;
	public static String reset;
	public static HashMap<String, String> hash;
	public static Set<String> curators;
	public static Set<String> devs;

	public static void editNameTag(final Player player) {
		String prefix = "";
		String suffix = "";
		if (PermissionGroup.hasGroup(player.getName(), "premium")) {
			prefix = CivColor.BOLD + "§eP" + TagManager.reset;
		}
		if (PermissionGroup.hasGroup(player.getName(), "ultra")) {
			prefix = "§a" + CivColor.BOLD + "§dU" + TagManager.reset;
		}
		if (PermissionGroup.hasGroup(player.getName(), "deluxe")) {
			prefix = "§a" + CivColor.BOLD + "§bD" + TagManager.reset;
		}
		if (PermissionGroup.hasGroup(player.getName(), "improver")) {
			prefix = "§a" + CivColor.BOLD + "С" + TagManager.reset;
		}
		if (PermissionGroup.hasGroup(player.getName(), "moderator")) {
			prefix = "§a" + CivColor.BOLD + "§aM" + TagManager.reset;
		}
		if (PermissionGroup.hasGroup(player.getName(), "stmoderator")) {
			prefix = "§a" + CivColor.BOLD + "St.M" + TagManager.reset;
		}
		if (PermissionGroup.hasGroup(player.getName(), "winner")) {
			prefix = "§6" + CivColor.BOLD + "WIN" + TagManager.reset;
		}
		if (isCurator(player)) {
			prefix = CivColor.Gold + "K" + TagManager.reset;
		}
		if (isDev(player)) {
			prefix = CivColor.RoseBold + "DEV" + TagManager.reset;
		}

		final Resident resident = CivGlobal.getResident(player);
		try {
			if (resident != null && !StringUtils.isBlank(resident.getPrefix())) {
				if (player.hasPermission("civcraft.prefix") && !isDev(player)) {
					prefix = CivColor.LightBlueBold + resident.getPrefix() + TagManager.reset;
				} else
					if (isDev(player)) {
						prefix = CivColor.DarkPurple + resident.getPrefix() + TagManager.reset;
					}
			}
		} catch (NullPointerException ex) {}
		if (resident != null) {
			if (resident.getCiv() != null) {
				suffix = CivGlobal.getNameTagColor(resident.getCiv()) + " [" + resident.getCiv().getTag() + "]";
			} else
				if (resident.getCamp() != null) {
					suffix = "§7 [" + StringUtils.left(resident.getCamp().getName(), 5) + "]";
				}
		}
		String fullCustom = prefix;
		if (!StringUtils.isBlank(prefix)) {
			fullCustom += " ";
		}
		fullCustom = fullCustom + player.getName() + suffix;
		fullCustom = normalizeColors(fullCustom);
		player.setCustomNameVisible(true);
		player.setCustomName(fullCustom);
		player.setPlayerListName(fullCustom);
		TagManager.hash.put(player.getName(), fullCustom);
		if (resident != null) {
			resident.setSavedPrefix(prefix);
		}
	}

	public static void editNameTag(Resident resident) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
			editNameTag(player);
		} catch (CivException e) {}
	}

	private static boolean isCurator(Player var1) {
		return TagManager.curators.contains(var1.getName());
	}

	public static void editNameTag(final Town var1) {
		for (final Resident resident : var1.getOnlineResidents()) {
			try {
				final Player player = CivGlobal.getPlayer(resident);
				editNameTag(player);
			} catch (Exception local) {}
		}
	}

	public static void editNameTag(final Civilization var1) {
		for (final Resident resident : var1.getOnlineResidents()) {
			try {
				final Player player = CivGlobal.getPlayer(resident);
				editNameTag(player);
			} catch (Exception local) {}
		}
	}

	public static void editNameTag(final Camp var1) {
		for (final Resident resident : var1.getMembers()) {
			try {
				final Player player = CivGlobal.getPlayer(resident);
				editNameTag(player);
			} catch (Exception local) {}
		}
	}

	public static String getFullNameTag(final Player var1) {
		final String full = TagManager.hash.get(var1.getName());
		return ((full != null) ? full : var1.getName()) + TagManager.reset;
	}

	public static boolean isCuator(final Player var1) {
		return TagManager.curators.contains(var1.getName());
	}

	public static boolean isDev(final Player var1) {
		return TagManager.devs.contains(var1.getName());
	}

	public static String normalizeColors(final String str) {
		final Matcher matcher = TagManager.CLEANUP_REDUNDANT_COLORS_REGEX.matcher(str);
		StringBuffer sb = null;

		while (matcher.find()) {
			if (sb == null) {
				sb = new StringBuffer(str.length() - 2);
			}
			final String truncated = matcher.group(1);
			int spaces = 0;
			for (final char c : truncated.toCharArray()) {
				if (c == ' ') {
					++spaces;
				}
			}
			final String valid = matcher.group(2);
			matcher.appendReplacement(sb, valid);
			if (spaces > 0) {
				final StringBuilder spacesStr = new StringBuilder(spaces);
				for (int i = 0; i < spaces; ++i) {
					spacesStr.append(' ');
				}
				sb.insert(sb.length() - 2, spacesStr);
			}
		}

		if (sb != null) {
			matcher.appendTail(sb);
			return sb.toString();
		}

		return str;
	}

	static {
		CLEANUP_REDUNDANT_COLORS_REGEX = Pattern.compile("§[0-9a-fk-or]((?: *§[0-9a-fk-or])*)( *§[0-9a-fr])");
		TagManager.reset = CivColor.RESET;
		TagManager.hash = new HashMap<>();
		TagManager.curators = new HashSet<>(Arrays.asList("Test"));
		TagManager.devs = new HashSet<>(Arrays.asList("MuffinColor", "Rozipp"));
	}
}
