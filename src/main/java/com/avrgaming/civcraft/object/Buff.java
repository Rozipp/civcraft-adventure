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

import java.text.DecimalFormat;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Buff {
	
	/* Quick redefines for id/name from yml. */
	public static final String FINE_ART = "buff_fine_art";
	public static final String CONSTRUCTION = "buff_construction";
	public static final String GROWTH_RATE = "buff_year_of_plenty";
	public static final String TRADE = "buff_monopoly";
	public static final String REDUCE_CONSUME = "buff_preservative";	
	public static final String SCIENCE_RATE = "buff_innovation";
	public static final String EXTRA_CULTURE = "buff_doesnotexist";
	public static final String COTTAGE_RATE = "buff_doesnotexist";
	public static final String ADVANCED_TOOLING = "buff_advanced_tooling";
	public static final String BARRICADE = "buff_barricade";
	public static final String BARTER = "buff_barter";
	public static final String EXTRACTION = "buff_extraction";
	public static final String FIRE_BOMB = "buff_fire_bomb";
	public static final String FISHING = "buff_fishing";
	public static final String MEDICINE = "buff_medicine";
	public static final String RUSH = "buff_rush";
	public static final String DEBUFF_PYRAMID_LEECH = "debuff_pyramid_leech";
	
	private ConfigBuff config;
	private String source;
	private String key;
	
	public Buff(String buffkey, String buffId, String source) {
		config = CivSettings.buffs.get(buffId);
		setKey(buffkey);
		this.source = source;
	}
	
	@Override
	public int hashCode() {
		return config.id.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Buff) {
			Buff otherBuff = (Buff)other;
			if (otherBuff.getConfig().id.equals(this.getConfig().id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the source
	 */

	public boolean isStackable() {
		return config.stackable;
	}
	
	public String getId() {
		return config.id;
	}

	public String getValue() {
		return config.value;
	}
	
	public String getDisplayDouble() {
		try {
			double d = Double.valueOf(config.value);
			DecimalFormat df = new DecimalFormat();
			return df.format(d*100)+"%";
		} catch (NumberFormatException e) {
			return "NAN!";
		}
	}
	
	public String getDisplayInt() {
		try {
			int i = Integer.valueOf(config.value);
			return ""+i;
		} catch (NumberFormatException e) {
			return "NAN!";
		}
	}

	public String getDisplayName() {
		return config.name;
	}
}
