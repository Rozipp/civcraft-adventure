/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.interactive;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;

public class InteractiveBuildCommand implements InteractiveResponse {

	Buildable buildable;

	public InteractiveBuildCommand(Buildable buildable) {
		this.buildable = buildable;
	}

	@Override
	public void respond(String message, Resident resident) {
		CivLog.debug("build log: send yes");

		Town town = buildable.getTown();
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}

		if (!message.equalsIgnoreCase("yes")) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_cancel"));
			resident.clearInteractiveMode();
			resident.undoPreview();
			return;
		}

		if (!buildable.validated) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_invalid"));
			return;
		}

		if (!buildable.isValid() && !player.isOp()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_invalidNotOP"));
			return;
		}

		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(resident);
				} catch (CivException e) {
					return;
				}

				try {
					if (buildable instanceof Structure) {
						resident.clearInteractiveMode();
						town.buildStructure(player, (Structure) buildable);
					}
					if (buildable instanceof Wonder) {
						resident.clearInteractiveMode();
						town.buildWonder(player, (Wonder) buildable);
					}
				} catch (CivException e) {
					CivMessage.sendError(player, e.getMessage());
				}
			}
		});
	}
}