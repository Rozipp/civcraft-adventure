package com.avrgaming.civcraft.object;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.permission.PermissionGroup;

/**
 * <p>
 * <b>Менеджер груп</b>
 * </p>
 * Используеться только в классе {@link com.avrgaming.civcraft.object.Town}
 * @author Rozipp */
public class TownGroupManager extends GroupManager {

	protected Town town;
	private PermissionGroup defaultGroup;
	private PermissionGroup mayorGroup;
	private PermissionGroup assistantGroup;

	public String defaultGroupName = "Residents";
	public String mayorGroupName = "Mayors";
	public String assistantGroupName = "Assistants";

	public TownGroupManager(Town town, String defaultGroupName, String mayorGroupName, String assistantGroupName) {
		super();
		this.town = town;
		if (defaultGroupName != null) this.defaultGroupName = defaultGroupName;
		if (mayorGroupName != null) this.mayorGroupName = mayorGroupName;
		if (assistantGroupName != null) this.assistantGroupName = assistantGroupName;
	}

	@Override
	public void delete() throws SQLException {
		for (PermissionGroup grp : this.groups.values())
			grp.delete();
		if (defaultGroup != null) defaultGroup.delete();
		if (mayorGroup != null) mayorGroup.delete();
		if (assistantGroup != null) assistantGroup.delete();
	}

	public void init() {
		try {
			defaultGroup = new PermissionGroup(town, defaultGroupName);
			defaultGroup.save();
			mayorGroup = new PermissionGroup(town, mayorGroupName);
			mayorGroup.save();
			assistantGroup = new PermissionGroup(town, assistantGroupName);
			assistantGroup.save();
		} catch (InvalidNameException e) {}
	}

	@Override
	public void newGroup(String name) throws InvalidNameException {
		if (isProtectedGroupName(name)) throw new InvalidNameException("Это защищенная група");
		PermissionGroup grp = new PermissionGroup(town, name);
		this.newGroup(grp);
	}

	public void addGroup(PermissionGroup grp) {
		if (grp.getName().equalsIgnoreCase(this.mayorGroupName))
			this.mayorGroup = grp;
		else
			if (grp.getName().equalsIgnoreCase(this.assistantGroupName))
				this.assistantGroup = grp;
			else
				if (grp.getName().equalsIgnoreCase(this.defaultGroupName))
					this.defaultGroup = grp;
				else
					groups.put(grp.getName(), grp);
		grp.save();
	}

	public void addMayor(Resident res) throws CivException {
		if (res.getTown() != town) throw new CivException("Игрок " + res.getName() + " не состоит в городе " + town.getName());
		if (this.mayorGroup.hasMember(res)) throw new CivException("Игрок " + res.getName() + " уже состоит в групе " + mayorGroupName);

		this.mayorGroup.addMember(res);
		this.mayorGroup.save();
	}

	public void setMayor(Resident res) throws CivException {
		this.mayorGroup.clearMembers();
		this.mayorGroup.addMember(res);
	}

	public Resident getMayor() {
		for (Resident mayor : mayorGroup.getMembers().values()) {
			return mayor;
		}
		return null;
	}

	public Collection<Resident> getMayors() {
		return mayorGroup.getMembers().values();
	}

	public void removeMayor(Resident res) throws CivException {
		if (this.isMayor(res)) {
			this.mayorGroup.removeMember(res);
			this.mayorGroup.save();
		} else
			throw new CivException("Игрок " + res.getName() + " не состоит в групе " + mayorGroupName);
	}

	public boolean areMayorsInactive() {
		int mayor_inactive_days;
		try {
			mayor_inactive_days = CivSettings.getInteger(CivSettings.townConfig, "town.mayor_inactive_days");
			for (Resident resident : getMayors())
				if (!resident.isInactiveForDays(mayor_inactive_days)) return false;
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void addAssistant(Resident res) throws CivException {
		if (this.isAssistant(res)) throw new CivException("Игрок " + res.getName() + " уже состоит в групе " + assistantGroupName);
		this.assistantGroup.addMember(res);
		this.assistantGroup.save();
	}

	public void removeAssistant(Resident res) throws CivException {
		if (!this.isAssistant(res)) throw new CivException("Игрок " + res.getName() + " не состоит в групе " + assistantGroupName);
		this.assistantGroup.removeMember(res);
		this.assistantGroup.save();
	}

	public void addDefault(Resident res) throws CivException {
		if (defaultGroup.hasMember(res)) throw new CivException("Игрок " + res.getName() + " уже состоит в групе " + defaultGroupName);
		defaultGroup.addMember(res);
		defaultGroup.save();
	}

	public void removeDefault(Resident res) throws CivException {
		if (defaultGroup.hasMember(res)) throw new CivException("Игрок " + res.getName() + " не состоит в групе " + defaultGroupName);
		defaultGroup.removeMember(res);
		defaultGroup.save();
	}

	public boolean isResident(Resident res) {
		if (this.defaultGroup.hasMember(res)) return true;
		return false;
	}

	public boolean isOneMayor(Resident res) {
		return mayorGroup.hasMember(res) && mayorGroup.getMemberCount() == 1;
	}

	public boolean isMayor(Resident res) {
		if (this.mayorGroup.hasMember(res)) return true;
		return false;
	}

	public boolean isAssistant(Resident res) {
		if (this.assistantGroup.hasMember(res)) return true;
		return false;
	}

	public boolean isMayorOrAssistant(Resident res) {
		return isMayor(res) || isAssistant(res);
	}

	@Override
	public Collection<PermissionGroup> getProtectedGroups() {
		ArrayList<PermissionGroup> grp = new ArrayList<PermissionGroup>();
		grp.add(mayorGroup);
		grp.add(assistantGroup);
		grp.add(defaultGroup);
		return grp;
	}

	@Override
	public boolean isProtectedGroup(PermissionGroup grp) {
		if ((grp == defaultGroup) || (grp == mayorGroup) || (grp == assistantGroup)) return true;
		return false;
	}

	@Override
	public boolean isProtectedGroupName(String name) {
		if ((name == defaultGroupName) || (name == mayorGroupName) || (name == assistantGroupName)) return true;
		return false;
	}

	@Override
	public void renameProtectedGroup(String oldName, String newName) throws InvalidNameException {
		if (oldName == this.mayorGroupName) {
			mayorGroup.setName(newName);
			mayorGroupName = newName;
			return;
		}
		if (oldName == this.assistantGroupName) {
			assistantGroup.setName(newName);
			assistantGroupName = newName;
			return;
		}
		if (oldName == this.defaultGroupName) {
			defaultGroup.setName(newName);
			defaultGroupName = newName;
			return;
		}
	}

	public PermissionGroup getDefaultGroup() {
		return defaultGroup;
	}

	public PermissionGroup getMayorGroup() {
		return mayorGroup;
	}

	public PermissionGroup getAssistantGroup() {
		return assistantGroup;
	}

}
