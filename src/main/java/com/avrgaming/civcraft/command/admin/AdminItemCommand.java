package com.avrgaming.civcraft.command.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.Commander;
import com.avrgaming.civcraft.command.CustomCommand;
import com.avrgaming.civcraft.command.MenuAbstractCommand;
import com.avrgaming.civcraft.command.taber.AbstractCashedTaber;
import com.avrgaming.civcraft.command.taber.ResidentInWorldTaber;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

import com.avrgaming.gpl.AttributeUtil;
import net.minecraft.server.v1_12_R1.NBTTagCompound;

public class AdminItemCommand extends MenuAbstractCommand {

	public AdminItemCommand(String perentCommand) {
		super(perentCommand);
		displayName = CivSettings.localize.localizedString("adcmd_item_cmdDesc");

		add(new CustomCommand("showAllNBT").withDescription(CivSettings.localize.localizedString("adcmd_item_enhanceDesc")).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack is = player.getEquipment().getItemInMainHand();
				CivMessage.send(player, "HashCode предмета в вашер руке " + is.hashCode());
				net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(is);
				if (nmsStack != null && nmsStack.getTag() != null) {
					NBTTagCompound compound = nmsStack.getTag();
					if (compound != null) CivMessage.send(player, compound.toString());
				}
			}
		}));
		add(new CustomCommand("setcivnbt").withDescription("[key] [value] - adds this key.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String key = Commander.getNamedString(args, 0, "key");
				String value = Commander.getNamedString(args, 1, "value");

				ItemStack inHand = player.getInventory().getItemInMainHand();
				if (inHand == null) throw new CivException("You must have an item in hand.");

				AttributeUtil attrs = new AttributeUtil(inHand);
				attrs.setCivCraftProperty(key, value);
				player.getInventory().setItemInMainHand(attrs.getStack());
				CivMessage.sendSuccess(player, "Set property.");
			}
		}));
		add(new CustomCommand("getcivnbt").withDescription("[key] - gets this key").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				String key = Commander.getNamedString(args, 0, "key");

				ItemStack inHand = player.getInventory().getItemInMainHand();
				if (inHand == null) throw new CivException("You must have an item in hand.");

				AttributeUtil attrs = new AttributeUtil(inHand);
				String value = attrs.getCivCraftProperty(key);
				CivMessage.sendSuccess(player, "property:  " + value);
			}
		}));
		add(new CustomCommand("getmid").withDescription("Gets the MID of this item.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack inHand = player.getInventory().getItemInMainHand();
				if (inHand == null) throw new CivException("You need an item in your hand.");
				CivMessage.send(player, "MID:  " + CustomMaterial.getMID(inHand));
			}
		}));
		add(new CustomCommand("getdura").withDescription("gets the durability of an item").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack inHand = player.getInventory().getItemInMainHand();
				CivMessage.send(player, "Durability: " + inHand.getDurability());
				CivMessage.send(player, "MaxDura: " + inHand.getType().getMaxDurability());
			}
		}));
		add(new CustomCommand("setdura").withDescription("sets the durability of an item").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				Integer dura = Commander.getNamedInteger(args, 0);
				ItemStack inHand = player.getInventory().getItemInMainHand();
				inHand.setDurability((short) dura.shortValue());
				CivMessage.send(player, "Set Durability: " + inHand.getDurability());
				CivMessage.send(player, "MaxDura: " + inHand.getType().getMaxDurability());
			}
		}));
		add(new CustomCommand("enhance").withDescription(CivSettings.localize.localizedString("adcmd_item_enhanceDesc")).withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (String enchName : EnchantmentCustom.allEnchantmentCustom.keySet()) {
					if (enchName.toLowerCase().startsWith(arg)) l.add(enchName);
				}
				return l;
			}
		}).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Player player = Commander.getPlayer(sender);
				ItemStack inHand = player.getInventory().getItemInMainHand();
				if (inHand == null || ItemManager.getTypeId(inHand) == CivData.AIR) throw new CivException(CivSettings.localize.localizedString("adcmd_item_enhanceNoItem"));

				if (args.length < 1) {
					CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_item_enhancementList"));
					String out = "";
					for (String enchName : EnchantmentCustom.allEnchantmentCustom.keySet()) {
						out += enchName + ", ";
					}
					CivMessage.send(sender, out);
					return;
				}

				String name = Commander.getNamedString(args, 0, "enchantname");
				EnchantmentCustom ench = EnchantmentCustom.allEnchantmentCustom.get(name);
				Integer level = null;
				try {
					level = Integer.parseInt(args[1]);
				} catch (Exception e) {}
				if (level == null) level = 1;
				Enchantments.addEnchantment(inHand, ench, level);
				player.getInventory().setItemInMainHand(inHand);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_item_enhanceSuccess", name));
				return;
			}
		}));
		add(new CustomCommand("give").withDescription(CivSettings.localize.localizedString("adcmd_item_giveDesc")).withTabCompleter(new AbstractCashedTaber() {
			@Override
			protected List<String> newTabList(String arg) {
				List<String> l = new ArrayList<>();
				for (CustomMaterial customMat : CustomMaterial.getAllCustomMaterial()) {
					String name = customMat.getId();
					if (name.toLowerCase().startsWith(arg)) l.add(name);
				}
				return l;
			}
		}).withTabCompleter(new ResidentInWorldTaber()).withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				Resident resident = Commander.getNamedResident(args, 0);
				String id = Commander.getNamedString(args, 1, CivSettings.localize.localizedString("adcmd_item_givePrompt") + " materials.yml");
				int amount = Commander.getNamedInteger(args, 2);
				Player player = CivGlobal.getPlayer(resident);
				CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(id);
				if (craftMat == null) throw new CivException(CivSettings.localize.localizedString("adcmd_item_giveInvalid") + id);
				ItemStack stack = CraftableCustomMaterial.spawn(craftMat);
				stack.setAmount(amount);
				HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
				for (ItemStack is : leftovers.values()) {
					player.getWorld().dropItem(player.getLocation(), is);
				}
				CivMessage.sendSuccess(player, CivSettings.localize.localizedString("adcmd_item_giveSuccess"));
			}
		}));
		add(new CustomCommand("matmap").withDescription("prints the materials map in console.").withExecutor(new CustomExecutor() {
			@Override
			public void run(CommandSender sender, Command cmd, String label, String[] args) throws CivException {
				CivMessage.send(sender, "Print map in console");
				for (CustomMaterial mat : CustomMaterial.getAllCustomMaterial()) {
					String mid = mat.getId();
					CivLog.info("material id: " + mid + " mat: " + mat);
				}
			}
		}));
	}
}
