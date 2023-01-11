package com.avrgaming.civcraft.items.components;

import com.avrgaming.gpl.AttributeUtil;

import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.config.CivSettings;
import ua.rozipp.gui.GuiInventory;
import com.avrgaming.civcraft.util.CivColor;

public class TutorialBook extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		attrs.addLore(CivColor.Gold + CivSettings.localize.localizedString("tutorialBook_lore1"));
		attrs.addLore(CivColor.Rose + CivSettings.localize.localizedString("tutorialBook_lore2"));
	}

	public void onInteract(PlayerInteractEvent event) {
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) && !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		GuiInventory.openGuiInventory(event.getPlayer(), "GuiBook", null);
	}

	public void onItemSpawn(ItemSpawnEvent event) {
		event.setCancelled(true);
	}

}
