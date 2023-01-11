package com.avrgaming.civcraft.trade;

import java.util.HashMap;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import ua.rozipp.gui.GuiInventory;
import ua.rozipp.gui.GuiItem;
import com.avrgaming.civcraft.listener.SimpleListener;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class TradeInventoryListener extends SimpleListener {

	public static HashMap<String, TradeInventoryPair> tradeInventories = new HashMap<>();

	public static final int OTHERS_SLOTS_START = 9;
	public static final int OTHERS_SLOTS_END = 2 * 9;
	public static final int MY_SLOTS_START = 3 * 9;
	public static final int MY_SLOTS_END = 4 * 9;
	public static final int SLOTS_END = 5 * 9;
	public static final int MY_SLOT_BUTTON = 44;
	public static final int OTHER_SLOT_BUTTON = 8;

	public static final int MY_COINS_DOWN = 9 * 4;
	public static final int MY_COINS_UP = (9 * 4) + 1;
	public static final int MY_COIN_OFFER = 43;
	public static final int OTHER_COIN_OFFER = 7;

	public static String getTradeInventoryKey(Player player) {
		return player.getUniqueId().toString() + ":inventroy";
	}

	public class SyncInventoryChange implements Runnable {
		int sourceSlot;
		int destSlot;

		Inventory sourceInventory;
		Player otherPlayer;
		Inventory otherInventory;

		public SyncInventoryChange(int sourceSlot, int destSlot, Inventory sourceInventory, Player otherPlayer, Inventory otherInventory) {
			this.sourceInventory = sourceInventory;
			this.sourceSlot = sourceSlot;
			this.destSlot = destSlot;
			this.otherPlayer = otherPlayer;
			this.otherInventory = otherInventory;
		}

		@Override
		public void run() {
			if (otherPlayer.getOpenInventory() != otherInventory) return;
			if (otherInventory != null) otherInventory.setItem(destSlot, sourceInventory.getItem(sourceSlot));
		}
	}

	public class SyncInventoryChangeAll implements Runnable {
		Inventory sourceInventory;
		Player otherPlayer;
		Inventory otherInventory;

		public SyncInventoryChangeAll(Inventory src, Player other, Inventory otherInv) {
			this.sourceInventory = src;
			this.otherPlayer = other;
			this.otherInventory = otherInv;
		}

		@Override
		public void run() {
			if (otherPlayer.getOpenInventory() != this.otherInventory) return;

			if (otherInventory != null) {
				int k = OTHERS_SLOTS_START;
				for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
					otherInventory.setItem(k, sourceInventory.getItem(i));
					k++;
				}
			}
		}
	}

	public boolean handleSlotChange(int slot, TradeInventoryPair pair) {
		/* Prevent user from clicking in the other player's spot. */
		if (slot <= OTHERS_SLOTS_END) return false;

		/* Update the other inventory */
		if ((slot >= MY_SLOTS_START) && (slot <= MY_SLOTS_END)) {
			int relativeSlot = slot % 9;
			TaskMaster.syncTask(new SyncInventoryChange(slot, OTHERS_SLOTS_START + relativeSlot, pair.inv.getInventory(), pair.otherPlayer, pair.otherInv.getInventory()));
		}

		return true;
	}

	public void handleShiftClick(InventoryClickEvent event, Player player, TradeInventoryPair pair) {
		/* First determine if we're shifting into our inv or into the trade window. */
		if (event.getRawSlot() > SLOTS_END) {
			/* We're clicking in our inventory, trying to bring in an item. lets cheat by creating a new temp inventory which contains the current
			 * contents of our slots, add this item to it, then replace the slots with that inv's contents. */
			Inventory tempInv = Bukkit.createInventory(event.getWhoClicked(), 9);
			/* Copy contents from current slots. */
			int k = 0;
			for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
				tempInv.setItem(k, event.getInventory().getItem(i));
				k++;
			}

			/* Add this item to our tempInv. */
			HashMap<Integer, ItemStack> leftovers = tempInv.addItem(event.getCurrentItem());

			/* Copy contents of the temp inventory on top of our slots. */
			k = 0;
			for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
				event.getInventory().setItem(i, tempInv.getItem(k));
				k++;
			}

			/* Now, remove the item from the player's inventory. */
			player.getInventory().setItem(event.getSlot(), null);

			/* Re-add any leftovers we might have. */
			for (ItemStack stack : leftovers.values()) {
				player.getInventory().addItem(stack);
			}

			/* Cancel normal event processing. */
			TaskMaster.syncTask(new SyncInventoryChangeAll(pair.inv.getInventory(), pair.otherPlayer, pair.otherInv.getInventory()));
			event.setCancelled(true);
		} else {
			/* We're clicking in the trade inventory, tryign to take out an item. */
			if (event.getRawSlot() < OTHERS_SLOTS_END) {
				/* We tried to shift click on the other player's side. Cancel. */
				event.setCancelled(true);
			} else {
				/* We're clicking on our side, allow it as normal. */
				TaskMaster.syncTask(new SyncInventoryChangeAll(pair.inv.getInventory(), pair.otherPlayer, pair.otherInv.getInventory()));
			}
		}
	}

	private void handleDoubleClick(InventoryClickEvent event, Player player, TradeInventoryPair pair) {
		/* If we've double clicked anywhere at all, just update the inventory to reflect the changes. */
		TaskMaster.syncTask(new SyncInventoryChangeAll(pair.inv.getInventory(), pair.otherPlayer, pair.otherInv.getInventory()));
	}

	private void handleCoinChange(TradeInventoryPair pair, TradeInventoryPair otherPair, double change) {
		if (change > 0) {
			Resident resident = CivGlobal.getResident(pair.player);
			/* We're adding coins, so lets check that we have enough coins on our person. */
			if (resident.getTreasury().getBalance() < (pair.coins + change)) {
				pair.coins = resident.getTreasury().getBalance();
				otherPair.otherCoins = pair.coins;
			} else {
				/* We've got enough, so add it for now. */
				pair.coins += change;
				otherPair.otherCoins = pair.coins;
			}
		} else {
			/* We're removing coins. */
			change *= -1; /* flip sign on change so we can make sense of things */
			if (change > pair.coins) {
				/* Remove all the offered coins. */
				pair.coins = 0;
				otherPair.otherCoins = 0;
			} else {
				/* change is negative, so just add. */
				pair.coins -= change;
				otherPair.otherCoins = pair.coins;
			}
		}

		/* Update our display item. */
		GuiItem guiStack;
		if (pair.coins == 0) {
			guiStack = GuiItem.newGuiItem()//
					.setTitle("" + CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"))//
					.setMaterial(Material.NETHER_BRICK_ITEM)//
					.setLore(CivColor.Yellow + "0 " + CivSettings.CURRENCY_NAME);
		} else {
			guiStack = GuiItem.newGuiItem()//
					.setTitle("" + CivSettings.CURRENCY_NAME + " " + CivSettings.localize.localizedString("resident_tradeOffered"))//
					.setMaterial(Material.GOLD_INGOT)//
					.setLore(CivColor.Yellow + pair.coins + " " + CivSettings.CURRENCY_NAME);
		}
		pair.inv.addGuiItem(MY_COIN_OFFER, guiStack);

		/* Update our offerings in the other's inventory. */
		otherPair.inv.addGuiItem(OTHER_COIN_OFFER, guiStack);
	}

	public void markTradeValid(TradeInventoryPair pair) {
		pair.valid = true;
		pair.inv.addGuiItem(MY_SLOT_BUTTON, GuiItem.newGuiItem(ItemManager.createItemStack(CivData.WOOL, CivData.DATA_WOOL_GREEN, 1))//
				.setTitle(CivSettings.localize.localizedString("resident_tradeYourConfirm"))//
				.setLore(CivColor.Gold + CivSettings.localize.localizedString("resident_tradeClicktoUnConfirm")));

		pair.otherInv.addGuiItem(OTHER_SLOT_BUTTON, GuiItem.newGuiItem(ItemManager.createItemStack(CivData.WOOL, CivData.DATA_WOOL_GREEN, 1))//
				.setTitle("Your Confirm")//
				.setLore(CivSettings.localize.localizedString("var_resident_hasConfirmedTrade", (CivColor.LightBlue + pair.otherPlayer.getName() + CivColor.LightGreen))));
	}

	public void markTradeInvalid(TradeInventoryPair pair) {
		pair.valid = false;
		pair.inv.addGuiItem(MY_SLOT_BUTTON, GuiItem.newGuiItem(ItemManager.createItemStack(CivData.WOOL, CivData.DATA_WOOL_RED, 1))//
				.setTitle(CivSettings.localize.localizedString("resident_tradeYourConfirm"))//
				.setLore(CivColor.Gold + CivSettings.localize.localizedString("resident_tradeClicktoConfirm")));

		pair.otherInv.addGuiItem(OTHER_SLOT_BUTTON, GuiItem.newGuiItem(ItemManager.createItemStack(CivData.WOOL, CivData.DATA_WOOL_RED, 1))//
				.setTitle(pair.otherPlayer.getName() + " " + CivSettings.localize.localizedString("resident_tradeNotconfirmed"))//
				.setLore(CivColor.LightGreen + CivSettings.localize.localizedString("var_resident_hasNotConfirmedTrade1", CivColor.LightBlue + pair.otherPlayer.getName()), //
						CivColor.LightGray + CivSettings.localize.localizedString("resident_hasNotConfirmedTrade1")));
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		Player player = (Player) event.getWhoClicked();
		TradeInventoryPair pair = tradeInventories.get(getTradeInventoryKey(player));
		if (pair == null) return;
		TradeInventoryPair otherPair = tradeInventories.get(getTradeInventoryKey(pair.otherPlayer));
		if (otherPair == null) return;

		GuiInventory savedTradeInventory = pair.inv;
		if (savedTradeInventory == null) return;
		if (!savedTradeInventory.getName().equals(event.getInventory().getName())) return;

		/* Check to see if we've clicked on a button. */
		if (event.getRawSlot() == MY_SLOT_BUTTON) {
			ItemStack button = event.getInventory().getItem(MY_SLOT_BUTTON);
			if (ItemManager.getData(button) == CivData.DATA_WOOL_RED) {
				/* Mark trade as valid. */
				markTradeValid(pair);

				if (pair.valid && otherPair.valid) {
					try {
						completeTransaction(pair, otherPair);
					} catch (CivException e) {
						e.printStackTrace();
						CivMessage.sendError(pair.player, e.getMessage());
						CivMessage.sendError(pair.otherPlayer, e.getMessage());
						GuiInventory.closeInventory(player);
						GuiInventory.closeInventory(pair.otherPlayer);
						return;
					}
				}
			} else {
				/* Mark trade as invalid. */
				markTradeInvalid(pair);
			}
			return;
		} else
			if (event.getRawSlot() == MY_COINS_DOWN) {
				if (event.isShiftClick()) {
					handleCoinChange(pair, otherPair, -1000.0);
				} else {
					handleCoinChange(pair, otherPair, -100.0);
				}
				event.setCancelled(true);
				return;
			} else
				if (event.getRawSlot() == MY_COINS_UP) {
					if (event.isShiftClick()) {
						handleCoinChange(pair, otherPair, 1000.0);
					} else {
						handleCoinChange(pair, otherPair, 100.0);
					}
					event.setCancelled(true);
					return;
				}

		if (pair.valid || otherPair.valid) {
			/* We're changing the inventory. Cant be valid anymore. */
			markTradeInvalid(pair);
			player.updateInventory();
			markTradeInvalid(otherPair);
			pair.otherPlayer.updateInventory();
			event.setCancelled(true);
			return;
		}

		/* Handle the ugly click types. */
		if (event.getClick().equals(ClickType.SHIFT_LEFT) || event.getClick().equals(ClickType.SHIFT_RIGHT)) {
			/* Manually move over item to correct slot. */
			handleShiftClick(event, player, pair);
			return;
		}

		if (event.getClick().equals(ClickType.DOUBLE_CLICK)) {
			handleDoubleClick(event, player, pair);
		}

		if (!handleSlotChange(event.getRawSlot(), pair)) {
			event.setCancelled(true);
		}
	}

	private void completeTransaction(TradeInventoryPair pair, TradeInventoryPair otherPair) throws CivException {
		try {
			/* Remove these pairs from the hashtable as we dont need them anymore. */
			tradeInventories.remove(getTradeInventoryKey(pair.player));
			tradeInventories.remove(getTradeInventoryKey(otherPair.player));

			LinkedList<ItemStack> myStuff = new LinkedList<ItemStack>();
			LinkedList<ItemStack> theirStuff = new LinkedList<ItemStack>();

			int k = OTHERS_SLOTS_START;
			for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++, k++) {
				/* Verify that our "mine" inventory matches the other player's "theirs" inventory. */
				GuiItem stack = pair.inv.getGuiItem(i);
				GuiItem stack2 = otherPair.inv.getGuiItem(k);

				if (stack == null && stack2 == null) continue;

				if ((stack == null && stack2 != null) || (stack != null && stack2 == null)) {
					CivLog.error("Mismatch. One stack was null. stack:" + stack + " stack2:" + stack2 + " i:" + i + " vs k:" + k);
					throw new CivException("Inventory mismatch");
				}

				if (!stack.toString().equals(stack2.toString())) {
					CivLog.error("Is Mine Equal to Theirs?");
					CivLog.error("Position i:" + i + " stack:" + stack);
					CivLog.error("Position k:" + k + " stack2:" + stack2);
					throw new CivException("Inventory mismatch.");
				}

				myStuff.add(stack.getStack());
			}

			k = MY_SLOTS_START;
			for (int i = OTHERS_SLOTS_START; i < OTHERS_SLOTS_END; i++, k++) {
				/* Verify that our "theirs" inventory matches the other player's "mine" inventory. */
				GuiItem stack = pair.inv.getGuiItem(i);
				GuiItem stack2 = otherPair.inv.getGuiItem(k);

				if (stack == null && stack2 == null) continue;

				if (stack == null || stack2 == null) {
					CivLog.error("Mismatch. One stack was null. stack:" + stack + " stack2:" + stack2 + " i:" + i + " vs k:" + k);
					throw new CivException("Inventory mismatch");
				}

				if (!stack.toString().equals(stack2.toString())) {
					CivLog.error("Is Theirs Equal to Mine?");
					CivLog.error("Position i:" + i + " stack:" + stack);
					CivLog.error("Position k:" + k + " stack2:" + stack2);
					throw new CivException("Inventory mismatch.");
				}

				theirStuff.add(stack.getStack());
			}
			/* Transfer any coins. */
			if (pair.coins != otherPair.otherCoins) {
				CivLog.error("pair.coins = " + pair.coins);
				CivLog.error("otherPair.otherCoins = " + otherPair.otherCoins);
				throw new CivException("Coin mismatch...");
			}

			if (otherPair.coins != pair.otherCoins) {
				CivLog.error("otherPair.coins = " + otherPair.coins);
				CivLog.error("pair.coins = " + pair.coins);
				new CivException("Coin mismatch...");
			}

			if (pair.coins < 0 || pair.otherCoins < 0 || otherPair.coins < 0 || otherPair.otherCoins < 0) {
				throw new CivException("Coin amount invalid.");
			}

			Resident usRes = CivGlobal.getResident(pair.player);
			Resident themRes = CivGlobal.getResident(pair.otherPlayer);
			if (!usRes.getTreasury().hasEnough(pair.coins)) {
				CivMessage.sendError(pair.player, pair.player.getName() + " " + CivSettings.localize.localizedString("resident_trade_doesnothaveenough") + " " + CivSettings.CURRENCY_NAME + "!");
				CivMessage.sendError(pair.otherPlayer, pair.player.getName() + " " + CivSettings.localize.localizedString("resident_trade_doesnothaveenough") + " " + CivSettings.CURRENCY_NAME + "!");
				GuiInventory.closeInventory(pair.player);
				GuiInventory.closeInventory(pair.otherPlayer);
				return;
			}

			if (!themRes.getTreasury().hasEnough(pair.otherCoins)) {
				CivMessage.sendError(pair.player, pair.otherPlayer.getName() + " " + CivSettings.localize.localizedString("resident_trade_doesnothaveenough") + " " + CivSettings.CURRENCY_NAME + "!");
				CivMessage.sendError(pair.otherPlayer, pair.otherPlayer.getName() + " " + CivSettings.localize.localizedString("resident_trade_doesnothaveenough") + " " + CivSettings.CURRENCY_NAME + "!");
				GuiInventory.closeInventory(pair.player);
				GuiInventory.closeInventory(pair.otherPlayer);
				return;
			}

			if (pair.coins != 0) {
				usRes.getTreasury().withdraw(pair.coins);
				themRes.getTreasury().deposit(pair.coins);
				CivMessage.sendSuccess(pair.player, CivSettings.localize.localizedString("var_resident_trade_gave", (CivColor.Rose + pair.coins + " " + CivSettings.CURRENCY_NAME), pair.otherPlayer.getName()));
				CivMessage.sendSuccess(pair.otherPlayer, CivSettings.localize.localizedString("var_resident_trade_Receive", (CivColor.Yellow + pair.coins + " " + CivSettings.CURRENCY_NAME), pair.player.getName()));
			}

			if (pair.otherCoins != 0) {
				themRes.getTreasury().withdraw(pair.otherCoins);
				usRes.getTreasury().deposit(pair.otherCoins);

				CivMessage.sendSuccess(pair.player, CivSettings.localize.localizedString("var_resident_trade_Receive", (CivColor.Yellow + pair.coins + " " + CivSettings.CURRENCY_NAME), pair.otherPlayer.getName()));
				CivMessage.sendSuccess(pair.otherPlayer, CivSettings.localize.localizedString("var_resident_trade_gave", (CivColor.Rose + pair.coins + " " + CivSettings.CURRENCY_NAME), pair.player.getName()));
			}

			/* Finally, give their stuff to me. And my stuff to them. */
			for (ItemStack is : theirStuff) {
				HashMap<Integer, ItemStack> leftovers = pair.player.getInventory().addItem(is);
				for (ItemStack stack : leftovers.values()) {
					pair.player.getPlayer().getWorld().dropItem(pair.player.getLocation(), stack);
				}
			}

			for (ItemStack is : myStuff) {
				HashMap<Integer, ItemStack> leftovers = pair.otherPlayer.getInventory().addItem(is);
				for (ItemStack stack : leftovers.values()) {
					pair.otherPlayer.getPlayer().getWorld().dropItem(pair.otherPlayer.getLocation(), stack);
				}
			}

			CivMessage.sendSuccess(pair.player, CivSettings.localize.localizedString("resident_trade_success"));
			CivMessage.sendSuccess(pair.otherPlayer, CivSettings.localize.localizedString("resident_trade_success"));
		} finally {
			GuiInventory.closeInventory(pair.player);
			GuiInventory.closeInventory(pair.otherPlayer);
		}

	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInventoryDragEvent(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		Player player = (Player) event.getWhoClicked();
		TradeInventoryPair pair = tradeInventories.get(getTradeInventoryKey(player));
		if (pair == null) return;

		GuiInventory savedTradeInventory = pair.inv;
		if (savedTradeInventory == null) return;
		if (!savedTradeInventory.getName().equals(event.getInventory().getName())) return;

		for (int slot : event.getRawSlots()) {
			if (!handleSlotChange(slot, pair)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) return;
		Player player = (Player) event.getPlayer();

		TradeInventoryPair pair = tradeInventories.get(getTradeInventoryKey(player));
		if (pair == null) return;

		GuiInventory savedTradeInventory = pair.inv;
		if (savedTradeInventory == null) return;
		if (!savedTradeInventory.getName().equals(event.getInventory().getName())) return;

		/* Refund anything in our slots. */
		for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
			ItemStack stack = pair.inv.getGuiItem(i).getStack();
			if (stack == null) continue;

			HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
			for (ItemStack left : leftovers.values()) {
				player.getWorld().dropItem(player.getLocation(), left);
			}
		}

		// XXX Хаа. "Хороший фикс". Отправить player.updateInventory в синхронный поток
		TaskMaster.syncTask(new Runnable() {
			@Override
			public void run() {
				player.updateInventory();
			}
		});

		tradeInventories.remove(getTradeInventoryKey(player));

		/* Close other player's inventory if open. */
		TradeInventoryPair otherPair = tradeInventories.get(getTradeInventoryKey(pair.otherPlayer));
		if (otherPair != null) {
			GuiItem guiStack = GuiItem.newGuiItem()//
					.setTitle(pair.otherPlayer.getName() + " " + CivSettings.localize.localizedString("resident_tradeNotconfirmed"))//
					.setMaterial(Material.BEDROCK)//
					.setLore(CivColor.LightGray + CivSettings.localize.localizedString("var_resident_trade_cancelled", player.getName()));
			for (int i = OTHERS_SLOTS_START; i < OTHERS_SLOTS_END; i++) {
				otherPair.inv.addGuiItem(i, guiStack);
			}
		}
	}
}