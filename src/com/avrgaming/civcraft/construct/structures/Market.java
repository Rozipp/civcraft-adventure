package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMarketItem;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.LinkedList;

public class Market extends Structure {

	public HashMap<Integer, LinkedList<ConstructSign>> signIndex = new HashMap<>();
	
	public static int BULK_AMOUNT = 64;
		
	public Market(String id, Town town) {
		super(id, town);
	}

	@Override
	public void delete() {
		super.delete();
		CivGlobal.removeMarket(this);
	}
	
	public static void globalSignUpdate(int id) {
		for (Market market : CivGlobal.getMarkets()) {
			
			LinkedList<ConstructSign> signs = market.signIndex.get(id);
			if (signs == null) {
				continue;
			}
			
			for (ConstructSign sign : signs) {			
				ConfigMarketItem item = CivSettings.marketItems.get(id);
				if (item != null) {
					try {
					market.setSignText(sign, item);
					} catch (ClassCastException e) {
						CivLog.error("Can't cast structure sign to sign for market update. "+sign.getCoord().getX()+" "+sign.getCoord().getY()+" "+sign.getCoord().getZ());
					}
				}
			}
		}
	}
	
	public void processBuy(Player player, Resident resident, int bulkCount, ConfigMarketItem item) throws CivException {
		item.buy(resident, player, bulkCount);
	}
	
	public void processSell(Player player, Resident resident, int bulkCount, ConfigMarketItem item) throws CivException {
		item.sell(resident, player, bulkCount);
	}
	
	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
		
		int id = Integer.parseInt(sign.getType());
		ConfigMarketItem item = CivSettings.marketItems.get(id);
		Resident resident = CivGlobal.getResident(player);

		if (resident == null) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("market_invalidPlayer"));
			return;
		}
		
		if (item == null) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("market_invalidID")+id);
			return;
		}
		
		switch (sign.getAction().toLowerCase()) {
		case "sellbig":
			processSell(player, resident, BULK_AMOUNT, item);
			break;
		case "sell":
			processSell(player, resident, 1, item);
			break;
		case "buy":
			processBuy(player, resident, 1, item);
			break;
		case "buybig":
			processBuy(player, resident, BULK_AMOUNT, item);
			break;
		}
	
		player.updateInventory();
		Market.globalSignUpdate(id);
	}
	
	public void setSignText(ConstructSign sign, ConfigMarketItem item) throws ClassCastException {

		String itemColor;
		switch (item.lastaction) {
		case BUY:
			itemColor = CivColor.LightGreen;
			break;
		case SELL:
			itemColor = CivColor.Rose;
			break;
		default:
			itemColor = CivColor.Black;
			break;
		}
		
		try {
		Sign s;
		switch (sign.getAction().toLowerCase()) {
		case "sellbig":
			if (sign.getCoord().getBlock().getState() instanceof Sign) {
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+CivSettings.localize.localizedString("market_sign_sellBulk"));
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getSellCostForAmount(BULK_AMOUNT)+" "+CivSettings.CURRENCY_NAME);
			s.setLine(3, CivSettings.localize.localizedString("var_market_sign_amount",BULK_AMOUNT));
			s.update();
			}
			break;
		case "sell":
			if (sign.getCoord().getBlock().getState() instanceof Sign) {
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+CivSettings.localize.localizedString("market_sign_sell"));
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getSellCostForAmount(1)+" "+CivSettings.CURRENCY_NAME);
			s.setLine(3, CivSettings.localize.localizedString("var_market_sign_amount",1));
			s.update();
			}
			break;
		case "buy":
			if (sign.getCoord().getBlock().getState() instanceof Sign) {
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+CivSettings.localize.localizedString("market_sign_buy"));
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getBuyCostForAmount(1)+" "+CivSettings.CURRENCY_NAME);
			s.setLine(3, CivSettings.localize.localizedString("var_market_sign_amount",1));
			s.update();
			}
			break;
		case "buybig":
			if (sign.getCoord().getBlock().getState() instanceof Sign) {
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+CivSettings.localize.localizedString("market_sign_buyBulk"));
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getBuyCostForAmount(BULK_AMOUNT)+" "+CivSettings.CURRENCY_NAME);
			s.setLine(3, CivSettings.localize.localizedString("var_market_sign_amount",BULK_AMOUNT));
			s.update();
			}
			break;
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void buildSign(String action, Integer id, BlockCoord absCoord, ConfigMarketItem item, SimpleBlock commandBlock) {
		Block b = absCoord.getBlock();
		
		ItemManager.setTypeIdAndData(b, ItemManager.getMaterialId(Material.WALL_SIGN), (byte)commandBlock.getData(), false);
		
		ConstructSign structSign = CivGlobal.getConstructSign(absCoord);
		if (structSign == null) {
			structSign = new ConstructSign(absCoord, this);
		}
		
		structSign.setDirection(ItemManager.getData(b.getState()));
		structSign.setType(""+id);
		structSign.setAction(action);

		structSign.setOwner(this);
		this.addConstructSign(structSign);
		
		LinkedList<ConstructSign> signs = this.signIndex.get(id);
		if (signs == null) {
			signs = new LinkedList<>();
		}
	
		signs.add(structSign);
		this.signIndex.put(id, signs);
		this.setSignText(structSign, item);
	}
	
	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock commandBlock) {
		Integer id;
		ConfigMarketItem item;
		switch (commandBlock.command.toLowerCase().trim()) {
		case "/sellbig":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				if (item.isStackable()) {
					buildSign("sellbig", id, absCoord, item, commandBlock);
				}
			}
			break;
		case "/sell":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				buildSign("sell", id, absCoord, item, commandBlock);
			}		
			break;
		case "/buy":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				buildSign("buy", id, absCoord, item, commandBlock);
			}		
			break;
		case "/buybig":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				if (item.isStackable()) {
					buildSign("buybig", id, absCoord, item, commandBlock);
				}
			}		
			break;
		}
	}
	
	@Override
	public void onPostBuild() {
		CivGlobal.addMarket(this);
	}

}
