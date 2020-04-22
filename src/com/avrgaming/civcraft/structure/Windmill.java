/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class Windmill extends Structure {

	public enum CropType {
		WHEAT, CARROTS, POTATOES
	}

	public Windmill(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public Windmill(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	@Override
	public void onMinuteUpdate() {
		Windmill windmill = this;
		/* Fire a sync task to perform this. */
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				/* Find adjacent farms, get their chunk snapshots and continue processing in our thread. */
				ChunkCoord ccB = new ChunkCoord(windmill.getCorner());
				ArrayList<ChunkSnapshot> snapshots = new ArrayList<ChunkSnapshot>();

				int[][] move = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {1, 1}, {-1, -1}, {-1, 1}, {1, -1}};
				for (int i = 0; i < 8; i++) {
					ChunkCoord cc = new ChunkCoord(ccB.getWorldname(), ccB.getX() + move[i][0], ccB.getZ() + move[i][1]);
					FarmChunk farmChunk = CivGlobal.getFarmChunk(cc);
					if (farmChunk != null) snapshots.add(farmChunk.getChunk().getChunkSnapshot());
				}
				if (snapshots.size() == 0) return;
				/* Fire off an async task to do some post processing. */
				TaskMaster.asyncTask("", new WindmillPreProcessTask(windmill, snapshots), 0);
			}
		}, 0);
	}

	class WindmillPreProcessTask extends CivAsyncTask {

		private ArrayList<ChunkSnapshot> snapshots;
		private Windmill windmill;

		public WindmillPreProcessTask(Windmill windmill, ArrayList<ChunkSnapshot> snaphots) {
			this.snapshots = snaphots;
			this.windmill = windmill;
		}

		@Override
		public void run() {
			int plant_max;
			try {
				plant_max = CivSettings.getInteger(CivSettings.structureConfig, "windmill.plant_max");
				if (windmill.getCiv().hasTechnology("tech_machinery")) plant_max *= 2;
				if (windmill.getTown().getBuffManager().hasBuff("buff_farm")) plant_max *= 2;
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}

			/* Read in the source inventory's contents. Make sure we have seeds to plant. */
			ArrayList<ConstructChest> sources = windmill.getAllChestsById("0");
			MultiInventory source_inv = new MultiInventory();

			for (ConstructChest src : sources) {
				try {
					this.syncLoadChunk(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getZ());
					Inventory tmp;
					try {
						tmp = this.getChestInventory(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getY(), src.getCoord().getZ(), true);
					} catch (CivTaskAbortException e) {
						return;
					}
					source_inv.addInventory(tmp);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}

			int breadCount = 0;
			int carrotCount = 0;
			int potatoCount = 0;
			for (ItemStack stack : source_inv.getContents()) {
				if (stack == null) continue;
				switch (ItemManager.getTypeId(stack)) {
					case CivData.BREAD_SEED :
						breadCount += stack.getAmount();
						break;
					case CivData.CARROT_ITEM :
						carrotCount += stack.getAmount();
						break;
					case CivData.POTATO_ITEM :
						potatoCount += stack.getAmount();
						break;
					default :
						continue;
				}
			}

			/* If we've got nothing in the seed basket, nothing to plant! */
			if (breadCount == 0 && carrotCount == 0 && potatoCount == 0) return;

			/* Only try to plant as many crops as we have (or the max) */
			plant_max = Math.min((breadCount + carrotCount + potatoCount), plant_max);

			/* Read snapshots and find blocks that can be planted. */
			ArrayList<BlockCoord> blocks = new ArrayList<BlockCoord>();
			for (ChunkSnapshot snapshot : this.snapshots) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						for (int y = 0; y < 255; y++) {
							if (ItemManager.getBlockTypeId(snapshot, x, y, z) == CivData.FARMLAND) {
								if (ItemManager.getBlockTypeId(snapshot, x, y + 1, z) == CivData.AIR) {
									int blockx = (snapshot.getX() * 16) + x;
									int blocky = y + 1;
									int blockz = (snapshot.getZ() * 16) + z;
									blocks.add(new BlockCoord(this.windmill.getCorner().getWorldname(), blockx, blocky, blockz));
								}
							}
						}
					}
				}
			}

			ArrayList<BlockCoord> plantBlocks = new ArrayList<BlockCoord>();
			/* Select up to plant_max of these blocks to be planted. */
			Random rand = new Random();
			for (int i = 0; i < plant_max; i++) {
				if (blocks.isEmpty()) break;
				BlockCoord coord = blocks.get(rand.nextInt(blocks.size()));
				blocks.remove(coord);
				plantBlocks.add(coord);
			}
			// Fire off a sync task to complete the operation.
			TaskMaster.syncTask(new WindmillPostProcessSyncTask(windmill, plantBlocks, breadCount, carrotCount, potatoCount, source_inv));
		}
	}

	class WindmillPostProcessSyncTask implements Runnable {
		ArrayList<BlockCoord> plantBlocks;
		Windmill windmill;
		int breadCount;
		int carrotCount;
		int potatoCount;
		MultiInventory source_inv;

		public WindmillPostProcessSyncTask(Windmill windmill, ArrayList<BlockCoord> plantBlocks, int breadCount, int carrotCount, int potatoCount, MultiInventory source_inv) {
			this.plantBlocks = plantBlocks;
			this.windmill = windmill;
			this.breadCount = breadCount;
			this.carrotCount = carrotCount;
			this.potatoCount = potatoCount;
			this.source_inv = source_inv;
		}

		@Override
		public void run() {
			Random rand = new Random();
			for (BlockCoord coord : plantBlocks) {
				int randomCropType = rand.nextInt(3);
				switch (randomCropType) {
					case 0 :
						if (breadCount > 0) {
							/* bread seed */
							try {
								source_inv.removeItem(CivData.BREAD_SEED, 1, true);
							} catch (CivException e) {
								e.printStackTrace();
							}
							breadCount--;
							ItemManager.setTypeId(coord.getBlock(), CivData.WHEAT);
							ItemManager.setData(coord.getBlock(), 0, true);
							continue;
						}
					case 1 :
						if (carrotCount > 0) {
							/* carrots */
							try {
								source_inv.removeItem(CivData.CARROT_ITEM, 1, true);
							} catch (CivException e) {
								e.printStackTrace();
							}
							carrotCount--;
							ItemManager.setTypeId(coord.getBlock(), CivData.CARROTS);
							ItemManager.setData(coord.getBlock(), 0, true);

							continue;
						}
						break;
					case 2 :
						if (potatoCount > 0) {
							/* potatoes */
							try {
								source_inv.removeItem(CivData.POTATO_ITEM, 1, true);
							} catch (CivException e) {
								e.printStackTrace();
							}
							potatoCount--;
							ItemManager.setTypeId(coord.getBlock(), CivData.POTATOES);
							ItemManager.setData(coord.getBlock(), 0, true);

							continue;
						}
				}

				/* our randomly selected crop couldn't be placed, try them all now. */
				if (breadCount > 0) {
					/* bread seed */
					try {
						source_inv.removeItem(CivData.BREAD_SEED, 1, true);
					} catch (CivException e) {
						e.printStackTrace();
					}
					breadCount--;
					ItemManager.setTypeId(coord.getBlock(), CivData.WHEAT);
					ItemManager.setData(coord.getBlock(), 0, true);

					continue;
				}
				if (carrotCount > 0) {
					/* carrots */
					try {
						source_inv.removeItem(CivData.CARROT_ITEM, 1, true);
					} catch (CivException e) {
						e.printStackTrace();
					}
					carrotCount--;
					ItemManager.setTypeId(coord.getBlock(), CivData.CARROTS);
					ItemManager.setData(coord.getBlock(), 0, true);

					continue;
				}
				if (potatoCount > 0) {
					/* potatoes */
					try {
						source_inv.removeItem(CivData.POTATO_ITEM, 1, true);
					} catch (CivException e) {
						e.printStackTrace();
					}
					potatoCount--;
					ItemManager.setTypeId(coord.getBlock(), CivData.POTATOES);
					ItemManager.setData(coord.getBlock(), 0, true);
					continue;
				}
			}
		}
	}
}
