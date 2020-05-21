package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ChoiseTemplate;
import com.avrgaming.civcraft.construct.WarCamp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveConfirm;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
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
		try {
			warCamp = WarCamp.newWarCamp(player, player.getLocation());
			new ChoiseTemplate(player, warCamp, this);
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

	private String templateTheme = null;
	private String creationConfirm = null;

	@Override
	public void execute(String... strings) {
		if (templateTheme == null) {
			templateTheme = strings[0];
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
		}
		if (creationConfirm == null) {
			creationConfirm = strings[0];
			if (!creationConfirm.equalsIgnoreCase("yes")) {
				CivMessage.send(resident, CivSettings.localize.localizedString("interactive_warcamp_Cancel"));
				resident.clearInteractiveMode();
				return;
			}
			try {
				ItemStack stack = player.getInventory().getItemInMainHand();
				CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
				if (craftMat == null || !craftMat.hasComponent("FoundWarCamp")) throw new CivException(CivSettings.localize.localizedString("warcamp_missingItem"));
				warCamp.createWarCamp(player);
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
				resident.clearInteractiveMode();
				e.printStackTrace();
			}
		}
	}
}
