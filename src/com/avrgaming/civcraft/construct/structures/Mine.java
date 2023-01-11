package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.components.AttributeBiomeRadiusPerLevel;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.MultiInventory;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;

public class Mine extends Structure {

	public Mine(String id, Town town) {
		super(id, town);
	}

	public String getkey() {
		return getTownOwner().getName() + "_" + this.getConfigId() + "_" + this.getCorner().toString();
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "hammer";
	}

	public Result consume(CivAsyncTask task) throws InterruptedException {
		// Look for the mine's chest.
		ArrayList<ConstructChest> chests = this.getChestsById("0");
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
			CivLog.exception(this.getDisplayName() + " Process Error in town: " + this.getTownOwner().getName() + " and Location: " + this.getCorner(), e);
			return Result.STAGNATE;
		}
	}

	@Override
	public void onCivtickUpdate(CivAsyncTask task) {
		Result result;
		try {
			result = this.consume(task);
			switch (result) {
			case STARVE:
				CivMessage.sendTown(getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("var_mine_productionFell", getConsumeComponent().getLevel(), CivColor.LightGreen + getConsumeComponent().getCountString()));
				break;
			case LEVELDOWN:
				CivMessage.sendTown(getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("var_mine_lostalvl", getConsumeComponent().getLevel()));
				break;
			case STAGNATE:
				CivMessage.sendTown(getTownOwner(), CivColor.Rose + CivSettings.localize.localizedString("var_mine_stagnated", getConsumeComponent().getLevel(), CivColor.LightGreen + getConsumeComponent().getCountString()));
				break;
			case GROW:
				CivMessage.sendTown(getTownOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_mine_productionGrew", getConsumeComponent().getLevel(), getConsumeComponent().getCountString()));
				break;
			case LEVELUP:
				CivMessage.sendTown(getTownOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_mine_lvlUp", getConsumeComponent().getLevel()));
				break;
			case MAXED:
				CivMessage.sendTown(getTownOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_mine_maxed", getConsumeComponent().getLevel(), CivColor.LightGreen + getConsumeComponent().getCountString()));
				break;
			default:
				break;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public double getBonusHammers() {
		return getConsumeComponent().getConsumeLevelStorageResult(getConsumeComponent().getLevel());
	}

	public int getLevel() {
		if (!this.isComplete()) return 1;
		return this.getConsumeComponent().getLevel();
	}

	public double getHammersPerTile() {
		AttributeBiomeRadiusPerLevel attrBiome = (AttributeBiomeRadiusPerLevel) this.getComponent("AttributeBiomeRadiusPerLevel");
		double base = 1.0;

		if (attrBiome != null) base = attrBiome.getBaseValue();

		double rate = 1;
		rate += this.getTownOwner().getBuffManager().getEffectiveDouble(Buff.ADVANCED_TOOLING);
		return (rate * base);
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

	public static int getMineMaxLevel() {
		return CivSettings.consumeLevels.get("mine").levels.size();
	}
}
