package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.construct.constructs.WarCamp;
import com.avrgaming.civcraft.construct.constructvalidation.StructureValidator;
import com.avrgaming.civcraft.construct.BuildableStatic;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.interactive.InteractiveConfirm;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class FoundWarCamp extends ItemComponent implements CallbackInterface {

	private Player player;
	private Resident resident;
	private WarCamp warCamp;

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET + CivColor.Gold + CivSettings.localize.localizedString("buildWarCamp_lore1"));
		attrUtil.addLore(ChatColor.RESET + CivColor.Rose + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
		player = event.getPlayer();
		resident = CivGlobal.getResident(player);
		resident.setPendingCallback(this);
		try {
			warCamp = WarCamp.newWarCamp(player, player.getLocation());
			GuiInventory.openGuiInventory(player, "ChoiseTemplate", warCamp.getInfo().id);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

	private String templateTheme = null;
	private String structureValidatorfinish = null;
	private String creationConfirm = null;

	@Override
	public void execute(String... strings) {
		if (templateTheme == null) {
			templateTheme = strings[0];
			
			try {
				Template old_tpl = warCamp.getTemplate();
				String tplPath = Template.getTemplateFilePath(warCamp.getInfo().template_name, old_tpl.getDirection(), templateTheme);
				Template tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);
				warCamp.setTemplate(tpl);

				BuildableStatic.buildPlayerPreview(player, warCamp);
				CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("build_checking_position"));
				TaskMaster.asyncTask(new StructureValidator(player, warCamp, this), 0);
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
			
			int warTimeout;
			try {
				warTimeout = CivSettings.getInteger(CivSettings.warConfig, "warcamp.rebuild_timeout");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}

			CivMessage.sendHeading(player, CivSettings.localize.localizedString("buildWarCamp_heading"));
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("buildWarCamp_prompt1"));
			CivMessage.send(player, CivColor.LightGreen + "   -" + CivSettings.localize.localizedString("buildWarCamp_prompt2"));
			CivMessage.send(player, CivColor.LightGreen + "   -" + CivSettings.localize.localizedString("var_buildWarCamp_prompt3", warTimeout));
			CivMessage.send(player, " ");
			CivMessage.send(player, CivColor.LightGreen + ChatColor.BOLD + CivSettings.localize.localizedString("buildWarCamp_prompt5"));
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("buildWarCamp_prompt6"));

			resident.setInteractiveMode(new InteractiveConfirm());
			return;
		}
		if (creationConfirm == null) {
			creationConfirm = strings[0];
			resident.clearInteractiveMode();
			resident.undoPreview();
			if (!creationConfirm.equalsIgnoreCase("yes")) {
				CivMessage.send(resident, CivSettings.localize.localizedString("interactive_warcamp_Cancel"));
				return;
			}
			try {
				ItemStack stack = player.getInventory().getItemInMainHand();
				CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
				if (craftMat == null || !craftMat.hasComponent("FoundWarCamp")) throw new CivException(CivSettings.localize.localizedString("warcamp_missingItem"));
				warCamp.createWarCamp(player);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
