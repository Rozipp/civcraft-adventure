package com.avrgaming.civcraft.interactive;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class BuildCallback implements CallbackInterface {

	private Player player;
	private Resident resident;
	private Buildable buildable;

	public BuildCallback(Player player, ConfigBuildableInfo sinfo, Town town) throws CivException {
		this.player = player;
		this.resident = CivGlobal.getResident(player);

		if (sinfo.id.equals("wonder_stock_exchange") && !town.BM.canBuildStock(player)) {
			throw new CivException(CivColor.Red + CivSettings.localize.localizedString("var_buildStockExchange_nogoodCondition", "http://wiki.minetexas.com/index.php/Stock_Exchange"));
		}
		if (sinfo.isWonder)
			buildable = Wonder.newWonder(player, player.getLocation(), sinfo.id, town);
		else
			buildable = Structure.newStructure(player, player.getLocation(), sinfo.id, town, true);
		GuiInventory.openGuiInventory(player, "ChoiseTemplate", buildable.getInfo().id);
	}

	private String templateTheme = null;
	private String structureValidatorfinish = null;
	private String buildConfirm = null;

	@Override
	public void execute(String... strings) {
		if (templateTheme == null) {
			templateTheme = strings[0];
			try {
				Template old_tpl = buildable.getTemplate();
				String tplPath = Template.getTemplateFilePath(buildable.getInfo().template_name, old_tpl.getDirection(), templateTheme);
				Template tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				buildable.setTemplate(tpl);

				BuildableStatic.buildPlayerPreview(player, buildable);
				CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
				TaskMaster.asyncTask(new StructureValidator(player, buildable, this), 0);
				return;
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}
		}

		if (structureValidatorfinish == null) {
			structureValidatorfinish = "true";

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
				buildable.build(player);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			return;
		}
	}

}
