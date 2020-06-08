
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.AttributeBiomeRadiusPerLevel;
import com.avrgaming.civcraft.components.TradeLevelComponent;
import com.avrgaming.civcraft.components.TradeLevelComponent.Result;
import com.avrgaming.civcraft.components.TradeShipResults;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;

public class TradeShip extends WaterStructure {

	private int upgradeLevel = 1;
	private int lastConsume;
	private int tickLevel = 1;

	public HashSet<BlockCoord> goodsDepositPoints = new HashSet<BlockCoord>();
	public HashSet<BlockCoord> goodsWithdrawPoints = new HashSet<BlockCoord>();

	private TradeLevelComponent consumeComp = null;

	public TradeShip(String id, Town town) throws CivException {
		super(id, town);
		setUpgradeLvl(town.saved_tradeship_upgrade_levels);
		this.lastConsume = 128;
	}

	public TradeShip(ResultSet rs) throws SQLException, CivException {
		super(rs);
		this.lastConsume = 128;
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

	public TradeLevelComponent getTradeLevelComponent() {
		if (consumeComp == null) {
			consumeComp = (TradeLevelComponent) this.getComponent(TradeLevelComponent.class.getSimpleName());
		}
		return consumeComp;
	}

	@Override
	public void updateSignText() {
		reprocessCommandSigns();
	}

	public void reprocessCommandSigns() {
		/* Load in the template. */
		Template tpl = this.getTemplate();
		if (tpl == null) return;

		BlockCoord structCorner = this.getCorner();
		class SyncTask implements Runnable {
			@Override
			public void run() {
				processCommandSigns(tpl, structCorner);
			}
		}
		TaskMaster.syncTask(new SyncTask(), TimeTools.toTicks(1));
	}

	private void processCommandSigns(Template tpl, BlockCoord corner) {
		for (SimpleBlock sb : tpl.commandBlockRelativeLocations) {
			BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(sb.getX(), sb.getY(), sb.getZ()));

			switch (sb.command) {
			case "/incoming": {
				Integer ID = Integer.valueOf(sb.keyvalues.get("id"));
				if (this.getUpgradeLvl() >= ID + 1) {
					this.goodsWithdrawPoints.add(absCoord);
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.CHEST));
					byte data3 = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setData(absCoord.getBlock(), data3);
				} else {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.AIR));
					ItemManager.setData(absCoord.getBlock(), sb.getData());
				}
				this.addConstructBlock(absCoord, false);
				break;
			}
			case "/inSign": {
				Integer ID = Integer.valueOf(sb.keyvalues.get("id"));
				if (this.getUpgradeLvl() >= ID + 1) {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
					ItemManager.setData(absCoord.getBlock(), sb.getData());

					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
					sign.setLine(1, "" + (ID + 1));
					sign.setLine(2, "");
					sign.setLine(3, "");
					sign.update();
				} else {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
					ItemManager.setData(absCoord.getBlock(), sb.getData());

					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
					sign.setLine(1, CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line1"));
					sign.setLine(2, (CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line2")));
					sign.setLine(3, CivSettings.localize.localizedString("tradeship_sign_input_notupgraded_line3"));
					sign.update();
				}
				this.addConstructBlock(absCoord, false);
				break;
			}
			case "/outgoing": {
				Integer ID = Integer.valueOf(sb.keyvalues.get("id"));

				if (this.getLevel() >= (ID * 2) + 1) {
					this.goodsDepositPoints.add(absCoord);
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.CHEST));
					byte data3 = CivData.convertSignDataToChestData((byte) sb.getData());
					ItemManager.setData(absCoord.getBlock(), data3);
					this.addConstructBlock(absCoord, false);
				} else {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.AIR));
					ItemManager.setData(absCoord.getBlock(), sb.getData());
				}
				break;
			}
			case "/outSign": {
				Integer ID = Integer.valueOf(sb.keyvalues.get("id"));
				if (this.getLevel() >= (ID * 2) + 1) {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
					ItemManager.setData(absCoord.getBlock(), sb.getData());

					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_output_line0"));
					sign.setLine(1, "" + (ID + 1));
					sign.setLine(2, "");
					sign.setLine(3, "");
					sign.update();
				} else {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
					ItemManager.setData(absCoord.getBlock(), sb.getData());

					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_output_line0"));
					sign.setLine(1, CivSettings.localize.localizedString("tradeship_sign_output_notupgraded_line1"));
					sign.setLine(2, (CivSettings.localize.localizedString("var_tradeship_sign_output_notupgraded_line2", ((ID * 2) + 1))));
					sign.setLine(3, CivSettings.localize.localizedString("tradeship_sign_output_notupgraded_line3"));
					sign.update();
				}
				this.addConstructBlock(absCoord, false);
				break;
			}
			case "/in": {
				Integer ID = Integer.valueOf(sb.keyvalues.get("id"));
				if (ID == 0) {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
					ItemManager.setData(absCoord.getBlock(), sb.getData());

					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
					sign.setLine(1, "1");
					sign.setLine(2, "2");
					sign.setLine(3, "");
					sign.update();
				} else {
					ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
					ItemManager.setData(absCoord.getBlock(), sb.getData());

					Sign sign = (Sign) absCoord.getBlock().getState();
					sign.setLine(0, CivSettings.localize.localizedString("tradeship_sign_input_line0"));
					sign.setLine(1, "3");
					sign.setLine(2, "4");
					sign.setLine(3, "");
					sign.update();
				}
				this.addConstructBlock(absCoord, false);
				break;
			}
			default: {
				/* Unrecognized command... treat as a literal sign. */
				ItemManager.setTypeId(absCoord.getBlock(), ItemManager.getMaterialId(Material.WALL_SIGN));
				ItemManager.setData(absCoord.getBlock(), sb.getData());

				Sign sign = (Sign) absCoord.getBlock().getState();
				sign.setLine(0, sb.message[0]);
				sign.setLine(1, sb.message[1]);
				sign.setLine(2, sb.message[2]);
				sign.setLine(3, sb.message[3]);
				sign.update();

				this.addConstructBlock(absCoord, false);
				break;
			}
			}
		}
	}

	public TradeShipResults consume(CivAsyncTask task) throws InterruptedException {
		TradeShipResults tradeResult;
		// Look for the TradeShip chests.
		if (this.goodsDepositPoints.size() == 0 || this.goodsWithdrawPoints.size() == 0) {
			tradeResult = new TradeShipResults();
			tradeResult.setResult(Result.STAGNATE);
			return tradeResult;
		}
		MultiInventory mInv = new MultiInventory();

		for (BlockCoord bcoord : this.goodsDepositPoints) {
			task.syncLoadChunk(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
			Inventory tmp;
			try {
				tmp = task.getChestInventory(bcoord.getWorldname(), bcoord.getX(), bcoord.getY(), bcoord.getZ(), true);
			} catch (CivTaskAbortException e) {
				tradeResult = new TradeShipResults();
				tradeResult.setResult(Result.STAGNATE);
				return tradeResult;
			}
			mInv.addInventory(tmp);
		}

		if (mInv.getInventoryCount() == 0) {
			tradeResult = new TradeShipResults();
			tradeResult.setResult(Result.STAGNATE);
			return tradeResult;
		}
		getTradeLevelComponent().setSource(mInv);
		getTradeLevelComponent().setConsumeRate(1.0);

		try {
			tradeResult = getTradeLevelComponent().processConsumption(this.getUpgradeLvl() - 1);
			getTradeLevelComponent().onSave();
		} catch (IllegalStateException e) {
			tradeResult = new TradeShipResults();
			tradeResult.setResult(Result.STAGNATE);
			CivLog.exception(this.getDisplayName() + " Process Error in town: " + this.getTown().getName() + " and Location: " + this.getCorner(), e);
			return tradeResult;
		}
		return tradeResult;
	}

	@Override
	public void onHourlyUpdate(CivAsyncTask task) {
		TradeShipResults tradeResult;
		try {
			tradeResult = this.consume(task);

			Result result = tradeResult.getResult();
			switch (result) {
			case STAGNATE:
				CivMessage.sendTown(getTown(), CivColor.Rose + CivSettings.localize.localizedString("var_tradeship_stagnated", getTradeLevelComponent().getLevel(), CivColor.LightGreen + getTradeLevelComponent().getCountString()));
				break;
			case GROW:
				CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_productionGrew", getTradeLevelComponent().getLevel(), getTradeLevelComponent().getCountString()));
				break;
			case LEVELUP:
				CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_lvlUp", getTradeLevelComponent().getLevel()));
				this.reprocessCommandSigns();
				break;
			case MAXED:
				CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_maxed", getTradeLevelComponent().getLevel(), CivColor.LightGreen + getTradeLevelComponent().getCountString()));
				break;
			default:
				break;
			}
			if (tradeResult.getCulture() >= 1) {
				int total_culture = (int) Math.round(tradeResult.getCulture());

				this.getTown().addAccumulatedCulture(total_culture);
				this.getTown().save();
			}
			if (tradeResult.getMoney() >= 1) {
				double total_coins = tradeResult.getMoney();
				if (this.getTown().getBuffManager().hasBuff("buff_ingermanland_trade_ship_income")) {
					total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_ingermanland_trade_ship_income");
				}

				if (this.getTown().getBuffManager().hasBuff("buff_great_lighthouse_trade_ship_income")) {
					total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_great_lighthouse_trade_ship_income");
				}
				if (this.getTown().getBuildableTypeCount("s_lighthouse") >= 1) {
					try {
						total_coins *= CivSettings.getDouble(CivSettings.townConfig, "town.lighthouse_trade_ship_boost");
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
					}
				}

				double taxesPaid = total_coins * this.getTown().getDepositCiv().getIncomeTaxRate();

				if (total_coins >= 1) {
					CivMessage.sendTown(getTown(),
							CivColor.LightGreen + CivSettings.localize.localizedString("var_tradeship_success", Math.round(total_coins), CivSettings.CURRENCY_NAME, tradeResult.getCulture(), "Культуры", tradeResult.getConsumed()));
					this.lastConsume = tradeResult.getConsumed();
				}
				if (taxesPaid > 0) {
					CivMessage.sendTown(this.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_tradeship_taxesPaid", Math.round(taxesPaid), CivSettings.CURRENCY_NAME));
				}

				this.getTown().getTreasury().deposit(total_coins - taxesPaid);
				this.getTown().getDepositCiv().taxPayment(this.getTown(), taxesPaid);
			}

			if (tradeResult.getReturnCargo().size() >= 1) {
				MultiInventory multiInv = new MultiInventory();

				for (BlockCoord bcoord : this.goodsWithdrawPoints) {
					task.syncLoadChunk(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
					Inventory tmp;
					try {
						tmp = task.getChestInventory(bcoord.getWorldname(), bcoord.getX(), bcoord.getY(), bcoord.getZ(), true);
						multiInv.addInventory(tmp);
					} catch (CivTaskAbortException e) {

						e.printStackTrace();
					}
				}

				for (ItemStack item : tradeResult.getReturnCargo()) {
					multiInv.addItemStack(item);
				}
				CivMessage.sendTown(getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("tradeship_successSpecail"));
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
		this.upgradeLevel = getTown().saved_tradeship_upgrade_levels;
		this.reprocessCommandSigns();
	}

	public int getUpgradeLvl() {
		return upgradeLevel;
	}

	public void setUpgradeLvl(int level) {
		this.upgradeLevel = level;

		if (this.isComplete()) {
			this.reprocessCommandSigns();
		}
	}

	public int getLevel() {
		try {
			return this.getTradeLevelComponent().getLevel();
		} catch (Exception e) {
			return tickLevel;
		}
	}

	public double getHammersPerTile() {
		AttributeBiomeRadiusPerLevel attrBiome = (AttributeBiomeRadiusPerLevel) this.getComponent("AttributeBiomeBase");
		double base = attrBiome.getBaseValue();

		double rate = 1;
		rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.ADVANCED_TOOLING);
		return (rate * base);
	}

	public Result getLastResult() {
		return this.getTradeLevelComponent().getLastResult();
	}

	@Override
	public void delete() {
		super.delete();
		if (getTradeLevelComponent() != null) getTradeLevelComponent().onDelete();
	}

	public void onDestroy() {
		super.onDestroy();

		getTradeLevelComponent().setLevel(1);
		getTradeLevelComponent().setCount(0);
		getTradeLevelComponent().onSave();
	}

	public double getBonusRate() {
		double rate = 1.0;
		if (this.getTown().getBuffManager().hasBuff("buff_ingermanland_trade_ship_income")) {
			rate *= 1.3;
		}
		if (this.getTown().getBuffManager().hasBuff("buff_great_lighthouse_trade_ship_income")) {
			rate *= 1.2;
		}
		if (this.getTown().getBuildableTypeCount("s_lighthouse") >= 1) {
			rate *= 1.2;
		}
		if (this.getTown().getCiv().getGovernment().id.equalsIgnoreCase("gov_theocracy")) {
			rate *= 2.0;
		}
		return rate;
	}

	public int getLastConsume() {
		return this.lastConsume;
	}
}
