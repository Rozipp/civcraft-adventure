package com.avrgaming.civcraft.object;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TownPeoplesManager.Prof;
import com.avrgaming.civcraft.object.TownStorageManager.StorageType;
import com.avrgaming.civcraft.util.CivColor;

public class TownPeoplesIntakeTable {

	private static TownPeoplesIntakeTable defaultTable;

	private EnumMap<Prof, EnumMap<StorageType, Integer>> intakeTable;

	public static void loadConfig(FileConfiguration cfg) {
		defaultTable = new TownPeoplesIntakeTable();
		defaultTable.intakeTable = new EnumMap<>(Prof.class);
		List<Map<?, ?>> intake_tables = cfg.getMapList("peoples_intake_table");
		for (Map<?, ?> b : intake_tables) {
			Prof prof = Prof.valueOf(((String) b.get("prof")).toUpperCase());
			CivLog.debug("prof = " + prof);
			EnumMap<StorageType, Integer> ctr = new EnumMap<>(StorageType.class);
			for (StorageType sType : StorageType.values()) {
				Integer c = 0;
				Object obj = b.get(sType.toString());
				if (obj != null) c = (Integer) obj;
				ctr.put(sType, c);
			}
			defaultTable.intakeTable.put(prof, ctr);
		}
	}

	public static TownPeoplesIntakeTable createTownPeoplesIntakeTable() {
		TownPeoplesIntakeTable n = new TownPeoplesIntakeTable();
		n.intakeTable = defaultTable.intakeTable.clone();
		return n;
	}

	public void addIntake(Prof prof, StorageType storageType, Integer add) {
		setIntake(prof, storageType, getIntake(prof, storageType) + add);
	}

	public void setIntake(Prof prof, StorageType storageType, Integer intake) {
		intakeTable.get(prof).put(storageType, intake);
	}

	public Integer getIntake(Prof prof, StorageType storageType) {
		return intakeTable.get(prof).getOrDefault(storageType, 0);
	}

	public void showIntakeTable(CommandSender sender) {
		String ss = CivColor.addTabToString("", "", 13);
		for (StorageType storageType : StorageType.values()) {
			ss = CivColor.addTabToString(ss, storageType.name().substring(0, 3), 5);
		}
		CivMessage.send(sender, ss);
		for (Prof prof : Prof.values()) {
			ss = CivColor.addTabToString("", prof.name(), 15);
			for (StorageType storageType : StorageType.values()) {
				ss = CivColor.addTabToString(ss, getIntake(prof, storageType).toString(), 5);
			}
			CivMessage.send(sender, ss);
		}

	}
}
