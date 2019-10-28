package com.avrgaming.civcraft.threading.tasks;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

public class BuildUndoTask implements Runnable {
	private String undoTemplatePath;
	private BlockCoord cornerBlock;
	private int savedBlockCount;

	private final int MAX_BLOCKS_PER_TICK = 300;
	private final int DELAY_SPEED = 100;

	public BuildUndoTask(String undoTemplateId, BlockCoord cornerBlock, int savedBlockCount) {
		this.undoTemplatePath = Template.getUndoFilePath(undoTemplateId);
		this.cornerBlock = cornerBlock;
		this.savedBlockCount = savedBlockCount;
	}

	@Override
	public void run() {
		Template undo_tpl;
		Queue<SimpleBlock> syncBlockQueue = new LinkedList<SimpleBlock>();
		try {
			undo_tpl = new Template(Template.getUndoFilePath(this.cornerBlock.toString()));
			/* For 1.0 Templates, SimpleBlocks are inside 3D array called 'blocks' */
			int builtBlockCount = 0;
			for (int y = undo_tpl.size_y - 1; y >= 0; y--) {
				for (int x = 0; x < undo_tpl.size_x; x++) {
					for (int z = 0; z < undo_tpl.size_z; z++) {
						SimpleBlock sb = undo_tpl.blocks[x][y][z];
						builtBlockCount++;
						/* We're resuming an undo task after reboot and this block is already built. Or This block is restricted */
						if (builtBlockCount < savedBlockCount) return;
						if (CivSettings.restrictedUndoBlocks.contains(sb.getMaterial())) sb.setType(CivData.AIR);

						/* Convert relative template x,y,z to real x,y,z in world. */
						sb.x = x + cornerBlock.getX();
						sb.y = y + cornerBlock.getY();
						sb.z = z + cornerBlock.getZ();
						sb.worldname = cornerBlock.getWorldname();
						/* Add block to sync queue, will be built on next sync tick. */
						syncBlockQueue.add(sb);

						if (builtBlockCount > MAX_BLOCKS_PER_TICK) {
							/* Wait for a period of time. */
							int timeleft = DELAY_SPEED;
							while (timeleft > 0) {
								int min = Math.min(10000, timeleft);
								Thread.sleep(min);
								timeleft -= 10000;
							}
							SyncBuildUpdateTask.queueSimpleBlock(syncBlockQueue);
							syncBlockQueue.clear();
							builtBlockCount = 0;
						}
					}
				}
			}
			/* Build last remaining blocks. */
			SyncBuildUpdateTask.queueSimpleBlock(syncBlockQueue);
			syncBlockQueue.clear();
			Template.deleteFilePath(undoTemplatePath);
		} catch (IOException | CivException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}