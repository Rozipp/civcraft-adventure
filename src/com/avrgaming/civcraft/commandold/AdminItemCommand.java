package com.avrgaming.civcraft.commandold;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

public class AdminItemCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad item";
		displayName = CivSettings.localize.localizedString("adcmd_item_cmdDesc");

		cs.add("enhance", CivSettings.localize.localizedString("adcmd_item_enhanceDesc"));
		cs.add("give", CivSettings.localize.localizedString("adcmd_item_giveDesc"));
	}

	public void give_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		String id = getNamedString(2, CivSettings.localize.localizedString("adcmd_item_givePrompt") + " materials.yml");
		int amount = getNamedInteger(3);

		Player player = CivGlobal.getPlayer(resident);

		CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(id);
		if (craftMat == null) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_item_giveInvalid") + id);
		}

		ItemStack stack = CraftableCustomMaterial.spawn(craftMat);

		stack.setAmount(amount);
		HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
		for (ItemStack is : leftovers.values()) {
			player.getWorld().dropItem(player.getLocation(), is);
		}

		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("adcmd_item_giveSuccess"));
	}

	public void enhance_cmd() throws CivException {
		Player player = getPlayer();
		HashMap<String, EnchantmentCustom> enhancements = new HashMap<String, EnchantmentCustom>();
		ItemStack inHand = getPlayer().getInventory().getItemInMainHand();

		enhancements.put("soulbound", EnchantmentCustom.SoulBound);
		enhancements.put("attack", EnchantmentCustom.Attack);
		enhancements.put("defence", EnchantmentCustom.Defense);

		if (inHand == null || ItemManager.getTypeId(inHand) == CivData.AIR) {
			throw new CivException(CivSettings.localize.localizedString("adcmd_item_enhanceNoItem"));
		}

		if (args.length < 2) {
			CivMessage.sendHeading(sender, CivSettings.localize.localizedString("adcmd_item_enhancementList"));
			String out = "";
			for (String str : enhancements.keySet()) {
				out += str + ", ";
			}
			CivMessage.send(sender, out);
			return;
		}

		String name = getNamedString(1, "enchantname");
		name.toLowerCase();
		for (String str : enhancements.keySet()) {
			if (name.equals(str)) {
				EnchantmentCustom ench = enhancements.get(str);
				Enchantments.addEnchantment(inHand, ench, 1);
				player.getInventory().setItemInMainHand(inHand);
				CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_adcmd_item_enhanceSuccess", name));
				return;
			}
		}
	}

	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {

	}

}
