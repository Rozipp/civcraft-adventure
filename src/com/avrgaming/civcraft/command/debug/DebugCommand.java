/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.command.debug;

import gpl.AttributeUtil;
import ua.rozipp.sound.SoundManager;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementSoulBound;
import com.avrgaming.civcraft.lorestorage.LoreStoreage;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.siege.Cannon;
import com.avrgaming.civcraft.structure.ArrowTower;
import com.avrgaming.civcraft.structure.Road;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.structure.Wall;
import com.avrgaming.civcraft.structure.wonders.GrandShipIngermanland;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.AsciiMap;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.global.perks.Perk;

public class DebugCommand extends CommandBase {

	@Override
	public void init() {
		command = "/dbg";
		displayName = "Debug";

		this.cs.add("show", "Show data base info.");
		this.cs.add("reloadconf", "перезагрузка настроек из файла");
		this.cs.add("tradegood", "Дебаг торговых ресурсов");
		this.cs.add("cave", "Дебаг пещер");
		
		cs.add("map", "shows a town chunk map of the current area.");

		cs.add("moveframes", "[x] [y] [z] moves item frames in this chunk to x,y,z");
		cs.add("frame", "gets player's town and shows the goodie frames in this town.");
		cs.add("makeframe", "[loc] [direction]");
		cs.add("dupe", "duplicates the item in your hand.");
		cs.add("test", "Run test suite commands.");
		
		cs.add("firework", "fires off a firework here.");
		cs.add("sound", "[name] [pitch]");
		cs.add("arrow", "[power] change arrow's power.");
		cs.add("wall", "wall Info about the chunk you're on.");
		cs.add("processculture", "forces a culture reprocess");
		cs.add("givebuff", "[id] gives this id buff to a town.");
		cs.add("unloadchunk", "[x] [z] - unloads this chunk.");
		cs.add("setspeed", "[speed] - set your speed to this");
		
		cs.add("restoresigns", "restores all structure signs");
		
		cs.add("quickcodereload", "Reloads the quick code plugin");
		cs.add("loadbans", "Loads bans from ban list into global table");
		cs.add("setallculture", "[amount] - sets all towns culture in the world to this amount.");
		cs.add("timers", "show all the timer information.");
		cs.add("runtimer", "run timer commands.");
		cs.add("loretest", "tests if the magic lore is set.");
		cs.add("loreset", "adds magic lore tag.");
		cs.add("giveold", "[name] [first lore]");
		cs.add("farm", "show debug commands for farms");
		cs.add("flashedges", "[town] flash edge blocks for town.");
		cs.add("refreshchunk", "refreshes the chunk you're standing in.. for science.");
		cs.add("touches", "[town] - prints a list of friendly touches for this town's culture.");
		cs.add("listconquered", "shows a list of conquered civilizations.");
		cs.add("camp", "Debugs camps.");
		cs.add("blockinfo", "[x] [y] [z] shows block info for this block.");
		cs.add("fakeresidents", "[town] [count] - Adds this many fake residents to a town.");
		cs.add("clearresidents", "[town] - clears this town of it's random residents.");
		cs.add("biomehere", "- shows you biome info where you're standing.");
		cs.add("scout", "[civ] - enables debugging for scout towers in this civ.");
		cs.add("getit", "gives you an item.");
		cs.add("showinv", "shows you an inventory");
		cs.add("showcraftinv", "shows you crafting inventory");
		cs.add("setspecial", "sets special stuff");
		cs.add("getspecial", "gets the special stuff");
		cs.add("setcivnbt", "[key] [value] - adds this key.");
		cs.add("getcivnbt", "[key] - gets this key");
		cs.add("getmid", "Gets the MID of this item.");
		cs.add("getdura", "gets the durability of an item");
		cs.add("setdura", "sets the durability of an item");
		cs.add("togglebookcheck", "Toggles checking for enchanted books on and off.");
		cs.add("circle", "[int] - draws a circle at your location, with this radius.");
		cs.add("loadperks", "loads perks for yourself");
		cs.add("colorme", "[hex] adds nbt color value to item held.");
		cs.add("preview", "show a single block preview at your feet.");
		cs.add("sql", "Show SQL health info.");
		cs.add("templatetest", "tests out some new template stream code.");
//		cs.add("buildspawn", "[civname] [capitolname] Builds spawn from spawn template.");
		cs.add("matmap", "prints the material map.");
		cs.add("ping", "print something.");
		cs.add("datebypass", "Bypasses certain date restrictions");
		cs.add("spawn", "remote entities test");
		cs.add("heal", "heals you....");
		cs.add("skull", "[player] [title]");
		cs.add("giveperk", "<id> gives yourself this perk id.");
		cs.add("packet", "sends custom auth packet.");
		cs.add("disablemap", "disables zan's minimap");
		cs.add("world", "Show world debug options");
		cs.add("cannon", "builds a war cannon.");
		cs.add("saveinv", "save an inventory");
		cs.add("restoreinv", "restore your inventory.");
	}

