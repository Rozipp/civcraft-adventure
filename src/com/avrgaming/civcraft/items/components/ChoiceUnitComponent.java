package com.avrgaming.civcraft.items.components;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.loregui.OpenInventoryTask;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.ConfigUnitComponent;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CivColor;

import gpl.AttributeUtil;

public class ChoiceUnitComponent extends ItemComponent {
	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
	}

	public void onInteract(PlayerInteractEvent event) {

		Player player = event.getPlayer();
		Resident resident = CivGlobal.getResident(player);
		int unitid = resident.getUnitObjectId();
		if (unitid <= 0) {
			CivMessage.send(player, "Юнит не найден");
			event.setCancelled(true);
			return;
		}
		Inventory inv = Bukkit.getServer().createInventory((InventoryHolder) player, 54, "Окно выбора новых способностей");

		UnitObject uo = CivGlobal.getUnitObject(CivGlobal.getResident(player).getUnitObjectId());
		if (uo == null) {
			CivMessage.send(player, "Юнит не найден");
			return;
		}

		for (ConfigUnitComponent comp : UnitStatic.configUnitComponents.values()) {
			if (!uo.getConfigUnit().enable_components.contains(comp.id)) continue; // если не относиться к етому юниту то пропустить

			Boolean isRequire = true;
			int type = comp.gui_item_id;
			ArrayList<String> loreArray = new ArrayList<>();
			for (String s : comp.gui_lore)
				loreArray.add(CivColor.LightGray + s);

			//проверка на требуемый урвоень
			int oldValue = uo.getComponent(comp.id);
			int neadLevel = comp.require_level + comp.require_level_upgrade * oldValue;
			if (neadLevel > uo.getLevel()) { 
				loreArray.add(CivColor.Red + "требуемый уровень: " + CivColor.RedBold + neadLevel);
				type = CivData.OBSIDIAN;
				isRequire = false;
			}

			if (comp.require_component != null) { //проверка на требуемые компоненты
				for (String key : comp.require_component.keySet()) {
					if (uo.getComponent(key) < comp.require_component.get(key)) {
						String levelString = ((comp.require_component.get(key) > 1)
								? (CivColor.Red + " уровня " + CivColor.RedBold + comp.require_component.get(key))
								: "");
						loreArray.add(
								CivColor.Red + "требуеться способность " + CivColor.RedBold + "\"" + UnitStatic.configUnitComponents.get(key).name + "\"" + levelString);
						type = CivData.OBSIDIAN;
						isRequire = false;
					}
				}
			}

			if (comp.require_tech != null) { //проверка на изученые технологии
				String[] split = comp.require_tech.split(",");
				for (int i = split.length - 1; i >= 0; i--) {
					if (!uo.getTownOwner().getCiv().hasTechnology(split[i])) {
						loreArray.add(CivColor.Red + "изучите " + CivColor.RedBold + "\"" + split[i] + "\"");
						type = CivData.OBSIDIAN;
						isRequire = false;
					}
				}
			}

			// проверка на максимальный уровень
						if (comp.max_upgrade <= oldValue) {
							loreArray.add(CivColor.Red + "Достигнуто максимаьное улучшение");
							type = CivData.DIAMOND_BLOCK;
							isRequire = false;
						}
			
			if (isRequire) {
				loreArray.add(CivColor.LightGreen + "Клик для выбора компонента");
			}

			String[] lore = new String[loreArray.size()];
			int i = 0;
			for (String s : loreArray)
				lore[i++] = s;

			ItemStack itemStack = LoreGuiItem.build(comp.name + " уровень " + (oldValue + 1), type, 0, lore);
			if (isRequire) {
				itemStack = LoreGuiItem.setAction(itemStack, "ChoiceUnitComponent");
				itemStack = LoreGuiItem.setActionData(itemStack, "component", comp.id);
			}

			ItemMeta im = itemStack.getItemMeta();
			im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			itemStack.setItemMeta(im);
			inv.setItem(comp.gui_slot, itemStack);
		}

		LoreGuiItemListener.guiInventories.put(inv.getName(), inv);
		TaskMaster.syncTask(new OpenInventoryTask(player, inv));
	}

	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		event.setCancelled(true);
	}

	public void onPlayerLeashEvent(PlayerLeashEntityEvent event) {

	}
}
