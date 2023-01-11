package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.enchantment.EnchantmentCustom;
import com.avrgaming.civcraft.enchantment.Enchantments;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.LibraryEnchantment;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class Library extends Structure {

	private int level;
	ArrayList<LibraryEnchantment> enchantments = new ArrayList<>();

	public Library(String id, Town town) {
		super(id, town);
		setLevel(town.BM.saved_library_level);
	}

	public static EnchantmentCustom getEnchantFromString(String name) {
		// Armor Enchantments
		if (name.equalsIgnoreCase("protection")) return EnchantmentCustom.PROTECTION_ENVIRONMENTAL;
		if (name.equalsIgnoreCase("fire_protection")) return EnchantmentCustom.PROTECTION_FIRE;
		if (name.equalsIgnoreCase("feather_falling")) return EnchantmentCustom.PROTECTION_FALL;
		if (name.equalsIgnoreCase("blast_protection")) return EnchantmentCustom.PROTECTION_EXPLOSIONS;
		if (name.equalsIgnoreCase("projectile_protection")) return EnchantmentCustom.PROTECTION_PROJECTILE;
		if (name.equalsIgnoreCase("respiration")) return EnchantmentCustom.OXYGEN;
		if (name.equalsIgnoreCase("aqua_affinity")) return EnchantmentCustom.WATER_WORKER;

		// Sword Enchantments
		if (name.equalsIgnoreCase("sharpness")) return EnchantmentCustom.DAMAGE_ALL;
		if (name.equalsIgnoreCase("smite")) return EnchantmentCustom.DAMAGE_UNDEAD;
		if (name.equalsIgnoreCase("bane_of_arthropods")) return EnchantmentCustom.DAMAGE_ARTHROPODS;
		if (name.equalsIgnoreCase("knockback")) return EnchantmentCustom.KNOCKBACK;
		if (name.equalsIgnoreCase("fire_aspect")) return EnchantmentCustom.FIRE_ASPECT;
		if (name.equalsIgnoreCase("looting")) return EnchantmentCustom.LOOT_BONUS_MOBS;

		// Tool Enchantments
		if (name.equalsIgnoreCase("efficiency")) return EnchantmentCustom.DIG_SPEED;
		if (name.equalsIgnoreCase("silk_touch")) return EnchantmentCustom.SILK_TOUCH;
		if (name.equalsIgnoreCase("unbreaking")) return EnchantmentCustom.DURABILITY;
		if (name.equalsIgnoreCase("fortune")) return EnchantmentCustom.LOOT_BONUS_BLOCKS;

		// Bow Enchantments
		if (name.equalsIgnoreCase("power")) return EnchantmentCustom.ARROW_DAMAGE;
		if (name.equalsIgnoreCase("punch")) return EnchantmentCustom.ARROW_KNOCKBACK;
		if (name.equalsIgnoreCase("flame")) return EnchantmentCustom.ARROW_FIRE;
		if (name.equalsIgnoreCase("infinity")) return EnchantmentCustom.ARROW_INFINITE;
		if (name.equalsIgnoreCase("soul_bound")) return EnchantmentCustom.SoulBound;

		return null;
	}

	public double getNonResidentFee() {
		return this.getNonMemberFeeComponent().getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.getNonMemberFeeComponent().setFeeRate(nonResidentFee);
	}

	private String getNonResidentFeeString() {
		return "Fee: " + ((int) (getNonResidentFee() * 100) + "%");
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	private ConstructSign getSignFromSpecialId(int special_id) {
		for (ConstructSign sign : getSigns()) {
			int id = Integer.parseInt(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}

	@Override
	public void updateSignText() {

		int count = 0;

		for (LibraryEnchantment enchant : this.enchantments) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:" + count);
				return;
			}
			double price = enchant.price;

			if (this.getTownOwner().BM.hasStructure("s_shopingcenter")) price /= 2.0;
			sign.setText(enchant.displayName + "\n" + "Level " + enchant.level + "\n" + getNonResidentFeeString() + "\n" + "For " + price);
			sign.update();
			count++;
		}

		for (; count < getSigns().size(); count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			sign.setText("Library Slot\nEmpty");
			sign.update();
		}
	}

	public void validateEnchantment(ItemStack item, LibraryEnchantment ench) throws CivException {
		if (ench.enchant != null) {
			if (!ench.enchant.canEnchantItem(item)) {
				throw new CivException(CivSettings.localize.localizedString("library_enchant_cannotEnchant"));
			}
			if (Enchantments.hasEnchantment(item, ench.enchant) && Enchantments.getLevelEnchantment(item, ench.enchant) >= ench.level) {
				throw new CivException(CivSettings.localize.localizedString("library_enchant_hasEnchant"));
			}
		}
	}

	public ItemStack addEnchantment(ItemStack item, LibraryEnchantment ench) {
		Enchantments.addEnchantment(item, ench.enchant, ench.level);
		return item;
	}

	public void add_enchantment_to_tool(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
		int special_id = Integer.parseInt(sign.getAction());

		if (!event.hasItem()) {
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("library_enchant_itemNotInHand"));
			return;
		}
		ItemStack item = event.getItem();

		if (special_id >= this.enchantments.size()) {
			throw new CivException(CivSettings.localize.localizedString("library_enchant_notReady"));
		}

		LibraryEnchantment ench = this.enchantments.get(special_id);
		this.validateEnchantment(item, ench);

		int payToTown = (int) Math.round(ench.price * getNonResidentFee());
		Resident resident;

		resident = CivGlobal.getResident(player.getName());
		Town t = resident.getTown();
		if (t == this.getTownOwner()) {
			// Pay no taxes! You're a member.
			payToTown = 0;
		}

		// Determine if resident can pay.
		if (!resident.getTreasury().hasEnough(ench.price + payToTown)) {
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_library_enchant_cannotAfford", ench.price + payToTown, CivSettings.CURRENCY_NAME));
			return;
		}

		// Take money, give to server, TEH SERVER HUNGERS ohmnom nom
		resident.getTreasury().withdraw(ench.price);

		// Send money to town for non-resident fee
		if (payToTown != 0) {
			getTownOwner().depositDirect(payToTown);
			CivMessage.send(player, CivColor.Yellow + " " + CivSettings.localize.localizedString("var_taxes_paid", payToTown, CivSettings.CURRENCY_NAME));
		}

		// Successful payment, process enchantment.
		ItemStack newStack = this.addEnchantment(item, ench);
		player.getInventory().setItemInMainHand(newStack);
		CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_library_enchantment_added", ench.displayName));
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		try {
			add_enchantment_to_tool(player, sign, event);
		} catch (CivException e) {
			CivMessage.send(player, CivColor.Rose + e.getMessage());
		}
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + this.getDisplayName() + "</u></b><br/>";

		if (this.enchantments.size() == 0) {
			out += CivSettings.localize.localizedString("library_dynmap_nothingStocked");
		} else {
			for (LibraryEnchantment mat : this.enchantments) {
				out += CivSettings.localize.localizedString("var_library_dynmap_item", mat.displayName, mat.price) + "<br/>";
			}
		}
		return out;
	}

	public void addEnchant(LibraryEnchantment enchant) throws CivException {
		if (enchantments.size() >= 4) {
			throw new CivException(CivSettings.localize.localizedString("library_full"));
		}
		enchantments.add(enchant);
	}

	@Override
	public String getMarkerIconName() {
		return "bookshelf";
	}

	public void reset() {
		this.enchantments.clear();
		this.updateSignText();
	}

}
