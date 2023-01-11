package com.avrgaming.civcraft.construct;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.threading.TaskMaster;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BuildableStatic {

	public static Location repositionCenterStatic(Location center, int templateYShift, Template tpl) {
		return repositionCenterStatic(center, templateYShift, tpl, false);
	}

	/** Перемещает координаты постройки так, что бы человек стоял в приблизительно среднем чанке переднего края. Если stepToForvard = true, то
	 * смещает постройку на чанк назад, что бы игрок не умер в блоках постройки */
	public static Location repositionCenterStatic(Location center, int templateYShift, Template tpl, Boolean stepToForvard) {
		Location loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
		if (tpl != null) {
			Direction dir = tpl.getDirection();
			double x_size = tpl.getSize_x();
			double z_size = tpl.getSize_z();

			int xc = (int) (x_size - 1) / 16;
			int zc = (int) (z_size - 1) / 16;
			switch (dir) {
			case west:
				loc.setX(loc.getX() - xc * 16 - (stepToForvard ? 16 : 0));
				loc.setZ(loc.getZ() - Math.floorDiv(zc + 1, 2) * 16);
				break;
			case north:
				loc.setX(loc.getX() - Math.floorDiv(xc, 2) * 16);
				loc.setZ(loc.getZ() - zc * 16 - (stepToForvard ? 16 : 0));
				break;
			case east:
				loc.setX(loc.getX() + (stepToForvard ? 16 : 0));
				loc.setZ(loc.getZ() - Math.floorDiv(zc, 2) * 16);
				break;
			case south:
				loc.setX(loc.getX() - Math.floorDiv((xc + 1), 2) * 16);
				loc.setZ(loc.getZ() + (stepToForvard ? 16 : 0));
				break;
			default:
				break;
			}
		}
		if (templateYShift != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(loc.getY() + templateYShift);
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
			Location cityHallLoc = civ.getCapitolLocation();
			if (cityHallLoc == null) continue;
			double distance = cityHallLoc.distance(loc);
			if (distance < requiredDistance) {
				throw new CivException(CivSettings.localize.localizedString("var_buildable_toocloseToSpawn1", requiredDistance));
			}
		}
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

	public static void buildPlayerPreview(Player player, Construct construct) {
		TaskMaster.asyncTask(() -> {
			Template tpl = construct.getTemplate();
			tpl.buildPreviewScaffolding(construct.getCorner(), player);
		}, 0);
	}
}