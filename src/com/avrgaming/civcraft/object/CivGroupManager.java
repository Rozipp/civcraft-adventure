package com.avrgaming.civcraft.object;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.permission.PermissionGroup;

public class CivGroupManager extends GroupManager {

	protected Civilization civ;

	public PermissionGroup leaderGroup;
	public PermissionGroup adviserGroup;

	/* Strings used for reverse lookups. */
	public String leaderGroupName = "Leaders";
	public String advisersGroupName = "Advisers";

	public CivGroupManager(Civilization civ, String leaderGroupName, String advisersGroupName) throws InvalidNameException {
		super();
		this.civ = civ;
		this.leaderGroupName = leaderGroupName;
		this.advisersGroupName = advisersGroupName;
	}

	@Override
	public void delete() throws SQLException {
		for (PermissionGroup grp : this.groups.values()) {
			grp.delete();
		}
		leaderGroup.delete();
		adviserGroup.delete();
	}

	public void init() {
		try {
			leaderGroup = new PermissionGroup(civ, leaderGroupName);
			adviserGroup = new PermissionGroup(civ, advisersGroupName);
		} catch (InvalidNameException e) {}

	}

	@Override
	public void newGroup(String name) throws InvalidNameException {
		if (isProtectedGroupName(name)) throw new InvalidNameException("It is protection group");
		PermissionGroup grp = new PermissionGroup(civ, name);
		this.newGroup(grp);
	}

	public void addGroup(PermissionGroup grp) {
		if (grp.getName().equalsIgnoreCase(this.leaderGroupName))
			leaderGroup = grp;
		else
			if (grp.getName().equalsIgnoreCase(this.advisersGroupName))
				adviserGroup = grp;
			else {
				groups.put(grp.getName(), grp);
				grp.save();
			}
		grp.save();
	}

	public boolean areLeaderInactive() {
		int mayor_inactive_days;
		try {
			mayor_inactive_days = CivSettings.getInteger(CivSettings.townConfig, "town.mayor_inactive_days");
			for (Resident resident : this.getLeaders())
				if (!resident.isInactiveForDays(mayor_inactive_days)) return false;
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void addLeader(Resident res) throws CivException {
		if (this.leaderGroup != null && !this.leaderGroup.hasMember(res) && civ.hasResident(res)) {
			this.leaderGroup.addMember(res);
			this.leaderGroup.save();
		} else
			throw new CivException("FIXMI " + res.getName() + "  -  " + leaderGroupName);
	}

	public void setLeader(Resident res) {
		this.leaderGroup.clearMembers();
		this.leaderGroup.addMember(res);
	}

	public Resident getLeader() {
		for (Resident resident : this.getLeaders())
			return resident;
		return null;
	}

	public Collection<Resident> getLeaders() {
		return leaderGroup.getMembers().values();
	}

	public void removeLeader(Resident res) throws CivException {
		if (this.isLeader(res)) {
			this.leaderGroup.removeMember(res);
			this.leaderGroup.save();
		} else
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderNotInGroup", res.getName(), civ.getName()));
	}

	public void addAdviser(Resident res) throws CivException {
		if (this.adviserGroup != null && !this.adviserGroup.hasMember(res) && civ.hasResident(res)) {
			this.adviserGroup.addMember(res);
			this.adviserGroup.save();
		} else
			throw new CivException("FIXMI  " + res.getName() + "  -   " + advisersGroupName);
	}

	public void removeAdviser(Resident res) throws CivException {
		if (this.isAdviser(res)) {
			this.adviserGroup.removeMember(res);
			this.adviserGroup.save();
		} else
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorNotInGroup", res.getName(), civ.getName()));
	}

	public Collection<Resident> getAdvisers() {
		return adviserGroup.getMembers().values();
	}

	public boolean isOneLeader(Resident res) {
		return isLeader(res) && leaderGroup.getMemberCount() == 1;
	}

	public boolean isLeader(Resident res) {
		return this.leaderGroup.hasMember(res);
	}

	public boolean isAdviser(Resident res) {
		return this.adviserGroup.hasMember(res);
	}

	public boolean isLeaderOrAdviser(Resident res) {
		return isLeader(res) || isAdviser(res);
	}

	@Override
	public Collection<PermissionGroup> getProtectedGroups() {
		ArrayList<PermissionGroup> grp = new ArrayList<PermissionGroup>();
		grp.add(leaderGroup);
		grp.add(adviserGroup);
		return grp;
	}

	@Override
	public boolean isProtectedGroup(PermissionGroup grp) {
		if ((grp == leaderGroup) || (grp == adviserGroup)) return true;
		return false;
	}

	@Override
	public boolean isProtectedGroupName(String name) {
		if ((name == leaderGroupName) || (name == advisersGroupName)) return true;
		return false;
	}

	@Override
	public void renameProtectedGroup(String oldName, String newName) throws InvalidNameException {
		if (oldName.equals(this.leaderGroupName)) {
			leaderGroup.setName(newName);
			leaderGroupName = newName;
			leaderGroup.save();
			civ.save();
			return;
		}
		if (oldName.equals(this.advisersGroupName)) {
			adviserGroup.setName(newName);
			advisersGroupName = newName;
			adviserGroup.save();
			civ.save();
			return;
		}
		throw new InvalidNameException("Не найдена група " + oldName);
	}

	public PermissionGroup getLeaderGroup() {
		return leaderGroup;
	}

	public PermissionGroup getAdviserGroup() {
		return adviserGroup;
	}

}
