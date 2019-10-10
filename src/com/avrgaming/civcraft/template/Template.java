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
import java.util.LinkedList;
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
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.PlayerBlockChangeUtil;
import com.avrgaming.civcraft.util.SimpleBlock;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Template {
	public SimpleBlock[][][] blocks;
	public int size_x;
	public int size_y;
	public int size_z;
	private String theme;
	private String direction;
	private String filepath;
	private Queue<SimpleBlock> sbs; //Blocks to add to main sync task queue;

	/* Save the command block locations when we init the template, so we dont have to search for them later. */
	public ArrayList<BlockCoord> commandBlockRelativeLocations = new ArrayList<BlockCoord>();
	public LinkedList<BlockCoord> doorRelativeLocations = new LinkedList<BlockCoord>();
	public LinkedList<BlockCoord> attachableLocations = new LinkedList<BlockCoord>();

	public Template() {
		sbs = new LinkedList<SimpleBlock>();
	}

	public void updateBlocksQueue(Queue<SimpleBlock> sbs) {
		SyncBuildUpdateTask.queueSimpleBlock(sbs);
		return;
	}

	public void buildConstructionScaffolding(Location center, Player player) {
		Block block = center.getBlock();
		ItemManager.setTypeIdAndData(block, ItemManager.getMaterialId(Material.CHEST), 0, false);
	}

	public void buildPreviewScaffolding(Location center, Player player) {
		Resident resident = CivGlobal.getResident(player);
		resident.undoPreview();
		Template tpl = this;
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				int count = 0;
				int step_floor = (tpl.size_x * tpl.size_z) / 225;
				for (int y = 0; y < tpl.size_y; y++) {
					for (int x = 0; x < tpl.size_x; x++) {
						for (int z = 0; z < tpl.size_z; z++) {
							// Must set to air in a different loop, since setting to air can break attachables.
							boolean bb = false;
							Block b = center.getBlock().getRelative(x, y, z);
							if ((x == 0 || x == tpl.size_x - 1) && (z == 0 || z == tpl.size_z - 1) && Math.floorMod(y, 2) == 0) bb = true;
							if (y == 0 && (Math.floorMod(x, step_floor) == 1 || Math.floorMod(z, step_floor) == 1)) bb = true;
							if ((y == tpl.size_y - 1) && (x == 0 || x == tpl.size_x - 1) && Math.floorMod(z, 3) == 0) bb = true;
							if ((y == tpl.size_y - 1) && (z == 0 || z == tpl.size_z - 1) && Math.floorMod(x, 3) == 0) bb = true;

							if (bb) {
								count++;
								ItemManager.sendBlockChange(player, b.getLocation(), 198, 0);
								resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
								if (count > 100) {
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
				}
			}
		}, 0);

//		for (int y = 0; y < this.size_y; y++) {
//			Block b = center.getBlock().getRelative(0, y, 0);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//
//			b = center.getBlock().getRelative(this.size_x - 1, y, this.size_z - 1);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//
//			b = center.getBlock().getRelative(this.size_x - 1, y, 0);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//
//			b = center.getBlock().getRelative(0, y, this.size_z - 1);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//
//		}
//
//		for (int x = 0; x < this.size_x; x++) {
//			Block b = center.getBlock().getRelative(x, this.size_y - 1, 0);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//
//			b = center.getBlock().getRelative(x, this.size_y - 1, this.size_z - 1);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//
//		}
//
//		for (int z = 0; z < this.size_z; z++) {
//			Block b = center.getBlock().getRelative(0, this.size_y - 1, z);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//
//			b = center.getBlock().getRelative(this.size_x - 1, this.size_y - 1, z);
//			ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//			resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//		}
//
//		for (int z = 0; z < this.size_z; z++) {
//			for (int x = 0; x < this.size_x; x++) {
//				Block b = center.getBlock().getRelative(x, 0, z);
//				ItemManager.sendBlockChange(player, b.getLocation(), CivData.BEDROCK, 0);
//				resident.previewUndo.put(new BlockCoord(b.getLocation()), new SimpleBlock(ItemManager.getTypeId(b), ItemManager.getData(b)));
//			}
//		}

	}

	public void buildScaffolding(Location center) {
		Template tpl = this;
		class AsyncTask extends CivAsyncTask {
			@Override
			public void run() {
				int blocksPerTick = 100000;
				try {
					int count = 0;
					Queue<SimpleBlock> sBs = new LinkedList<SimpleBlock>();
					for (int y = 0; y < tpl.size_y; y++) {
						for (int x = 0; x < tpl.size_x; x++) {
							for (int z = 0; z < tpl.size_z; z++) {
								// Must set to air in a different loop, since setting to air can break attachables.
								Block b = center.getBlock().getRelative(x, y, z);
								int type_id = 0;
								int data_id = 0;
								if ((x == 0 || x == tpl.size_x - 1) || (z == 0 || z == tpl.size_z - 1) //
										|| (y == 0) //
										|| (y == tpl.size_y - 1)// && (x == 0 || x == tpl.size_x - 1 || z == 0 || z == tpl.size_z - 1) 
								) {
									type_id = CivSettings.scaffoldingType;
									data_id = CivSettings.scaffoldingData;
								}

								SimpleBlock sb = new SimpleBlock(type_id, data_id);
								sb.worldname = center.getWorld().getName();
								sb.x = b.getX();
								sb.y = b.getY();
								sb.z = b.getZ();
								sBs.add(sb);
								count++;

								if (count < blocksPerTick) continue;

								SyncBuildUpdateTask.queueSimpleBlock(sBs);
								sBs.clear();
								count = 0;
								Thread.sleep(1000);
							}
						}
					}
					if (!sBs.isEmpty()) {
						SyncBuildUpdateTask.queueSimpleBlock(sBs);
						sBs.clear();
					}
				} catch (InterruptedException e) {}
			}
		}

		TaskMaster.asyncTask(new AsyncTask(), 0);
//
//		for (int y = 0; y < this.size_y; y++) {
//			Block b = center.getBlock().getRelative(0, y, 0);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//
//			b = center.getBlock().getRelative(this.size_x - 1, y, this.size_z - 1);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//
//			b = center.getBlock().getRelative(this.size_x - 1, y, 0);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//
//			b = center.getBlock().getRelative(0, y, this.size_z - 1);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//		}
//
//		for (int x = 0; x < this.size_x; x++) {
//			Block b = center.getBlock().getRelative(x, this.size_y - 1, 0);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//
//			b = center.getBlock().getRelative(x, this.size_y - 1, this.size_z - 1);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//		}
//
//		for (int z = 0; z < this.size_z; z++) {
//			Block b = center.getBlock().getRelative(0, this.size_y - 1, z);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//
//			b = center.getBlock().getRelative(this.size_x - 1, this.size_y - 1, z);
//			ItemManager.setTypeId(b, CivData.BEDROCK);
//		}
//
//		for (int z = 0; z < this.size_z; z++) {
//			for (int x = 0; x < this.size_x; x++) {
//				Block b = center.getBlock().getRelative(x, 0, z);
//				ItemManager.setTypeId(b, CivData.BEDROCK);
//			}
//		}

	}

	public void removeScaffolding(Location center) {
		for (int y = 0; y < this.size_y; y++) {
			Block b = center.getBlock().getRelative(0, y, 0);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);
			}

			b = center.getBlock().getRelative(this.size_x - 1, y, this.size_z - 1);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);
			}

			b = center.getBlock().getRelative(this.size_x - 1, y, 0);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);

			}

			b = center.getBlock().getRelative(0, y, this.size_z - 1);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);

			}
		}

		for (int x = 0; x < this.size_x; x++) {
			Block b = center.getBlock().getRelative(x, this.size_y - 1, 0);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);
			}

			b = center.getBlock().getRelative(x, this.size_y - 1, this.size_z - 1);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);
			}

		}

		for (int z = 0; z < this.size_z; z++) {
			Block b = center.getBlock().getRelative(0, this.size_y - 1, z);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);
			}

			b = center.getBlock().getRelative(this.size_x - 1, this.size_y - 1, z);
			if (ItemManager.getTypeId(b) == CivData.BEDROCK) {
				ItemManager.setTypeId(b, CivData.AIR);
				ItemManager.setData(b, 0, true);
			}

		}

	}

	public void saveUndoTemplate(String string, String subdir, Location center) throws CivException, IOException {

		String filepath = "templates/undo/" + subdir;
		File undo_tpl_file = new File(filepath);
		undo_tpl_file.mkdirs();

		FileWriter writer = new FileWriter(undo_tpl_file.getAbsolutePath() + "/" + string);

		//TODO Extend this to save paintings?
		writer.write(this.size_x + ";" + this.size_y + ";" + this.size_z + "\n");
		for (int x = 0; x < this.size_x; x++) {
			for (int y = 0; y < this.size_y; y++) {
				for (int z = 0; z < this.size_z; z++) {
					Block b = center.getBlock().getRelative(x, y, z);

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

	public void initUndoTemplate(String structureHash, String subdir) throws IOException, CivException {
		String filepath = "templates/undo/" + subdir + "/" + structureHash;

		File templateFile = new File(filepath);
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
		getTemplateBlocks(reader, size_x, size_y, size_z);
		reader.close();
	}

	/* This function will save a copy of the template currently building into the town's temp directory. It does this so that we: 1) Dont have to remember the
	 * template's direction when we resume 2) Can change the master template without messing up any builds in progress 3) So we can pick a random template and
	 * "resume" the correct one. (e.g. cottages) */
	public String getTemplateCopy(String masterTemplatePath, String string, Town town) {
		String copyTemplatePath = "templates/inprogress/" + town.getName();
		File inprogress_tpl_file = new File(copyTemplatePath);
		inprogress_tpl_file.mkdirs();

		// Copy File...
		File master_tpl_file = new File(masterTemplatePath);
		inprogress_tpl_file = new File(copyTemplatePath + "/" + string);

		try {
			Files.copy(master_tpl_file.toPath(), inprogress_tpl_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("Failure to copy file!");
			e.printStackTrace();
			return null;
		}

		return copyTemplatePath + "/" + string;
	}

	public void setDirection(Location loc) throws CivException {
		// Get the direction we are facing.
		//We want the direction the building should be facing, not the player.
		direction = TemplateStatic.getDirection(loc);
		if (direction == null) {
			throw new CivException(CivSettings.localize.localizedString("template_unknwonDirection"));
		}
	}

	public void resumeTemplate(String templatePath, Buildable buildable) throws IOException, CivException {
		this.setFilepath(templatePath);
		load_template(templatePath);
		buildable.setTotalBlockCount(size_x * size_y * size_z);
	}

	public void initTemplate(Location center, ConfigBuildableInfo info, String theme) throws CivException, IOException {
		this.setDirection(center);
		String templatePath = TemplateStatic.getTemplateFilePath(info.template_base_name, direction, "structures", theme);
		this.setFilepath(templatePath);
		load_template(templatePath);
	}

	public void initTemplate(Location center, Buildable buildable, String theme) throws IOException, CivException {
		this.setDirection(center);
		if (!buildable.hasTemplate()) {
			/* Certain structures are built procedurally such as walls and roads. They do not have a direction and do not have a template. */
			direction = "";
		}

		// Find the template file.
		this.setTheme(theme);
		String templatePath = TemplateStatic.getTemplateFilePath(center, buildable, theme);
		this.setFilepath(templatePath);
		load_template(templatePath);
		buildable.setTotalBlockCount(size_x * size_y * size_z);
	}

	public void initTemplate(Location center, Buildable buildable) throws CivException, IOException {
		initTemplate(center, buildable, "default");
	}

	public void load_template(String filepath) throws IOException, CivException {
		File templateFile = new File(filepath);
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
		getTemplateBlocks(reader, size_x, size_y, size_z);
		this.filepath = filepath;
		reader.close();
	}

	private void getTemplateBlocks(BufferedReader reader, int regionX, int regionY, int regionZ) throws NumberFormatException, IOException {
		String line;
		SimpleBlock blocks[][][] = new SimpleBlock[regionX][regionY][regionZ];
		// Read blocks from file.
		while ((line = reader.readLine()) != null) {
			String locTypeSplit[] = line.split(",");
			String location = locTypeSplit[0];
			String type = locTypeSplit[1];

			//Parse location
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

			SimpleBlock block = new SimpleBlock(blockId, blockData);

			if (blockId == CivData.WOOD_DOOR || blockId == CivData.IRON_DOOR || blockId == CivData.SPRUCE_DOOR || blockId == CivData.BIRCH_DOOR
					|| blockId == CivData.JUNGLE_DOOR || blockId == CivData.ACACIA_DOOR || blockId == CivData.DARK_OAK_DOOR) {
				this.doorRelativeLocations.add(new BlockCoord("", blockX, blockY, blockZ));
			}

			// look for signs.
			if (blockId == CivData.WALL_SIGN || blockId == CivData.SIGN) {
				if (locTypeSplit.length > 2) {
					// The first character on special signs needs to be a /.
					if (locTypeSplit[2] != null && !locTypeSplit[2].equals("") && locTypeSplit[2].charAt(0) == '/') {
						block.specialType = SimpleBlock.Type.COMMAND;

						// Got a command, save it.
						block.command = locTypeSplit[2];

						// Save any key values we find.
						if (locTypeSplit.length > 3) {
							for (int i = 3; i < locTypeSplit.length; i++) {
								if (locTypeSplit[i] == null || locTypeSplit[i].equals("")) continue;

								String[] keyvalue = locTypeSplit[i].split(":");
								if (keyvalue.length < 2) {
									CivLog.warning("Invalid keyvalue:" + locTypeSplit[i] + " in template:" + this.filepath);
									continue;
								}
								block.keyvalues.put(keyvalue[0].trim(), keyvalue[1].trim());
							}
						}

						/* This block coord does not point to a location in a world, just a template. */
						this.commandBlockRelativeLocations.add(new BlockCoord("", blockX, blockY, blockZ));

					} else {
						block.specialType = SimpleBlock.Type.LITERAL;
						// Literal sign, copy the sign into the simple block
						for (int i = 0; i < 4; i++) {
							try {
								block.message[i] = locTypeSplit[i + 2];
							} catch (ArrayIndexOutOfBoundsException e) {
								block.message[i] = "";
							}
						}
						this.attachableLocations.add(new BlockCoord("", blockX, blockY, blockZ));
					}
				}
			} else
				if (TemplateStatic.isAttachable(blockId)) this.attachableLocations.add(new BlockCoord("", blockX, blockY, blockZ));
			blocks[blockX][blockY][blockZ] = block;
		}

		this.blocks = blocks;
		return;
	}

	public void deleteUndoTemplate(String string, String subdir) throws CivException, IOException {
		String filepath = "templates/undo/" + subdir + "/" + string;
		File templateFile = new File(filepath);
		templateFile.delete();
	}

	public void deleteInProgessTemplate(String string, Town town) {
		String filepath = "templates/inprogress/" + town.getName() + "/" + string;
		File templateFile = new File(filepath);
		templateFile.delete();
	}

	public void previewEntireTemplate(Template tpl, Block cornerBlock, Player player) {
		PlayerBlockChangeUtil util = new PlayerBlockChangeUtil();
		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block b = cornerBlock.getRelative(x, y, z);
					try {
						util.addUpdateBlock("", new BlockCoord(b), tpl.blocks[x][y][z].getType(), tpl.blocks[x][y][z].getData());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		util.sendUpdate(player.getName());
	}

	public void buildUndoTemplate(Template tpl, Block cornerBlock) {
		HashMap<Chunk, Chunk> chunkUpdates = new HashMap<Chunk, Chunk>();

		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block b = cornerBlock.getRelative(x, y, z);
					if (TemplateStatic.isAttachable(b.getType())) {
						SimpleBlock sb = new SimpleBlock(b);
						sb.setTypeAndData(0, 0);
						sbs.add(sb);
					}
				}
			}
		}
		updateBlocksQueue(sbs);
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
					if (CivSettings.restrictedUndoBlocks.contains(sb.getMaterial())) sb.setType(CivData.AIR);
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
		updateBlocksQueue(sbs);
		sbs.clear();
	}
}
