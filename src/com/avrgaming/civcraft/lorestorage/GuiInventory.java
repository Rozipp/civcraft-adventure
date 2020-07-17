package com.avrgaming.civcraft.lorestorage;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CallbackInterface;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GuiInventory implements CallbackInterface {
	public static final int MAX_INV_SIZE = 54;
	public static final int INV_COLUM_COUNT = 9;

	private static Map<UUID, ArrayDeque<GuiInventory>> playersGuiInventoryStack = new HashMap<>();
	protected static Map<String, GuiInventory> staticGuiInventory = new HashMap<>();

	private String id;
	private Player player;
	private Resident resident;
	private Town town;
	private Civilization civ;
	private String arg = null;
	private Inventory inventory = null;
	private HashMap<Integer, GuiItem> items = new HashMap<>();
	private String title = "";
	private Integer row = 6;

	public GuiInventory(Player player, String arg) {
		this.player = player;
		this.resident = CivGlobal.getResident(player);
		this.arg = arg;
		this.id = buildId(this.getClass().getSimpleName(), arg);
	}

	// ------------- builders

	public GuiInventory setRow(int row) {
		this.row = row;
		if (this.row < 1) this.row = 1;
		if (this.row > 6) this.row = 6;
		return this;
	}

	public GuiInventory setTitle(String title) {
		this.title = (title.length() > 32) ? title.substring(0, 32) : title;
		return this;
	}

	public GuiInventory setTown(Town town) {
		this.town = town;
		return this;
	}

	public GuiInventory setCiv(Civilization civ) {
		this.civ = civ;
		return this;
	}

	// -------------- items

	public void addGuiItem(GuiItem item) {
		addGuiItem(0, item);
	}

	public void addGuiItem(Integer slot, GuiItem item) {
		if (slot < 0 || slot >= size()) {
			int i;
			for (i = 0; i < size(); i++) {
				if (items.get(i) == null) break;
			}
			items.put(i, item);
			return;
		}
		if (items.get(slot) == null) {
			items.put(slot, item);
			return;
		}
		if (items.size() >= size()) {
			items.put(slot, item);
			return;
		}
		int i;
		for (i = 0; i < size(); i++) {
			if (items.get(i) == null) break;
		}
		items.put(i, items.get(slot));
		items.put(slot, item);
	}

	public void addLastItem() {
		addLastItem(CivSettings.localize.localizedString("loreGui_recipes_back"));
	}

	public void addLastItem(String lore) {
		ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(getPlayer().getUniqueId());
		if (gis.isEmpty()) {
			items.put(size(), GuiItems.newGuiItem()//
					.setTitle("§c" + CivSettings.localize.localizedString("loregui_cancel"))//
					.setMaterial(Material.EMPTY_MAP)//
					.setLore(lore)//
					.setAction("CloseInventory"));
		} else {
			items.put(size(), GuiItems.newGuiItem()//
					.setTitle(CivSettings.localize.localizedString("loreGui_recipes_back"))//
					.setMaterial(Material.MAP)//
					.setLore(lore)//
					.addLore(CivSettings.localize.localizedString("bookReborn_backTo", gis.getFirst().getName()))//
					.setAction("OpenBackInventory"));
		}
	}

	public GuiItem getGuiItem(Integer i) {
		return items.get(i);
	}

	public Integer size() {
		return row * INV_COLUM_COUNT - 1;
	}

	// --------------- getters

	public String getName() {
		return inventory.getName();
	}

	public String getTitle() {
		return title;
	}

	private InventoryHolder getInventoryHolder() {
		return player;
	}

	// -------------------- Inventory

	public void openInventory() {
		ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(player.getUniqueId());
		player.openInventory(getInventory());
		gis.push(this);
		GuiInventory.setInventoryStack(player.getUniqueId(), gis);
	}

	public Inventory getInventory() {
		if (inventory == null) try {
			inventory = Bukkit.createInventory(getInventoryHolder(), size() + 1, title);
			addLastItem();
			for (int slot = 0; slot <= size(); slot++) {
				GuiItem item = items.get(slot);
				if (item == null) continue;
				inventory.setItem(slot, item.getStack());
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return inventory;
	}

	@Override
	public void execute(String... strings) {
		// XXX Children Override
		ArrayDeque<GuiInventory> gis = getInventoryStack(player.getUniqueId());
		if (!gis.isEmpty()) gis.getFirst().execute(strings);
	}

	protected void saveStaticGuiInventory() {
		staticGuiInventory.put(this.getId(), this);
	}

	// ------------------- static

	private static GuiInventory newGuiInventory(Player player, String className, String arg) {
		try {
			Class<?> cls = Class.forName("com.avrgaming.civcraft.loreguiinventory." + className);
			Class<?> partypes[] = { Player.class, String.class };
			Object arglist[] = { player, arg };
			return (GuiInventory) cls.getConstructor(partypes).newInstance(arglist);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static GuiInventory getGuiInventory(Player player, String className, String arg) {
		String id = GuiInventory.buildId(className, arg);
		if (staticGuiInventory.containsKey(id))
			return staticGuiInventory.get(id);
		else
			return newGuiInventory(player, className, arg);
	}

	public static boolean isGuiInventory(Inventory inv) {
		for (ItemStack stack : inv) {
			if (stack == null) continue;
			return GuiItems.isGUIItem(stack);
		}
		return false;
	}

	public static void closeInventory(Player player) {
		player.closeInventory();
	}

	public static String buildId(String classname, String arg) {
		return arg == null ? classname : classname + "_" + arg;
	}

	public static ArrayDeque<GuiInventory> getInventoryStack(UUID uuid) {
		if (playersGuiInventoryStack.get(uuid) == null) playersGuiInventoryStack.put(uuid, new ArrayDeque<>());
		return playersGuiInventoryStack.get(uuid);
	}

	public static void setInventoryStack(UUID uuid, ArrayDeque<GuiInventory> gis) {
		GuiInventory.playersGuiInventoryStack.put(uuid, gis);
	}

}
