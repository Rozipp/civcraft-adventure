package com.avrgaming.civcraft.interactive;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CallbackInterface;

public class BuildCallbackDbg implements CallbackInterface {

	private Player player;
	private Resident resident;

	public BuildCallbackDbg(Player player) throws CivException {
		this.player = player;
		resident = CivGlobal.getResident(player);
		GuiInventory inv = new GuiInventory(player, null);
		inv.setTitle(CivSettings.localize.localizedString("resident_structuresGuiHeading"));
		for (final ConfigBuildableInfo info : CivSettings.structures.values()) {
			inv.addGuiItem(GuiItems.newGuiItem()//
					.setTitle(info.displayName)//
					.setMaterial(Material.EMERALD_BLOCK)//
					.setLore("§6" + CivSettings.localize.localizedString("clicktobuild"))//
					.setCallbackGui(info.id));
			continue;
		}
		inv.addGuiItem(53, GuiItems.newGuiItem()//
				.setTitle("§e" + CivSettings.localize.localizedString("4udesa"))//
				.setMaterial(Material.DIAMOND_BLOCK)//
				.setLore("§6" + CivSettings.localize.localizedString("click_to_view"))//
				.setOpenInventory("Wonders", "true"));
		resident.setPendingCallback(this);
		inv.openInventory();
	}

	private ConfigBuildableInfo sinfo = null;
	private String templateTheme = null;

	@Override
	public void execute(String... strings) {
		if (sinfo == null) {
			String buildName = strings[0];
			try {
				sinfo = CivSettings.structures.get(buildName);
				if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildName);
				GuiInventory.closeInventory(player);

				GuiInventory.openGuiInventory(player, "ChoiseTemplate", sinfo.id);
			} catch (CivException e) {
				e.printStackTrace();
				sinfo = null;
				resident.clearInteractiveMode();
			}
			return;
		}

		if (templateTheme == null) {
			templateTheme = strings[0];

			Template tpl;
			try {
				String tplPath = Template.getTemplateFilePath(player.getLocation(), sinfo, templateTheme);
				tpl = Template.getTemplate(tplPath);
				if (tpl == null) throw new CivException("Не найден шаблон " + tplPath);

				tpl.buildTemplateDbg(new BlockCoord(player.getLocation()));
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			resident.clearInteractiveMode();
			return;
		}

	}

}
