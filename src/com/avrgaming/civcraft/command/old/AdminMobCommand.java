package com.avrgaming.civcraft.command.old;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.mythicmob.MobStatic;

import io.lumine.xikage.mythicmobs.MythicMobs;

public class AdminMobCommand extends MenuAbstractCommand {

	public AdminMobCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("cmd_mob_managament");

		add(new CustomCommand("count").withDescription("Shows mob totals globally")
		// .withExecutor(new CustomExecutor() {
		// @Override
		// public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
		// Player player = Commander.getPlayer(sender);
		// HashMap<String, Integer> amounts = new HashMap<String, Integer>();
		// int total = CommonCustomMob.customMobs.size();
		// CommonCustomMob.customMobs.values().forEach((mob) -> {
		// Integer count = amounts.get(mob.getClass().getSimpleName());
		// if (count == null) {
		// count = 0;
		// }
		//
		// amounts.put(mob.getClass().getSimpleName(), count + 1);
		// });
		//
		// CivMessage.sendHeading(player, "Custom Mob Counts");
		// CivMessage.send(player, CivColor.LightGray + "Red mobs are over their count limit for this area and should no longer spawn.");
		// for (Iterator<String> it = amounts.keySet().iterator(); it.hasNext();) {
		// String mob = it.next();
		// int count = amounts.get(mob);
		// LinkedList<Entity> entities = EntityProximity.getNearbyEntities(null, player.getLocation(), MobSpawnerTimer.MOB_AREA,
		// EntityCreature.class);
		// if (entities.size() > MobSpawnerTimer.MOB_AREA_LIMIT) {
		// CivMessage.send(player, CivColor.Red + mob + ": " + CivColor.Rose + count);
		// } else {
		// CivMessage.send(player, CivColor.Green + mob + ": " + CivColor.LightGreen + count);
		// }
		// }
		// CivMessage.send(player, CivColor.Green + "Total Mobs:" + CivColor.LightGreen + total);
		// }
		// })
		);
		add(new CustomCommand("disable").withDescription("[name] Disables this mob from spawning").withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (String mob : MythicMobs.inst().getMobManager().getMobNames()) {
					String name = mob.replace(" ", "_");
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String name = Commander.getNamedString(args, 0, "Enter a mob name");
				MobStatic.disableCustomMobs.add(name);
				CivMessage.sendSuccess(player, "Add " + name + " to DisabledCustomMobs");
			}
		}));
		add(new CustomCommand("enable").withDescription("[name] Enables this mob to spawn.").withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (String mob : MythicMobs.inst().getMobManager().getMobNames()) {
					String name = mob.replace(" ", "_");
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String name = Commander.getNamedString(args, 0, "Enter a mob name").replace("_", " ");
				if (MobStatic.disableCustomMobs.contains(name)) MobStatic.disableCustomMobs.remove(name);
				CivMessage.sendSuccess(player, "Remove" + name + " to DisabledCustomMobs");
			}
		}));
		add(new CustomCommand("showdisables").withDescription("Show disableCustomMobs.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String res = "";
				for (String s : MobStatic.disableCustomMobs) {
					res = res + s + ", ";
				}
				CivMessage.sendSuccess(player, res);
			}
		}));
		add(new CustomCommand("killall").withDescription("Removes all of these mobs from the game instantly.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				int count = MobStatic.despawnAll();
				CivMessage.sendSuccess(player, "Removed " + count + " mobs of type ");// + name);
			}
		}));
	}

}