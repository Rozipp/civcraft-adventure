package com.avrgaming.civcraft.interactive;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.WarCamp;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class InteractiveWarCampFound implements InteractiveResponse {

	public InteractiveWarCampFound() {
	}
	
	@Override
	public void respond(String message, Resident resident) {
		resident.clearInteractiveMode();

		if (!message.equalsIgnoreCase("yes")) {
			CivMessage.send(resident, CivSettings.localize.localizedString("interactive_warcamp_Cancel"));
			return;
		}
		
		WarCamp.newCamp(resident);
	}

}
