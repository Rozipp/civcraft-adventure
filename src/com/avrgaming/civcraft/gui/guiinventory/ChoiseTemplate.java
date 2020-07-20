package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.template.ConfigTheme;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class ChoiseTemplate extends GuiInventory {

	public ChoiseTemplate(Player player, String buildableId) throws CivException {
		super(player, player, buildableId);
		this.setPlayer(player);
		ConfigBuildableInfo sinfo = CivSettings.structures.get(buildableId);
		this.setTown(getResident().getTown());
		/* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
		/* Build an inventory full of templates to select. */
		this.setRow(3);
		this.setTitle("Выберите стиль постройки " + sinfo.displayName);
		for (ConfigTheme theme : ConfigTheme.getConfigThemeForConstruct(sinfo.template_name)) {
			this.addGuiItem(GuiItems.newGuiItem(ItemManager.createItemStack(theme.item_id, (short) theme.data, 1))//
					.setTitle(theme.display_name)//
					.setLore(CivColor.Gold + CivSettings.localize.localizedString("loreGui_template_clickToBuild"))//
					.setCallbackGui(theme.simple_name));
		}
	}

	@Override
	public void execute(String... strings) {
		getResident().getPendingCallback().execute(strings[0]);
		GuiInventory.closeInventory(getPlayer());
	}

}
