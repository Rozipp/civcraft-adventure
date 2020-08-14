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
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class FoundCivCallback implements CallbackInterface {

	private Player player;
	private Resident resident;
	private Structure cityhall;
	private Town town;
	private Civilization civ;

	public FoundCivCallback(Player player) throws CivException {
		this.player = player;
		this.resident = CivGlobal.getResident(player);
		if (resident == null) throw new CivException(CivSettings.localize.localizedString("var_civGlobal_noResident", player.getName()));
		if (resident.hasCamp()) throw new CivException(CivSettings.localize.localizedString("civ_found_mustleavecamp"));

		/* Build a preview for the Capitol structure. */
		cityhall = Structure.newStructure(player, player.getLocation(), "s_cityhall", null,true);
		town = new Town((Civilization) null);
		town.checkCanCreatedTown(resident, cityhall.getCenterLocation());
		civ = new Civilization();
		civ.checkCanCreatedCiv(player);
		
		GuiInventory.clearInventoryStack(player.getUniqueId());
		GuiInventory.openGuiInventory(player, "ChoiseTemplate", cityhall.getInfo().id);
	}

	private String templateTheme = null;
	private String structureValidatorfinish = null;
	private String civName = null;
	private String tagName = null;
	private String townName = null;
	private String interactiveConfirm = null;

	@Override
	public void execute(String... strings) {
		InteractiveGetName interactive;
		if (templateTheme == null) {
			templateTheme = strings[0];
			try {
				Template old_tpl = cityhall.getTemplate();
				String tplPath = Template.getTemplateFilePath(cityhall.getInfo().template_name, old_tpl.getDirection(), templateTheme);
				Template tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				cityhall.setTemplate(tpl);

				BuildableStatic.buildPlayerPreview(player, cityhall);
				GuiInventory.closeInventory(player);
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

			CivMessage.sendHeading(player, CivSettings.localize.localizedString("foundCiv_Heading"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("foundCiv_Prompt1"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("foundCiv_Prompt2"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("foundCiv_Prompt3"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("foundCiv_Prompt4"));
			CivMessage.send(player, " ");
			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("foundCiv_Prompt5"));
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("build_cancel_prompt"));

			interactive = new InteractiveGetName();
			interactive.cancelMessage = CivSettings.localize.localizedString("interactive_civ_cancel");
			interactive.invalidMessage = CivSettings.localize.localizedString("interactive_civ_invalid");
			interactive.undoPreviewCancel = true;
			resident.setInteractiveMode(interactive);
			return;
		}

		if (civName == null) {
			civName = strings[0];
			try {
				if (CivGlobal.getCivFromName(civName) != null || CivGlobal.getConqueredCiv(civName) != null) throw new InvalidNameException(CivSettings.localize.localizedString("var_civ_found_civExists", civName));
				civ.setName(civName);
				tagName = civName.substring(0, Math.min(5, civName.length()-1));
				if (!player.isOp() && (tagName.length() < 2 || tagName.length() > 5)) throw new InvalidNameException("§c" + CivSettings.localize.localizedString("cmd_prefix_illegalArgument"));
				if (CivGlobal.anybodyHasTag(tagName)) throw new InvalidNameException(CivColor.RoseBold + CivSettings.localize.localizedString("interactive_civtag_arleadyExists"));
				civ.setTag(tagName);
			} catch (InvalidNameException e) {
				civName = null;
				CivMessage.sendError(player, e.getMessage());
				return;
			}

			CivMessage.send((Object) player, (String) ("§a" + CivSettings.localize.localizedString("var_interactive_civ_success1", "§e" + civName + "§a")));
			// CivMessage.send((Object) player, (String) " ");
			// CivMessage.send((Object) player, (String) (CivColor.LightGreenBold + CivSettings.localize.localizedString("interactive_civ_success3")));
			// CivMessage.send((Object) player, (String) ("§a" + CivSettings.localize.localizedString("cmd_rename_help2", "§c" +
			// CivSettings.localize.localizedString("cmd_rename_help5"))));
			// CivMessage.send((Object) player, (String) ("§6" + CivSettings.localize.localizedString("cmd_rename_help3") + "§c" +
			// CivSettings.localize.localizedString("cmd_rename_help4")));
			// CivMessage.send((Object) player, (String) ("§7" + CivSettings.localize.localizedString("interactive_civ_tocancel")));
			// resident.setInteractiveMode(new InteractiveGetName());
			// return;
			// }
			//
			// if (tagName == null) {
			// tagName = strings[0];
			// try {
			// if (!player.isOp() && (tagName.length() < 2 || tagName.length() > 5)) throw new CivException("§c" +
			// CivSettings.localize.localizedString("cmd_prefix_illegalArgument"));
			// if (CivGlobal.anybodyHasTag(tagName)) throw new CivException(CivColor.RoseBold +
			// CivSettings.localize.localizedString("interactive_civtag_arleadyExists"));
			// civ.setTag(tagName);
			// } catch (Exception e) {
			// CivMessage.send(player, e.getMessage());
			// tagName = null;
			// return;
			// }
			//
			// CivMessage.send(player, "§a" + CivSettings.localize.localizedString("var_interactive_civtag_success1", "§e" + tagName + "§a" ));
			CivMessage.send(player, " ");
			CivMessage.send(player, CivColor.LightGreenBold + CivSettings.localize.localizedString("interactive_civ_success4"));
			CivMessage.send(player, "§7" + CivSettings.localize.localizedString("interactive_civ_tocancel"));

			interactive = new InteractiveGetName();
			interactive.cancelMessage = CivSettings.localize.localizedString("interactive_capitol_cancel");
			interactive.invalidMessage = CivSettings.localize.localizedString("interactive_capitol_invalidname");
			interactive.undoPreviewCancel = true;
			resident.setInteractiveMode(interactive);
			return;
		}

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

			CivMessage.sendHeading(player, CivSettings.localize.localizedString("interactive_capitol_confirmSurvey"));

			CivMessage.send(player, TownCommand.survey(player.getLocation()));
			CivMessage.send(player, "");
			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("interactive_capitol_confirmPrompt"));

			resident.setInteractiveMode(new InteractiveConfirm());
			return;
		}

		if (interactiveConfirm == null) {
			interactiveConfirm = strings[0];
			resident.clearInteractiveMode();
			resident.undoPreview();
			if (!interactiveConfirm.equalsIgnoreCase("yes")) {
				CivMessage.send(player, CivSettings.localize.localizedString("interactive_civ_cancelcreate"));
				return;
			}

			CivMessage.send(player, CivSettings.localize.localizedString("foundation_of_civilization_1", player.getName()));
			CivMessage.send(player, CivSettings.localize.localizedString("foundation_of_civilization_2"));
			CivMessage.send(player, CivSettings.localize.localizedString("foundation_of_civilization_3"));
			try {
				civ.createCiv(player, town, cityhall);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
				return;
			}
			return;
		}
	}
}
