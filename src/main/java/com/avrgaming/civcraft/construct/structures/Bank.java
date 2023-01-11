package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;

public class Bank extends Structure {

	private int level = 1;
	private double interestRate = 0;

	// private static final int EMERALD_SIGN = 3;
	private static final int IRON_SIGN = 0;
	private static final int GOLD_SIGN = 1;
	private static final int DIAMOND_SIGN = 2;
	private static final int EMERALD_SIGN = 3;
	private static final int IRON_BLOCK_SIGN = 4;
	private static final int GOLD_BLOCK_SIGN = 5;
	private static final int DIAMOND_BLOCK_SIGN = 6;
	private static final int EMERALD_BLOCK_SIGN = 7;

	public Bank(String id, Town town) {
		super(id, town);
	}

	public double getBankExchangeRate() {
		double exchange_rate = 0.0;
		switch (level) {
		case 1:
			exchange_rate = 0.40;
			break;
		case 2:
			exchange_rate = 0.50;
			break;
		case 3:
			exchange_rate = 0.60;
			break;
		case 4:
			exchange_rate = 0.70;
			break;
		case 5:
			exchange_rate = 0.80;
			break;
		case 6:
			exchange_rate = 0.90;
			break;
		case 7:
			exchange_rate = 1;
			break;
		case 8:
			exchange_rate = 1.20;
			break;
		case 9:
			exchange_rate = 1.50;
			break;
		case 10:
			exchange_rate = 2;
			break;
		}

		double rate = 1;
		double addtional = rate * this.getTownOwner().getBuffManager().getEffectiveDouble(Buff.BARTER);
		rate += addtional;
		if (rate > 1) {
			exchange_rate *= rate;
		}
		if (this.getCivOwner().getStockExchangeLevel() >= 3) {
			exchange_rate *= 1.25;
		}
		return exchange_rate;
	}

	private String getExchangeRateString() {
		return ((int) (getBankExchangeRate() * 100) + "%");
	}

	private String getNonResidentFeeString() {
		return CivSettings.localize.localizedString("bank_sign_fee") + " " + ((int) (this.getNonMemberFeeComponent().getFeeRate() * 100) + "%");
	}

	private String getSignItemPrice(int signId) {
		double itemPrice;
		if (signId == IRON_SIGN) {
			itemPrice = CivSettings.iron_rate;
		} else
			if (signId == IRON_BLOCK_SIGN) {
				itemPrice = CivSettings.iron_rate * 9;
			} else
				if (signId == GOLD_SIGN) {
					itemPrice = CivSettings.gold_rate;
				} else
					if (signId == GOLD_BLOCK_SIGN) {
						itemPrice = CivSettings.gold_rate * 9;
					} else
						if (signId == DIAMOND_SIGN) {
							itemPrice = CivSettings.diamond_rate;
						} else
							if (signId == DIAMOND_BLOCK_SIGN) {
								itemPrice = CivSettings.diamond_rate * 9;
							} else
								if (signId == EMERALD_SIGN) {
									itemPrice = CivSettings.emerald_rate;
								} else {
									itemPrice = CivSettings.emerald_rate * 9;
								}

		String out = "1 = ";
		out += (int) (itemPrice * getBankExchangeRate());
		out += " Coins";
		return out;
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) return;