	public void saveinv_cmd() throws CivException {
		Resident resident = getResident();
		resident.saveInventory();
		CivMessage.sendSuccess(resident, "saved inventory.");
	}

	public void restoreinv_cmd() throws CivException {
		Resident resident = getResident();
		resident.restoreInventory();
		CivMessage.sendSuccess(resident, "restore inventory.");
	}

	public void cannon_cmd() throws CivException {
		Player player = getPlayer();
		Cannon.newCannon(player);

		CivMessage.sendSuccess(player, "built cannon.");
	}

	public void world_cmd() {
		DebugWorldCommand cmd = new DebugWorldCommand();
		cmd.onCommand(sender, null, "world", this.stripArgs(args, 1));
	}

	public void disablemap_cmd() throws CivException {
		Player player = getPlayer();
		player.sendMessage("§3§6§3§6§3§6§e");
		player.sendMessage("§3§6§3§6§3§6§d");
		CivMessage.sendSuccess(player, "Disabled.");
	}

	public void packet_cmd() throws CivException {
		Player player = getPlayer();
		player.sendPluginMessage(CivCraft.getPlugin(), "CAC", "Test Message".getBytes());
		CivMessage.sendSuccess(player, "Sent test message");
	}

	public void giveperk_cmd() throws CivException {
		Resident resident = getResident();
		String perkId = getNamedString(1, "Enter a perk ID");
		ConfigPerk configPerk = CivSettings.perks.get(perkId);

		Perk p2 = resident.perks.get(configPerk.id);
		if (p2 != null) {
			p2.count++;
			resident.perks.put(p2.getConfigId(), p2);
		} else {
			Perk p = new Perk(configPerk);
			resident.perks.put(p.getConfigId(), p);
			p2 = p;
		}

		CivMessage.sendSuccess(resident, "Added perk:" + p2.getDisplayName());
	}

	public void skull_cmd() throws CivException {
		Player player = getPlayer();
		String playerName = getNamedString(1, "Enter a player name");
		String message = getNamedString(2, "Enter a title.");

		ItemStack skull = ItemManager.spawnPlayerHead(playerName, message);
		player.getInventory().addItem(skull);
		CivMessage.sendSuccess(player, "Added skull item.");
	}

	public void heal_cmd() throws CivException {
		Player player = getPlayer();
		Double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
		player.setHealth(maxHP);
		player.setFoodLevel(50);
		CivMessage.send(player, "Healed....");
	}

	public void datebypass_cmd() {
		CivGlobal.debugDateBypass = !CivGlobal.debugDateBypass;
		CivMessage.send(sender, "Date bypass is now:" + CivGlobal.debugDateBypass);
	}

	public void ping_cmd() {
		CivMessage.send(sender, "test....");
	}

	public void matmap_cmd() throws CivException {
		CivMessage.send(getPlayer(), "Print in consol");

		for (CustomMaterial mat : CustomMaterial.getAllCustomMaterial()) {
			String mid = mat.getId();
			CivLog.info("material id: " + mid + " mat: " + mat);
		}
	}

