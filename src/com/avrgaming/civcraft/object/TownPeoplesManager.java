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

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;

public class TownPeoplesManager {

	public static enum Prof {
		Unhappines, // Несчасные жители не работают
		Worker, // Обычные рабочие строят здания
		WorkerNotWork, // Ими временно становяться рабочие которые ничего не строят
		Farmer, // Добывает пищю
		Engineer, // приносят продукцию в шахтах
		Artist, // Приносят культуру в театрах
		Merchant, // Приносят деньги в котеджах
		Scientist// Приносят науку в библиотеках
	}

	private Town town;

	private Integer foodsPeoplesOuttake = 1; // Каждый житель потребляет столько пищи в тик
	private Integer hammersWorkerOuttake = 1; // Каждый Рабочий может взять на стройку столько материалов в тик

	private Integer peoplesTotal = 0; // Все жители
	private Map<Prof, Integer> peoples = new HashMap<>();
	private List<Prof> peoplesPriority = new ArrayList<>();

	private EnumMap<StorageType, EnumMap<Prof, Integer>> intakeTable;

	public TownPeoplesManager(Town town) {
		this.town = town;
		intakeTable = createNewIntakeTable();
		peoplesPriority = Arrays.asList(Prof.Worker, Prof.Engineer, Prof.Merchant, Prof.Artist, Prof.Scientist, Prof.Farmer, Prof.Unhappines);
		for (Prof prof : Prof.values()) {
			this.peoples.put(prof, 0);
		}

		this.peoplesTotal += this.addPeoplesWithPriority(1);
	}

	public TownPeoplesManager(Town town, ResultSet rs) throws SQLException {
		this.town = town;
		intakeTable = createNewIntakeTable();
		this.peoplesTotal = rs.getInt("peoplesTotal");
		loadPeopesFromString(rs.getString("peoples"));
	}

	private EnumMap<StorageType, EnumMap<Prof, Integer>> createNewIntakeTable() {
		return new EnumMap<>(Map.of(//
				StorageType.Food, /*    */new EnumMap<>(Map.of(Prof.Unhappines, 0, Prof.Worker, 1, Prof.WorkerNotWork, 2, Prof.Farmer, 4, Prof.Engineer, 1, Prof.Artist, 1, Prof.Merchant, 1, Prof.Scientist, 1)), //
				StorageType.Hammer, /*  */new EnumMap<>(Map.of(Prof.Unhappines, 0, Prof.Worker, 0, Prof.WorkerNotWork, 2, Prof.Farmer, 0, Prof.Engineer, 10, Prof.Artist, 0, Prof.Merchant, 0, Prof.Scientist, 0)), //
				StorageType.Culture, /* */new EnumMap<>(Map.of(Prof.Unhappines, 0, Prof.Worker, 0, Prof.WorkerNotWork, 2, Prof.Farmer, 0, Prof.Engineer, 0, Prof.Artist, 10, Prof.Merchant, 0, Prof.Scientist, 0)), //
				StorageType.Econ, /*    */new EnumMap<>(Map.of(Prof.Unhappines, 0, Prof.Worker, 0, Prof.WorkerNotWork, 2, Prof.Farmer, 0, Prof.Engineer, 0, Prof.Artist, 0, Prof.Merchant, 10, Prof.Scientist, 0)), //
				StorageType.Beakers, /* */new EnumMap<>(Map.of(Prof.Unhappines, 0, Prof.Worker, 0, Prof.WorkerNotWork, 2, Prof.Farmer, 0, Prof.Engineer, 0, Prof.Artist, 0, Prof.Merchant, 0, Prof.Scientist, 10))));//
	}

	public void saveNow(HashMap<String, Object> hashmap) {
		hashmap.put("peoplesTotal", this.peoplesTotal);
		hashmap.put("peoples", peopesToString());
	}

	public String peopesToString() {
		String s = "";
		for (Prof prof : peoplesPriority) {
			s = s + prof.toString() + ":" + peoples.get(prof) + ", ";
		}
		return s.substring(0, s.length() - 2);
	}

