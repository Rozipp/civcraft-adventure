package com.avrgaming.civcraft.units;

public enum Equipments {
	HELMET, CHESTPLATE, LEGGINGS, BOOTS, MAINHAND, TWOHAND;

	private int slot = -1;

	public int getSlot() {
		if (slot == -1) {
			switch (this) {
			case HELMET:
				slot = 39;
				break;
			case CHESTPLATE:
				slot = 38;
				break;
			case LEGGINGS:
				slot = 37;
				break;
			case BOOTS:
				slot = 36;
				break;
			case MAINHAND:
				slot = 0;
				break;
			case TWOHAND:
				slot = 40;
				break;
			}
		}
		return slot;
	}

	public static Equipments identificateEquipments(String mid) {
		if (mid.contains("_helmet")) return Equipments.HELMET;
		if (mid.contains("_chestplate")) return Equipments.CHESTPLATE;
		if (mid.contains("_leggings")) return Equipments.LEGGINGS;
		if (mid.contains("_boots")) return Equipments.BOOTS;
		if (mid.contains("_sword")) return Equipments.MAINHAND;
		if (mid.contains("_bow")) return Equipments.TWOHAND;
		return null;
	}
}
