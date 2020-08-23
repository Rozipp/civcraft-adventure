package com.avrgaming.civcraft.construct.wonders;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigEnchant;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class Oracle extends Wonder {

	public Oracle(String id, Town town) {
		super(id, town);
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
		this.removeBuffFromCiv(this.getCivOwner(), "buff_oracle_extra_hp");
		this.removeBuffFromTown(this.getTownOwner(), "buff_oracle_double_tax_beakers");
	}

	@Override
	protected void addBuffs() {
		this.addBuffToCiv(this.getCivOwner(), "buff_oracle_extra_hp");
		this.addBuffToTown(this.getTownOwner(), "buff_oracle_double_tax_beakers");
	}

	@Override
	public void updateSignText() {
		for (final ConstructSign sign : this.getSigns()) {
			final String lowerCase = sign.getAction().toLowerCase();
			switch (lowerCase) {
			case "0": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_thorns");
				sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			case "1": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_walker");
				sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			case "2": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_fall");
				sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			case "3": {
				final ConfigEnchant enchant = CivSettings.enchants.get("ench_strider");
				sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
				break;
			}
			}
			sign.update();
		}
	}

	@Override
	public void processSignAction(final Player player, final ConstructSign sign, final PlayerInteractEvent event) {
		final Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;
		if (!resident.hasTown() || resident.getCiv() != this.getCivOwner()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_greatLibrary_nonMember", this.getCivOwner().getName()));
			return;
		}
		if (!this.getCivOwner().GM.isLeader(resident)) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_greatLibrary_onlyleader", this.getCivOwner().getName()));
			return;
		}
		ItemStack hand = player.getInventory().getItemInMainHand();
		final String action = sign.getAction();
		int n = -1;
		switch (action.hashCode()) {
		case 48:
			if (action.equals("0")) n = 0;
			break;
		case 49:
			if (action.equals("1")) n = 1;
			break;
		case 50:
			if (action.equals("2")) n = 2;
			break;
		case 51:
			if (action.equals("3")) n = 3;
			break;
		}
		switch (n) {
		case 0: {
			if (!EnchantmentCustom.THORNS.canEnchantItem(hand)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("oracle_enchant_cannotEnchant"));
				return;
			}
			if (Enchantments.hasEnchantment(hand, EnchantmentCustom.THORNS)) {
				try {
					throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
			final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_thorns");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
				return;
			}
			resident.getTreasury().withdraw(configEnchant.cost);
			Enchantments.addEnchantment(hand, EnchantmentCustom.THORNS, 2);
			break;
		}
		case 1: {
			if (!EnchantmentCustom.FROST_WALKER.canEnchantItem(hand)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("moscowstateuni_enchant_cannotEnchant"));
				return;
			}
			if (Enchantments.hasEnchantment(hand, EnchantmentCustom.FROST_WALKER)) {
				try {
					throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
			final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_walker");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
				return;
			}
			resident.getTreasury().withdraw(configEnchant.cost);
			Enchantments.addEnchantment(hand, EnchantmentCustom.FROST_WALKER, 1);
			break;
		}
		case 2: {
			if (!EnchantmentCustom.PROTECTION_FALL.canEnchantItem(hand)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("oracle_enchant_cannotEnchant"));
				return;
			}
			if (Enchantments.hasEnchantment(hand, EnchantmentCustom.PROTECTION_FALL)) {
				try {
					throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
			final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_fall");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
				return;
			}
			resident.getTreasury().withdraw(configEnchant.cost);
			Enchantments.addEnchantment(hand, EnchantmentCustom.PROTECTION_FALL, 2);
			break;
		}
		case 3: {
			if (!EnchantmentCustom.DEPTH_STRIDER.canEnchantItem(hand)) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("oracle_enchant_cannotEnchant"));
				return;
			}
			if (Enchantments.hasEnchantment(hand, EnchantmentCustom.DEPTH_STRIDER)) {
				try {
					throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
			final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_strider");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
				return;
			}
			resident.getTreasury().withdraw(configEnchant.cost);
			Enchantments.addEnchantment(hand, EnchantmentCustom.DEPTH_STRIDER, 2);
			break;
		}
		default: {
			return;
		}
		}
		player.getInventory().setItemInMainHand(hand);
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("library_enchantment_success"));
	}
	/* @Override public void updateSignText() { for (final StructureSign sign : this.getSigns()) { final String lowerCase =
	 * sign.getAction().toLowerCase(); switch (lowerCase) { case "0": { final ConfigEnchant enchant = CivSettings.enchants.get("ench_thorns");
	 * sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME); break; } case "1": { final ConfigEnchant
	 * enchant = CivSettings.enchants.get("ench_walker"); sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " +
	 * CivSettings.CURRENCY_NAME); break; } case "2": { final ConfigEnchant enchant = CivSettings.enchants.get("ench_fall");
	 * sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME); break; } case "3": { final ConfigEnchant
	 * enchant = CivSettings.enchants.get("ench_strider"); sign.setText(enchant.name + "\n\n\n" + "§a" + enchant.cost + " " +
	 * CivSettings.CURRENCY_NAME); break; } } sign.update(); } } */
}

