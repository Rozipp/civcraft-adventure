package com.avrgaming.civcraft.enchantment;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.TimeTools;

public class LightningStrikeEnchantment extends EnchantmentCustom {

	public LightningStrikeEnchantment(int i) {
		super(i, "light_strike", "LightStrike", ItemSet.WEAPONS, 1, null);
	}

	public static void onAttack(EntityDamageByEntityEvent event) {
		if(event.isCancelled() && CivCraft.civRandom.nextInt(1000000) > 55000) {
			if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof ArmorStand) && !(event.getEntity() instanceof Slime)) {
				LivingEntity toDamage = (LivingEntity) event.getEntity();
				toDamage.getWorld().strikeLightningEffect(toDamage.getLocation());
				if (toDamage.getHealth() - 6.0 > 0.0) {
					toDamage.setHealth(toDamage.getHealth() - 6.0);
					toDamage.setFireTicks((int) TimeTools.toTicks(3L));
					event.setDamage(CivCraft.minDamage);
				} else {
					toDamage.setHealth(0.1);
					event.setDamage(1.0);
				}
				Object[] arrobject = new Object[2];
				arrobject[0] = toDamage;
				arrobject[1] = !(toDamage instanceof Player) ? CivColor.RoseBold + CivColor.ITALIC + "Player " + CivColor.RESET + CivColor.LightBlueItalic : "";
				CivMessage.send((Object) event.getDamager(), CivColor.LightBlueItalic + CivSettings.localize.localizedString("loreEnh_LightStrike_Sucusses", arrobject));
				if (toDamage instanceof Player) CivMessage.send((Object) event.getEntity(), CivSettings.localize.localizedString("loreEnh_LightStrike_Sucusses2", event.getDamager().getName()));
			} else {
				CivMessage.send((Object) event.getDamager(), CivColor.LightBlueItalic + CivSettings.localize.localizedString("loreEnh_LightStrike_warning"));
			}
		}
	}
}
