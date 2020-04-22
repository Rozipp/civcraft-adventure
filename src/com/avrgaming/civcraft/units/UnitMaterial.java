/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.units;

import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.tasks.DelayMoveInventoryItem;
import com.avrgaming.civcraft.util.CivColor;
import gpl.AttributeUtil;

/** Клас предметов которые выступают в качестве юнитов */
public abstract class UnitMaterial extends CustomMaterial {

	private ConfigUnit configUnit = null;
	public static final int LAST_SLOT = 8;
	public static HashMap<String, UnitMaterial> unitMaterials = new HashMap<>();

	// =============== EquipmentElemen
	public HashMap<String, EquipmentElement> equipmentElemens = new HashMap<>();

	public abstract void initAmmunitions();

	public String getAmuntMatTir(String eq, Integer tir) {
		EquipmentElement e = equipmentElemens.get(eq);
		if (e == null)
			return "";
		return e.getMatTir(tir);
	}

	public Integer getSlot(String eq) {
		EquipmentElement e = equipmentElemens.get(eq);
		if (e == null)
			return 0;
		return e.getSlot();
	}

	// =============== init Unit
	public abstract void initLore(AttributeUtil attrs, UnitObject uo);

	public abstract void initUnitObject(UnitObject uo);

	public UnitMaterial(String id, ConfigUnit configUnit) {
		super(id, configUnit.item_id, (short) configUnit.item_data);
		this.configUnit = configUnit;
		// TODO initLore
		this.setName(configUnit.name);
		this.initAmmunitions();
	}

	public ConfigUnit getConfigUnit() {
		return configUnit;
	}

	public void addMaterial() {
		unitMaterials.put(this.getId(), this);
	}

	// ============== extends CustomMaterial @Override

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {
	}

	@Override
	public void onBlockInteract(PlayerInteractEvent event) {
	}

