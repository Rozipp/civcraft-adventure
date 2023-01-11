package com.avrgaming.civcraft.components;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.TradeLevelComponent.Result;

@Getter
@Setter
public class TradeShipResults {

	private Result result;
	private double money;
	private int culture;
	private int consumed;

	private List<ItemStack> returnCargo = new LinkedList<ItemStack>();

	public TradeShipResults() {
		this.money = 0;
		this.culture = 0;
		this.consumed = 0;
		this.result = Result.UNKNOWN;
	}

	public void addReturnCargo(ItemStack cargo) {
		this.returnCargo.add(cargo);
	}
}