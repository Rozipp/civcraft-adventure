package com.avrgaming.civcraft.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

public class ConfigMaterialCategory {

	private static TreeMap<String, ConfigMaterialCategory> categories = new TreeMap<String, ConfigMaterialCategory>();

	public String id;
	public String title;
	public HashMap<String, ConfigCraftableMaterial> materials = new HashMap<String, ConfigCraftableMaterial>();
	public int craftableCount = 0;

	public static void addMaterial(ConfigCraftableMaterial mat) {
		ConfigMaterialCategory cat = categories.get(mat.categoryId);
		if (cat == null) {
			cat = new ConfigMaterialCategory();
			cat.title = mat.category;
			cat.id = mat.categoryId;
		}

		cat.materials.put(mat.id, mat);
		if (mat.craftable) cat.craftableCount++;
		categories.put(cat.id, cat);
	}

	public static Collection<ConfigMaterialCategory> getCategories() {
		return categories.values();
	}

	public static ConfigMaterialCategory getCategory(String key) {
		return categories.get(key);
	}

}
