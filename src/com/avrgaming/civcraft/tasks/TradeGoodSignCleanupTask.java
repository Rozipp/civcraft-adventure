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
package com.avrgaming.civcraft.tasks;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class TradeGoodSignCleanupTask implements Runnable {

	String playerName;
	int xoff = 0;
	int zoff = 0;

	public TradeGoodSignCleanupTask(String playername, int xoff, int zoff) {
		this.playerName = playername;
		this.xoff = xoff;
		this.zoff = zoff;
	}

	@Override
	public void run() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			e.printStackTrace();
			return;
		}

		int count = 0;
		int i = 0;

		// for(ChunkCoord coord : CivGlobal.preGenerator.goodPicks.keySet()) {
		for (TradeGood tg : CivGlobal.getTradeGoods()) {
			
			//TODO ДАльше полный бред. НАдо полностью переписать
			ChunkCoord chunkCoord = new ChunkCoord(tg.getCoord().getRelative(xoff, 0, zoff));
			int centerX = (chunkCoord.getX() << 4) + 8;
			int centerZ = (chunkCoord.getZ() << 4) + 8;
			int centerY = CivCraft.mainWorld.getHighestBlockYAt(centerX, centerZ);

			BlockCoord bcoord = new BlockCoord(chunkCoord, 8, centerY, 8);
			for (int y = centerY; y < 256; y++) {
				Block top = bcoord.getBlockRelative(0, y, 0);
				ItemManager.setTypeId(top, CivData.AIR);
				ItemManager.setData(top, 0, true);
				y++;

				top = top.getRelative(BlockFace.NORTH);
				if (ItemManager.getTypeId(top) == CivData.WALL_SIGN || ItemManager.getTypeId(top) == CivData.SIGN) {
					count++;
				}
				ItemManager.setTypeId(top, CivData.AIR);
				ItemManager.setData(top, 0, true);

				top = top.getRelative(BlockFace.SOUTH);
				if (ItemManager.getTypeId(top) == CivData.WALL_SIGN || ItemManager.getTypeId(top) == CivData.SIGN) {
					count++;
					ItemManager.setTypeId(top, CivData.AIR);
					ItemManager.setData(top, 0, true);
				}

				top = top.getRelative(BlockFace.EAST);
				if (ItemManager.getTypeId(top) == CivData.WALL_SIGN || ItemManager.getTypeId(top) == CivData.SIGN) {
					count++;
					ItemManager.setTypeId(top, CivData.AIR);
					ItemManager.setData(top, 0, true);

				}

				top = top.getRelative(BlockFace.WEST);
				if (ItemManager.getTypeId(top) == CivData.WALL_SIGN || ItemManager.getTypeId(top) == CivData.SIGN) {
					count++;
					ItemManager.setTypeId(top, CivData.AIR);
					ItemManager.setData(top, 0, true);
				}
			}

			i++;
			if ((i % 80) == 0) {
				CivMessage.send(player, "Goodie:" + i + " cleared " + count + " signs...");
				// TaskMaster.syncTask(new TradeGoodPostGenTask(playerName, (i)));
				// return;
			}

		}

		CivMessage.send(player, CivSettings.localize.localizedString("Finished"));
	}

}