/* @SuppressWarnings("unused")
 * @Override public void processSignAction(final Player player, final StructureSign sign, final PlayerInteractEvent event) { final Resident
 * resident = CivGlobal.getResident(player); if (resident == null) { return; } if (!resident.hasTown() || resident.getCiv() !=
 * this.getCiv()) { CivMessage.sendError(player, CivSettings.localize.localizedString("var_moscowstateuni_nonMember",
 * this.getCiv().getName())); return; } if (!resident.hasTown() || resident.getCiv() != this.getCiv()) { CivMessage.sendError(player,
 * CivSettings.localize.localizedString("var_moscowstateuni_nonMember", this.getCiv().getName())); return; } if
 * (!this.getCiv().getLeaderGroup().hasMember(resident)) { CivMessage.sendError(player,
 * CivSettings.localize.localizedString("var_moscowstateuni_onlyleader", this.getCiv().getName())); return; } ItemStack hand =
 * player.getInventory().getItemInMainHand(); final String action = sign.getAction(); int n = -1; switch (action.hashCode()) { case 48: { if
 * (action.equals("0")) { n = 0; break; } break; } case 49: { if (action.equals("1")) { n = 1; break; } break; } case 50: { if
 * (action.equals("2")) { n = 2; break; } break; } case 51: { if (action.equals("3")) { n = 3; break; } break; } } Label_1113: { switch (n)
 * { case 0: { if (!Enchantment.THORNS.canEnchantItem(hand)) { CivMessage.sendError(player,
 * CivSettings.localize.localizedString("oracle_enchant_cannotEnchant")); return; } if (hand.containsEnchantment(Enchantment.THORNS)) { try
 * { throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant")); } catch (CivException e) {
 * e.printStackTrace(); } } final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_thorns"); if
 * (!resident.getTreasury().hasEnough(configEnchant.cost)) { CivMessage.send(player, "§c" +
 * CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME)); return; }
 * resident.getTreasury().withdraw(configEnchant.cost); hand.addEnchantment(Enchantment.THORNS, 2); break; } case 1: { if
 * (!Enchantment.FROST_WALKER.canEnchantItem(hand)) { CivMessage.sendError(player,
 * CivSettings.localize.localizedString("moscowstateuni_enchant_cannotEnchant")); return; } if
 * (hand.containsEnchantment(Enchantment.FROST_WALKER)) { try { throw new
 * CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant")); } catch (CivException e) { e.printStackTrace(); } }
 * final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_walker"); if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
 * CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost,
 * CivSettings.CURRENCY_NAME)); return; } resident.getTreasury().withdraw(configEnchant.cost); hand.addEnchantment(Enchantment.FROST_WALKER,
 * 1); break; } case 2: { if (!Enchantment.PROTECTION_FALL.canEnchantItem(hand)) { CivMessage.sendError(player,
 * CivSettings.localize.localizedString("oracle_enchant_cannotEnchant")); return; } if
 * (hand.containsEnchantment(Enchantment.PROTECTION_FALL)) { try { throw new
 * CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant")); } catch (CivException e) { e.printStackTrace(); } }
 * final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_fall"); if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
 * CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost,
 * CivSettings.CURRENCY_NAME)); return; } resident.getTreasury().withdraw(configEnchant.cost);
 * hand.addEnchantment(Enchantment.PROTECTION_FALL, 2); break; } case 3: { if (!Enchantment.DEPTH_STRIDER.canEnchantItem(hand)) {
 * CivMessage.sendError(player, CivSettings.localize.localizedString("oracle_enchant_cannotEnchant")); return; } if
 * (hand.containsEnchantment(Enchantment.DEPTH_STRIDER)) { try { throw new
 * CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant")); } catch (CivException e) { e.printStackTrace(); } }
 * final ConfigEnchant configEnchant = CivSettings.enchants.get("ench_strider"); if (!resident.getTreasury().hasEnough(configEnchant.cost))
 * { CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_oracle_enchant_cannotAfford", configEnchant.cost,
 * CivSettings.CURRENCY_NAME)); return; } resident.getTreasury().withdraw(configEnchant.cost);
 * hand.addEnchantment(Enchantment.DEPTH_STRIDER, 2); break; } default: { return; } } } CivMessage.sendSuccess((CommandSender)player,
 * CivSettings.localize.localizedString("oracle_enchantment_success")); } } /* buff_oracle_extra_beakers buff_oracle_double_tax_beakers
 * oracle_enchant_cannotEnchant oracle_enchantment_success var_oracle_nonMember oracle_enchant_cannotEnchant var_oracle_enchant_cannotAfford
 * var_oracle_enchant_cannotAfford oracle_enchant_hasEnchantment oracle_enchant_nonEnchantable ench_kara */
