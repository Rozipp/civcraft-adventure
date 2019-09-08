package com.avrgaming.civcraft.interactive;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.*;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.*;
import com.avrgaming.civcraft.object.*;
import com.avrgaming.civcraft.util.*;

public class InteractiveCivTagName implements InteractiveResponse {
	   public void respond(String message, Resident resident) {
	      Player player;
	      try {
	         player = CivGlobal.getPlayer(resident);
	      } catch (CivException var5) {
	         return;
	      }

	      if (!message.equalsIgnoreCase("cancel") && !message.equalsIgnoreCase("сфтсуд")) {
	         if (!player.isOp()) {
	            if (message.length() < 2 || message.length() > 5) {
	               CivMessage.send(player, "§c" + CivSettings.localize.localizedString("cmd_prefix_illegalArgument"));
	               resident.clearInteractiveMode();
	               return;
	            }
	         }

	         if (CivGlobal.anybodyHasTag(message)) {
	            CivMessage.send(player, CivColor.RoseBold + CivSettings.localize.localizedString("interactive_civtag_arleadyExists"));
	            resident.clearInteractiveMode();
	         } else {
	            message = message.replace(" ", "_");
	            message = message.replace("\"", "");
	            message = message.replace("'", "");
	            resident.desiredTag = message;
	            CivMessage.send(player, "§a" + CivSettings.localize.localizedString("var_interactive_civtag_success1", new Object[]{"§e" + message + "§a"}));
	            CivMessage.send(player, " ");
	            CivMessage.send(player, CivColor.LightGreenBold + CivSettings.localize.localizedString("interactive_civ_success4"));
	            CivMessage.send(player, "§7" + CivSettings.localize.localizedString("interactive_civ_tocancel"));
	            resident.setInteractiveMode(new InteractiveCapitolName());
	         }
	      } else {
	         CivMessage.send(player, CivSettings.localize.localizedString("interactive_civ_cancel"));
	         resident.clearInteractiveMode();
	      }
	   }
	}