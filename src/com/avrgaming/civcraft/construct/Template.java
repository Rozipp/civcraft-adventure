package com.avrgaming.civcraft.construct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.config.ConfigConstructInfo.ConstructType;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.SimpleType;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Template {
	public ArrayList<ArrayList<SimpleBlock>> blocks;
	public int size_x;
	public int size_y;
	public int size_z;
	private String direction;
	public String filepath;
	public ArrayList<Integer> layerBlocksCount;
	public int totalBlocks;

	/* Save the command block locations when we init the template, so we dont have to search for them later. */
	public ArrayList<SimpleBlock> commandBlockRelativeLocations = new ArrayList<>();
	public ArrayList<SimpleBlock> emptyLocations = new ArrayList<>();
	public LinkedList<SimpleBlock> doorRelativeLocations = new LinkedList<>();
	public LinkedList<SimpleBlock> attachableLocations = new LinkedList<>();

	public Template() {
	}

	public Template(String filepath) throws IOException, CivException {
		this.filepath = filepath;
		this.direction = Template.getDirection(filepath);
		load_template();
	}

	public void load_template() throws IOException, CivException {
		File templateFile = new File(this.filepath);
		BufferedReader reader = new BufferedReader(new FileReader(templateFile));

		// Read first line and get size.
		String line = reader.readLine();
		if (line == null) {
			reader.close();
			throw new CivException(CivSettings.localize.localizedString("template_invalidFile") + " " + filepath);
		}

		String[] split = line.split(";");
		size_x = Integer.parseInt(split[0]);
		size_y = Integer.parseInt(split[1]);
		size_z = Integer.parseInt(split[2]);
		this.totalBlocks = 0;

		SimpleBlock[][][] loadedblocks = new SimpleBlock[size_x][size_y][size_z];

		// Read blocks from file.
		while ((line = reader.readLine()) != null) {
			String[] locTypeSplit = line.split(",");

			// Parse location
			String[] locationSplit = locTypeSplit[0].split(":");
			int blockX = Integer.parseInt(locationSplit[0]);
			int blockY = Integer.parseInt(locationSplit[1]);
			int blockZ = Integer.parseInt(locationSplit[2]);

			// Parse type
			String[] typeSplit = locTypeSplit[1].split(":");
			int blockId = Integer.parseInt(typeSplit[0]);
			int blockData = Integer.parseInt(typeSplit[1]);
			if (blockId == 0) continue;

			this.totalBlocks++;
			SimpleBlock sblock = new SimpleBlock(null, blockX, blockY, blockZ, blockId, blockData);

			if (blockId == CivData.WOOD_DOOR || blockId == CivData.IRON_DOOR || blockId == CivData.SPRUCE_DOOR || blockId == CivData.BIRCH_DOOR || blockId == CivData.JUNGLE_DOOR || blockId == CivData.ACACIA_DOOR
					|| blockId == CivData.DARK_OAK_DOOR) {
				this.doorRelativeLocations.add(sblock);
			}

			// look for signs.
			if (blockId == CivData.WALL_SIGN || blockId == CivData.SIGN) {
				if (locTypeSplit.length > 2) {
					// The first character on special signs needs to be a /.
					if (locTypeSplit[2] != null && !locTypeSplit[2].equals("") && locTypeSplit[2].charAt(0) == '/') {
						sblock.specialType = SimpleBlock.SimpleType.COMMAND;

						// Got a command, save it.
						sblock.command = locTypeSplit[2];

						// Save any key values we find.
						if (locTypeSplit.length > 3) {
							for (int i = 3; i < locTypeSplit.length; i++) {
								if (locTypeSplit[i] == null || locTypeSplit[i].equals("")) continue;

								String[] keyvalue = locTypeSplit[i].split(":");
								if (keyvalue.length < 2) {
									CivLog.warning("Invalid keyvalue:" + locTypeSplit[i] + " in template:" + this.filepath);
									continue;
								}
								String key = keyvalue[0].trim();
								String keyValue = keyvalue[1].trim();
								if (!key.isEmpty()) sblock.keyvalues.put(key, keyValue);
							}
						}

						/* This block coord does not point to a location in a world, just a template. */
						this.commandBlockRelativeLocations.add(sblock);

					} else {
						sblock.specialType = SimpleBlock.SimpleType.LITERAL;
						// Literal sign, copy the sign into the simple block
						for (int i = 0; i < 4; i++)
							try {
								sblock.message[i] = locTypeSplit[i + 2];
							} catch (ArrayIndexOutOfBoundsException e) {
								sblock.message[i] = "";
							}
						this.attachableLocations.add(sblock);
					}
				}
			} else
				if (Template.isAttachable(blockId)) this.attachableLocations.add(sblock);
			loadedblocks[blockX][blockY][blockZ] = sblock;
		}
		reader.close();

		blocks = new ArrayList<>();
		layerBlocksCount = new ArrayList<>();
		Random rand = new Random(2020);
		int count = 0;
		layerBlocksCount.add(count);
		for (int y = 0; y < size_y; y++) {
			ArrayList<SimpleBlock> layer = new ArrayList<>();
			for (int x = 0; x < size_x; x++)
				for (int z = 0; z < size_z; z++) {
					SimpleBlock sb = loadedblocks[x][y][z];
					if (sb == null) sb = new SimpleBlock(null, x, y, z, 0, 0);
					if (sb.getType() == 0)
						emptyLocations.add(sb);
					else
						layer.add(loadedblocks[x][y][z]);
				}
			Collections.shuffle(layer, rand);
			count += layer.size();
			layerBlocksCount.add(count);
			blocks.add(layer);
		}
	}

	// ---------------- debug edit Template

	public void getBlocksWithWorld(Location loc, int sizeX, int sizeY, int sizeZ, String filepath) {
		World world = loc.getWorld();
		size_x = sizeX;
		size_y = sizeY;
		size_z = sizeZ;
		this.filepath = filepath;

		blocks = new ArrayList<>();
		// Read blocks from file.
		for (int y = 0; y < size_y; y++) {
			blocks.add(new ArrayList<>());
			for (int z = 0; z < size_z; z++) {
				for (int x = 0; x < size_x; x++) {
					Block block = world.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
					int typeId = ItemManager.getTypeId(block);
					int data = ItemManager.getData(block);
					SimpleBlock sb = new SimpleBlock(null, x, y, z, typeId, data);

					if (typeId == CivData.WOOD_DOOR || typeId == CivData.IRON_DOOR || typeId == CivData.SPRUCE_DOOR || typeId == CivData.BIRCH_DOOR || typeId == CivData.JUNGLE_DOOR || typeId == CivData.ACACIA_DOOR
							|| typeId == CivData.DARK_OAK_DOOR) {
						this.doorRelativeLocations.add(sb);
					}

					if (block.getState() instanceof Sign) {
						Sign sign = (Sign) block.getState();
						String firstLine = sign.getLine(0);
						if (!firstLine.isEmpty() && firstLine.charAt(0) == '/') {
							sb.specialType = SimpleType.COMMAND;
							sb.command = firstLine;
							for (int i = 1; i < 4; i++) {
								String[] split = sign.getLine(i).split(":");
								if (split.length < 2) continue;
								sb.keyvalues.put(split[0], split[1]);
							}
						} else {
							sb.specialType = SimpleType.LITERAL;
							for (int i = 0; i < 4; i++)
								sb.message[i] = sign.getLine(i).replace(",", "");
						}
					}

					if (Template.isAttachable(typeId)) this.attachableLocations.add(sb);
					blocks.get(y).add(sb);
				}
			}
		}

	}

	public void saveTemplate() throws IOException {
		File templateFile = new File(this.filepath);

		templateFile.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(templateFile);

		writer.write(this.size_x + ";" + this.size_y + ";" + this.size_z + "\n");

		for (int y = 0; y < this.size_y; y++) {
			for (SimpleBlock sb : blocks.get(y)) {
				if (sb.getType() == 0) continue;

				StringBuilder ss = new StringBuilder(sb.getX() + ":" + y + ":" + sb.getZ() + "," + sb.getType() + ":" + sb.getData());
				switch (sb.specialType) {
				case NORMAL:
					break;
				case LITERAL:
					for (String line : sb.message) {
						ss.append(",").append(line);
					}
					break;
				case COMMANDDBG:
				case COMMAND:
					ss.append(",").append(sb.command);
					for (String key : sb.keyvalues.keySet()) {
						ss.append(",").append(key).append(":").append(sb.keyvalues.get(key));
					}
					break;
				}
				writer.write(ss + "\n");
			}
		}
		writer.close();
	}

	public SimpleBlock getNextBlockBuild(int blocksCompleted) {
		for (int y = 0; y < size_y; y++) {
			int layerCount = layerBlocksCount.get(y + 1);
			if (layerCount > blocksCompleted) {
				int size = blocks.get(y).size();
				int xz = (blocksCompleted - layerBlocksCount.get(y)) % size;
				return blocks.get(y).get(xz);
			}
		}
		return null;
	}

	public void buildPreviewScaffolding(BlockCoord bcoord, Player player) {
		Resident resident = CivGlobal.getResident(player);
		TaskMaster.asyncTask(() -> {
			resident.undoPreview();
			int count = 0;
			for (int y = 0; y < this.size_y; y++) {
				for (int x = 0; x < this.size_x; x++) {
					for (int z = 0; z < this.size_z; z++) {
						boolean bb = false;
						Block b = bcoord.getBlockRelative(x, y, z);
						if ((x == 0 || x == this.size_x - 1) && (z == 0 || z == this.size_z - 1) && Math.floorMod(y, 2) == 0) bb = true;
						if (y == 0 && (Math.floorMod(x + z, 3) == 1)) bb = true;
						if ((y == this.size_y - 1) && (x == 0 || x == this.size_x - 1) && Math.floorMod(z, 3) == 0) bb = true;
						if ((y == this.size_y - 1) && (z == 0 || z == this.size_z - 1) && Math.floorMod(x, 3) == 0) bb = true;
						if (!bb) continue;

						count++;
						ItemManager.sendBlockChange(player, b.getLocation(), 85, 0);
						resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));

						if (count < 1000) continue;
						count = 0;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, 0);
	}

	public void buildScaffolding(BlockCoord corner) {
		TaskMaster.asyncTask(() -> {
			int blocksPerTick = 100000;
			try {
				int count = 0;
				Queue<SimpleBlock> sbs = new LinkedList<>();
				for (int y = this.size_y - 1; y >= 0; y--)
					for (int x = 0; x < this.size_x; x++) {
						for (int z = 0; z < this.size_z; z++) {
							// Must set to air in a different loop, since setting to air can break
							// attachables.
							Block b = corner.getBlock().getRelative(x, y, z);
							boolean isPutBlock = (x == 0 || x == this.size_x - 1 || z == 0 || z == this.size_z - 1) || (y == 0) || (y == this.size_y - 1);
							// && (x == 0 || x == tpl.size_x - 1 || z == 0 || z == tpl.size_z - 1)

							SimpleBlock sb = (isPutBlock) ? new SimpleBlock(CivSettings.scaffoldingType, CivSettings.scaffoldingData) : new SimpleBlock(CivData.AIR, 0);
							sb.worldname = corner.getWorld().getName();
							sb.x = b.getX();
							sb.y = b.getY();
							sb.z = b.getZ();
							sbs.add(sb);
							count++;

							if (count >= blocksPerTick) {
								SyncBuildUpdateTask.queueSimpleBlock(sbs);
								sbs.clear();
								count = 0;
								Thread.sleep(1000);
							}
						}
					}
				if (!sbs.isEmpty()) {
					SyncBuildUpdateTask.queueSimpleBlock(sbs);
					sbs.clear();
				}
			} catch (InterruptedException ignored) {}
		}, 10);
	}

	public void removeScaffolding(Location center) {
		TaskMaster.asyncTask(() -> {
			try {
				int count = 0;
				Queue<SimpleBlock> sbs = new LinkedList<>();
				for (int y = 0; y < this.size_y; y++) {
					for (int x = 0; x < this.size_x; x++) {
						for (int z = 0; z < this.size_z; z++) {
							// Must set to air in a different loop, since setting to air can break
							// attachables.
							Block b = center.getBlock().getRelative(x, y, z);
							boolean bb = false;
							if (x == 0 || x == this.size_x - 1 || z == 0 || z == this.size_z - 1) bb = true;
							if (y == 0) bb = true;
							if (y == this.size_y - 1) bb = true;// && (x == 0 || x == tpl.size_x - 1 || z == 0 || z == tpl.size_z - 1)
							if (!bb) continue;

							if (ItemManager.getTypeId(b) == CivSettings.scaffoldingType)//
								ItemManager.setTypeIdAndData(b, CivData.AIR, 0, true);
							count++;

							if (count < 10000) continue;

							SyncBuildUpdateTask.queueSimpleBlock(sbs);
							sbs.clear();
							count = 0;
							Thread.sleep(1000);
						}
					}
				}
				if (!sbs.isEmpty()) {
					SyncBuildUpdateTask.queueSimpleBlock(sbs);
					sbs.clear();
				}
			} catch (InterruptedException ignored) {}
		}, 0);
	}

	public void saveUndoTemplate(String string, BlockCoord corner) throws IOException {
		FileWriter writer = new FileWriter(Template.getUndoFilePath(string));

		// TODO Extend this to save paintings?
		writer.write(this.size_x + ";" + this.size_y + ";" + this.size_z + "\n");
		for (int x = 0; x < this.size_x; x++) {
			for (int y = 0; y < this.size_y; y++) {
				for (int z = 0; z < this.size_z; z++) {
					Block b = corner.getBlock().getRelative(x, y, z);
					if (ItemManager.getTypeId(b) == CivData.WALL_SIGN || ItemManager.getTypeId(b) == CivData.SIGN) {
						if (b.getState() instanceof Sign) {
							Sign sign = (Sign) b.getState();
							StringBuilder signText = new StringBuilder();
							for (String line : sign.getLines()) {
								signText.append(line).append(",");
							}
							writer.write(x + ":" + y + ":" + z + "," + ItemManager.getTypeId(b) + ":" + ItemManager.getData(b) + "," + signText + "\n");
						}
					} else {
						writer.write(x + ":" + y + ":" + z + "," + ItemManager.getTypeId(b) + ":" + ItemManager.getData(b) + "\n");
					}
				}
			}
		}
		writer.close();
	}

	public void buildTemplate(BlockCoord corner) {
		Queue<SimpleBlock> sbs = new LinkedList<>();
		// Not Attachable blocks
		for (int y = 0; y < this.size_y; ++y) {
			for (SimpleBlock sb : this.blocks.get(y)) {
				if (Template.isAttachable(sb.getMaterial())) continue;
				sbs.add(new SimpleBlock(corner, sb));
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
		// Attachable blocks
		for (int y = 0; y < this.size_y; ++y) {
			for (SimpleBlock sb : this.blocks.get(y)) {
				if (!Template.isAttachable(sb.getMaterial())) continue;
				sbs.add(new SimpleBlock(corner, sb));
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
	}

	public void buildAirBlocks(BlockCoord corner) {
		Queue<SimpleBlock> sbs = new LinkedList<>();
		for (SimpleBlock sb : emptyLocations) {
			sbs.add(new SimpleBlock(corner, sb));
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
	}

	public void buildTemplateDbg(BlockCoord corner) {
		buildTemplate(corner);
		buildAirBlocks(corner);
		Queue<SimpleBlock> sbs = new LinkedList<>();
		for (SimpleBlock sb : this.commandBlockRelativeLocations) {
			SimpleBlock sbt = new SimpleBlock(corner, sb);
			sbt.specialType = SimpleType.COMMANDDBG;
			sbs.add(sbt);
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
	}

	/* Handles the processing of CivTemplates which store cubiods of blocks for later use. */
	public static HashMap<String, Template> templateCache = new HashMap<>();
	// -------------- Attachable Types
	public static HashSet<Material> attachableTypes = Sets.newHashSet(Material.SAPLING, Material.BED, Material.BED_BLOCK, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.LONG_GRASS, Material.DEAD_BUSH, Material.YELLOW_FLOWER,
			Material.RED_ROSE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.TORCH, Material.REDSTONE_WIRE, Material.WHEAT, Material.LADDER, Material.RAILS, Material.LEVER, Material.STONE_PLATE, Material.WOOD_PLATE,
			Material.REDSTONE_TORCH_ON, Material.REDSTONE_TORCH_OFF, Material.STONE_BUTTON, Material.CACTUS, Material.SUGAR_CANE, Material.COMMAND_REPEATING, Material.TRAP_DOOR, Material.PUMPKIN_STEM, Material.MELON_STEM, Material.VINE,
			Material.WATER_LILY, Material.BREWING_STAND, Material.COCOA, Material.TRIPWIRE, Material.TRIPWIRE_HOOK, Material.FLOWER_POT, Material.CARROT, Material.POTATO, Material.WOOD_BUTTON, Material.ANVIL, Material.GOLD_PLATE,
			Material.IRON_PLATE, Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_COMPARATOR_OFF, Material.DAYLIGHT_DETECTOR, Material.ACTIVATOR_RAIL, Material.WOOD_DOOR, Material.IRON_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
			Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.SIGN, Material.WALL_SIGN, Material.BED);

	@SuppressWarnings("deprecation")
	public static boolean isAttachable(int blockID) {
		return isAttachable(Material.getMaterial(blockID));
	}

	public static boolean isAttachable(Material mat) {
		return attachableTypes.contains(mat);
	}

	// -------------- get filePath
	public static String getTemplateFilePath(Location loc, ConfigConstructInfo info, String theme) throws CivException {
		if (info.type == ConstructType.Wonder) theme = "wonders";
		return Template.getTemplateFilePath(info.template_name, Template.getDirection(loc), theme);
	}

	public static String getTemplateFilePath(String template_name, String direction, String theme) throws CivException {
		if (template_name == null) throw new CivException("Попытка искать шаблон без имени");
		if (theme == null) theme = "default";
		template_name = template_name.replaceAll(" ", "_");
		String ss = "templates/themes/" + theme + "/" + template_name + "/" + template_name;
		if (direction != null && !direction.equals("")) ss = ss + "_" + direction;
		return (ss + ".def").toLowerCase();
	}

	public static String getCaveFilePath(String string) {
		return "templates/themes/caves/" + string + ".def";
	}

	public static String getUndoFilePath(String string) {
		return "templates/undo/" + string;
	}

	// ------------ file process
	public static void deleteFilePath(String filepath) {
		File templateFile = new File(filepath);
		templateFile.delete();
	}

	public static boolean checkFile(String filepath) {
		File templateFile = new File(filepath);
		return templateFile.exists();
	}

	// ------------------ Direction
	public static String getDirection(Location loc) {
		return invertDirection(parseDirection(loc));
	}

	public static String parseDirection(Location loc) {
		double rotation = (loc.getYaw() - 90) % 360;
		if (rotation < 0) {
			rotation += 360.0;
		}
		if (0 <= rotation && rotation < 22.5) {
			return "east"; // S > E
		} else
			if (22.5 <= rotation && rotation < 67.5) {
				return "east"; // SW > SE
			} else
				if (67.5 <= rotation && rotation < 112.5) {
					return "south"; // W > E
				} else
					if (112.5 <= rotation && rotation < 157.5) {
						return "west"; // NW > SW
					} else
						if (157.5 <= rotation && rotation < 202.5) {
							return "west"; // N > W
						} else
							if (202.5 <= rotation && rotation < 247.5) {
								return "west"; // NE > NW
							} else
								if (247.5 <= rotation && rotation < 292.5) {
									return "north"; // E > N
								} else
									if (292.5 <= rotation && rotation < 337.5) {
										return "east"; // SE > NE
									} else
										if (337.5 <= rotation && rotation < 360.0) {
											return "east"; // S > E
										} else {
											return "east";
										}
	}

	public static String invertDirection(String dir) {
		if (dir.equalsIgnoreCase("east")) return "west";
		if (dir.equalsIgnoreCase("west")) return "east";
		if (dir.equalsIgnoreCase("north")) return "south";
		if (dir.equalsIgnoreCase("south")) return "north";
		return null;
	}

	public static String getDirection(String filepath) {
		if (filepath.contains("_east")) return "east";
		if (filepath.contains("_south")) return "south";
		if (filepath.contains("_west")) return "west";
		if (filepath.contains("_north")) return "north";
		return "";
	}

	public static Template getTemplate(String filepath) {
		/* Attempt to get template statically. */
		if (filepath == null) return null;
		Template tpl = templateCache.get(filepath);
		if (tpl == null) {
			/* No template found in cache. Load it. */
			try {
				tpl = new Template(filepath);
			} catch (IOException | CivException e) {
				CivLog.error("Can not load template " + filepath);
				return null;
			}
			templateCache.put(filepath, tpl);
		}
		return tpl;
	}

}