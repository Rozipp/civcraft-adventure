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
package com.avrgaming.civcraft.command;

import java.util.Queue;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ItemManager;

public class ReportPlayerInventoryTask implements Runnable {

	Queue<Player> players;
	CommandSender sender;

	public ReportPlayerInventoryTask(CommandSender sender, Queue<Player> players) {
		this.sender = sender;
		this.players = players;
	}

	private int countItem(ItemStack[] stacks, int id) {
		int total = 0;
		for (ItemStack stack : stacks) {
			if (stack == null) continue;
			if (ItemManager.getTypeId(stack) == id) total += stack.getAmount();
		}
		return total;
	}

	@Override
	public void run() {
		CivMessage.sendError(sender, "Deprecated do not use anymore.. or fix it..");
		for (int i = 0; i < 20; i++) {
			Player pl = players.poll();
			if (pl == null) {
				sender.sendMessage("Done.");
				return;
			}

			try {
				int diamondBlocks = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.DIAMOND_BLOCK));
				int diamonds = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.DIAMOND));
				int goldBlocks = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.GOLD_BLOCK));
				int gold = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.GOLD_INGOT));
				int emeraldBlocks = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.EMERALD_BLOCK));
				int emeralds = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.EMERALD));
				int diamondOre = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.DIAMOND_ORE));
				int goldOre = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.GOLD_ORE));
				int emeraldOre = countItem(pl.getEnderChest().getContents(), ItemManager.getId(Material.EMERALD_ORE));

				String out = pl.getName() + ": DB:" + diamondBlocks + " EB:" + emeraldBlocks + " GB:" + goldBlocks + " D:" + diamonds + " E:" + emeralds + " G:" + gold + " DO:" + diamondOre + " EO:" + emeraldOre + " GO:" + goldOre;
				if (diamondBlocks != 0 || diamonds != 0 || goldBlocks != 0 || gold != 0 || emeraldBlocks != 0 || emeralds != 0 || diamondOre != 0 || goldOre != 0 || emeraldOre != 0) {
					CivMessage.send(sender, out);
					CivLog.info("REPORT:" + out);
				}

				diamondBlocks = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.DIAMOND_BLOCK));
				diamonds = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.DIAMOND));
				goldBlocks = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.GOLD_BLOCK));
				gold = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.GOLD_INGOT));
				emeraldBlocks = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.EMERALD_BLOCK));
				emeralds = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.EMERALD));
				diamondOre = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.DIAMOND_ORE));
				goldOre = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.GOLD_ORE));
				emeraldOre = countItem(pl.getInventory().getContents(), ItemManager.getId(Material.EMERALD_ORE));

				String out2 = pl.getName() + ": DB:" + diamondBlocks + " EB:" + emeraldBlocks + " GB:" + goldBlocks + " D:" + diamonds + " E:" + emeralds + " G:" + gold + " DO:" + diamondOre + " EO:" + emeraldOre + " GO:" + goldOre;
				if (diamondBlocks != 0 || diamonds != 0 || goldBlocks != 0 || gold != 0 || emeraldBlocks != 0 || emeralds != 0 || diamondOre != 0 || goldOre != 0 || emeraldOre != 0) {
					CivMessage.send(sender, out2);
					CivLog.info("REPORT:" + out2);
				}
			} catch (Exception e) {
				CivLog.info("REPORT: " + pl.getName() + " EXCEPTION:" + e.getMessage());
			}
		}
		TaskMaster.syncTask(new ReportPlayerInventoryTask(sender, players));
	}

}
