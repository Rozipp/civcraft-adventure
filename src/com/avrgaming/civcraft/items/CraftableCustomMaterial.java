package com.avrgaming.civcraft.items;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCraftableMaterial;
import com.avrgaming.civcraft.config.ConfigIngredient;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ItemManager;

@Getter
@Setter
public class CraftableCustomMaterial extends BaseCustomMaterial {

	private boolean craftable;
	private boolean shaped;

	/* We will allow duplicate recipes with MC/materials by checking this map based on the results. The key is the material's ID as a string, so
	 * we can are only checking for custom items. The Itemstack array is the matrix for the recipe where the first 3 items represent the top
	 * row, and the last 3 represent the bottom row. */
	public static HashMap<CraftableCustomMaterial, ItemStack[]> shapedRecipes = new HashMap<CraftableCustomMaterial, ItemStack[]>();
	public static HashMap<String, CraftableCustomMaterial> shapedKeys = new HashMap<String, CraftableCustomMaterial>();

	/* We will allow duplicate shaped recipes by checking this map based on the results. In order for the recipe to be valid it must contain all
	 * of the item stacks and the respective amounts. */
	public static HashMap<CraftableCustomMaterial, LinkedList<ItemStack>> shapelessRecipes = new HashMap<CraftableCustomMaterial, LinkedList<ItemStack>>();
	public static HashMap<String, CraftableCustomMaterial> shapelessKeys = new HashMap<String, CraftableCustomMaterial>();

	public CraftableCustomMaterial(String id, int typeID, short damage) {
		super(id, typeID, damage);
	}

	@Override
	public void addMaterial() {
		CustomMaterial.craftableMaterials.put(this.getId(), this);
	}

	@Override
	public ConfigCraftableMaterial getConfigMaterial() {
		return (ConfigCraftableMaterial) this.configMaterial;
	}

	public static void buildStaticMaterials() {
		/* Loads in materials from configuration file. */
		for (ConfigCraftableMaterial cfgMat : CivSettings.craftableMaterials.values()) {
			CraftableCustomMaterial loreMat = new CraftableCustomMaterial(cfgMat.id, cfgMat.item_id, (short) cfgMat.item_data);
			loreMat.setName(cfgMat.name);
			loreMat.setLore(cfgMat.lore);
			loreMat.setCraftable(cfgMat.craftable);
			loreMat.setShaped(cfgMat.shaped);
			loreMat.configMaterial = cfgMat;
			loreMat.buildComponents();
			// materials.put(cfgMat.id, loreMat);
		}
	}

	public static String getShapedRecipeKey(ItemStack[] matrix) {
		String key = "";
		for (int i = 0; i < matrix.length; i++) {
			key += i + ":";

			ItemStack stack = matrix[i];
			if (stack == null) {
				key += "null,";
				continue;
			}
			String gMID = CustomMaterial.getMID(stack);
			if (!gMID.isEmpty()) {
				key += gMID + ",";
			} else {
				// key += "mc_"+stack.getTypeId()+"_"+stack.getDurability()+",";
				key += "mc_" + ItemManager.getTypeId(stack) + ",";

			}
		}

		return key;
	}

	public static String getShapelessRecipeKey(ItemStack[] matrix) {
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		List<String> items = new LinkedList<String>();

		/* Gather the counts for all the items in the matrix. */
		for (int i = 0; i < matrix.length; i++) {
			ItemStack stack = matrix[i];

			if (stack == null || ItemManager.getTypeId(stack) == CivData.AIR) continue;

			String item = CustomMaterial.getMID(stack);
			if (item.isEmpty()) {
				// item = "mc_"+stack.getTypeId()+"_"+stack.getDurability();
				item = "mc_" + ItemManager.getTypeId(stack) + ",";
			}

			Integer count = counts.get(item);
			if (count == null)
				count = 1;
			else count++;
			counts.put(item, count);
		}

		/* Merge the counts in to the string list. */
		for (String item : counts.keySet()) {
			Integer count = counts.get(item);
			items.add(item + ":" + count);
		}

		/* Sort the list alphabetically so that ordering is consistent. */
		java.util.Collections.sort(items);

		String fullString = "";
		for (String item : items) {
			fullString += item + ",";
		}

		return fullString;
	}

