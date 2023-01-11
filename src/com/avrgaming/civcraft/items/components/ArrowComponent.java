package com.avrgaming.civcraft.items.components;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;

import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.units.Cooldown;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import gpl.AttributeUtil;

public class ArrowComponent extends ItemComponent {

	public static FixedMetadataValue arrow_fire1 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_fire1");
	public static FixedMetadataValue arrow_fire2 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_fire2");
	public static FixedMetadataValue arrow_fire3 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_fire3");
	public static FixedMetadataValue arrow_frost1 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_frost1");
	public static FixedMetadataValue arrow_frost2 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_frost2");
	public static FixedMetadataValue arrow_frost3 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_frost3");
	public static FixedMetadataValue arrow_poison1 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_potion1");
	public static FixedMetadataValue arrow_poison2 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_potion2");
	public static FixedMetadataValue arrow_poison3 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_potion3");
	public static FixedMetadataValue arrow_knockback1 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_knockback1");
	public static FixedMetadataValue arrow_knockback2 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_knockback2");
	public static FixedMetadataValue arrow_knockback3 = new FixedMetadataValue(CivCraft.getPlugin(), "arrow_knockback3");

	public static int foundShootingArrow(PlayerInventory inv) {
		{
			ItemStack stack = inv.getItemInMainHand();
			if (stack != null && stack.getType() != Material.AIR) {
				if (stack.getType() == Material.ARROW || stack.getType() == Material.TIPPED_ARROW) return inv.getHeldItemSlot();
			}
		}
		for (int i = 0; i < inv.getSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack == null || stack.getType() == Material.AIR) continue;
			if (stack.getType() == Material.ARROW || stack.getType() == Material.TIPPED_ARROW) return i;
		}
		return -1;
	}

	public static FixedMetadataValue getMetadata(ItemStack stack) {
		if (stack == null || stack.getType() == Material.AIR) return null;

		UnitCustomMaterial cm = CustomMaterial.getUnitCustomMaterial(stack);
		if (cm != null) {
			ItemComponent comp = cm.getComponent("ArrowComponent");
			if (comp != null) {
				switch (comp.getString("effect") + 1) {
				case "fire1":
					return arrow_fire1;
				case "fire2":
					return arrow_fire2;
				case "fire3":
					return arrow_fire3;
				case "frost1":
					return arrow_frost1;
				case "frost2":
					return arrow_frost2;
				case "frost3":
					return arrow_frost3;
				case "poison1":
					return arrow_poison1;
				case "poison2":
					return arrow_poison2;
				case "poison3":
					return arrow_poison3;
				case "knockback1":
					return arrow_knockback1;
				case "knockback2":
					return arrow_knockback2;
				case "knockback3":
					return arrow_knockback3;
				}
			}
		}
		return null;
	}

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		// Add generic attack damage of 0 to clear the default lore on item.
		attrs.addLore("Клик для выбора типа стрелы");
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
		event.setCancelled(true);

		Player player = event.getPlayer();
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;

		Cooldown cooldown = Cooldown.getCooldown(event.getItem());
		if (cooldown != null) {
			CivMessage.sendError(player, "Подождите " + cooldown.getTime() + " секунд");
			return;
		}

		UnitObject uo = CivGlobal.getUnitObject(resident.getUnitObjectId());
		if (uo == null) {
			CivMessage.send(player, "Юнит не найден");
			UnitStatic.removeChildrenItems(player);
			return;
		}

		GuiInventory.openGuiInventory(player, "ChoiceArrowComponent", null);
	}
}
