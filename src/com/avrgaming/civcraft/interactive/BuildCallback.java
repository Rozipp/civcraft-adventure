package com.avrgaming.civcraft.interactive;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.ChoiseTemplate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class BuildCallback implements CallbackInterface {

	private Player player;
	private Resident resident;
	private Buildable buildable;

	public BuildCallback(Player player, ConfigBuildableInfo sinfo, Town town) throws CivException {
		this.player = player;
		resident = CivGlobal.getResident(player);

		if (sinfo.id.equals("wonder_stock_exchange") && !town.canBuildStock(player)) {
			throw new CivException(CivColor.Red + CivSettings.localize.localizedString("var_buildStockExchange_nogoodCondition", "http://wiki.minetexas.com/index.php/Stock_Exchange"));
		}

		Structure replaceStructure = null;
		Location location = player.getLocation();
		String repStruct = sinfo.replace_structure;
		if (repStruct != null) {
			Vector dir = location.getDirection();
			replaceStructure = town.getStructureByType(repStruct);
			if (replaceStructure == null) throw new CivException("не найдено здание " + repStruct + " для замены");

			BlockCoord bc = replaceStructure.getCorner();
			location = bc.getRelative(0, -replaceStructure.getTemplateYShift(), 0).getLocation();
			location.setDirection(dir);
		}

		if (sinfo.isWonder)
			buildable = Wonder.newWonder(player, location, sinfo.id, town);
		else
			buildable = Structure.newStructure(player, location, sinfo.id, town,true);
		buildable.replaceStructure = replaceStructure;
		new ChoiseTemplate(player, buildable, this);
	}

	private String templateTheme = null;
	private String buildConfirm = null;

	@Override
	public void execute(String... strings) {
		if (templateTheme == null) {
			templateTheme = strings[0];
			CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildable_preview_heading"));
			CivMessage.send(player, CivColor.Yellow + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt1"));
			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("buildable_preview_prompt2"));

			resident.setInteractiveMode(new InteractiveConfirm());
			return;
		}
		if (buildConfirm == null) {
			buildConfirm = strings[0];
			if (!buildConfirm.equalsIgnoreCase("yes")) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_cancel"));
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}

			if (!buildable.validated) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_invalid"));
				buildConfirm = null;
				return;
			}

			if (!buildable.isValid() && !player.isOp()) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_invalidNotOP"));
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}

			try {
				resident.clearInteractiveMode();
				resident.undoPreview();
				if (buildable instanceof Wonder)
					// FIXME Сделать универсальным для билдабле
					buildable.getTown().buildWonder(player, (Wonder) buildable);
				else
					buildable.getTown().buildStructure(player, (Structure) buildable);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			return;
		}
	}

}
