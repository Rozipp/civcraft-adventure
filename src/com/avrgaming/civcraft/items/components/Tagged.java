package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.util.ItemManager;

public class Tagged extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
	}

	public ItemStack addTag(ItemStack src, String tag) {
		AttributeUtil attrs = new AttributeUtil(src);
		attrs.setCivCraftProperty("tag", tag);
		return attrs.getStack();
	}
	
	public String getTag(ItemStack src) {
		return ItemManager.getProperty(src,"tag");
	}
	
	public static String matrixHasSameTag(ItemStack[] matrix) {
		String tag = null;
		
		for (ItemStack stack : matrix) {
			if ((stack == null) || (ItemManager.getTypeId(stack) == CivData.AIR)) {
				continue;
			}
			
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(stack);
			if (craftMat == null) {
				return null;
			}
			
			Tagged tagged = (Tagged)craftMat.getComponent("Tagged");
			if (tagged == null) {
				return null;
			}
			
			if (tag == null) {
				tag = tagged.getTag(stack);
				continue;
			} else {
				if (!tagged.getTag(stack).equals(tag)) {
					return null;
				}
			}
		}
		
		return tag;
		
	}
}
