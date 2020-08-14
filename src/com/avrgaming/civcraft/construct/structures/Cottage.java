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
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class Cottage extends Structure {

	private ConsumeLevelComponent consumeComp = null;

	public Cottage(String id, Town town) throws CivException {
		super(id, town);
	}

	public Cottage(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public ConsumeLevelComponent getConsumeComponent() {
		if (consumeComp == null) consumeComp = (ConsumeLevelComponent) this.getComponent(ConsumeLevelComponent.class.getSimpleName());
		return consumeComp;
	}

	@Override
	public String getDynmapDescription() {
		if (getConsumeComponent() == null) return "";
		String out = "";
		out += CivSettings.localize.localizedString("Level") + " " + getConsumeComponent().getLevel() + " " + getConsumeComponent().getCountString();
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "house";
	}

	public String getkey() {
		return this.getTown().getName() + "_" + this.getConfigId() + "_" + this.getCorner().toString();
	}

	/* Returns true if the granary has been poisoned, false otherwise. */
	public boolean processIsPoison(MultiInventory inv) {
		// Check to make sure the granary has not been poisoned!
		String key = "posiongranary:" + getTown().getName();
		ArrayList<SessionEntry> entries;
		entries = CivGlobal.getSessionDatabase().lookup(key);
		int max_poison_ticks = -1;
		for (SessionEntry entry : entries) {
			int next = Integer.valueOf(entry.value);

			if (next > max_poison_ticks) {
				max_poison_ticks = next;
			}
		}

		if (max_poison_ticks > 0) {
			CivGlobal.getSessionDatabase().delete_all(key);
			max_poison_ticks--;

			if (max_poison_ticks > 0) CivGlobal.getSessionDatabase().add(key, "" + max_poison_ticks, this.getTown().getCiv().getId(), this.getTown().getId(), this.getId());

			// Add some rotten flesh to the chest lol
			CivMessage.sendTown(this.getTown(), CivColor.Rose + CivSettings.localize.localizedString("cottage_poisoned"));
			inv.addItemStackAsync(ItemManager.createItemStack(CivData.ROTTEN_FLESH, 4));
			return true;
		}
		return false;
	}

	@Override
	public void onCivtickUpdate(CivAsyncTask task) { //onHourlyUpdate
		if (!this.isActive()) return;

		/* Build a multi-inv from granaries. */
		MultiInventory multiInv = new MultiInventory();

		for (Structure struct : this.getTown().BM.getStructures()) {
			if (struct instanceof Granary) {
				ArrayList<ConstructChest> chests = struct.getChestsById("1");

				// Make sure the chunk is loaded and add it to the inventory.
				try {
					for (ConstructChest c : chests) {
						task.syncLoadChunk(c.getCoord().getChunkCoord());
						Inventory tmp;
						try {
							tmp = task.getChestInventory(c.getCoord(), true);
							multiInv.addInventory(tmp);
						} catch (CivTaskAbortException e) {
							e.printStackTrace();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		getConsumeComponent().setMultiInventory(multiInv);

		double cottage_consume_mod = 1.0; // allows buildings and govs to change the totals for cottage consumption.
		if (this.getTown().getBuffManager().hasBuff(Buff.REDUCE_CONSUME)) cottage_consume_mod *= this.getTown().getBuffManager().getEffectiveDouble(Buff.REDUCE_CONSUME);
		if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_consume")) cottage_consume_mod *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_consume");

		getConsumeComponent().setConsumeRate(cottage_consume_mod);
		Result result = Result.STAGNATE;
		try {
			result = getConsumeComponent().processConsumption(this.getProfesionalComponent().isWork);
			getConsumeComponent().onSave();
		} catch (IllegalStateException e) {
			CivLog.exception(this.getDisplayName() + " Process Error in town: " + this.getTown().getName() + " and Location: " + this.getCorner(), e);
		}

		/* Bail early for results that do not generate coins. */
		switch (result) {
		case STARVE:
			CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_cottage_starved_base", getConsumeComponent().getLevel(),
					CivSettings.localize.localizedString("var_cottage_status_starved", getConsumeComponent().getCountString()), CivSettings.CURRENCY_NAME));
			return;
		case LEVELDOWN:
			CivMessage.sendTown(getTown(),
					CivColor.Rose + CivSettings.localize.localizedString("var_cottage_starved_base", (getConsumeComponent().getLevel() + 1), CivSettings.localize.localizedString("var_cottage_status_lvlDown"), CivSettings.CURRENCY_NAME));
			return;
		case STAGNATE:
			CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_cottage_starved_base", getConsumeComponent().getLevel(),
					CivSettings.localize.localizedString("var_cottage_status_stagnated", getConsumeComponent().getCountString()), CivSettings.CURRENCY_NAME));
			return;
		case UNKNOWN:
			CivMessage.sendTown(getTown(), CivColor.DarkPurple + CivSettings.localize.localizedString("var_cottage_starved_unknwon", CivSettings.CURRENCY_NAME));
			return;
		default:
			break;
		}

		if (processIsPoison(multiInv)) return;

		/* Calculate how much money we made. */
		/* XXX leveling down doesnt generate coins, so we don't have to check it here. */
		Double coins = 0.0;
		if (result == Result.LEVELUP) {
			coins = getConsumeComponent().getConsumeLevelStorageResult(getConsumeComponent().getLevel() - 1);
		} else {
			coins = getConsumeComponent().getConsumeLevelStorageResult(getConsumeComponent().getLevel());
		}

		int total_coins = (int) Math.round(coins * this.getTown().getCottageRate());
		if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_bonus")) total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_bonus");
		if (this.getTown().getBuffManager().hasBuff("buff_hotel")) total_coins = (int) ((double) total_coins * this.getTown().getBuffManager().getEffectiveDouble("buff_hotel"));
		if (this.getCiv().getStockExchangeLevel() >= 1) total_coins = (int) ((double) total_coins * 1.3);
		if (this.getCiv().hasTechnologys("tech_taxation")) {
			double taxation_bonus;
			try {
				taxation_bonus = CivSettings.getDouble(CivSettings.techsConfig, "taxation_cottage_buff");
				total_coins *= taxation_bonus;
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
		}

		double taxesPaid = total_coins * this.getTown().getDepositCiv().getIncomeTaxRate();
		String stateMessage = "";
		switch (result) {
		case GROW:
			stateMessage = CivColor.Green + CivSettings.localize.localizedString("var_cottage_grew", getConsumeComponent().getCountString()) + CivColor.LightGreen;
			break;
		case LEVELUP:
			stateMessage = CivColor.Green + CivSettings.localize.localizedString("var_cottage_grew_lvlUp") + CivColor.LightGreen;
			break;
		case MAXED:
			stateMessage = CivColor.Green + CivSettings.localize.localizedString("var_cottage_grew_isMaxed", getConsumeComponent().getCountString()) + CivColor.LightGreen;
			break;
		default:
			break;
		}

		if (taxesPaid > 0) {
			CivMessage.sendTown(this.getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_cottage_grew_base", getConsumeComponent().getLevel(), stateMessage, total_coins, CivSettings.CURRENCY_NAME,
					CivColor.Yellow + CivSettings.localize.localizedString("var_cottage_grew_taxes", Math.floor(taxesPaid), this.getTown().getDepositCiv().getName())));
		} else {
			CivMessage.sendTown(this.getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_cottage_grew_base", getConsumeComponent().getLevel(), stateMessage, total_coins, CivSettings.CURRENCY_NAME, ""));
		}

		this.getTown().getTreasury().deposit(total_coins - taxesPaid);
		this.getTown().getDepositCiv().taxPayment(this.getTown(), taxesPaid);
	}

	public int getLevel() {
		return getConsumeComponent().getLevel();
	}

	public Result getLastResult() {
		return getConsumeComponent().getLastResult();
	}

	public int getCount() {
		return getConsumeComponent().getCount();
	}

	public int getMaxCount() {
		return getConsumeComponent().getConsumeLevelPointMax(getConsumeComponent().getLevel());
	}

	public double getCoinsGenerated() {
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
	public void delete() {
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

	public static int getCottageMaxLevel() {
		return CivSettings.consumeLevels.get("cottage").levels.size();
	}
}
