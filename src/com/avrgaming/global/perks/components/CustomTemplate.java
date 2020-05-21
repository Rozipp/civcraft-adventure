package com.avrgaming.global.perks.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveCustomTemplateConfirm;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.perks.Perk;

public class CustomTemplate extends PerkComponent {

	@Override
	public void onActivate(Resident resident) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}

		Town town = resident.getTown();
		if (town == null) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("customTemplate_noTown"));
			return;
		}

		if (hasTownTemplate(town)) {
			CivMessage.sendError(player, CivColor.Rose + CivSettings.localize.localizedString("customTemplatE_alreadyBound"));
			return;
		}

		/* Send resident into interactive mode to confirm that they want to bind the template to this town. */
		resident.setInteractiveMode(new InteractiveCustomTemplateConfirm(resident.getName(), this));

	}

	private String getTemplateSessionKey(Town town) {
		return "customtemplate:" + town.getName() + ":" + this.getString("template");
	}
	private static String getTemplateSessionKey(Town town, String buildableBaseName) {
		return "customtemplate:" + town.getName() + ":" + buildableBaseName;
	}

	private static String getTemplateSessionValue(Perk perk, Resident resident) {
		return perk.getConfigId() + ":" + resident.getName();
	}

	public void bindTemplateToTown(Town town, Resident resident) {
		CivGlobal.getSessionDatabase().add(getTemplateSessionKey(town), getTemplateSessionValue(this.getParent(), resident), town.getCiv().getId(), town.getId(), 0);
	}

	public boolean hasTownTemplate(Town town) {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getTemplateSessionKey(town));

		for (SessionEntry entry : entries) {
			String[] split = entry.value.split(":");
			if (this.getParent().getConfigId().equals(split[0])) return true;
		}

		return false;
	}

	public static Set<Perk> getTemplatePerksForBuildable(Town town, String buildableBaseName) {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getTemplateSessionKey(town, buildableBaseName));
		Set<Perk> perks = new HashSet<Perk>();

		for (SessionEntry entry : entries) {
			String[] split = entry.value.split(":");

			Perk perk = Perk.staticPerks.get(split[0]);
			if (perk != null) {
				Perk tmpPerk = new Perk(perk.configPerk);
				tmpPerk.provider = split[1];
				perks.add(tmpPerk);
			} else {
				CivLog.warning("Unknown perk in session db:" + split[0]);
				continue;
			}
		}
		return perks;
	}

//	public String getTheme() {
//		return this.getString("theme");
//	}
	
	
//	public Template getTemplate(Player player, Buildable buildable) {
//		Template tpl = null;
//		try {
//			tpl = new Template(player.getLocation(), buildable, this.getString("theme"));
//			buildable.setTotalBlockCount(tpl.size_x * tpl.size_y * tpl.size_z);
//		} catch (CivException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return tpl;
//	}

}
