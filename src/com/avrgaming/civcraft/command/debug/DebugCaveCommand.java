package com.avrgaming.civcraft.command.debug;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCave;
import com.avrgaming.civcraft.construct.Cave;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;

public class DebugCaveCommand extends CommandBase {
	@Override
	public void init() {
		command = "/dbg cave ";
		displayName = "Cave Commands";

		cs.add("tp", "телепорт в пещеру под указанным номером");
		cs.add("regencavechunk", "regens every chunk that has a trade good in it");
		cs.add("cavegenerate", "generates caves at picked locations");
		cs.add("createcave", "[cave_id] - creates a cave here.");
	}

	public void tp_cmd() throws CivException {
		Player player = this.getPlayer();

		World world = player.getWorld();

		if (world.equals(CivCraft.cavesWorld)) {
			Resident res = CivGlobal.getResident(player);
			Location loc;
			if (res.getTown() != null)
				loc = res.getTown().getCityhall().getRandomRespawnPoint().getLocation();
			else
				loc = CivCraft.mainWorld.getSpawnLocation();
			player.teleport(loc);
		}
		if (world.equals(CivCraft.mainWorld)) {
			BlockCoord coord = new BlockCoord(getPlayer().getLocation());

			Cave cave = CivGlobal.getCave(coord.getChunkCoord());
			if (cave == null) throw new CivException("Not Found cave this location");

			player.teleport(cave.getSpawns().get("1").getLocation());
			CivMessage.sendSuccess(sender, "Teleporting to cave " + cave.getName());
		}
	}

	public void cavegenerate_cmd() throws CivException {
		// String playerName;
		//
		// if (sender instanceof Player) {
		// playerName = sender.getName();
		// } else {
		// playerName = null;
		// }

		CivMessage.send(sender, "Starting Trade Generation task...");
		// TaskMaster.asyncTask(new TradeGoodPostGenTask(playerName, 0), 0);
	}

	public void regencavechunk_cmd() {

		// World world =CivCraft.mainWorld;

		// for (ChunkCoord coord : CivGlobal.tradeGoodPreGenerator.goodPicks.keySet()) {
		//
		// world.regenerateChunk(coord.getX(), coord.getZ());
		// CivMessage.send(sender, "Regened:" + coord);
		// }
	}

	public void createcave_cmd() throws Exception {
		if (args.length < 2) {
			throw new CivException("Enter cave id");
		}

		ConfigCave cave = CivSettings.caves.get(args[1]);
		if (cave == null) {
			throw new CivException("Unknown cave id:" + args[1]);
		}

		BlockCoord coord = new BlockCoord(getPlayer().getLocation());
		Cave.newCaveEntrance(cave.id, coord);
		CivMessage.sendSuccess(sender, "Created a " + cave.name + " here.");
	}

	public void repo_cmd() {
//		GoodieRepoEvent.repoProcess();
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
