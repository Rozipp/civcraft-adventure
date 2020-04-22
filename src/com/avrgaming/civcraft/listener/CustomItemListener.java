/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.listener;

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigRemovedRecipes;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.items.ItemDurabilityEntry;
import com.avrgaming.civcraft.listener.armor.ArmorType;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementCritical;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementSoulBound;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementUnitItem;
import com.avrgaming.civcraft.lorestorage.ItemChangeResult;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.mythicmob.MobStatic;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import gpl.AttributeUtil;
import gpl.HorseModifier;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

@SuppressWarnings("deprecation")
public class CustomItemListener implements Listener {

	public static HashMap<String, LinkedList<ItemDurabilityEntry>> itemDuraMap = new HashMap<String, LinkedList<ItemDurabilityEntry>>();
	public static boolean duraTaskScheduled = false;

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreak(BlockBreakEvent event) {
		// this.onItemDurabilityChange(event.getPlayer(),
		// event.getPlayer().getInventory().getItemInMainHand());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreakSpawnItems(BlockBreakEvent event) {
		if (event.getBlock().getType().equals(Material.LAPIS_ORE)) {
			if (event.getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) 
				return;
			event.setCancelled(true);
			ItemManager.setTypeIdAndData(event.getBlock(), CivData.AIR, (byte) 0, true);

			try {
				Random rand = new Random();
				int min = CivSettings.getInteger(CivSettings.craftableMaterialsConfig, "tungsten_min_drop");
				int max = event.getPlayer().getInventory().getItemInMainHand()
						.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)
								? CivSettings.getInteger(CivSettings.craftableMaterialsConfig,
										"tungsten_max_drop_with_fortune")
								: CivSettings.getInteger(CivSettings.craftableMaterialsConfig, "tungsten_max_drop");

				int randAmount = rand.nextInt(min + max);
				randAmount -= min;
				if (randAmount <= 0)
					randAmount = 1;

				for (int i = 0; i < randAmount; i++) {
					ItemStack stack = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tungsten_ore"));
					event.getPlayer().getWorld().dropItemNaturally(event.getBlock().getLocation(), stack);
				}

			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
		if (stack == null || stack.getType().equals(Material.AIR))
			return;
		CustomMaterial mat = CustomMaterial.getCustomMaterial(stack);
		if (mat == null)
			return;
		mat.onBlockPlaced(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack stack = null;
		if (event.getHand() == EquipmentSlot.OFF_HAND)
			stack = event.getPlayer().getInventory().getItemInOffHand();
		else
			stack = event.getPlayer().getInventory().getItemInMainHand();
		if (stack == null)
			return;
		CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
		if (material != null)
			material.onInteract(event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		if (event.isCancelled())
			return;
		ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
		if (stack == null)
			return;
		CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
		if (material != null)
			material.onInteractEntity(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemHeld(PlayerItemHeldEvent event) {
		if (event.isCancelled())
			return;
		ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
		if (stack == null)
			return;
		CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
		if (material != null)
			material.onHold(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnPlayerDropItem(PlayerDropItemEvent event) {
		if (event.isCancelled())
			return;
		ItemStack stack = event.getItemDrop().getItemStack();

		CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
		if (cmat != null) {
			cmat.onDropItem(event);
			return;
		}

		String custom = isCustomDrop(stack);
		if (custom != null)
			event.setCancelled(true);
	}

	private static String isCustomDrop(ItemStack stack) {
		if (stack == null || ItemManager.getTypeId(stack) != 166)
			return null;
		if (LoreGuiItem.isGUIItem(stack))
			return null;
		return stack.getItemMeta().getDisplayName();
	}

	/* Prevent the player from using goodies in crafting recipies. */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnCraftItemEvent(CraftItemEvent event) {
		for (ItemStack stack : event.getInventory().getMatrix()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null)
				cmat.onCraftItem(event);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnPlayerItemPickup(EntityPickupItemEvent event) {
		if (event.isCancelled())
			return;
		CustomMaterial cmat = CustomMaterial.getCustomMaterial(event.getItem().getItemStack());
		if (cmat != null)
			cmat.onPickupItem(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnItemSpawn(ItemSpawnEvent event) {
		ItemStack stack = event.getEntity().getItemStack();
		CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
		if (cmat != null) {
			cmat.onItemSpawn(event);
			return;
		}

		String custom = isCustomDrop(stack);

		if (custom != null) {
			ItemStack newStack = CustomMaterial.spawn(CustomMaterial.getCustomMaterial(custom), stack.getAmount());
			event.getEntity().getWorld().dropItemNaturally(event.getLocation(), newStack);
			event.setCancelled(true);
			return;
		}

		if (isUnwantedVanillaItem(stack)) {
			if (!stack.getType().equals(Material.HOPPER) && !stack.getType().equals(Material.HOPPER_MINECART)) {
				event.setCancelled(true);
				event.getEntity().remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDefenseAndAttack(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Double baseDamage = event.getDamage();

		Player defendingPlayer = null;
		if (event.getEntity() instanceof Player) {
			defendingPlayer = (Player) event.getEntity();
		}

		if (event.getDamager() instanceof LightningStrike) {
			/* Return after Tesla tower does damage, do not apply armor defense. */
			try {
				event.setDamage(CivSettings.getInteger(CivSettings.warConfig, "tesla_tower.damage"));
				return;
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
		}

		if (event.getDamager() instanceof Arrow) {
			LivingEntity shooter = (LivingEntity) ((Arrow) event.getDamager()).getShooter();

			if (shooter instanceof Player) {
				ItemStack inHand = ((Player) shooter).getInventory().getItemInMainHand();
				CraftableCustomMaterial craftMat = CustomMaterial.getCraftableCustomMaterial(inHand);
				if (craftMat != null)
					craftMat.onRangedAttack(event, inHand);
			} else {
				ArrowFiredCache afc = CivCache.arrowsFired.get(event.getDamager().getUniqueId());
				if (afc != null) {
					/* Arrow was fired by a tower. */
					afc.setHit(true);
					afc.destroy(event.getDamager());
					if (defendingPlayer != null) {
						Resident defenderResident = CivGlobal.getResident(defendingPlayer);
						if (defenderResident != null && defenderResident.hasTown()
								&& defenderResident.getTown().getCiv() == afc.getFromTower().getTown().getCiv()) {
							/* Prevent friendly fire from arrow towers. */
							event.setCancelled(true);
							return;
						}
					}

					/* Return after arrow tower does damage, do not apply armor defense. */
					event.setDamage((double) afc.getFromTower().getDamage());
					return;
				}
			}
		} else if (event.getDamager() instanceof Player) {
			ItemStack inHand = ((Player) event.getDamager()).getInventory().getItemInMainHand();
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(inHand);
			if (craftMat != null) {
				craftMat.onAttack(event, inHand);
			} else {
				/* Non-civcraft items only do 0.5 damage. */
				event.setDamage(0.5);
			}
		} else {
			if (MobStatic.isMithicMobEntity(event.getDamager())) {
				event.setDamage(MobStatic.getMithicMob(event.getDamager()).getDamage());
			}
		}

		if (event.getEntity() instanceof Horse) {
			if (HorseModifier.isCivCraftHorse((LivingEntity) event.getEntity())) {
				// Horses take 50% damage from all sources.
				event.setDamage(event.getDamage() / 2.0);
			}
		}

		if (defendingPlayer != null) {
			/* Search equipt items for defense event. */
			for (ItemStack stack : defendingPlayer.getEquipment().getArmorContents()) {
				CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
				if (cmat != null)
					cmat.onDefense(event, stack);
			}
			if (event.getDamager() instanceof LivingEntity) {
				LivingEntity le = (LivingEntity) event.getDamager();
				ItemStack chestplate = defendingPlayer.getEquipment().getChestplate();
				AttributeUtil attrs = new AttributeUtil(chestplate);
				if (attrs.hasEnhancement("LoreEnhancementThorns")) {
					le.damage(
							event.getDamage()
									* Double.valueOf(attrs.getEnhancementData("LoreEnhancementThorns", "value")),
							defendingPlayer);
				}
			}
		}
		Entity e = event.getDamager();
		if (e instanceof Player) {
			Player player = (Player) e;
			AttributeUtil au = new AttributeUtil(player.getInventory().getItemInMainHand());
			if (au.hasEnhancement("LoreEnhancementCritical") && LoreEnhancementCritical.randomCriticalAttack(au)) {
				if (event.getEntity() instanceof LivingEntity) {
					LivingEntity le = (LivingEntity) event.getEntity();
					TaskMaster.syncTask(new Runnable() {
						@Override
						public void run() {
							CivLog.debug("LoreEnhancementCritical");
							le.damage(baseDamage, player);
						}
					}, 10);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnInventoryClose(InventoryCloseEvent event) {
		for (ItemStack stack : event.getInventory().getContents()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null)
				cmat.onInventoryClose(event);
		}

		for (ItemStack stack : event.getPlayer().getInventory()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null)
				cmat.onInventoryClose(event);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnInventoryOpen(InventoryOpenEvent event) {
		for (ItemStack stack : event.getInventory().getContents()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null)
				cmat.onInventoryOpen(event, stack);
		}

		for (ItemStack stack : event.getPlayer().getInventory()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null)
				cmat.onInventoryOpen(event, stack);
		}

		for (ItemStack stack : event.getPlayer().getInventory().getArmorContents()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null)
				cmat.onInventoryOpen(event, stack);
		}
	}

	/* Returns false if item is destroyed. */
	private boolean processDurabilityChanges(PlayerDeathEvent event, ItemStack stack, int i) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat != null) {
			ItemChangeResult result = craftMat.onDurabilityDeath(event, stack);
			if (result != null) {
				if (!result.destroyItem) {
					event.getEntity().getInventory().setItem(i, result.stack);
				} else {
					event.getEntity().getInventory().setItem(i, new ItemStack(Material.AIR));
					event.getDrops().remove(stack);
					return false;
				}
			}
		}

		return true;
	}

	private boolean processArmorDurabilityChanges(PlayerDeathEvent event, ItemStack stack, int i) {
		CraftableCustomMaterial craftMat = CustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat != null) {
			ItemChangeResult result = craftMat.onDurabilityDeath(event, stack);
			if (result != null) {
				if (!result.destroyItem) {
					replaceItem(event.getEntity().getInventory(), stack, result.stack);
				} else {
					replaceItem(event.getEntity().getInventory(), stack, new ItemStack(Material.AIR));
					event.getDrops().remove(stack);
					return false;
				}
			}
		}

		return true;
	}

	private void replaceItem(PlayerInventory playerInventory, ItemStack oldItem, ItemStack newItem) {
		ArmorType type = ArmorType.matchType(oldItem);
		switch (type) {
		case HELMET: {
			playerInventory.setHelmet(newItem);
			break;
		}
		case CHESTPLATE: {
			playerInventory.setChestplate(newItem);
			break;
		}
		case LEGGINGS: {
			playerInventory.setLeggings(newItem);
			break;
		}
		case BOOTS: {
			playerInventory.setBoots(newItem);
			break;
		}
		}

	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		HashMap<Integer, ItemStack> noDrop = new HashMap<Integer, ItemStack>();
		ItemStack[] armorNoDrop = new ItemStack[4];

		/* Search and execute any enhancements */
		for (int i = 0; i < event.getEntity().getInventory().getSize(); i++) {
			ItemStack stack = event.getEntity().getInventory().getItem(i);
			if (stack == null) {
				continue;
			}

			if (!processDurabilityChanges(event, stack, i)) {
				/* Don't process anymore more enhancements on items after its been destroyed. */
				continue;
			}

			if (!CustomMaterial.hasEnhancements(stack)) {
				continue;
			}

			AttributeUtil attrs = new AttributeUtil(stack);
			for (LoreEnhancement enhance : attrs.getEnhancements()) {
				if (enhance instanceof LoreEnhancementSoulBound) {
					/* Stack is not going to be dropped on death. */
					event.getDrops().remove(stack);
					noDrop.put(i, stack);
				}
				if (enhance instanceof LoreEnhancementUnitItem) {
					/* Stack is not going to be dropped on death. */
					event.getDrops().remove(stack);
				}
			}
		}

		/* Search for armor, apparently it doesnt show up in the normal inventory. */
		ItemStack[] contents = event.getEntity().getInventory().getArmorContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if (stack == null) {
				continue;
			}

			if (!processArmorDurabilityChanges(event, stack, i)) {
				/* Don't process anymore more enhancements on items after its been destroyed. */
				continue;
			}

			if (!CustomMaterial.hasEnhancements(stack)) {
				continue;
			}

			AttributeUtil attrs = new AttributeUtil(stack);
			for (LoreEnhancement enhance : attrs.getEnhancements()) {
				if (enhance instanceof LoreEnhancementSoulBound) {
					/* Stack is not going to be dropped on death. */
					event.getDrops().remove(stack);
					noDrop.put(i, stack);
				}
				if (enhance instanceof LoreEnhancementUnitItem) {
					/* Stack is not going to be dropped on death. */
					event.getDrops().remove(stack);
				}
			}
		}

		// event.getEntity().getInventory().getArmorContents()
		class SyncRestoreItemsTask implements Runnable {
			HashMap<Integer, ItemStack> restore;
			String playerName;
			ItemStack[] armorContents;

			public SyncRestoreItemsTask(HashMap<Integer, ItemStack> restore, ItemStack[] armorContents,
					String playerName) {
				this.restore = restore;
				this.playerName = playerName;
				this.armorContents = armorContents;
			}

			@Override
			public void run() {
				try {
					Player player = CivGlobal.getPlayer(playerName);
					PlayerInventory inv = player.getInventory();
					for (Integer slot : restore.keySet()) {
						ItemStack stack = restore.get(slot);
						inv.setItem(slot, stack);
					}

					inv.setArmorContents(this.armorContents);
				} catch (CivException e) {
					e.printStackTrace();
					return;
				}
			}

		}
		Boolean keepInventory = Boolean.valueOf(Bukkit.getWorld("world").getGameRuleValue("keepInventory"));
		if (!keepInventory) {
			TaskMaster.syncTask(new SyncRestoreItemsTask(noDrop, armorNoDrop, event.getEntity().getName()));
		}

	}

	@EventHandler(priority = EventPriority.LOW)
	public void OnEntityDeath(EntityDeathEvent event) {
		if (event.getEntity() instanceof Player) {
			return;
		}

		/* Remove any vanilla item IDs that can't be crafted from vanilla drops. */
		LinkedList<ItemStack> removed = new LinkedList<ItemStack>();
		for (ItemStack stack : event.getDrops()) {
			Integer key = ItemManager.getTypeId(stack);

			if (CivSettings.removedRecipies.containsKey(key)) {
				if (!CustomMaterial.isCustomMaterial(stack)) {
					removed.add(stack);
				}
			}
		}

		event.getDrops().removeAll(removed);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (event.isCancelled())
			return;
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			ItemStack oldStack = event.getItem().getItemStack();
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(oldStack);
			if (craftMat == null) {
				ItemStack newStack = getConvertLegacyItem(oldStack);
				newStack.setAmount(oldStack.getAmount());
				player.getInventory().addItem(newStack);
				player.updateInventory();
				event.getItem().remove();
				event.setCancelled(true);
			}
		}
	}

	/*
	 * Called when we click on an object, used for conversion to fix up reverse
	 * compat problems.
	 */
	public void convertLegacyItem(InventoryClickEvent event) {
		ItemStack oldStack = event.getCurrentItem();
		if ((oldStack == null) || (oldStack.getType() == Material.AIR))
			return;
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(oldStack);
		if (craftMat == null) {
			ItemStack newStack = getConvertLegacyItem(oldStack);
			newStack.setAmount(oldStack.getAmount());
			event.setCurrentItem(newStack);
		}
	}

	public ItemStack getConvertLegacyItem(ItemStack is) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(is);
		if (craftMat == null) {
			switch (is.getType()) {
			case SLIME_BALL:
				return CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_vanilla_slime"));
			case ENDER_PEARL:
				return CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ender_pearl"));
			case TNT:
				return CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_vanilla_tnt"));
			default:
				return is;
			}
		}
		return is;
	}

	private void onInventoryClickClick(InventoryClickEvent event) {
		ItemStack currentStack = event.getCurrentItem();
		ItemStack cursorStack = event.getCursor();
		CustomMaterial current = CustomMaterial.getCustomMaterial(currentStack);
		CustomMaterial cursor = CustomMaterial.getCustomMaterial(cursorStack);

		boolean currentEmpty = (current == null) || (ItemManager.getTypeId(currentStack) == CivData.AIR);
		boolean cursorEmpty = (cursor == null) || (ItemManager.getTypeId(cursorStack) == CivData.AIR);
		if (currentEmpty && cursorEmpty)
			return;
		convertLegacyItem(event);

		Inventory clickedInv = event.getClickedInventory();

		if (!currentEmpty)
			current.onInvItemPickup(event, clickedInv, currentStack);
		if (!cursorEmpty)
			cursor.onInvItemDrop(event, clickedInv, cursorStack);
	}

	private void onInventoryClickMove(InventoryClickEvent event) {
		ItemStack firstStack = event.getCurrentItem();
		CustomMaterial first = CustomMaterial.getCustomMaterial(firstStack);

		boolean firstEmpty = (first == null) || (ItemManager.getTypeId(firstStack) == CivData.AIR);
		if (firstEmpty)
			return;
		convertLegacyItem(event);

		InventoryView view = event.getView();

		Inventory clickedInv = event.getClickedInventory();
		Inventory otherInv;
		if (view.getType().equals(InventoryType.CRAFTING)) {
			// This is the player's own inventory. The 'top' inventory is the 2x2 crafting
			// area plus the output.
			// During shift click, items do not go there so the otherInv should always be
			// the player's inventory aka the bottom.
			otherInv = view.getBottomInventory();
		} else if (event.getRawSlot() == view.convertSlot(event.getRawSlot())) // Clicked in the top holder
			otherInv = view.getBottomInventory();
		else
			otherInv = view.getTopInventory();

		first.onInvItemPickup(event, clickedInv, firstStack);
		first.onInvItemDrop(event, otherInv, firstStack);
	}

	private void onInventoryClickHotbar(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		ItemStack firstStack = event.getCurrentItem();
		ItemStack secondStack = player.getInventory().getItem(event.getHotbarButton());
		CustomMaterial first = CustomMaterial.getCustomMaterial(firstStack);
		CustomMaterial second = CustomMaterial.getCustomMaterial(secondStack);

		boolean firstEmpty = (first == null) || (ItemManager.getTypeId(firstStack) == CivData.AIR);
		boolean secondEmpty = (second == null) || (ItemManager.getTypeId(secondStack) == CivData.AIR);
		if (firstEmpty && secondEmpty)
			return;
		convertLegacyItem(event);

		Inventory clickedInv = event.getClickedInventory();
		Inventory otherInv = player.getInventory();

		if (!firstEmpty) {
			first.onInvItemPickup(event, clickedInv, firstStack);
			first.onInvItemDrop(event, otherInv, firstStack);
		}
		if (!secondEmpty) {
			second.onInvItemPickup(event, otherInv, secondStack);
			second.onInvItemDrop(event, clickedInv, secondStack);
		}
	}

	private void onInventoryClickDrop(InventoryClickEvent event) {
		ItemStack firstStack = event.getCurrentItem();
		CustomMaterial first = CustomMaterial.getCustomMaterial(firstStack);
		boolean firstEmpty = (first == null) || (ItemManager.getTypeId(firstStack) == CivData.AIR);
		if (firstEmpty)
			return;
		convertLegacyItem(event);

		Inventory clickedInv = event.getInventory();
		first.onInvItemPickup(event, clickedInv, firstStack);
	}

	/* Track the location of the goodie. */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.isCancelled())
			return;
		InventoryAction ia = event.getAction();
		switch (ia) {
		case NOTHING:
			return;
		case PICKUP_ALL:
		case PICKUP_HALF:
		case PICKUP_ONE:
		case PICKUP_SOME:
		case PLACE_ALL:
		case PLACE_ONE:
		case PLACE_SOME:
		case SWAP_WITH_CURSOR:
		case COLLECT_TO_CURSOR: // TODO Тут наверно надо обработку для предметов у которых ограничено
								// максимальное количество в стаке
			onInventoryClickClick(event);
			break;
		case DROP_ALL_CURSOR:
		case DROP_ALL_SLOT:
		case DROP_ONE_CURSOR:
		case DROP_ONE_SLOT:
			onInventoryClickDrop(event);
			break;
		case MOVE_TO_OTHER_INVENTORY:
			onInventoryClickMove(event);
			break;
		case HOTBAR_MOVE_AND_READD:
		case HOTBAR_SWAP:
			onInventoryClickHotbar(event);
			break;
		default:
			break;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryMoveItemEvent event) {
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryDragEvent event) {
		if (event.isCancelled())
			return;
//		Player player = (Player) event.getWhoClicked();
		ItemStack firstStack = event.getOldCursor();
		CustomMaterial first = CustomMaterial.getCustomMaterial(firstStack);
		boolean firstEmpty = (first == null) || (ItemManager.getTypeId(firstStack) == CivData.AIR);
		if (firstEmpty)
			return;

		InventoryView view = event.getView();

		Inventory clickedInv = event.getInventory();
//		Inventory otherInv = player.getInventory();
		int ff = (int) event.getRawSlots().toArray()[0];
		if (ff == view.convertSlot(ff)) // Clicked in the top holder
			clickedInv = view.getTopInventory();
		else
			clickedInv = view.getBottomInventory();
		// TODO Несколько перемещений
		if (!firstEmpty)
			first.onInvDrag(event, clickedInv, firstStack);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial
				.getCraftableCustomMaterial(event.getPlayer().getInventory().getItemInMainHand());
		if (craftMat == null)
			return;
		craftMat.onPlayerInteractEntityEvent(event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerLeashEvent(PlayerLeashEntityEvent event) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial
				.getCraftableCustomMaterial(event.getPlayer().getInventory().getItemInMainHand());
		if (craftMat == null)
			return;
		craftMat.onPlayerLeashEvent(event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onItemDurabilityChange(PlayerItemDamageEvent event) {
		ItemStack stack = event.getItem();

		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat == null)
			return;
		craftMat.onItemDurabilityChange(event);
	}

	private static boolean isUnwantedVanillaItem(final ItemStack stack) {
		if (stack == null)
			return false;
		final CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat != null)
			return false;
		if (LoreGuiItem.isGUIItem(stack))
			return false;

		ConfigRemovedRecipes removed = CivSettings.removedRecipies.get(ItemManager.getTypeId(stack));
		if (removed == null && !stack.getType().equals(Material.ENCHANTED_BOOK)) {
			/* Check for badly enchanted tools */
			if (stack.containsEnchantment(Enchantment.DAMAGE_ALL)
					|| stack.containsEnchantment(Enchantment.DAMAGE_ARTHROPODS)
					|| stack.containsEnchantment(Enchantment.KNOCKBACK)
					|| stack.containsEnchantment(Enchantment.DAMAGE_UNDEAD)
					|| stack.containsEnchantment(Enchantment.DURABILITY)) {
			} else if (stack.containsEnchantment(Enchantment.FIRE_ASPECT)
					&& stack.getEnchantmentLevel(Enchantment.FIRE_ASPECT) > 2) {
				// Remove any fire aspect above this amount
			} else if (stack.containsEnchantment(Enchantment.LOOT_BONUS_MOBS)
					&& stack.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS) > 1) {
				// Only allow looting 1
			} else if (stack.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)
					&& stack.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS) > 1) {
				// Only allow fortune 1
			} else if (stack.containsEnchantment(Enchantment.DIG_SPEED)
					&& stack.getEnchantmentLevel(Enchantment.DIG_SPEED) > 5) {
				// only allow effiencey 5
			} else {
				/* Not in removed list, so allow it. */
				return false;
			}
		}
		return true;
	}

	public static void removeUnwantedVanillaItems(Player player, Inventory inv) {
		if (player.isOp())
			return; /* Allow OP to carry vanilla stuff. */
		boolean sentMessage = false;

		for (ItemStack stack : inv.getContents()) {
			if (!isUnwantedVanillaItem(stack))
				continue;
			inv.remove(stack);
			if (player != null)
				CivLog.info("Removed vanilla item:" + stack + " from " + player.getName());
			if (!sentMessage) {
				if (player != null)
					CivMessage.send(player, CivColor.LightGray
							+ CivSettings.localize.localizedString("customItem_restrictedItemsRemoved"));
				sentMessage = true;
			}
		}

		/* Also check the player's equipt. */
		if (player != null) {
			ItemStack[] contents = player.getEquipment().getArmorContents();
			boolean foundBad = false;
			for (int i = 0; i < contents.length; i++) {
				ItemStack stack = contents[i];
				if (stack == null)
					continue;
				CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
				if (craftMat != null)
					continue; /* Assume we are good if we are custom. */

				ConfigRemovedRecipes removed = CivSettings.removedRecipies.get(stack.getTypeId());
				if (removed == null && !stack.getType().equals(Material.ENCHANTED_BOOK))
					continue; /* Not in removed list, so allow it. */

				CivLog.info("Removed vanilla item:" + stack + " from " + player.getName() + " from armor.");
				contents[i] = new ItemStack(Material.AIR);
				foundBad = true;
				if (!sentMessage) {
					CivMessage.send(player, CivColor.LightGray
							+ CivSettings.localize.localizedString("customItem_restrictedItemsRemoved"));
					sentMessage = true;
				}
			}
			if (foundBad)
				player.getEquipment().setArmorContents(contents);
		}
		if (sentMessage)
			if (player != null)
				player.updateInventory();
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnInventoryHold(PlayerItemHeldEvent event) {
		ItemStack stack = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if (stack == null)
			return;
		CustomMaterial mat = CustomMaterial.getCustomMaterial(stack);
		if (mat == null)
			return;
		mat.onHold(event);
	}

//	/* Prevent books from being inside an inventory. */
	/* Prevent vanilla gear from being used. */
	/*
	 * @EventHandler(priority = EventPriority.LOWEST) public void
	 * OnInventoryOpenRemove(InventoryOpenEvent event) {
	 * //CivLog.debug("open event."); if (event.getPlayer() instanceof Player) {
	 * 
	 * //for (ItemStack stack : event.getInventory()) { for (int i = 0; i <
	 * event.getInventory().getSize(); i++) { ItemStack stack =
	 * event.getInventory().getItem(i); //CivLog.debug("stack cleanup");
	 * 
	 * AttributeUtil attrs = ItemCleanup(stack); if (attrs != null) {
	 * event.getInventory().setItem(i, attrs.getStack()); } } } }
	 */

	/*
	 * @EventHandler(priority = EventPriority.LOW) public void
	 * onPlayerLogin(PlayerLoginEvent event) {
	 * 
	 * class SyncTask implements Runnable { String playerName;
	 * 
	 * public SyncTask(String name) { playerName = name; }
	 * 
	 * @Override public void run() { try { Player player =
	 * CivGlobal.getPlayer(playerName);
	 * 
	 * for (int i = 0; i < player.getInventory().getSize(); i++) { ItemStack stack =
	 * player.getInventory().getItem(i);
	 * 
	 * AttributeUtil attrs = ItemCleanup(stack); if (attrs != null) {
	 * player.getInventory().setItem(i, attrs.getStack()); } }
	 * 
	 * ItemStack[] contents = new
	 * ItemStack[player.getInventory().getArmorContents().length]; for (int i = 0; i
	 * < player.getInventory().getArmorContents().length; i++) { ItemStack stack =
	 * player.getInventory().getArmorContents()[i];
	 * 
	 * AttributeUtil attrs = ItemCleanup(stack); if (attrs != null) { contents[i] =
	 * attrs.getStack(); } else { contents[i] = stack; } }
	 * 
	 * player.getInventory().setArmorContents(contents);
	 * 
	 * } catch (CivException e) { return; }
	 * 
	 * } }
	 * 
	 * TaskMaster.syncTask(new SyncTask(event.getPlayer().getName()));
	 * 
	 * }
	 */

	@EventHandler(priority = EventPriority.LOWEST)
	public void OnInventoryClickEvent(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			removeUnwantedVanillaItems((Player) event.getWhoClicked(), event.getView().getBottomInventory());
		}
	}

	/*
	 * Checks a players inventory and inventories that are opened for items. -
	 * Currently looks for old catalyst enhancements and marks them so they can be
	 * refunded.
	 */
	public AttributeUtil ItemCleanup(ItemStack stack) {

		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat == null) {
			return null;
		}

		AttributeUtil attrs = new AttributeUtil(stack);
		if (!attrs.hasEnhancements()) {
			return null;
		}
//		
//		/* Found a legacy catalysts. Repair it. */
//		ItemStack cleanItem = LoreCraftableMaterial.spawn(craftMat);
//		AttributeUtil attrsClean = new AttributeUtil(cleanItem);
//		
//		double level = 0;
//		for (LoreEnhancement enh : LoreCraftableMaterial.getLegacyEnhancements(stack)) {
//			if (enh instanceof LoreEnhancementDefense) {
//				level = Double.valueOf(attrs.getLegacyEnhancementData("LoreEnhancementDefense"));
//				LoreCraftableMaterial compatCatalyst = getCompatibleCatalyst(craftMat);
//				attrs.setCivCraftProperty("freeCatalyst", ""+level+":"+compatCatalyst.getId());
//				attrs.removeLegacyEnhancement("LoreEnhancementDefense");
//			} else if (enh instanceof LoreEnhancementAttack) {
//				level = Double.valueOf(attrs.getLegacyEnhancementData("LoreEnhancementAttack"));
//				LoreCraftableMaterial compatCatalyst = getCompatibleCatalyst(craftMat);
//				attrs.setCivCraftProperty("freeCatalyst", ""+level+":"+compatCatalyst.getId());
//				attrs.removeLegacyEnhancement("LoreEnhancementAttack");
//			} 
//		}
//		
//		attrs.setLore(attrsClean.getLore());
//		attrs.setName(attrsClean.getName());
//		attrs.add(Attribute.newBuilder().name("Attack").
//				type(AttributeType.GENERIC_ATTACK_DAMAGE).
//				amount(0).
//				build());
//		
//		if (level != 0) {
//			attrs.addLore(CivColor.LightBlue+level+" free enhancements! Redeem at blacksmith.");
//			CivLog.cleanupLog("Converted stack:"+stack+" with enhancement level:"+level);
//		
//		}
//		
//		for (LoreEnhancement enh : LoreCraftableMaterial.getLegacyEnhancements(stack)) {
//			if (enh instanceof LoreEnhancementSoulBound) {	
//				LoreEnhancementSoulBound soulbound = (LoreEnhancementSoulBound)LoreEnhancement.enhancements.get("LoreEnhancementSoulBound");
//				soulbound.add(attrs);
//			}
//		}
//		
//		
//
		return attrs;
	}

}
