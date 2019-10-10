/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.block.Block;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.TemplateStatic;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;

public class BuildAsyncTask extends CivAsyncTask {
	/* This task slow-builds a struct block-by-block based on the town's hammer rate. This task is per-structure building and will use the CivAsynTask interface
	 * to send synchronous requests to the main thread to build individual blocks. */

	public Buildable buildable;
	public int speed;
	public int blocks_per_tick;
	public Template tpl;
	public Block centerBlock;

	private int count;
	private int extra_blocks;
	private int percent_complete;
	private Queue<SimpleBlock> sbs; //Blocks to add to main sync task queue;
	public Boolean aborted = false;
	public Date lastSave;

	private final int SAVE_INTERVAL = 5 * 1000; /* once every 5 sec. */

	public BuildAsyncTask(Buildable bld, Template t, int blocks_per_tick, Block center) {
		buildable = bld;
		speed = 500;
		tpl = t;
		centerBlock = center;
		this.blocks_per_tick = blocks_per_tick;
		this.percent_complete = 0;
		sbs = new LinkedList<SimpleBlock>();
	}

	@Override
	public void run() {
		try {
			start();
			// Do something if we aborted???
		} catch (Exception e) {
			CivLog.exception("BuildAsyncTask town:" + buildable.getTown() + " struct:" + buildable.getDisplayName() + " template:" + tpl.getDirection(), e);
		}
	}

