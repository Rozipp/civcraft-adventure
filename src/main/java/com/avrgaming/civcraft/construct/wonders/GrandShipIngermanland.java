package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;
import com.avrgaming.civcraft.components.ProjectileCannonComponent;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;

public class GrandShipIngermanland extends Wonder {

	ProjectileArrowComponent arrowComponent;
	ProjectileCannonComponent cannonComponent;

	public GrandShipIngermanland(String id, Town town) {
		super(id, town);
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCivOwner(), "buff_ingermanland_fishing_boat_immunity");
		addBuffToCiv(this.getCivOwner(), "buff_ingermanland_trade_ship_income");
		addBuffToCiv(this.getCivOwner(), "buff_ingermanland_water_range");
	}

	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCivOwner(), "buff_ingermanland_fishing_boat_immunity");
		removeBuffFromCiv(this.getCivOwner(), "buff_ingermanland_trade_ship_income");
		removeBuffFromCiv(this.getCivOwner(), "buff_ingermanland_water_range");
	}
	
	@Override
	public void loadSettings() {
		super.loadSettings();
		arrowComponent = new ProjectileArrowComponent(this);
		arrowComponent.createComponent(this);

		cannonComponent = new ProjectileCannonComponent(this); 
		cannonComponent.createComponent(this);
	}
	
	public int getArrowDamage() {
		double rate = 1;
		rate += this.getTownOwner().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
		return (int) (arrowComponent.getDamage() * rate);
	}

	public void setArrowDamage(int damage) {
		arrowComponent.setDamage(damage);
	}
	
	public double getArrowPower() {
		return arrowComponent.getPower();
	}

	public void setArrorPower(double power) {
		arrowComponent.setPower(power);
	}

	public void setArrowLocation(BlockCoord absCoord) {
		arrowComponent.setTurretLocation(absCoord);
	}	
	
	public int getCannonDamage() {
		double rate = 1;
		rate += this.getTownOwner().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
		return (int) (cannonComponent.getDamage() * rate);
	}
	
	public void setCannonLocation(BlockCoord absCoord) {
		cannonComponent.setTurretLocation(absCoord);
	}
	
}
