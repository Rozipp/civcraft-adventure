package ua.rozipp.gui;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
public class GuiInventory implements CallbackInterface {
    public static final int MAX_INV_SIZE = 54;
    public static final int INV_COLUM_COUNT = 9;
    protected static Map<String, GuiInventory> staticGuiInventory = new HashMap<>();
    private static Map<UUID, ArrayDeque<GuiInventory>> playersGuiInventoryStack = new HashMap<>();
    private InventoryHolder holder;
    private Player player;
    private Resident resident;
    private Town town;
    private Civilization civ;
    private String arg;
    protected Inventory inventory = null;
    private HashMap<Integer, GuiItem> items = new HashMap<>();
    private String title = "";
    private Integer row = 6;

    public GuiInventory(InventoryHolder holder, Player player, String arg) throws CivException {
        this.holder = holder;
        this.player = player;
        if (player != null) {
            this.resident = CivGlobal.getResident(player);
            if (this.resident == null) throw new CivException("Resident not found");
        }
        this.arg = arg;
    }

    // ------------- builders

    private static GuiInventory newGuiInventory(Player player, String className, String arg) throws CivException {
        try {
            Class<?> cls = Class.forName("ua.rozipp.gui.guiinventory." + className);
            Class<?>[] partypes = {Player.class, String.class};
            Object[] arglist = {player, arg};
            return (GuiInventory) cls.getConstructor(partypes).newInstance(arglist);
        } catch (Exception e) {
            throw new CivException(e.getMessage());
        }
    }

    public static GuiInventory getGuiInventory(Player player, String className, String arg) throws CivException {
        String id = GuiInventory.buildId(className, arg);
        if (staticGuiInventory.containsKey(id))
            return staticGuiInventory.get(id);
        else {
            GuiInventory gi = newGuiInventory(player, className, arg);
            if (gi.getPlayer() == null) staticGuiInventory.put(id, gi);
            return gi;
        }
    }

    public static void openGuiInventory(Player player, String className, String arg) {
        try {
            GuiInventory gi = getGuiInventory(player, className, arg);
            gi.openInventory(player);
        } catch (CivException e) {
            e.printStackTrace();
            CivMessage.send(player, e.getMessage());
        }
    }

    // -------------- items

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

    public static GuiInventory getActiveGuiInventory(UUID uuid) {
        if (playersGuiInventoryStack.get(uuid) == null) return null;
        if (playersGuiInventoryStack.get(uuid).isEmpty()) return null;
        return playersGuiInventoryStack.get(uuid).getFirst();
    }

    public static void setInventoryStack(UUID uuid, ArrayDeque<GuiInventory> gis) {
        GuiInventory.playersGuiInventoryStack.put(uuid, gis);
    }

    public static void clearInventoryStack(UUID uuid) {
        GuiInventory.playersGuiInventoryStack.put(uuid, null);
    }

    // --------------- getters

    public GuiInventory setRow(int row) {
        this.row = row;
        if (this.row < 1) this.row = 1;
        if (this.row > 6) this.row = 6;
        return this;
    }

    public GuiInventory setTown(Town town) {
        this.town = town;
        return this;
    }

    // -------------------- Inventory

    public GuiInventory setCiv(Civilization civ) {
        this.civ = civ;
        return this;
    }

    public Integer addGuiItem(GuiItem item) {
        return addGuiItem(0, item);
    }

    public Integer addGuiItem(Integer slot, GuiItem item) {
        if (slot < 0 || slot >= size()) {
            int i;
            for (i = 0; i < size(); i++) {
                if (items.get(i) == null) break;
            }
            items.put(i, item);
            return i;
        }
        if (items.get(slot) == null) {
            items.put(slot, item);
            return slot;
        }
        if (items.size() >= size()) {
            items.put(slot, item);
            return slot;
        }
        int i;
        for (i = 0; i < size(); i++) {
            if (items.get(i) == null) break;
        }
        items.put(i, items.get(slot));
        items.put(slot, item);
        return slot;
    }

    public void addLastItem(UUID uuid) {
        ArrayDeque<GuiInventory> gis;
        gis = GuiInventory.getInventoryStack(uuid);
        if (gis.isEmpty()) {
            items.put(size(), GuiItem.newGuiItem()//
                    .setTitle("§c" + "Закрыть меню")//
                    .setMaterial(Material.EMPTY_MAP)//
                    .setAction("CloseInventory"));
        } else {
            items.put(size(), GuiItem.newGuiItem()//
                    .setTitle(CivSettings.localize.localizedString("loreGui_recipes_back"))//
                    .setMaterial(Material.MAP)//
                    .addLore(CivSettings.localize.localizedString("bookReborn_backTo", CivColor.White + gis.getFirst().getName()))//
                    .setAction("OpenBackInventory"));
        }
    }

    // ------------------- static

    public GuiItem getGuiItem(Integer i) {
        return items.get(i);
    }

    public Integer size() {
        return row * INV_COLUM_COUNT - 1;
    }

    public String getName() {
        return inventory.getName();
    }

    public String getTitle() {
        return title;
    }

    public GuiInventory setTitle(String title) {
        this.title = (title.length() > 32) ? title.substring(0, 32) : title;
        return this;
    }

    public void openInventory(Player player) {
        ArrayDeque<GuiInventory> gis = GuiInventory.getInventoryStack(player.getUniqueId());
        player.openInventory(getInventory(player));
        gis.push(this);
        GuiInventory.setInventoryStack(player.getUniqueId(), gis);
    }

    public Inventory getInventory(Player player) {
        if (inventory == null) try {
            inventory = Bukkit.createInventory(holder, size() + 1, title);
            addLastItem(player.getUniqueId());
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

    public boolean onItemToInventory(Cancellable event, Player player, Inventory inv, ItemStack stack) {
        return true;
    }

    public boolean onItemFromInventory(Cancellable event, Player player, Inventory inv, ItemStack stack) {
        if (event.isCancelled()) return true;
        if (GuiItem.isGUIItem(stack)) {
            String action = GuiItem.getAction(stack);
            if (action != null) GuiItem.processAction(action, stack, player);
            return true;
        }
        return true;
    }

    @Override
    public void execute(String... strings) {
        // XXX Children Override
        UUID uuid = (getPlayer() == null) ? UUID.fromString(strings[1]) : getPlayer().getUniqueId();

        ArrayDeque<GuiInventory> gis = getInventoryStack(uuid);
        if (!gis.isEmpty()) gis.getFirst().execute(strings);
    }

}
