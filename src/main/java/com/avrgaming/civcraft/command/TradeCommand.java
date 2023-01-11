package com.avrgaming.civcraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.Question;
import com.avrgaming.civcraft.questions.TradeRequest;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.util.CivColor;

public class TradeCommand extends CustomCommand {
	public static int TRADE_TIMEOUT = 30000;

	public TradeCommand(String string_cmd) {
		super(string_cmd);
		withUsage(CivColor.LightPurple + string_cmd + " " + CivColor.Yellow + CivSettings.localize.localizedString("cmd_trade_resName") + " " + CivColor.LightGray + CivSettings.localize.localizedString("cmd_trade_cmdDesc"));
		withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident trader = Commander.getResident(sender);
				Resident resident = Commander.getNamedResident(args, 0);
				double max_trade_distance;
				try {
					max_trade_distance = CivSettings.getDouble(CivSettings.civConfig, "global.max_trade_distance");
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					throw new CivException(e.getMessage());
				}
				Player traderPlayer = CivGlobal.getPlayer(trader);
				Player residentPlayer = CivGlobal.getPlayer(resident);
				if (trader == resident) throw new CivException(CivSettings.localize.localizedString("cmd_trade_YourselfError"));
				if (traderPlayer.getLocation().distance(residentPlayer.getLocation()) > max_trade_distance) throw new CivException(CivSettings.localize.localizedString("var_cmd_trade_tooFarError", resident.getName()));
				if (TradeInventoryListener.tradeInventories.containsKey(TradeInventoryListener.getTradeInventoryKey(traderPlayer)))
					throw new CivException(CivSettings.localize.localizedString("var_cmd_trade_alreadyTradingError", resident.getName()));
				TradeRequest tradeRequest = new TradeRequest();
				tradeRequest.resident = resident;
				tradeRequest.trader = trader;
				Question.questionPlayer(traderPlayer, residentPlayer, CivSettings.localize.localizedString("cmd_trade_popTheQuestion") + " " + traderPlayer.getName() + "?", TRADE_TIMEOUT, tradeRequest);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_trade_requestSent"));
			}
		});
	}
}
