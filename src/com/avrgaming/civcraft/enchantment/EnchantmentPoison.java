package com.avrgaming.civcraft.enchantment;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class EnchantmentPoison extends CustomEnchantment {

	public EnchantmentPoison(int i) {
		super(i, "poision", CivColor.LightGreenBold + "Ядовитое лезвие", ItemSet.WEAPONS, 1, null);
	}

	public static void onAttack(EntityDamageByEntityEvent event) {
		Player player = null;
		Resident resident = null;

		if (!event.isCancelled() && event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof ArmorStand) && !(event.getEntity() instanceof Slime)) {
			if (!(event.getEntity() instanceof Player)) {
				((LivingEntity) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 50, 1));
			} else {
				player = (Player) event.getEntity();
				resident = CivGlobal.getResident(player);
				if (!resident.isPoisonImmune()) {
					resident.addPosionImmune();
					player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 0));
				}
			}
		}
	}
}
