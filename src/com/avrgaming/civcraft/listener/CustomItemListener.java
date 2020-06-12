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
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.EnchantmentCritical;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.items.components.ArrowComponent;
import com.avrgaming.civcraft.listener.armor.ArmorType;
import com.avrgaming.civcraft.lorestorage.ItemChangeResult;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.mythicmob.MobStatic;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.Cooldown;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import gpl.HorseModifier;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Arrow.PickupStatus;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class CustomItemListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreakSpawnItems(BlockBreakEvent event) {
		if (event.getBlock().getType().equals(Material.LAPIS_ORE)) {
			if (Enchantments.hasEnchantment(event.getPlayer().getInventory().getItemInMainHand(), CustomEnchantment.SILK_TOUCH)) return;
			event.setCancelled(true);
			ItemManager.setTypeIdAndData(event.getBlock(), CivData.AIR, (byte) 0, true);

			try {
				Random rand = new Random();
				int min = CivSettings.getInteger(CivSettings.craftableMaterialsConfig, "tungsten_min_drop");
				int max = event.getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS) ? CivSettings.getInteger(CivSettings.craftableMaterialsConfig, "tungsten_max_drop_with_fortune")
						: CivSettings.getInteger(CivSettings.craftableMaterialsConfig, "tungsten_max_drop");

				int randAmount = rand.nextInt(min + max);
				randAmount -= min;
				if (randAmount <= 0) randAmount = 1;

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
		if (event.isCancelled()) return;
		ItemStack stack = event.getItemInHand();
		if (stack == null || stack.getType().equals(Material.AIR)) return;
		CustomMaterial mat = CustomMaterial.getCustomMaterial(stack);
		if (mat == null) return;
		mat.onBlockPlaced(event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack stack = null;
		if (event.getHand() == EquipmentSlot.OFF_HAND)
			stack = event.getPlayer().getInventory().getItemInOffHand();
		else
			stack = event.getPlayer().getInventory().getItemInMainHand();
		if (stack == null) return;
		CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
		if (material != null) material.onInteract(event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		if (event.isCancelled()) return;
		ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
		if (stack == null) return;
		CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
		if (material != null) material.onInteractEntity(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onItemHeld(PlayerItemHeldEvent event) {
		if (event.isCancelled()) return;
		ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
		if (stack == null) return;
		CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
		if (material != null) material.onHold(event);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void OnPlayerDropItem(PlayerDropItemEvent event) {
		if (event.isCancelled()) return;
		ItemStack stack = event.getItemDrop().getItemStack();

		if (Enchantments.hasEnchantment(stack, CustomEnchantment.UnitItem)) {
			event.setCancelled(true);
			return;
		}

		CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
		if (cmat != null) {
			cmat.onDropItem(event);
			return;
		}

		String custom = isCustomDrop(stack);
		if (custom != null) {
			event.setCancelled(true);
			return;
		}

	}

	private static String isCustomDrop(ItemStack stack) {
		if (stack == null || ItemManager.getTypeId(stack) != 166) return null;
		if (LoreGuiItem.isGUIItem(stack)) return null;
		return stack.getItemMeta().getDisplayName();
	}

	/* Prevent the player from using goodies in crafting recipies. */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnCraftItemEvent(CraftItemEvent event) {
		for (ItemStack stack : event.getInventory().getMatrix()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null) cmat.onCraftItem(event);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnPlayerItemPickup(EntityPickupItemEvent event) {
		if (event.isCancelled()) return;
		CustomMaterial cmat = CustomMaterial.getCustomMaterial(event.getItem().getItemStack());
		if (cmat != null) cmat.onPickupItem(event);
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
	public void onEntityShootBowEvent(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();

			Arrow arrow = (Arrow) event.getProjectile();
			Location loc = event.getEntity().getEyeLocation();
			Vector velocity = arrow.getVelocity();
			float speed = (float) velocity.length();
			Vector dir = event.getEntity().getEyeLocation().getDirection();
			
			int slot = ArrowComponent.foundArrowComponent(player.getInventory());
			ItemStack stack = player.getInventory().getItem(slot);
			stack.setAmount(2);
			player.getInventory().setItem(slot, stack);
			FixedMetadataValue metadata = ArrowComponent.getMetadata(stack);
			if (metadata != null) {
				TippedArrow tarrow = loc.getWorld().spawnArrow(loc.add(dir.multiply(2)), dir, speed, 0.0f, TippedArrow.class);
				if (metadata == ArrowComponent.arrow_fire) tarrow.setFireTicks(2000);
				tarrow.setMetadata("civ_arrow_effect", metadata);
				arrow = tarrow;
			} else {
				arrow = loc.getWorld().spawnArrow(loc.add(dir.multiply(2)), dir, speed, 0.0f);
			}
			arrow.setShooter(event.getEntity());
			arrow.setPickupStatus(PickupStatus.DISALLOWED);
			event.setProjectile(arrow);
		}

	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDefenseAndAttack(EntityDamageByEntityEvent event) {
		Player attacker = null;
		if (event.getDamager() instanceof Player) {

		} else
			if (event.getDamager() instanceof Arrow) {
				Arrow arrow = (Arrow) event.getDamager();
				if (arrow.getShooter() instanceof Player) attacker = (Player) arrow.getShooter();
			}

		if (event.isCancelled()) return;
		Double baseDamage = event.getDamage();

		Player defendingPlayer = null;
		if (event.getEntity() instanceof Player) defendingPlayer = (Player) event.getEntity();

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
			Arrow arrow = (Arrow) event.getDamager();

			if (event.getEntity() instanceof LivingEntity) {
				if (arrow.hasMetadata("civ_arrow_effect")) {
					for (MetadataValue dd : arrow.getMetadata("civ_arrow_effect")) {
						// if (dd.equals(ArrowComponent.arrow_fire)) defendingPlayer.set);
						if (dd.equals(ArrowComponent.arrow_frost)) ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 5, 2));
						if (dd.equals(ArrowComponent.arrow_poison)) ((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5, 2));
					}
				}
			}
			
			if (arrow.getShooter() instanceof Player) {
				attacker = (Player) arrow.getShooter();
				ItemStack inHand = attacker.getEquipment().getItemInMainHand();
				if (!CustomMaterial.getMID(inHand).contains("_bow")) inHand = attacker.getEquipment().getItemInOffHand();

				CraftableCustomMaterial craftMat = CustomMaterial.getCraftableCustomMaterial(inHand);
				if (craftMat != null) craftMat.onRangedAttack(event, inHand);

			} else {
				ArrowFiredCache afc = CivCache.arrowsFired.get(event.getDamager().getUniqueId());
				if (afc != null) {
					/* Arrow was fired by a tower. */
					afc.setHit(true);
					afc.destroy(event.getDamager());
					if (defendingPlayer != null) {
						Resident defenderResident = CivGlobal.getResident(defendingPlayer);
						if (defenderResident != null && defenderResident.hasTown() && defenderResident.getTown().getCiv() == afc.getFromTower().getTown().getCiv()) {
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
		}

		if (event.getDamager() instanceof Player) {
			attacker = (Player) event.getDamager();

			ItemStack inHand = attacker.getInventory().getItemInMainHand();
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(inHand);

			if (craftMat != null) {
				craftMat.onAttack(event, inHand);
			} else {
				/* Non-civcraft items only do 0.5 damage. */
				event.setDamage(CivCraft.minDamage);
			}

			if (Enchantments.hasEnchantment(attacker.getInventory().getItemInMainHand(), CustomEnchantment.Critical) && EnchantmentCritical.randomCriticalAttack(attacker.getInventory().getItemInMainHand())) {
				if (event.getEntity() instanceof LivingEntity) {
					LivingEntity le = (LivingEntity) event.getEntity();
					final Player player = attacker;
					TaskMaster.syncTask(new Runnable() {
						@Override
						public void run() {
							le.damage(baseDamage, player);
						}
					}, 10);
				}
			}
		}

		if (MobStatic.isMithicMobEntity(event.getDamager())) {
			event.setDamage(MobStatic.getMithicMob(event.getDamager()).getDamage());
		}

		if (event.getEntity() instanceof Horse) {
			if (HorseModifier.isCivCraftHorse((LivingEntity) event.getEntity())) {
				// Horses take 50% damage from all sources.
				event.setDamage(event.getDamage() / 2.0);
			}
		}

		if (MobStatic.isMithicMobEntity(event.getEntity())) {
			ActiveMob mob = MobStatic.getMithicMob(event.getEntity());
			double dmg = event.getDamage();
			dmg = dmg - mob.getType().getBaseArmor();
			if (dmg < CivCraft.minDamage) {
				dmg = CivCraft.minDamage;
				if (attacker != null) CivMessage.sendErrorNoRepeat(attacker, "У вас не достаточно сил, что бы нанести урон");
			}
			event.setDamage(dmg);
		}

		if (defendingPlayer != null) {
			/* Search equipt items for defense event. */
			for (ItemStack stack : defendingPlayer.getEquipment().getArmorContents()) {
				CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
				if (cmat != null) cmat.onDefense(event, stack);
			}
			if (event.getDamager() instanceof LivingEntity) {
				LivingEntity le = (LivingEntity) event.getDamager();
				ItemStack chestplate = defendingPlayer.getEquipment().getChestplate();
				if (chestplate != null && Enchantments.hasEnchantment(chestplate, CustomEnchantment.Thorns)) {
					le.damage(event.getDamage() * Enchantments.getLevelEnchantment(chestplate, CustomEnchantment.Thorns), defendingPlayer);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnInventoryClose(InventoryCloseEvent event) {
		for (ItemStack stack : event.getInventory().getContents()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null) cmat.onInventoryClose(event);
		}

		for (ItemStack stack : event.getPlayer().getInventory()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null) cmat.onInventoryClose(event);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnInventoryOpen(InventoryOpenEvent event) {
		for (ItemStack stack : event.getInventory().getContents()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null) cmat.onInventoryOpen(event, stack);
		}

		for (ItemStack stack : event.getPlayer().getInventory()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null) cmat.onInventoryOpen(event, stack);
		}

		for (ItemStack stack : event.getPlayer().getInventory().getArmorContents()) {
			CustomMaterial cmat = CustomMaterial.getCustomMaterial(stack);
			if (cmat != null) cmat.onInventoryOpen(event, stack);
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

			if (Enchantments.hasEnchantment(stack, CustomEnchantment.SoulBound)) {
				event.getDrops().remove(stack);
				noDrop.put(i, stack);
			}
			if (Enchantments.hasEnchantment(stack, CustomEnchantment.UnitItem)) {
				event.getDrops().remove(stack);
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

			if (Enchantments.hasEnchantment(stack, CustomEnchantment.SoulBound)) {
				event.getDrops().remove(stack);
				noDrop.put(i, stack);
			}
			if (Enchantments.hasEnchantment(stack, CustomEnchantment.UnitItem)) {
				event.getDrops().remove(stack);
			}

		}

		// event.getEntity().getInventory().getArmorContents()
		class SyncRestoreItemsTask implements Runnable {
			HashMap<Integer, ItemStack> restore;
			String playerName;
			ItemStack[] armorContents;

			public SyncRestoreItemsTask(HashMap<Integer, ItemStack> restore, ItemStack[] armorContents, String playerName) {
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
			if (CivSettings.removedRecipies.contains(stack.getType())) {
				if (!CustomMaterial.isCustomMaterial(stack)) {
					removed.add(stack);
				}
			}
		}

		event.getDrops().removeAll(removed);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (event.isCancelled()) return;
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

	// ---------------Inventory begin
	private void onInventoryClickClick(InventoryClickEvent event) {
		ItemStack currentStack = event.getCurrentItem();
		ItemStack cursorStack = event.getCursor();
		boolean currentEmpty = (currentStack == null) || currentStack.getType().equals(Material.AIR);
		boolean cursorEmpty = (cursorStack == null) || cursorStack.getType().equals(Material.AIR);
		if (currentEmpty && cursorEmpty) return;
		convertLegacyItem(event);

		if (Cooldown.isCooldown(currentStack) || Cooldown.isCooldown(cursorStack)) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			((Player) event.getWhoClicked()).updateInventory();
			return;
		}

		Inventory clickedInv = event.getClickedInventory();

		if (Enchantments.hasEnchantment(cursorStack, CustomEnchantment.UnitItem) && clickedInv.getType() != InventoryType.PLAYER) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			((Player) event.getWhoClicked()).updateInventory();
			return;
		}

		CustomMaterial current = CustomMaterial.getCustomMaterial(currentStack);
		CustomMaterial cursor = CustomMaterial.getCustomMaterial(cursorStack);

		if (current != null) current.onInvItemPickup(event, clickedInv, currentStack);
		if (cursor != null) cursor.onInvItemDrop(event, clickedInv, cursorStack);
	}

	private void onInventoryClickMove(InventoryClickEvent event) {
		ItemStack currentStack = event.getCurrentItem();
		boolean currentEmpty = (currentStack == null) || currentStack.getType().equals(Material.AIR);
		if (currentEmpty) return;
		convertLegacyItem(event);

		if (Cooldown.isCooldown(currentStack)) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			((Player) event.getWhoClicked()).updateInventory();
			return;
		}

		InventoryView view = event.getView();

		Inventory clickedInv = event.getClickedInventory();
		Inventory otherInv = view.getBottomInventory();
		if (!view.getType().equals(InventoryType.CRAFTING)) {
			if (event.getRawSlot() == event.getSlot())
				otherInv = view.getBottomInventory();
			else
				otherInv = view.getTopInventory();
		}

		if (Enchantments.hasEnchantment(currentStack, CustomEnchantment.UnitItem) && otherInv.getType() != InventoryType.PLAYER) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			((Player) event.getWhoClicked()).updateInventory();
			return;
		}

		CustomMaterial current = CustomMaterial.getCustomMaterial(currentStack);

		if (current != null) {
			current.onInvItemPickup(event, clickedInv, currentStack);
			current.onInvItemDrop(event, otherInv, currentStack);
		}
	}

	private void onInventoryClickHotbar(InventoryClickEvent event) {
		Inventory playerInventory = event.getWhoClicked().getInventory();
		ItemStack firstStack = event.getCurrentItem();
		ItemStack secondStack = playerInventory.getItem(event.getHotbarButton());
		boolean firstEmpty = (firstStack == null) || firstStack.getType().equals(Material.AIR);
		boolean secondEmpty = (secondStack == null) || secondStack.getType().equals(Material.AIR);
		if (firstEmpty && secondEmpty) return;
		convertLegacyItem(event);

		if (Cooldown.isCooldown(firstStack) || Cooldown.isCooldown(firstStack)) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			((Player) event.getWhoClicked()).updateInventory();
			return;
		}

		Inventory clickedInv = event.getClickedInventory();
		Inventory otherInv = playerInventory;

		if (Enchantments.hasEnchantment(firstStack, CustomEnchantment.UnitItem) && otherInv.getType() != InventoryType.PLAYER) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			((Player) event.getWhoClicked()).updateInventory();
			return;
		}

		if (Enchantments.hasEnchantment(secondStack, CustomEnchantment.UnitItem) && clickedInv.getType() != InventoryType.PLAYER) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			((Player) event.getWhoClicked()).updateInventory();
			return;
		}

		CustomMaterial first = CustomMaterial.getCustomMaterial(firstStack);
		CustomMaterial second = CustomMaterial.getCustomMaterial(secondStack);

		if (first != null) {
			first.onInvItemPickup(event, clickedInv, firstStack);
			first.onInvItemDrop(event, otherInv, firstStack);
		}
		if (second != null) {
			second.onInvItemPickup(event, otherInv, secondStack);
			second.onInvItemDrop(event, clickedInv, secondStack);
		}
	}

	private void onInventoryClickDrop(InventoryClickEvent event) {
		ItemStack currentStack = event.getCurrentItem();
		boolean currentEmpty = (currentStack == null) || currentStack.getType().equals(Material.AIR);
		if (currentEmpty) return;
		convertLegacyItem(event);

		CustomMaterial current = CustomMaterial.getCustomMaterial(currentStack);
		Inventory clickedInv = event.getInventory();

		if (current != null) current.onInvItemPickup(event, clickedInv, currentStack);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.isCancelled()) return;

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
		case COLLECT_TO_CURSOR: // TODO Тут наверно надо обработку для предметов у которых ограничено максимальное количество в стаке
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
	public void onInventoryMoveItem(InventoryMoveItemEvent event) {
		// TODO это перемещение с помощью воронки. Оно необрабатывается. А надо бы.
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryDrag(InventoryDragEvent event) {
		if (event.isCancelled()) return;
		ItemStack stack = event.getOldCursor();
		if ((stack == null) || stack.getType().equals(Material.AIR)) return;

		Integer[] iSlots = event.getInventorySlots().toArray(new Integer[0]);
		Integer[] rSlots = event.getRawSlots().toArray(new Integer[0]);

		InventoryView view = event.getView();
		Inventory inv = view.getBottomInventory();
		for (int j = 0; j < iSlots.length; j++) {
			if (event.isCancelled()) return;
			if (!view.getType().equals(InventoryType.CRAFTING)) {
				if (rSlots[j] == iSlots[j]) // Clicked in the top holder
					inv = view.getTopInventory();
				else
					inv = view.getBottomInventory();
			}

			if (Enchantments.hasEnchantment(stack, CustomEnchantment.UnitItem) && inv.getType() != InventoryType.PLAYER) {
				event.setCancelled(true);
				event.setResult(Result.DENY);
				((Player) event.getWhoClicked()).updateInventory();
				return;
			}

			CustomMaterial custMat = CustomMaterial.getCustomMaterial(stack);
			if (custMat != null) custMat.onInvItemDrag(event, inv, stack);
		}

	}
	// ---------------Inventory end

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(event.getPlayer().getInventory().getItemInMainHand());
		if (craftMat == null) return;
		craftMat.onPlayerInteractEntityEvent(event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerLeashEvent(PlayerLeashEntityEvent event) {
		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(event.getPlayer().getInventory().getItemInMainHand());
		if (craftMat == null) return;
		craftMat.onPlayerLeashEvent(event);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onItemDurabilityChange(PlayerItemDamageEvent event) {
		ItemStack stack = event.getItem();

		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat == null) return;
		craftMat.onItemDurabilityChange(event);
	}

	private static boolean isUnwantedVanillaItem(final ItemStack stack) {
		if (stack == null) return false;
		final CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
		if (craftMat != null) return false;
		if (LoreGuiItem.isGUIItem(stack)) return false;

		return CivSettings.removedRecipies.contains(stack.getType()) || stack.getType().equals(Material.ENCHANTED_BOOK);
	}

	public static void removeUnwantedVanillaItems(Player player, Inventory inv) {
		if (player.isOp()) return; /* Allow OP to carry vanilla stuff. */
		boolean sentMessage = false;

		for (ItemStack stack : inv.getContents()) {
			if (!isUnwantedVanillaItem(stack)) continue;
			inv.remove(stack);
			if (player != null) CivLog.info("Removed vanilla item:" + stack + " from " + player.getName());
			if (!sentMessage) {
				if (player != null) CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("customItem_restrictedItemsRemoved"));
				sentMessage = true;
			}
		}

		/* Also check the player's equipt. */
		if (player != null) {
			ItemStack[] contents = player.getEquipment().getArmorContents();
			boolean foundBad = false;
			for (int i = 0; i < contents.length; i++) {
				ItemStack stack = contents[i];
				if (stack == null) continue;
				CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
				if (craftMat != null) continue; /* Assume we are good if we are custom. */

				if (!CivSettings.removedRecipies.contains(stack.getType()) && !stack.getType().equals(Material.ENCHANTED_BOOK)) continue; /* Not in removed list, so allow it. */

				CivLog.info("Removed vanilla item:" + stack + " from " + player.getName() + " from armor.");
				contents[i] = new ItemStack(Material.AIR);
				foundBad = true;
				if (!sentMessage) {
					CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("customItem_restrictedItemsRemoved"));
					sentMessage = true;
				}
			}
			if (foundBad) player.getEquipment().setArmorContents(contents);
		}
		if (sentMessage) if (player != null) player.updateInventory();
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnInventoryHold(PlayerItemHeldEvent event) {
		ItemStack stack = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if (stack == null) return;
		CustomMaterial mat = CustomMaterial.getCustomMaterial(stack);
		if (mat == null) return;
		mat.onHold(event);
	}

	// /* Prevent books from being inside an inventory. */
	/* Prevent vanilla gear from being used. */
	/* @EventHandler(priority = EventPriority.LOWEST) public void OnInventoryOpenRemove(InventoryOpenEvent event) {
	 * //CivLog.debug("open event."); if (event.getPlayer() instanceof Player) { //for (ItemStack stack : event.getInventory()) { for (int i =
	 * 0; i < event.getInventory().getSize(); i++) { ItemStack stack = event.getInventory().getItem(i); //CivLog.debug("stack cleanup");
	 * AttributeUtil attrs = ItemCleanup(stack); if (attrs != null) { event.getInventory().setItem(i, attrs.getStack()); } } } } */

	/* @EventHandler(priority = EventPriority.LOW) public void onPlayerLogin(PlayerLoginEvent event) { class SyncTask implements Runnable {
	 * String playerName; public SyncTask(String name) { playerName = name; }
	 * @Override public void run() { try { Player player = CivGlobal.getPlayer(playerName); for (int i = 0; i < player.getInventory().getSize();
	 * i++) { ItemStack stack = player.getInventory().getItem(i); AttributeUtil attrs = ItemCleanup(stack); if (attrs != null) {
	 * player.getInventory().setItem(i, attrs.getStack()); } } ItemStack[] contents = new
	 * ItemStack[player.getInventory().getArmorContents().length]; for (int i = 0; i < player.getInventory().getArmorContents().length; i++) {
	 * ItemStack stack = player.getInventory().getArmorContents()[i]; AttributeUtil attrs = ItemCleanup(stack); if (attrs != null) { contents[i]
	 * = attrs.getStack(); } else { contents[i] = stack; } } player.getInventory().setArmorContents(contents); } catch (CivException e) {
	 * return; } } } TaskMaster.syncTask(new SyncTask(event.getPlayer().getName())); } */

	@EventHandler(priority = EventPriority.LOWEST)
	public void OnInventoryClickEvent(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			removeUnwantedVanillaItems((Player) event.getWhoClicked(), event.getView().getBottomInventory());
		}
	}

	/* Called when we click on an object, used for conversion to fix up reverse compat problems. */
	public void convertLegacyItem(InventoryClickEvent event) {
		ItemStack oldStack = event.getCurrentItem();
		if ((oldStack == null) || (oldStack.getType() == Material.AIR)) return;
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

}
