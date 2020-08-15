/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.sync;

import java.util.ArrayList;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Template;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.SimpleType;

public class RecoverStructureSyncTask implements Runnable {

	ArrayList<Structure> structures;
	CommandSender sender;

	public RecoverStructureSyncTask(CommandSender sender, ArrayList<Structure> structs) {
		this.structures = structs;
		this.sender = sender;
	}

	public void repairStructure(Structure struct) {
		// Repairs a structure, one block at a time. Does not bother repairing
		// command blocks since they will be re-populated in onLoad() anyway.

		// Template is already loaded.
		Template tpl = struct.getTemplate();
		if (tpl == null) return;

		Block cornerBlock = struct.getCorner().getBlock();
		for (int y = 0; y < tpl.size_y; y++) {
			for (SimpleBlock sb : tpl.blocks.get(y)) {
				if (sb.specialType != SimpleType.NORMAL) continue;
				Block nextBlock = cornerBlock.getRelative(sb.x, y, sb.z);

				if (ItemManager.getTypeId(nextBlock) != sb.getType()) {
					ItemManager.setTypeId(nextBlock, sb.getType());
					ItemManager.setData(nextBlock, sb.getData());
				}
			}
		}
	}


	@Override
	public void run() {
		for (Structure struct : this.structures) {
			CivMessage.send(sender, CivSettings.localize.localizedString("structureRepairStart") + " " + struct.getDisplayName() + " @ " + CivColor.Yellow + struct.getCorner());
			repairStructure(struct);
		}
		CivMessage.send(sender, CivSettings.localize.localizedString("structureRepairComplete"));
	}

}