	public static void buildRecipes() {
		/* Loads in materials from configuration file. */
		for (BaseCustomMaterial bmat : getAllCraftableCustomMaterial()) {
			CraftableCustomMaterial mat = (CraftableCustomMaterial) bmat;
			if (!mat.isCraftable()) continue;

			ItemStack stack = CustomMaterial.spawn(mat);
			ConfigCraftableMaterial configMaterial = (ConfigCraftableMaterial) mat.configMaterial;

			if (mat.isShaped()) {
				ItemStack[] matrix = new ItemStack[9];
				 ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(CivCraft.getPlugin(), "civ_" + mat.getConfigId()), stack);
//				ShapedRecipe recipe = new ShapedRecipe(stack);
				recipe.shape(configMaterial.shape[0], configMaterial.shape[1], configMaterial.shape[2]);

				/* Setup the ingredients. */
				for (ConfigIngredient ingred : configMaterial.ingredients.values()) {
					ItemStack ingredStack = null;

					if (ingred.custom_id == null) {
						recipe.setIngredient(ingred.letter.charAt(0), ItemManager.getMaterialData(ingred.type_id, ingred.data));
						ingredStack = ItemManager.createItemStack(ingred.type_id, (short) ingred.data, 1);
					} else {
						CraftableCustomMaterial customLoreMat = getCraftableCustomMaterial(ingred.custom_id);
						if (customLoreMat == null) {
							CivLog.warning("Couldn't find custom material id:" + ingred.custom_id);
						}

						ConfigCraftableMaterial customMat = (ConfigCraftableMaterial) customLoreMat.configMaterial;
						if (customMat != null) {
							recipe.setIngredient(ingred.letter.charAt(0), ItemManager.getMaterialData(customMat.item_id, customMat.item_data));
						} else {
							CivLog.warning("Couldn't find custom material id:" + ingred.custom_id);
						}

						ingredStack = CustomMaterial.spawn(customLoreMat);
					}

					/* Add this incred to the shape. */
					int i = 0;
					for (String row : configMaterial.shape) {
						for (int c = 0; c < row.length(); c++) {
							if (row.charAt(c) == ingred.letter.charAt(0)) {
								matrix[i] = ingredStack;
							} else if (row.charAt(c) == ' ') {
								// TODO matrix[i] = new ItemStack(Material.AIR, 0, (short) -1);
								matrix[i] = null;
							}
							i++;
						}
					}

				}

				shapedRecipes.put(mat, matrix);
				String key = getShapedRecipeKey(matrix);
				shapedKeys.put(key, mat);

				/* Register recipe with server. */
				Bukkit.getServer().addRecipe(recipe);
			} else {
				/* Shapeless Recipe */
				 ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(CivCraft.getPlugin(), "civ_" + mat.getConfigId()), stack);
//				ShapelessRecipe recipe = new ShapelessRecipe(stack);
				LinkedList<ItemStack> items = new LinkedList<ItemStack>();
				ItemStack[] matrix = new ItemStack[9];
				int matrixIndex = 0;
				/* Setup the ingredients. */
				for (ConfigIngredient ingred : configMaterial.ingredients.values()) {
					ItemStack ingredStack = null;
					try {
						if (ingred.custom_id == null) {
							recipe.addIngredient(ingred.count, ItemManager.getMaterialData(ingred.type_id, ingred.data));
							ingredStack = ItemManager.createItemStack(ingred.type_id, (short) ingred.data, 1);
						} else {
							CraftableCustomMaterial customLoreMat = getCraftableCustomMaterial(ingred.custom_id);
							if (customLoreMat == null) CivLog.error("Couldn't configure ingredient:" + ingred.custom_id + " in config mat:" + configMaterial.id);
							ConfigMaterial customMat = customLoreMat.configMaterial;
							if (customMat != null) {
								recipe.addIngredient(ingred.count, ItemManager.getMaterialData(customMat.item_id, customMat.item_data));
								ingredStack = CustomMaterial.spawn(customLoreMat);
							} else CivLog.warning("Couldn't find custom material id:" + ingred.custom_id);
						}
					} catch (IllegalArgumentException e) {
						CivLog.warning("Trying to process ingredient:" + ingred.type_id + ":" + ingred.custom_id + " for material:" + configMaterial.id);
						throw e;
					}
					if (ingredStack != null) {
						// loreMat.shaplessIngredientList.add(ingredStack);
						for (int i = 0; i < ingred.count; i++) {
							if (matrixIndex > 9) break;
							matrix[matrixIndex] = ingredStack;
							matrixIndex++;
						}
						ingredStack.setAmount(ingred.count);
						items.add(ingredStack);
					}
				}
				shapelessRecipes.put(mat, items);
				String key = getShapelessRecipeKey(matrix);
				shapelessKeys.put(key, mat);
				/* Register recipe with server. */
				Bukkit.getServer().addRecipe(recipe);
			}
		}
	}

	public int getCraftAmount() {
		return ((ConfigCraftableMaterial) this.configMaterial).amount;
	}
}
