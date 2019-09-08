package com.avrgaming.civcraft.units;

import com.avrgaming.civcraft.units.EquipmentElement.Equipments;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class Warrior extends UnitMaterial {
	
	public Warrior(String id, ConfigUnit configUnit) {
		super(id, configUnit);
	}

	@Override
	public void initUnitObject(UnitObject uo) {
		uo.setComponent("sword", 0);
	}

	@Override
	public void initLore(AttributeUtil attrs, UnitObject uo) {
		Integer temp;
		temp = uo.getComponent(Equipments.HELMET);
		String h = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.CHESTPLATE);
		String c = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.LEGGINGS);
		String l = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.BOOTS);
		String b = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.SWORD);
		String s = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.TWOHAND);
		String t = CivColor.GreenBold + ((temp != 0) ? "T" + temp : "--");
		
		attrs.addLore(CivColor.RoseBold + "           Амуниция:");
		attrs.addLore(CivColor.Rose     + " правая   Голова  : " + h + CivColor.Rose + "    левая");
		attrs.addLore(CivColor.Rose     + "  рука    Грудь   : " + c + CivColor.Rose + "    рука ");
		attrs.addLore(CivColor.Rose     + "   " + s + CivColor.Rose + "     Ноги    : " + l + CivColor.Rose + "     " + t);
		attrs.addLore(CivColor.Rose     + "          Ступни  : " + b);
	}
	
	@Override
	public void initAmmunitions() {
		EquipmentElement e = null;
		//helmet
		e = new EquipmentElement(39);
		e.addMatTir("");
		e.addMatTir("mat_iron_helmet");
		e.addMatTir("mat_steel_helmet");
		e.addMatTir("mat_carbide_steel_helmet");
		e.addMatTir("mat_tungsten_helmet");
		equipmentElemens.put("helmet", e);

		//chestplate
		e = new EquipmentElement(38);
		e.addMatTir("");
		e.addMatTir("mat_iron_chestplate");
		e.addMatTir("mat_steel_chestplate");
		e.addMatTir("mat_carbide_steel_chestplate");
		e.addMatTir("mat_tungsten_chestplate");
		equipmentElemens.put("chestplate", e);

		//leggings
		e = new EquipmentElement(37);
		e.addMatTir("");
		e.addMatTir("mat_iron_leggings");
		e.addMatTir("mat_steel_leggings");
		e.addMatTir("mat_carbide_steel_leggings");
		e.addMatTir("mat_tungsten_leggings");
		equipmentElemens.put("leggings", e);

		//boots
		e = new EquipmentElement(36);
		e.addMatTir("");
		e.addMatTir("mat_iron_boots");
		e.addMatTir("mat_steel_boots");
		e.addMatTir("mat_carbide_steel_boots");
		e.addMatTir("mat_tungsten_boots");
		equipmentElemens.put("boots", e);

		//sword
		e = new EquipmentElement(0);
		e.addMatTir("");
		e.addMatTir("mat_iron_sword");
		e.addMatTir("mat_steel_sword");
		e.addMatTir("mat_carbide_steel_sword");
		e.addMatTir("mat_tungsten_sword");
		equipmentElemens.put("sword", e);

		//two
		e = new EquipmentElement(40);
		e.addMatTir("");
		e.addMatTir("");
		e.addMatTir("");
		e.addMatTir("");
		e.addMatTir("");
		equipmentElemens.put("two", e);
	}
}
