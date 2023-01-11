package com.avrgaming.civcraft.questions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.threading.TaskMaster;

public class Question {
	private static Map<String, QuestionBaseTask> questions = new ConcurrentHashMap<String, QuestionBaseTask>();
	public static Map<String, CivQuestionTask> civQuestions = new ConcurrentHashMap<String, CivQuestionTask>();

	// -------------- question
	public static void questionPlayer(Player fromPlayer, Player toPlayer, String question, long timeout, QuestionResponseInterface finishedFunction)
			throws CivException {

		PlayerQuestionTask task = (PlayerQuestionTask) questions.get(toPlayer.getName());
		if (task != null) {
			/* Player already has a question pending. Lets deny this question until it times out this will allow questions to come in on a pseduo 'first come
			 * first serve' and prevents question spamming. */
			throw new CivException(CivSettings.localize.localizedString("civGlobal_hasPendingRequest"));
		}

		task = new PlayerQuestionTask(toPlayer, fromPlayer, question, timeout, finishedFunction);
		questions.put(toPlayer.getName(), task);
		TaskMaster.asyncTask("", task, 0);
	}
	public static void questionLeaders(Player fromPlayer, Civilization toCiv, String question, long timeout, QuestionResponseInterface finishedFunction)
			throws CivException {

		CivLeaderQuestionTask task = (CivLeaderQuestionTask) questions.get("civ:" + toCiv.getName());
		if (task != null) {
			/* Player already has a question pending. Lets deny this question until it times out this will allow questions to come in on a pseduo 'first come
			 * first serve' and prevents question spamming. */
			throw new CivException(CivSettings.localize.localizedString("civGlobal_civHasPendingRequest"));
		}

		task = new CivLeaderQuestionTask(toCiv, fromPlayer, question, timeout, finishedFunction);
		questions.put("civ:" + toCiv.getName(), task);
		TaskMaster.asyncTask("", task, 0);
	}
	public static QuestionBaseTask getQuestionTask(String string) {
		return questions.get(string);
	}
	public static void removeQuestion(String name) {
		questions.remove(name);
	}

	public static void requestRelation(Civilization fromCiv, Civilization toCiv, String question, long timeout, QuestionResponseInterface finishedFunction)
			throws CivException {
		CivQuestionTask task = civQuestions.get(toCiv.getName());
		if (task != null) {
			/* Civ already has a question pending. Lets deny this question until it times out this will allow questions to come in on a pseduo 'first come first
			 * serve' and prevents question spamming. */
			throw new CivException(CivSettings.localize.localizedString("civGlobal_civHasPendingRequest"));
		}
		task = new CivQuestionTask(toCiv, fromCiv, question, timeout, finishedFunction);
		civQuestions.put(toCiv.getName(), task);
		TaskMaster.asyncTask("", task, 0);
	}
	public static void requestCoalition(Civilization fromCiv, Civilization toCiv, String question, long timeout, QuestionResponseInterface finishedFunction)
			throws CivException {

		CivQuestionTask task = civQuestions.get(toCiv.getName());
		if (task != null) throw new CivException(CivSettings.localize.localizedString("civGlobal_civHasPendingRequest"));
		task = new CivQuestionTask(toCiv, fromCiv, question, timeout, finishedFunction);
		civQuestions.put(toCiv.getName(), task);
		TaskMaster.asyncTask("", task, 0);
	}
	public static void requestSurrender(Civilization fromCiv, Civilization toCiv, String question, long timeout, QuestionResponseInterface finishedFunction)
			throws CivException {
		CivQuestionTask task = civQuestions.get(toCiv.getName());
		if (task != null) {
			/* Civ already has a question pending. Lets deny this question until it times out this will allow questions to come in on a pseduo 'first come first
			 * serve' and prevents question spamming. */
			throw new CivException(CivSettings.localize.localizedString("civGlobal_civHasPendingRequest"));
		}
		task = new CivQuestionTask(toCiv, fromCiv, question, timeout, finishedFunction);
		civQuestions.put(toCiv.getName(), task);
		TaskMaster.asyncTask("", task, 0);
	}
	public static void removeRequest(String name) {
		civQuestions.remove(name);
	}
	public static CivQuestionTask getCivQuestionTask(Civilization senderCiv) {
		return civQuestions.get(senderCiv.getName());
	}

}
