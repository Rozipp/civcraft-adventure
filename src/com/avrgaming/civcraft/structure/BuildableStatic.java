package com.avrgaming.civcraft.structure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveBuildCommand;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.tutorial.Book;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.global.perks.Perk;

public class BuildableStatic {
	// Number of blocks to shift the structure away from us when built.
	public static final double SHIFT_OUT = 0;
	public static final int MIN_DISTANCE = 7;

	public static final double DEFAULT_HAMMERRATE = 1.0;

	/* This function is called before we build structures that do not have a town
	 * yet. This includes Capitols, Camps, and Town Halls. */
	public static void buildVerifyStatic(Player player, ConfigBuildableInfo info, Location centerLoc, CallbackInterface callback) throws CivException {

		Resident resident = CivGlobal.getResident(player);
		/* Look for any custom template perks and ask the player if they want to use
		 * them. */
		LinkedList<Perk> perkList = resident.getPersonalTemplatePerks(info);
		if (perkList.size() != 0) {

			/* Store the pending buildable. */
			resident.pendingBuildableInfo = info;
			resident.pendingCallback = callback;

			/* Build an inventory full of templates to select. */
			Inventory inv = Bukkit.getServer().createInventory(player, Book.MAX_CHEST_SIZE * 9);
			ItemStack infoRec = LoreGuiItem.build("Default " + info.displayName, ItemManager.getMaterialId(Material.WRITTEN_BOOK), 0, CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"));
			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithDefaultPersonalTemplate");
			inv.addItem(infoRec);

			for (Perk perk : perkList) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), perk.configPerk.type_id, perk.configPerk.data, CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"), CivColor.Gray + CivSettings.localize
						.localizedString("loreGui_template_providedBy") + " " + CivColor.LightBlue + CivSettings.localize.localizedString("loreGui_template_Yourself"));
				infoRec = LoreGuiItem.setAction(infoRec, "BuildWithPersonalTemplate");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getConfigId());
				inv.addItem(infoRec);
				player.openInventory(inv);
			}
			/* We will resume by calling buildPlayerPreview with the template when a gui
			 * item is clicked. */
			return;
		}

		String path = Template.getTemplateFilePath(info.template_name, Template.getDirection(player.getLocation()), null);

		Template tpl;
		tpl = Template.getTemplate(path);
		if (tpl == null)
			return;

		centerLoc = repositionCenterStatic(centerLoc, info.templateYShift, tpl);
		//		validate(player, null, tpl, centerLoc, callback);
		TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, callback), 0);
	}

	/* XXX this is called only on structures which do not have towns yet. For
	 * Example Capitols, Camps and Town Halls. */
	public static Location repositionCenterStatic(Location center, int templateYShift, Template tpl) throws CivException {
		Location loc = center.clone();

		loc = loc.getChunk().getBlock(0, loc.getBlockY(), 0).getLocation();
		if (tpl != null) {
			String dir = tpl.getDirection();
			double x_size = tpl.getSize_x();
			double z_size = tpl.getSize_z();

			int xc = (int) (x_size - 1) / 16;
			int zc = (int) (z_size - 1) / 16;
			switch (dir.toLowerCase()) {
			case "west":
				loc.setX(loc.getX() - xc * 16);
				loc.setZ(loc.getZ() - (zc + 1) / 2 * 16);
				break;
			case "north":
				loc.setX(loc.getX() - xc / 2 * 16);
				loc.setZ(loc.getZ() - zc * 16);
				break;
			case "east":
				loc.setX(loc.getX());
				loc.setZ(loc.getZ() - zc / 2 * 16);
				break;
			case "south":
				loc.setX(loc.getX() - (xc + 1) / 2 * 16);
				loc.setZ(loc.getZ());
				break;
			default:
				break;
			}
		}
		if (templateYShift != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(loc.getY() + templateYShift);
			if (loc.getY() < 1)
				throw new CivException(CivSettings.localize.localizedString("buildable_TooCloseToBedrock"));
		}

		return loc;
	}

	public static void validateDistanceFromSpawn(Location loc) throws CivException {
		/* Check distance from spawn. */
		double requiredDistance;
		try {
			requiredDistance = CivSettings.getDouble(CivSettings.civConfig, "global.distance_from_spawn");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		for (Civilization civ : CivGlobal.getAdminCivs()) {
			Location townHallLoc = civ.getCapitolTownHallLocation();
			if (townHallLoc == null)
				continue;
			double distance = townHallLoc.distance(loc);
			if (distance < requiredDistance) {
				throw new CivException(CivSettings.localize.localizedString("var_buildable_toocloseToSpawn1", requiredDistance));
			}
		}
	}

	public static int getBlockIDFromSnapshotMap(HashMap<ChunkCoord, ChunkSnapshot> snapshots, int absX, int absY, int absZ, String worldName) throws CivException {

		int chunkX = ChunkCoord.castToChunk(absX);
		int chunkZ = ChunkCoord.castToChunk(absZ);

		int blockChunkX = absX % 16;
		int blockChunkZ = absZ % 16;

		if (blockChunkX < 0)
			blockChunkX += 16;
		if (blockChunkZ < 0)
			blockChunkZ += 16;

		ChunkCoord coord = new ChunkCoord(worldName, chunkX, chunkZ);

		ChunkSnapshot snapshot = snapshots.get(coord);
		if (snapshot == null)
			throw new CivException("Snapshot for chunk " + chunkX + ", " + chunkZ + " in " + worldName + " not found for abs:" + absX + "," + absZ);

		return ItemManager.getBlockTypeId(snapshot, blockChunkX, absY, blockChunkZ);
	}

	public static int getReinforcementValue(int typeId) {
		switch (typeId) {
		case CivData.WATER:
		case CivData.WATER_RUNNING:
		case CivData.LAVA:
		case CivData.LAVA_RUNNING:
		case CivData.AIR:
		case CivData.COBWEB:
			return 0;
		case CivData.IRON_BLOCK:
			return 4;
		case CivData.STONE_BRICK:
			return 3;
		case CivData.STONE:
			return 2;
		default:
			return 1;
		}
	}

	public static void buildPlayerPreview(Player player, Location playerLoc, Buildable buildable) throws CivException, IOException {
		Template tpl = buildable.getTemplate();
		Location cornerLoc = buildable.repositionCenter(playerLoc, tpl);
		if (buildable.getReplaceStructure() != null) {
			Vector dir = cornerLoc.getDirection();
			Structure replaceStructure = buildable.getTown().getStructureByType(buildable.getReplaceStructure());
			if (replaceStructure == null)
				throw new CivException("не найдено здание " + buildable.getReplaceStructure() + " для замены");

			BlockCoord bc = replaceStructure.getCorner();
			cornerLoc = new Location(cornerLoc.getWorld(), bc.getX(), bc.getY() - replaceStructure.getTemplateYShift() + buildable.getTemplateYShift(), bc.getZ());
			cornerLoc.setDirection(dir);
		}
		buildable.setCorner(new BlockCoord(cornerLoc));
		buildable.setCenterLocation(buildable.getCorner().getLocation().add(tpl.size_x / 2, tpl.size_y / 2, tpl.size_z / 2));
		buildable.getTown().checkIsTownCanBuildStructure(buildable);

		tpl.buildPreviewScaffolding(cornerLoc, player);
		CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildable_preview_heading"));
		CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt1"));
		CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt2"));

		/* Run validation on position. */
		if (buildable.getReplaceStructure() == null) {
			TaskMaster.asyncTask(new StructureValidator(player, buildable), 0);
		} else {
			buildable.validated = true;
			buildable.setValid(true);
		}
		CivGlobal.getResident(player).setInteractiveMode(new InteractiveBuildCommand(buildable));
	}

	public static ArrayList<ChunkCoord> getChunkCoords(Buildable build) {
		ArrayList<ChunkCoord> ccs = new ArrayList<>();
		Template tpl = build.getTemplate();
		ChunkCoord cCorner = build.getCorner().getChunkCoord();
		for (int dx = 0; dx * 16 < tpl.size_x; dx++) {
			for (int dz = 0; dz * 16 < tpl.size_x; dz++) {
				ChunkCoord newCC = new ChunkCoord(cCorner.getWorldname(), cCorner.getX() + dx, cCorner.getZ() + dz);
				ccs.add(newCC);
			}
		}
		return ccs;
	}
}
