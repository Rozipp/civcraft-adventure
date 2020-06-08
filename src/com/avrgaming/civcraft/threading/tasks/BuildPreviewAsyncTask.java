/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

public class BuildPreviewAsyncTask extends CivAsyncTask {
	/* This task slow-builds a struct block-by-block based on the town's hammer rate. This task is per-structure building and will use the
	 * CivAsynTask interface to send synchronous requests to the main thread to build individual blocks. */

	public Template tpl;
	public BlockCoord corner;
	public UUID playerUUID;
	public Boolean aborted = false;
	public ReentrantLock lock = new ReentrantLock();
	private int blocksPerTick;
	private int speed;
	private Resident resident;

	public BuildPreviewAsyncTask(Template tpl, BlockCoord corner, Player player) {
		this.tpl = tpl;
		this.corner = corner;
		this.playerUUID = player.getUniqueId();
		this.resident = CivGlobal.getResident(player);
		this.blocksPerTick = 1000;
		this.speed = 500;
	}

	public Player getPlayer() throws CivException {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null) {
			throw new CivException("Player offline");
		}
		return player;
	}

	@Override
	public void run() {
		try {
			int count = 0;
			for (int y = 1; y < tpl.size_y-1; y++) {
				for (int x = 1; x < tpl.size_x-1; x++) {
					for (int z = 1; z < tpl.size_z-1; z++) {
						if (aborted) return;
						Block b = corner.getBlockRelative(x, y, z);

						if (tpl.blocks[x][y][z].isAir()) continue;
						lock.lock();
						try {
							resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
							ItemManager.sendBlockChange(getPlayer(), b.getLocation(), CivSettings.previewMaterial, 5);
							count++;
						} finally {
							lock.unlock();
						}

						if (count < blocksPerTick) continue;
						count = 0;
						int timeleft = speed;
						while (timeleft > 0) {
							int min = Math.min(10000, timeleft);
							Thread.sleep(min);
							timeleft -= 10000;
						}
					}
				}
			}
		} catch (CivException | InterruptedException e) {
			// abort task.
		}
	}

}