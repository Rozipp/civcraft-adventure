/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.object;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.util.BlockCoord;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ControlPoint implements ConstructDamageBlock{

	/* Location of the control block. */
	private BlockCoord coord;

	/* Hitpoints for this control block. */
	private int hitpoints;

	/* Max hitpoints for this control block. */
	private int maxHitpoints;

	/* TownHall this control point belongs to. */
	private Construct construct;
	private String info;

	public ControlPoint(BlockCoord coord, Construct construct, int hitpoints, String info) {
		this.coord = coord;
		this.setConstruct(construct);
		this.maxHitpoints = hitpoints;
		this.hitpoints = this.maxHitpoints;
		this.info = info;
	}

	public void damage(int amount) {
		if (this.hitpoints <= 0) return;
		this.hitpoints -= amount;
		if (this.hitpoints <= 0) this.hitpoints = 0;
	}

	public boolean isDestroyed() {
		return this.hitpoints <= 0;
	}

	@Override
	public Construct getOwner() {
		return construct;
	}

	@Override
	public void setOwner(Construct owner) {
		this.construct = owner;
	}

	@Override
	public Town getTown() {
		return construct.getTownOwner();
	}

	@Override
	public Civilization getCiv() {
		return construct.getCivOwner();
	}

	@Override
	public boolean isDamageable() {
		return true;
	}

	@Override
	public void setDamageable(boolean damageable) {
	}

	@Override
	public boolean canDestroyOnlyDuringWar() {
		return false;
	}

	@Override
	public boolean allowDamageNow(Player player) {
		return true;
	}

	@Override
	public World getWorld() {
		return coord.getWorld();
	}
}
