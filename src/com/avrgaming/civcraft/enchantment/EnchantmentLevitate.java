package com.avrgaming.civcraft.enchantment;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.Levitate;

public class EnchantmentLevitate extends CustomEnchantment {

	public EnchantmentLevitate(int i) {
		super(i, "levitate", CivColor.LightGrayBold + CivSettings.localize.localizedString("itemLore_levitate"), ItemSet.BOOTS, 1, null);
	}

	public static void onAttack(EntityDamageByEntityEvent event) {
		Player player = null;
		Resident resident = null;

		if (!event.isCancelled() && event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof ArmorStand) && !(event.getEntity() instanceof Slime) && event.getEntity() instanceof Player
				&& !(resident = CivGlobal.getResident(player = (Player) event.getEntity())).isLevitateImmune()) {
			resident.addLevitateImmune();
			new Levitate(player, 3000L).start();
		}
	}
}
