package ua.rozipp.gui;

import java.lang.reflect.Constructor;
import java.util.*;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.util.ItemManager;

import com.avrgaming.gpl.AttributeUtil;

public class GuiItem {

	private ItemStack stack = null; // Готовий предмет
	private ItemStack stackOrigin = null; // Предмет оригинал

	// Параметри GuiItem предмета
	private int amount = 1;
	private String title = null;
	private final List<String> lore = new ArrayList<>();
	private String action = null;
	private final Map<String, String> data = new HashMap<>();
	private boolean dirt = true; // Флаг требующий обновления предмета в переменной private ItemStack stack

	public GuiItem() {
	}

	// ------------ builders

	public GuiItem setStack(ItemStack stack) {
		this.stackOrigin = stack;
		return this;
	}

	public GuiItem setMaterial(Material mat) {
		this.stackOrigin = ItemManager.createItemStack(mat, amount);
		return this;
	}

	public GuiItem setAmount(int amount) {
		if (stackOrigin == null) this.amount = amount;
			else stackOrigin.setAmount(amount);
		return this;
	}

	public GuiItem setTitle(String title) {
		this.title = title;
		this.dirt = true;
		return this;
	}

	public GuiItem setLore(String... lore) {
		this.lore.clear();
		return addLore(lore);
	}

	public GuiItem addLore(String... lore) {
		Collections.addAll(this.lore, lore);
		this.dirt = true;
		return this;
	}

	public GuiItem setAction(String action) {
		this.action = action;
		this.dirt = true;
		return this;
	}

	public GuiItem setActionData(String key, String value) {
		this.data.put(key, value);
		this.dirt = true;
		return this;
	}

	/** Добавляет предмету action "CallbackGui", По нажатию на guiItem возвращает на GuiInventory.execute(String... strings) строку data */
	public GuiItem setCallbackGui(String data) {
		this.action = "CallbackGui";
		this.data.put("data", data);
		return this;
	}

	/** Добавляеть предмету action "OpenInventory". По нажатию на guiItem открываеться инвентарь с именем "className" которому передається параметр "arg" */
	public GuiItem setOpenInventory(String className, String arg) {
		this.action = "OpenInventory";
		this.data.put("className", className);
		if (arg != null) this.data.put("arg", arg);
		return this;
	}

	// -------------- getStack

	/**Конструктор готового стака из этого объекта*/
	public ItemStack getStack() {
		if (stack == null || dirt) {
			if (stackOrigin == null || stackOrigin.getType() == Material.AIR) stackOrigin = ItemManager.createItemStack(Material.WOOL, amount);
			stack = stackOrigin.clone();
			dirt = false;
		}
		AttributeUtil attrs = new AttributeUtil(stack);
		if (title == null)
			attrs.setCivCraftProperty("GUI", "" + ItemManager.getTypeId(stack));
		else {
			attrs.setCivCraftProperty("GUI", title);
			attrs.addLore("GUI: " + title); // TODO for debag
			attrs.setName(title);
		}
		attrs.setLore(lore);
		if (action != null) {
			attrs.setCivCraftProperty("GUI_ACTION", action);
			attrs.addLore("GUI_ACTION " + action); // TODO for debag
		}

		if (!data.isEmpty()) {
			for (String key : data.keySet()) {
				attrs.setCivCraftProperty("GUI_ACTION_DATA:" + key, data.get(key));
				attrs.addLore("GUI_ACTION_DATA:" + key + " " + data.get(key)); // TODO for debag
			}
		}
		stack = attrs.getStack();
		ItemMeta meta = stack.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		stack.setItemMeta(meta);
		
		return stack;
	}

	//----------------------------- static

	public static final int INV_ROW_COUNT = 9;

	public static GuiItem newGuiItem(ItemStack stack) {
		return (new GuiItem()).setStack(stack);
	}

	public static GuiItem newGuiItem() {
		return new GuiItem();
	}

	public static ItemStack addGui(ItemStack stack, String name) {
		return ItemManager.setProperty(stack, "GUI", name);
	}
	public static ItemStack addGuiAction(ItemStack stack, String action) {
		return ItemManager.setProperty(stack, "GUI_ACTION", action);
	}

	public static ItemStack addGuiAtributes(ItemStack stack, String name,  String action) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setCivCraftProperty("GUI", name);
		attrs.addLore("GUI:" + name);
		if (action!= null) {
			attrs.setCivCraftProperty("GUI_ACTION", action);
			attrs.addLore("GUI_ACTION:" + action);
		}

		return attrs.getStack();
	}

	public static ItemStack removeGuiAtributes(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.removeCivCraftProperty("GUI");
		attrs.removeCivCraftProperty("GUI_ACTION");
		return attrs.getStack();
	}

	public static boolean isGUIItem(ItemStack stack) {
		return ItemManager.getProperty(stack, "GUI") != null;
	}

	public static String getAction(ItemStack stack) {
		return ItemManager.getProperty(stack, "GUI_ACTION");
	}

	public static String getActionData(ItemStack stack, String key) {
		return ItemManager.getProperty(stack, "GUI_ACTION_DATA:" + key);
	}

	public static void processAction(String action, ItemStack stack, Player player) {
		/* Get class name from reflection and perform assigned action */
		try {
			Class<?> clazz = Class.forName("ua.rozipp.gui.action." + action);
			Constructor<?> constructor = clazz.getConstructor();
			GuiAction instance = (GuiAction) constructor.newInstance();
			instance.performAction(player, stack);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}