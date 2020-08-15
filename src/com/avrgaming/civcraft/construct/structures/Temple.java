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
import java.util.ArrayList;

import org.bukkit.inventory.Inventory;

import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.MultiInventory;

public class Temple extends Structure {

	private ConsumeLevelComponent consumeComp = null;

	public Temple(String id, Town town) throws CivException {
		super(id, town);
	}

	public Temple(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public ConsumeLevelComponent getConsumeComponent() {
		if (consumeComp == null) {
			consumeComp = (ConsumeLevelComponent) this.getComponent(ConsumeLevelComponent.class.getSimpleName());
		}
		return consumeComp;
	}

	@Override
	public String getDynmapDescription() {
		if (getConsumeComponent() == null) {
			return "";
		}

		String out = "";
		out += CivSettings.localize.localizedString("Level") + " " + getConsumeComponent().getLevel() + " " + getConsumeComponent().getCountString();
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "church";
	}

	public String getkey() {
		return this.getTown().getName() + "_" + this.getConfigId() + "_" + this.getCorner().toString();
	}

	public Result consume(CivAsyncTask task) throws InterruptedException {

		//Look for the temple's chest.
		ArrayList<ConstructChest> chests = this.getChestsById("1");
		if (chests.isEmpty()) return Result.STAGNATE;
		MultiInventory multiInv = new MultiInventory();
		// Make sure the chest is loaded and add it to the multi inv.
		for (ConstructChest c : chests) {
			task.syncLoadChunk(c.getCoord().getChunkCoord());
			Inventory tmp;
			try {
				tmp = task.getChestInventory(c.getCoord(), true);
			} catch (CivTaskAbortException e) {
				return Result.STAGNATE;
			}
			multiInv.addInventory(tmp);
		}
		getConsumeComponent().setMultiInventory(multiInv);
		getConsumeComponent().setConsumeRate(1.0);
		try {
			Result result = getConsumeComponent().processConsumption(this.getProfesionalComponent().isWork);
			getConsumeComponent().onSave();
			return result;
		} catch (IllegalStateException e) {
			CivLog.exception(this.getDisplayName() + " Process Error in town: " + this.getTown().getName() + " and Location: " + this.getCorner(), e);
			return Result.STAGNATE;
		}
	}

	@Override
	public void onHourlyUpdate(CivAsyncTask task) {
		Result result;
		try {
			result = this.consume(task);

			switch (result) {
				case STARVE :
					CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_temple_productionFell",
							getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
					return;
				case LEVELDOWN :
					CivMessage.sendTown(getTown(),
							CivColor.Rose + CivSettings.localize.localizedString("var_temple_lostalvl", getConsumeComponent().getLevel()));
					return;
				case STAGNATE :
					CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_temple_stagnated",
							getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
					return;
				case GROW :
					CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_temple_productionGrew",
							getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
					break;
				case LEVELUP :
					CivMessage.sendTown(getTown(),
							CivColor.LightGreen + CivSettings.localize.localizedString("var_temple_lvlUp", getConsumeComponent().getLevel()));
					break;
				case MAXED :
					CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_temple_maxed",
							getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
					break;
				case UNKNOWN :
					CivMessage.sendTown(getTown(), CivColor.DarkPurple + CivSettings.localize.localizedString("temple_unknown"));
					return;
				default :
					break;
			}

			Double culture = 0.0;
			if (result == Result.LEVELUP) {
				culture = getConsumeComponent().getConsumeLevelStorageResult(getConsumeComponent().getLevel() - 1);
			} else {
				culture = getConsumeComponent().getConsumeLevelStorageResult(getConsumeComponent().getLevel());
			}

			int total_culture = (int) Math.round(culture * this.getTown().getCottageRate());
//		if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_bonus")) {
//			total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_bonus");
//		}
			this.getTown().SM.addCulture(total_culture);
			this.getTown().save();

			CivMessage.sendTown(getTown(), CivColor.LightGreen
					+ CivSettings.localize.localizedString("var_temple_cultureGenerated", (CivColor.LightPurple + total_culture + CivColor.LightGreen)));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public int getLevel() {
		return this.getConsumeComponent().getLevel();
	}

	public int getCount() {
		return this.getConsumeComponent().getCount();
	}

	public int getMaxCount() {
		return getConsumeComponent().getConsumeLevelPointMax(getConsumeComponent().getLevel());
	}

	public Result getLastResult() {
		return this.getConsumeComponent().getLastResult();
	}

	public double getCultureGenerated() {
		return getConsumeComponent().getConsumeLevelStorageResult(getConsumeComponent().getLevel());
	}

	public void delevel() {
		if (getLevel() > 1) {
			getConsumeComponent().setLevel(getLevel() - 1);
			getConsumeComponent().setCount(0);
			getConsumeComponent().onSave();
		}
	}

	@Override
	public void delete(){
		if (getConsumeComponent() != null) {
			getConsumeComponent().onDelete();
		}
		super.delete();
	}

	public void onDestroy() {
		super.onDestroy();

		getConsumeComponent().setLevel(1);
		getConsumeComponent().setCount(0);
		getConsumeComponent().onSave();
	}

	public static int getTempleMaxLevel() {
		return CivSettings.consumeLevels.get("template").levels.size();
	}
	
}