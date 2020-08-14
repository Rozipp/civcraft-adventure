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
package com.avrgaming.civcraft.command.old;

import org.bukkit.Effect;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.construct.farm.FarmChunk;
import com.avrgaming.civcraft.construct.farm.FarmGrowthSyncTask;
import com.avrgaming.civcraft.construct.farm.FarmPreCachePopulateTimer;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;

public class DebugFarmCommand extends MenuAbstractCommand {

	public DebugFarmCommand(String perentCommand) {
		super(perentCommand);
		displayName = "Farm Commands";

		add(new CustomCommand("showgrowth").withDescription("Highlight the crops that grew last tick.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ChunkCoord coord = new ChunkCoord(player.getLocation());
				FarmChunk fc = CivGlobal.getFarmChunk(coord);
				if (fc == null) throw new CivException("This is not a farm.");
				for (BlockCoord bcoord : fc.getLastGrownCrops()) {
					bcoord.getBlock().getWorld().playEffect(bcoord.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
				}
				CivMessage.sendSuccess(player, "Flashed last grown crops");
			}
		}));
		add(new CustomCommand("grow").withDescription("[x] grows ALL farm chunks x many times.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				int count = Commander.getNamedInteger(args, 0);
				for (int i = 0; i < count; i++) {
					TaskMaster.asyncTask(new FarmGrowthSyncTask(), 0);
				}
				CivMessage.sendSuccess(sender, "Grew all farms.");
			}
		}));
		add(new CustomCommand("cropcache").withDescription("show the crop cache for this plot.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ChunkCoord coord = new ChunkCoord(player.getLocation());
				FarmChunk fc = CivGlobal.getFarmChunk(coord);
				if (fc == null) throw new CivException("This is not a farm.");
				for (BlockCoord bcoord : fc.cropLocationCache) {
					bcoord.getBlock().getWorld().playEffect(bcoord.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
				}
				CivMessage.sendSuccess(player, "Flashed cached crops.");
			}
		}));
		add(new CustomCommand("unloadchunk").withDescription("[x] [z] unloads this farm chunk").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				int x = Commander.getNamedInteger(args, 0);
				int z = Commander.getNamedInteger(args, 1);
				CivCraft.mainWorld.unloadChunk(x, z);
				CivMessage.sendSuccess(sender, "Chunk " + x + "," + z + " unloaded");
			}
		}));
		add(new CustomCommand("RunCachePopulate").withDescription("Runs the crop cache task.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				TaskMaster.syncTask(new FarmPreCachePopulateTimer());
			}
		}));

	}
}
