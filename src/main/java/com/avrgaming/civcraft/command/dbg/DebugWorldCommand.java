package com.avrgaming.civcraft.command.dbg;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;

public class DebugWorldCommand extends MenuAbstractCommand {

	public DebugWorldCommand(String perentCommand) {
		super(perentCommand);
		displayName = "Debug World";
		
		add(new CustomCommand("create").withDescription("[name] - creates a new test world with this name.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				String name = Commander.getNamedString(args, 0, "enter a world name");
				WorldCreator wc = new WorldCreator(name);
				wc.environment(Environment.NORMAL);
				wc.type(WorldType.FLAT);
				wc.generateStructures(false);
				World world = Bukkit.getServer().createWorld(wc);
				world.setSpawnFlags(false, false);
				CivMessage.sendSuccess(sender, "World "+name+" created.");
			}
		}));
		add(new CustomCommand("tp").withDescription("[name] teleports you to spawn at the specified world.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				String name = Commander.getNamedString(args, 0, "enter a world name");
				Player player = Commander.getPlayer(sender);
				World world = Bukkit.getWorld(name);
				player.teleport(world.getSpawnLocation());
				CivMessage.sendSuccess(sender, "Teleported to spawn at world:"+name);
			}
		}));
		add(new CustomCommand("list").withDescription("Lists worlds according to bukkit.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.sendHeading(sender, "Worlds");
				for (World world : Bukkit.getWorlds()) {
					CivMessage.send(sender, world.getName());
				}
			}
		}));
	}
	
}
