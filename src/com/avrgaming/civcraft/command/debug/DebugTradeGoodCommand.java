package com.avrgaming.civcraft.command.debug;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.event.GoodieRepoEvent;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.populators.TradeGoodPopulator;
import com.avrgaming.civcraft.tasks.TradeGoodSignCleanupTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.TradeGoodPostGenTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;

public class DebugTradeGoodCommand extends CommandBase{

	@Override
	public void init() {
		command = "/dbg tradegood ";
		displayName = "TradeGood Commands";
		
		cs.add("repo", "repos all goods back to outpost.");
		cs.add("printgoodie", "[id] - prints the goodie in memory with this id.");
		cs.add("repogoodie", "[id] - repos the goodie with id.");
		cs.add("regentradegoodchunk", "regens every chunk that has a trade good in it");
		cs.add("regenmobspawnerchunk", "regens every chunk that has a Mob Spawner in it");
		cs.add("tradegenerate", "generates trade goods at picked locations");
		cs.add("mobspawnergenerate", "generates mob spawners at picked locations");
		cs.add("createtradegood", "[good_id] - creates a trade goodie here.");
		cs.add("createmobspawner", "[spawner_id] - creates a mob spawner here.");
		cs.add("cleartradesigns", "clears extra trade signs above trade outpots");
	}
	public void repogoodie_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter the id of the goodie you want to repo.");
		}
		for (BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
			if (goodie.getId() == Integer.valueOf(args[1])) {
				CivMessage.send(sender, "Repo'd Goodie " + goodie.getId() + " (" + goodie.getDisplayName() + ")");
				goodie.replenish();
				return;
			}
		}

	}

	public void printgoodie_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter the id of the goodie you want to inspect.");
		}

		for (BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
			if (goodie.getId() == Integer.valueOf(args[1])) {
				CivMessage.sendHeading(sender, "Goodie " + goodie.getId() + " (" + goodie.getDisplayName() + ")");

				if (goodie.getItem() != null) {
					CivMessage.send(sender,
							"Item: " + goodie.getItem().getUniqueId() + " loc:" + goodie.getItem().getLocation());
				} else {
					CivMessage.send(sender, "Item: null");
				}

				if (goodie.getFrame() != null) {
					CivMessage.send(sender,
							"Frame: " + goodie.getFrame().getUUID() + " loc:" + goodie.getFrame().getLocation());
				} else {
					CivMessage.send(sender, "Frame: null");
				}

				if (goodie.getHolder() != null) {
					CivMessage.send(sender, "Holder: " + goodie.getHolder().toString());
				} else {
					CivMessage.send(sender, "holder: null");
				}

				org.bukkit.inventory.ItemStack stack = goodie.getStack();
				if (stack != null) {
					CivMessage.send(sender, "Stack: " + stack.toString());
				} else {
					CivMessage.send(sender, "Stack: null");
				}
				return;
			}
		}
		CivMessage.send(sender, "No goodie found.");
	}

	public void cleartradesigns_cmd() throws CivException {
		CivMessage.send(sender, "Starting task");

		if (args.length < 3) {
			throw new CivException("bad arg count");
		}

		try {
			Integer xoff = Integer.valueOf(args[1]);
			Integer zoff = Integer.valueOf(args[2]);
			TaskMaster.syncTask(new TradeGoodSignCleanupTask(getPlayer().getName(), xoff, zoff));

		} catch (NumberFormatException e) {
			throw new CivException("Bad number format");
		}

	}

	public void tradegenerate_cmd() throws CivException {
		String playerName;

		if (sender instanceof Player) {
			playerName = sender.getName();
		} else {
			playerName = null;
		}

		CivMessage.send(sender, "Starting Trade Generation task...");
		TaskMaster.asyncTask(new TradeGoodPostGenTask(playerName, 0), 0);
	}
	public void regentradegoodchunk_cmd() {

		World world = Bukkit.getWorld("world");

		for (ChunkCoord coord : CivGlobal.tradeGoodPreGenerator.goodPicks.keySet()) {

			world.regenerateChunk(coord.getX(), coord.getZ());
			CivMessage.send(sender, "Regened:" + coord);
		}
	}
	public void createtradegood_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter trade goodie id");
		}

		ConfigTradeGood good = CivSettings.goods.get(args[1]);
		if (good == null) {
			throw new CivException("Unknown trade good id:" + args[1]);
		}

		BlockCoord coord = new BlockCoord(getPlayer().getLocation());
		TradeGoodPopulator.buildTradeGoodie(good, coord, getPlayer().getLocation().getWorld(), false);
		CivMessage.sendSuccess(sender, "Created a " + good.name + " here.");
	}

	public void repo_cmd() {
		GoodieRepoEvent.repoProcess();
	}
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}
}
