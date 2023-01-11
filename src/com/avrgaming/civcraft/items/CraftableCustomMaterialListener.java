package com.avrgaming.civcraft.items;

import gpl.AttributeUtil;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCraftableMaterial;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.config.ConfigTechItem;
import com.avrgaming.civcraft.items.components.Tagged;
import com.avrgaming.civcraft.listener.SimpleListener;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ItemManager;

public class CraftableCustomMaterialListener extends SimpleListener {

	@EventHandler(priority = EventPriority.LOW)
	public void OnCraftItemEvent(CraftItemEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();

			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(event.getInventory().getResult());
			if (craftMat == null) {

				/* Disable notch apples */
				ItemStack resultStack = event.getInventory().getResult();
				if (resultStack == null) {
					return;
				}
				if (resultStack.getType().equals(Material.GOLDEN_APPLE)) {
					CivMessage.sendError((Player) event.getWhoClicked(), CivSettings.localize.localizedString("loreCraft_goldenApples"));
					event.setCancelled(true);
					return;
				}

				ConfigTechItem restrictedTechItem = CivSettings.techItems.get(ItemManager.getTypeId(resultStack));
				if (restrictedTechItem != null) {
					ConfigTech tech = CivSettings.techs.get(restrictedTechItem.require_tech);
					CivMessage.sendError(player, CivSettings.localize.localizedString("var_loreCraft_missingTech", tech.name));
					event.setCancelled(true);
					return;
				}

				return;
			}

			if (!(craftMat.getConfigMaterial()).playerHasTechnology(player)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_loreCraft_missingTech", ((ConfigCraftableMaterial) craftMat.getConfigMaterial()).getRequireString()));
				event.setCancelled(true);
				return;
			}
			// String matName =craftMat.getId();
			// if (matName.contains("_alt"))
			// {
			// ItemStack resultStack = event.getInventory().getResult();
			// String id = matName.replaceAll("_alt(.*)", "");
			// ItemStack newStack = LoreMaterial.spawn(LoreMaterial.materialMap.get(id));
			// newStack.setAmount(resultStack.getAmount());
			// event.getInventory().setResult(newStack);
			// CivLog.debug("Item Crafting: " +id);
			// }

			// if (craftMat.hasComponent("Tagged")) {
			// String tag = Tagged.matrixHasSameTag(event.getInventory().getMatrix());
			// if (tag == null) {
			// CivMessage.sendError(player, "All items must have been generated from the same camp.");
			// event.setCancelled(true);
			// return;
			// }
			//
			// Tagged tagged = (Tagged)craftMat.getComponent("Tagged");
			// ItemStack stack = tagged.addTag(event.getInventory().getResult(), tag);
			// AttributeUtil attrs = new AttributeUtil(stack);
			// attrs.addLore(CivColor.LightGray+tag);
			// stack = attrs.getStack();
			// event.getInventory().setResult(stack);
			// }

			Resident resident = CivGlobal.getResident(player);
			if (craftMat.getId().equals("mat_found_camp")) {
				// PlatinumManager.givePlatinumOnce(resident,
				// CivSettings.platinumRewards.get("buildCamp").name,
				// CivSettings.platinumRewards.get("buildCamp").amount,
				// "Achievement! You've founded your first camp and earned %d");
			} else
				if (craftMat.getId().equals("mat_found_civ")) {
					// PlatinumManager.givePlatinumOnce(resident,
					// CivSettings.platinumRewards.get("buildCiv").name,
					// CivSettings.platinumRewards.get("buildCiv").amount,
					// "Achievement! You've founded your first Civilization and earned %d");
				} else {
					class AsyncTask implements Runnable {
						Resident resident;
						int craftAmount;

						public AsyncTask(Resident resident, int craftAmount) {
							this.resident = resident;
							this.craftAmount = craftAmount;
						}

						@Override
						public void run() {
							String key = resident.getName() + ":platinumCrafted";
							ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(key);
							Integer amount = 0;

							if (entries.size() == 0) {
								amount = craftAmount;
								CivGlobal.getSessionDatabase().add(key, "" + amount, 0, 0, 0);

							} else {
								amount = Integer.valueOf(entries.get(0).value);
								amount += craftAmount;
								if (amount >= 100) {
									// PlatinumManager.givePlatinum(resident,
									// CivSettings.platinumRewards.get("craft100Items").amount,
									// "Expert crafting earns you %d");
									amount -= 100;
								}

								CivGlobal.getSessionDatabase().update(entries.get(0).request_id, key, "" + amount);
							}
						}
					}

					/* if shift clicked, the amount crafted is always min. */
					int amount;
					if (event.isShiftClick()) {
						amount = 64; // cant craft more than 64.
						for (ItemStack stack : event.getInventory().getMatrix()) {
							if (stack == null) {
								continue;
							}

							if (stack.getAmount() < amount) {
								amount = stack.getAmount();
							}
						}
					} else {
						amount = 1;
					}

					TaskMaster.asyncTask(new AsyncTask(resident, amount), 0);
				}
		}
	}

	private boolean matrixContainsCustom(ItemStack[] matrix) {
		for (ItemStack stack : matrix) {
			if (CustomMaterial.isCustomMaterial(stack)) {
				return true;
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void OnPrepareItemCraftEvent(PrepareItemCraftEvent event) {
		if (event.getRecipe() instanceof ShapedRecipe) {
			String key = CraftableCustomMaterial.getShapedRecipeKey(event.getInventory().getMatrix());
			CraftableCustomMaterial loreMat = CraftableCustomMaterial.shapedKeys.get(key);

			if (loreMat == null) {
				if (CustomMaterial.isCustomMaterial(event.getRecipe().getResult())) {
					/* Result is custom, but we have found no custom recipie. Set to blank. */
					event.getInventory().setResult(new ItemStack(Material.AIR));
				}
				if (matrixContainsCustom(event.getInventory().getMatrix())) {
					event.getInventory().setResult(new ItemStack(Material.AIR));
				}
				return;
			} else {
				if (!CustomMaterial.isCustomMaterial(event.getRecipe().getResult())) {
					/* Result is not custom, but recipie is. Set to blank. */
					if (!loreMat.isVanilla()) {
						event.getInventory().setResult(new ItemStack(Material.AIR));
						return;
					}
				}
			}

			String matName = loreMat.getId();
			if (matName.contains("_alt")) {
				String id = matName.replaceAll("_alt(.*)", "");
				loreMat = CraftableCustomMaterial.getCraftableCustomMaterial(id);
			}

			ItemStack newStack;
			if (!loreMat.isVanilla()) {
				newStack = CustomMaterial.spawn(loreMat);
				AttributeUtil attrs = new AttributeUtil(newStack);
				loreMat.applyAttributes(attrs);
				newStack.setAmount(loreMat.getCraftAmount());
			} else {
				newStack = ItemManager.createItemStack(loreMat.getTypeID(), loreMat.getCraftAmount());
			}

			event.getInventory().setResult(newStack);

		} else
			if (event.getRecipe() instanceof ShapelessRecipe) {
				String key = CraftableCustomMaterial.getShapelessRecipeKey(event.getInventory().getMatrix());
				CraftableCustomMaterial loreMat = CraftableCustomMaterial.shapelessKeys.get(key);

				if (loreMat == null) {
					if (CustomMaterial.isCustomMaterial(event.getRecipe().getResult())) {
						/* Result is custom, but we have found no custom recipie. Set to blank. */
						event.getInventory().setResult(new ItemStack(Material.AIR));
					}
					if (matrixContainsCustom(event.getInventory().getMatrix())) {
						event.getInventory().setResult(new ItemStack(Material.AIR));
					}
					return;
				} else {
					if (!CustomMaterial.isCustomMaterial(event.getRecipe().getResult())) {
						/* Result is not custom, but recipie is. Set to blank. */
						if (!loreMat.isVanilla()) {
							event.getInventory().setResult(new ItemStack(Material.AIR));
							return;
						}
					}
				}

				String matName = loreMat.getId();
				if (matName.contains("_alt")) {
					String id = matName.replaceAll("_alt(.*)", "");
					loreMat = CraftableCustomMaterial.getCraftableCustomMaterial(id);
				}

				ItemStack newStack;
				if (!loreMat.isVanilla()) {
					newStack = CustomMaterial.spawn(loreMat);
					AttributeUtil attrs = new AttributeUtil(newStack);
					loreMat.applyAttributes(attrs);
					newStack.setAmount(loreMat.getCraftAmount());
				} else {
					newStack = ItemManager.createItemStack(loreMat.getTypeID(), loreMat.getCraftAmount());
				}
				event.getInventory().setResult(newStack);
			}

		ItemStack result = event.getInventory().getResult();
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(result);
		if (craftMat != null) {
			if (craftMat.hasComponent("Tagged")) {
				String tag = Tagged.matrixHasSameTag(event.getInventory().getMatrix());
				if (tag == null) {
					event.getInventory().setResult(ItemManager.createItemStack(CivData.AIR, 1));
					return;
				}

				Tagged tagged = (Tagged) craftMat.getComponent("Tagged");
				ItemStack stack = tagged.addTag(result, tag);
				event.getInventory().setResult(stack);
			}
		}

	}
}