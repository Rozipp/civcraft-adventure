package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class TownNewRequest implements QuestionResponseInterface {

	public Resident resident;
	public Resident leader;
	public Civilization civ;
	public String name;
	public CallbackInterface callback;

	public TownNewRequest(Resident resident, Civilization civ, String name, CallbackInterface callback) {
		this.resident = resident;
		this.civ = civ;
		this.name = name;
		this.callback = callback;
	}

	@Override
	public void processResponse(String param) {
		if (param.equalsIgnoreCase("accept")) {
			CivMessage.send(civ, CivColor.LightGreen + CivSettings.localize.localizedString("newTown_accepted1", leader.getName(), name));
			callback.execute(param);
		} else {
			CivMessage.send(resident, CivColor.LightGray + CivSettings.localize.localizedString("var_newTown_declined", leader.getName()));
		}
	}

	@Override
	public void processResponse(String response, Resident responder) {
		this.leader = responder;
		processResponse(response);
	}
}
