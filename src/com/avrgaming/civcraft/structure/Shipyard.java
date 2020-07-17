
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.components.AttributeBiomeRadiusPerLevel;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;

public class Shipyard extends WaterStructure {
	private ConstructSign respawnSign;
	private int index = 0;

	public Shipyard(String id, Town town) throws CivException {
		super(id, town);
	}

	public Shipyard(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public String getkey() {
		return getTown().getName() + "_" + this.getConfigId() + "_" + this.getCorner().toString();
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "anchor";
	}

	public double getHammersPerTile() {
		AttributeBiomeRadiusPerLevel attrBiome = (AttributeBiomeRadiusPerLevel) this.getComponent("AttributeBiomeBase");
		double base = attrBiome.getBaseValue();

		double rate = 1;
		rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.ADVANCED_TOOLING);
		return (rate * base);
	}

	private RespawnLocationHolder getSelectedHolder() {
		ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
		return respawnables.get(this.index);
	}

	private void changeIndex(int newIndex) {
		ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
		if (this.respawnSign != null) {
			block4: {
				try {
					this.respawnSign.setText(CivSettings.localize.localizedString("stable_sign_respawnAt") + "\n" + CivColor.GreenBold + respawnables.get(newIndex).getRespawnName());
					this.index = newIndex;
				} catch (IndexOutOfBoundsException e) {
					if (respawnables.size() <= 0) break block4;
					this.respawnSign.setText(CivSettings.localize.localizedString("stable_sign_respawnAt") + "\n" + CivColor.GreenBold + respawnables.get(0).getRespawnName());
					this.index = 0;
				}
			}
			this.respawnSign.update();
		} else {
			CivLog.warning("Could not find civ spawn sign:" + this.getId() + " at " + this.getCorner());
		}
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) {
			return;
		}
		if (War.isWarTime()) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_wartime_returned"));
			return;
		}
		Boolean hasPermission = false;
		Civilization civ = this.getTown().getCiv();
		if (civ.hasResident(resident)) {
			hasPermission = true;
		}
		switch (sign.getAction()) {
		case "prev": {
			if (hasPermission.booleanValue()) {
				this.changeIndex(this.index - 1);
				break;
			}
			CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_Sign_noPermission"));
			break;
		}
		case "next": {
			if (hasPermission.booleanValue()) {
				this.changeIndex(this.index + 1);
				break;
			}
			CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_Sign_noPermission"));
			break;
		}
		case "respawn": {
			long timeNow;
			ArrayList<RespawnLocationHolder> respawnables = this.getTown().getCiv().getAvailableRespawnables();
			if (this.index >= respawnables.size()) {
				this.index = 0;
				this.changeIndex(this.index);
				CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_cannotRespawn"));
				return;
			}
			respawnables.get(this.index).getRandomRevivePoint();
			if (!hasPermission.booleanValue()) {
				CivMessage.sendError(resident, CivSettings.localize.localizedString("stable_Sign_noPermission"));
				return;
			}
			long nextTeleport = resident.getNextTeleport();
			if (nextTeleport > (timeNow = Calendar.getInstance().getTimeInMillis())) {
				SimpleDateFormat sdf = CivGlobal.dateFormat;
				CivMessage.sendError(resident, CivSettings.localize.localizedString("var_stable2_teleportNotAva", sdf.format(nextTeleport)));
				return;
			}
			RespawnLocationHolder holder = this.getSelectedHolder();
			Town toTeleport = CivGlobal.getCultureChunk(holder.getRandomRevivePoint().getLocation()).getTown();
			boolean hasShipyard = false;
			Location placeToTeleport = null;
			for (Structure structure : toTeleport.BM.getStructures()) {
				if (!(structure instanceof Shipyard)) continue;
				hasShipyard = true;
				placeToTeleport = ((Shipyard) structure).respawnSign.getCoord().getLocation();
				break;
			}
			if (!hasShipyard) {
				CivMessage.sendError(resident, CivSettings.localize.localizedString("shipyard_Sign_teleport_noShupyard", toTeleport.getName()));
				return;
			}
			if (!resident.getTreasury().hasEnough(1000.0)) {
				CivMessage.sendError(resident, CivSettings.localize.localizedString("shipyard_Sign_noMoney", CivColor.Gold + (2000.0 - resident.getTreasury().getBalance()), CivColor.Green + CivSettings.CURRENCY_NAME + CivColor.Red));
				return;
			}
			nextTeleport = timeNow + 60000L;
			CivMessage.send((Object) player, "§a" + CivSettings.localize.localizedString("stable_respawningAlert"));
			player.teleport(placeToTeleport);
			resident.getTreasury().withdraw(1000.0);
			resident.setNextTeleport(nextTeleport);
			resident.save();
		}
		}
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock commandBlock) {
		switch (commandBlock.command) {
		case "/next": {
			ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
			ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
			ConstructSign structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + (Object) ChatColor.BOLD + (Object) ChatColor.UNDERLINE + CivSettings.localize.localizedString("stable_sign_nextLocation"));
			structSign.setDirection(commandBlock.getData());
			structSign.setAction("next");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		}
		case "/prev": {
			ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
			ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
			ConstructSign structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + (Object) ChatColor.BOLD + (Object) ChatColor.UNDERLINE + CivSettings.localize.localizedString("stable_sign_previousLocation"));
			structSign.setDirection(commandBlock.getData());
			structSign.setAction("prev");
			structSign.update();
			this.addConstructSign(structSign);
			break;
		}
		case "/respawn": {
			ItemManager.setTypeId(absCoord.getBlock(), commandBlock.getType());
			ItemManager.setData(absCoord.getBlock(), commandBlock.getData());
			ConstructSign structSign = new ConstructSign(absCoord, this);
			structSign.setText(CivSettings.localize.localizedString("shipyard_sign"));
			structSign.setDirection(commandBlock.getData());
			structSign.setAction("respawn");
			structSign.update();
			this.addConstructSign(structSign);
			this.respawnSign = structSign;
			this.changeIndex(this.index);
		}
		}
	}
}
