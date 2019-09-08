package com.avrgaming.civcraft.units;

import java.util.ArrayList;

public class EquipmentElement {

	public static String[] allEquipments = {Equipments.HELMET, Equipments.CHESTPLATE, Equipments.LEGGINGS, Equipments.BOOTS, Equipments.SWORD,
			Equipments.TWOHAND};

	public class Equipments {
		public static final String HELMET = "helmet";
		public static final String CHESTPLATE = "chestplate";
		public static final String LEGGINGS = "leggings";
		public static final String BOOTS = "boots";
		public static final String SWORD = "sword";
		public static final String TWOHAND = "two";
	}

	public static boolean isEquipments(String key) {
		for (String equip : EquipmentElement.allEquipments) {
			if (equip.equalsIgnoreCase(key)) {
				return true;
			}
		}
		return false;
	}

	int slot;
	ArrayList<String> matTir;

	public Integer getSlot() {
		return slot;
	}
	public EquipmentElement(int slot) {
		this.slot = slot;
		matTir = new ArrayList<>();
	}
	public void addMatTir(String s) {
		matTir.add(s);
	}
	public String getMatTir(int tir) {
		return matTir.get(tir);
	}

}
