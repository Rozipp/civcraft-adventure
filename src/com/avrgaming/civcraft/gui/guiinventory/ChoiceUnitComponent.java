package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItem;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.units.ConfigUnitComponent;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class ChoiceUnitComponent extends GuiInventory {

	public ChoiceUnitComponent(Player player, String arg) throws CivException {
		super(player, arg);
		this.setTitle("Окно выбора новых способностей");

		UnitObject uo = CivGlobal.getUnitObject(getResident().getUnitObjectId());
		if (uo == null) {
			CivMessage.send(getResident(), "Юнит не найден");
			return;
		}

		for (String enable_component : uo.getConfigUnit().enable_components) {
			ConfigUnitComponent comp = UnitStatic.configUnitComponents.get(enable_component);
			if (comp == null) continue;

			GuiItem item = GuiItems.newGuiItem(ItemManager.createItemStack(comp.gui_item_id, 1));

			Boolean isRequire = true;
			for (String s : comp.gui_lore)
				item.addLore(CivColor.LightGray + s);

			// проверка на требуемый урвоень
			int oldValue = uo.getComponentValue(comp.id);
			item.setTitle(comp.name + " уровень " + (oldValue + 1));
			int neadLevel = comp.require_level + comp.require_level_upgrade * oldValue;
			if (neadLevel > uo.getLevel()) {
				item.addLore(CivColor.Red + "требуемый уровень: " + CivColor.RedBold + neadLevel);
				item.setMaterial(Material.OBSIDIAN);
				isRequire = false;
			}

			if (comp.require_component != null) { // проверка на требуемые компоненты
				for (String key : comp.require_component.keySet()) {
					if (uo.getComponentValue(key) < comp.require_component.get(key)) {
						String levelString = ((comp.require_component.get(key) > 1) ? (CivColor.Red + " уровня " + CivColor.RedBold + comp.require_component.get(key)) : "");
						item.addLore(CivColor.Red + "требуеться способность " + CivColor.RedBold + "\"" + UnitStatic.configUnitComponents.get(key).name + "\"" + levelString);
						item.setMaterial(Material.OBSIDIAN);
						isRequire = false;
					}
				}
			}

			if (comp.require_tech != null) { // проверка на изученые технологии
				String[] split = comp.require_tech.split(",");
				for (int i = split.length - 1; i >= 0; i--) {
					if (!uo.getTownOwner().getCiv().hasTechnologys(split[i])) {
						item.addLore(CivColor.Red + "изучите " + CivColor.RedBold + "\"" + split[i] + "\"");
						item.setMaterial(Material.OBSIDIAN);
						isRequire = false;
					}
				}
			}

			// проверка на максимальный уровень
			if (comp.max_upgrade <= oldValue) {
				item.addLore(CivColor.Red + "Достигнуто максимаьное улучшение").setMaterial(Material.DIAMOND_BLOCK);
				isRequire = false;
			}

			if (isRequire) {
				item.addLore(CivColor.LightGreen + "Клик для выбора компонента").setCallbackGui(comp.id);
			}
			this.addGuiItem(comp.gui_slot, item);
		}
	}

	@Override
	public void execute(String... strings) {
		try {
			UnitObject uo = CivGlobal.getUnitObject(getResident().getUnitObjectId());

			uo.addComponent(strings[0]);

			UnitStatic.updateUnitForPlaeyr(getPlayer());
			uo.removeLevelUp();
			uo.save();
			uo.rebuildUnitItem(getPlayer());
			GuiInventory.closeInventory(getPlayer());
		} catch (CivException e) {
			// TODO Автоматически созданный блок catch
			e.printStackTrace();
		}
	}
}
