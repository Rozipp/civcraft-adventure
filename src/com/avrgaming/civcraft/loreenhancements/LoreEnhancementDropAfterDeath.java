
package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementDropAfterDeath
extends LoreEnhancement {
    @Override
    public AttributeUtil add(AttributeUtil attrs) {
        attrs.addEnhancement("LoreEnhancementBuyItem", null, null);
        attrs.addLore(CivColor.Red + this.getDisplayName());
        return attrs;
    }

    @Override
    public String getDisplayName() {
        return "Vanishing";
    }

    @Override
    public String serialize(ItemStack stack) {
        return "";
    }

    @Override
    public ItemStack deserialize(ItemStack stack, String data) {
        return stack;
    }
}
//TODO from furnex
//public class LoreEnhancementDropAfterDeath extends LoreEnhancement {
//	public AttributeUtil add(AttributeUtil attrs) {
//		attrs.addEnhancement("LoreEnhancementBuyItem", null, null);
//		attrs.addLore("&c" + getDisplayName());
//		return attrs;
//	}
//
//	public boolean onDeath(final PlayerDeathEvent event, final ItemStack stack) {
//		event.getDrops().remove(stack);
//		return false;
//	}
//	
//	@Override
//	public boolean onDroped(PlayerDropItemEvent event) {
//		if ((new AttributeUtil(event.getItemDrop().getItemStack())).hasEnhancement("LoreEnhancementUnitItem")) {
//			event.setCancelled(true);
//			return false;
//		}
//		return true;
//	}
//
//	public String getDisplayName() {
//		return "Если выкинуть или умереть, то предмет исчезнет.";
//	}
//
//	public String serialize(ItemStack stack) {
//		return "";
//	}
//
//	public ItemStack deserialize(ItemStack stack, String data) {
//		return stack;
//	}
//}