	@Override
	public void onBlockPlaced(BlockPlaceEvent event) {
		event.setCancelled(true);
		CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("unitMaterial_cannotPlace"));
	}

	@Override
	public void onHit(EntityDamageByEntityEvent event) {
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {
	}

	@Override
	public void onInteractEntity(PlayerInteractEntityEvent event) {
	}

	@Override
	public void onCraftItem(CraftItemEvent event) {
		CivMessage.sendError(event.getWhoClicked(), CivSettings.localize.localizedString("unitItem_cannotCraft"));
		event.setCancelled(true);
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		event.setCancelled(true);
		Player player = event.getPlayer();
		Resident resident = CivGlobal.getResident(player);

		if (resident.isUnitActive()) {
			// Деактивация юнита
			resident.setUnitObjectId(0);
			UnitStatic.removeChildrenItems(player);

			CivMessage.send(player, CivColor.LightGreenBold + "Юнит деактивирован");
		} else {
			// Активация юнита
			ItemStack unitItemStack = event.getItem();
			UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(unitItemStack));
			if (uo == null) {
				CivMessage.send(player, "Юнит не найден. Можно спокойно выбросить этото предмет на мусорку");
				return;
			}
			resident.setUnitObjectId(uo.getId());
			uo.dressAmmunitions(player);

			CivMessage.send(player, CivColor.LightGreen + CivColor.BOLD + "Юнит активирован ");
		}

		UnitStatic.updateUnitForPlaeyr(player);
		resident.calculateWalkingModifier(player);
		UnitStatic.setModifiedMovementSpeed(player);
		UnitStatic.setModifiedJumping(player);
	}

	@Override
	public void onDropItem(PlayerDropItemEvent event) {
		Player player = (Player) event.getPlayer();
		if (CivGlobal.getResident(player).isUnitActive()) {
			CivMessage.sendError(player, "сперва нужно отключить юнита");
			event.setCancelled(true);
			player.updateInventory();
			return;
		}
		this.onItemFromPlayer(player, event.getItemDrop().getItemStack());
	}

	@Override
	public void onPickupItem(EntityPickupItemEvent event) {
		// поднятие с земли предмета
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			Resident res = CivGlobal.getResident(player);
			if (res.isUnitActive()) {
				CivMessage.sendError(player,
						CivSettings.localize.localizedString("var_unitMaterial_errorHave", this.getConfigUnit().name));
				event.setCancelled(true);
				player.updateInventory();
				return;
			}
			ItemStack stack = event.getItem().getItemStack();
			UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack));
			if (uo == null) {
				CivMessage.sendErrorNoRepeat(player, "Юнит не найден.");
				event.setCancelled(true);
				player.updateInventory();
				return;
			}
			if (!uo.validateUnitUse(player, stack)) {
				CivMessage.sendErrorNoRepeat(player,
						CivSettings.localize.localizedString("unitMaterial_errorWrongCiv"));
				event.setCancelled(true);
				player.updateInventory();
				return;
			}
			// Prevent dropping in two unit materials.
			// Если у игрока уже есть юнит в инвентаре, то нового не ложим
			List<Integer> slots = UnitStatic.findAllUnits(player.getInventory());
			if (slots.size() != 1) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_unitMaterial_errorHave"));
				event.setCancelled(true);
				player.getInventory().remove(stack);
				player.getWorld().dropItem(player.getLocation().add(player.getLocation().getDirection().multiply(2)),
						stack);
				player.updateInventory();
				return;
			} else {
				Integer unitSlot = slots.get(0);
				if (unitSlot != LAST_SLOT)
					DelayMoveInventoryItem.beginTask(player, stack, LAST_SLOT);
				return;
			}
		} else {
			event.setCancelled(true);
		}
	}

	@Override
	public void onInvItemDrop(InventoryClickEvent event, Inventory toInv, ItemStack stack) {
		// Ложим предмет в инвентарь
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			if (!this.isCanUseInventoryTypes(toInv)) {
				CivMessage.sendError(player, "Нельзя использовать этот предмет в инвентаре " + toInv.getType());
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			}
		}
		if (toInv.getHolder() instanceof Player) {
			// A hack to make sure we are always moving the item to the player's inv.
			// A player inv is always on the bottom, toInv could be the 'crafting' inv
			// Меня этот хак удивил, но проверять его целесообразность не буду
			toInv = event.getView().getBottomInventory();
			Player player = (Player) toInv.getHolder();

			UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack));
			if (uo == null)
				CivLog.error("Не найден UnitObject");
			if (!uo.validateUnitUse(player, stack)) { // если ложим игроку то проверяем, может ли носить этот игрок
														// этого юнита
				CivMessage.sendError(player, CivSettings.localize.localizedString("unitMaterial_errorWrongCiv"));
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			}
			// Prevent dropping in two unit materials.
			// Если у игрока уже есть юнит в инвентаре, то нового не ложим
			ItemStack unit = UnitStatic.findUnit(player);
			if (unit != null) {
				if (!unit.equals(stack))
					CivMessage.sendError(player, CivSettings.localize.localizedString("var_unitMaterial_errorHave",
							unit.getItemMeta().getDisplayName()));
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			}
			if (event.getSlot() != LAST_SLOT)
				DelayMoveInventoryItem.beginTask(player, stack, LAST_SLOT);
			onItemToPlayer(player, stack);
		}
	}

	public void onInvDrag(InventoryDragEvent event, Inventory toInv, ItemStack stack) {
		// Ложим предмет в инвентарь
		if (event.isCancelled())
			return;
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player) event.getWhoClicked();
			if (!this.isCanUseInventoryTypes(toInv)) {
				CivMessage.sendError(player, "Нельзя использовать этот предмет в инвентаре " + toInv.getType());
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			}
		}
		if (toInv.getHolder() instanceof Player) {
			// A hack to make sure we are always moving the item to the player's inv.
			// A player inv is always on the bottom, toInv could be the 'crafting' inv
			// Меня этот хак удивил, но проверять его целесообразность не буду
			toInv = event.getView().getBottomInventory();
			Player player = (Player) toInv.getHolder();

			UnitObject uo = CivGlobal.getUnitObject(UnitStatic.getUnitIdNBTTag(stack));
			if (uo == null) {
				CivLog.error("Не найден UnitObject");
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			}
			// если ложим игроку то проверяем, может ли носить этот игрок этого юнита
			if (!uo.validateUnitUse(player, stack)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("unitMaterial_errorWrongCiv"));
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			}
			// Prevent dropping in two unit materials.
			// Если у игрока уже есть юнит в инвентаре, то нового не ложим
			List<Integer> slots = UnitStatic.findAllUnits(player.getInventory());
			if (slots.size() > 0) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_unitMaterial_errorHave"));
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			} else {
				int ff = (int) event.getInventorySlots().toArray()[0];
				if (event.getView().convertSlot(ff) != LAST_SLOT)
					DelayMoveInventoryItem.beginTask(player, stack, LAST_SLOT);
				onItemToPlayer(player, stack);
			}
		}
	}

	@Override
	public void onInvItemPickup(InventoryClickEvent event, Inventory fromInv, ItemStack stack) {
		// Забераем предмет из инвентаря
		if (event.isCancelled())
			return;

		if (fromInv.getHolder() instanceof Player) {
			Player player = (Player) fromInv.getHolder();
			if (CivGlobal.getResident(player).isUnitActive()) {
				CivMessage.sendError(player, "сперва нужно отключить юнита");
				event.setCancelled(true);
				event.setResult(Result.DENY);
				player.updateInventory();
				return;
			}
			onItemFromPlayer(player, stack);
		}
	}

	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
	}

	@Override
	public boolean onAttack(EntityDamageByEntityEvent event, ItemStack stack) {
		return false;
	}

	@Override
	public void onPlayerDeath(EntityDeathEvent event, ItemStack stack) {
	}

	@Override
	public boolean isCanUseInventoryTypes(Inventory inv) {
		switch (inv.getType()) {
		case CHEST:
		case DROPPER:
		case ENDER_CHEST:
		case HOPPER:
		case PLAYER:
		case SHULKER_BOX:
			return true;
		default:
			return false;
		}
	}

	@Override
	public void onInventoryClose(InventoryCloseEvent event) {
		ItemStack firstFoundStack = null;
		for (ItemStack stack : event.getPlayer().getInventory().getContents()) {
			if (stack == null)
				continue;

			if (CustomMaterial.getCustomMaterial(stack) instanceof UnitMaterial) {
				if (firstFoundStack == null) {
					firstFoundStack = stack;
				} else {// Одного нашли. Остальных выбросить
					event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), stack);
					event.getPlayer().getInventory().remove(stack);
				}
			}
		}
	}

	/* Called when a unit material is added to a player. */
	public void onItemToPlayer(Player player, ItemStack stack) {
	}

	/* Called when a unit material is removed from a player. */
	public void onItemFromPlayer(Player player, ItemStack stack) {
	}

}
