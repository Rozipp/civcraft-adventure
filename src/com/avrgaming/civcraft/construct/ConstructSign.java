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
import org.bukkit.block.Sign;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.BlockCoord;

@Getter
@Setter
public class ConstructSign {

	private String text;
	private Construct owner;
	private String type;
	private String action;
	private BlockCoord coord;
	private int direction;
	private boolean allowRightClick = false;

	public ConstructSign(BlockCoord coord, Construct owner) {
		this.coord = coord;
		this.owner = owner;
	}

	public void delete() {
		CivGlobal.removeConstructSign(this);
	}

	public void setText(String string) {
		this.text = string;
	}

	public void setText(String[] message) {
		this.text = "";
		for (String str : message) {
			text += str + "\n";
		}
	}

	public void update() {
		if (coord.getBlock().getState() instanceof Sign) {
			Sign sign = (Sign) coord.getBlock().getState();
			String[] lines = this.text.split("\\n");

			for (int i = 0; i < 4; i++) {
				if (i < lines.length) {
					sign.setLine(i, lines[i]);
				} else {
					sign.setLine(i, "");
				}
			}
			sign.update();
		}
	}
}