	private boolean start() {
		lastSave = new Date();

		for (; buildable.getBuiltBlockCount() < (tpl.size_x * tpl.size_y * tpl.size_z); buildable.builtBlockCount++) {
			speed = buildable.getBuildSpeed();
			blocks_per_tick = buildable.getBlocksPerTick();

			synchronized (aborted) {
				if (aborted) {
					return aborted;
				}
			}

			if (buildable.isComplete()) {
				break;
			}

			if (buildable instanceof Wonder) {
				if (buildable.getTown().getMotherCiv() != null) {
					CivMessage.sendTown(buildable.getTown(),
							CivSettings.localize.localizedString("var_buildAsync_wonderHaltedConquered", buildable.getTown().getCiv().getName()));
					try {
						Thread.sleep(1800000); //30 min notify.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				Buildable inProgress = buildable.getTown().getCurrentStructureInProgress();
				if (inProgress != null && inProgress != buildable) {
					CivMessage.sendTown(buildable.getTown(),
							CivSettings.localize.localizedString("var_buildAsync_wonderHaltedOtherConstruction", inProgress.getDisplayName()));
					try {
						Thread.sleep(60000); //1 min notify.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (buildable.getTown().getTownHall() == null) {
					CivMessage.sendTown(buildable.getTown(), CivSettings.localize.localizedString("buildAsync_wonderHaltedNoTownHall"));
					try {
						Thread.sleep(600000); //10 min notify.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			if (build()) continue;//skip to next run.

			Date now = new Date();
			if (now.getTime() > lastSave.getTime() + SAVE_INTERVAL) {
				buildable.updateBuildProgess();
				lastSave = now;
			}

			count = 0; //reset count, this tick is over.
			// Add all of the blocks from this tick to the sync task.
			synchronized (this.aborted) {
				if (!this.aborted) {
					SyncBuildUpdateTask.queueSimpleBlock(sbs);
					sbs.clear();
				} else {
					return aborted;
				}
			}

			try {
				int nextPercentComplete = (int) (((double) buildable.getBuiltBlockCount() / (double) buildable.getTotalBlockCount()) * 100);
				if (nextPercentComplete > this.percent_complete) {
					this.percent_complete = nextPercentComplete;
					if ((this.percent_complete % 10 == 0)) {
						if (this.buildable instanceof Wonder) {
							CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_progressWonder", this.buildable.getDisplayName(),
									this.buildable.getTown().getName(), nextPercentComplete, this.buildable.getCiv().getName()));
						} else {
							CivMessage.sendTown(buildable.getTown(), CivColor.Yellow
									+ CivSettings.localize.localizedString("var_buildAsync_progressOther", buildable.getDisplayName(), nextPercentComplete));
						}
					}
				}

				int timeleft = speed;
				while (timeleft > 0) {
					int min = Math.min(10000, timeleft);
					Thread.sleep(min);
					timeleft -= 10000;

					/* Calculate our speed again in case our hammer rate has changed. */
					int newSpeed = buildable.getBuildSpeed();
					if (newSpeed != speed) {
						speed = newSpeed;
						timeleft = newSpeed;
					}
				}

				if (buildable instanceof Wonder) {
					if (checkOtherWonderAlreadyBuilt()) {
						processWonderAbort();
						return false; //wonder aborted via function above, no need to abort again.
					}

					if (buildable.isDestroyed()) {
						CivMessage.sendTown(buildable.getTown(), CivSettings.localize.localizedString("var_buildAsync_destroyed", buildable.getDisplayName()));
						abortWonder();
						return false;
					}

					if (buildable.getTown().getMotherCiv() != null) {
						// Can't build wonder while we're conquered.
						continue;
					}
				}
				//check if wonder was completed...
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
		}
		// Make sure the last iteration makes it on to the queue.
		if (sbs.size() > 0) {
			SyncBuildUpdateTask.queueSimpleBlock(sbs);
			sbs.clear();
		}
		//structures are always available
		if (buildable instanceof Wonder) {
			if (checkOtherWonderAlreadyBuilt()) {
				processWonderAbort();
				return false;
			}
		}

		buildable.setComplete(true);
		if (buildable instanceof Wonder) {
			buildable.getTown().setCurrentWonderInProgress(null);
		} else {
			buildable.getTown().setCurrentStructureInProgress(null);
		}
		buildable.savedBlockCount = buildable.builtBlockCount;
		buildable.updateBuildProgess();
		buildable.save();

		tpl.deleteInProgessTemplate(buildable.getCorner().toString(), buildable.getTown());
		buildable.getTown().build_tasks.remove(this);
		buildable.postBuildSyncTask(tpl, 10);
		if (this.buildable instanceof Structure) {
			CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_completed", this.buildable.getTown().getName(),
					"§2" + this.buildable.getDisplayName() + CivColor.RESET));
		} else
			if (this.buildable instanceof Wonder) {
				CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_completedWonder",
						CivColor.Red + this.buildable.getCiv().getName() + CivColor.RESET, "§6" + this.buildable.getTown().getName() + CivColor.RESET,
						"§a" + this.buildable.getDisplayName() + CivColor.RESET));
			}
		buildable.onComplete();
		return false;
	}

	public boolean build() {

		boolean skipToNext = false;

		// Apply extra blocks first, then work on this blocks per tick.
		if (this.extra_blocks > 0) {
			synchronized (this) {
				extra_blocks--;
				skipToNext = true;
			}
		} else
			if (count < this.blocks_per_tick) {
				count++;
				skipToNext = true;
			}

		//3D mailman algorithm...	

		int y = (buildable.getBuiltBlockCount() / (tpl.size_x * tpl.size_z)); //bottom to top.
		//int y = (tpl.size_y - (buildable.builtBlockCount / (tpl.size_x*tpl.size_z))) - 1; //Top to bottom
		int z = (buildable.getBuiltBlockCount() / tpl.size_x) % tpl.size_z;
		int x = buildable.getBuiltBlockCount() % tpl.size_x;

		SimpleBlock sb = tpl.blocks[x][y][z];

		// Convert relative x,y,z to real x,y,z in world.
		sb.x = x + centerBlock.getX();
		sb.y = y + centerBlock.getY();
		sb.z = z + centerBlock.getZ();
		sb.worldname = centerBlock.getWorld().getName();
		sb.buildable = buildable;
		/* // посрока по спирали. на неквадратные постройки не работает int n = buildable.builtBlockCount % (tpl.size_x * tpl.size_z); int p = (int)
		 * Math.floor(Math.sqrt(n)); int q = n - p * p; int k = (p % 2) * 2 - 1; //p парне, то -1, непарне, то 1 int x = Math.floorDiv(p + 1, 2) * k; int z =
		 * -Math.floorDiv(p + 1, 2) * k + (p % 2); int y = (buildable.builtBlockCount / (tpl.size_x * tpl.size_z)); //bottom to top. if (q < p) { z = z + k * q;
		 * } else { x = x - k * (q - p); z = z + k * p; } // перенос координат из центра на угол x = x + (tpl.size_x+1)/2 - 1; z = z + (tpl.size_z+1)/2 - 1;
		 * SimpleBlock sb = tpl.blocks[x][y][z];
		 * 
		 * // Convert relative x,y,z to real x,y,z in world. sb.x = x + centerBlock.getX(); sb.y = y + centerBlock.getY(); sb.z = z + centerBlock.getZ();
		 * sb.worldname = centerBlock.getWorld().getName(); sb.buildable = buildable; */

		// Add this SimpleBlock to the update queue and *assume* that all of the 
		// synchronous stuff is now going to be handled later. Perform the reset
		// of the build task async.
		synchronized (this.aborted) {
			if (!this.aborted) {
				if (!TemplateStatic.isAttachable(sb.getMaterial())) sbs.add(sb);
				if (buildable.isDestroyable() == false && sb.getType() != CivData.AIR) {
					if (sb.specialType != Type.COMMAND) {
						BlockCoord coord = new BlockCoord(sb.worldname, sb.x, sb.y, sb.z);
						if (sb.y == 0)
							buildable.addStructureBlock(coord, false);
						else
							buildable.addStructureBlock(coord, true);
					}
				}
			} else {
				sbs.clear();
				return false;
			}
		}

		return skipToNext;
	}

	private boolean checkOtherWonderAlreadyBuilt() {
		if (buildable.isComplete()) {
			return false; //We are completed, other wonders are not already built. 
		}

		return (!Wonder.isWonderAvailable(buildable.getConfigId()));
	}

	private void processWonderAbort() {
		CivMessage.sendTown(buildable.getTown(),
				CivColor.Rose + CivSettings.localize.localizedString("var_buildAsync_wonderFarAway", buildable.getDisplayName()));

		//Refund the town half the cost of the wonder.
		double refund = (int) (buildable.getCost() / 2);
		buildable.getTown().depositDirect(refund);

		CivMessage.sendTown(buildable.getTown(),
				CivColor.Yellow + CivSettings.localize.localizedString("var_buildAsync_wonderRefund", refund, CivSettings.CURRENCY_NAME));
		abortWonder();
	}

	private void abortWonder() {
		class SyncTask implements Runnable {

			@SuppressWarnings("unlikely-arg-type")
			@Override
			public void run() {
				//Remove build task from town..
				buildable.getTown().build_tasks.remove(this);
				buildable.unbindStructureBlocks();

				//remove wonder from town.
				synchronized (buildable.getTown()) {
					//buildable.getTown().wonders.remove(buildable);
					buildable.getTown().removeWonder(buildable);
				}

				//Remove the scaffolding..
				tpl.removeScaffolding(buildable.getCorner().getLocation());
				try {
					((Wonder) buildable).delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		TaskMaster.syncTask(new SyncTask());

	}

	public double setExtraHammers(double extra_hammers) {

		double leftover_hammers = 0.0;
		//Get the total number of blocks represented by the extra hammers.
		synchronized (this) {
			this.extra_blocks = (int) (buildable.getBlocksPerHammer() * extra_hammers);
			int blocks_left = buildable.getTotalBlockCount() - buildable.getBuiltBlockCount();
			if (this.extra_blocks > blocks_left) {
				leftover_hammers = (this.extra_blocks - blocks_left) / buildable.getBlocksPerHammer();
			}
		}
		return leftover_hammers;
	}

	public void abort() {
		synchronized (aborted) {
			aborted = true;
		}
	}

}