	public void buildspawn_cmd() throws CivException {
		/* First create a new Civilization and spawn capitol */
//		String civName = getNamedString(1, "Enter a Civ name/");
//		String capitolName = getNamedString(2, "Enter a capitol name.");
//		Resident resident = getResident();
//
//		try {
//			/* Build a spawn civ. */
//			Civilization spawnCiv = new Civilization(civName, capitolName, "spawn", resident);
//			spawnCiv.saveNow();
//
//			/* Build a spawn capitol */
//			Town spawnCapitol = new Town(capitolName, resident, spawnCiv);
//			spawnCapitol.saveNow();
//
//			PermissionGroup leaders = new PermissionGroup(spawnCiv, "leaders");
//			spawnCiv.addGroup(leaders);
//			leaders.addMember(resident);
//			spawnCiv.setLeader(resident);
//			spawnCiv.setLeaderGroup(leaders);
//			leaders.save();
//
//			PermissionGroup advisers = new PermissionGroup(spawnCiv, "advisers");
//			spawnCiv.addGroup(advisers);
//			spawnCiv.setAdviserGroup(advisers);
//			advisers.save();
//
//			PermissionGroup mayors = new PermissionGroup(spawnCapitol, "mayors");
//			spawnCapitol.addGroup(mayors);
//			spawnCapitol.setMayorGroup(mayors);
//			mayors.addMember(resident);
//			mayors.save();
//
//			PermissionGroup assistants = new PermissionGroup(spawnCapitol, "assistants");
//			spawnCapitol.addGroup(assistants);
//			spawnCapitol.setAssistantGroup(assistants);
//			assistants.save();
//
//			PermissionGroup residents = new PermissionGroup(spawnCapitol, "residents");
//			spawnCapitol.addGroup(residents);
//			spawnCapitol.setDefaultGroup(residents);
//			residents.save();
//
//			spawnCiv.addTown(spawnCapitol);
//			spawnCiv.setCapitolName(spawnCapitol.getName());
//
//			spawnCiv.setAdminCiv(true);
//			spawnCiv.save();
//			spawnCapitol.save();
//			resident.save();
//
//			CivGlobal.addTown(spawnCapitol);
//			CivGlobal.addCiv(spawnCiv);
//
//			/* Setup leader and adivsers groups. */
//			try {
//				spawnCapitol.addResident(resident);
//			} catch (AlreadyRegisteredException e) {
//				e.printStackTrace();
//				return;
//			}
//
//			class BuildSpawnTask implements Runnable {
//				CommandSender sender;
//				int start_x;
//				int start_y;
//				int start_z;
//				Town spawnCapitol;
//
//				public BuildSpawnTask(CommandSender sender, int x, int y, int z, Town capitol) {
//					this.sender = sender;
//					this.start_x = x;
//					this.start_y = y;
//					this.start_z = z;
//					this.spawnCapitol = capitol;
//				}
//
//				@Override
//				public void run() {
//					try {
//
//						/* Initialize the spawn template */
//						Template tpl ;
//						try {
//							tpl = new Template("templates/spawn.def");
//						} catch (IOException e) {
//							e.printStackTrace();
//							throw new CivException("IO Error.");
//						}
//
//						Player player = (Player) sender;
//						ConfigBuildableInfo info = new ConfigBuildableInfo();
//						info.tile_improvement = false;
//						info.templateYShift = 0;
//						Location center = BuildableStatic.repositionCenterStatic(player.getLocation(), info, tpl);
//
//						CivMessage.send(sender, "Building from " + start_x + "," + start_y + "," + start_z);
//						for (int y = start_y; y < tpl.size_y; y++) {
//							for (int x = start_x; x < tpl.size_x; x++) {
//								for (int z = start_z; z < tpl.size_z; z++) {
//									BlockCoord next = new BlockCoord(center);
//									next.setX(next.getX() + x);
//									next.setY(next.getY() + y);
//									next.setZ(next.getZ() + z);
//
//									SimpleBlock sb = tpl.blocks[x][y][z];
//
//									if (sb.specialType.equals(SimpleBlock.Type.COMMAND)) {
//										String buildableName = sb.command.replace("/", "");
//
//										info = null;
//										for (ConfigBuildableInfo buildInfo : CivSettings.structures.values()) {
//											if (buildInfo.displayName.equalsIgnoreCase(buildableName)) {
//												info = buildInfo;
//												break;
//											}
//										}
//										if (info == null) {
//											try {
//												Block block = next.getBlock();
//												ItemManager.setTypeIdAndData(block, CivData.AIR, 0, false);
//												continue;
//											} catch (Exception e) {
//												e.printStackTrace();
//												continue;
//											}
//										}
//
//										CivMessage.send(sender, "Setting up " + buildableName);
//										int yShift = 0;
//										String lines[] = sb.getKeyValueString().split(",");
//										String split[] = lines[0].split(":");
//										String dir = split[0];
//										yShift = Integer.valueOf(split[1]);
//
//										Location loc = next.getLocation();
//										loc.setY(loc.getY() + yShift);
//
//										Structure struct = Structure.newStructure(loc, info.id, spawnCapitol);
//										if (struct instanceof Capitol) {
//											AdminTownCommand.claimradius(spawnCapitol, center, 15);
//										}
//										struct.setTemplateFilePath(Template.getTemplateFilePath(info.template_name, dir, null));
//										struct.bindBuildableBlocks();
//										struct.setComplete(true);
//										struct.setHitpoints(info.max_hitpoints);
//										CivGlobal.addStructure(struct);
//										spawnCapitol.addStructure(struct);
//
//										Template tplStruct;
//										try {
//											tplStruct = Template.getTemplate(struct.getTemplateFilePath());
//											struct.postBuildSyncTask(tplStruct, 10);
//										} catch (IOException e) {
//											e.printStackTrace();
//											throw new CivException("IO Exception.");
//										}
//
//										struct.save();
//										spawnCapitol.save();
//
//									} else
//										if (sb.specialType.equals(SimpleBlock.Type.LITERAL)) {
//											try {
//												Block block = next.getBlock();
//												ItemManager.setTypeIdAndData(block, sb.getType(), sb.getData(), false);
//
//												Sign s = (Sign) block.getState();
//												for (int j = 0; j < 4; j++) {
//													s.setLine(j, sb.message[j]);
//												}
//
//												s.update();
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//										} else {
//											try {
//												Block block = next.getBlock();
//												ItemManager.setTypeIdAndData(block, sb.getType(), sb.getData(), false);
//											} catch (Exception e) {
//												e.printStackTrace();
//											}
//										}
//								}
//							}
//						}
//
//						CivMessage.send(sender, "Finished building.");
//
//						spawnCapitol.addAccumulatedCulture(60000000);
//						spawnCapitol.save();
//
//					} catch (CivException e) {
//						e.printStackTrace();
//						CivMessage.send(sender, e.getMessage());
//					}
//				}
//			}
//
//			TaskMaster.syncTask(new BuildSpawnTask(sender, 0, 0, 0, spawnCapitol));
//		} catch (InvalidNameException e) {
//			throw new CivException(e.getMessage());
//		} catch (SQLException e) {
//			e.printStackTrace();
//			throw new CivException("Internal DB Error.");
//		}

	}

