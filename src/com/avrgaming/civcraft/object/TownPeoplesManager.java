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
		UNHAPPINES, // Несчасные жители не работают
		WORKER, // Обычные рабочие строят здания
		WORKERNOTWORK, // Ими временно становяться рабочие которые ничего не строят. Они добывают материалы
		MINER, // Добывает материалы в шахтах
		FARMER, // Добывает пищю
		ARTIST, // Приносят культуру в театрах
		MERCHANT, // Приносят деньги в котеджах
		SCIENTIST// Приносят науку в библиотеках
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
		peoplesPriority = Arrays.asList(Prof.WORKER, Prof.MINER, Prof.MERCHANT, Prof.ARTIST, Prof.SCIENTIST, Prof.FARMER, Prof.UNHAPPINES);
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
		EnumMap<StorageType, EnumMap<Prof, Integer>> m = new EnumMap<>(StorageType.class);
		
		EnumMap<Prof, Integer> mf = new EnumMap<>(Prof.class);
		mf.put(Prof.UNHAPPINES, 0);
		mf.put(Prof.WORKER, 1);
		mf.put(Prof.WORKERNOTWORK, 2);
		mf.put(Prof.FARMER, 4);
		mf.put(Prof.MINER, 1);
		mf.put(Prof.ARTIST, 1);
		mf.put(Prof.MERCHANT, 1);
		mf.put(Prof.SCIENTIST, 1);
		m.put(StorageType.FOOD, mf);
		
		EnumMap<Prof, Integer> mh = new EnumMap<Prof, Integer>(Prof.class);
		mh.put(Prof.UNHAPPINES, 0);
		mh.put(Prof.WORKER, 0);
		mh.put(Prof.WORKERNOTWORK, 2);
		mh.put(Prof.FARMER, 0);
		mh.put(Prof.MINER, 10);
		mh.put(Prof.ARTIST, 0);
		mh.put(Prof.MERCHANT, 0);
		mh.put(Prof.SCIENTIST, 0);
		m.put(StorageType.HAMMER, mh);
		
		EnumMap<Prof, Integer> mc = new EnumMap<>(Prof.class);
		mc.put(Prof.UNHAPPINES, 0);
		mc.put(Prof.WORKER, 0);
		mc.put(Prof.WORKERNOTWORK, 2);
		mc.put(Prof.FARMER, 0);
		mc.put(Prof.MINER, 0);
		mc.put(Prof.ARTIST, 10);
		mc.put(Prof.MERCHANT, 0);
		mc.put(Prof.SCIENTIST, 0);
		m.put(StorageType.CULTURE, mc);
		
		EnumMap<Prof, Integer> me = new EnumMap<>(Prof.class);
		me.put(Prof.UNHAPPINES, 0);
		me.put(Prof.WORKER, 0);
		me.put(Prof.WORKERNOTWORK, 2);
		me.put(Prof.FARMER, 0);
		me.put(Prof.MINER, 0);
		me.put(Prof.ARTIST, 0);
		me.put(Prof.MERCHANT, 10);
		me.put(Prof.SCIENTIST, 0);
		m.put(StorageType.ECON, me);
		
		EnumMap<Prof, Integer> mb = new EnumMap<>(Prof.class);
		mb.put(Prof.UNHAPPINES, 0);
		mb.put(Prof.WORKER, 0);
		mb.put(Prof.WORKERNOTWORK, 2);
		mb.put(Prof.FARMER, 0);
		mb.put(Prof.MINER, 0);
		mb.put(Prof.ARTIST, 0);
		mb.put(Prof.MERCHANT, 0);
		mb.put(Prof.SCIENTIST, 10);
		m.put(StorageType.BEAKERS, mb);
		return m;
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
		peoples.put(Prof.WORKERNOTWORK, peoples.get(Prof.WORKER));
	}

	public int progressBuildGetHammers(int neadHammers) {
		if (neadHammers == 0) return 0;
		int neadWorker = 1 + (neadHammers - 1) / hammersWorkerOuttake;
		int workerWork = Math.min(getCount(Prof.WORKERNOTWORK), neadWorker);
		setCount(Prof.WORKERNOTWORK, getCount(Prof.WORKERNOTWORK) - workerWork);
		return Math.min(workerWork * hammersWorkerOuttake, neadHammers);
	}

	public int calcHammerPerCivtick() {
		return getCount(Prof.WORKERNOTWORK) * hammersWorkerOuttake;
	}

	// ----------- private Peoples

	private int getMaxPeoplesWithProfesion(Prof prof) {
		switch (prof) {
		case ARTIST:
		case MERCHANT:
		case SCIENTIST:
			return 5; // FIXME MaxPeoplesWithProfesion
		case MINER:
			return 1 + ((int) town.SM.getAttrHammer().total - 1) / intakeTable.get(StorageType.HAMMER).get(Prof.MINER);
		case FARMER:
			return 1 + ((int) town.SM.getAttrGrowth().total - 1) / intakeTable.get(StorageType.FOOD).get(Prof.FARMER);
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
		if (prof == Prof.WORKER) return;
		int dismissed = removeProfPeople(prof, count);
		addProfPeoples(Prof.WORKER, dismissed);
	}

	/** Уволить работников с должности */
	public void hirePeoples(Prof prof, Integer count) {
		if (prof == Prof.WORKER) return;
		int dismissed = removeProfPeople(Prof.WORKER, count);
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
