package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.components.ProfesionalComponent;
import com.avrgaming.civcraft.construct.structures.Structure;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;

public class TownPeoplesManager {

	public static enum Prof {
		UNHAPPINES, // Несчасные жители не работают
		BUILDER, // Обычные рабочие строят здания
		NOTWORK, // Ими временно становяться строители которые ничего не строят. Они добывают всего по чучуть
		MINER, // Добывает материалы в шахтах
		FARMER, // Добывает пищю на фермах
		ARTIST, // Приносят культуру в театрах
		MERCHANT, // Приносят деньги в котеджах
		SCIENTIST,// Приносят науку в библиотеках
		UNIT // 
	}

	private Town town;

	private Integer foodsPeoplesOuttake = 1; // Каждый житель потребляет столько пищи в тик
	private Integer suppliesWorkerOuttake = 1; // Каждый Рабочий может взять на стройку столько материалов в тик

	private EnumMap<Prof, Integer> peoples = new EnumMap<>(Prof.class);
	private EnumMap<Prof, Integer> peoplesWork = new EnumMap<>(Prof.class);

	private List<Prof> peoplesPriority = new ArrayList<>();
	public TownPeoplesIntakeTable intakeTable;

	public TownPeoplesManager(Town town) {
		this.town = town;
		intakeTable = TownPeoplesIntakeTable.createTownPeoplesIntakeTable();
		peoplesPriority = Arrays.asList(Prof.BUILDER, Prof.MINER, Prof.MERCHANT, Prof.ARTIST, Prof.SCIENTIST, Prof.FARMER, Prof.UNIT, Prof.UNHAPPINES);
		for (Prof prof : Prof.values()) {
			this.peoples.put(prof, 0);
		}
	}

	public TownPeoplesManager(Town town, ResultSet rs) throws SQLException {
		this.town = town;
		intakeTable = TownPeoplesIntakeTable.createTownPeoplesIntakeTable();
		loadPeopesFromString(rs.getString("peoples"));
	}

	public void saveNow(HashMap<String, Object> hashmap) {
		hashmap.put("peoples", peopesToString());
	}

	public void onHourlyUpdate() {

	}

	/** Очищаю список всех работающих. Проверяю все здания. Если у здания есть ProfesionalComponent, то выдаю ему рабочего, и отмечаю isWork =
	 * true. Всех професионалов, кто не нашел здания для работы по профессии, отмечаем неработающими */
	public void onCivtickUpdate() {
		this.calcUnhappines();
		for (Prof prof : Prof.values())
			setPeoplesWorker(prof, 0);

		for (Structure struct : town.BM.getStructures()) {
			if (!struct.isEnabled()) continue;
			ProfesionalComponent pComponent = struct.getProfesionalComponent();
			if (pComponent == null) continue;
			Prof prof = pComponent.prof;
			int oldWork = getPeoplesWorker(prof);
			int get = Math.min(pComponent.count, getPeoplesProfCount(prof) - oldWork);
			if (get > 0) modifyPeoplesWorker(prof, get);
			pComponent.isWork = (get > 0);
		}

		setPeoplesWorker(Prof.NOTWORK, 0);
		for (Prof prof : Prof.values()) {
			if (prof == Prof.NOTWORK || prof == Prof.UNHAPPINES || prof == Prof.UNIT) continue;
			int notWork = getPeoplesProfCount(prof) - getPeoplesWorker(prof);
			modifyPeoplesWorker(Prof.NOTWORK, notWork);
		}
	}

	private void calcUnhappines() {
		town.SM.calcAttrHappiness();
		town.SM.calcAttrUnhappiness();
		int oldUnhappiness = getPeoplesProfCount(Prof.UNHAPPINES);
		peoples.put(Prof.UNHAPPINES, oldUnhappiness);
	}

	public String peopesToString() {
		String s = "";
		for (Prof prof : peoplesPriority) {
			s = s + prof.toString() + ":" + getPeoplesProfCount(prof) + ", ";
		}
		return s.substring(0, s.length() - 2);
	}

	private void loadPeopesFromString(String string) {
		CivLog.debug("loadPeopesFromString  " + string);
		peoples.clear();
		for (Prof prof : Prof.values()) {
			this.peoples.put(prof, 0);
		}
		for (String s : string.split(",")) {
			String[] split = s.trim().split(":");
			if (split.length != 2) continue;
			Prof prof = Prof.valueOf(split[0].toUpperCase());
			peoples.put(prof, Integer.parseInt(split[1]));
			peoplesPriority.add(prof);
		}
	}

	// ---------- getters

