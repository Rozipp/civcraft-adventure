/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.dbg;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.TownInCivTaber;
import com.avrgaming.civcraft.command.taber.TownInWorldTaber;
import com.avrgaming.civcraft.construct.constructs.Cannon;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.timers.LagSimulationTimer;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;

public class DebugTestCommand extends MenuAbstractCommand {

	/* Here we'll build a collection of integration tests that can be run on server start to verify everything is working. */

	public DebugTestCommand(String perentCommand) {
		super(perentCommand);
		displayName = "Test Commands";

		add(new CustomCommand("setlag").withDescription("[tps] - tries to set the tps to this amount to simulate lag.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Integer tps = Commander.getNamedInteger(args, 0);
				TaskMaster.syncTimer("lagtimer", new LagSimulationTimer(tps), 0);
				CivMessage.sendSuccess(sender, "Let the lagging begin.");
			}
		}));
		add(new CustomCommand("getitem").withDescription("[umid] - give umid material.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String umid = Commander.getNamedString(args, 0, "введите umid предмета");
				ItemStack stack = ItemManager.createItemStack(umid, 1);
				if (stack == null) {
					CivMessage.sendSuccess(sender, "Item not found");
					return;
				}
				player.getInventory().addItem(stack);
				CivMessage.sendSuccess(sender, "Item added");
			}
		}));
		add(new CustomCommand("firework").withDescription("fires off a firework here.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				FireworkEffectPlayer fw = new FireworkEffectPlayer();
				try {
					fw.playFirework(player.getWorld(), player.getLocation(), FireworkEffect.builder().withColor(Color.RED).flicker(true).with(Type.BURST).build());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}));
		add(new CustomCommand("flashedges").withDescription("[town] flash edge blocks for town.").withTabCompleter(new TownInCivTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				for (TownChunk chunk : town.savedEdgeBlocks) {
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							Block b = CivCraft.mainWorld.getHighestBlockAt(((chunk.getChunkCoord().getX() + x << 4) + x), ((chunk.getChunkCoord().getZ() << 4) + z));
							CivCraft.mainWorld.playEffect(b.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
						}
					}
				}
				CivMessage.sendSuccess(sender, "flashed");
			}
		}));
		add(new CustomCommand("fakeresidents").withDescription("[town] [count] - Adds this many fake residents to a town.").withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				Integer count = Commander.getNamedInteger(args, 1);
				for (int i = 0; i < count; i++) {
					SecureRandom random = new SecureRandom();
					String name = (new BigInteger(130, random).toString(32));
					try {
						Resident fake = new Resident(UUID.randomUUID(), "RANDOM_" + name);
						town.addResident(fake);
						town.addFakeResident(fake);
					} catch (AlreadyRegisteredException | InvalidNameException e) {}
				}
				CivMessage.sendSuccess(sender, "Added " + count + " residents.");
			}
		}));
		add(new CustomCommand("clearresidents").withDescription("[town] - clears this town of it's random residents.").withTabCompleter(new TownInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Town town = Commander.getNamedTown(args, 0);
				ArrayList<Resident> removeUs = new ArrayList<Resident>();
				for (Resident resident : town.getResidents()) {
					if (resident.getName().startsWith("RANDOM_")) removeUs.add(resident);
				}
				for (Resident resident : removeUs) {
					town.removeResident(resident);
				}
			}
		}));
		add(new CustomCommand("cannon").withDescription("builds a war cannon.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Cannon.newCannon(player);
				CivMessage.sendSuccess(player, "built cannon.");
			}
		}));
	}

}
