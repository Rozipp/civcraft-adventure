package com.avrgaming.civcraft.units;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	private int exp;
	private int level;
	private String configUnitId;
	private ConfigUnit configUnit;
	private Town townOwner;
	private Resident lastResident = null;
	private int lastHashCode = 0;
	private long lastActivate = 0;
	private HashMap<String, Integer> ammunitionSlots = new HashMap<>();

	private HashMap<String, Integer> totalComponents = new HashMap<>();
	private HashMap<Integer, List<String>> levelComponents = new HashMap<>();
	private HashMap<Equipments, String> equipments = new HashMap<>();
	private Integer levelUp = 0;

	public UnitObject(String configUnitId, Town town) {
		this.configUnitId = configUnitId;
		this.configUnit = UnitStatic.configUnits.get(this.configUnitId);
		this.townOwner = town;
		this.level = 0;
		this.exp = 0;
		try {
			this.saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		UnitMaterial um = UnitStatic.getUnit(this.configUnit.id);
		um.initUnitObject(this);
		this.townOwner.unitInventory.addUnit(this.getId());
	}

	public UnitObject(ResultSet rs) throws CivException, SQLException {
		try {
			this.load(rs);
		} catch (Exception e) {
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
					+ "`lastHashCode` int(11) DEFAULT 0," //
					+ "`lastActivate` BIGINT NOT NULL DEFAULT 0," //
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
				CivLog.info("CLEANING town_id = " + rs.getInt("town_id"));
				this.delete();
			}
			throw new CivException("Not fount town ID (" + rs.getInt("town_id") + ") to load this town chunk(" + this.getId());
		} else this.townOwner.unitInventory.addUnit(this.getId());
		this.exp = rs.getInt("exp");
		this.level = UnitStatic.calcLevel(this.exp);
		this.lastResident = CivGlobal.getResident(rs.getString("lastResident"));
		this.lastHashCode = rs.getInt("lastHashCode");
		this.lastActivate = rs.getLong("lastActivate");

		String tempAmmun = rs.getString("ammunitions");
		if (tempAmmun != null) {
			String[] ammunitionsSplit = tempAmmun.split(",");
			for (int i = ammunitionsSplit.length - 1; i >= 0; i--) {
				String[] a = ammunitionsSplit[i].split("=");
				if (a.length >= 2) ammunitionSlots.put(a[0].toLowerCase(), Integer.parseInt(a[1]));
			}
		}
		loadComponents(rs.getString("components"));
	}

	public void loadComponents(String sourceString) {
		if (sourceString == null || sourceString.isEmpty()) return;

		String[] source = sourceString.split("@");

		this.levelUp = Integer.valueOf(source[0]);

		String[] aqStrings = source[1].split(",");
		for (String eq : aqStrings) {
			String[] keys = eq.split(":");
			this.equipments.put(Equipments.valueOf(keys[0]), keys[1]);
		}

		String[] levelSplits = source[2].split(";");
		for (String levelComp : levelSplits) {
			String[] levelCompSplit = levelComp.split(":");

			Integer level = Integer.parseInt(levelCompSplit[0]);

			String[] componentsSplit = levelCompSplit[1].replace("[", "").replace("]", "").split(",");
			if (componentsSplit.length < 1) continue;
			List<String> newComponents = new ArrayList<>();
			for (String c : componentsSplit) {
				newComponents.add(c);
			}
			this.levelComponents.put(level, newComponents);
		}
		for (Integer level : levelComponents.keySet()) {
			List<String> comp = levelComponents.get(level);
			for (String key : comp) {
				this.totalComponents.put(key, this.totalComponents.getOrDefault(key, 0) + 1);
			}
		}
	}

	@Override
	public void saveNow() throws SQLException {
		final HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("id", this.getId());
		hashmap.put("configUnitId", this.configUnitId);
		hashmap.put("town_id", this.townOwner.getId());
		hashmap.put("exp", this.exp);
		hashmap.put("lastResident", (this.lastResident == null ? 0 : this.lastResident.getName()));
		hashmap.put("lastHashCode", this.lastHashCode);
		hashmap.put("lastActivate", this.lastActivate);

		if (!ammunitionSlots.isEmpty()) hashmap.put("ammunitions", ammunitionSlots.toString().replace("{", "").replace("}", "").replace(" ", ""));

		hashmap.put("components", getSaveComponentsString());
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	public String getSaveComponentsString() {
		if (this.levelComponents.isEmpty()) return "";

		String sss = levelUp.toString() + "@";
		// Add equipments
		for (Equipments eq : Equipments.values()) {
			if (!this.equipments.containsKey(eq)) continue;
			String comps = this.equipments.get(eq);
			sss = sss + eq.name() + ":" + comps + ",";
		}
		if (sss.charAt(sss.length() - 1) == ',') sss = sss.substring(0, sss.length() - 1);

		sss = sss + "@";
		// Add levelComponents
		for (Integer i = 0; i <= 100; i++) {
			if (!this.levelComponents.containsKey(i)) continue;
			List<String> comps = this.levelComponents.get(i);
			sss = sss + i + ":" + (comps.isEmpty() ? "" : comps.toString().replace(" ", "")) + ";";
		}
		if (sss.charAt(sss.length() - 1) == ';') sss = sss.substring(0, sss.length() - 1);
		return sss;
	}

	@Override
	public void delete() throws SQLException {
		if (townOwner != null) this.townOwner.unitInventory.removeUnit(this.getId());
		CivGlobal.removeUnitObject(this);
		SQL.deleteNamedObject(this, TABLE_NAME);
	}
	// -----------------SQL end

	@Override
	public String getName() {
		return this.configUnit.name;
	}

	// ------------------exp level
	public void addExp(Integer exp) {
		this.exp = this.exp + exp;
		while (this.exp >= this.getTotalExpToLevel(level + 1)) {
			addLevel();
			addLevelUp();
			CivMessage.send(this.lastResident, CivColor.LightGrayBold + "   " + "Уровень вашего " + CivColor.PurpleBold + this.getConfigUnit().name + CivColor.LightGrayBold + " поднялся до " + CivColor.LightGray + this.getLevel());
		}
		this.save();
	}

	public void removeExp(Integer exp) {
		this.exp = this.exp - exp;
		while (this.exp < this.getTotalExpToLevel(level)) {
			removeLevel();
		}
		this.save();
	}

	public float getPercentExpToNextLevel() {
		return ((float) exp - this.getTotalExpToLevel(level)) / this.getExpToNextLevel();
	}

	public int getExpToNextLevel() {
		return UnitStatic.first_exp + (level) * UnitStatic.step_exp;
	}

	public int getTotalExpToLevel(int level) {
		if (level > UnitStatic.max_level) level = UnitStatic.max_level;
		return UnitStatic.first_exp * level + (level - 1) * level * UnitStatic.step_exp / 2;
	}

	public void addLevel() {
		this.level = this.level + 1;
	}

	public void removeLevel() {
		if (level > 0) {
			level--;
			List<String> removeComponents = levelComponents.get(level);
			for (String key : removeComponents) {
				levelUp = levelUp + 1;
				int oldLevel = totalComponents.getOrDefault(key, 0);
				if (oldLevel > 0) totalComponents.put(key, oldLevel - 1);
				CivMessage.send(this.lastResident, "Ваш юнит забыл " + key + " уровень " + oldLevel);
			}
			levelComponents.remove(level);
			removeLevelUp();
		}
	}

	// --------------------
	public void used(Resident res, ItemStack is) {
		this.lastResident = res;
		this.lastActivate = System.currentTimeMillis();
		this.lastHashCode = is.hashCode();
		save();
	}

	public boolean validLastActivate() {
		if (lastHashCode == 0) return false;
		if (lastActivate == 0) return true;
		return (System.currentTimeMillis() - lastActivate) < UnitStatic.unitTimeDiactivate * 60000;
	}

	public boolean validLastHashCode(ItemStack is) {
		return lastHashCode == is.hashCode();
	}

	public void setAmunitionSlot(String mat, Integer slot) {
		ammunitionSlots.put(mat, slot);
		save();
	}

	public int getAmmunitionSlot(String mat) {
		return ammunitionSlots.get(mat);
	}

	// ------------------ levelUp
	public void addLevelUp() {
		this.levelUp++;
	}

	public void removeLevelUp() {
		if (this.levelUp > 0) this.levelUp--;
	}

	public Integer getLevelUp() {
		return this.levelUp;
	}

	// ------------equipments
	public void setEquipment(Equipments eq, String mid) {
		equipments.put(eq, mid);
	}

	public String getEquipment(Equipments eq) {
		return equipments.get(eq);
	}

	// ------------- Components
	public void addComponent(String key) {
		if (!levelComponents.containsKey(level)) levelComponents.put(level, new ArrayList<>());
		levelComponents.get(level).add(key);

		totalComponents.put(key, totalComponents.getOrDefault(key, 0) + 1);
		int newlevel = totalComponents.get(key);
		if (newlevel > 1)
			CivMessage.send(this.lastResident, "Ваш юнит изучил " + key);
		else CivMessage.send(this.lastResident, "Ваш юнит изучил " + key + " уровень " + newlevel);
	}

	public Integer getComponentValue(String key) {
		return totalComponents.getOrDefault(key, 0);
	}

	public Boolean hasComponent(String key) {
		return totalComponents.containsKey(key);
	}

	// ---------------unit
	public UnitMaterial getUnit() {
		return UnitStatic.getUnit(this.configUnitId);
	}

	public boolean validateUnitUse(Player player, ItemStack stack) {
		if (stack == null) return false;
		Resident resident = CivGlobal.getResident(player);
		if (this.townOwner == null) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorInvalidOwner"));
			return false;
		}
		if (resident.getTown() == null) {
			CivMessage.sendError(player, "У вас нет города");
			return false;
		}
		if (!resident.getTown().equals(this.townOwner)) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("settler_errorNotOwner"));
			return false;
		}
		return true;
	}

	public void dressAmmunitions(Player player) {
		PlayerInventory inv = player.getInventory();

		// создаю предметы амуниции
		HashMap<Equipments, ItemStack> newEquipments = new HashMap<>();
		for (Equipments equip : Equipments.values()) {
			String mid = equipments.get(equip);
			if (mid == null || mid.isEmpty()) continue;
			ItemStack is = ItemManager.createItemStack(mid, 1);
			newEquipments.put(equip, is);
		}

		HashMap<String, Integer> newItems = new HashMap<>();
		// проверяю все компоненты юнита
		for (String key : totalComponents.keySet()) {
			// Если это компонент предмет. Создаем его
			UnitCustomMaterial ucmat = CustomMaterial.getUnitCustomMaterial(key);
			if (ucmat != null) {
				newItems.put(ucmat.getConfigId(), ammunitionSlots.getOrDefault(ucmat.getConfigId(), ucmat.getSocketSlot()));
				continue;
			}

			// если это атрибут амуниции, добавляем его
			ConfigUnitComponent cuc = UnitStatic.configUnitComponents.get(key);
			if (cuc != null) {
				newEquipments.put(cuc.ammunition, UnitStatic.addAttribute(newEquipments.get(cuc.ammunition), key, getComponentValue(key)));
				continue;
			}

			// если ничего не найдено
			CivLog.warning("Компонент " + key + " у юнита id=" + this.getId() + " был удален, так как не найдена его обработка");
		}
		if (getLevelUp() > 0) {
			newItems.put("u_choiceunitcomponent", ammunitionSlots.getOrDefault("u_choiceunitcomponent", 7)); // ItemManager.createItemStack("u_choiceunitcomponent", getLevelUp()
		}

		//раскладываем созданные предметы по слотам, сохраненных в this.ammunitions или в стандартные из um.getSlot() 
		HashMap<Integer, ItemStack> newSlots = new HashMap<>();
		for (Equipments equip : newEquipments.keySet()) {
			newSlots.put(ammunitionSlots.getOrDefault(equipments.get(equip), equip.getSlot()), newEquipments.get(equip));
		}
		for (String mid : newItems.keySet()) {
			Integer slot = newItems.get(mid);
			if (newSlots.containsKey(slot)) {
				while (newSlots.get(slot) != null) {
					slot = slot + 1;
					if (slot > 36) slot = 0;
				}
			}
			newSlots.put(slot, ItemManager.createItemStack(mid, 1));
		}

		ArrayList<ItemStack> removes = new ArrayList<>(); // список предметов которые занимают нужные слоты
		for (Integer slot : newSlots.keySet()) {
			UnitStatic.putItemToPlayerSlot(inv, newSlots.get(slot), slot, removes);
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

}
