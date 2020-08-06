/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.cache.PlayerLocationCache;
import com.avrgaming.civcraft.components.PlayerProximityComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class ScoutTower extends Structure {

	double range;
	private PlayerProximityComponent proximityComponent;

	private int reportSeconds = 60;
	private int count = 0;

	public ScoutTower(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public ScoutTower(String id, Town town) throws CivException {
		super(id, town);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	@Override
	public void loadSettings() {
		super.loadSettings();

		try {
			range = CivSettings.getDouble(CivSettings.warConfig, "scout_tower.range");
			
			int reportrate = (int) CivSettings.getDouble(CivSettings.warConfig, "scout_tower.update");
			if (this.getTown().getBuffManager().hasBuff("buff_colossus_coins_from_culture")
					&& this.getTown().getBuffManager().hasBuff("buff_great_lighthouse_tower_range")) {
				range = 600.0;
				reportrate = 60;
			} else {
				range = 400.0;
				reportrate = 120;
			}

			reportSeconds = reportrate;

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onPostBuild() {
		proximityComponent = new PlayerProximityComponent();
		proximityComponent.setCenter(new BlockCoord(getCenterLocation()));
		proximityComponent.setRadius(range);
		proximityComponent.createComponent(this);
	}
	private void scoutDebug(String str) {
		if (this.getCiv().scoutDebug && this.getCiv().scoutDebugPlayer != null) {
			Player player;
			try {
				player = CivGlobal.getPlayer(this.getCiv().scoutDebugPlayer);
			} catch (CivException e) {
				return;
			}
			CivMessage.send(player, CivColor.Yellow + "[ScoutDebug] " + str);
		}
	}

	@Override
	public int getMaxHitPoints() {
		double rate = 1.0;
		if (this.getTown().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) {
			rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
		}
		if (this.getTown().getBuffManager().hasBuff("buff_barricade")) {
			rate += this.getTown().getBuffManager().getEffectiveDouble("buff_barricade");
		}
		return (int) ((double) this.getInfo().max_hitpoints * rate);
	}
	
	@Override
	public void onSecondUpdate(CivAsyncTask task) {
		if (!CivGlobal.towersEnabled) return;
		HashSet<String> announced = new HashSet<String>();
		this.process(announced);
	}

	/* Asynchronously sweeps for players within the scout tower's radius. If it finds a player that is not in the civ, then it informs the town. If the town is
	 * the capitol, it informs the civ. */
	public void process(HashSet<String> alreadyAnnounced) {
		count++;
		if (count < reportSeconds) {
			return;
		}

		count = 0;
		boolean empty = true;

		for (PlayerLocationCache pc : proximityComponent.tryGetNearbyPlayers(true)) {
			empty = false;
			scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_inspectingPlayer") + pc.getName());
			Player player;
			try {
				player = CivGlobal.getPlayer(pc.getName());
			} catch (CivException e) {
				scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_notOnline"));
				return;
			}

			if (player.isOp() || player.getGameMode() != GameMode.SURVIVAL) {
				scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_isOP"));
				continue;
			}

			/* Do not re-announce players announced by other scout towers */
			if (alreadyAnnounced.contains(this.getCiv().getName() + ":" + player.getName())) {
				scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_alreadyAnnounced") + pc.getName());
				continue;
			}

			/* Always announce outlaws, so skip down to bottom. */
			String relationName = "";
			String relationColor = "";
			if (!this.getTown().isOutlaw(player.getName())) {
				/* do not announce residents in this civ */
				Resident resident = CivGlobal.getResident(player);
				if (resident != null && resident.hasTown() && resident.getCiv() == this.getCiv()) {
					scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_sameCiv"));
					continue;
				}

				/* Only announce hostile, war, and neutral players */
				Relation.Status relation = this.getCiv().getDiplomacyManager().getRelationStatus(player);
				switch (relation) {
					case PEACE :
					case ALLY :
//				case VASSAL:
//				case MASTER:
						scoutDebug(CivSettings.localize.localizedString("acoutTower_debug_ally"));
						continue;
					default :
						break;
				}

				relationName = relation.name();
				relationColor = Relation.getRelationColor(relation);
			} else {
				relationName = CivSettings.localize.localizedString("scoutTower_isOutlaw");
				relationColor = CivColor.Yellow;
			}

			Location center = this.getCenterLocation();

			if (center.getWorld() != this.getCorner().getLocation().getWorld()) {
				scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_wrongWorld"));
				continue;
			}

			if (center.distanceSquared(player.getLocation()) < range * range) {
				/* Notify the town or civ. */
				CivMessage.sendScout(this.getCiv(),
						CivSettings.localize.localizedString("var_scoutTower_detection",
								(relationColor + player.getName() + "(" + relationName + ")" + CivColor.White),
								(player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ()),
								this.getTown().getName()));
				alreadyAnnounced.add(this.getCiv().getName() + ":" + player.getName());
				CivMessage.send(player, CivColor.RoseItalic + CivSettings.localize.localizedString("event_found_by_scoutTower", this.getTown().getName()));
			}
		}

		if (empty) {
			scoutDebug(CivSettings.localize.localizedString("scoutTower_debug_emptyCache"));
		}
	}

	@Override
	public String getMarkerIconName() {
		return "tower";
	}

	public int getReportSeconds() {
		return reportSeconds;
	}

	public void setReportSeconds(int reportSeconds) {
		this.reportSeconds = reportSeconds;
	}
}
