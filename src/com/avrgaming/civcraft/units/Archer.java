package com.avrgaming.civcraft.units;

import com.avrgaming.civcraft.units.EquipmentElement.Equipments;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class Archer extends UnitMaterial {

	public Archer(String id, ConfigUnit configUnit) {
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
		String h = CivColor.Green + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.CHESTPLATE);
		String c = CivColor.Green + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.LEGGINGS);
		String l = CivColor.Green + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.BOOTS);
		String b = CivColor.Green + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.SWORD);
		String s = CivColor.Green + ((temp != 0) ? "T" + temp : "--");
		temp = uo.getComponent(Equipments.TWOHAND);
		String t = CivColor.Green + ((temp != 0) ? "T" + temp : "--");
		
		attrs.addLore(CivColor.RoseBold + "           Амуниция:");
		attrs.addLore(CivColor.Rose     + "          Голова  : " + h);
		attrs.addLore(CivColor.Rose     + "  мечь    Грудь   : " + c + CivColor.Rose + "    лук");
		attrs.addLore(CivColor.Rose     + "   " + s + CivColor.Rose + "     Ноги    : " + l + CivColor.Rose + "     " + t);
		attrs.addLore(CivColor.Rose     + "          Ступни  : " + b);
	}

	@Override
	public void initAmmunitions() {
		EquipmentElement e = null;
		//helmet
		e = new EquipmentElement(39);
		e.addMatTir("");
		e.addMatTir("mat_leather_helmet");
		e.addMatTir("mat_refined_leather_helmet");
		e.addMatTir("mat_hardened_leather_helmet");
		e.addMatTir("mat_composite_leather_helmet");
		equipmentElemens.put(Equipments.HELMET, e);

		//chestplate
		e = new EquipmentElement(38);
		e.addMatTir("");
		e.addMatTir("mat_leather_chestplate");
		e.addMatTir("mat_refined_leather_chestplate");
		e.addMatTir("mat_hardened_leather_chestplate");
		e.addMatTir("mat_composite_leather_chestplate");
		equipmentElemens.put(Equipments.CHESTPLATE, e);

		//leggings
		e = new EquipmentElement(37);
		e.addMatTir("");
		e.addMatTir("mat_leather_leggings");
		e.addMatTir("mat_refined_leather_leggings");
		e.addMatTir("mat_hardened_leather_leggings");
		e.addMatTir("mat_composite_leather_leggings");
		equipmentElemens.put(Equipments.LEGGINGS, e);

		//boots
		e = new EquipmentElement(36);
		e.addMatTir("");
		e.addMatTir("mat_leather_boots");
		e.addMatTir("mat_refined_leather_boots");
		e.addMatTir("mat_hardened_leather_boots");
		e.addMatTir("mat_composite_leather_boots");
		equipmentElemens.put(Equipments.BOOTS, e);

		//sword
		e = new EquipmentElement(0);
		e.addMatTir("mat_stone_sword");
		e.addMatTir("mat_iron_sword");
		e.addMatTir("mat_steel_sword");
		e.addMatTir("mat_carbide_steel_sword");
		e.addMatTir("mat_tungsten_sword");
		equipmentElemens.put(Equipments.SWORD, e);

		//two
		e = new EquipmentElement(40);
		e.addMatTir("");
		e.addMatTir("mat_hunting_bow");
		e.addMatTir("mat_recurve_bow");
		e.addMatTir("mat_longbow");
		e.addMatTir("mat_marksmen_bow");
		equipmentElemens.put(Equipments.TWOHAND, e);
	}

}
