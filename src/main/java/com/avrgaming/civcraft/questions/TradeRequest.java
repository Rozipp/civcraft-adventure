package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.trade.TradeInventoryPair;
import com.avrgaming.civcraft.util.CivColor;

public class TradeRequest implements QuestionResponseInterface {

	public Resident resident;
	public Resident trader;

	@Override
	public void processResponse(String param) {
		if (param.equalsIgnoreCase("accept")) {
			TradeInventoryPair pair = new TradeInventoryPair();
			try {
				pair.player = CivGlobal.getPlayer(trader);
				pair.otherPlayer = CivGlobal.getPlayer(resident);
			} catch (CivException e) {
				CivMessage.sendError(resident, e.getMessage());
				CivMessage.sendError(trader, e.getMessage());
				return;
			}
			pair.inv = trader.startTradeWith(resident);
			if (pair.inv == null) return;

			pair.otherInv = resident.startTradeWith(trader);
			if (pair.otherInv == null) return;

			TradeInventoryListener.tradeInventories.put(TradeInventoryListener.getTradeInventoryKey(pair.player), pair);

			TradeInventoryPair otherPair = new TradeInventoryPair();
			otherPair.inv = pair.otherInv;
			otherPair.otherInv = pair.inv;
			otherPair.player = pair.otherPlayer;
			otherPair.otherPlayer = pair.player;
			TradeInventoryListener.tradeInventories.put(TradeInventoryListener.getTradeInventoryKey(pair.otherPlayer), otherPair);
		} else {
			CivMessage.send(trader, CivColor.LightGray + CivSettings.localize.localizedString("var_trade_declined", resident.getName()));
		}
	}

	@Override
	public void processResponse(String response, Resident responder) {
		processResponse(response);
	}
}