	public void templatetest_cmd() throws CivException {
		Player player = getPlayer();
		String filename = getNamedString(1, "Enter a filename");

		String filePatch = null;

		if (Template.checkFile(filename))
			filePatch = filename;
		if (Template.checkFile(filename + ".def"))
			filePatch = filename + ".def";
		if (Template.checkFile("templates/" + filename))
			filePatch = "templates/" + filename;
		if (Template.checkFile("templates/" + filename + ".def"))
			filePatch = "templates/" + filename + ".def";

		Template tpl = Template.getTemplate(filePatch);
		if (tpl == null) {
			throw new CivException(CivSettings.localize.localizedString("template_invalidFile") + " " + filename);
		}

		tpl.buildTemplate(new BlockCoord(player.getLocation()));
	}

	public void sql_cmd() {
//		HashMap<String, String> stats = new HashMap<String, String>();
//		CivMessage.send(sender, "ConnectionsRequested: " + SQL.gameDatabase.getStats().getConnectionsRequested());
//		CivMessage.send(sender, "Free Pool Members: " + SQL.gameDatabase.getStats().getTotalFree());
//		CivMessage.send(sender, "Leased Pool Members: " + SQL.gameDatabase.getStats().getTotalLeased());
//		CivMessage.send(sender, "--------------------------");
//
//		stats.clear();
//		for (String key : SQLUpdate.statSaveRequests.keySet()) {
//			Integer requests = SQLUpdate.statSaveRequests.get(key);
//			Integer completes = SQLUpdate.statSaveCompletions.get(key);
//
//			if (requests == null) {
//				requests = 0;
//			}
//
//			if (completes == null) {
//				completes = 0;
//			}
//
//			CivMessage.send(sender, key + " requested:" + requests + " completed:" + completes);
//		}

//		CivMessage.send(sender, makeInfoString(stats, CivColor.Green, CivColor.LightGreen));
		CivMessage.send(sender, "TODO NOT WORK");
	}

	public void preview_cmd() throws CivException {
//		Player player = getPlayer();
//		PlayerBlockChangeUtil util = new PlayerBlockChangeUtil();
//		
//		ProtocolManager manager = ProtocolLibrary.getProtocolManager();
//		PacketContainer mapChunk =  manager.createPacket(Packets.Server.MAP_CHUNK);
//		
//		mapChunk.getBytes().write(arg0, arg1)
//		//Packet3CExplosion expo = new Packet3CExplosion();
//		//Packet51MapChunk c = new Packet51MapChunk();
//		c.
//		//mapChunk.set
//		
//		
//		//util.addUpdateBlock(player.getName(), new BlockCoord(player.getLocation().add(0, -1, 0)), CivData.WOOD, 3);
//		//util.sendUpdate(player.getName());
//		//CivMessage.sendSuccess(player, "Changed block");
	}

	public void colorme_cmd() throws CivException {
		Player player = getPlayer();
		String hex = getNamedString(1, "color code");
		long value = Long.decode(hex);

		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand == null || ItemManager.getTypeId(inHand) == CivData.AIR) {
			throw new CivException("please have an item in your hand.");
		}

