package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.construct.Transmuter;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class FishHatchery extends Structure {

	private int level = 1;
	public Transmuter transmuter;

	public FishHatchery(String id, Town town) throws CivException {
		super(id, town);
	}

	public FishHatchery(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void delete() {
		transmuter.stop();
		super.delete();
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

	public double getChance(double chance) {
		return chance;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	private ConstructSign getSignFromSpecialId(int special_id) {
		for (ConstructSign sign : getSigns()) {
			int id = Integer.valueOf(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}

	@Override
	public void updateSignText() {
		int count = 0;

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
		int special_id = Integer.valueOf(sign.getAction());
		if (special_id < this.level) {
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_fishery_pool_msg_online", (special_id + 1)));
		} else {
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_fishery_pool_msg_offline", (special_id + 1)));
		}
	}

	@Override
	public void onPostBuild() {
		this.level = getTown().saved_fish_hatchery_level;
		for (ConstructChest chest : this.getAllChests().values()) {
			int id;
			try {
				id = Integer.parseInt(chest.getChestId());
			} catch (NumberFormatException e) {
				continue;
			}
			if (id < level) chest.setChestId("source");
			if (id == 4) chest.setChestId("result");
		}
		transmuter = new Transmuter(this);
		this.transmuter.addRecipe("fishhatchery");
		if (CivGlobal.fisheryEnabled) this.transmuter.start();
	}

}