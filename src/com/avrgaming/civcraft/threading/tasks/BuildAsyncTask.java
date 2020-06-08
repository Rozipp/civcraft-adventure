/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.threading.tasks;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;

public class BuildAsyncTask extends CivAsyncTask {
	/* This task slow-builds a struct block-by-block based on the town's hammer rate. This task is per-structure building and will use the
	 * CivAsynTask interface to send synchronous requests to the main thread to build individual blocks. */

	public Buildable buildable;
	public int period;
	public int blocks_per_tick;
	public Template tpl;
	public BlockCoord centerBlock;

	private int extra_blocks;
	private int percent_complete;
	private Queue<SimpleBlock> sbs; // Blocks to add to main sync task queue;
	public Boolean aborted = false;
	public Date lastSave;

	private final int SAVE_INTERVAL = 5 * 1000; /* once every 5 sec. */

	public BuildAsyncTask(Buildable bld) {
		buildable = bld;
		period = 500;
		tpl = buildable.getTemplate();
		centerBlock = buildable.getCorner();
		this.blocks_per_tick = buildable.getBlocksPerTick();
		this.percent_complete = 0;
		sbs = new LinkedList<SimpleBlock>();
	}

	@Override
	public void run() {
		try {
			if (start()) {
				// Do something if we aborted???
			}
		} catch (Exception e) {
			CivLog.exception("BuildAsyncTask town:" + buildable.getTown() + " struct:" + buildable.getDisplayName() + " template:" + tpl.getDirection(), e);
		}
	}

