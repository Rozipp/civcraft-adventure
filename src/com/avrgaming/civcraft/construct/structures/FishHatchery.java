package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;

public class FishHatchery extends Structure {

	private int level = 1;

	public FishHatchery(String id, Town town) {
		super(id, town);
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + this.getDisplayName() + "</u></b><br/>";
		out += CivSettings.localize.localizedString("Level") + " " + this.level;
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "cutlery";
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
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
		int count;

		for (count = 0; count < level; count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:" + count);
				return;
			}
			sign.setText(CivSettings.localize.localizedString("fishery_sign_pool") + "\n" + (count + 1));
			sign.update();
		}

		for (; count < getSigns().size(); count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:" + count);
				return;
			}
			sign.setText(CivSettings.localize.localizedString("fishery_sign_poolOffline"));
			sign.update();
		}
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		int special_id = Integer.parseInt(sign.getAction());
		if (special_id < this.level) {
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_fishery_pool_msg_online", (special_id + 1)));
		} else {
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_fishery_pool_msg_offline", (special_id + 1)));
		}
	}
	
	@Override
	public ArrayList<ConstructChest> getChestsById(String id) {
		if (id.equals("result")) return super.getChestsById("4");
		
		ArrayList<ConstructChest> chests = new ArrayList<>();
		ArrayList<ConstructChest> ch; 
		if ((ch = super.getChestsById("0")) != null) chests.addAll(ch);
		if ((ch = super.getChestsById("1")) != null) chests.addAll(ch);
		if ((ch = super.getChestsById("2")) != null) chests.addAll(ch);
		if ((ch = super.getChestsById("3")) != null) chests.addAll(ch);
		return chests;
	}
	
	@Override
	public void onPostBuild() {
		this.level = getTownOwner().BM.saved_fish_hatchery_level;
	}

}