		AttributeUtil attrs = new AttributeUtil(inHand);
		attrs.setColor(value);
		player.getInventory().setItemInMainHand(attrs.getStack());
		CivMessage.sendSuccess(player, "Set color.");
	}

	public void circle_cmd() throws CivException {
		Player player = getPlayer();
		int radius = getNamedInteger(1);

		HashMap<String, SimpleBlock> simpleBlocks = new HashMap<String, SimpleBlock>();
		Road.getCircle(player.getLocation().getBlockX(), player.getLocation().getBlockY() - 1,
				player.getLocation().getBlockZ(), player.getLocation().getWorld().getName(), radius, simpleBlocks);

		for (SimpleBlock sb : simpleBlocks.values()) {
			Block block = player.getWorld().getBlockAt(sb.x, sb.y, sb.z);
			ItemManager.setTypeId(block, sb.getType());
		}

		CivMessage.sendSuccess(player, "Built a circle at your feet.");
	}

	public void togglebookcheck_cmd() {
		CivGlobal.checkForBooks = !CivGlobal.checkForBooks;
		CivMessage.sendSuccess(sender, "Check for books is:" + CivGlobal.checkForBooks);
	}

	public void setcivnbt_cmd() throws CivException {
		Player player = getPlayer();
		String key = getNamedString(1, "key");
		String value = getNamedString(2, "value");

		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand == null) {
			throw new CivException("You must have an item in hand.");
		}

		AttributeUtil attrs = new AttributeUtil(inHand);
		attrs.setCivCraftProperty(key, value);
		player.getInventory().setItemInMainHand(attrs.getStack());
		CivMessage.sendSuccess(player, "Set property.");

	}

	public void getcivnbt_cmd() throws CivException {
		Player player = getPlayer();
		String key = getNamedString(1, "key");

		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand == null) {
			throw new CivException("You must have an item in hand.");
		}

		AttributeUtil attrs = new AttributeUtil(inHand);
		String value = attrs.getCivCraftProperty(key);
		CivMessage.sendSuccess(player, "got property:" + value);

	}

	public void getdura_cmd() throws CivException {
		Player player = getPlayer();
		ItemStack inHand = player.getInventory().getItemInMainHand();
		CivMessage.send(player, "Durability:" + inHand.getDurability());
		CivMessage.send(player, "MaxDura:" + inHand.getType().getMaxDurability());

	}

	public void setdura_cmd() throws CivException {
		Player player = getPlayer();
		Integer dura = getNamedInteger(1);

		ItemStack inHand = player.getInventory().getItemInMainHand();
		inHand.setDurability((short) dura.shortValue());

		CivMessage.send(player, "Set Durability:" + inHand.getDurability());
		CivMessage.send(player, "MaxDura:" + inHand.getType().getMaxDurability());

	}

	public void getmid_cmd() throws CivException {
		Player player = getPlayer();
		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand == null) {
			throw new CivException("You need an item in your hand.");
		}

		CivMessage.send(player, "MID:" + CustomMaterial.getMID(inHand));
	}

	public void setspecial_cmd() throws CivException {
		Player player = getPlayer();
		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand == null) {
			throw new CivException("You need an item in your hand.");
		}

		// AttributeUtil attrs = new AttributeUtil(inHand);
		// attrs.setCivCraftProperty("customId", "testMyCustomId");
		ItemStack stack = CustomMaterial.addEnhancement(inHand, new LoreEnhancementSoulBound());
		player.getInventory().setItemInMainHand(stack);
		CivMessage.send(player, "Set it.");
	}

	public void getspecial_cmd() throws CivException {
		Player player = getPlayer();
		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand == null) {
			throw new CivException("You need an item in your hand.");
		}

		AttributeUtil attrs = new AttributeUtil(inHand);
		String value = attrs.getCivCraftProperty("soulbound");

		CivMessage.send(player, "Got:" + value);
	}

	public void showinv_cmd() throws CivException {
		Book.spawnGuiBook(getPlayer());
	}

	public void showcraftinv_cmd() throws CivException {
		Book.showCraftingHelp(getPlayer());
	}

	public void scout_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);

		if (!civ.scoutDebug) {
			civ.scoutDebug = true;
			civ.scoutDebugPlayer = getPlayer().getName();
			CivMessage.sendSuccess(sender, "Enabled scout tower debugging in " + civ.getName());
		} else {
			civ.scoutDebug = false;
			civ.scoutDebugPlayer = null;
			CivMessage.sendSuccess(sender, "Disabled scout tower debugging in " + civ.getName());
		}
	}

	public void biomehere_cmd() throws CivException {
		Player player = getPlayer();

		Biome biome = player.getWorld().getBiome(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
		CivMessage.send(player, "Got biome:" + biome.name());
	}

	public void clearresidents_cmd() throws CivException {
		Town town = getNamedTown(1);

		ArrayList<Resident> removeUs = new ArrayList<Resident>();
		for (Resident resident : town.getResidents()) {
			if (resident.getName().startsWith("RANDOM_")) {
				removeUs.add(resident);
			}
		}

		for (Resident resident : removeUs) {
			town.removeResident(resident);
		}
	}

	public void fakeresidents_cmd() throws CivException {
		Town town = getNamedTown(1);
		Integer count = getNamedInteger(2);

		for (int i = 0; i < count; i++) {
			SecureRandom random = new SecureRandom();
			String name = (new BigInteger(130, random).toString(32));

			try {

				Resident fake = new Resident(UUID.randomUUID(), "RANDOM_" + name);
				town.addResident(fake);
				town.addFakeResident(fake);
			} catch (AlreadyRegisteredException e) {
				// ignore
			} catch (InvalidNameException e) {
				// ignore
			}
		}
		CivMessage.sendSuccess(sender, "Added " + count + " residents.");
	}

	public void blockinfo_cmd() throws CivException {
		int x = getNamedInteger(1);
		int y = getNamedInteger(2);
		int z = getNamedInteger(3);

		Block b = Bukkit.getWorld("world").getBlockAt(x, y, z);

		CivMessage.send(sender,
				"type:" + ItemManager.getTypeId(b) + " data:" + ItemManager.getData(b) + " name:" + b.getType().name());

	}

	public void camp_cmd() {
		DebugCampCommand cmd = new DebugCampCommand();
		cmd.onCommand(sender, null, "farm", this.stripArgs(args, 1));
	}

	public void listconquered_cmd() {
		CivMessage.sendHeading(sender, "Conquered Civs");
		String out = "";
		for (Civilization civ : CivGlobal.getConqueredCivs()) {
			out += civ.getName() + ", ";
		}
		CivMessage.send(sender, out);
	}

	public void touches_cmd() throws CivException {
		Town town = getNamedTown(1);

		CivMessage.sendHeading(sender, "Touching Towns");
		String out = "";
		for (Town t : town.townTouchList) {
			out += t.getName() + ", ";
		}

		if (town.touchesCapitolCulture(new HashSet<Town>())) {
			CivMessage.send(sender, CivColor.LightGreen + "Touches capitol.");
		} else {
			CivMessage.send(sender, CivColor.Rose + "Does NOT touch capitol.");
		}

		CivMessage.send(sender, out);
	}

	public void refreshchunk_cmd() throws CivException {
		Player you = getPlayer();
		ChunkCoord coord = new ChunkCoord(you.getLocation());

		for (Player player : Bukkit.getOnlinePlayers()) {
			player.getWorld().unloadChunk(coord.getX(), coord.getZ());
			player.getWorld().loadChunk(coord.getX(), coord.getZ());
		}
	}

	public void flashedges_cmd() throws CivException {
		Town town = getNamedTown(1);

		for (TownChunk chunk : town.savedEdgeBlocks) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					Block b = Bukkit.getWorld("world").getHighestBlockAt(((chunk.getChunkCoord().getX() + x << 4) + x),
							((chunk.getChunkCoord().getZ() << 4) + z));
					Bukkit.getWorld("world").playEffect(b.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
				}
			}
		}
		CivMessage.sendSuccess(sender, "flashed");
	}

	public void farm_cmd() {
		DebugFarmCommand cmd = new DebugFarmCommand();
		cmd.onCommand(sender, null, "farm", this.stripArgs(args, 1));
	}

	public void giveold_cmd() throws CivException {
		Player player = getPlayer();

		if (args.length < 3) {
			throw new CivException("Enter name and first lore line.");
		}

		ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand != null) {

			ItemMeta meta = inHand.getItemMeta();
			meta.setDisplayName(args[1]);

			ArrayList<String> lore = new ArrayList<String>();
			lore.add(this.combineArgs(this.stripArgs(args, 2)));
			meta.setLore(lore);

			inHand.setItemMeta(meta);
		}
	}

	public void loretest_cmd() throws CivException {
		Player player = getPlayer();

		org.bukkit.inventory.ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand != null) {
			ItemMeta meta = inHand.getItemMeta();
			List<String> newLore = meta.getLore();
			if (newLore != null && newLore.size() > 0 && newLore.get(0).equalsIgnoreCase("RJMAGIC")) {
				CivMessage.sendSuccess(player, "found magic lore");
			} else {
				CivMessage.sendSuccess(player, "No magic lore.");
			}
		}
	}

	public void loreset_cmd() throws CivException {
		Player player = getPlayer();

		org.bukkit.inventory.ItemStack inHand = player.getInventory().getItemInMainHand();
		if (inHand != null) {
			LoreStoreage.setMatID(1337, inHand);
		}
	}

	public void runtimer_cmd() {
		DebugRuntimerCommand cmd = new DebugRuntimerCommand();
		cmd.onCommand(this.sender, null, "runtimer", this.stripArgs(this.args, 1));
	}

	public void timers_cmd() {

		CivMessage.sendHeading(sender, "Timers");
		SimpleDateFormat sdf = CivGlobal.dateFormat;

		CivMessage.send(sender, "Now:" + sdf.format(new Date()));
		for (EventTimer timer : EventTimer.timers.values()) {

			CivMessage.send(sender, timer.getName());
			CivMessage.send(sender, "    next: " + sdf.format(timer.getNext().getTime()));
			if (timer.getLast().getTime().getTime() == 0) {
				CivMessage.send(sender, "    last: never");
			} else {
				CivMessage.send(sender, "    last: " + sdf.format(timer.getLast().getTime()));
			}

		}

	}

	public void setallculture_cmd() throws CivException {
		Integer culture = getNamedInteger(1);

		for (Town town : CivGlobal.getTowns()) {
			town.addAccumulatedCulture(culture);
			town.save();
		}

		CivGlobal.processCulture();
		CivMessage.sendSuccess(sender, "Set all town culture to " + culture + " points.");
	}

	public void quickcodereload_cmd() {

		Bukkit.getPluginManager().getPlugin("QuickCode");

	}

	

	public void restoresigns_cmd() {

		CivMessage.send(sender, "restoring....");
		for (ConstructSign sign : CivGlobal.getConstructSigns()) {

			BlockCoord bcoord = sign.getCoord();
			Block block = bcoord.getBlock();
			ItemManager.setTypeId(block, CivData.WALL_SIGN);
			ItemManager.setData(block, sign.getDirection());

			Sign s = (Sign) block.getState();
			String[] lines = sign.getText().split("\n");

			if (lines.length > 0) {
				s.setLine(0, lines[0]);
			}
			if (lines.length > 1) {
				s.setLine(1, lines[1]);
			}

			if (lines.length > 2) {
				s.setLine(2, lines[2]);
			}

			if (lines.length > 3) {
				s.setLine(3, lines[3]);
			}
			s.update();

		}

	}

	public void setspeed_cmd() throws CivException {
		Player player = getPlayer();

		if (args.length < 2) {
			throw new CivException("Enter a speed.");
		}

		player.setWalkSpeed(Float.valueOf(args[1]));
		CivMessage.sendSuccess(player, "speed changed");
	}

	public void unloadchunk_cmd() throws CivException {
		if (args.length < 3) {
			throw new CivException("Enter an x and z");
		}

		this.getPlayer().getWorld().unloadChunk(Integer.valueOf(args[1]), Integer.valueOf(args[2]));

		CivMessage.sendSuccess(sender, "unloaded.");
	}

	public void givebuff_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter the buff id");
		}

		ConfigBuff buff = CivSettings.buffs.get(args[1]);
		if (buff == null) {
			throw new CivException("No buff id:" + args[1]);
		}

		getSelectedTown().getBuffManager().addBuff(buff.id, buff.id, "Debug");
		CivMessage.sendSuccess(sender, "Gave buff " + buff.name + " to town");
	}

	public void processculture_cmd() {
		CivGlobal.processCulture();
		CivMessage.sendSuccess(sender, "Forced process of culture");
	}

	public void wall_cmd() throws CivException {
		Player player = getPlayer();
		HashSet<Wall> walls = CivGlobal.getWallChunk(new ChunkCoord(player.getLocation()));
		if (walls == null) {
			CivMessage.sendError(player, "Sorry, this is not a wall chunk.");
			return;
		}
		for (Wall wall : walls) {
			CivMessage.send(player, "Wall:" + wall.getId() + " town:" + wall.getTown() + " chunk:"
					+ new ChunkCoord(player.getLocation()));
		}
	}

	public void arrow_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("/arrow [power]");
		}
		for (Town town : CivGlobal.getTowns()) {
			for (Structure struct : town.getStructures()) {
				if (struct instanceof ArrowTower) {
					((ArrowTower) struct).setPower(Float.valueOf(args[1]));
				}
			}
			for (Wonder wonder : town.getWonders()) {
				if (wonder instanceof GrandShipIngermanland) {
					((GrandShipIngermanland) wonder).setArrorPower(Float.valueOf(args[1]));
				}
			}
		}
	}

	public void sound_cmd() throws CivException {
		final Player player = this.getPlayer();
		if (this.args.length < 2) {
			throw new CivException("Enter sound key from suond.yml");
		}
		// player.getWorld().playSound(player.getLocation(),
		// Sound.valueOf(this.args[1].toUpperCase()), 1.0f,
		// (float)Float.valueOf(this.args[2]));
		SoundManager.playSound(this.args[1], player.getLocation());// "ambient.cave"
	}

	public void firework_cmd() throws CivException {
		Player player = getPlayer();
		FireworkEffectPlayer fw = new FireworkEffectPlayer();
		try {
			fw.playFirework(player.getWorld(), player.getLocation(),
					FireworkEffect.builder().withColor(Color.RED).flicker(true).with(Type.BURST).build());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void test_cmd() throws CivException {
		DebugTestCommand cmd = new DebugTestCommand();
		cmd.onCommand(sender, null, "test", this.stripArgs(args, 1));
	}

	public void dupe_cmd() throws CivException {
		Player player = getPlayer();
		if (player.getInventory().getItemInMainHand() == null
				|| ItemManager.getTypeId(player.getInventory().getItemInMainHand()) == 0) {
			throw new CivException("No item in hand.");
		}
		player.getInventory().addItem(player.getInventory().getItemInMainHand());
		CivMessage.sendSuccess(player, player.getInventory().getItemInMainHand().getType().name() + "duplicated.");
	}

	public void makeframe_cmd() throws CivException {
		if (args.length > 3) {
			throw new CivException("Provide a x,y,z and a direction (n,s,e,w)");
		}

		String locationString = "world," + args[1];
		BlockFace face;

		switch (args[2]) {
		case "n":
			face = BlockFace.NORTH;
			break;

		case "s":
			face = BlockFace.SOUTH;
			break;

		case "e":
			face = BlockFace.EAST;
			break;

		case "w":
			face = BlockFace.WEST;
			break;
		default:
			throw new CivException("Invalid direction, use n,s,e,w");
		}

		Location loc = CivGlobal.getLocationFromHash(locationString);
		new ItemFrameStorage(loc, face);
		CivMessage.send(sender, "Created frame.");
	}
	public void frame_cmd() throws CivException {
		Town town = getSelectedTown();

		Townhall townhall = town.getTownHall();
		if (townhall == null) {
			throw new CivException("No town hall?");
		}

		for (ItemFrameStorage itemstore : townhall.getGoodieFrames()) {
			String itemString = "empty";

			if (!itemstore.isEmpty()) {
				BonusGoodie goodie = CivGlobal.getBonusGoodie(itemstore.getItem());
				itemString = goodie.getDisplayName();
			}
			CivMessage.send(sender, "GoodieFrame UUID:" + itemstore.getUUID() + " item:" + itemString);
		}

	}
	public void moveframes_cmd() throws CivException {
		Player player = getPlayer();
		Chunk chunk = player.getLocation().getChunk();

		// int x = this.getNamedInteger(1);
		// int y = this.getNamedInteger(2);
		// int z = this.getNamedInteger(3);

		// Location loc = new Location(player.getWorld(), x, y, z);

		for (Entity entity : chunk.getEntities()) {
			if (entity instanceof ItemFrame) {
				CivMessage.send(sender, "Teleported...");
				entity.teleport(entity.getLocation());
			}
		}

	}

	public void culturechunk_cmd() {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			CultureChunk cc = CivGlobal.getCultureChunk(player.getLocation());

			if (cc == null) {
				CivMessage.send(sender, "No culture chunk found here.");
				return;
			}

			CivMessage.send(sender,
					"loc:" + cc.getChunkCoord() + " town:" + cc.getTown().getName() + " civ:" + cc.getCiv().getName()
							+ " distanceToNearest:" + cc.getDistanceToNearestEdge(cc.getTown().savedEdgeBlocks));
		}
	}

//	public void addculture_cmd() throws CivException {
//		if (args.length < 3) {
//			throw new CivException("enter the town, then level you want to set.");
//		}
//		
//		Town town = getNamedTown(1);
//
//		try {
//			
//			int level = Integer.valueOf(args[2]);
//			town.addCulture(level);
//			CivMessage.sendSuccess(sender, "Added "+args[2]+" culture to town "+args[1]);
//		} catch (NumberFormatException e) {
//			throw new CivException(args[2]+" is not a number.");
//		}
//		
//	}

	public void map_cmd() throws CivException {
		Player player = getPlayer();
		CivMessage.send(player, AsciiMap.getMapAsString(player.getLocation()));
	}
	public void cave_cmd() {
		final DebugCaveCommand cmd = new DebugCaveCommand();
		cmd.onCommand(this.sender, null, "cave", this.stripArgs(this.args, 1));
	}
	public void tradegood_cmd() {
		final DebugTradeGoodCommand cmd = new DebugTradeGoodCommand();
		cmd.onCommand(this.sender, null, "tradegood", this.stripArgs(this.args, 1));
	}
	public void reloadconf_cmd() {
		final DebugReloadConfCommand cmd = new DebugReloadConfCommand();
		cmd.onCommand(this.sender, null, "reloadconf", this.stripArgs(this.args, 1));
	}

	public void show_cmd() {
		final DebugShowCommand cmd = new DebugShowCommand();
		cmd.onCommand(this.sender, null, "show", this.stripArgs(this.args, 1));
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		if (sender instanceof Player) {
			if (((Player) sender).isOp()) {
				return;
			}
		} else {
			return;
		}
		throw new CivException("Only OP can do this.");
	}

	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

}