		Material itemMat;
		Material itemMat_alt = null;
		double coins;
		double coins_alt = 0;
		String itemName;
		ItemStack inHand = player.getInventory().getItemInMainHand();
		try {
			if (CustomMaterial.isCustomMaterial(inHand))  throw new CivException(CivSettings.localize.localizedString("bank_invalidItem"));
			switch (sign.getAction()) {
			case "iron":
				itemMat = Material.IRON_INGOT;
				coins = CivSettings.iron_rate;
				itemName = CivSettings.localize.localizedString("bank_itemName_iron");
				itemMat_alt = Material.IRON_BLOCK;
				coins_alt = CivSettings.iron_rate * 9;
				break;
			case "gold":
				itemMat = Material.GOLD_INGOT;
				coins = CivSettings.gold_rate;
				itemName = CivSettings.localize.localizedString("bank_itemName_gold");
				itemMat_alt = Material.GOLD_BLOCK;
				coins_alt = CivSettings.gold_rate * 9;
				break;
			case "diamond":
				itemMat = Material.DIAMOND;
				coins = CivSettings.diamond_rate;
				itemName = CivSettings.localize.localizedString("bank_itemName_diamond");
				itemMat_alt = Material.DIAMOND_BLOCK;
				coins_alt = CivSettings.diamond_rate * 9;
				break;
			case "emerald":
				itemMat = Material.EMERALD;
				coins = CivSettings.emerald_rate;
				itemName = CivSettings.localize.localizedString("bank_itemName_emerald");
				itemMat_alt = Material.EMERALD_BLOCK;
				coins_alt = CivSettings.emerald_rate * 9;
				break;
			case "ironB":
				itemMat = Material.IRON_BLOCK;
				coins = CivSettings.iron_rate * 9;
				itemName = CivSettings.localize.localizedString("bank_itemName_iron");
				break;
			case "goldB":
				itemMat = Material.GOLD_BLOCK;
				coins = CivSettings.gold_rate * 9;
				itemName = CivSettings.localize.localizedString("bank_itemName_gold");
				break;
			case "diamondB":
				itemMat = Material.DIAMOND_BLOCK;
				coins = CivSettings.diamond_rate * 9;
				itemName = CivSettings.localize.localizedString("bank_itemName_diamond");
				break;
			case "emeraldB":
				itemMat = Material.EMERALD_BLOCK;
				coins = CivSettings.emerald_rate * 9;
				itemName = CivSettings.localize.localizedString("bank_itemName_emerald");
				break;
			default:
				// TODO Обработка других табличек
				CivSettings.localize.localizedString("bank_itemName_stuff");
				return;
			}

			double exchange_rate;

			exchange_rate = getBankExchangeRate();

			if (!(inHand.getType() == itemMat)) if (inHand.getType() == itemMat_alt) {
				coins = coins_alt;
			} else
				throw new CivException(CivSettings.localize.localizedString("var_bank_notEnoughInHand", itemName));

			int count = inHand.getAmount();
			player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
			player.updateInventory();

			// Resident is in his own town.

			// non-resident must pay the town's non-resident tax
			double giveToPlayer = (coins * count) * exchange_rate;
			double giveToTown = (resident.getTown() != this.getTownOwner()) ? giveToPlayer * this.getNonResidentFee() : 0;
			giveToPlayer -= giveToTown;

			giveToTown = Math.round(giveToTown);
			giveToPlayer = Math.round(giveToPlayer);

			this.getTownOwner().depositDirect(giveToTown);
			resident.getTreasury().deposit(giveToPlayer);

			DecimalFormat df = new DecimalFormat();
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_bank_exchanged", count, itemName, df.format(giveToPlayer), CivSettings.CURRENCY_NAME));
			if (giveToTown != 0) CivMessage.send(player, CivColor.Yellow + " " + CivSettings.localize.localizedString("var_taxes_paid", df.format(giveToTown), CivSettings.CURRENCY_NAME));
		} catch (CivException e) {
			CivMessage.send(player, CivColor.Rose + e.getMessage());
		}
	}

	@Override
	public void updateSignText() {
		for (ConstructSign sign : getSigns()) {

			switch (sign.getAction().toLowerCase()) {
			case "iron":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_iron") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(IRON_SIGN) + "\n" + getNonResidentFeeString());
				break;
			case "gold":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_gold") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(GOLD_SIGN) + "\n" + getNonResidentFeeString());
				break;
			case "diamond":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_diamond") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(DIAMOND_SIGN) + "\n" + getNonResidentFeeString());
				break;
			case "emerald":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_emerald") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(EMERALD_SIGN) + "\n" + getNonResidentFeeString());
				break;
			case "ironb":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_ironBlock") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(IRON_BLOCK_SIGN) + "\n" + getNonResidentFeeString());
				break;
			case "goldb":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_goldBlock") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(GOLD_BLOCK_SIGN) + "\n" + getNonResidentFeeString());
				break;
			case "diamondb":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_diamondBlock") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(DIAMOND_BLOCK_SIGN) + "\n" + getNonResidentFeeString());
				break;
			case "emeraldb":
				sign.setText(CivSettings.localize.localizedString("bank_itemName_emeraldBlock") + "\n" + "At " + getExchangeRateString() + "\n" + getSignItemPrice(EMERALD_BLOCK_SIGN) + "\n" + getNonResidentFeeString());
				break;
			}

			sign.update();
		}
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + CivSettings.localize.localizedString("bank_dynmapName") + "</u></b><br/>";
		out += CivSettings.localize.localizedString("Level") + " " + this.level;
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "bank";
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getNonResidentFee() {
		return this.getNonMemberFeeComponent().getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.getNonMemberFeeComponent().setFeeRate(nonResidentFee);
	}

	public double getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(double interestRate) {
		this.interestRate = interestRate;
	}

	@Override
	public void onLoad() {
		/* Process the interest rate. */
		if (interestRate == 0.0) {
			this.getTownOwner().getTreasury().setPrincipalAmount((double) 0);
			return;
		}

		/* Update the principal with the new value. */
		this.getTownOwner().getTreasury().setPrincipalAmount(this.getTownOwner().getTreasury().getBalance());
	}

	@Override
	public void onDailyUpdate() {

		/* Process the interest rate. */
		double effectiveInterestRate = interestRate;
		if (effectiveInterestRate == 0.0) {
			this.getTownOwner().getTreasury().setPrincipalAmount((double) 0);
			return;
		}

		double principal = this.getTownOwner().getTreasury().getPrincipalAmount();

		if (this.getTownOwner().getBuffManager().hasBuff("buff_greed")) {
			double increase = this.getTownOwner().getBuffManager().getEffectiveDouble("buff_greed");
			effectiveInterestRate += increase;
			CivMessage.sendTown(this.getTownOwner(), CivColor.LightGray + CivSettings.localize.localizedString("bank_greed"));
		}

		double newCoins = principal * effectiveInterestRate;

		// Dont allow fractional coins.
		newCoins = Math.floor(newCoins);

		if (newCoins != 0) {
			CivMessage.sendTown(this.getTownOwner(), CivColor.LightGreen + CivSettings.localize.localizedString("var_bank_interestMsg1", newCoins, CivSettings.CURRENCY_NAME, principal));
			this.getTownOwner().getTreasury().deposit(newCoins);

		}

		/* Update the principal with the new value. */
		this.getTownOwner().getTreasury().setPrincipalAmount(this.getTownOwner().getTreasury().getBalance());

	}

	@Override
	public void onPostBuild() {
		this.level = getTownOwner().BM.saved_bank_level;
		this.interestRate = getTownOwner().BM.saved_bank_interest_amount;
	}

	public String getHoloItemPrice(int signId) {
		double itemPrice;
		switch (signId) {
		case 0:
			itemPrice = CivSettings.iron_rate;
			break;
		case 4:
			itemPrice = CivSettings.iron_rate * 9.0D;
			break;
		case 1:
			itemPrice = CivSettings.gold_rate;
			break;
		case 5:
			itemPrice = CivSettings.gold_rate * 9.0D;
			break;
		case 2:
			itemPrice = CivSettings.diamond_rate;
			break;
		case 6:
			itemPrice = CivSettings.diamond_rate * 9.0D;
			break;
		case 3:
			itemPrice = CivSettings.emerald_rate;
			break;
		default:
			itemPrice = CivSettings.emerald_rate * 9.0D;
		}
		int plusars = (int) (itemPrice * getBankExchangeRate());
		String out = "1 штука = ";
		out = out + (int) (itemPrice * getBankExchangeRate());
		out = out + CivMessage.plurals(plusars, " Монеты", " Монета", " Монет");
		return out;
	}
}
