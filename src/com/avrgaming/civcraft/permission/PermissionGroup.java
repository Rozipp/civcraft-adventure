/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.permission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionGroup extends SQLObject {

	private Map<String, Resident> members = new ConcurrentHashMap<String, Resident>();
	/* Only cache towns as the 'civ' can change when a town gets conquered or gifted/moved. */
	private Town town = null;
	private Civilization civ = null;

	private int civId;
	private int townId;

	public PermissionGroup(Civilization civ, String name) throws InvalidNameException {
		this.civId = civ.getId();
		this.civ = civ;
		this.setName(name);
	}

	public PermissionGroup(Town town, String name) throws InvalidNameException {
		this.townId = town.getId();
		this.town = town;
		this.setName(name);
	}

	public PermissionGroup(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		try {
			this.load(rs);
		} catch (CivException e) {
			this.delete();
			throw new CivException(e.getMessage());
		}
	}

	public void addMember(Resident res) {
		members.put(res.getUuid().toString(), res);
	}

	public void removeMember(Resident res) {
		members.remove(res.getUuid().toString());
	}

	public boolean hasMember(Resident res) {
		return members.containsKey(res.getUuid().toString());
	}

	public void clearMembers() {
		members.clear();
	}

	public static final String TABLE_NAME = "GROUPS";

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" + "`id` int(11) unsigned NOT NULL auto_increment," + "`name` VARCHAR(64) NOT NULL," + "`town_id` int(11)," + "`civ_id` int(11)," + "`members` mediumtext,"
					+
					// "FOREIGN KEY (town_id) REFERENCES "+SQL.tb_prefix+"TOWN(id),"+
					// "FOREIGN KEY (civ_id) REFERENCES "+SQL.tb_prefix+"CIVILIZATIONS(id),"+
					"PRIMARY KEY (`id`)" + ")";

			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.setTownId(rs.getInt("town_id"));
		this.setCivId(rs.getInt("civ_id"));
		loadMembersFromSaveString(rs.getString("members"));

		if (townId != 0) {
			town = CivGlobal.getTownFromId(this.getTownId());
			if (town == null) {
				CivLog.warning("TownChunk tried to load without a town...");
				if (CivGlobal.isHaveTestFlag("cleanupDatabase")) {
					CivLog.info("CLEANING");
					this.delete();
				}
				throw new CivException("COUlD NOT FIND TOWN ID:" + this.getCivId() + " for group: " + this.getName() + " to load.");
			} else
				this.getTown().GM.addGroup(this);
		}
		if (civId != 0) {
			civ = CivGlobal.getCivFromId(this.getCivId());
			if (civ == null) {
				civ = CivGlobal.getConqueredCivFromId(this.getCivId());
				if (civ == null) {
					CivLog.warning("TownChunk tried to load without a town...");
					if (CivGlobal.isHaveTestFlag("cleanupDatabase")) {
						CivLog.info("CLEANING");
						this.delete();
					}
					throw new CivException("COUlD NOT FIND CIV ID:" + this.getCivId() + " for group: " + this.getName() + " to load.");
				}
			}

			civ.GM.addGroup(this);
		}
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();

		hashmap.put("name", this.getName());
		hashmap.put("members", this.getMembersSaveString());
		hashmap.put("town_id", this.getTownId());
		hashmap.put("civ_id", this.getCivId());

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
	}

	private String getMembersSaveString() {
		String ret = "";

		for (String name : members.keySet()) {
			ret += name + ",";
		}

		return ret;
	}

	private void loadMembersFromSaveString(String src) {
		String[] names = src.split(",");

		for (String n : names) {
			Resident res;

			if (n.length() >= 1) {
				res = CivGlobal.getResidentViaUUID(UUID.fromString(n));

				if (res != null) {
					members.put(n, res);
				}
			}
		}
	}

	public Town getTown() {
		return town;
	}

	public int getMemberCount() {
		return members.size();
	}

	public Collection<Resident> getMemberList() {
		return members.values();
	}

	public Civilization getCiv() {
		return civ;
	}

	public String getMembersString() {
		String out = "";

		for (String uuid : members.keySet()) {
			Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(uuid));
			out += res.getName() + ", ";
		}
		return out;
	}

	public static boolean hasGroup(String playerName, String groupName) {
		// try {
		// RegisteredServiceProvider<Chat> chat = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
		//
		// Player playerToCheck = Bukkit.getPlayer(playerName);
		// String group = chat.getProvider().getPrimaryGroup(playerToCheck);
		// if (playerToCheck != null) {
		// if (!groupName.contains("Helper")) {
		// String[] var3 = chat.getProvider().getPlayerGroups(playerToCheck);
		// int var4 = var3.length;
		//
		// for (int var5 = 0; var5 < var4; ++var5) {
		// String g = var3[var5];
		// if (g.equalsIgnoreCase(groupName)) {
		// return true;
		// }
		// }
		// }
		// }
		// } catch (NoClassDefFoundError e) {
		// e.printStackTrace();
		// }
		return false;
	}

	public Resident getRandomMember() {
		try {
			final Collection<Resident> members = this.members.values();
			final Resident[] residents = members.toArray(new Resident[members.size()]);
			if (residents.length > 2) {
				return residents[CivCraft.civRandom.nextInt(residents.length - 1)];
			}
			return residents[0];
		} catch (Exception error) {
			return null;
		}
	}
}
