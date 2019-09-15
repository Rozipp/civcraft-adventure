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
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ControlPoint {

	/* Location of the control block. */
	private BlockCoord coord;
	
	/* Hitpoints for this control block. */
	private int hitpoints;
	
	/* Max hitpoints for this control block. */
	private int maxHitpoints;
	
	/* TownHall this control point belongs to. */
	private Buildable buildable;
    private String info;

	public ControlPoint (BlockCoord coord, Buildable buildable, int hitpoints, String info) {
		this.coord = coord;
		this.setBuildable(buildable);
		this.maxHitpoints = hitpoints;
		this.hitpoints = this.maxHitpoints;
		this.info = info;
	}

	public void damage(int amount) {
		if (this.hitpoints <= 0) {
			return;
		}
		
		this.hitpoints -= amount;
		
		if (this.hitpoints <= 0) {
			this.hitpoints = 0;
		}
		
	}
	
	public boolean isDestroyed() {
		if (this.hitpoints <= 0) {
			return true;
		}
		return false;
	}
}
