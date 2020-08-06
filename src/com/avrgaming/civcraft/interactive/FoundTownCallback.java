package com.avrgaming.civcraft.interactive;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.menu.TownCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Template;
import com.avrgaming.civcraft.construct.constructvalidation.StructureValidator;
import com.avrgaming.civcraft.construct.structures.BuildableStatic;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.questions.Question;
import com.avrgaming.civcraft.questions.TownNewRequest;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class FoundTownCallback implements CallbackInterface {

	private static int INVITE_TIMEOUT = 60000;// 60 seconds

	private Player player;
	private Resident resident;
	private Structure cityhall;
	private Town town;

	public FoundTownCallback(Player player) throws CivException {
		this.player = player;
		this.resident = CivGlobal.getResident(player);
		if (resident == null) throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noResident", player.getName()));
		if (!resident.hasTown()) throw new CivException(CivSettings.localize.localizedString("settler_errorNotRes"));

		cityhall = Structure.newStructure(player, player.getLocation(), "s_cityhall", null, true);

		town = new Town(resident.getCiv());
		town.checkCanCreatedTown(resident, cityhall.getCenterLocation());

		GuiInventory.openGuiInventory(player, "ChoiseTemplate", cityhall.getInfo().id);
	}

	private String templateTheme = null;
	private String structureValidatorfinish = null;
	private String townName = null;
	private String townCreationConfirm = null;
	private String interactiveConfirm = null;

	@Override
	public void execute(String... strings) {
		/* getTownName */
		if (templateTheme == null) {
			templateTheme = strings[0];
			try {
				Template old_tpl = cityhall.getTemplate();
				String tplPath = Template.getTemplateFilePath(cityhall.getInfo().template_name, old_tpl.getDirection(), templateTheme);
				Template tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				cityhall.setTemplate(tpl);

				BuildableStatic.buildPlayerPreview(player, cityhall);
				CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
				TaskMaster.asyncTask(new StructureValidator(player, cityhall, this), 0);
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

			CivMessage.sendHeading(player, CivSettings.localize.localizedString("settler_heading"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("settler_prompt1"));
			CivMessage.send(player, " ");
			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("settler_prompt2"));
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("build_cancel_prompt"));

			InteractiveGetName interactive = new InteractiveGetName();
			interactive.cancelMessage = CivSettings.localize.localizedString("interactive_town_cancelled");
			interactive.invalidMessage = CivSettings.localize.localizedString("interactive_town_nameInvalid");
			interactive.undoPreviewCancel = true;
			resident.setInteractiveMode(interactive);
			return;
		}

		/* getInteractiveConfirmTownCreation */
		if (townName == null) {
			townName = strings[0];

			if (!cityhall.validated) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_invalid"));
				townName = null;
				return;
			}

			if (!cityhall.isValid() && !player.isOp()) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("interactive_build_invalidNotOP"));
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}
			try {
				if (CivGlobal.getTownFromName(townName) != null) throw new InvalidNameException(CivSettings.localize.localizedString("var_town_found_errorNameExists", townName));
				town.setName(townName);
			} catch (InvalidNameException e) {
				townName = null;
				CivMessage.sendError(player, e.getMessage());
				return;
			}

			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_interactive_town_confirmName", CivColor.Yellow + townName + CivColor.LightGreen));
			CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_town_surveyResults"));
			CivMessage.send(player, TownCommand.survey(player.getLocation()));

			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_town_confirm"));
			resident.setInteractiveMode(new InteractiveConfirm());
			return;
		}

		if (townCreationConfirm == null) {
			townCreationConfirm = strings[0];
			if (!townCreationConfirm.equalsIgnoreCase("yes")) {
				CivMessage.send(player, CivSettings.localize.localizedString("interactive_town_cancel"));
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}

			TownNewRequest join = new TownNewRequest(resident, resident.getCiv(), townName, this);
			try {
				Question.questionLeaders(player, resident.getCiv(), CivSettings.localize.localizedString("var_interactive_town_alert", player.getName(), townName, (cityhall.getCorner().toStringNotWorld())), INVITE_TIMEOUT, join);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}
			resident.clearInteractiveMode();
			resident.undoPreview();
			CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("interactive_town_request"));
			return;
		}

		if (interactiveConfirm == null) {
			try {
				town.createTown(resident, cityhall);
			} catch (CivException e) {
				CivMessage.send(player, CivColor.Rose + e.getMessage());
				return;
			}
			CivMessage.global(CivSettings.localize.localizedString("var_FoundTownSync_Success", townName, resident.getCiv().getName()));
			return;
		}
	}

}
