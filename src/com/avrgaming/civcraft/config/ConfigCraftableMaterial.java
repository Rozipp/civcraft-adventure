package com.avrgaming.civcraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class ConfigCraftableMaterial extends ConfigMaterial {

	/* Required */
	public String category = CivSettings.localize.localizedString("config_material_misc");
	public String categoryId = category;
	public int tier = 0;

	/* Optional */
	public boolean craftable = false;
	public String required_tech = null;
	public boolean shaped = false;
	public HashMap<String, ConfigIngredient> ingredients;
	public String[] shape;
	public boolean vanilla = false;
	public int amount = 1;

	public static void loadConfigCraftable(FileConfiguration cfg, Map<String, ConfigCraftableMaterial> materials) {
		materials.clear();
		List<Map<?, ?>> configMaterials = cfg.getMapList("materials");
		for (Map<?, ?> b : configMaterials) {
			ConfigCraftableMaterial mat = loadOneCraftableConfig(b);
			/* Add to category map. */
			ConfigMaterialCategory.addMaterial(mat);
			materials.put(mat.id, mat);
		}
		CivLog.info("Loaded " + materials.size() + " Materials.");
	}

	@SuppressWarnings("unchecked")
	public static ConfigCraftableMaterial loadOneCraftableConfig(Map<?, ?> b) {
		ConfigCraftableMaterial mat = new ConfigCraftableMaterial();

		/* Mandatory Settings */
		mat.id = (String) b.get("id");
		String[] itemSplit = ((String) b.get("item_id")).split(":");
		mat.item_id = Integer.valueOf(itemSplit[0]);
		mat.item_data = Integer.valueOf(itemSplit[1]);
		mat.name = (String) b.get("name");
		mat.name = CivColor.colorize(mat.name);

		String category = (String) b.get("category");
		if (category != null) {
			mat.category = CivColor.colorize(category);
			mat.categoryId = CivColor.stripTags(category).toLowerCase().replace(" ", "_");

			if (mat.category.toLowerCase().contains("ier 1")) {
				mat.tier = 1;
			} else if (mat.category.toLowerCase().contains("ier 2")) {
				mat.tier = 2;
			} else if (mat.category.toLowerCase().contains("ier 3")) {
				mat.tier = 3;
			} else if (mat.category.toLowerCase().contains("ier 4")) {
				mat.tier = 4;
			} else {
				mat.tier = 0;
			}
		}

		List<?> configLore = (List<?>) b.get("lore");
		if (configLore != null) {
			String[] lore = new String[configLore.size()];
			int i = 0;
			for (Object obj : configLore) {
				if (obj instanceof String) {
					lore[i] = (String) obj;
					i++;
				}
			}
		}

		Boolean isShiny = (Boolean) b.get("shiny");
		if (isShiny != null) mat.shiny = isShiny;

		Double tValue = (Double) b.get("tradeValue");
		if (tValue != null) {
			mat.tradeable = true;
			mat.tradeValue = tValue;
		} else mat.tradeable = false;

		Boolean vanilla = (Boolean) b.get("vanilla");
		if (vanilla != null) mat.vanilla = vanilla;

		String required_tech = (String) b.get("required_techs");
		if (required_tech != null) mat.required_tech = required_tech;

		List<Map<?, ?>> comps = (List<Map<?, ?>>) b.get("components");
		if (comps != null) {
			for (Map<?, ?> compObj : comps) {
				HashMap<String, String> compMap = new HashMap<String, String>();
				for (Object key : compObj.keySet())
					compMap.put((String) key, (String) compObj.get(key));
				mat.components.add(compMap);
			}
		}

		List<Map<?, ?>> configIngredients = (List<Map<?, ?>>) b.get("ingredients");
		if (configIngredients != null) {
			mat.craftable = true;
			mat.ingredients = new HashMap<String, ConfigIngredient>();

			for (Map<?, ?> ingred : configIngredients) {
				ConfigIngredient ingredient = new ConfigIngredient();
				String ingred_id = (String) ingred.get("ingred_id");

				String key = null;
				if (ingred_id.contains(":")) {
					ingredient.type_id = Integer.valueOf(ingred_id.split(":")[0]);
					ingredient.data = Integer.valueOf(ingred_id.split(":")[1]);
					key = "mc_" + ingredient.type_id;
				} else {
					ingredient.custom_id = ingred_id;
					key = ingred_id;
				}
				Boolean ignore_data = (Boolean) ingred.get("ignore_data");
				if (ignore_data == null || ignore_data == false)
					ingredient.ignore_data = false;
				else ingredient.ignore_data = true;

				Integer count = (Integer) ingred.get("count");
				if (count != null) ingredient.count = count;

				String letter = (String) ingred.get("letter");
				if (letter != null) ingredient.letter = letter;

				mat.ingredients.put(key, ingredient);
				// ConfigIngredient.ingredientMap.put(ingredient.custom_id, ingredient);
			}
		} else mat.craftable = false;

		/* Optional shape argument. */
		List<?> configShape = (List<?>) b.get("shape");
		if (configShape != null) {
			mat.shaped = true;
			String[] shape = new String[configShape.size()];
			int i = 0;
			for (Object obj : configShape)
				if (obj instanceof String) {
					shape[i] = (String) obj;
					i++;
				}
			mat.shape = shape;
		} else mat.shaped = false;
		return mat;
	}

	public boolean playerHasTechnology(Player player) {
		if (this.required_tech == null) return true;
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown()) return false;
		/* Parse technoloies */
		String[] split = this.required_tech.split(",");
		for (String tech : split) {
			tech = tech.replace(" ", "");
			if (!resident.getCiv().hasTechnologys(tech)) return false;
		}
		return true;
	}

	public String getRequireString() {
		String out = "";
		if (this.required_tech == null) return out;
		/* Parse technoloies */
		String[] split = this.required_tech.split(",");
		for (String tech : split) {
			tech = tech.replace(" ", "");
			ConfigTech technology = CivSettings.techs.get(tech);
			if (technology != null) out += technology.name + ", ";
		}
		return out;
	}

	public static void removeRecipes(FileConfiguration cfg, HashSet<Material> removedRecipies) {

		List<Map<?, ?>> configMaterials = cfg.getMapList("removed_recipes");
		for (Map<?, ?> b : configMaterials) {
			int type_id = (Integer) b.get("type_id");
			int data = (Integer) b.get("data");
			ItemStack is = new ItemStack(ItemManager.getMaterial(type_id), 1, (short) data);
			removedRecipies.add(is.getType());
		}

		// Idk why you change scope, but why not
		List<Recipe> backup = new ArrayList<Recipe>();
		Iterator<Recipe> a = Bukkit.getServer().recipeIterator();
		while (a.hasNext()) {
			Recipe recipe = a.next();
			ItemStack result = recipe.getResult();
			Material mat = result.getType();
			if (!removedRecipies.contains(mat)) {
				backup.add(recipe);
			}
		}

		Bukkit.getServer().clearRecipes();
		for (Recipe r : backup) {
			Bukkit.getServer().addRecipe(r);
		}
	}

}
