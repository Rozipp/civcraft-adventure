
package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;
import gpl.AttributeUtil.Attribute;
import gpl.AttributeUtil.AttributeType;

import org.bukkit.event.player.PlayerItemHeldEvent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.items.components.ItemComponent;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class Health extends ItemComponent {
	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		attrs.add(Attribute.newBuilder().name("Health").
				type(AttributeType.GENERIC_MAX_HEALTH).
				amount(0).
				build());
		attrs.addLore(CivColor.Blue + this.getDouble("value") + " " + CivSettings.localize.localizedString("newItemLore_Defense"));
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {
			CivMessage.send((Object) resident, CivColor.Red + CivSettings.localize.localizedString("itemLore_Warning") + " - " + CivColor.LightGray
					+ CivSettings.localize.localizedString("itemLore_defenseHalfPower"));
		}
	}
}
