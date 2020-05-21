
package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigEnchant;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class MoscowStateUni extends Wonder {
	public MoscowStateUni(final String id, final Town town) throws CivException {
		super(id, town);
	}

	public MoscowStateUni(final ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public String getMarkerIconName() {
		return "beer";
	}

	@Override
	public void onLoad() {
		if (this.isActive()) {
			this.addBuffs();
		}
	}

	@Override
	public void onComplete() {
		this.addBuffs();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.removeBuffs();
	}

	@Override
	protected void removeBuffs() {
		this.removeBuffFromCiv(this.getCiv(), "buff_moscowstateuni_extra_beakers");
		this.removeBuffFromTown(this.getTown(), "buff_moscowstateuni_profit_sharing");
	}

	@Override
	protected void addBuffs() {
		this.addBuffToCiv(this.getCiv(), "buff_moscowstateuni_extra_beakers");
		this.addBuffToTown(this.getTown(), "buff_moscowstateuni_profit_sharing");
	}

	@Override
	public void updateSignText() {
		for (final ConstructSign sign : this.getSigns()) {
			final String lowerCase = sign.getAction().toLowerCase();
			switch (lowerCase) {
			case "0": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_infinity");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			case "1": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_fall_protection");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			case "2": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_oxygen");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			case "3": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_lightningstrike");
				sign.setText(enchant.name + "\n\n" + CivColor.LightGreen + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			}
			sign.update();
		}
	}

	@Override
	public void processSignAction(Player player, final ConstructSign sign, final PlayerInteractEvent event) {
//		final Resident resident = CivGlobal.getResident(player);
//		if (resident == null) {
//			return;
//		}
//		if (!resident.hasTown() || resident.getCiv() != this.getCiv()) {
//			CivMessage.sendError(player, CivSettings.localize.localizedString("var_moscowstateuni_nonMember", this.getCiv().getName()));
//			return;
//		}
//		if (!resident.hasTown() || resident.getCiv() != this.getCiv()) {
//			CivMessage.sendError(player, CivSettings.localize.localizedString("var_moscowstateuni_nonMember", this.getCiv().getName()));
//			return;
//		}
//		ItemStack hand = player.getInventory().getItemInMainHand();
//		switch (sign.getAction()) {
//		case "0": {
//			if (!Enchantment.ARROW_INFINITE.canEnchantItem(hand)) {
//				CivMessage.sendError(player, CivSettings.localize.localizedString("moscowstateuni_enchant_cannotEnchant"));
//				return;
//			}
//			if (hand.containsEnchantment(Enchantment.ARROW_INFINITE)) {
//				try {
//					throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
//				} catch (CivException e) {
//					e.printStackTrace();
//				}
//			}
//			final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_infinity");
//			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
//				CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_moscowstateuni_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
//				return;
//			}
//			resident.getTreasury().withdraw(configEnchant.cost);
//			hand.addEnchantment(Enchantment.ARROW_INFINITE, 1);
//			break;
//		}
//		case "1": {
//			if (!Enchantment.PROTECTION_FALL.canEnchantItem(hand)) {
//				CivMessage.sendError(player, CivSettings.localize.localizedString("moscowstateuni_enchant_cannotEnchant"));
//				return;
//			}
//			if (hand.containsEnchantment(Enchantment.PROTECTION_FALL)) {
//				try {
//					throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
//				} catch (CivException e) {
//					e.printStackTrace();
//				}
//			}
//			final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_fall_protection");
//			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
//				CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_moscowstateuni_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
//				return;
//			}
//			resident.getTreasury().withdraw(configEnchant.cost);
//			hand.addEnchantment(Enchantment.PROTECTION_FALL, 4);
//			break;
//		}
//		case "2": {
//			if (!Enchantment.OXYGEN.canEnchantItem(hand)) {
//				CivMessage.sendError(player, CivSettings.localize.localizedString("moscowstateuni_enchant_cannotEnchant"));
//				return;
//			}
//			if (hand.containsEnchantment(Enchantment.OXYGEN)) {
//				try {
//					throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
//				} catch (CivException e) {
//					e.printStackTrace();
//				}
//			}
//			final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_oxygen");
//			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
//				CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_moscowstateuni_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
//				return;
//			}
//			resident.getTreasury().withdraw(configEnchant.cost);
//			hand.addEnchantment(Enchantment.OXYGEN, 3);
//			break;
//		}
//		case "3": {
//			switch (ItemManager.getTypeId(hand)) {
//			case CivData.IRON_AXE:
//			case CivData.IRON_SWORD:
//			case CivData.WOOD_SWORD:
//			case CivData.WOOD_AXE:
//			case CivData.STONE_SWORD:
//			case CivData.STONE_AXE:
//			case CivData.DIAMOND_SWORD:
//			case CivData.DIAMOND_AXE:
//			case CivData.GOLD_SWORD:
//			case CivData.GOLD_AXE: {
//				final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_lightningstrike");
//				if (!CustomMaterial.isCustomMaterial(hand)) {
//					CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_nonEnchantable"));
//					return;
//				}
//				if (hand.getItemMeta().hasEnchant(CustomEnchantment.getByName(configEnchant.enchant_id))) {
//					CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_hasEnchantment"));
//					return;
//				}
//				if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
//					CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_moscowstateuni_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
//					return;
//				}
//				resident.getTreasury().withdraw(configEnchant.cost);
//				hand.addEnchantment(CustomEnchantment.getByName(configEnchant.enchant_id), 1);
//				player.getInventory().setItemInMainHand(hand);
//				break;
//			}
//			default: {
//				CivMessage.sendError(player, CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
//				return;
//			}
//			}
//		}
//		default: {
//			return;
//		}
//		}
//
//		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("moscowstateuni_enchantment_success"));
	}
}