	private boolean start() {
		lastSave = new Date();
		int count = 0;
		for (; buildable.getBlocksCompleted() < buildable.getTotalBlock(); buildable.blocksCompleted++) {
			period = buildable.getTimeLag();
			blocks_per_tick = buildable.getBlocksPerTick();

			synchronized (aborted) {
				if (aborted) return aborted;
			}

			if (buildable.isComplete()) break;

			if (buildable instanceof Wonder) {
				try {
					if (buildable.getTown().getMotherCiv() != null) { // Если нас захватили то ждать 30 минут
						CivMessage.sendTown(buildable.getTown(), CivSettings.localize.localizedString("var_buildAsync_wonderHaltedConquered", buildable.getTown().getCiv().getName()));
						Thread.sleep(1800000); // 30 min notify.
					}
					Buildable inProgress = buildable.getTown().getCurrentStructureInProgress();
					if (inProgress != null && inProgress != buildable) { // если строим другое здание, то ждем 1 минуту
						CivMessage.sendTown(buildable.getTown(), CivSettings.localize.localizedString("var_buildAsync_wonderHaltedOtherConstruction", inProgress.getDisplayName()));
						Thread.sleep(60000); // 1 min notify.
					}
					if (buildable.getTown().getTownHall() == null) {
						CivMessage.sendTown(buildable.getTown(), CivSettings.localize.localizedString("buildAsync_wonderHaltedNoTownHall"));
						Thread.sleep(600000); // 10 min notify.
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

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

			SimpleBlock sb = getSBlockRandom(buildable.getBlocksCompleted());

			// Add this SimpleBlock to the update queue and *assume* that all of the synchronous stuff is now going to be handled later. Perform the
			// reset of the build task async.
			if (!Template.isAttachable(sb.getMaterial())) sbs.add(sb);
			if (!buildable.isDestroyable() && sb.getType() != CivData.AIR) {
				if (sb.specialType != Type.COMMAND) {
					BlockCoord coord = new BlockCoord(sb.worldname, sb.x, sb.y, sb.z);
					buildable.addConstructBlock(coord, sb.y != 0);
				}
			}

			if (skipToNext) continue;

			Date now = new Date();
			if (now.getTime() > lastSave.getTime() + SAVE_INTERVAL) {
				buildable.updateBuildProgess();
				lastSave = now;
			}

			count = 0; // reset count, this tick is over.
			// Add all of the blocks from this tick to the sync task.
			synchronized (this.aborted) {
				if (!this.aborted) {
					SyncBuildUpdateTask.queueSimpleBlock(sbs);
					sbs.clear();
				} else
					return aborted;
			}

			try {
				int nextPercentComplete = (int) (((double) buildable.getBlocksCompleted() / (double) buildable.getTotalBlock()) * 100);
				if (nextPercentComplete > this.percent_complete) {
					this.percent_complete = nextPercentComplete;
					if ((this.percent_complete % 10 == 0)) {
						if (this.buildable instanceof Wonder)
							CivMessage
									.global(CivSettings.localize.localizedString("var_buildAsync_progressWonder", this.buildable.getDisplayName(), this.buildable.getTown().getName(), nextPercentComplete, this.buildable.getCiv().getName()));
						else
							CivMessage.sendTown(buildable.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildAsync_progressOther", buildable.getDisplayName(), nextPercentComplete));
					}
				}

				int timeleft = period;
				while (timeleft > 0) {
					int min = Math.min(10000, timeleft);
					Thread.sleep(min);
					timeleft -= 10000;

					/* Calculate our speed again in case our hammer rate has changed. */
					int newSpeed = buildable.getTimeLag();
					if (newSpeed != period) {
						period = newSpeed;
						timeleft = newSpeed;
					}
				}

				if (buildable instanceof Wonder) {
					if (checkOtherWonderAlreadyBuilt()) {
						processWonderAbort();
						return false; // wonder aborted via function above, no need to abort again.
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
				// check if wonder was completed...
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}
		// Make sure the last iteration makes it on to the queue.
		if (sbs.size() > 0) {
			SyncBuildUpdateTask.queueSimpleBlock(sbs);
			sbs.clear();
		}
		return finished();
	}

	private boolean finished() {
		// structures are always available
		if (buildable instanceof Wonder) {
			if (checkOtherWonderAlreadyBuilt()) {
				processWonderAbort();
				return false;
			}
		}

		buildable.setComplete(true);
		if (buildable instanceof Wonder)
			buildable.getTown().setCurrentWonderInProgress(null);
		else
			buildable.getTown().setCurrentStructureInProgress(null);
		buildable.savedBlockCount = buildable.blocksCompleted;
		buildable.updateBuildProgess();
		buildable.save();

		Template.deleteFilePath(Template.getInprogressFilePath(buildable.getCorner().toString()));
		buildable.getTown().build_tasks.remove(this);
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				buildable.repairFromTemplate();
			}
		});
		buildable.postBuildSyncTask();
		if (this.buildable instanceof Structure) {
			CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_completed", this.buildable.getTown().getName(), "§2" + this.buildable.getDisplayName() + CivColor.RESET));
		} else
			if (this.buildable instanceof Wonder) {
				CivMessage.global(CivSettings.localize.localizedString("var_buildAsync_completedWonder", CivColor.Red + this.buildable.getCiv().getName() + CivColor.RESET, "§6" + this.buildable.getTown().getName() + CivColor.RESET,
						"§a" + this.buildable.getDisplayName() + CivColor.RESET));
			}
		buildable.onComplete();
		return false;
	}

	// -------------- getSimpleBlock__()------------
	// private SimpleBlock getSBlockSpiral(int blockCount) {
	// // посрока по спирали. на неквадратные постройки не работает
	// int n = buildable.blocksCompleted % (tpl.size_x * tpl.size_z);
	// int p = (int) Math.floor(Math.sqrt(n));
	// int q = n - p * p;
	// int k = (p % 2) * 2 - 1; // p парне, то -1, непарне, то 1
	// int x = Math.floorDiv(p + 1, 2) * k;
	// int z = -Math.floorDiv(p + 1, 2) * k + (p % 2);
	// int y = (buildable.blocksCompleted / (tpl.size_x * tpl.size_z)); // bottom to top.
	// if (q < p) {
	// z = z + k * q;
	// } else {
	// x = x - k * (q - p);
	// z = z + k * p;
	// } // перенос координат из центра на угол
	// x = x + (tpl.size_x + 1) / 2 - 1;
	// z = z + (tpl.size_z + 1) / 2 - 1;
	// SimpleBlock sb = new SimpleBlock(centerBlock, tpl.blocks[x][y][z]);
	// sb.buildable = buildable;
	// return sb;
	// }

	/** Возвращает ii-тое псевдо случайное число при максимальном max */
	private static int getR(int ii, int max) {
		int i = (ii < max) ? ii : (ii % max);
		List<Integer> rm = Arrays.asList(85, 79, 44, 2, 342, 373, 496, 48, 321, 340, 330, 57, 213, 311, 201, 114, 403, 300, 182, 431, 488, 93, 358, 501, 16, 179, 362, 348, 30, 170, 457, 183, 426, 475, 132, 291, 80, 450, 499, 423, 464, 385,
				111, 197, 410, 346, 350, 302, 375, 249, 42, 404, 411, 229, 38, 395, 202, 159, 434, 231, 14, 441, 367, 126, 76, 95, 485, 427, 287, 169, 289, 437, 366, 120, 285, 243, 444, 506, 21, 490, 370, 75, 344, 425, 216, 133, 361, 331,
				320, 98, 4, 421, 473, 156, 290, 32, 356, 136, 34, 212, 22, 192, 322, 260, 388, 72, 493, 415, 384, 407, 147, 489, 278, 112, 13, 89, 507, 406, 148, 308, 296, 369, 74, 482, 50, 161, 167, 305, 119, 61, 222, 318, 293, 463, 315,
				448, 430, 379, 236, 484, 41, 37, 240, 357, 139, 31, 195, 313, 284, 155, 219, 43, 327, 137, 374, 246, 433, 476, 298, 162, 223, 368, 232, 303, 432, 68, 199, 265, 312, 478, 466, 378, 500, 445, 247, 49, 326, 196, 19, 329, 276,
				363, 189, 92, 481, 271, 319, 63, 471, 439, 474, 332, 412, 398, 125, 171, 274, 230, 495, 66, 143, 449, 491, 163, 310, 338, 456, 12, 337, 314, 281, 67, 418, 347, 360, 275, 154, 172, 400, 408, 181, 84, 279, 250, 46, 207, 117,
				5, 286, 90, 429, 208, 60, 349, 447, 509, 193, 18, 277, 64, 211, 301, 334, 458, 462, 96, 436, 483, 150, 188, 328, 244, 304, 101, 194, 88, 110, 272, 380, 254, 106, 268, 498, 480, 364, 352, 414, 185, 91, 440, 242, 27, 116, 146,
				166, 469, 45, 142, 324, 173, 472, 140, 405, 461, 257, 258, 227, 335, 52, 341, 65, 339, 387, 453, 145, 144, 217, 28, 225, 59, 391, 294, 269, 273, 77, 200, 316, 203, 51, 86, 6, 389, 135, 82, 381, 264, 238, 24, 118, 127, 81,
				168, 422, 248, 383, 309, 511, 198, 228, 397, 214, 152, 99, 58, 267, 157, 209, 307, 508, 94, 435, 151, 345, 413, 178, 23, 54, 87, 69, 438, 252, 224, 460, 237, 420, 510, 396, 215, 371, 175, 78, 477, 205, 29, 283, 220, 467,
				109, 306, 97, 3, 492, 177, 390, 351, 376, 10, 221, 399, 503, 251, 134, 105, 233, 377, 299, 354, 121, 486, 184, 336, 394, 39, 401, 129, 128, 35, 180, 73, 372, 113, 138, 149, 124, 424, 504, 295, 70, 8, 115, 455, 292, 259, 55,
				255, 9, 325, 459, 282, 428, 393, 270, 141, 359, 297, 131, 280, 204, 210, 122, 36, 15, 0, 266, 235, 505, 470, 71, 186, 108, 7, 502, 443, 103, 386, 451, 56, 165, 40, 262, 419, 317, 343, 261, 452, 454, 468, 25, 11, 20, 190,
				263, 353, 107, 416, 160, 26, 17, 241, 174, 392, 104, 164, 288, 365, 100, 218, 245, 62, 33, 187, 234, 83, 382, 497, 53, 494, 446, 409, 355, 206, 256, 417, 158, 47, 253, 323, 1, 333, 102, 239, 191, 465, 130, 442, 226, 479,
				123, 176, 153, 487, 402);
		int ret = rm.get(i);
		while (ret >= max) {
			ret = rm.get(ret);
		}
		return ret;
	}

	private SimpleBlock getSBlockRandom(int blockCount) {
		// постройка по случайным блокам
		int n = buildable.blocksCompleted % (tpl.size_x * tpl.size_z);

		int i = n / tpl.size_z;
		int j = n % tpl.size_z;

		int z = getR(j, tpl.size_z);
		int x = getR(getR(i + j + 2, tpl.size_x), tpl.size_x);

		int y = (buildable.blocksCompleted / (tpl.size_x * tpl.size_z)); // bottom to top.

		SimpleBlock sb = new SimpleBlock(centerBlock, tpl.blocks[x][y][z]);
		sb.buildable = buildable;
		return sb;
	}

	// private SimpleBlock getSBlockOrig(int blockCount) {
	// // 3D mailman algorithm...
	// int y = (blockCount / (tpl.size_x * tpl.size_z)); // bottom to top.
	// // int y = (tpl.size_y - (blockCount / (tpl.size_x*tpl.size_z))) - 1; //Top to
	// // bottom
	// int z = (blockCount / tpl.size_x) % tpl.size_z;
	// int x = blockCount % tpl.size_x;
	//
	// SimpleBlock sb = new SimpleBlock(centerBlock, tpl.blocks[x][y][z]);
	// sb.buildable = buildable;
	// return sb;
	// }

	private boolean checkOtherWonderAlreadyBuilt() {
		if (buildable.isComplete()) return false; // We are completed, other wonders are not already built.
		return (!Wonder.isWonderAvailable(buildable.getConfigId()));
	}

	private void processWonderAbort() {
		CivMessage.sendTown(buildable.getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_buildAsync_wonderFarAway", buildable.getDisplayName()));

		// Refund the town half the cost of the wonder.
		double refund = (int) (buildable.getCost() / 2);
		buildable.getTown().depositDirect(refund);

		CivMessage.sendTown(buildable.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildAsync_wonderRefund", refund, CivSettings.CURRENCY_NAME));
		abortWonder();
	}

	private void abortWonder() {
		// class SyncTask implements Runnable {
		// @Override
		// public void run() {
		// Remove build task from town..
		buildable.getTown().build_tasks.remove(this);
		buildable.unbindConstructBlocks();
		// remove wonder from town.
		synchronized (buildable.getTown()) {
			buildable.getTown().removeWonder((Wonder)buildable);
		}
		// Remove the scaffolding..
		tpl.removeScaffolding(buildable.getCorner().getLocation());
		buildable.delete();
		// }
		// }
		// TaskMaster.syncTask(new SyncTask());
	}

	public double setExtraHammers(double extra_hammers) {
		double leftover_hammers = 0.0;
		// Get the total number of blocks represented by the extra hammers.
		synchronized (this) {
			this.extra_blocks = (int) (buildable.getBlocksPerHammer() * extra_hammers);
			int blocks_left = buildable.getTotalBlock() - buildable.getBlocksCompleted();
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
