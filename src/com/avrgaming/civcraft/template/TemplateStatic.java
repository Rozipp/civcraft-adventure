package com.avrgaming.civcraft.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.wonders.Wonder;

public class TemplateStatic {
	/* Handles the processing of CivTemplates which store cubiods of blocks for later use. */
	public static HashMap<String, Template> templateCache = new HashMap<String, Template>();

	public static HashSet<Material> attachableTypes = new HashSet<Material>();
	@SuppressWarnings("deprecation")
	public static void initAttachableTypes() {
		attachableTypes.add(Material.SAPLING);
		attachableTypes.add(Material.BED);
		attachableTypes.add(Material.BED_BLOCK);
		attachableTypes.add(Material.POWERED_RAIL);
		attachableTypes.add(Material.DETECTOR_RAIL);
		attachableTypes.add(Material.LONG_GRASS);
		attachableTypes.add(Material.DEAD_BUSH);
		attachableTypes.add(Material.YELLOW_FLOWER);
		attachableTypes.add(Material.RED_ROSE);
		attachableTypes.add(Material.BROWN_MUSHROOM);
		attachableTypes.add(Material.RED_MUSHROOM);
		attachableTypes.add(Material.TORCH);
		attachableTypes.add(Material.REDSTONE_WIRE);
		attachableTypes.add(Material.WHEAT);
		attachableTypes.add(Material.LADDER);
		attachableTypes.add(Material.RAILS);
		attachableTypes.add(Material.LEVER);
		attachableTypes.add(Material.STONE_PLATE);
		attachableTypes.add(Material.WOOD_PLATE);
		attachableTypes.add(Material.REDSTONE_TORCH_ON);
		attachableTypes.add(Material.REDSTONE_TORCH_OFF);
		attachableTypes.add(Material.STONE_BUTTON);
		attachableTypes.add(Material.CACTUS);
		attachableTypes.add(Material.SUGAR_CANE);
		attachableTypes.add(Material.getMaterial(93)); //redstone repeater off
		attachableTypes.add(Material.getMaterial(94)); //redstone repeater on
		attachableTypes.add(Material.TRAP_DOOR);
		attachableTypes.add(Material.PUMPKIN_STEM);
		attachableTypes.add(Material.MELON_STEM);
		attachableTypes.add(Material.VINE);
		attachableTypes.add(Material.WATER_LILY);
		attachableTypes.add(Material.BREWING_STAND);
		attachableTypes.add(Material.COCOA);
		attachableTypes.add(Material.TRIPWIRE);
		attachableTypes.add(Material.TRIPWIRE_HOOK);
		attachableTypes.add(Material.FLOWER_POT);
		attachableTypes.add(Material.CARROT);
		attachableTypes.add(Material.POTATO);
		attachableTypes.add(Material.WOOD_BUTTON);
		attachableTypes.add(Material.ANVIL);
		attachableTypes.add(Material.GOLD_PLATE);
		attachableTypes.add(Material.IRON_PLATE);
		attachableTypes.add(Material.REDSTONE_COMPARATOR_ON);
		attachableTypes.add(Material.REDSTONE_COMPARATOR_OFF);
		attachableTypes.add(Material.DAYLIGHT_DETECTOR);
		attachableTypes.add(Material.ACTIVATOR_RAIL);
		attachableTypes.add(Material.WOOD_DOOR);
		attachableTypes.add(Material.IRON_DOOR);
		attachableTypes.add(Material.SPRUCE_DOOR);
		attachableTypes.add(Material.BIRCH_DOOR);
		attachableTypes.add(Material.JUNGLE_DOOR);
		attachableTypes.add(Material.ACACIA_DOOR);
		attachableTypes.add(Material.DARK_OAK_DOOR);
		attachableTypes.add(Material.SIGN);
		attachableTypes.add(Material.WALL_SIGN);
	}
	@SuppressWarnings("deprecation")
	public static boolean isAttachable(int blockID) {
		return isAttachable(Material.getMaterial(blockID));
	}
	public static boolean isAttachable(Material mat) {
		return attachableTypes.contains(mat);
	}

	public static String getTemplateFilePath(Location playerLocationForDirection, Buildable buildable, String theme) {
		String type = "structures";
		if (buildable instanceof Wonder) type = "wonders";
		return TemplateStatic.getTemplateFilePath(buildable.getTemplateBaseName(), TemplateStatic.getDirection(playerLocationForDirection), type, theme);
	}

	public static String getTemplateFilePath(String template_file, String direction, String type, String theme) {
		template_file = template_file.replaceAll(" ", "_");

		if (direction.equals("")) return ("templates/themes/" + theme + "/" + type + "/" + template_file + "/" + template_file + ".def").toLowerCase();

		return ("templates/themes/" + theme + "/" + type + "/" + template_file + "/" + template_file + "_" + direction + ".def").toLowerCase();
	}

	public static String getDirection(Location loc) {
		return invertDirection(parseDirection(loc));
	}

	public static Template getTemplate(String filepath, Location dirLoc) throws IOException, CivException {
		/* Attempt to get template statically. */
		Template tpl = templateCache.get(filepath);
		if (tpl == null) {
			/* No template found in cache. Load it. */
			tpl = new Template();
			tpl.load_template(filepath);
		}

		if (dirLoc != null) tpl.setDirection(dirLoc);
		return tpl;
	}

	public static void moveUndoTemplate(String string, String subdirInput, String subdirOutput) {
		try {
			Files.move(Paths.get("templates/undo", subdirInput, string), Paths.get("templates/undo", subdirOutput, string),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String parseDirection(Location loc) {
		double rotation = (loc.getYaw() - 90) % 360;
		if (rotation < 0) {
			rotation += 360.0;
		}
		if (0 <= rotation && rotation < 22.5) {
			return "east"; //S > E
		} else
			if (22.5 <= rotation && rotation < 67.5) {
				return "east"; //SW > SE
			} else
				if (67.5 <= rotation && rotation < 112.5) {
					return "south"; //W > E
				} else
					if (112.5 <= rotation && rotation < 157.5) {
						return "west"; //NW > SW
					} else
						if (157.5 <= rotation && rotation < 202.5) {
							return "west"; //N > W
						} else
							if (202.5 <= rotation && rotation < 247.5) {
								return "west"; //NE > NW
							} else
								if (247.5 <= rotation && rotation < 292.5) {
									return "north"; //E > N
								} else
									if (292.5 <= rotation && rotation < 337.5) {
										return "east"; //SE > NE
									} else
										if (337.5 <= rotation && rotation < 360.0) {
											return "east"; //S > E
										} else {
											return null;
										}
	}

	public static String invertDirection(String dir) {
		if (dir.equalsIgnoreCase("east")) return "west";
		if (dir.equalsIgnoreCase("west")) return "east";
		if (dir.equalsIgnoreCase("north")) return "south";
		if (dir.equalsIgnoreCase("south")) return "north";
		return null;
	}
}
