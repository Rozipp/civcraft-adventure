package com.avrgaming.civcraft.interactive;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConstructInfo;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
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
		this.resident = CivGlobal.getResident(player);
		GuiInventory.openGuiInventory(player, "Structure", "true");
	}

	private ConfigConstructInfo sinfo = null;
	private String templateTheme = null;

	@Override
	public void execute(String... strings) {
		if (sinfo == null) {
			String buildId = strings[0];
			try {
				sinfo = CivSettings.constructs.get(buildId);
				if (sinfo == null) throw new CivException(CivSettings.localize.localizedString("cmd_build_defaultUnknownStruct") + " " + buildId);
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
			GuiInventory.closeInventory(player);
			try {
				String tplPath = Template.getTemplateFilePath(player.getLocation(), sinfo, templateTheme);
				Template tpl = Template.getTemplate(tplPath);
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
