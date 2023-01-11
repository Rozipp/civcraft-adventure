package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

import java.util.LinkedList;
import java.util.Queue;

public class BuildTemplateTask implements Runnable {
	private final Template tpl;
	private final BlockCoord cornerBlock;

	private final Queue<SimpleBlock> syncBlockQueue = new LinkedList<>();
	int builtBlockCount = 0;
	boolean buildAir;
	boolean delete;

	private final int MAX_BLOCKS_PER_TICK = CivSettings.getIntBase("update_limit_for_sync_build_task");
	private final int SLEEP = 100; /* Wait for a period of time. */

	public BuildTemplateTask(Template tpl, BlockCoord cornerBlock, boolean buildAir) {
		this(tpl, cornerBlock, buildAir,false);
	}

	public BuildTemplateTask(Template tpl, BlockCoord cornerBlock, boolean buildAir, boolean delete) {
		this.tpl = tpl;
		this.cornerBlock = cornerBlock;
		this.buildAir = buildAir;
		this.delete = delete;
	}

	private void oneLayer(Template tpl, int size_x, int y, int size_z) throws InterruptedException {
		if (tpl == null)
			for (int x = 0; x < size_x; x++) {
				for (int z = 0; z < size_z; z++) {
					SimpleBlock sb = new SimpleBlock(CivData.BARRIER, 0);
					syncBlockQueue.add(new SimpleBlock(cornerBlock, sb));
				}
			}
		else
			for (SimpleBlock sb : tpl.blocks.get(y)) {
				/* We're resuming an undo task after reboot and this block is already built. Or This block is restricted */
				SimpleBlock sbnew = new SimpleBlock(cornerBlock, sb); 
				if (CivSettings.restrictedUndoBlocks.contains(sb.getMaterial())) sbnew.setType(CivData.AIR);
				syncBlockQueue.add(sbnew);
			}
		builtBlockCount += syncBlockQueue.size();
		if (builtBlockCount > MAX_BLOCKS_PER_TICK) {
			SyncBuildUpdateTask.queueSimpleBlock(syncBlockQueue);
			syncBlockQueue.clear();
			builtBlockCount = 0;
			Thread.sleep(SLEEP);
		}
	}

	@Override
	public void run() {
		try {
			oneLayer(null, tpl.size_x, tpl.size_y - 1, tpl.size_z); // Для того что бы не сыпался песок сверху. Верхний
																	// слой устанавливается на барьер.
			for (int y = tpl.size_y - 2; y >= 0; y--) {
				oneLayer(tpl, tpl.size_x, y, tpl.size_z);
			}
			oneLayer(tpl, tpl.size_x, tpl.size_y - 1, tpl.size_z); // убераем верхний слой

			/* Build last remaining blocks. */
			SyncBuildUpdateTask.queueSimpleBlock(syncBlockQueue);
			syncBlockQueue.clear();
			
			if (buildAir) tpl.buildAirBlocks(cornerBlock);
			if (delete) Template.deleteFilePath(tpl.filepath);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}