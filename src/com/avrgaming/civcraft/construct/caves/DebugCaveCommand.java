package com.avrgaming.civcraft.construct.caves;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCave;
//import com.avrgaming.civcraft.construct.caves.Cave;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;

public class DebugCaveCommand extends MenuAbstractCommand {
	public DebugCaveCommand(String perentCommand) {
		super(perentCommand);
		displayName = "Cave Commands";

		add(new CustomCommand("tp").withDescription("телепорт в пещеру под указанным номером").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
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
					BlockCoord coord = new BlockCoord(player.getLocation());

					Cave cave = CivGlobal.getCave(coord.getChunkCoord());
					if (cave == null) throw new CivException("Not Found cave this location");

					player.teleport(cave.getSpawns().get("1").getLocation());
					CivMessage.sendSuccess(sender, "Teleporting to cave " + cave.getName());
				}
			}
		}));
		add(new CustomCommand("regencavechunk").withDescription("regens every chunk that has a trade good in it").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				// World world =CivCraft.mainWorld;

				// for (ChunkCoord coord : CivGlobal.tradeGoodPreGenerator.goodPicks.keySet()) {
				//
				// world.regenerateChunk(coord.getX(), coord.getZ());
				// CivMessage.send(sender, "Regened:" + coord);
				// }
			}
		}));
		add(new CustomCommand("cavegenerate").withDescription("generates caves at picked locations").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
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
		}));
		add(new CustomCommand("createcave").withDescription("[cave_id] - creates a cave here.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				if (args.length < 2) throw new CivException("Enter cave id");

				ConfigCave cave = CivSettings.caves.get(args[1]);
				if (cave == null) throw new CivException("Unknown cave id:" + args[1]);

				BlockCoord coord = new BlockCoord(getPlayer().getLocation());
				Cave.newCaveEntrance(cave.id, coord);
				CivMessage.sendSuccess(sender, "Created a " + cave.name + " here.");
			}
		}));
	}

}
