package com.avrgaming.civcraft.units;

import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class Archer extends UnitMaterial {

	public Archer(String id, ConfigUnit configUnit) {
		super(id, configUnit);
	}

	@Override
	public void initUnitObject(UnitObject uo) {
		uo.setEquipment(Equipments.MAINHAND, "mat_stone_sword");
	}

	@Override
	public void initLore(AttributeUtil attrs, UnitObject uo) {
		String h = getTirToString(uo.getEquipment(Equipments.HELMET));
		String c = getTirToString(uo.getEquipment(Equipments.CHESTPLATE));
		String l = getTirToString(uo.getEquipment(Equipments.LEGGINGS));
		String b = getTirToString(uo.getEquipment(Equipments.BOOTS));
		String m = getTirToString(uo.getEquipment(Equipments.MAINHAND));
		String t = getTirToString(uo.getEquipment(Equipments.TWOHAND));
		
		attrs.addLore(CivColor.RoseBold + "           Амуниция:");
		attrs.addLore(CivColor.Rose     + "          Голова  : " + h);
		attrs.addLore(CivColor.Rose     + "  мечь    Грудь   : " + c + CivColor.Rose + "    лук");
		attrs.addLore(CivColor.Rose     + "   " + m + CivColor.Rose + "     Ноги    : " + l + CivColor.Rose + "     " + t);
		attrs.addLore(CivColor.Rose     + "          Ступни  : " + b);
	}


}
