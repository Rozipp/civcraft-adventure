package com.avrgaming.civcraft.units;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitObject extends SQLObject {

	public static final String TABLE_NAME = "UNITS";

	private Town townOwner;
	private int exp;
	private int level;
	private String configUnitId;
	private ConfigUnit configUnit;
	private Resident lastResident;
	private HashMap<String, Integer> ammunitionSlots = new HashMap<>();
	private ComponentsManager compManager = new ComponentsManager();

	public UnitObject(String configUnitId, Town town) {
		this.configUnitId = configUnitId;
		this.configUnit = UnitStatic.configUnits.get(this.configUnitId);
		this.townOwner = town;
		this.level = 0;
		this.exp = 0;
		this.lastResident = null;
		try {
			this.saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		UnitMaterial um = UnitStatic.getUnit(this.configUnit.id);
		um.initUnitObject(this);
		this.townOwner.addUnitToList(this.getId());
	}

	public UnitObject(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		try {
			this.load(rs);
		} catch (CivException e) {
			// TODO Автоматически созданный блок catch
			e.printStackTrace();
			this.delete();
			throw new CivException(e.getMessage());
		}
	}

	// -----------------SQL begin

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			final String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" //
					+ "`id` int(11) unsigned NOT NULL auto_increment," //
					+ "`configUnitId` VARCHAR(64) NOT NULL," //
					+ "`town_id` int(11) DEFAULT '0'," //
					+ "`exp` int(11) DEFAULT '0'," //
					+ "`lastResident` VARCHAR(64) DEFAULT NULL," //
					+ "`ammunitions` mediumtext DEFAULT NULL," //
					+ "`components` mediumtext DEFAULT NULL," //
					+ "PRIMARY KEY (`id`))";
			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}

	public void load(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.setId(rs.getInt("id"));
		this.configUnitId = rs.getString("configUnitId");
		this.configUnit = UnitStatic.configUnits.get(this.configUnitId);
		this.townOwner = CivGlobal.getTownFromId(rs.getInt("town_id"));
		if (this.townOwner == null) {
			CivLog.warning("TownChunk tried to load without a town...");
			if (CivGlobal.isHaveTestFlag("cleanupDatabase")) {
				CivLog.info("CLEANING");
				this.delete();
			}
			throw new CivException(
					"Not fount town ID (" + rs.getInt("town_id") + ") to load this town chunk(" + this.getId());
		} else
			this.townOwner.addUnitToList(this.getId());
		this.exp = rs.getInt("exp");
		this.level = UnitStatic.calcLevel(this.exp);
		this.lastResident = CivGlobal.getResident(rs.getString("lastResident"));

		String tempAmmun = rs.getString("ammunitions");
		if (tempAmmun != null) {
			String[] ammunitionsSplit = tempAmmun.split(",");
			for (int i = ammunitionsSplit.length - 1; i >= 0; i--) {
				String[] a = ammunitionsSplit[i].split("=");
				if (a.length >= 2)
					ammunitionSlots.put(a[0].toLowerCase(), Integer.parseInt(a[1]));
			}
		}
		compManager.loadComponents(rs.getString("components"));
	}

	@Override
	public void saveNow() throws SQLException {
		final HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("id", this.getId());
		hashmap.put("configUnitId", this.configUnitId);
		hashmap.put("town_id", this.townOwner.getId());
		hashmap.put("exp", this.exp);
		hashmap.put("lastResident", (this.lastResident == null ? 0 : this.lastResident.getName()));

		if (!ammunitionSlots.isEmpty())
			hashmap.put("ammunitions", ammunitionSlots.toString().replace("{", "").replace("}", "").replace(" ", ""));

		hashmap.put("components", compManager.getSaveString());
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		if (townOwner != null)
			this.townOwner.removeUnitToList(this.getId());
		CivGlobal.removeUnitObject(this);
		SQL.deleteNamedObject(this, TABLE_NAME);
	}
	// -----------------SQL end

	@Override
	public String getName() {
		return this.configUnit.name;
	}

	public void addLevel() {
		this.level = this.level + 1;
	}

	public void removeLevel() {
		if (this.level > 0)
			this.level = this.level - 1;
		compManager.removeLevelUp();
	}

	public void setLastResident(Resident res) {
		this.lastResident = res;
		save();
	}

	public Resident getLastResident() {
		return this.lastResident;
	}

	public void setAmunitionSlot(String mat, Integer slot) {
		ammunitionSlots.put(mat, slot);
		save();
	}

	public int getAmmunitionSlot(String mat) {
		return ammunitionSlots.get(mat);
	}

	public void setComponent(String key, Integer value) {
		compManager.setBaseComponent(key, value);
		this.save();
	}

	public Integer getComponent(String key) {
		if (compManager.getBaseComponents().containsKey(key.toLowerCase()))
			return compManager.getBaseComponents().get(key.toLowerCase());
		return compManager.getComponentValue(key);
	}

	public boolean hasComponent(String key) {
		return compManager.hasComponent(key);
	}

	public void addComponent(String key, Integer value) {
		compManager.addComponenet(this.level, key, value);
		CivMessage.send(this.lastResident, "Ваш юнит получил новый компонент " + key + "+" + value);
	}

	public void removeLevelUp() {
		this.compManager.removeLevelUp();
	}

	public void removeLastComponent() {
		Collection<String> ss = compManager.removeLastComponents(this.level);
		for (String s : ss) {
			CivMessage.send(this.lastResident, "Ваш юнит потерял компонент " + s);
		}
	}

	public void addExp(Integer exp) {
		this.exp = this.exp + exp;
		while (this.exp >= this.getTotalExpToNextLevel()) {
			addLevel();
			compManager.addLevelUp();
			CivMessage.send(this.lastResident,
					CivColor.LightGrayBold + "   " + "Уровень вашего " + CivColor.PurpleBold + this.getConfigUnit().name
							+ CivColor.LightGrayBold + " поднялся до " + CivColor.LightGray + this.getLevel());
		}
		this.save();
	}

	public void removeExp(Integer exp) {
		this.exp = this.exp - exp;
		while (this.exp < this.getTotalExpThisLevel()) {
			removeLastComponent();
			removeLevel();
		}
		this.save();
	}

	public float getFloatExp() {
		return ((float) exp - this.getTotalExpThisLevel()) / this.getExpToNextLevel();
	}

	public int getExpToNextLevel() {
		return UnitStatic.first_exp + (level) * UnitStatic.step_exp;
	}

	public int getTotalExpToNextLevel() {
		int level = this.level + 1;
		if (level > UnitStatic.max_level)
			level = UnitStatic.max_level;
		return UnitStatic.first_exp * level + (level - 1) * level * UnitStatic.step_exp / 2;
	}

	public int getTotalExpThisLevel() {
		int level = this.level;
		if (level > UnitStatic.max_level)
			level = UnitStatic.max_level;
		return UnitStatic.first_exp * level + (level - 1) * level * UnitStatic.step_exp / 2;
	}

	public UnitMaterial getUnit() {
		return UnitStatic.getUnit(this.configUnitId);
	}

	public boolean validateUnitUse(Player player, ItemStack stack) {
		if (stack == null)
			return false;
		Resident resident = CivGlobal.getResident(player);
		if (this.townOwner == null) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorInvalidOwner"));
			return false;
		}
		if (resident.getTown() == null) {
			CivMessage.sendError(player, "У вас нет города");
			return false;
		}
		if (!resident.getCiv().equals(this.townOwner.getCiv())) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotOwner"));
			return false;
		}
		return true;
	}

	class NewStackList {
		HashMap<String, ItemStack> newStack = new HashMap<>();
		ArrayList<String> newStackSlot;

		public NewStackList() {
			newStackSlot = new ArrayList<>();
			for (int i = 0; i < 42; i++) {
				newStackSlot.add(null);
			}
		}

		public void addItem(String id, ItemStack stack, Integer slot) {
			newStack.put(id, stack);
			Integer oldslot = ammunitionSlots.getOrDefault(id, slot);
			if (newStackSlot.get(oldslot) == null) {
				newStackSlot.set(oldslot, id);
				return;
			}
			while (newStackSlot.get(slot) != null) {
				slot = slot + 1;
				if (slot > 40)
					slot = 0;
			}
			newStackSlot.set(slot, id);
		}

		public ItemStack getItemStack(Integer slot) {
			String mat = newStackSlot.get(slot);
			return getItemStack(mat);
		}

		public ItemStack getItemStack(String mat) {
			return newStack.get(mat);
		}

		public void putAmunitionComponent(String ammunition, String key, Integer value) {
			ItemStack stack = newStack.get(ammunition);
			newStack.put(ammunition, UnitStatic.addAttribute(stack, key, value));
		}
	}

	public void dressAmmunitions(Player player) {
		PlayerInventory inv = player.getInventory();
		UnitMaterial um;
		um = UnitStatic.getUnit(this.configUnitId);

		// создаю предметы амуниции
		NewStackList newStack = new NewStackList();
		for (String equip : EquipmentElement.allEquipments) {
			Integer tir = compManager.getBaseComponentValue(equip);
			String mat = um.getAmuntMatTir(equip, tir);
			if (mat == null || mat == "")
				continue;
			newStack.addItem(equip, ItemManager.createItemStack(mat, 1), um.getSlot(equip));
		}

		// проверяю все компоненты юнита
		for (String key : compManager.getComponentsKey()) {
			// Если это компонент предмет. Создаем его
			UnitCustomMaterial ucmat = CustomMaterial.getUnitCustomMaterial(key);
			if (ucmat != null) {
				newStack.addItem(key, CustomMaterial.spawn(ucmat), ucmat.getSocketSlot());
				continue;
			}

			// если это атрибут амуниции, добавляем его
			ConfigUnitComponent cuc = UnitStatic.configUnitComponents.get(key);
			if (cuc != null) {
				newStack.putAmunitionComponent(cuc.ammunition, key, this.getComponent(key));
				continue;
			}

			// если ничего не найдено
			CivLog.warning("Компонент " + key + " у юнита id=" + this.getId()
					+ " был удален, так как не найдена его обработка");
		}
		if (compManager.getLevelUp() > 0) {
			newStack.addItem("u_choiceunitcomponent",
					ItemManager.createItemStack("u_choiceunitcomponent", compManager.getLevelUp()), 7);
		}

		ArrayList<ItemStack> removes = new ArrayList<>(); // список предметов которые занимают нужные слоты
		// ложу все предметы в слоты сохраненные в this.ammunitions или в стандартные из
		// um.getSlot()
		for (Integer slot = 0; slot <= 40; slot++) {
			UnitStatic.putItemSlot(inv, newStack.getItemStack(slot), slot, removes);
		}

		// Try to re-add any removed items.
		for (ItemStack is : removes) {
			HashMap<Integer, ItemStack> leftovers = inv.addItem(is);
			for (ItemStack s : leftovers.values())
				player.getWorld().dropItem(player.getLocation(), s);
		}
	}

	public void rebuildUnitItem(Player player) {
		UnitStatic.removeChildrenItems(player);
		this.dressAmmunitions(player);
	}

	public void printAllComponents(Player player) {
		for (String key : this.compManager.getComponentsKey()) {
			String ss = "компонент \"";
			ss = CivColor.AddTabToString(ss, key, 10);
			ss = ss + "\" имеет значение = ";
			ss = CivColor.AddTabToString(ss, "" + this.getComponent(key), 2);
			CivMessage.send(player, ss);
		}
	}
}
