package com.avrgaming.civcraft.interactive;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Camp;
import com.avrgaming.civcraft.construct.constructvalidation.StructureValidator;
import com.avrgaming.civcraft.construct.structures.BuildableStatic;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class FoundCampCallback implements CallbackInterface  {

	private Player player;
	private Resident resident;
	private Camp camp;

	public FoundCampCallback(Player player)  throws CivException{
		this.player = player;
		resident = CivGlobal.getResident(player);
		resident.setPendingCallback(this);
		try {
			camp = Camp.newCamp(player, player.getLocation());
			GuiInventory.openGuiInventory(player, "ChoiseTemplate", camp.getInfo().id);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

	private String templateTheme = null;
	private String structureValidatorfinish = null;
	private String campName = null;

	@Override
	public void execute(String... strings) {
		if (templateTheme == null) {
			templateTheme = strings[0];
			try {
				Template old_tpl = camp.getTemplate();
				String tplPath = Template.getTemplateFilePath(camp.getInfo().template_name, old_tpl.getDirection(), templateTheme);
				Template tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				camp.setTemplate(tpl);
				
				GuiInventory.closeInventory(player);
				BuildableStatic.buildPlayerPreview(player, camp);
				CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
				TaskMaster.asyncTask(new StructureValidator(player, camp, this), 0);
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
			
			CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildcamp_Heading"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("buildcamp_prompt1"));
			CivMessage.send(player, " ");
			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("buildcamp_prompt2"));
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("build_cancel_prompt"));

			InteractiveGetName interactive = new InteractiveGetName();
			interactive.cancelMessage = CivSettings.localize.localizedString("interactive_camp_cancel");
			interactive.invalidMessage = CivSettings.localize.localizedString("interactive_camp_invalid");
			interactive.undoPreviewCancel = true;
			resident.setInteractiveMode(interactive);
			return;
		}

		if (campName == null) {
			campName = strings[0];
			try {
				if (CivGlobal.getCamp(campName) != null) throw new InvalidNameException("(" + campName + ") " + CivSettings.localize.localizedString("camp_nameTaken"));
				camp.setName(campName);
			} catch (InvalidNameException e) {
				CivMessage.sendError(player, e.getMessage());
				campName = null;
				return;
			}

			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(player.getInventory().getItemInMainHand());
			if (craftMat == null || !craftMat.hasComponent("FoundCamp")) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("camp_missingItem"));
				resident.clearInteractiveMode();
				resident.undoPreview();
				return;
			}
			resident.clearInteractiveMode();
			resident.undoPreview();
			try {
				camp.createCamp(player);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			return;
		}
	}
}
