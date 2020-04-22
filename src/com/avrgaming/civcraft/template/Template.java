/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Template {
	public SimpleBlock[][][] blocks;
	public int size_x;
	public int size_y;
	public int size_z;
	private String direction;
	private String filepath;
	public int totalBlocks;

	/* Save the command block locations when we init the template, so we dont have
	 * to search for them later. */
	public ArrayList<SimpleBlock> commandBlockRelativeLocations = new ArrayList<SimpleBlock>();
	public LinkedList<SimpleBlock> doorRelativeLocations = new LinkedList<SimpleBlock>();
	public LinkedList<SimpleBlock> attachableLocations = new LinkedList<SimpleBlock>();

	public Template(String filepath) throws IOException, CivException {
		this.filepath = filepath;
		this.direction = Template.getDirection(filepath);
		load_template();
	}

	public void load_template() throws IOException, CivException {
		File templateFile = new File(this.filepath);
		BufferedReader reader = new BufferedReader(new FileReader(templateFile));

		// Read first line and get size.
		String line = null;
		line = reader.readLine();
		if (line == null) {
			reader.close();
			throw new CivException(CivSettings.localize.localizedString("template_invalidFile") + " " + filepath);
		}

		String split[] = line.split(";");
		size_x = Integer.valueOf(split[0]);
		size_y = Integer.valueOf(split[1]);
		size_z = Integer.valueOf(split[2]);
		this.totalBlocks = 0;
		SimpleBlock blocks[][][] = new SimpleBlock[size_x][size_y][size_z];
		// Read blocks from file.
		while ((line = reader.readLine()) != null) {
			String locTypeSplit[] = line.split(",");
			String location = locTypeSplit[0];
			String type = locTypeSplit[1];

			// Parse location
			String locationSplit[] = location.split(":");
			int blockX, blockY, blockZ;
			blockX = Integer.valueOf(locationSplit[0]);
			blockY = Integer.valueOf(locationSplit[1]);
			blockZ = Integer.valueOf(locationSplit[2]);

			// Parse type
			String typeSplit[] = type.split(":");
			int blockId, blockData;
			blockId = Integer.valueOf(typeSplit[0]);
			blockData = Integer.valueOf(typeSplit[1]);

			if (blockId != 0)
				this.totalBlocks++;
			SimpleBlock sblock = new SimpleBlock("", blockX, blockY, blockZ, blockId, blockData);

			if (blockId == CivData.WOOD_DOOR || blockId == CivData.IRON_DOOR || blockId == CivData.SPRUCE_DOOR || blockId == CivData.BIRCH_DOOR || blockId == CivData.JUNGLE_DOOR || blockId == CivData.ACACIA_DOOR
					|| blockId == CivData.DARK_OAK_DOOR) {
				this.doorRelativeLocations.add(sblock);
			}

			// look for signs.
			if (blockId == CivData.WALL_SIGN || blockId == CivData.SIGN) {
				if (locTypeSplit.length > 2) {
					// The first character on special signs needs to be a /.
					if (locTypeSplit[2] != null && !locTypeSplit[2].equals("") && locTypeSplit[2].charAt(0) == '/') {
						sblock.specialType = SimpleBlock.Type.COMMAND;

						// Got a command, save it.
						sblock.command = locTypeSplit[2];

						// Save any key values we find.
						if (locTypeSplit.length > 3) {
							for (int i = 3; i < locTypeSplit.length; i++) {
								if (locTypeSplit[i] == null || locTypeSplit[i].equals(""))
									continue;

								String[] keyvalue = locTypeSplit[i].split(":");
								if (keyvalue.length < 2) {
									CivLog.warning("Invalid keyvalue:" + locTypeSplit[i] + " in template:" + this.filepath);
									continue;
								}
								sblock.keyvalues.put(keyvalue[0].trim(), keyvalue[1].trim());
							}
						}

						/* This block coord does not point to a location in a world, just a template. */
						this.commandBlockRelativeLocations.add(sblock);

					} else {
						sblock.specialType = SimpleBlock.Type.LITERAL;
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
			} else if (Template.isAttachable(blockId))
				this.attachableLocations.add(sblock);
			blocks[blockX][blockY][blockZ] = sblock;
		}

		this.blocks = blocks;
		reader.close();
	}

	public List<SimpleBlock> getBlocksForLayer(int y) {
		List<SimpleBlock> ret = new ArrayList<SimpleBlock>();
		for (int x = 0; x < size_x; x++) {
			for (int z = 0; z < size_z; z++) {
				ret.add(blocks[x][y][z]);
			}
		}
		return ret;
	}

	public void buildPreviewScaffolding(Location center, Player player) {
		Resident resident = CivGlobal.getResident(player);
		Template tpl = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				resident.undoPreview();
				int count = 0;
				for (int y = 0; y < tpl.size_y; y++) {
					for (int x = 0; x < tpl.size_x; x++) {
						for (int z = 0; z < tpl.size_z; z++) {
							boolean bb = false;
							Block b = center.getBlock().getRelative(x, y, z);
							if ((x == 0 || x == tpl.size_x - 1) && (z == 0 || z == tpl.size_z - 1) && Math.floorMod(y, 2) == 0)
								bb = true;
							if (y == 0 && (Math.floorMod(x + z, 3) == 1))
								bb = true;
							if ((y == tpl.size_y - 1) && (x == 0 || x == tpl.size_x - 1) && Math.floorMod(z, 3) == 0)
								bb = true;
							if ((y == tpl.size_y - 1) && (z == 0 || z == tpl.size_z - 1) && Math.floorMod(x, 3) == 0)
								bb = true;
							if (!bb)
								continue;

							count++;
							ItemManager.sendBlockChange(player, b.getLocation(), 85, 0);
							resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));

							if (count < 1000)
								continue;
							count = 0;
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}, 10);
	}

	public void buildScaffolding(BlockCoord corner) {
		Template tpl = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				int blocksPerTick = 100000;
				try {
					int count = 0;
					Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
					for (int y = tpl.size_y - 1; y >= 0; y--) {
						for (int x = 0; x < tpl.size_x; x++) {
							for (int z = 0; z < tpl.size_z; z++) {
								// Must set to air in a different loop, since setting to air can break
								// attachables.
								Block b = corner.getBlock().getRelative(x, y, z);
								boolean isPutBlock = (x == 0 || x == tpl.size_x - 1 || z == 0 || z == tpl.size_z - 1) || (y == 0) || (y == tpl.size_y - 1);
								// && (x == 0 || x == tpl.size_x - 1 || z == 0 || z == tpl.size_z - 1)

								SimpleBlock sb = (isPutBlock) ? new SimpleBlock(CivSettings.scaffoldingType, CivSettings.scaffoldingData) : new SimpleBlock(CivData.AIR, 0);
								sb.worldname = corner.getWorldname();
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
					}
					if (!sbs.isEmpty()) {
						SyncBuildUpdateTask.queueSimpleBlock(sbs);
						sbs.clear();
					}
				} catch (InterruptedException e) {
				}
			}
		}, 10);
	}

	public void removeScaffolding(Location center) {
		Template tpl = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				try {
					int count = 0;
					Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
					for (int y = 0; y < tpl.size_y; y++) {
						for (int x = 0; x < tpl.size_x; x++) {
							for (int z = 0; z < tpl.size_z; z++) {
								// Must set to air in a different loop, since setting to air can break
								// attachables.
								Block b = center.getBlock().getRelative(x, y, z);
								boolean bb = false;
								if (x == 0 || x == tpl.size_x - 1 || z == 0 || z == tpl.size_z - 1)
									bb = true;
								if (y == 0)
									bb = true;
								if (y == tpl.size_y - 1)
									bb = true;// && (x == 0 || x == tpl.size_x - 1 || z == 0 || z == tpl.size_z - 1)
								if (!bb)
									continue;

								if (ItemManager.getTypeId(b) == CivSettings.scaffoldingType)//
									ItemManager.setTypeIdAndData(b, CivData.AIR, 0, true);
								count++;

								if (count < 10000)
									continue;

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
				} catch (InterruptedException e) {
				}
			}
		}, 0);
		//
		//		for (int y = 0; y < this.size_y; y++) {
		//			Block b = center.getBlock().getRelative(0, y, 0);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//			}
		//
		//			b = center.getBlock().getRelative(this.size_x - 1, y, this.size_z - 1);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//			}
		//
		//			b = center.getBlock().getRelative(this.size_x - 1, y, 0);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//
		//			}
		//
		//			b = center.getBlock().getRelative(0, y, this.size_z - 1);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//
		//			}
		//		}
		//
		//		for (int x = 0; x < this.size_x; x++) {
		//			Block b = center.getBlock().getRelative(x, this.size_y - 1, 0);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//			}
		//
		//			b = center.getBlock().getRelative(x, this.size_y - 1, this.size_z - 1);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//			}
		//
		//		}
		//
		//		for (int z = 0; z < this.size_z; z++) {
		//			Block b = center.getBlock().getRelative(0, this.size_y - 1, z);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//			}
		//
		//			b = center.getBlock().getRelative(this.size_x - 1, this.size_y - 1, z);
		//			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
		//				ItemManager.setTypeId(b, CivData.AIR);
		//				ItemManager.setData(b, 0, true);
		//			}
		//
		//		}

	}

	public void saveUndoTemplate(String string, BlockCoord corner) throws CivException, IOException {
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
							String signText = "";
							for (String line : sign.getLines()) {
								signText += line + ",";
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
		Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
		// Not Attachable blocks
		for (int y = 0; y < this.size_y; ++y) {
			for (int x = 0; x < this.size_x; ++x) {
				for (int z = 0; z < this.size_z; ++z) {
					SimpleBlock sb = this.blocks[x][y][z];
					if (Template.isAttachable(sb.getMaterial()))
						continue;
					sbs.add(new SimpleBlock(corner, sb));
				}
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
		// Attachable blocks
		for (int y = 0; y < this.size_y; ++y) {
			for (int x = 0; x < this.size_x; ++x) {
				for (int z = 0; z < this.size_z; ++z) {
					SimpleBlock sb = this.blocks[x][y][z];
					if (!Template.isAttachable(sb.getMaterial()))
						continue;
					sbs.add(new SimpleBlock(corner, sb));
				}
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
	}

	public void debugBuildTemplateLayer(BlockCoord corner, int y, boolean attachable) {
		Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
		for (int x = 0; x < this.size_x; ++x) {
			for (int z = 0; z < this.size_z; ++z) {
				SimpleBlock sb = blocks[x][y][z];
				if (Template.isAttachable(sb.getMaterial()))
					continue;
				sbs.add(new SimpleBlock(corner, sb));
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
		// Attachable blocks
		if (!attachable)
			return;
		for (int x = 0; x < this.size_x; ++x) {
			for (int z = 0; z < this.size_z; ++z) {
				SimpleBlock sb = blocks[x][y][z];
				if (!Template.isAttachable(sb.getMaterial()))
					continue;
				sbs.add(new SimpleBlock(corner, sb));
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
	}

	@Deprecated
	public void buildUndoTemplate(Template tpl, Block cornerBlock) {
		Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
		HashMap<Chunk, Chunk> chunkUpdates = new HashMap<Chunk, Chunk>();

		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block b = cornerBlock.getRelative(x, y, z);
					if (Template.isAttachable(b.getType())) {
						SimpleBlock sb = new SimpleBlock(b);
						sb.setTypeAndData(0, 0);
						sbs.add(sb);
					}
				}
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();

		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block b = cornerBlock.getRelative(x, y, z);
					chunkUpdates.put(b.getChunk(), b.getChunk());

					if (ItemManager.getTypeId(b) == CivData.WALL_SIGN || ItemManager.getTypeId(b) == CivData.SIGN) {
						Sign s2 = (Sign) b.getState();
						s2.setLine(0, tpl.blocks[x][y][z].message[0]);
						s2.setLine(1, tpl.blocks[x][y][z].message[1]);
						s2.setLine(2, tpl.blocks[x][y][z].message[2]);
						s2.setLine(3, tpl.blocks[x][y][z].message[3]);
						s2.update();
					}

					SimpleBlock sb = tpl.blocks[x][y][z];
					if (CivSettings.restrictedUndoBlocks.contains(sb.getMaterial()))
						sb.setType(CivData.AIR);
					// Convert relative x,y,z to real x,y,z in world.
					sb.x = x + cornerBlock.getX();
					sb.y = y + cornerBlock.getY();
					sb.z = z + cornerBlock.getZ();
					sb.worldname = cornerBlock.getWorld().getName();
					//					sb.buildable = buildable;
					sbs.add(sb);
				}
			}
		}
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		sbs.clear();
	}

	/* Handles the processing of CivTemplates which store cubiods of blocks for
	 * later use. */
	public static HashMap<String, Template> templateCache = new HashMap<String, Template>();
	// -------------- Attachable Types
	public static HashSet<Material> attachableTypes = Sets.newHashSet(Material.SAPLING, Material.BED, Material.BED_BLOCK, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.LONG_GRASS, Material.DEAD_BUSH, Material.YELLOW_FLOWER,
			Material.RED_ROSE, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.TORCH, Material.REDSTONE_WIRE, Material.WHEAT, Material.LADDER, Material.RAILS, Material.LEVER, Material.STONE_PLATE, Material.WOOD_PLATE,
			Material.REDSTONE_TORCH_ON, Material.REDSTONE_TORCH_OFF, Material.STONE_BUTTON, Material.CACTUS, Material.SUGAR_CANE, Material.COMMAND_REPEATING, Material.TRAP_DOOR, Material.PUMPKIN_STEM, Material.MELON_STEM, Material.VINE,
			Material.WATER_LILY, Material.BREWING_STAND, Material.COCOA, Material.TRIPWIRE, Material.TRIPWIRE_HOOK, Material.FLOWER_POT, Material.CARROT, Material.POTATO, Material.WOOD_BUTTON, Material.ANVIL, Material.GOLD_PLATE,
			Material.IRON_PLATE, Material.REDSTONE_COMPARATOR_ON, Material.REDSTONE_COMPARATOR_OFF, Material.DAYLIGHT_DETECTOR, Material.ACTIVATOR_RAIL, Material.WOOD_DOOR, Material.IRON_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
			Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR, Material.SIGN, Material.WALL_SIGN);

	@SuppressWarnings("deprecation")
	public static boolean isAttachable(int blockID) {
		return isAttachable(Material.getMaterial(blockID));
	}

	public static boolean isAttachable(Material mat) {
		return attachableTypes.contains(mat);
	}

	// -------------- get filePath
	public static String getTemplateFilePath(Location loc, ConfigBuildableInfo info, String theme) {
		if (info.isWonder)
			theme = "wonders";
		return Template.getTemplateFilePath(info.template_name, Template.getDirection(loc), theme);
	}

	public static String getTemplateFilePath(String template_name, String direction, String theme) {
		if (template_name == null)
			return null;
		if (theme == null)
			theme = "default";
		template_name = template_name.replaceAll(" ", "_");
		String ss = "templates/themes/" + theme + "/" + template_name + "/" + template_name;
		if (!direction.equals(""))
			ss = ss + "_" + direction;
		return (ss + ".def").toLowerCase();
	}

	public static String getCaveFilePath(String string) {
		return "templates/themes/caves/" + string + ".def";
	}

	public static String getUndoFilePath(String string) {
		return "templates/undo/" + string;
	}

	public static String getInprogressFilePath(String string) {
		return "templates/inprogress/" + string;
	}

	// ------------ file process
	public static void deleteFilePath(String filepath) {
		File templateFile = new File(filepath);
		templateFile.delete();
	}

	/* This function will save a copy of the template currently building into the
	 * town's temp directory. It does this so that we: 1) Dont have to remember the
	 * template's direction when we resume 2) Can change the master template without
	 * messing up any builds in progress 3) So we can pick a random template and
	 * "resume" the correct one. (e.g. cottages) */
	public static void copyFilePath(String masterTemplatePath, String copyTemplatePath) {
		// Copy File...
		File master_tpl_file = new File(masterTemplatePath);
		File inprogress_tpl_file = new File(copyTemplatePath);

		try {
			Files.copy(master_tpl_file.toPath(), inprogress_tpl_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("Failure to copy file!");
			e.printStackTrace();
		}
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
		} else if (22.5 <= rotation && rotation < 67.5) {
			return "east"; // SW > SE
		} else if (67.5 <= rotation && rotation < 112.5) {
			return "south"; // W > E
		} else if (112.5 <= rotation && rotation < 157.5) {
			return "west"; // NW > SW
		} else if (157.5 <= rotation && rotation < 202.5) {
			return "west"; // N > W
		} else if (202.5 <= rotation && rotation < 247.5) {
			return "west"; // NE > NW
		} else if (247.5 <= rotation && rotation < 292.5) {
			return "north"; // E > N
		} else if (292.5 <= rotation && rotation < 337.5) {
			return "east"; // SE > NE
		} else if (337.5 <= rotation && rotation < 360.0) {
			return "east"; // S > E
		} else {
			return "east";
		}
	}

	public static String invertDirection(String dir) {
		if (dir.equalsIgnoreCase("east"))
			return "west";
		if (dir.equalsIgnoreCase("west"))
			return "east";
		if (dir.equalsIgnoreCase("north"))
			return "south";
		if (dir.equalsIgnoreCase("south"))
			return "north";
		return null;
	}

	public static String getDirection(String filepath) {
		if (filepath.contains("_east"))
			return "east";
		if (filepath.contains("_south"))
			return "south";
		if (filepath.contains("_west"))
			return "west";
		if (filepath.contains("_north"))
			return "north";
		return "";
	}

	public static Template getTemplate(String filepath) {
		/* Attempt to get template statically. */
		if (filepath == null)
			return null;
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

	public static boolean checkFile(String filepath) {
		File templateFile = new File(filepath);
		if (templateFile.exists())
			return true;
		else
			return false;
	}

}
