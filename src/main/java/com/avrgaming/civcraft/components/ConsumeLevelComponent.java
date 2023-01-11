/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.components;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigConsumeRecipe;
import com.avrgaming.civcraft.config.ConfigConsumeRecipe.ConsumeLevel;
import com.avrgaming.civcraft.config.SourceItem;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.MultiInventory;

@Getter
@Setter
public class ConsumeLevelComponent extends Component {

	private int level;
	private int count;
	private Result lastResult;
	private ConfigConsumeRecipe cConsR;

	/** Consumption mod rate, can be used to increase or decrease consumption rates. */
	private double consumeRate;

	/* Inventory we're trying to pull from. */
	private MultiInventory multInv;

	// consumeComp.createComponent(this);

	@Override
	public void createComponent(Construct constr, boolean async) {
		super.createComponent(constr, async);
		String consume_id = this.getString("consume_id");
		if (consume_id != null) {
			this.cConsR = CivSettings.consumeLevels.get(consume_id);
			CivLog.debug("consume_id = " + consume_id + "   " + ((this.cConsR == null) ? "this.cConsR == null" : "this.cConsR found"));
		} else
			CivLog.debug("consume_id = null " + consume_id);
	}

	/* Possible Results. */
	public enum Result {
		STAGNATE, GROW, STARVE, LEVELUP, LEVELDOWN, MAXED, UNKNOWN
	}

	public ConsumeLevelComponent() {
		this.level = 1;
		this.count = 0;
		this.consumeRate = 1.0;
		this.lastResult = Result.UNKNOWN;
	}

	private String getKey() {
		return getConstruct().getConfigId() + ":" + getConstruct().getId() + ":" + "levelcount";
	}

	private String getValue() {
		return this.level + ":" + this.count;
	}

	@Override
	public void onLoad() {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getKey());
		if (entries.size() == 0)
			getConstruct().sessionAdd(getKey(), getValue());
		else {
			String[] split = entries.get(0).value.split(":");
			this.level = Integer.valueOf(split[0]);
			this.count = Integer.valueOf(split[1]);
		}
	}

	@Override
	public void onSave() {
		if (getConstruct().getId() != 0) {
			TaskMaster.asyncTask(new Runnable() {
				@Override
				public void run() {
					ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getKey());
					if (entries.size() == 0)
						getConstruct().sessionAdd(getKey(), getValue());
					else
						CivGlobal.getSessionDatabase().update(entries.get(0).request_id, getKey(), getValue());
				}
			}, 0);
		}
	}

	public void onDelete() {
		if (getConstruct().getId() != 0) {
			TaskMaster.asyncTask(new Runnable() {
				@Override
				public void run() {
					CivGlobal.getSessionDatabase().delete_all(getKey());
				}
			}, 0);
		}
	}

	public void setMultiInventory(MultiInventory multInvs) {
		this.multInv = multInvs;
	}

	public Integer getMaxLevel() {
		return cConsR.levels.size();
	}

	public ConsumeLevel getConsumeLevel(Integer level) {
		return cConsR.levels.get(level);
	}

	public Integer getConsumeLevelPointMax(Integer level) {
		ConsumeLevel cl = getConsumeLevel(level);
		return (cl == null) ? null : cl.point;
	}

	public Double getConsumeLevelStorageResult(Integer level) {
		ConsumeLevel cl = getConsumeLevel(level);
		return (cl == null) ? null : cl.storage_result;
	}

	public StorageType getConsumeStorageType(Integer level) {
		return cConsR.storage_type;
	}

	public int getConsumedAmount(int amount) {
		return (int) Math.max(1, amount * this.consumeRate);
	}

	private boolean hasEnoughToConsume(MultiInventory sMInv, List<SourceItem> sourceItems) {
		// Проверка ести ли все предметы в сундуках в нужных количествах
		for (SourceItem si : sourceItems) {
			int cc = multInv.hasEnough(si, 1);
			CivLog.debug("found " + si.items[0] + "   count = " + cc);
			if (cc < 1) return false;
		}
		return true;
	}

	private void deleteConsumeItems(MultiInventory sMInv, List<SourceItem> sourceItems) {
		try {
			for (SourceItem si : sourceItems) {
				multInv.deleteFoundItems(si, 1);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Result processConsumption(boolean isWork) {
		lastResult = _processConsumption(isWork);
		return lastResult;
	}

	public Result _processConsumption(boolean isWork) {
		// HashMap<String, MultiInventory> multInvs = foundInventory();
		if (multInv == null) return Result.UNKNOWN;
		ConsumeLevel consumeLevel = getConsumeLevel(this.level);
		Integer currentPointMax = getConsumeLevelPointMax(this.level);
		if (currentPointMax == null) return Result.UNKNOWN;

		if (hasEnoughToConsume(multInv, consumeLevel.sourceItems)) {
			deleteConsumeItems(multInv, consumeLevel.sourceItems);
			if ((this.count + 1) >= currentPointMax) {
				// Level up?
				Integer nextCountMax = getConsumeLevelPointMax(this.level + 1);
				if (nextCountMax == null) return Result.MAXED;
				this.count = 0;
				if (hasEnoughToConsume(multInv, consumeLevel.sourceItems)) {
					// we have what we need for the next level, process it as a levelup.
					this.level++;
					return Result.LEVELUP;
				} else {
					// we don't have enough for the next level, process as a MAXED.
					this.count = currentPointMax;
					return Result.MAXED;
				}
			} else {
				this.count++;
				return Result.GROW;
			}
		} else {
			if (isWork) return Result.STARVE;
			if ((this.count - 1) < 0) {
				if (getConsumeLevelPointMax(this.level - 1) == null) return Result.STAGNATE;
				this.level--;
				return Result.LEVELDOWN;
			} else {
				this.count--;
				return Result.STARVE;
			}
		}
	}

	public String getCountString() {
		String out = "(" + this.count + "/";
		Integer currentCountMax = getConsumeLevelPointMax(this.level);
		if (currentCountMax != null)
			out += currentCountMax + ")";
		else
			out += "?)";
		return out;
	}
}