	public void modifyPeoplesWorker(Prof prof, int count) {
		peoplesWork.put(prof, peoplesWork.getOrDefault(prof, 0) + count);
	}

	public void setPeoplesWorker(Prof prof, int count) {
		peoplesWork.put(prof, count);
	}

	public int getPeoplesWorker(Prof prof) {
		return peoplesWork.getOrDefault(prof, 0);
	}

	public int getIntake(StorageType type) {
		int total = 0;
		for (Prof prof : Prof.values()) {
			total += intakeTable.getIntake(prof, type) * getPeoplesWorker(prof);
		}
		return total;
	}

	public int getFoodsOuttake() {
		return getPeoplesTotal() * foodsPeoplesOuttake;
	}

	public int getPeoplesTotal() {
		int peoplesTotal = 0;
		for (Integer count : peoples.values())
			peoplesTotal += count;
		return peoplesTotal;
	}

	public Map<Prof, Integer> getPeoples() {
		return peoples;
	}

	public Integer getPeoplesProfCount(Prof prof) {
		return peoples.get(prof);
	}

	public List<Prof> getPeoplesPriority() {
		return peoplesPriority;
	}

	public int progressBuildGetSupplies(int neadSupplies) {
		if (neadSupplies == 0) return 0;
		int neadBuilder = 1 + (neadSupplies - 1) / suppliesWorkerOuttake;
		int builderWork = Math.min(getPeoplesWorker(Prof.NOTWORK), neadBuilder);
		modifyPeoplesWorker(Prof.NOTWORK, -builderWork);
		modifyPeoplesWorker(Prof.BUILDER, +builderWork);
		return Math.min(builderWork * suppliesWorkerOuttake, neadSupplies);
	}

	public int calcHammerPerCivtick() {
		return getPeoplesProfCount(Prof.NOTWORK) * suppliesWorkerOuttake;
	}

	// ----------- private Peoples

	private int getMaxPeoplesWithProfesion(Prof prof) {
		return Integer.MAX_VALUE;
	}

	private int addProfPeoples(Prof prof, Integer count) {
		int oldCount = peoples.get(prof);
		int added = Math.min(getMaxPeoplesWithProfesion(prof) - oldCount, count);
		peoples.put(prof, oldCount + added);
		return added;
	}

	private int removeProfPeople(Prof prof, Integer count) {
		int oldCount = peoples.get(prof);
		int removed = Math.min(oldCount, count);
		peoples.put(prof, oldCount - removed);
		return removed;
	}

	private int addPeoplesWithPriority(int count) {
		int added = 0;
		for (Prof prof : peoplesPriority) {
			int add = addProfPeoples(prof, count);
			added += add;
			count -= add;
			if (count <= 0) break;
		}
		return added;
	}

	private int removePeoplesWithPriority(int count) {
		int removed = 0;
		for (Prof prof : peoplesPriority) {
			int rem = removeProfPeople(prof, count);
			removed += rem;
			count -= rem;
			if (count <= 0) break;
		}
		return removed;
	}

	// ------------- public Peoples

	/** Родилось count житель */
	public void bornPeoples(int count) {
		this.addPeoplesWithPriority(count);
		CivMessage.sendCiv(town.getCiv(), ChatColor.GREEN + "Население города " + town.getName() + " выросло до " + getPeoplesTotal() + " жителей");
	}

	/** Умерло count жителей */
	public void deadPeoples(int count) {
		if (this.getPeoplesTotal() <= count) return;
		this.removePeoplesWithPriority(count);
		CivMessage.sendCiv(town.getCiv(), ChatColor.RED + "Население города " + town.getName() + " уменьшилось до " + getPeoplesTotal() + " жителей");
	}

	/** Уволить работников с должности */
	public void dismissPeoples(Prof prof, Integer count) {
		if (prof == Prof.BUILDER) return;
		int dismissed = removeProfPeople(prof, count);
		addProfPeoples(Prof.BUILDER, dismissed);
	}

	/** Назначить работников на должность */
	public void hirePeoples(Prof prof, Integer count) {
		if (prof == Prof.BUILDER) return;
		int dismissed = removeProfPeople(Prof.BUILDER, count);
		addProfPeoples(prof, dismissed);
	}

	/** Установить найвысший приоритет на автоматическое изменение указанную професия */
	public void setHigestPriority(Prof prof) {
		ArrayList<Prof> newPeoplesPriority = new ArrayList<>();
		newPeoplesPriority.add(prof);
		for (Prof pr : peoplesPriority) {
			if (!pr.equals(prof)) newPeoplesPriority.add(pr);
		}
		this.peoplesPriority = newPeoplesPriority;
	}

}
