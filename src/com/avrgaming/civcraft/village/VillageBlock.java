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
package com.avrgaming.civcraft.village;

import com.avrgaming.civcraft.util.BlockCoord;

public class VillageBlock {
	//XXX TODO merge this with structure block?
	private BlockCoord coord;
	private Village village;
	private boolean friendlyBreakable = false;
	
	public VillageBlock(BlockCoord coord, Village village) {
		this.coord = coord;
		this.village = village;
	}
	
	public VillageBlock(BlockCoord coord, Village village, boolean friendlyBreakable) {
		this.coord = coord;
		this.village = village;
		this.friendlyBreakable = friendlyBreakable;
	}

	public BlockCoord getCoord() {
		return coord;
	}
	public void setCoord(BlockCoord coord) {
		this.coord = coord;
	}
	public Village getVillage() {
		return village;
	}
	public void setVillage(Village village) {
		this.village = village;
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
	
	public boolean canBreak(String playerName) {
		if (this.friendlyBreakable == false) {
			return false;
		}
		
		if (village.hasMember(playerName)) {
			return true;
		}
		
		return false;
	}
	
}
