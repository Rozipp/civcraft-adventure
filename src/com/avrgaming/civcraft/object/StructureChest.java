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
public class StructureChest {

	private BlockCoord coord;
	private Buildable owner;
	private int direction;
	
	/* The chest id defines which chests are 'paired' for double chests. */
	private String chestId;
	
	public StructureChest(BlockCoord coord, Buildable owner) {
		this.setCoord(coord);
		this.setOwner(owner);
	}
}
