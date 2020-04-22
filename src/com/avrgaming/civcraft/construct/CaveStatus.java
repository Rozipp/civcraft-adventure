package com.avrgaming.civcraft.construct;

import java.util.Date;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;

public class CaveStatus {

	public enum StatusType {
		founded, // найдены координаты
		available, // доступно для захвата
		captured, // захвачено
		lost, // утеряна
		updated, // обновлена
		used // использована
	}

	public StatusType statusType;
	public Date date;
	public int caveId;
	private Cave cave;
	public int activatorId;

	public CaveStatus(StatusType statusType, int caveId, Date date, Integer activatorId) {
		this.statusType = statusType;
		this.caveId = caveId;
		this.date = date;
		this.activatorId = activatorId;
	}

	public Cave getCave() {
		if (this.cave == null)
			this.cave = CivGlobal.getCaveFromId(this.caveId);
		return this.cave;
	}

	public static void newCaveStatus(Cave cave, Resident res) {
		Civilization civ = res.getCiv();
		if (civ == null)
			return;

		if (civ.getCaveStatuses().get(cave) != null) {
			return;
		}

		if (cave.getCaveConfig().isAvailable(civ))
			civ.addCaveStatus(cave, new CaveStatus(StatusType.available, cave.getId(), new Date(), res.getId()));
		else
			civ.addCaveStatus(cave, new CaveStatus(StatusType.founded, cave.getId(), new Date(), res.getId()));
	}

	public void editCaveStatusCaptured(Civilization civ) {
		if (civ == null)
			return;
		CaveStatus cStatus = civ.getCaveStatuses().get(getCave());
		if (cStatus == null)
			return;

		cStatus.activatorId = civ.getId();
		cStatus.date = new Date();
		cStatus.statusType = StatusType.captured;
		civ.addCaveStatus(getCave(), cStatus);
	}

	public void editCaveStatusLost(Civilization newCiv) {
		Civilization civ = this.getCave().getCiv();
		if (newCiv == null || civ == null)
			return;
		CaveStatus cStatus = civ.getCaveStatus(getCave());
		if (cStatus == null)
			return;

		cStatus.activatorId = newCiv.getId();
		cStatus.date = new Date();
		cStatus.statusType = StatusType.lost;
		civ.addCaveStatus(getCave(), cStatus);
	}

	public void editCaveStatusUpdate() {
		Civilization civ = this.getCave().getCiv();
		if (civ == null)
			return;
		CaveStatus cStatus = civ.getCaveStatuses().get(getCave());
		if (cStatus == null)
			return;

		if ((new Date()).getTime() < getCave().getLastUpdateTime() + getCave().getCaveConfig().updateTime)
			return;

		cStatus.activatorId = civ.getId();
		cStatus.date = new Date();
		cStatus.statusType = StatusType.updated;
		civ.addCaveStatus(getCave(), cStatus);
	}

	public void editCaveStatusUsed(Resident res) {
		Civilization civ = res.getCiv();
		if (civ == null)
			return;
		CaveStatus cStatus = civ.getCaveStatus(getCave());
		if (cStatus == null)
			return;

		cStatus.activatorId = res.getId();
		cStatus.date = new Date();
		getCave().setLastUpdateTime(cStatus.date.getTime());
		cStatus.statusType = StatusType.used;
		civ.addCaveStatus(getCave(), cStatus);
	}
}
