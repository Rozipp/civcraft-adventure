package com.avrgaming.global.perks.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class CustomPersonalTemplate extends PerkComponent {
	
	@Override
	public void onActivate(Resident resident) {
		CivMessage.send(resident, CivColor.LightGreen+CivSettings.localize.localizedString("customTemplate_personal"));
	}
	
//	public String getTheme() {
//		return this.getString("theme");
//	}
//	
//	public Template getTemplate(Player player, ConfigBuildableInfo info) {
//		Template tpl = new Template();
//		try {
//			tpl.initTemplate(player.getLocation(), info, this.getString("theme"));
//		} catch (CivException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		return tpl;
//	}
}
