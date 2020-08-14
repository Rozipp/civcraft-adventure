/************************************************************************* AVRGAMING LLC __________________
 *
 * [2013] AVRGAMING LLC All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.construct.structures;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveRepairItem;
import com.avrgaming.civcraft.items.CraftableCustomMaterial;
import com.avrgaming.civcraft.items.components.RepairCost;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Barracks extends Structure {

	public Barracks(String id, Town town) throws CivException {
		super(id, town);
	}

	public Barracks(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public double getRepairCost() {
		return (int) (this.getCost() / 2) * (1 - CivSettings.getDoubleStructure("reducing_cost_of_repairing_fortifications"));
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;
		switch (sign.getAction()) {
		case "repair_item":
			repairItem(player, resident, event);
			break;
		}
	}

	private void repairItem(Player player, Resident resident, PlayerInteractEvent event) {
		try {
			ItemStack inHand = player.getInventory().getItemInMainHand();
			if (inHand == null || inHand.getType().equals(Material.AIR)) throw new CivException(CivSettings.localize.localizedString("barracks_repair_noItem"));
			if (Enchantments.hasEnchantment(inHand, EnchantmentCustom.NoRepair)) throw new CivException(CivSettings.localize.localizedString("barracks_repair_noRepairEnch"));
			if (inHand.getType().getMaxDurability() == 0) throw new CivException(CivSettings.localize.localizedString("barracks_repair_invalidItem"));
			if (inHand.getDurability() == 0) throw new CivException(CivSettings.localize.localizedString("barracks_repair_atFull"));
			CraftableCustomMaterial craftMat = CraftableCustomMaterial.getCraftableCustomMaterial(inHand);
			if (craftMat == null) throw new CivException(CivSettings.localize.localizedString("barracks_repair_irreperable"));
			try {
				double totalCost;
				if (craftMat.hasComponent("RepairCost")) {
					RepairCost repairCost = (RepairCost) craftMat.getComponent("RepairCost");
					totalCost = repairCost.getDouble("value");
				} else {
					double baseTierRepair = CivSettings.getDouble(CivSettings.structureConfig, "barracks.base_tier_repair");
					double tierDamp = CivSettings.getDouble(CivSettings.structureConfig, "barracks.tier_damp");
					double tierCost = Math.pow((craftMat.getConfigMaterial().tier), tierDamp);
					double fromTier = Math.pow(baseTierRepair, tierCost);
					totalCost = Math.round(fromTier + 0);
				}
				InteractiveRepairItem repairItem = new InteractiveRepairItem(totalCost, player.getName(), craftMat);
				repairItem.displayMessage();
				resident.setInteractiveMode(repairItem);
				return;
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException(CivSettings.localize.localizedString("internalException"));
			}
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
			event.setCancelled(true);
		}
	}

	public static void repairItemInHand(double cost, String playerName, CraftableCustomMaterial craftMat) {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		Resident resident = CivGlobal.getResident(player);
		if (!resident.getTreasury().hasEnough(cost)) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_barracks_repair_TooPoor", cost, CivSettings.CURRENCY_NAME));
			return;
		}

		CraftableCustomMaterial craftMatInHand = CraftableCustomMaterial.getCraftableCustomMaterial(player.getInventory().getItemInMainHand());
		if (!craftMatInHand.getConfigId().equals(craftMat.getConfigId())) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("barracks_repair_DifferentItem"));
			return;
		}
		resident.getTreasury().withdraw(cost);
		player.getInventory().getItemInMainHand().setDurability((short) 0);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("var_barracks_repair_Success", craftMat.getName(), cost, CivSettings.CURRENCY_NAME));
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		ConstructSign structSign;
		switch (sb.command) {
		case "/repair":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new ConstructSign(absCoord, this);
			structSign.setText("\n" + ChatColor.BOLD + ChatColor.UNDERLINE + CivSettings.localize.localizedString("barracks_sign_repairItem"));
			structSign.setDirection(sb.getData());
			structSign.setAction("repair_item");
			structSign.update();
			this.addConstructSign(structSign);
			break;

		}
	}
}
