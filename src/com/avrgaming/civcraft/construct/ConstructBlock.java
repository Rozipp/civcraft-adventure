/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.construct;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.war.War;

@Getter
@Setter
public class ConstructBlock implements ConstructDamageBlock {

	private BlockCoord coord = null;
	private Construct owner = null;
	private boolean damageable = true;
	private boolean alwaysDamage = false;
	
	/* This is a block that can be damaged. */
	public ConstructBlock(BlockCoord coord, Construct owner) {
		this.coord = coord;
		this.owner = owner;
	}
	
	public Town getTown() {
		if (this.owner instanceof Buildable) return ((Buildable)this.owner).getTown();
		return null;
	}
	
	public Civilization getCiv() {
		if (this.owner instanceof Buildable) return ((Buildable)this.owner).getCiv();
		return null;
	}
	
	public int getX() {
		return this.coord.getX();
	}
	
	public int getY() {
		return this.coord.getY();
	}
	
	public int getZ() {
		return this.coord.getZ();
	}
	
	public String getWorldname() {
		return this.coord.getWorldname();
	}
	
	public boolean canDestroyOnlyDuringWar() {
		return true;
	}

	@Override
	public boolean allowDamageNow(Player player) {	
		// Dont bother making any checks if we're not at war
		if (War.isWarTime()) {
			// Structures with max hitpoints of 0 cannot be damaged.
			if (this.getOwner().getMaxHitPoints() != 0) {
				Resident res = CivGlobal.getResident(player.getName());
				if (res == null) {
					return false;
				}
				
				// Make sure the resident has a town
				if (res.hasTown()) {
					if (res.getTown().defeated) {
						CivMessage.sendError(player, CivSettings.localize.localizedString("structBlock_errorDefeated"));
						return false;
					}
					
					Civilization civ = res.getTown().getCiv();
					// Make sure we are at war with this civilization. 
					// Cant be at war with our own, will be false if our own structure.
					if (civ.getDiplomacyManager().atWarWith(this.getCiv())) {
						if (this.alwaysDamage) {
							return true;
						}
						
						if (!this.isDamageable()) {
							CivMessage.sendError(player, CivSettings.localize.localizedString("structBlock_error1"));
						} else if (CivGlobal.willInstantBreak(this.getCoord().getBlock().getType())) {
							CivMessage.sendError(player, CivSettings.localize.localizedString("structBlock_error2"));								
						} else {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
