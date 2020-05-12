package com.avrgaming.civcraft.construct;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.scheduler.BukkitTask;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTransmuterRecipe;
import com.avrgaming.civcraft.threading.TaskMaster;

public class Transmuter {

	public ReentrantLock lock = new ReentrantLock();
	private List<ConfigTransmuterRecipe> cTranRs = new ArrayList<>();
	long delay = 1000;
	private Construct construct;
	private String name = "";

	public Transmuter(Construct construct) {
		this.construct = construct;
	}

	public Transmuter(Construct construct, String name) {
		this.construct = construct;
		this.name = name;
	}

	public void addRecipe(String s) {
		ConfigTransmuterRecipe ctr = CivSettings.transmuterRecipes.get(s);
		if (ctr != null) {
			this.cTranRs.add(ctr);
			calcDelay();
		}
	}

	public void setRecipe(String[] ss) {
		this.cTranRs.clear();
		for (String s : ss)
			addRecipe(s);
	}

	public void setRecipe(String s) {
		this.cTranRs.clear();
		addRecipe(s);
	}

	public List<ConfigTransmuterRecipe> getTransmuterRecipe() {
		return cTranRs;
	}

	public void addAllRecipeToLevel(int level) {
		addAllRecipeToLevel(construct.getClass().getSimpleName().toLowerCase(), level);
	}

	public void addAllRecipeToLevel(String name, int level) {
		this.cTranRs.clear();
		for (int i = 1; i <= level; i++) {
			addRecipe(name + i);
		}
	}

	public void run() {
		if (cTranRs.isEmpty()) return;
		BukkitTask timer = TaskMaster.getTimer(this.toString());
		if (timer == null || timer.isCancelled()) TaskMaster.asyncTimer(this.toString(), new TransmuterAsyncTimer(construct, this), delay * 20);
	}

	public void stop() {
		BukkitTask timer = TaskMaster.getTimer(this.toString());
		if (timer != null) timer.cancel();
	}

	private void calcDelay() {
		int min = Integer.MAX_VALUE;
		for (ConfigTransmuterRecipe ctr : cTranRs) {
			if (min > ctr.delay) min = ctr.delay;
		}
		this.delay = (min < 1) ? 1 : min;
	}

	@Override
	public String toString() {
		return "transmuter:" + construct.getCorner().toString() + name;
	}
}
