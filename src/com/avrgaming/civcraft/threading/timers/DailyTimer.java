package com.avrgaming.civcraft.threading.timers;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.construct.wonders.*;
import com.avrgaming.civcraft.endgame.EndGameCheckTask;
import com.avrgaming.civcraft.event.DailyEvent;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

public class DailyTimer implements Runnable {

	public static ReentrantLock lock = new ReentrantLock();

	public DailyTimer() {
	}

	@Override
	public void run() {

		if (lock.tryLock()) {
			try {
				try {
					CivLog.info("---- Running Daily Timer -----");
					CivMessage.globalTitle(CivColor.LightBlue + CivSettings.localize.localizedString("general_upkeep_tick"), "");
					collectTownTaxes();
					payTownUpkeep();
					payCivUpkeep();
					checkAutoCapitulate();
					Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
					while (iter.hasNext()) {
						try {
							Structure struct = iter.next().getValue();
							struct.onDailyUpdate();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					for (Wonder wonder : CivGlobal.getWonders()) {
						try {
							wonder.onDailyUpdate();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					/* Check for any winners. */
					TaskMaster.asyncTask(new EndGameCheckTask(), 0);

				} finally {
					CivLog.info("Daily timer is finished, setting true.");
					CivMessage.globalTitle(CivColor.LightBlue + CivSettings.localize.localizedString("general_upkeep_tick_finish"), "");
					DailyEvent.dailyTimerFinished = true;
				}
			} finally {
				lock.unlock();
			}
		}

	}

	private void payCivUpkeep() {
		label:
		for (Wonder wonder : CivGlobal.getWonders()) {
			if (wonder != null) {
				switch (wonder.getConfigId()) {
					case "w_colossus":
						try {
							wonder.processCoinsFromCulture();
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
					case "w_notre_dame":
						try {
							((NotreDame) wonder).processPeaceTownCoins();
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
					case "w_colosseum":
						try {
							wonder.processCoinsFromColosseum();
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
					case "w_neuschwanstein":
						try {
							wonder.processCoinsFromNeuschwanstein();
						} catch (Exception e) {
							e.printStackTrace();
						}
						break label;
					case "w_statue_of_zeus":
						try {
							((StatueOfZeus) wonder).processBonuses();
							break;
						} catch (Exception e) {
							e.printStackTrace();
						}
						break;
				}
			}
		}

		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.isAdminCiv()) continue;
			try {
				double total = civ.payUpkeep();
				if (civ.getTreasury().inDebt()) civ.incrementDaysInDebt();
				CivMessage.sendCiv(civ, CivColor.Yellow + CivSettings.localize.localizedString("var_daily_civUpkeep", total, CivSettings.CURRENCY_NAME));
				civ.save();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void payTownUpkeep() {
		for (Town t : CivGlobal.getTowns()) {
			try {
				double total = t.payUpkeep();
				if (t.inDebt()) t.incrementDaysInDebt();

				t.save();
				CivMessage.sendTown(t, CivColor.Yellow + CivSettings.localize.localizedString("var_daily_townUpkeep", total, CivSettings.CURRENCY_NAME));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void collectTownTaxes() {
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.isAdminCiv()) continue;
			double total = 0;
			for (Town t : civ.getTowns()) {
				try {
					double taxrate = t.getDepositCiv().getIncomeTaxRate();
					double townTotal = 0;
					double taxesToCiv = total * taxrate;
					townTotal -= taxesToCiv;
					CivMessage.sendTown(t, CivSettings.localize.localizedString("var_daily_residentTaxes", townTotal, CivSettings.CURRENCY_NAME));
					t.depositTaxed(townTotal);

					if (t.getDepositCiv().getId() == civ.getId()) total += taxesToCiv;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (civ.isForSale()) {
				/* Civs for sale cannot maintain aggressive wars. */
				civ.clearAggressiveWars();
			}

			// TODO make a better messaging system...
			CivMessage.sendCiv(civ, CivSettings.localize.localizedString("var_daily_townTaxes", total, CivSettings.CURRENCY_NAME));
		}

	}

	private void checkAutoCapitulate() {
		for (Town town : CivGlobal.getTowns()) {
			int daysPassed = (int) (Calendar.getInstance().getTimeInMillis() - town.getConqueredDate()) / 24 / 60 / 60 / 1000;
			int auto_capitulate_days = 7;
			try {
				auto_capitulate_days = CivSettings.getInteger(CivSettings.civConfig, "civ.auto_capitulate_days");
			} catch (InvalidConfiguration e1) {
				e1.printStackTrace();
			}

			if (town.getConqueredDate() == 0L || daysPassed < auto_capitulate_days) continue;
			if (town.getMotherCiv() != null) {
				if (town.getMotherCiv().getCapitolId() == town.getId()) {
					String newCiv = "Error";
					for (Town var1 : town.getMotherCiv().getTowns()) {
						var1.setMotherCiv(null);
						var1.setConqueredDate(0L);
						var1.save();
						newCiv = town.getCiv().getName();
					}
					CivMessage.globalTitle("§6" + CivSettings.localize.localizedString("var_autoCapitulate_CivTitle", town.getMotherCiv().getName()), "§6" + CivSettings.localize.localizedString("var_autoCapitulate_CivSubTitle", newCiv));
					town.getMotherCiv().delete();
					continue;
				}
				String motherCiv = town.getMotherCiv().getName();
				town.setMotherCiv(null);
				town.setConqueredDate(0L);
				town.save();
				CivMessage.globalTitle("§6" + CivSettings.localize.localizedString("var_autoCapitulate_townTitle", town.getName()), "§6" + CivSettings.localize.localizedString("var_autoCapitulate_townSubTitle", motherCiv));
				continue;
			}
			CivLog.warning(town.getName() + "has no Mother civilization. AutoCapitulate interrupted");
		}
	}

}
