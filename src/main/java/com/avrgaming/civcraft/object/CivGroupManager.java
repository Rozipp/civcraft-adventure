package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
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

	public PermissionGroup leadersGroup;
	public PermissionGroup advisersGroup;

	/* Strings used for reverse lookups. */
	public String leadersGroupName = "Leaders";
	public String advisersGroupName = "Advisers";

	public CivGroupManager(Civilization civ) {
		super();
		this.civ = civ;
	}

	public CivGroupManager(Civilization civ, ResultSet rs) throws SQLException {
		super();
		this.civ = civ;
		this.leadersGroupName = rs.getString("leaderGroupName");
		this.advisersGroupName = rs.getString("advisersGroupName");
	}

	@Override
	public void delete() throws SQLException {
		for (PermissionGroup grp : this.groups.values()) {
			grp.delete();
		}
		if (leadersGroup != null) leadersGroup.delete();
		if (advisersGroup != null) advisersGroup.delete();
	}

	public void init() {
		try {
			leadersGroup = new PermissionGroup(civ, leadersGroupName);
			leadersGroup.save();
			advisersGroup = new PermissionGroup(civ, advisersGroupName);
			advisersGroup.save();
		} catch (InvalidNameException e) {}

	}

	public PermissionGroup getLeaderGroup() {
		return leadersGroup;
	}

	public PermissionGroup getAdviserGroup() {
		return advisersGroup;
	}

	@Override
	public void newGroup(String name) throws InvalidNameException {
		if (isProtectedGroup(name)) throw new InvalidNameException("It is protection group");
		PermissionGroup grp = new PermissionGroup(civ, name);
		this.newGroup(grp);
	}

	public void addGroup(PermissionGroup grp) {
		if (grp.getName().equalsIgnoreCase(this.leadersGroupName))
			leadersGroup = grp;
		else
			if (grp.getName().equalsIgnoreCase(this.advisersGroupName))
				advisersGroup = grp;
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

	public void addInProtectedGroup(Resident res) throws CivException {

	}

	public void addLeader(Resident res) throws CivException {
		if (this.leadersGroup != null && !this.leadersGroup.hasMember(res) && civ.hasResident(res)) {
			this.leadersGroup.addMember(res);
			this.leadersGroup.save();
		} else
			throw new CivException("FIXMI " + res.getName() + "  -  " + leadersGroupName);
	}

	public void setLeader(Resident res) {
		this.leadersGroup.clearMembers();
		this.leadersGroup.addMember(res);
	}

	public Resident getLeader() {
		for (Resident resident : this.getLeaders())
			return resident;
		return null;
	}

	public Collection<Resident> getLeaders() {
		return leadersGroup.getMembers().values();
	}

	public void removeLeader(Resident res) throws CivException {
		if (this.isLeader(res)) {
			this.leadersGroup.removeMember(res);
			this.leadersGroup.save();
		} else
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_rmLeaderNotInGroup", res.getName(), civ.getName()));
	}

	public void addAdviser(Resident res) throws CivException {
		if (this.advisersGroup != null && !this.advisersGroup.hasMember(res) && civ.hasResident(res)) {
			this.advisersGroup.addMember(res);
			this.advisersGroup.save();
		} else
			throw new CivException("FIXMI  " + res.getName() + "  -   " + advisersGroupName);
	}

	public void removeAdviser(Resident res) throws CivException {
		if (this.isAdviser(res)) {
			this.advisersGroup.removeMember(res);
			this.advisersGroup.save();
		} else
			throw new CivException(CivSettings.localize.localizedString("var_adcmd_civ_rmAdvisorNotInGroup", res.getName(), civ.getName()));
	}

	public Collection<Resident> getAdvisers() {
		return advisersGroup.getMembers().values();
	}

	public boolean isOneLeader(Resident res) {
		return isLeader(res) && leadersGroup.getMemberCount() == 1;
	}

	public boolean isLeader(Resident res) {
		return this.leadersGroup.hasMember(res);
	}

	public boolean isAdviser(Resident res) {
		return this.advisersGroup.hasMember(res);
	}

	public boolean isLeaderOrAdviser(Resident res) {
		return isLeader(res) || isAdviser(res);
	}

	@Override
	public Collection<PermissionGroup> getProtectedGroups() {
		ArrayList<PermissionGroup> grp = new ArrayList<PermissionGroup>();
		grp.add(leadersGroup);
		grp.add(advisersGroup);
		return grp;
	}

	@Override
	public boolean isProtectedGroup(PermissionGroup grp) {
		if ((grp == leadersGroup) || (grp == advisersGroup)) return true;
		return false;
	}

	@Override
	public boolean isProtectedGroup(String name) {
		if ((name == leadersGroupName) || (name == advisersGroupName)) return true;
		return false;
	}

	@Override
	public void renameProtectedGroup(PermissionGroup group, String newName) throws InvalidNameException {
		if (group == leadersGroup) leadersGroupName = newName;
		if (group == advisersGroup) advisersGroupName = newName;
		group.setName(newName);
		group.save();
		civ.save();
	}

	@Override
	public void addToProtectedGroup(PermissionGroup group, Resident res) throws CivException {
		if (group == advisersGroup) {
			addAdviser(res);
			return;
		}
		if (group == leadersGroup) {
			addLeader(res);
			return;
		}
	}

	@Override
	public void removeFromProtectedGroup(PermissionGroup group, Resident res) throws CivException {
		if (group == advisersGroup) {
			removeAdviser(res);
			return;
		}
		if (group == leadersGroup) {
			removeLeader(res);
			return;
		}
	}

}
