package com.avrgaming.civcraft.construct;

import java.util.HashSet;
import java.util.Set;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;

public class Transmuter {

	private TransmuterAsyncTimer task;
	private Construct construct;
	private String name = "";
	public Double modifyChance = 1.0;

	public Transmuter(Construct construct) {
		this.construct = construct;
		task = new TransmuterAsyncTimer(construct, this);
		Transmuter.transmuters.add(this);
	}

	public Transmuter(Construct construct, String name) {
		this.name = name;
		this.construct = construct;
		task = new TransmuterAsyncTimer(construct, this);
		Transmuter.transmuters.add(this);
	}

	public void addRecipe(String s) {
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				task.addRecipe(s);
			}
		}, 2);
	}

	public void setRecipe(String... ss) {
		clearRecipe();
		for (String s : ss)
			addRecipe(s);
	}

	public void clearRecipe() {
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				task.clearRecipe();
			}
		}, 1);
	}

	public void start() {
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				task.stop();
				TaskMaster.cancelTask(this.toString());
				TaskMaster.asyncTask(this.toString(), task, 20);
			}
		}, 1);
	}

	public void stop() {
		task.stop();
	}

	// --------------- static
	private static Set<Transmuter> transmuters = new HashSet<>();

	public static void pauseAllTransmuter() {
		int count = 0;
		for (Transmuter tr : Transmuter.transmuters) {
			tr.stop();
			count++;
		}
		CivLog.info("Paused " + count + " transmuters");
	}

	public static void resumeAllTransmuter() {
		int count = 0;
		for (Transmuter tr : Transmuter.transmuters) {
			tr.start();
			count++;
		}
		CivLog.info("Resume " + count + " transmuters");
	}

	public static void stopAllTransmuter() {
		int count = 0;
		for (Transmuter tr : Transmuter.transmuters) {
			tr.stop();
			count++;
		}
		Transmuter.transmuters.clear();
		CivLog.info("Stoped " + count + " transmuters");
	}

	@Override
	public String toString() {
		return "transmuter:" + construct.getCorner().toString() + ":" + name;
	}

}
