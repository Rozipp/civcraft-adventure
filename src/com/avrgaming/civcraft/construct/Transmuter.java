package com.avrgaming.civcraft.construct;

import java.util.HashSet;
import java.util.Set;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;

public class Transmuter {

	private TransmuterAsyncTimer task;
	@SuppressWarnings("unused")
	private String name = "";
	public Double modifyChance = 1.0;

	public Transmuter(Construct construct) {
		task = new TransmuterAsyncTimer(construct, this);
		Transmuter.transmuters.add(this);
	}

	public Transmuter(Construct construct, String name) {
		this.name = name;
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
				if (!task.isFinished()) task.stop();
				TaskMaster.asyncTask(task, 20);
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

}
