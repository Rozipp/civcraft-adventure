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
package com.avrgaming.civcraft.structure.farm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Block;

import com.avrgaming.civcraft.components.ActivateOnBiome;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.exception.InvalidBlockLocation;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Farm;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BlockSnapshot;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FarmChunk {

	private Construct construct;
	private ChunkCoord cCoord;
	public ChunkSnapshot snapshot;

	/* Populated Asynchronously, Integer represents last data value at that location.. may or may not be useful. */
	public ArrayList<BlockCoord> cropLocationCache = new ArrayList<BlockCoord>();
	public ArrayList<BlockCoord> staticCropLocationCache = new ArrayList<BlockCoord>();
	public ReentrantLock lock = new ReentrantLock();

	private ArrayList<BlockCoord> lastGrownCrops = new ArrayList<BlockCoord>();
	private LinkedList<GrowBlock> growBlocks;
	private Date lastGrowDate;
	private int lastGrowTickCount;
	private double lastChanceForLast;
	private int lastRandomInt;
	private int missedGrowthTicks;
	private int missedGrowthTicksStat;

	String biomeName = "none";
	private double lastEffectiveGrowthRate;

	public FarmChunk(ChunkCoord cCoord, Construct construct) {
		this.construct = construct;
		this.cCoord = cCoord;
		biomeName = cCoord.getChunk().getBlock(8, 64, 8).getBiome().name();
		CivGlobal.addFarmChunk(this.getCCoord(), this);
	}

	public String getNameOwner() {
		if (construct.getTown() != null) return construct.getTown().getName();
		if (construct instanceof Camp) return "Camp " + construct.getName();
		return construct.getName();
	}

	public double getGrowth() {
		if (construct.getTown() != null) return construct.getTown().getGrowth().total;
		if (construct instanceof Camp) return Camp.growthCampTotal;
		return 1.0;
	}

	public Chunk getChunk() {
		return this.cCoord.getChunk();
	}

	public boolean isHydrated(Block block) {
		Block beneath = block.getRelative(0, -1, 0);
		return beneath != null && ItemManager.getTypeId(beneath) == CivData.FARMLAND && ItemManager.getData(beneath) != 0x0;
	}

	public int getLightLevel(Block block) {
		return block.getLightLevel();
	}

	public void spawnMelonOrPumpkin(BlockSnapshot bs, BlockCoord growMe, CivAsyncTask task) throws InterruptedException {
		// search for a free spot
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		BlockSnapshot freeBlock = null;
		BlockSnapshot nextBlock = null;

		int xOff = 0;
		int zOff = 0;

		Random rand = new Random();
		int randChance = rand.nextInt(10);
		if (randChance <= 7) return;

		int randInt = rand.nextInt(4);
		try {
			nextBlock = bs.getRelative(offset[randInt][0], 0, offset[randInt][1]);
		} catch (InvalidBlockLocation e) {
			// An invalid block location can occur if we try to grow 'off the chunk' this kind of growth is not valid, simply continue onward.
			return;
		}
		if (nextBlock == null) return;
		if (nextBlock.getTypeId() == CivData.AIR) freeBlock = nextBlock;

		if ((nextBlock.getTypeId() == CivData.MELON && bs.getTypeId() == CivData.MELON_STEM) || (nextBlock.getTypeId() == CivData.PUMPKIN && bs.getTypeId() == CivData.PUMPKIN_STEM)) {
			return;
		}

		if (freeBlock == null) return;

		if (bs.getTypeId() == CivData.MELON_STEM)
			addGrowBlock(growMe.getRelative(xOff, 0, zOff), CivData.MELON, 0x0, true);
		else
			addGrowBlock(growMe.getRelative(xOff, 0, zOff), CivData.PUMPKIN, 0x0, true);

		return;
	}

	public void spawnSugarcane(BlockSnapshot bs, BlockCoord growMe, CivAsyncTask task) throws InterruptedException {
		// search for a free spot
		BlockSnapshot nextBlock = null;

		// Random rand = new Random();
		// int randChance = rand.nextInt(10);
		// if (randChance <= 7) return;
		int i;
		for (i = 0; i <= Farm.MAX_SUGARCANE_HEIGHT + 1; i++) {
			try {
				nextBlock = bs.getRelative(0, i, 0);
			} catch (InvalidBlockLocation e) {}
			if (nextBlock.getTypeId() == CivData.SUGARCANE) continue;
			if (nextBlock.getTypeId() == CivData.AIR) break;
		}
		if (nextBlock == null || i > Farm.MAX_SUGARCANE_HEIGHT) return;
		addGrowBlock(growMe.getRelative(0, i, 0), CivData.SUGARCANE, 0x0, true);
	}

	public void addGrowBlock(BlockCoord bcoord, int typeid, int data, boolean spawn) {
		this.growBlocks.add(new GrowBlock(bcoord, typeid, data, spawn));
	}

	public void growBlock(BlockSnapshot bs, BlockCoord growMe, CivAsyncTask task) throws InterruptedException {
		// XXX we are skipping hydration as I guess we dont seem to care.
		// XXX we also skip light level checks, as we dont really care about that either.
		switch (bs.getTypeId()) {
		case CivData.WHEAT:
		case CivData.CARROTS:
		case CivData.POTATOES:
			if (bs.getData() < 0x7) addGrowBlock(growMe, bs.getTypeId(), bs.getData() + 0x1, false);
			break;
		case CivData.NETHERWART:
			if (bs.getData() < 0x3) addGrowBlock(growMe, bs.getTypeId(), bs.getData() + 0x1, false);
			break;
		case CivData.MELON_STEM:
		case CivData.PUMPKIN_STEM:
			if (bs.getData() < 0x7) {
				addGrowBlock(growMe, bs.getTypeId(), bs.getData() + 0x1, false);
			} else
				if (bs.getData() == 0x7) {
					spawnMelonOrPumpkin(bs, growMe, task);
				}
			break;
		case CivData.COCOAPOD:
			if (CivData.canCocoaGrow(bs)) {
				addGrowBlock(growMe, bs.getTypeId(), CivData.getNextCocoaValue(bs), false);
			}
			break;
		case CivData.SUGARCANE:
			spawnSugarcane(bs, growMe, task);
			break;
		}
	}

	public void processGrowth(CivAsyncTask task) throws InterruptedException {
		if (this.getConstruct().isActive() == false || this.snapshot == null) return;

		// Lets let a growth rate of 100% mean 1 crop grows every 10 ticks(1/2 second)
		// Over 100% means we do more than 1 crop, under 100% means we check that probability.
		// So for example, if we have a 120% growth rate, every 10 ticks 1 crop *always* grows,
		// and another has a 20% chance to grow.
		double effectiveGrowthRate = 1.0;
		try {
			effectiveGrowthRate = (double) this.getGrowth() / (double) 100;
		} catch (NullPointerException e) {
			e.printStackTrace();
			CivLog.error("Farm at location " + this.getCCoord().toString() + " in town " + this.getNameOwner() + " Growth Error");
		}

		for (Component comp : this.getConstruct().attachedComponents) {
			if (comp instanceof ActivateOnBiome) {
				ActivateOnBiome ab = (ActivateOnBiome) comp;
				if (ab.isValidBiome(biomeName)) {
					Double val = ab.getValue();
					effectiveGrowthRate *= val;
					break;
				}
			}
		}
		this.setLastEffectiveGrowth(effectiveGrowthRate);

		int crops_per_growth_tick = (int) CivSettings.getIntegerStructure("farm.grows_per_tick");
		int numberOfCropsToGrow = (int) (effectiveGrowthRate * crops_per_growth_tick); // Since this is a double, 1.0 means 100% so int cast is # of crops
		int chanceForLast = (int) (this.getGrowth() % 100);

		this.lastGrownCrops.clear();
		this.lastGrowTickCount = numberOfCropsToGrow;
		this.lastChanceForLast = chanceForLast;
		Calendar c = Calendar.getInstance();
		this.lastGrowDate = c.getTime();
		this.growBlocks = new LinkedList<GrowBlock>();

		if (this.cropLocationCache.size() == 0) return;

		// Process number of crops that will grow this time. Select one at random
		Random rand = new Random();
		for (int i = 0; i < numberOfCropsToGrow; i++) {
			BlockCoord growMe = this.cropLocationCache.get(rand.nextInt(this.cropLocationCache.size()));

			int bsx = growMe.getX() % 16;
			int bsy = growMe.getY();
			int bsz = growMe.getZ() % 16;

			BlockSnapshot bs = new BlockSnapshot(bsx, bsy, bsz, snapshot);

			this.lastGrownCrops.add(growMe);
			growBlock(bs, growMe, task);
		}
		if (chanceForLast != 0) {
			int randInt = rand.nextInt(100);
			this.lastRandomInt = randInt;
			if (randInt < chanceForLast) {
				BlockCoord growMe = this.cropLocationCache.get(rand.nextInt(this.cropLocationCache.size()));
				int bsx = growMe.getX() % 16;
				int bsy = growMe.getY();
				int bsz = growMe.getZ() % 16;

				BlockSnapshot bs = new BlockSnapshot(bsx, bsy, bsz, snapshot);

				this.lastGrownCrops.add(growMe);
				growBlock(bs, growMe, task);
			}
		}

		task.growBlocks(this.growBlocks);
	}

	public void processMissedGrowths(boolean populate, CivAsyncTask task) {
		if (this.missedGrowthTicks > 0) {
			if (populate) {
				if (this.snapshot == null) this.snapshot = this.getChunk().getChunkSnapshot();
				this.populateCropLocationCache();
			}

			for (int i = 0; i < this.missedGrowthTicks; i++) {
				try {
					this.processGrowth(task);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
			this.missedGrowthTicks = 0;
		}
	}

	// public void addToCropLocationCache(Block b) {
	// this.cropLocationCache.put(new BlockCoord(b), (int) b.getData());
	// }

	/** Проходит по всем блокам чанка. Находит все блоки которые могут расти. И добавляет их в cropLocationCache */
	public void populateCropLocationCache() {
		this.lock.lock();
		try {
			this.cropLocationCache.clear();
			BlockSnapshot bs = new BlockSnapshot();
			if (staticCropLocationCache.isEmpty()) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						for (int y = 0; y < 256; y++) {
							bs.setFromSnapshotLocation(x, y, z, snapshot);
							if (CivData.canGrow(bs)) {
								this.cropLocationCache.add(new BlockCoord(snapshot.getWorldName(), //
										(snapshot.getX() << 4) + bs.getX(), //
										(bs.getY()), //
										(snapshot.getZ() << 4) + bs.getZ()));
							}
						}
					}
				}
			} else {
				for (BlockCoord bc : staticCropLocationCache) {
					bs.setFromSnapshotLocation(ChunkCoord.getCoordInChunk(bc.getX()), bc.getY(), ChunkCoord.getCoordInChunk(bc.getZ()), snapshot);
					if (CivData.canGrow(bs)) {
						this.cropLocationCache.add(bc);
					}
				}
			}
			CivLog.debug("cropLocationCache = " + cropLocationCache.size());
		} finally {
			this.lock.unlock();
		}
	}

	public void incrementMissedGrowthTicks() {
		this.missedGrowthTicks++;
		this.missedGrowthTicksStat++;
	}

	public void saveMissedGrowths() {
		int missedTicks = this.getMissedGrowthTicks();

		TaskMaster.asyncTask(new Runnable() {

			@Override
			public void run() {
				ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getSessionKey());

				if (entries == null || entries.size() == 0) {
					if (missedTicks > 0) {
						construct.sessionAdd(getSessionKey(), "" + missedTicks);
						return;
					}
				}

				if (missedTicks == 0)
					CivGlobal.getSessionDatabase().delete_all(getSessionKey());
				else
					CivGlobal.getSessionDatabase().update(entries.get(0).request_id, getSessionKey(), "" + missedTicks);
			}
		}, 0);
	}

	public String getSessionKey() {
		return "FarmMissedGrowth" + ":" + this.getCCoord().toString();
	}

	public void delete() {
		CivGlobal.removeFarmChunk(this.getCCoord());
		CivGlobal.getSessionDatabase().delete_all(this.getSessionKey());
	}

	public void setLastEffectiveGrowth(double effectiveGrowthRate) {
		this.lastEffectiveGrowthRate = effectiveGrowthRate;
	}

	public double getLastEffectiveGrowthRate() {
		return this.lastEffectiveGrowthRate;
	}

	public static boolean isBlockControlled(Block b) {
		switch (ItemManager.getTypeId(b)) {
		// case CivData.BROWNMUSHROOM:
		// case CivData.REDMUSHROOM:
		case CivData.COCOAPOD:
		case CivData.MELON:
		case CivData.MELON_STEM:
		case CivData.PUMPKIN:
		case CivData.PUMPKIN_STEM:
		case CivData.WHEAT:
		case CivData.CARROTS:
		case CivData.POTATOES:
		case CivData.NETHERWART:
		case CivData.SUGARCANE:
			return true;
		}

		return false;
	}
}
