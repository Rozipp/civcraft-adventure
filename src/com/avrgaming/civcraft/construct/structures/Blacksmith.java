package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.tasks.NotificationTask;
import com.avrgaming.civcraft.units.Equipments;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Blacksmith extends Structure {

	private static final long COOLDOWN = 5;
	//private static final double BASE_CHANCE = 0.8;
	public static int SMELT_TIME_SECONDS = 3600 * 3;
	public static double YIELD_RATE = 1.25;

	private Date lastUse = new Date();

	public Blacksmith(String id, Town town) {
		super(id, town);
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.getNonMemberFeeComponent().setFeeRate(nonResidentFee);
	}

	private String getNonResidentFeeString() {
		return CivSettings.localize.localizedString("Fee:") + " " + ((int) (this.getNonMemberFeeComponent().getFeeRate() * 100) + "%");
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "factory";
	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
		int special_id = Integer.parseInt(sign.getAction());
		Date now = new Date();
		long diff = now.getTime() - lastUse.getTime();
		diff /= 1000;
		if (diff < Blacksmith.COOLDOWN) throw new CivException(CivSettings.localize.localizedString("var_blacksmith_onCooldown", (Blacksmith.COOLDOWN - diff)));
		lastUse = now;
		switch (special_id) {
			case 0 :
				this.deposit_forge(player);
				break;
			case 1 :
				double cost = CivSettings.getDoubleStructure("blacksmith.forge_cost");
				this.perform_forge(player, cost);
				break;
			case 2 :
				this.depositSmelt(player, player.getInventory().getItemInMainHand());
				break;
			case 3 :
				this.withdrawSmelt(player);
				break;
		}
	}

	@Override
	public void updateSignText() {
		double cost = CivSettings.getDoubleStructure("blacksmith.forge_cost");
		for (ConstructSign sign : getSigns()) {
			int special_id = Integer.parseInt(sign.getAction());
			switch (special_id) {
				case 0 :
					sign.setText(CivSettings.localize.localizedString("rightClickDisabled"));
					break;
				case 1 :
					sign.setText("Одень амуницию"//CivSettings.localize.localizedString("blacksmith_sign_forgeCost")
							+ "\n " + cost + CivSettings.CURRENCY_NAME + "\n" + getNonResidentFeeString());
					break;
				case 2 :
					sign.setText(CivSettings.localize.localizedString("blacksmith_sign_depositOre"));
					break;
				case 3 :
					sign.setText(CivSettings.localize.localizedString("blacksmith_sign_withdrawOre"));
					break;
			}
			sign.update();
		}
	}

	public String getkey(Player player, Structure struct, String tag) {
		return player.getUniqueId().toString() + "_" + struct.getConfigId() + "_" + struct.getCorner().toString() + "_" + tag;
	}

	public static boolean canSmelt(int blockid) {
		switch (blockid) {
			case CivData.GOLD_ORE :
			case CivData.IRON_ORE :
				return true;
		}
		return false;
	}

	/* Converts the ore id's into the ingot id's */
	public static int convertType(int blockid) {
		switch (blockid) {
			case CivData.GOLD_ORE :
				return CivData.GOLD_INGOT;
			case CivData.IRON_ORE :
				return CivData.IRON_INGOT;
		}
		return -1;
	}

	/* Deposit forge will take the current item in the player's hand and deposit its information into the sessionDB. It will store the item's id, data, and
	 * damage. */
	public void deposit_forge(Player player) {
		CivMessage.sendSuccess(player, CivSettings.localize.localizedString("rightClickDisabled"));
	}

	public void perform_forge(Player player, double cost) throws CivException {
		if (!CivGlobal.getResident(player).getTreasury().hasEnough(cost)) {
			throw new CivException("§c" + CivSettings.localize.localizedString("blacksmith_forge_notEnough",
					"§6" + (cost - CivGlobal.getResident(player).getTreasury().getBalance())));
		}

		ItemStack stack = player.getInventory().getItemInMainHand();
		String mid = ItemManager.getUMid(stack);

		Resident resident = CivGlobal.getResident(player);
		if (!resident.isUnitActive()) throw new CivException("Сперва активируйте юнита");
		UnitObject uo = CivGlobal.getUnitObject(resident.getUnitObjectId());
		if (uo == null) throw new CivException("Юнит сломан. Ошибка базы данних. Обратитесь к администрации");

		Equipments equip = Equipments.identificateEquipments(mid);

		if (!uo.getConfigUnit().equipments.contains(mid)) throw new CivException("Этот предмет нельзя одеть на вашего юнита");
		String old_mid = uo.getEquipment(equip);
		if (mid.equals(old_mid)) throw new CivException("На Вас надета такая же аммуниция");

		CivGlobal.getResident(player).getTreasury().withdraw(cost);
		
		player.getInventory().setItemInMainHand(ItemManager.createItemStack(old_mid, 1));
		uo.setEquipment(equip, mid);
		uo.rebuildUnitItem(player);
		UnitStatic.updateUnitForPlaeyr(player);
		CivMessage.sendSuccess(player, "Аммуниция одета удачно");
	}

	/* Take the itemstack in hand and deposit it into the session DB. */
	@SuppressWarnings("deprecation")
	public void depositSmelt(Player player, ItemStack itemsInHand) throws CivException {

		// Make sure that the item is a valid smelt type.
		if (!Blacksmith.canSmelt(itemsInHand.getTypeId())) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_onlyOres"));
		}

		// Only members can use the smelter
		Resident res = CivGlobal.getResident(player.getName());
		if (!res.hasTown() || this.getTownOwner() != res.getTown()) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_notMember"));
		}

		String value = convertType(itemsInHand.getTypeId()) + ":" + (itemsInHand.getAmount() * Blacksmith.YIELD_RATE);
		String key = getkey(player, this, "smelt");

		// Store entry in session DB
		sessionAdd(key, value);

		// Take ore away from player.
		player.getInventory().removeItem(itemsInHand);
		//BukkitTools.sch
		// Schedule a message to notify the player when the smelting is finished.
		BukkitObjects.scheduleAsyncDelayedTask(new NotificationTask(player.getName(), CivColor.LightGreen + CivSettings.localize
				.localizedString("var_blacksmith_smelt_asyncNotify", itemsInHand.getAmount(), CivData.getDisplayName(itemsInHand.getTypeId()))),
				TimeTools.toTicks(SMELT_TIME_SECONDS));

		CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_blacksmith_smelt_depositSuccess", itemsInHand.getAmount(),
				CivData.getDisplayName(itemsInHand.getTypeId())));

		player.updateInventory();
	}

	/* Queries the sessionDB for entries for this player When entries are found, their inserted time is compared to the current time, if they have been in long
	 * enough each itemstack is sent to the players inventory.
	 * 
	 * For each itemstack ready to withdraw try to place it in the players inventory. If there is not enough space, take the leftovers and place them back in
	 * the sessionDB. If there are no leftovers, delete the sessionDB entry. */
	@SuppressWarnings("deprecation")
	public void withdrawSmelt(Player player) throws CivException {

		String key = getkey(player, this, "smelt");
		ArrayList<SessionEntry> entries;

		// Only members can use the smelter
		Resident res = CivGlobal.getResident(player.getName());
		if (!res.hasTown() || this.getTownOwner() != res.getTown()) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_notMember"));
		}

		entries = CivGlobal.getSessionDatabase().lookup(key);

		if (entries == null || entries.size() == 0) {
			throw new CivException(CivSettings.localize.localizedString("blacksmith_smelt_nothingInSmelter"));
		}

		Inventory inv = player.getInventory();
		HashMap<Integer, ItemStack> leftovers;

		for (SessionEntry se : entries) {
			String[] split = se.value.split(":");
			int itemId = Integer.parseInt(split[0]);
			double amount = Double.parseDouble(split[1]);
			long now = System.currentTimeMillis();
			int secondsBetween = CivGlobal.getSecondsBetween(se.time, now);

			// First determine the time between two events.
			if (secondsBetween < Blacksmith.SMELT_TIME_SECONDS) {
				DecimalFormat df1 = new DecimalFormat("0.##");

				double timeLeft = ((double) Blacksmith.SMELT_TIME_SECONDS - (double) secondsBetween) / (double) 60;
				//Date finish = new Date(now+(secondsBetween*1000));
				CivMessage.send(player, CivColor.Yellow + CivSettings.localize.localizedString("var_blacksmith_smelt_inProgress1", amount,
						CivData.getDisplayName(itemId), df1.format(timeLeft)));
				continue;
			}

			ItemStack stack = new ItemStack(itemId, (int) amount, (short) 0);
			leftovers = inv.addItem(stack);

			// If this stack was successfully withdrawn, delete it from the DB.
			if (leftovers.size() == 0) {
				CivGlobal.getSessionDatabase().delete(se.request_id, se.key);
				CivMessage.send(player, CivSettings.localize.localizedString("var_cmd_civ_withdrawSuccess", amount, CivData.getDisplayName(itemId)));

				break;
			} else {
				// We do not have space in our inventory, inform the player.
				CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("blacksmith_smelt_notEnoughInvenSpace"));

				// If the leftover size is the same as the size we are trying to withdraw, do nothing.
				int leftoverAmount = CivGlobal.getLeftoverSize(leftovers);

				if (leftoverAmount == amount) {
					continue;
				}

				if (leftoverAmount == 0) {
					//just in case we somehow get an entry with 0 items in it.
					CivGlobal.getSessionDatabase().delete(se.request_id, se.key);
				} else {
					// Some of the items were deposited into the players inventory but the sessionDB 
					// still has the full amount stored, update the db to only contain the leftovers.
					String newValue = itemId + ":" + leftoverAmount;
					CivGlobal.getSessionDatabase().update(se.request_id, se.key, newValue);
				}
			}

			// only withdraw one item at a time.
			break;
		}

		player.updateInventory();
	}

}
