package com.avrgaming.civcraft.listener;

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.CriticalEnchantment;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.items.ItemChangeResult;
import com.avrgaming.civcraft.items.components.ArrowComponent;
import com.avrgaming.civcraft.listener.armor.ArmorType;
import com.avrgaming.civcraft.main.*;
import com.avrgaming.civcraft.mythicmob.MobStatic;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.Cooldown;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.gpl.HorseModifier;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.entity.Arrow.PickupStatus;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class CustomItemListener extends SimpleListener {

    private static String isCustomDrop(ItemStack stack) {
        if (stack == null || ItemManager.getTypeId(stack) != 166) return null;
        if (GuiItem.isGUIItem(stack)) return null;
        return stack.getItemMeta().getDisplayName();
    }

    private static boolean isUnwantedVanillaItem(final ItemStack stack) {
        if (stack == null) return false;
        final CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
        if (craftMat != null) return false;
        if (GuiItem.isGUIItem(stack)) return false;

        return CivSettings.removedRecipies.contains(stack.getType()) || stack.getType().equals(Material.ENCHANTED_BOOK);
    }

    public static void removeUnwantedVanillaItems(Player player, Inventory inv) {
        if (player != null && player.isOp()) return; /* Allow OP to carry vanilla stuff. */
        boolean sentMessage = false;

        for (ItemStack stack : inv.getContents()) {
            if (!isUnwantedVanillaItem(stack)) continue;
            inv.remove(stack);
            if (player != null) CivLog.info("Removed vanilla item:" + stack + " from " + player.getName());
            if (!sentMessage) {
                if (player != null)
                    CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("customItem_restrictedItemsRemoved"));
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

                if (!CivSettings.removedRecipies.contains(stack.getType()) && !stack.getType().equals(Material.ENCHANTED_BOOK))
                    continue; /* Not in removed list, so allow it. */

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
    public void onBlockBreakSpawnItems(BlockBreakEvent event) {
        if (event.getBlock().getType().equals(Material.LAPIS_ORE)) {
            if (Enchantments.hasEnchantment(event.getPlayer().getInventory().getItemInMainHand(), EnchantmentCustom.SILK_TOUCH))
                return;
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
        if (event.getAction() == Action.PHYSICAL) return;
        ItemStack stack = (event.getHand() == EquipmentSlot.OFF_HAND) ? event.getPlayer().getInventory().getItemInOffHand() : event.getPlayer().getInventory().getItemInMainHand();
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
        if (material != null) material.onHeld(event);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void OnPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;
        ItemStack stack = event.getItemDrop().getItemStack();
        if (itemDrop(event, event.getPlayer(), stack)) {
            event.setCancelled(true);
            return;
        }
        String custom = isCustomDrop(stack);
        if (custom != null) {
            event.setCancelled(true);
        }

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

            int slot = ArrowComponent.foundShootingArrow(player.getInventory());
            if (slot == -1) {
//				event.setCancelled(true);
//				event.setProjectile(null);
            } else {
                Location loc = event.getEntity().getEyeLocation();
                Vector velocity = arrow.getVelocity();
                float speed = (float) velocity.length();
                Vector dir = event.getEntity().getEyeLocation().getDirection();

                ItemStack stack = player.getInventory().getItem(slot);
                FixedMetadataValue metadata = ArrowComponent.getMetadata(stack);
                if (metadata != null) {
                    TippedArrow tarrow = loc.getWorld().spawnArrow(loc.add(dir.multiply(2)), dir, speed, 0.0f, TippedArrow.class);
                    if (metadata.equals(ArrowComponent.arrow_fire1)) tarrow.setFireTicks(2000);
                    if (metadata.equals(ArrowComponent.arrow_fire2)) tarrow.setFireTicks(4000);
                    if (metadata.equals(ArrowComponent.arrow_fire3)) tarrow.setFireTicks(6000);
                    if (metadata.equals(ArrowComponent.arrow_knockback1)) tarrow.setKnockbackStrength(1);
                    if (metadata.equals(ArrowComponent.arrow_knockback2)) tarrow.setKnockbackStrength(2);
                    if (metadata.equals(ArrowComponent.arrow_knockback3)) tarrow.setKnockbackStrength(3);
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
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDefenseAndAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getDamager();
            if (arrow.getShooter() instanceof Player) attacker = (Player) arrow.getShooter();
        }

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
                        PotionEffect pe = null;
                        if (dd.equals(ArrowComponent.arrow_frost1)) pe = new PotionEffect(PotionEffectType.SLOW, 40, 1);
                        if (dd.equals(ArrowComponent.arrow_frost2)) pe = new PotionEffect(PotionEffectType.SLOW, 80, 1);
                        if (dd.equals(ArrowComponent.arrow_frost3))
                            pe = new PotionEffect(PotionEffectType.SLOW, 120, 1);
                        if (dd.equals(ArrowComponent.arrow_poison1))
                            pe = new PotionEffect(PotionEffectType.POISON, 40, 1);
                        if (dd.equals(ArrowComponent.arrow_poison2))
                            pe = new PotionEffect(PotionEffectType.POISON, 80, 1);
                        if (dd.equals(ArrowComponent.arrow_poison3))
                            pe = new PotionEffect(PotionEffectType.POISON, 120, 1);

                        if (pe != null) ((LivingEntity) event.getEntity()).addPotionEffect(pe);
                    }
                }
            }

            if (arrow.getShooter() instanceof Player) {
                attacker = (Player) arrow.getShooter();
                ItemStack inHand = attacker.getEquipment().getItemInMainHand();
                if (!CustomMaterial.getMID(inHand).contains("_bow"))
                    inHand = attacker.getEquipment().getItemInOffHand();

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
                    event.setDamage(afc.getFromTower().getDamage());
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

            if (Enchantments.hasEnchantment(attacker.getInventory().getItemInMainHand(), EnchantmentCustom.Critical) && CriticalEnchantment.randomCriticalAttack(attacker.getInventory().getItemInMainHand())) {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) event.getEntity();
                    final Player player = attacker;
                    TaskMaster.syncTask(() -> le.damage(baseDamage, player), 10);
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
                if (attacker != null)
                    CivMessage.sendErrorNoRepeat(attacker, "У вас не достаточно сил, что бы нанести урон");
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
                if (Enchantments.hasEnchantment(chestplate, EnchantmentCustom.Thorns)) {
                    le.damage(event.getDamage() * Enchantments.getLevelEnchantment(chestplate, EnchantmentCustom.Thorns), defendingPlayer);
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
        GuiInventory.clearInventoryStack(((Player) event.getPlayer()).getUniqueId());
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

    // ---------------Inventory begin

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        HashMap<Integer, ItemStack> noDrop = new HashMap<>();
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

            if (Enchantments.hasEnchantment(stack, EnchantmentCustom.SoulBound)) {
                event.getDrops().remove(stack);
                noDrop.put(i, stack);
            }
            if (Enchantments.hasEnchantment(stack, EnchantmentCustom.UnitItem)) {
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

            if (Enchantments.hasEnchantment(stack, EnchantmentCustom.SoulBound)) {
                event.getDrops().remove(stack);
                noDrop.put(i, stack);
            }
            if (Enchantments.hasEnchantment(stack, EnchantmentCustom.UnitItem)) {
                event.getDrops().remove(stack);
            }

        }

        // event.getEntity().getInventory().getArmorContents()
        class SyncRestoreItemsTask implements Runnable {
            final HashMap<Integer, ItemStack> restore;
            final String playerName;
            final ItemStack[] armorContents;

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
                }
            }

        }

        boolean keepInventory = Boolean.parseBoolean(event.getEntity().getWorld().getGameRuleValue("keepInventory"));
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
        LinkedList<ItemStack> removed = new LinkedList<>();
        for (ItemStack stack : event.getDrops()) {
            if (CivSettings.removedRecipies.contains(stack.getType())) {
                if (!CustomMaterial.isCustomMaterial(stack)) {
                    removed.add(stack);
                }
            }
        }

        event.getDrops().removeAll(removed);
    }

    private boolean itemToInventory(Cancellable event, Player player, Inventory inv, Inventory topInv, ItemStack stack) {
        boolean stackEmpty = (stack == null) || stack.getType().equals(Material.AIR);
        if (stackEmpty) return false;
        CivLog.debug("itemToInventory  " + inv.getName() + "   " + stack.getItemMeta().getDisplayName());
        GuiInventory gi = GuiInventory.getActiveGuiInventory(player.getUniqueId());
        if (gi != null && inv.equals(topInv)) return gi.onItemToInventory(event, player, inv, stack);
        if (Enchantments.hasEnchantment(stack, EnchantmentCustom.UnitItem) && inv.getType() != InventoryType.PLAYER) return true;
        CustomMaterial cMat = CustomMaterial.getCustomMaterial(stack);
        if (cMat != null) return cMat.onInvItemDrop(event, player, inv, stack);
        return false;
    }

    private boolean itemFromInventory(Cancellable event, Player player, Inventory inv, Inventory topInv, ItemStack stack) {
        boolean stackEmpty = (stack == null) || stack.getType().equals(Material.AIR);
        if (stackEmpty) return false;
        CivLog.debug("itemFromInventory  " + inv.getName() + "   " + stack.getItemMeta().getDisplayName());
        if (Cooldown.isCooldown(stack)) return true;
        GuiInventory gi = GuiInventory.getActiveGuiInventory(player.getUniqueId());
        if (gi != null && inv.equals(topInv)) return gi.onItemFromInventory(event, player, inv, stack);
        if (Enchantments.hasEnchantment(stack, EnchantmentCustom.UnitItem) && inv.getType() != InventoryType.PLAYER) return true;
        CustomMaterial cMat = CustomMaterial.getCustomMaterial(stack);
        if (cMat != null) return cMat.onInvItemPickup(event, player, inv, stack);
        return false;
    }

    private boolean itemDrop(Cancellable event, Player player, ItemStack stack) {
        boolean stackEmpty = (stack == null) || stack.getType().equals(Material.AIR);
        if (stackEmpty) return false;
        CivLog.debug("itemDrop  " + stack.toString());
        if (Cooldown.isCooldown(stack)) return true;
        GuiInventory gi = GuiInventory.getActiveGuiInventory(player.getUniqueId());
        if (gi != null && GuiItem.isGUIItem(stack)) return true;
        if (Enchantments.hasEnchantment(stack, EnchantmentCustom.UnitItem)) return true;
        CustomMaterial cMat = CustomMaterial.getCustomMaterial(stack);
        if (cMat != null) return cMat.onDropItem(player, stack);
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clickStack = event.getCurrentItem();
        Inventory clickInv = event.getClickedInventory();
        InventoryAction ia = event.getAction();
        Inventory topInv = event.getView().getTopInventory();

        ItemStack otherStack;
        Inventory otherInv;

        boolean result = false;
        switch (ia) {
            case DROP_ALL_CURSOR:
            case DROP_ONE_CURSOR:
                otherStack = event.getCursor();
                result = itemDrop(event, player, otherStack);
                break;
            case PICKUP_ALL: // Взять стак
            case PICKUP_HALF:// Взять пол стака
            case PICKUP_ONE: // Взять один
            case PICKUP_SOME://
            case COLLECT_TO_CURSOR: // Взять все что найду двойным кликом //TODO Отменить это опасное действие
                result = itemFromInventory(event, player, clickInv, topInv, clickStack);
                break;
            case DROP_ALL_SLOT:
            case DROP_ONE_SLOT:  // выбросить предмет
                if (result = itemFromInventory(event, player, clickInv, topInv, clickStack)) break;
                if (result = itemDrop(event, player, clickStack)) break;
                break;
            case PLACE_ALL:  //
            case PLACE_ONE:  // положить
            case PLACE_SOME: //
                otherStack = event.getCursor();
                if (result = itemToInventory(event, player, clickInv, topInv, otherStack)) break;
                break;
            case SWAP_WITH_CURSOR: // меняем то что в руке на то что в инвентаре
                otherStack = event.getCursor();
                if (result = itemFromInventory(event, player, clickInv, topInv, clickStack)) break;
                if (result = itemToInventory(event, player, clickInv, topInv, otherStack)) break;
                break;
            case MOVE_TO_OTHER_INVENTORY:
                InventoryView view = event.getView();
                otherInv = view.getBottomInventory();
                if (!view.getType().equals(InventoryType.CRAFTING)) {
                    if (event.getRawSlot() == event.getSlot())
                        otherInv = view.getBottomInventory();
                    else
                        otherInv = view.getTopInventory();
                }
                if (result = itemFromInventory(event, player, clickInv, topInv, clickStack)) break;
                if (result = itemToInventory(event, player, otherInv, topInv, clickStack)) break;
                break;
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
                otherInv = event.getWhoClicked().getInventory();
                otherStack = otherInv.getItem(event.getHotbarButton());
                if (result = itemFromInventory(event, player, clickInv, topInv, clickStack)) break;
                if (result = itemToInventory(event, player, otherInv, topInv, clickStack)) break;
                if (result = itemFromInventory(event, player, otherInv, topInv, otherStack)) break;
                if (result = itemToInventory(event, player, clickInv, topInv, otherStack)) break;
                break;
            default:
                break;
        }
        if (result) {
            event.setCancelled(true);
            event.setResult(Result.DENY);
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        //TODO это перемещение с помощью воронки. Оно необрабатывается. А надо бы.
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack stack = event.getOldCursor();
        if ((stack == null) || stack.getType().equals(Material.AIR)) return;

        Integer[] iSlots = event.getInventorySlots().toArray(new Integer[0]);
        Integer[] rSlots = event.getRawSlots().toArray(new Integer[0]);

        InventoryView view = event.getView();
        Inventory inv = view.getBottomInventory();
        for (int j = 0; j < iSlots.length; j++) {
            if (!view.getType().equals(InventoryType.CRAFTING)) {
                if (rSlots[j].equals(iSlots[j])) // Clicked in the top holder
                    inv = view.getTopInventory();
                else
                    inv = view.getBottomInventory();
            }

            if (itemToInventory(event, player, inv, view.getTopInventory(), stack)) {
                event.setCancelled(true);
                event.setResult(Result.DENY);
                player.updateInventory();
                return;
            }
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void OnInventoryHold(PlayerItemHeldEvent event) {
        ItemStack stack = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (stack == null) return;
        CustomMaterial mat = CustomMaterial.getCustomMaterial(stack);
        if (mat == null) return;
        mat.onHeld(event);
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
    public ItemStack convertLegacyItem(ItemStack oldStack) {
        if ((oldStack == null) || (oldStack.getType() == Material.AIR)) return oldStack;
        CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(oldStack);
        if (craftMat == null) {
            ItemStack newStack = getConvertLegacyItem(oldStack);
            newStack.setAmount(oldStack.getAmount());
            return newStack;
        }
        return oldStack;
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
