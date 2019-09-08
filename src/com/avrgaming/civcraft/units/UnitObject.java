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
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.items.UnitCustomMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class UnitObject extends SQLObject {

	public static final String TABLE_NAME = "UNITS";

	private int town_id;
	private Civilization civ_owner;
	private int exp;
	private int level;
	private String configUnitId;
	private ConfigUnit configUnit;
	private Resident lastResident;
	private HashMap<String, Integer> ammunitionSlots = new HashMap<>();
	private ComponentsManager compManager = new ComponentsManager();

	public UnitObject(String configUnitId, int town_id) {
		this.configUnitId = configUnitId;
		this.configUnit = UnitStatic.configUnits.get(this.configUnitId);
		this.town_id = town_id;
		this.civ_owner = CivGlobal.getTownFromId(this.town_id).getCiv();
		this.level = 0;
		this.exp = 0;
		this.lastResident = null;
		try {
			this.saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		UnitMaterial um = UnitStatic.getUnit(this.configUnit.class_name);
		um.initUnitObject(this);
	}

	public UnitObject(ResultSet rs) throws SQLException, InvalidNameException {
		this.load(rs);
	}

	//-----------------SQL begin

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			final String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" // 
					+ "`id` int(11) unsigned NOT NULL auto_increment," //
					+ "`configUnitId` VARCHAR(64) NOT NULL," //
					+ "`town_id` int(11) DEFAULT '0'," // 
					+ "`exp` int(11) DEFAULT '0'," //
					+ "`lastResidentId` int(11) DEFAULT '0'," //
					+ "`ammunitions` mediumtext DEFAULT NULL," //
					+ "`components` mediumtext DEFAULT NULL," //
					+ "PRIMARY KEY (`id`))";
			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}
	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));
		this.configUnitId = rs.getString("configUnitId");
		this.configUnit = UnitStatic.configUnits.get(this.configUnitId);
		this.town_id = rs.getInt("town_id");
		this.civ_owner = CivGlobal.getTownFromId(this.town_id).getCiv();
		this.exp = rs.getInt("exp");
		this.level = UnitStatic.calcLevel(this.exp);
		this.lastResident = CivGlobal.getResidentFromId(rs.getInt("lastResidentId"));

		String tempAmmun = rs.getString("ammunitions");
		if (tempAmmun != null) {
			String[] ammunitionsSplit = tempAmmun.split(",");
			for (int i = ammunitionsSplit.length - 1; i >= 0; i--) {
				String[] a = ammunitionsSplit[i].split("=");
				if (a.length >= 2) ammunitionSlots.put(a[0].toLowerCase(), Integer.parseInt(a[1]));
			}
		}
		compManager.loadComponents(rs.getString("components"));
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}
	@Override
	public void saveNow() throws SQLException {
		final HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("id", this.getId());
		hashmap.put("configUnitId", this.configUnitId);
		hashmap.put("town_id", this.town_id);
		hashmap.put("exp", this.exp);
		hashmap.put("lastResidentId", (this.lastResident == null ? 0 : this.lastResident.getId()));

		if (!ammunitionSlots.isEmpty()) hashmap.put("ammunitions", ammunitionSlots.toString().replace("{", "").replace("}", "").replace(" ", ""));

		hashmap.put("components", compManager.getSaveString());
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}
	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
	}
	//-----------------SQL end	

	@Override
	public String getName() {
		return this.configUnit.name;
	}

	public void setConfigUnit(ConfigUnit configUnit) {
		this.configUnit = configUnit;
	}
	public ConfigUnit getConfigUnit() {
		return configUnit;
	}

	public Civilization getCivilizationOwner() {
		return civ_owner;
	}

	public int getLevel() {
		return level;
	}
	public void addLevel() {
		this.level = this.level + 1;
	}
	public void removeLevel() {
		if (this.level > 0) this.level = this.level - 1;
		compManager.removeLevelUp();
	}

	public int getExp() {
		return this.exp;
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
	}
	public int getAmmunitionSlot(String mat) {
		return ammunitionSlots.get(mat);
	}

	public void setComponent(String key, Integer value) {
		compManager.setBaseComponent(key, value);
		this.save();
	}
	public Integer getComponent(String key) {
		if (compManager.getBaseComponents().containsKey(key.toLowerCase())) return compManager.getBaseComponents().get(key.toLowerCase());
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
			CivMessage.send(this.lastResident, CivColor.LightGrayBold + "   " + "Уровень вашего " + CivColor.PurpleBold + this.getConfigUnit().name
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
		if (level > UnitStatic.max_level) level = UnitStatic.max_level;
		return UnitStatic.first_exp * level + (level - 1) * level * UnitStatic.step_exp / 2;
	}
	public int getTotalExpThisLevel() {
		int level = this.level;
		if (level > UnitStatic.max_level) level = UnitStatic.max_level;
		return UnitStatic.first_exp * level + (level - 1) * level * UnitStatic.step_exp / 2;
	}

	public UnitMaterial getUnit() {
		return UnitStatic.getUnit(this.configUnitId);
	}

	public boolean validateUnitUse(Player player, ItemStack stack) {
		if (stack == null) return false;
		Resident resident = CivGlobal.getResident(player);
		if (this.civ_owner == null) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorInvalidOwner"));
			return false;
		}
		if (!this.civ_owner.equals(resident.getCiv())) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotOwner"));
			return false;
		}
		return true;
	}

	public void dressAmmunitions(Player player) {
		PlayerInventory inv = player.getInventory();
		UnitMaterial um;
		um = UnitStatic.getUnit(this.configUnitId);

		//создаю предметы амуниции
		HashMap<String, ItemStack> newStack = new HashMap<>();
		for (String equip : EquipmentElement.allEquipments) {
			Integer tir = compManager.getBaseComponentValue(equip);
			if (tir == 0) continue;
			String mat = um.getCustMatTir(equip, tir);
			if (mat == null) continue;
			newStack.put(equip, ItemManager.createItemStack(mat, 1));
		}

		ArrayList<ItemStack> removes = new ArrayList<>(); //список предметов которые занимают нужные слоты

		//проверяю все компоненты юнита
		for (String key : compManager.getComponentsKey()) {
			// Если это компонент предмет. Создаем его и ложим игроку
			UnitCustomMaterial ucmat = CustomMaterial.getUnitCustomMaterial(key);
			if (ucmat != null) {
				ItemStack stack = CustomMaterial.spawn(ucmat);
				Integer slot = ammunitionSlots.getOrDefault(ucmat, -1);
				if (slot == -1)
					UnitStatic.putItemSlot(inv, stack, 6, removes);
				else
					UnitStatic.putItemSlot(inv, stack, slot, removes);
				continue;
			}

			// если это атрибут амуниции, добавляем его
			ConfigUnitComponent cuc = UnitStatic.configUnitComponents.get(key);
			if (cuc != null) {
				String ammunition = cuc.ammunition;
				newStack.put(ammunition, UnitStatic.addAttribute(newStack.get(ammunition), key, this.getComponent(key)));
				continue;
			}

			// если ничего не найдено
			compManager.removeComponent(key);
			CivLog.warning("Компонент " + key + " у юнита id=" + this.getId() + " был удален, так как не найдена его обработка");
		}

		if (compManager.getLevelUp() > 0) {
			ItemStack stacklevelup = ItemManager.createItemStack("u_choiceunitcomponent", compManager.getLevelUp());
			UnitStatic.putItemSlot(inv, stacklevelup, 7, removes);
		}

		//ложу все предметы в слоты сохраненные в this.ammunitions или в стандартные из um.getSlot()
		for (String equip : newStack.keySet()) {
			ItemStack stack = newStack.get(equip);
			String mat = CustomMaterial.getMID(stack);
			Integer slot = ammunitionSlots.get(mat);
			if (slot == null) slot = um.getSlot(equip);
			UnitStatic.putItemSlot(inv, stack, slot, removes);
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
