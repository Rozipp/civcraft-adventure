/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigEnchant;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.enchantment.CustomEnchantment;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class GreatLibrary extends Wonder {

	public GreatLibrary(String id, Town town) throws CivException {
		super(id, town);
	}

	public GreatLibrary(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}

	@Override
	public void onComplete() {
		addBuffs();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}

	@Override
	protected void removeBuffs() {
		this.removeBuffFromCiv(this.getCiv(), "buff_greatlibrary_extra_beakers");
		this.removeBuffFromTown(this.getTown(), "buff_greatlibrary_double_tax_beakers");
	}

	@Override
	protected void addBuffs() {
		this.addBuffToCiv(this.getCiv(), "buff_greatlibrary_extra_beakers");
		this.addBuffToTown(this.getTown(), "buff_greatlibrary_double_tax_beakers");
	}

	@Override
	public void updateSignText() {

		for (ConstructSign sign : getSigns()) {
			ConfigEnchant enchant;
			switch (sign.getAction().toLowerCase()) {
			case "0":
				enchant = CivSettings.enchants.get("ench_fire_aspect");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			case "1":
				enchant = CivSettings.enchants.get("ench_fire_protection");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			case "2":
				enchant = CivSettings.enchants.get("ench_flame");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			case "3":
				enchant = CivSettings.enchants.get("ench_punchout");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}

			sign.update();
		}
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		// int special_id = Integer.valueOf(sign.getAction());
		Resident resident = CivGlobal.getResident(player);

		if (resident == null) {
			return;
		}

		if (resident.getCiv() != this.getCiv()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_greatLibrary_nonMember", this.getCiv().getName()));
			return;
		}
		// XXX ПРоверка на лидера цивы
		// if (!this.getCiv().getLeaderGroup().hasMember(resident)) {
		// CivMessage.sendError(player, CivSettings.localize.localizedString("var_greatLibrary_onlyleader", this.getCiv().getName()));
		// return;
		// }

		ItemStack hand = player.getInventory().getItemInMainHand();
		ConfigEnchant configEnchant;

		switch (sign.getAction()) {
		case "0": /* fire aspect */
			if (!CustomEnchantment.FIRE_ASPECT.canEnchantItem(hand)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
				return;
			}

			configEnchant = CivSettings.enchants.get("ench_fire_aspect");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_library_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
				return;
			}

			resident.getTreasury().withdraw(configEnchant.cost);
			hand = Enchantments.addEnchantment(hand, CustomEnchantment.FIRE_ASPECT, 2);
			break;
		case "1": /* fire protection */
			if (!CustomEnchantment.PROTECTION_FIRE.canEnchantItem(hand)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
				return;
			}

			configEnchant = CivSettings.enchants.get("ench_fire_protection");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_library_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
				return;
			}

			resident.getTreasury().withdraw(configEnchant.cost);
			hand = Enchantments.addEnchantment(hand, CustomEnchantment.PROTECTION_FIRE, 3);
			break;
		case "2": /* flame */
			if (!CustomEnchantment.ARROW_FIRE.canEnchantItem(hand)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
				return;
			}

			configEnchant = CivSettings.enchants.get("ench_flame");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_library_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
				return;
			}

			resident.getTreasury().withdraw(configEnchant.cost);
			hand = Enchantments.addEnchantment(hand, CustomEnchantment.ARROW_FIRE, 1);
			break;
		case "3":
			switch (ItemManager.getTypeId(hand)) {
			case CivData.WOOD_PICKAXE:
			case CivData.STONE_PICKAXE:
			case CivData.IRON_PICKAXE:
			case CivData.DIAMOND_PICKAXE:
			case CivData.GOLD_PICKAXE:
				configEnchant = CivSettings.enchants.get("ench_punchout");

				if (!CustomMaterial.isCustomMaterial(hand)) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_nonEnchantable"));
					return;
				}

				if (Enchantments.hasEnchantment(hand, CustomEnchantment.Punchout)) {
					CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_hasEnchantment"));
					return;
				}

				if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
					CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_library_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
					return;
				}

				resident.getTreasury().withdraw(configEnchant.cost);
				Enchantments.addEnchantment(hand, CustomEnchantment.Punchout, 1);
				player.getInventory().setItemInMainHand(hand);
				break;
			default:
				CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
				return;
			}
			break;
		default:
			return;
		}
		player.getInventory().setItemInMainHand(hand);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("library_enchantment_success"));
	}

}
