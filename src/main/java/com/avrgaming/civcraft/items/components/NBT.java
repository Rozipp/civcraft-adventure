package com.avrgaming.civcraft.items.components;

import com.avrgaming.gpl.AttributeUtil;

public class NBT extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		String value;
		if ((value = this.getString("color")) != null)  
			attrs.setColor(Long.decode("0x"+value));
		
		if ((value = this.getString("potion")) != null)
			attrs.setNBT("Potion", "minecraft:" +value);
	}

	/*
	"minecraft:water"
	"minecraft:thick"
	"minecraft:mundane"
	"minecraft:awkward"

	"minecraft:night_vision"
	"minecraft:long_night_vision"

	"minecraft:invisibility"
	"minecraft:long_invisibility"

	"minecraft:leaping"
	"minecraft:long_leaping"
	"minecraft:strong_leaping"

	"minecraft:fire_resistance"
	"minecraft:long_fire_resistance"

	"minecraft:swiftness"
	"minecraft:long_swiftness"
	"minecraft:strong_swiftness"

	"minecraft:slowness"
	"minecraft:long_slowness"
	"minecraft:strong_slowness"

	"minecraft:turtle_master"
	"minecraft:long_turtle_master"
	"minecraft:strong_turtle_master"

	"minecraft:water_breathing"
	"minecraft:long_water_breathing"

	"minecraft:healing"
	"minecraft:strong_healing"

	"minecraft:harming"
	"minecraft:strong_harming"

	"minecraft:poison"
	"minecraft:long_poison"
	"minecraft:strong_poison"

	"minecraft:regeneration"
	"minecraft:long_regeneration"
	"minecraft:strong_regeneration"

	"minecraft:strength"
	"minecraft:long_strength"
	"minecraft:strong_strength"

	"minecraft:weakness"
	 "minecraft:long_weakness"

	"minecraft:slow_falling"
	"minecraft:long_slow_falling"
*/
	
}
