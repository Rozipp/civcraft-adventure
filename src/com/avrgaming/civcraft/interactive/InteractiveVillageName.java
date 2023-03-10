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
package com.avrgaming.civcraft.interactive;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.village.Village;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class InteractiveVillageName implements InteractiveResponse {
	   public void respond(String message, Resident resident) {
	      Player player;
	      try {
	         player = CivGlobal.getPlayer(resident);
	      } catch (CivException var5) {
	         return;
	      }

	      if (!message.equalsIgnoreCase("cancel") && !message.equalsIgnoreCase("сфтсуд")) {
	         if (StringUtils.isAlpha(message) && StringUtils.isAsciiPrintable(message)) {
	            message = message.replace(" ", "_");
	            message = message.replace("\"", "");
	            message = message.replace("'", "");
	            Village.newVillage(resident, player, message);
	         } else {
	            CivMessage.send(player, "§c" + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_village_invalid"));
	         }
	      } else {
	         CivMessage.send(player, CivSettings.localize.localizedString("interactive_village_cancel"));
	         resident.clearInteractiveMode();
	      }
	   }
	}
