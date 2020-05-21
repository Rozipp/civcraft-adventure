package com.avrgaming.civcraft.structurevalidation;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructLayer;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;

public class StructureValidator implements Runnable {

	public static double validPercentRequirement = 0.8;
	private static boolean enable = false;

	private Player player;
	private Construct сonstruct = null;
	private BlockCoord corner = null;
	private CallbackInterface callback = null;
	private Template tpl = null;
	private HashMap<ChunkCoord, ChunkSnapshot> chunks = new HashMap<ChunkCoord, ChunkSnapshot>();
	
	/** Only validate a single structure at a time. */
	private static ReentrantLock validationLock = new ReentrantLock();
	/** Private tasks we'll reuse. */
	private static SyncLoadSnapshotsFromLayer layerLoadTask = new SyncLoadSnapshotsFromLayer();

	

	private static class SyncLoadSnapshotsFromLayer implements Runnable {
		public List<SimpleBlock> bottomLayer;
		public StructureValidator notifyTask;

		public SyncLoadSnapshotsFromLayer() {
		}

		@Override
		public void run() {
			/* Grab all of the chunk snapshots and go into async mode. */
			notifyTask.chunks.clear();

			for (SimpleBlock sb : bottomLayer) {
				Block next = notifyTask.corner.getBlock().getRelative(sb.x, notifyTask.corner.getY(), sb.z);
				ChunkCoord coord = new ChunkCoord(next.getLocation());
				if (notifyTask.chunks.containsKey(coord)) continue;
				notifyTask.chunks.put(coord, next.getChunk().getChunkSnapshot());
			}

			synchronized (notifyTask) {
				notifyTask.notify();
			}
		}
	}

	public static double getReinforcementRequirementForLevel(int level) {
		if (level > 10) return validPercentRequirement * 0.3;
		if (level > 40) return validPercentRequirement * 0.1;
		return validPercentRequirement;
	}

	public static boolean isEnabled() {
		if (enable) return true;
		try {
			String enabledStr = CivSettings.getString(CivSettings.civConfig, "global.structure_validation");
			enable = enabledStr.equalsIgnoreCase("true");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			enable = false;
		}
		return enable;
	}

	public StructureValidator(Player player, Construct bld, CallbackInterface callback) {
		this.player = player;
		this.сonstruct = bld;
		this.corner = this.сonstruct.getCorner();
		this.tpl = this.сonstruct.getTemplate();
		this.callback = callback;
	}

	public void validate(HashMap<ChunkCoord, ChunkSnapshot> chunks, List<SimpleBlock> bottomLayer) {
		int checkedLevelCount = 0;
		boolean valid = true;
		String message = "";

		for (int y = corner.getY() - 1; y > 0; y--) {
			checkedLevelCount++;
			double totalBlocks = 0;
			double reinforcementValue = 0;

			for (SimpleBlock sb : bottomLayer) {
				/* We only want the bottom layer of a template to be checked. */
				if (sb.getType() == CivData.AIR) {
					continue;
				}

				try {
					int absX;
					int absZ;
					absX = corner.getX() + sb.x;
					absZ = corner.getZ() + sb.z;

					int type = BuildableStatic.getBlockIDFromSnapshotMap(chunks, absX, y, absZ, corner.getWorldname());
					totalBlocks++;
					reinforcementValue += BuildableStatic.getReinforcementValue(type);
				} catch (CivException e) {
					e.printStackTrace();
					break;
				}
			}

			double percentValid = reinforcementValue / totalBlocks;
			сonstruct.layerValidPercentages.put(y, new ConstructLayer((int) reinforcementValue, (int) totalBlocks));

			if (valid) {
				if (percentValid < getReinforcementRequirementForLevel(checkedLevelCount)) {
					DecimalFormat df = new DecimalFormat();
					message = CivSettings.localize.localizedString("var_structureValidator_layerInvalid", y, df.format(percentValid * 100), (reinforcementValue + "/" + totalBlocks), df.format(validPercentRequirement * 100));
					valid = false;
					continue;
				}
			}
		}

		сonstruct.validated = true;
		сonstruct.invalidLayerMessage = message;
		сonstruct.valid = valid;
	}

	@Override
	public void run() {
		if (!isEnabled()) {
			сonstruct.validated = true;
			сonstruct.setValid(true);
			return;
		}

		/* Wait for validation lock to open. */
		validationLock.lock();

		try {
			/* Copy over instance variables to static variables. */
			if (сonstruct.isIgnoreFloating()) {
				сonstruct.validated = true;
				сonstruct.setValid(true);
				return;
			}

			List<SimpleBlock> bottomLayer = tpl.getBlocksForLayer(0);

			/* Launch sync layer load task. */
			layerLoadTask.bottomLayer = bottomLayer;
			layerLoadTask.notifyTask = this;
			TaskMaster.syncTask(layerLoadTask);

			/* Wait for sync task to notify us to continue. */
			synchronized (this) {
				this.wait();
			}

			this.validate(chunks, bottomLayer);

			if (player != null) {
				CivMessage.sendError(player, сonstruct.invalidLayerMessage);
				if (player.isOp()) {
					CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("structureValidator_isOP"));
					сonstruct.valid = true;
				}
				if (сonstruct.valid) {
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("structureValidator_isValid"));
					сonstruct.invalidLayerMessage = "";
				}
			}
			if (callback != null) callback.execute(player.getName());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			validationLock.unlock();
		}
	}
}
