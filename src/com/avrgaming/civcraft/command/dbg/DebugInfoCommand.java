package com.avrgaming.civcraft.command.dbg;

import org.bukkit.Chunk;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.CivInWorldTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.AsciiMap;
import com.avrgaming.civcraft.util.ItemManager;

public class DebugInfoCommand extends MenuAbstractCommand {

	public DebugInfoCommand(String perentCommand) {
		super(perentCommand);
		displayName = "Show data base info Commands";
		
		add(new CustomCommand("blockinfo").withDescription("[x] [y] [z] shows block info for this block.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				int x = Commander.getNamedInteger(args, 0);
				int y = Commander.getNamedInteger(args, 1);
				int z = Commander.getNamedInteger(args, 2);
				Block b = CivCraft.mainWorld.getBlockAt(x, y, z);
				CivMessage.send(sender, "type:" + ItemManager.getTypeId(b) + " data:" + ItemManager.getData(b) + " name:" + b.getType().name());
			}
		}));
		add(new CustomCommand("biomehere").withDescription("- shows you biome info where you're standing.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Biome biome = player.getWorld().getBiome(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
				CivMessage.send(player, "Got biome:" + biome.name());
			}
		}));
		add(new CustomCommand("resident").withDescription("[name] - prints out the resident identified by name.").withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Resident res = Commander.getNamedResident(args, 0);
				CivMessage.sendHeading(sender, "Resident " + res.getName());
				CivMessage.send(sender, "id: " + res.getId() + " lastOnline: " + res.getLastOnline() + " registered: " + res.getRegistered());
				CivMessage.send(sender, "debt: " + res.getTreasury().getDebt());
			}
		}));
		add(new CustomCommand("town").withDescription("[name] - prints out the town identified by name.").withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Town town = Commander.getNamedTown(args, 0);
				CivMessage.sendHeading(sender, "Town " + town.getName());
				CivMessage.send(sender, "id:" + town.getId() + " level: " + town.SM.getLevel());
			}
		}));
		add(new CustomCommand("civ").withDescription("[name] prints out civ info.").withTabCompleter(new CivInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Civilization civ = Commander.getNamedCiv(args, 0);
				CivMessage.sendHeading(sender, "Civ " + civ.getName());
				CivMessage.send(sender, "id:" + civ.getId() + " debt: " + civ.getTreasury().getDebt() + " balance:" + civ.getTreasury().getBalance());
			}
		}));
		add(new CustomCommand("map").withDescription("shows a town chunk map of the current area.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				CivMessage.send(player, AsciiMap.getMapAsString(player.getLocation()));
			}
		}));
		add(new CustomCommand("townchunk").withDescription(" gets the town chunk you are standing in and prints it.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Player player = Commander.getPlayer(sender);
				final TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
				if (tc == null) {
					CivMessage.send(sender, "No town chunk found here.");
					return;
				}
				CivMessage.send(sender, "id:" + tc.getId() + " coord:" + tc.getChunkCoord());
			}
		}));
		add(new CustomCommand("culturechunk").withDescription("gets the culture chunk you are standing in and prints it.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Player player = Commander.getPlayer(sender);
				final CultureChunk cc = CivGlobal.getCultureChunk(player.getLocation());
				if (cc == null) {
					CivMessage.send(sender, "No culture chunk found here.");
					return;
				}
				CivMessage.send(sender, "loc:" + cc.getChunkCoord() + " town:" + cc.getTown().getName() + " civ:" + cc.getCiv().getName() + " distanceToNearest:" + cc.getDistanceToNearestEdge(cc.getTown().savedEdgeBlocks));
			}
		}));
		add(new CustomCommand("showentity").withDescription("shows entity ids in this chunk.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				final Player player = Commander.getPlayer(sender);
				final Chunk chunk = player.getLocation().getChunk();
				Entity[] entities;
				for (int length = (entities = chunk.getEntities()).length, i = 0; i < length; ++i) {
					final Entity entity = entities[i];
					CivMessage.send(player, "E:" + entity.getType().name() + " UUID:" + entity.getUniqueId().toString());
					CivLog.info("E:" + entity.getType().name() + " UUID:" + entity.getUniqueId().toString());
				}
			}
		}));
		add(new CustomCommand("stackinhand").withDescription("- show information about item in you hand.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack is = player.getInventory().getItemInMainHand();
				CivMessage.send(player, "Got is typeid: " + ItemManager.getTypeId(is) + ",  dataid: " + ItemManager.getData(is));
			}
		}));
		add(new CustomCommand("setProperty").withDescription("- show setProperty").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack is = player.getInventory().getItemInMainHand();
				String key = Commander.getNamedString(args, 0, "Введите ключ");
				String ss = Commander.getNamedString(args, 1, "Введите значение ключа");
				player.getInventory().removeItem(is);
				is = ItemManager.setProperty(is, key, ss);
				player.getInventory().addItem(is);
				CivMessage.send(player, "setCivCraftProperty complited");
			}
		}));
		add(new CustomCommand("getAllProperty").withDescription("- show getAllProperty").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack is = player.getInventory().getItemInMainHand();
				CivMessage.send(player, ItemManager.getAllProperty(is));
			}
		}));
		add(new CustomCommand("printAllTask").withDescription("- show getAllProperty").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				TaskMaster.printAllTask();
				CivMessage.send(player, "printAllTask in consol");
			}
		}));
	}

}
