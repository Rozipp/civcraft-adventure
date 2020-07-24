package com.avrgaming.civcraft.gui.guiinventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.gui.GuiInventory;
import com.avrgaming.civcraft.gui.GuiItems;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.items.components.ItemComponent;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.units.ConfigUnitComponent;
import com.avrgaming.civcraft.units.Cooldown;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

import gpl.AttributeUtil;

public class ChoiceArrowComponent extends GuiInventory {

	public ChoiceArrowComponent(Player player, String arg) throws CivException {
		super(player, player, arg);
		if (getResident() == null) return;

		this.setTitle("Выберите тип стрелы");
		this.setRow(1);

		UnitObject uo = CivGlobal.getUnitObject(getResident().getUnitObjectId());
		if (uo == null) return;

		UnitCustomMaterial ucm = CustomMaterial.getUnitCustomMaterial("u_arrow_normal");
		this.addGuiItem(0, GuiItems.newGuiItem(CustomMaterial.spawn(ucm))//
				.setTitle(ucm.getName())//
				.setLore(CivColor.LightGreen + "Клик для выбора стрелы")//
				.setCallbackGui("u_arrow_normal"));

		int i = 1;
		for (String idComponent : uo.getConfigUnit().enable_components) {
			if (!idComponent.contains("u_arrow")) continue;
			ConfigUnitComponent comp = UnitStatic.configUnitComponents.get(idComponent);
			ItemStack itemStack = ItemManager.createItemStack(Material.TIPPED_ARROW, 1);
			AttributeUtil attrs = new AttributeUtil(itemStack);
			try {
				ucm = CustomMaterial.getUnitCustomMaterial(idComponent);
				ItemComponent icomp = ucm.getComponent("ArrowComponent");
				if (icomp != null) {
					switch (icomp.getString("effect")) {
					case "fire":
						attrs.setNBT("Potion", "minecraft:fire_resistance");
						break;
					case "frost":
						attrs.setNBT("Potion", "minecraft:water_breathing");
						break;
					case "poison":
						attrs.setNBT("Potion", "minecraft:slowness");
						break;
					case "knockback":
						attrs.setNBT("Potion", "minecraft:invisibility");
						break;
					}
				}
			} catch (Exception e) {
				CivLog.error("Not item found: " + idComponent);
				continue;
			}
			this.addGuiItem(++i, GuiItems.newGuiItem(attrs.getStack())//
					.setTitle(comp.name)//
					.setLore(CivColor.LightGreen + "Клик для выбора стрелы")//
					.setCallbackGui(comp.id));
		}
	}

	@Override
	public void execute(String... strings) {
		String mid = strings[0];
		ItemStack stack = ItemManager.createItemStack(mid, 1);
		Enchantments.addEnchantment(stack, EnchantmentCustom.UnitItem, 1);

		getPlayer().getInventory().setItemInMainHand(stack);

		Cooldown.startCooldown(getPlayer(), stack, 20, null);
		GuiInventory.closeInventory(getPlayer());
	}

}
