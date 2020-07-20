package com.avrgaming.civcraft.object;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.permission.PermissionGroup;

public abstract class GroupManager {

	ConcurrentHashMap<String, PermissionGroup> groups = new ConcurrentHashMap<String, PermissionGroup>();

	public GroupManager() {
	}

	public void delete() throws SQLException {
		for (PermissionGroup grp : this.groups.values()) {
			grp.delete();
		}
	}

	public abstract void newGroup(String name) throws InvalidNameException;

	public void newGroup(PermissionGroup grp) {
		groups.put(grp.getName(), grp);
	}

	public void removeGroup(PermissionGroup grp) {
		if (this.isProtectedGroup(grp))
			CivLog.error("Group " + grp.getName() + " is Protected");
		else {
			groups.remove(grp.getName());
			try {
				grp.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean hasGroup(String name) {
		return groups.containsKey(name.toLowerCase().trim());
	}

	public Collection<PermissionGroup> getGroups() {
		return groups.values();
	}

	public Collection<PermissionGroup> getAllGroups() {
		LinkedList<PermissionGroup> ret = new LinkedList<>();
		ret.addAll(getGroups());
		ret.addAll(getProtectedGroups());
		return ret;
	}

	public PermissionGroup getGroup(String name) {
		if (groups.contains(name.toLowerCase().trim())) return groups.get(name.toLowerCase().trim());
		for (PermissionGroup grp : this.getProtectedGroups())
			if (grp.getName().equalsIgnoreCase(name)) return grp;
		return null;
	}

	public PermissionGroup getGroupById(Integer id) {
		for (PermissionGroup grp : this.getProtectedGroups())
			if (grp.getId() == id) return grp;
		for (PermissionGroup grp : groups.values())
			if (grp.getId() == id) return grp;
		return null;
	}

	public PermissionGroup getGroupStartsWith(String subname) throws CivException {
		PermissionGroup found = null;
		for (PermissionGroup grp : groups.values()) {
			if (grp.getName().startsWith(subname.toLowerCase())) {
				if (found == null)
					found = grp;
				else
					throw new CivException("Найдено несколько груп");
			}
		}
		return found;
	}

	public boolean inGroup(String groupName, Resident resident) {
		PermissionGroup grp = this.groups.get(groupName);
		if (grp != null) return grp.hasMember(resident);
		return false;
	}

	public boolean inGroup(String groupName, Player player) {
		Resident res = CivGlobal.getResident(player);
		if (res == null) return false;
		return inGroup(groupName, res);
	}

	public void addToGroup(Resident res, PermissionGroup grp) {
		if (grp == null) return;
		grp.addMember(res);
		grp.save();
	}

	public void removeFromGroup(String groupName, Resident res) throws CivException {
		PermissionGroup grp = this.getGroup(groupName);
		removeFromGroup(res, grp);
	}

	public void removeFromGroup(Resident res, PermissionGroup grp) throws CivException {
		if (grp == null) return;
		if (grp.hasMember(res)) {
			grp.removeMember(res);
			grp.save();
		} else
			throw new CivException(CivSettings.localize.localizedString("var_cmd_civ_group_removeNotInGroup", res.getName(), grp.getName()));
	}

	public void removeAllGroup(Resident res) {
		/* Remove resident from any groups. */
		for (PermissionGroup group : getAllGroups())
			if (group.hasMember(res)) {
				group.removeMember(res);
				group.save();
			}
		res.save();
	}

	public void renameGroup(String oldName, String newName) throws CivException, InvalidNameException {
		if (isProtectedGroupName(oldName))
			renameProtectedGroup(oldName, newName);
		else {
			PermissionGroup grp = getGroup(oldName);
			this.removeGroup(grp);
			grp.setName(newName);
			this.newGroup(grp);
		}
	}

	abstract public Collection<PermissionGroup> getProtectedGroups();

	abstract public boolean isProtectedGroup(PermissionGroup grp);

	abstract public boolean isProtectedGroupName(String name);

	abstract public void renameProtectedGroup(String oldName, String newName) throws InvalidNameException;

}
