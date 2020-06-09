package com.avrgaming.civcraft.items.components;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.units.ConfigUnitComponent;
import com.avrgaming.civcraft.units.Cooldown;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

import gpl.AttributeUtil;

public class Arrow extends ItemComponent implements CallbackInterface {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		// Add generic attack damage of 0 to clear the default lore on item.
		attrs.addLore("Клик для выбора типа стрелы");
		return;
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) return;
		event.setCancelled(true);
		
		Player player = event.getPlayer();
		
		Cooldown cooldown = Cooldown.getCooldown(event.getItem());
		if (cooldown != null) {
			CivMessage.sendError(player, "Подождите " + cooldown.getTime() + " секунд");
			return;
		}
		
		Resident resident = CivGlobal.getResident(player);
		int unitid = resident.getUnitObjectId();
		if (unitid <= 0) {
			CivMessage.send(player, "Юнит не найден");
			return;
		}
		Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54, "Окно выбора новых способностей");

		UnitObject uo = CivGlobal.getUnitObject(unitid);
		if (uo == null) {
			CivMessage.send(player, "Юнит не найден");
			return;
		}

		UnitCustomMaterial ucm = CustomMaterial.getUnitCustomMaterial("u_arrow_normal");
		ItemStack itemStack = CustomMaterial.spawn(ucm);
		AttributeUtil attrs = new AttributeUtil(itemStack);
		attrs.setCivCraftProperty("GUI", ucm.getName());
		attrs.setLore(CivColor.LightGreen + "Клик для выбора стрелы");
		attrs.setCivCraftProperty("GUI_ACTION", "CallbackGui");
		attrs.setCivCraftProperty("GUI_ACTION_DATA:" + "data", "u_arrow_normal");
		itemStack = attrs.getStack();

		ItemMeta im = itemStack.getItemMeta();
		im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		itemStack.setItemMeta(im);

		inv.addItem(itemStack);

		for (String idComponent : uo.getConfigUnit().enable_components) {
			if (!idComponent.contains("u_arrow")) continue;
			CivLog.debug("idComponent = " + idComponent);
			ConfigUnitComponent comp = UnitStatic.configUnitComponents.get(idComponent);

			itemStack = CustomMaterial.spawn(CustomMaterial.getUnitCustomMaterial(idComponent));
			attrs = new AttributeUtil(itemStack);
			attrs.setCivCraftProperty("GUI", comp.name);
			attrs.setName(comp.name);
			attrs.setLore(CivColor.LightGreen + "Клик для выбора стрелы");
			attrs.setCivCraftProperty("GUI_ACTION", "CallbackGui");
			attrs.setCivCraftProperty("GUI_ACTION_DATA:" + "data", comp.id);
			itemStack = attrs.getStack();

			inv.addItem(itemStack);
		}
		
		LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
		resident.setPendingCallback(this);
		player.openInventory(inv);
	}

	@Override
	public void execute(String... strings) {
		Player player = Bukkit.getPlayer(strings[0]);
		Resident resident = CivGlobal.getResident(player);
		
		String mid = strings[1];
		int slot = player.getInventory().getHeldItemSlot();
		ItemStack stack = ItemManager.createItemStack(mid, 1);
		stack = Enchantments.addEnchantment(stack, CustomEnchantment.UnitItem, 1);
		player.getInventory().setItem(slot, stack);
		
		Cooldown.startCooldown(player, stack, 20, null);
		
		resident.clearInteractiveMode();
		player.closeInventory();
	}
}