	private void loadPeopesFromString(String string) {
		CivLog.debug("loadPeopesFromString  " + string);
		peoples.clear();
		for (String s : string.split(",")) {
			String[] split = s.trim().split(":");
			if (split.length != 2) continue;
			Prof prof = Prof.valueOf(split[0]);
			peoples.put(prof, Integer.parseInt(split[1]));
			peoplesPriority.add(prof);
		}
	}

	// ---------- getters

	public int getIntake(StorageType type) {
		int total = 0;
		for (Prof prof : Prof.values()) {
			total += intakeTable.get(type).get(prof) * peoples.get(prof);
		}
		return total;
	}

	public int getFoodsOuttake() {
		return peoplesTotal * foodsPeoplesOuttake;
	}

	public int getPeoplesTotal() {
		return peoplesTotal;
	}

	public Map<Prof, Integer> getPeoples() {
		return peoples;
	}

	public Integer getCount(Prof prof) {
		return peoples.get(prof);
	}

	public void setCount(Prof prof, int count) {
		peoples.put(prof, count);
	}

	public void markAllWorkerNotWork() {
		peoples.put(Prof.WorkerNotWork, peoples.get(Prof.Worker));
	}

	public int progressBuildGetHammers(int neadHammers) {
		if (neadHammers == 0) return 0;
		int neadWorker = 1 + (neadHammers - 1) / hammersWorkerOuttake;
		int workerWork = Math.min(getCount(Prof.WorkerNotWork), neadWorker);
		setCount(Prof.WorkerNotWork, getCount(Prof.WorkerNotWork) - workerWork);
		return Math.min(workerWork * hammersWorkerOuttake, neadHammers);
	}

	public int calcHammerPerCivtick() {
		return getCount(Prof.WorkerNotWork) * hammersWorkerOuttake;
	}
	
	// ----------- private Peoples

	private int getMaxPeoplesWithProfesion(Prof prof) {
		switch (prof) {
		case Artist:
		case Merchant:
		case Scientist:
			return 5; // FIXME MaxPeoplesWithProfesion
		case Engineer:
			return 1 + ((int) town.SM.getAttrHammer().total - 1) / intakeTable.get(StorageType.Hammer).get(Prof.Engineer);
		case Farmer:
			return 1 + ((int) town.SM.getAttrGrowth().total - 1) / intakeTable.get(StorageType.Food).get(Prof.Farmer);
		default:
			return Integer.MAX_VALUE;
		}
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
		this.peoplesTotal += this.addPeoplesWithPriority(count);
		CivMessage.sendCiv(town.getCiv(), ChatColor.GREEN + "Население города " + town.getName() + " выросло до " + peoplesTotal + " жителей");
	}

	/** Умерло count жителей */
	public void deadPeoples(int count) {
		if (this.peoplesTotal <= count) return;
		this.peoplesTotal -= this.removePeoplesWithPriority(count);
		CivMessage.sendCiv(town.getCiv(), ChatColor.RED + "Население города " + town.getName() + " уменьшилось до " + peoplesTotal + " жителей");
	}

	/** Назначить работников на должность */
	public void dismissPeoples(Prof prof, Integer count) {
		if (prof == Prof.Worker) return;
		int dismissed = removeProfPeople(prof, count);
		addProfPeoples(Prof.Worker, dismissed);
	}

	/** Уволить работников с должности */
	public void hirePeoples(Prof prof, Integer count) {
		if (prof == Prof.Worker) return;
		int dismissed = removeProfPeople(Prof.Worker, count);
		addProfPeoples(prof, dismissed);
	}

	public void setHigestPriority(Prof prof) {
		ArrayList<Prof> newPeoplesPriority = new ArrayList<>();
		newPeoplesPriority.add(prof);
		for (Prof pr : peoplesPriority) {
			if (!pr.equals(prof)) newPeoplesPriority.add(pr);
		}
		this.peoplesPriority = newPeoplesPriority;
	}

}
