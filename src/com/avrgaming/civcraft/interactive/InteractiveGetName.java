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

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class InteractiveGetName implements InteractiveResponse {

	public String invalidMessage = "Invalid name";
	public String cancelMessage = "Get Name Canseled";
	public boolean undoPreviewCancel = false;

	@Override
	public void respond(String message, Player player) {
		Resident resident = CivGlobal.getResident(player);

		if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("сфтсуд") || message.startsWith("/")) {
			CivMessage.send(player, cancelMessage);
			resident.clearInteractiveMode();
			if (undoPreviewCancel) resident.undoPreview();
			return;
		}

		if (!StringUtils.isAlpha(message) || !StringUtils.isAsciiPrintable(message)) {
			CivMessage.send(player, CivColor.RoseBold + invalidMessage);
			return;
		}

		message = message.replace(" ", "_").replace("\"", "").replace("\'", "").replace("\\", "");

		resident.pendingCallback.execute(message);
	}
}
