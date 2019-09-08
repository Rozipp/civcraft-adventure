package com.avrgaming.civcraft.units;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class ComponentsManager {
	private HashMap<String, Integer> totalComponents;
	private HashMap<Integer, HashMap<String, Integer>> levelComponents;
	private Integer levelUp;

	public ComponentsManager() {
		totalComponents = new HashMap<>();
		levelComponents = new HashMap<>();
		levelComponents.put(0, new HashMap<>());
		levelUp = 0;
	}

	public void addLevelUp() {
		this.levelUp = this.levelUp + 1;
	}
	public void removeLevelUp() {
		if (this.levelUp > 0) this.levelUp = this.levelUp - 1;
	}
	public Integer getLevelUp() {
		return this.levelUp;
	}

	public void addComponenet(Integer level, String key, Integer value) {
		if (!levelComponents.containsKey(level)) levelComponents.put(level, new HashMap<>());

		HashMap<String, Integer> comps = levelComponents.get(level);
		comps.put(key, comps.getOrDefault(key, 0) + value);
		levelComponents.put(level, comps);

		totalComponents.put(key, value + totalComponents.getOrDefault(key, 0));
	}

	public void setBaseComponent(String key, Integer value) {
		if (!levelComponents.containsKey(0)) levelComponents.put(0, new HashMap<>());
		levelComponents.get(0).put(key, value);
	}
	public Integer getBaseComponentValue(String key) {
		return levelComponents.get(0).getOrDefault(key, 0);
	}
	public HashMap<String, Integer> getBaseComponents() {
		return levelComponents.get(0);
	}

	public Integer getComponentValue(String key) {
		return totalComponents.getOrDefault(key, 0);
	}
	public void removeComponent(String key) {
		Integer oldValue = totalComponents.getOrDefault(key, 0);
		if (oldValue == 0)
			totalComponents.remove(key);
		else
			totalComponents.put(key, oldValue - 1);
	}
	public Boolean hasComponent(String key) {
		return totalComponents.containsKey(key);
	}
	public Set<String> getComponentsKey() {
		return totalComponents.keySet();
	}

	public Collection<String> removeLastComponents(Integer level) {
		ArrayList<String> ss = new ArrayList<>();
		HashMap<String, Integer> removeComponents = levelComponents.get(level);
		for (String key : removeComponents.keySet()) {
			while (removeComponents.get(key) > 0) {
				ss.add(key);
				levelUp = levelUp + 1;
				removeComponents.put(key, removeComponents.get(key) - 1);
				removeComponent(key);
			}
		}
		levelComponents.remove(level);
		return ss;
	}

	public void loadComponents(String sourceString) {
		if (sourceString == null || sourceString == "") return;

		String[] source = sourceString.split("@");
		if (source.length != 2) return;

		this.levelUp = Integer.valueOf(source[0]);
		String[] sourceSplit = source[1].split(";");
		for (String levelComp : sourceSplit) {
			String[] levelCompSplit = levelComp.split(":");
			if (levelCompSplit.length != 2) continue;
			Integer level = Integer.parseInt(levelCompSplit[0]);

			String[] componentsSplit = levelCompSplit[1].replace("[", "").replace("]", "").split(",");
			if (componentsSplit.length < 1) continue;
			HashMap<String, Integer> newComponents = new HashMap<>();
			for (int i = 0; i < componentsSplit.length; i++) {
				String[] c = componentsSplit[i].split("=");
				if (c.length != 2) continue;
				newComponents.put(c[0].toLowerCase(), Integer.parseInt(c[1]));
			}
			this.levelComponents.put(level, newComponents);
		}
		rebuildComponents();
	}

	public void rebuildComponents() {
		for (Integer level : levelComponents.keySet()) {
			if (level == 0) continue;
			HashMap<String, Integer> comp = levelComponents.get(level);
			for (String key : comp.keySet()) {
				this.totalComponents.put(key, this.totalComponents.getOrDefault(key, 0) + comp.get(key));
			}
		}
	}

	public String getSaveString() {
		if (this.levelComponents.isEmpty()) return "";

		String sss = levelUp.toString() + "@";
		for (Integer i = 0; i <= 100; i++) {
			if (!this.levelComponents.containsKey(i)) continue;
			HashMap<String, Integer> comp = this.levelComponents.get(i);
			sss = sss + i + ":" + "[" + (comp.isEmpty() ? "" : comp.toString().replace("{", "").replace("}", "").replace(" ", "")) + "];";
		}
		return sss.substring(0, sss.length() - 1);
	}

}