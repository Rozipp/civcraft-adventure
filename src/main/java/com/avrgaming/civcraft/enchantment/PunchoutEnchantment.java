package com.avrgaming.civcraft.enchantment;

import java.util.Random;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;

public class PunchoutEnchantment extends EnchantmentCustom {

	public PunchoutEnchantment(int i) {
		super(i, "punchout", CivSettings.localize.localizedString("itemLore_Punchout"), ItemSet.TOOLS, 1, null);
	}

	public static int onStructureBlockBreak(ConstructDamageBlock sb, int damage) {
		Random rand = new Random();

		if (damage <= 1) {
			if (rand.nextInt(100) <= 50) {
				damage += rand.nextInt(5) + 1;
			}
		}
		return damage;
	}

}