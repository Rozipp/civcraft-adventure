package com.avrgaming.civcraft.commandold;

import org.bukkit.Chunk;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ItemManager;

public class DebugShowCommand extends CommandBase {

	@Override
	public void init() {
		command = "/dbg show";
		displayName = "Show data base info Commands";

		cs.add("resident", "[name] - prints out the resident identified by name.");
		cs.add("town", "[name] - prints out the town identified by name.");
		cs.add("civ", "[name] prints out civ info.");
		cs.add("townchunk", " gets the town chunk you are standing in and prints it.");
		cs.add("culturechunk", "gets the culture chunk you are standing in and prints it.");
		cs.add("showentity", "shows entity ids in this chunk.");
		cs.add("biomehere", "- shows you biome info where you're standing.");
		cs.add("stackinhand", "- show information about item in you hand.");
		cs.add("setProperty", "- show setProperty");
		cs.add("getAllProperty", "- show getAllProperty");
		cs.add("printAllTask", "- show getAllProperty");
	}

	public void printAllTask_cmd() throws CivException {
		Player player = this.getPlayer();
		TaskMaster.printAllTask();
		CivMessage.send(player, "printAllTask in consol");
	}
	
	public void getAllProperty_cmd() throws CivException {
		Player player = this.getPlayer();
		ItemStack is = player.getInventory().getItemInMainHand();
		CivMessage.send(player, ItemManager.getAllProperty(is));
	}

	public void setProperty_cmd() throws CivException {
		Player player = this.getPlayer();
		ItemStack is = player.getInventory().getItemInMainHand();

		String key = getNamedString(1, "Введите ключ");
		String ss = getNamedString(2, "Введите значение ключа");

		player.getInventory().removeItem(is);
		is = ItemManager.setProperty(is, key, ss);

		player.getInventory().addItem(is);

		CivMessage.send(player, "setCivCraftProperty complited");
	}
	
	public void stackinhand_cmd() throws CivException {
		Player player = this.getPlayer();
		ItemStack is = player.getInventory().getItemInMainHand();
		CivMessage.send(player, "Got is typeid: " + ItemManager.getTypeId(is) + ",  dataid: " + ItemManager.getData(is));
	}

	public void biomehere_cmd() throws CivException {
		final Player player = this.getPlayer();
		final Biome biome = player.getWorld().getBiome(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
		CivMessage.send(player, "Got biome:" + biome.name());
	}

	public void showentity_cmd() throws CivException {
		final Player player = this.getPlayer();
		final Chunk chunk = player.getLocation().getChunk();
		Entity[] entities;
		for (int length = (entities = chunk.getEntities()).length, i = 0; i < length; ++i) {
			final Entity entity = entities[i];
			CivMessage.send(player, "E:" + entity.getType().name() + " UUID:" + entity.getUniqueId().toString());
			CivLog.info("E:" + entity.getType().name() + " UUID:" + entity.getUniqueId().toString());
		}
	}

	public void culturechunk_cmd() {
		if (this.sender instanceof Player) {
			final Player player = (Player) this.sender;
			final CultureChunk cc = CivGlobal.getCultureChunk(player.getLocation());
			if (cc == null) {
				CivMessage.send(this.sender, "No culture chunk found here.");
				return;
			}
			CivMessage.send(this.sender, "loc:" + cc.getChunkCoord() + " town:" + cc.getTown().getName() + " civ:" + cc.getCiv().getName()
					+ " distanceToNearest:" + cc.getDistanceToNearestEdge(cc.getTown().savedEdgeBlocks));
		}
	}

	public void town_cmd() throws CivException {
		if (this.args.length < 2) {
			CivMessage.sendError(this.sender, "Specifiy a town name.");
			return;
		}
		final Town town = this.getNamedTown(1);
		CivMessage.sendHeading(this.sender, "Town " + town.getName());
		CivMessage.send(this.sender, "id:" + town.getId() + " level: " + town.SM.getLevel());
	}

	public void civ_cmd() throws CivException {
		if (this.args.length < 2) {
			throw new CivException("Specify a civ name.");
		}
		final Civilization civ = this.getNamedCiv(1);
		CivMessage.sendHeading(this.sender, "Civ " + civ.getName());
		CivMessage.send(this.sender, "id:" + civ.getId() + " debt: " + civ.getTreasury().getDebt() + " balance:" + civ.getTreasury().getBalance());
	}

	public void townchunk_cmd() {
		if (this.sender instanceof Player) {
			final Player player = (Player) this.sender;
			final TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
			if (tc == null) {
				CivMessage.send(this.sender, "No town chunk found here.");
				return;
			}
			CivMessage.send(this.sender, "id:" + tc.getId() + " coord:" + tc.getChunkCoord());
		}
	}

	public void resident_cmd() throws CivException {
		if (this.args.length < 2) {
			CivMessage.sendError(this.sender, "Specifiy a resident name.");
			return;
		}
		final Resident res = this.getNamedResident(1);
		CivMessage.sendHeading(this.sender, "Resident " + res.getName());
		CivMessage.send(this.sender, "id: " + res.getId() + " lastOnline: " + res.getLastOnline() + " registered: " + res.getRegistered());
		CivMessage.send(this.sender, "debt: " + res.getTreasury().getDebt());
	}

	@Override
	public void doDefaultAction() throws CivException {
		showBasicHelp();
	}

	@Override
	public void showHelp() {
		showHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
	}

}
