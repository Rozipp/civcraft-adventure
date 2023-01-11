package com.avrgaming.civcraft.trade;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.gui.GuiInventory;

public class TradeInventoryPair {
	public GuiInventory inv;
	public GuiInventory otherInv;
	public Player player;
	public Player otherPlayer;
	public double coins;
	public double otherCoins;
	public boolean valid;
}
