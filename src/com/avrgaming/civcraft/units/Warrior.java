package com.avrgaming.civcraft.units;

import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class Warrior extends UnitMaterial {
	
	public Warrior(String id, ConfigUnit configUnit) {
		super(id, configUnit);
	}

	@Override
	public void initUnitObject(UnitObject uo) {
		uo.setEquipment(Equipments.MAINHAND, "mat_stone_sword");
	}

	@Override
	public void initLore(AttributeUtil attrs, UnitObject uo) {
//		Integer temp;
//		temp = uo.getComponentValue(Equipments.HELMET);
//		String h = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
//		temp = uo.getComponentValue(Equipments.CHESTPLATE);
//		String c = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
//		temp = uo.getComponentValue(Equipments.LEGGINGS);
//		String l = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
//		temp = uo.getComponentValue(Equipments.BOOTS);
//		String b = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
//		temp = uo.getComponentValue(Equipments.SWORD);
//		String s = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
//		temp = uo.getComponentValue(Equipments.TWOHAND);
//		String t = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
//		
//		attrs.addLore(CivColor.RoseBold + "           Амуниция:");
//		attrs.addLore(CivColor.Rose     + " правая   Голова  : " + h + CivColor.Rose + "    левая");
//		attrs.addLore(CivColor.Rose     + "  рука    Грудь   : " + c + CivColor.Rose + "    рука ");
//		attrs.addLore(CivColor.Rose     + "   " + s + CivColor.Rose + "     Ноги    : " + l + CivColor.Rose + "     " + t);
//		attrs.addLore(CivColor.Rose     + "          Ступни  : " + b);
	}
}
