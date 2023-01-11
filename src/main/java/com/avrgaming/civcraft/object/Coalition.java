package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Coalition extends SQLObject {

	public static final String TABLE_NAME = "COALITIONS";

	private int creatorId;
	private Map<Integer, Civilization> civs = new ConcurrentHashMap<Integer, Civilization>();

	public Coalition(String name, int creatorId) throws InvalidNameException, CivException {
		setName(name);
		this.creatorId = creatorId;
		this.save();
	}

	public Coalition(ResultSet rs) throws SQLException, InvalidNameException {
		this.load(rs);
	}

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			final String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME + " (" 
					+ "`id` int(11) unsigned NOT NULL auto_increment," // id коалиции
					+ "`name` VARCHAR(64) NOT NULL," // имя коалиции
					+ "`creatorId` int(11)," // id цивилизации основателя
					+ "`civsId` mediumtext," // id цивилизаций входящих в состав коалиции
					+ "UNIQUE KEY (`name`), PRIMARY KEY (`id`))";
			SQL.makeTable(table_create);
			CivLog.info("Created " + TABLE_NAME + " table");
		} else {
			CivLog.info(TABLE_NAME + " table OK!");
		}
	}

	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));
		this.creatorId = rs.getInt("creatorId");
		this.loadCoalitions(rs.getString("civsId"));
	}

	@Override
	public void saveNow() throws SQLException {
		final HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("name", this.getName());
		hashmap.put("creatorId", this.creatorId);
		hashmap.put("civsId", this.saveCoalitions());

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {

		/* Delete all of our towns. */
		for (Civilization civ : civs.values()) {
			civ.getDiplomacyManager().setCoalitionId(0);
		}

		CivGlobal.removeCoalition(this);
		SQL.deleteNamedObject(this, TABLE_NAME);
	}

	public Collection<Civilization> getCivs() {
		return civs.values();
	}

	public boolean hesCiv(Civilization civ) {
		return civs.containsKey(civ.getId());
	}

	public void addCiv(Civilization civ) throws CivException {
//		if (civ.getDiplomacyManager().getCoalitionId() == this.getMaterialId())
//			throw new CivException("Цивилизация "+civ.getName()+" уже состоит в коалиции "+Coalition.getCoalitionsName(civ));
		if (civ.getDiplomacyManager().getCoalitionId() != 0)
			throw new CivException("Цивилизация "+civ.getName()+" уже состоит в коалиции "+Coalition.getCoalitionsName(civ));
		civs.put(civ.getId(), civ);
		civ.getDiplomacyManager().setCoalitionId(this.getId());
	}
	public void addCiv(int civId) throws CivException {
		addCiv(CivGlobal.getCiv(civId));
	}

	public void removeCiv(Civilization civ) {
		civs.remove(civ.getId());
		civ.getDiplomacyManager().setCoalitionId(0);
		if (this.creatorId == civ.getId()) try {
			this.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void removeCiv(int civId) {
		removeCiv(CivGlobal.getCiv(civId));
	}
	
	private void loadCoalitions(String civString) {
		if (civString == null || civString.equals("")) return;

		String[] civSplit = civString.split(",");
		for (String civId : civSplit) {
			int id = Integer.parseInt(civId);
			Civilization civ = CivGlobal.getCiv(id);
			if (civ != null) {
				civs.put(id, civ);
				civ.getDiplomacyManager().setCoalitionId(this.getId());
			}
		}
	}
	private String saveCoalitions() {
		String out = "";
		for (Civilization civ : this.getCivs())
			out += civ.getId() + ",";
		return out;
	}

	public static boolean isProtectedCoalitionName(String name) {
		switch (name.toLowerCase()) {
			case "neutral" :
			case "hostile" :
			case "war" :
			case "peace" :
			case "ally" :
			case "coalition" :
				return true;
		}
		return false;
	}
	
	public int getCreatorId() {
		return creatorId;
	}
	public Civilization getCreator() {
		return CivGlobal.getCiv(creatorId);
	}
	
	public static String getCoalitionsName(Civilization civ) {
		return CivGlobal.getCoalition(civ.getDiplomacyManager().getCoalitionId()).getName();
	}
	
	public static String getCoalitionsName(int civID) {
		return CivGlobal.getCoalition(civID).getName();
	}

	public static void message (String out) {
		CivMessage.global(out);
	}
	
}
