package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class University extends Structure {

	public University(String id, Town town) {
		super(id, town);
	}

	@Override
	public String getMarkerIconName() {
		return "bronzestar";
	}
	
	private ConstructSign getSignFromSpecialId(int special_id) {
		for (ConstructSign sign : getSigns()) {
			int id = Integer.parseInt(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}
	
	@Override
	public void updateSignText() {
		int count = 0;
		for (; count < getSigns().size(); count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("University sign was null");
				return;
			}
			
			sign.setText("\n"+CivSettings.localize.localizedString("university_sign")+"\n"+
					this.getTownOwner().getName());
			
			sign.update();
		}
	}
	
	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		CivMessage.send(player, CivColor.Green+CivSettings.localize.localizedString("university_sign")+" "+this.getTownOwner().getName());
	}


}
