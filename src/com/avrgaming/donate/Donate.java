package com.avrgaming.donate;

import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.ItemStackBuilder;
import gpl.AttributeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.muffincolor.DonateAPI;

import java.util.HashMap;

public class Donate implements CommandExecutor, Listener {

    private static Inventory inventory;
    private static HashMap<Integer, Integer> itemsCost = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        inventory = Bukkit.createInventory(null, 54,  ChatColor.YELLOW + "Донат меню. " + ChatColor.DARK_GRAY + "Ваш баланс: " + DonateAPI.getBalance(commandSender.getName()));
        generateMenu((Player) commandSender, inventory);
        Player player = (Player) commandSender;
        player.openInventory(inventory);
        Bukkit.getPluginManager().registerEvents(this, CivCraft.getPlugin());
        return true;
    }

    public static void generateMenu(Player player, Inventory inventory) {
        // ---
        itemsCost.put(0, 150);
        inventory.setItem(0, generateCivItem("mat_found_civ", 1, itemsCost.get(0)));
        itemsCost.put(1, 50);
        inventory.setItem(1, generateCivItem("mat_found_warcamp", 1, itemsCost.get(1)));
        itemsCost.put(2, 30);
        inventory.setItem(2, generateCivItem("mat_found_camp", 1, itemsCost.get(2)));
        itemsCost.put(3, 20);
        inventory.setItem(3, generateCivItem("mat_build_cannon", 1, itemsCost.get(3)));

        // ---
        itemsCost.put(5, 400);
        inventory.setItem(5, generateVanillaItem("200 000 монет", Material.GOLD_BLOCK, 1, itemsCost.get(5),0, null, 1));
        itemsCost.put(6, 280);
        inventory.setItem(6, generateVanillaItem("140 000 монет", Material.GOLD_INGOT, 1, itemsCost.get(6),0, null, 1));
        itemsCost.put(7, 100);
        inventory.setItem(7, generateVanillaItem("50 000 монет", Material.GOLD_PLATE, 1, itemsCost.get(7),0, null, 1));
        itemsCost.put(8, 40);
        inventory.setItem(8, generateVanillaItem("20 000 монет", Material.GOLD_NUGGET, 1, itemsCost.get(8),0, null, 1));

        // ---
        itemsCost.put(18, 300);
        inventory.setItem(18, generateVanillaItem("Маяк", Material.BEACON, 1, itemsCost.get(18),0, null, 1));
        itemsCost.put(19, 25);
        inventory.setItem(19, generateVanillaItem("Шалкер", Material.WHITE_SHULKER_BOX, 1, itemsCost.get(19),0, null, 1));
        itemsCost.put(20, 10);
        inventory.setItem(20, generateVanillaItem("Тайный сундук", Material.ENDER_CHEST, 1, itemsCost.get(19),0, null, 1));
        itemsCost.put(21, 8);
        inventory.setItem(21, generateVanillaItem("Воронка", Material.HOPPER, 1, itemsCost.get(21),0, null, 1));

        // ---
        itemsCost.put(23, 250);
        inventory.setItem(23, generateVanillaItem("Донат-кирка", Material.DIAMOND_PICKAXE, 1, itemsCost.get(23),0, Enchantment.LOOT_BONUS_BLOCKS, 3));
        itemsCost.put(24, 130);
        inventory.setItem(24, generateVanillaItem("Донат-кирка", Material.DIAMOND_PICKAXE, 1, itemsCost.get(24),0, Enchantment.LOOT_BONUS_BLOCKS, 2));
        itemsCost.put(25, 50);
        inventory.setItem(25, generateVanillaItem("Донат-кирка", Material.DIAMOND_PICKAXE, 1, itemsCost.get(25),0, Enchantment.LOOT_BONUS_BLOCKS, 1));
        itemsCost.put(26, 32);
        inventory.setItem(26, generateVanillaItem("Элитры", Material.ELYTRA, 1, itemsCost.get(26),0, null, 0));

        // ---
        itemsCost.put(36, 200);
        inventory.setItem(36, generateVanillaItem("Тотем", Material.TOTEM, 1, itemsCost.get(26),0, null, 0));
        itemsCost.put(37, 15);
        inventory.setItem(37, generateVanillaItem("Стейк", Material.COOKED_BEEF, 64, itemsCost.get(26),0, null, 0));
        itemsCost.put(38, 40);
        inventory.setItem(38, generateVanillaItem("Хлеб", Material.BREAD, 64, itemsCost.get(26),0, null, 0));
        itemsCost.put(39, 32);
        inventory.setItem(39, generateVanillaItem("Мороковка", Material.GOLDEN_CARROT, 1, itemsCost.get(26),0, null, 0));

        itemsCost.put(41, 32);
        inventory.setItem(41, generateVanillaItem("Опыт для юнита", Material.EXP_BOTTLE, 16, itemsCost.get(41),0, null, 0));
        itemsCost.put(42, 32);
        inventory.setItem(42, generateVanillaItem("Опыт для юнита", Material.EXP_BOTTLE, 32, itemsCost.get(42),0, null, 0));
        itemsCost.put(43, 32);
        inventory.setItem(43, generateVanillaItem("Опыт для юнита", Material.EXP_BOTTLE, 48, itemsCost.get(43),0, null, 0));
        itemsCost.put(44, 32);
        inventory.setItem(44, generateVanillaItem("Опыт для юнита", Material.EXP_BOTTLE, 64, itemsCost.get(44),0, null, 0));

        // ---
        itemsCost.put(4, 32);
        inventory.setItem(4, generateVanillaItem("Средневековый стиль", Material.BRICK, 1, itemsCost.get(26),0, null, 0));
        itemsCost.put(13, 32);
        inventory.setItem(13, generateVanillaItem("Арктический стиль", Material.ICE, 1, itemsCost.get(13),0, null, 0));
        itemsCost.put(22, 32);
        inventory.setItem(22, generateVanillaItem("Эльфийский стиль", Material.LEAVES, 1, itemsCost.get(22),0, null, 0));
        itemsCost.put(31, 32);
        inventory.setItem(31, generateVanillaItem("Атлантический стиль", Material.PRISMARINE, 1, itemsCost.get(31),0, null, 0));
        itemsCost.put(40, 32);
        inventory.setItem(40, generateVanillaItem("Турецкий стиль", Material.RED_SANDSTONE, 1, itemsCost.get(40),0, null, 0));
        itemsCost.put(49, 32);
        inventory.setItem(49, generateVanillaItem("Культийский стиль", Material.STAINED_CLAY, 1, itemsCost.get(49),11, null, 0));
    }

    private static ItemStack generateCivItem(String umid, int amount, int cost){
        AttributeUtil attributeUtil = new AttributeUtil(ItemManager.createItemStack(umid, amount));
        attributeUtil.addLore("Стоимость: " + cost);
        return attributeUtil.getStack();
    }

    private static ItemStack generateVanillaItem(String name, Material material, int amount, int cost, int data, Enchantment enchantment, int enchLevel){
        if(enchantment != null){
            return new ItemStackBuilder(material).withData(data).withName(name).withLore("Стоимость: " + cost).withEnchantment(enchantment, enchLevel).withAmount(amount).build();
        } else {
            return new ItemStackBuilder(material).withData(data).withName(name).withLore("Стоимость: " + cost).withAmount(amount).build();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(event.getInventory().equals(inventory)) {
            event.setCancelled(true);
            ItemStack itemStack = event.getCurrentItem();
            Player player = (Player) event.getWhoClicked();
            if (DonateAPI.takeMoney(player.getName(), itemsCost.get(event.getSlot()))) {
                if (CustomMaterial.isCustomMaterial(itemStack)) {
                    player.getInventory().addItem(ItemManager.createItemStack(CustomMaterial.getMID(itemStack), 1));
                } else {
                    player.getInventory().addItem(event.getCurrentItem());
                }
            }
        }
    }
}
