package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.siege.Cannon;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class Build extends ItemComponent {

	public void onInteract(PlayerInteractEvent event) {
		String building = this.getString("building");
		Player player = event.getPlayer();
		switch (building) {
			case "Cannon" :
				buildCannon(player);
				break;
			default :
				break;
		}
		player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
	}

	private void buildCannon(Player player) {
		try {

			if (!War.isWarTime()) {
				throw new CivException(CivSettings.localize.localizedString("buildCannon_NotWar"));
			}

			Resident resident = CivGlobal.getResident(player);
			Cannon.newCannon(player);

			CivMessage.sendCiv(resident.getCiv(), CivSettings.localize.localizedString("var_buildCannon_Success", (player.getLocation().getBlockX()
					+ "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ())));
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
		}
	}

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET + CivColor.Gold + CivSettings.localize.localizedString("buildCannon_Lore1"));
		attrUtil.addLore(ChatColor.RESET + CivColor.Rose + CivSettings.localize.localizedString("itemLore_RightClickToUse"));
	}

}
