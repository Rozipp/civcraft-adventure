package com.avrgaming.civcraft.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMarketItem;
import com.avrgaming.civcraft.construct.Cave;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.database.ConnectionPool;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Coalition;
import com.avrgaming.civcraft.object.MissionLogger;
import com.avrgaming.civcraft.object.NamedObject;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Report;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.object.WallBlock;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.sessiondb.SessionDatabase;
import com.avrgaming.civcraft.structure.RoadBlock;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.units.UnitObject;
import com.avrgaming.civcraft.util.BiomeCache;
import com.avrgaming.global.perks.PerkManager;
import com.avrgaming.global.scores.ScoreManager;

public class SQL {

	public static String tb_prefix = "";

	public static ConnectionPool gameDatabase;
//	public static ConnectionPool globalDatabase;

	public static void initialize() throws InvalidConfiguration, SQLException, ClassNotFoundException {
		gameDatabase = SQL.createConnectionPool("mysql", "DATA");
//		globalDatabase = SQL.createConnectionPool("global_database", "GLOBAL");
		
		tb_prefix = CivSettings.getStringBase("mysql.table_prefix");

		CivGlobal.perkManager = new PerkManager();
		CivGlobal.perkManager.init();

		CivLog.heading("Initializing SQL Finished");
	}

	private static ConnectionPool createConnectionPool(String pref, String name) throws InvalidConfiguration, ClassNotFoundException, SQLException {
		CivLog.heading("Initializing " + name + " SQL Database");
		String useSSL = "false";

		useSSL = CivSettings.getStringBase(pref+".useSSL");
		String hostname = CivSettings.getStringBase(pref+".hostname");
		String port = CivSettings.getStringBase(pref+".port");
		String db_name = CivSettings.getStringBase(pref+".database");
		String username = CivSettings.getStringBase(pref+".username");
		String password = CivSettings.getStringBase(pref+".password");
		String dsn = "jdbc:mysql://" + hostname + ":" + port + "/" + tb_prefix + db_name + "?useSSL=" + useSSL + "&requireSSL=" + useSSL;

		CivLog.info("\t Using " + hostname + ":" + port + " user:" + username + " DB:" + db_name);

		CivLog.info("\t Building connection pool for " + name + " database.");
		ConnectionPool gameDatabase = new ConnectionPool(dsn, username, password);
		CivLog.info("\t Connected to " + name + " database");
		return gameDatabase;
	}
	
	public static void initCivObjectTables() throws SQLException {
		CivLog.heading("Building Civ Object Tables.");

		SessionDatabase.init();
		BiomeCache.init();
		Civilization.init();
		Town.init();
		Resident.init();
		Relation.init();
		TownChunk.init();
		Structure.init();
		Wonder.init();
		WallBlock.init();
		RoadBlock.init();
		PermissionGroup.init();
		Coalition.init();
		TradeGood.init();
		Cave.init();
		UnitObject.init();
		ProtectedBlock.init();
		BonusGoodie.init();
		MissionLogger.init();
		EventTimer.init();
		Camp.init();
		ConfigMarketItem.init();
		RandomEvent.init();
		ConstructSign.init();
		Report.init();

		CivLog.heading("Building Global Tables!!");
		ScoreManager.init();

		CivLog.info("----- Done Building Tables ----");

	}

	public static Connection getGameConnection() throws SQLException {
		return gameDatabase.getConnection();
	}

	public static Connection getGlobalConnection() throws SQLException {
//		return globalDatabase.getConnection();
		return gameDatabase.getConnection();
	}

//	public static Connection getPerkConnection() throws SQLException {
	//CivLog.debug("get connection ----> free conns:"+SQL.getGameDatabaseStats().getTotalFree()+" leased:"+SQL.getGameDatabaseStats().getTotalLeased());
//		if (SQL.getPerkDatabaseStats().getTotalFree() == 0) {
//			try {
//				throw new CivException("No more free connections! Possible connection leak!");
//			} catch (CivException e) {
//				e.printStackTrace();
//			}
//		}
//
//		return perkDatabase.getConnection();
//	}

	public static boolean hasTable(String name) throws SQLException {
		Connection context = null;
		ResultSet result = null;
		try {
			context = getGameConnection();
			DatabaseMetaData dbm = context.getMetaData();
			String[] types = {"TABLE"};

			result = dbm.getTables(null, null, SQL.tb_prefix + name, types);
			if (result.next()) {
				return true;
			}
			return false;
		} finally {
			SQL.close(result, null, context);
		}
	}

	public static boolean hasGlobalTable(String name) throws SQLException {
		Connection global_context = null;
		ResultSet rs = null;

		try {
			global_context = getGlobalConnection();
			DatabaseMetaData dbm = global_context.getMetaData();
			String[] types = {"TABLE"};
			rs = dbm.getTables(null, null, name, types);
			if (rs.next()) {
				return true;
			}
			return false;

		} finally {
			SQL.close(rs, null, global_context);
		}
	}

	public static boolean hasColumn(String tablename, String columnName) throws SQLException {
		Connection context = null;
		ResultSet result = null;

		try {
			context = getGameConnection();
			DatabaseMetaData dbm = context.getMetaData();
			result = dbm.getColumns(null, null, SQL.tb_prefix + tablename, columnName);
			boolean found = result.next();
			return found;
		} finally {
			SQL.close(result, null, context);
		}
	}

	public static void addColumn(String tablename, String columnDef) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;

		try {
			String table_alter = "ALTER TABLE " + SQL.tb_prefix + tablename + " ADD " + columnDef;
			context = getGameConnection();
			ps = context.prepareStatement(table_alter);
			ps.execute();
			CivLog.info("\tADDED:" + columnDef);
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public static boolean hasGlobalColumn(String tablename, String columnName) throws SQLException {
		Connection global_context = null;
		ResultSet rs = null;

		try {
			global_context = getGlobalConnection();
			DatabaseMetaData dbm = global_context.getMetaData();
			rs = dbm.getColumns(null, null, tablename, columnName);

			try {
				boolean found = rs.next();
				return found;
			} finally {
				rs.close();
			}

		} finally {
			SQL.close(rs, null, global_context);
		}
	}
	public static void addGlobalColumn(String tablename, String columnDef) throws SQLException {
		Connection global_context = null;
		PreparedStatement ps = null;

		try {
			global_context = SQL.getGlobalConnection();
			String table_alter = "ALTER TABLE " + tablename + " ADD " + columnDef;

			ps = global_context.prepareStatement(table_alter);
			ps.execute();
			CivLog.info("\tADDED GLOBAL:" + columnDef);
		} finally {
			SQL.close(null, ps, global_context);
		}
	}

	public static void updateNamedObjectAsync(NamedObject obj, HashMap<String, Object> hashmap, String tablename) {
		TaskMaster.asyncTask("", new SQLUpdateNamedObjectTask(obj, hashmap, tablename), 0);
	}

	public static void updateNamedObject(SQLObject obj, HashMap<String, Object> hashmap, String tablename) throws SQLException {
		if (obj.isDeleted()) {
			return;
		}

		if (obj.getId() == 0) {
			obj.setId(SQL.insertNow(hashmap, tablename));
		} else {
			SQL.update(obj.getId(), hashmap, tablename);
		}
	}

	public static void update(int id, HashMap<String, Object> hashmap, String tablename) throws SQLException {
		hashmap.put("id", id);
		update(hashmap, "id", tablename);
	}

	public static void update(HashMap<String, Object> hashmap, String keyname, String tablename) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;

		try {
			String sql = "UPDATE `" + SQL.tb_prefix + tablename + "` SET ";
			String where = " WHERE `" + keyname + "` = ?;";
			ArrayList<Object> values = new ArrayList<Object>();

			Object keyValue = hashmap.get(keyname);
			hashmap.remove(keyname);

			Iterator<String> keyIter = hashmap.keySet().iterator();
			while (keyIter.hasNext()) {
				String key = keyIter.next();

				sql += "`" + key + "` = ?";
				sql += "" + (keyIter.hasNext() ? ", " : " ");
				values.add(hashmap.get(key));
			}

			sql += where;

			context = SQL.getGameConnection();
			ps = context.prepareStatement(sql);

			int i = 1;
			for (Object value : values) {
				if (value instanceof String) {
					ps.setString(i, (String) value);
				} else
					if (value instanceof Integer) {
						ps.setInt(i, (Integer) value);
					} else
						if (value instanceof Boolean) {
							ps.setBoolean(i, (Boolean) value);
						} else
							if (value instanceof Double) {
								ps.setDouble(i, (Double) value);
							} else
								if (value instanceof Float) {
									ps.setFloat(i, (Float) value);
								} else
									if (value instanceof Long) {
										ps.setLong(i, (Long) value);
									} else {
										ps.setObject(i, value);
									}
				i++;
			}

			ps.setObject(i, keyValue);

			if (ps.executeUpdate() == 0) {
				insertNow(hashmap, tablename);
			}
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public static void insert(HashMap<String, Object> hashmap, String tablename) {
		TaskMaster.asyncTask(new SQLInsertTask(hashmap, tablename), 0);
	}

	public static int insertNow(HashMap<String, Object> hashmap, String tablename) throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;

		try {
			String sql = "INSERT INTO " + SQL.tb_prefix + tablename + " ";
			String keycodes = "(";
			String valuecodes = " VALUES ( ";
			ArrayList<Object> values = new ArrayList<Object>();

			Iterator<String> keyIter = hashmap.keySet().iterator();
			while (keyIter.hasNext()) {
				String key = keyIter.next();

				keycodes += key;
				keycodes += "" + (keyIter.hasNext() ? "," : ")");

				valuecodes += "?";
				valuecodes += "" + (keyIter.hasNext() ? "," : ")");

				values.add(hashmap.get(key));
			}

			sql += keycodes;
			sql += valuecodes;

			context = SQL.getGameConnection();
			ps = context.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			int i = 1;
			for (Object value : values) {
				if (value instanceof String) {
					ps.setString(i, (String) value);
				} else
					if (value instanceof Integer) {
						ps.setInt(i, (Integer) value);
					} else
						if (value instanceof Boolean) {
							ps.setBoolean(i, (Boolean) value);
						} else
							if (value instanceof Double) {
								ps.setDouble(i, (Double) value);
							} else
								if (value instanceof Float) {
									ps.setFloat(i, (Float) value);
								} else
									if (value instanceof Long) {
										ps.setLong(i, (Long) value);
									} else {
										ps.setObject(i, value);
									}
				i++;
			}

			ps.execute();
			int id = 0;
			rs = ps.getGeneratedKeys();

			while (rs.next()) {
				id = rs.getInt(1);
				break;
			}

			if (id == 0) {
				String name = (String) hashmap.get("name");
				if (name == null) {
					name = "Unknown";
				}

				CivLog.error("SQL ERROR: Saving an SQLObject returned a 0 ID! Name:" + name + " Table:" + tablename);
			}
			return id;

		} finally {
			SQL.close(rs, ps, context);
		}
	}

	public static void deleteNamedObject(SQLObject obj, String tablename) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;

		try {
			String sql = "DELETE FROM " + SQL.tb_prefix + tablename + " WHERE `id` = ?";
			context = SQL.getGameConnection();
			ps = context.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, obj.getId());
			ps.execute();
			ps.close();
			obj.setDeleted(true);
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public static void deleteByName(String name, String tablename) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;

		try {
			String sql = "DELETE FROM " + SQL.tb_prefix + tablename + " WHERE `name` = ?";
			context = SQL.getGameConnection();
			ps = context.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.execute();
			ps.close();
		} finally {
			SQL.close(null, ps, context);
		}
	}
	public static void makeCol(String colname, String type, String TABLE_NAME) throws SQLException {
		if (!SQL.hasColumn(TABLE_NAME, colname)) {
			CivLog.info("\tCouldn't find " + colname + " column for " + TABLE_NAME);
			SQL.addColumn(TABLE_NAME, "`" + colname + "` " + type);
		}
	}

	public static void makeTable(String table_create) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement(table_create);
			ps.execute();
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public static void deleteTable(String table_clear) {
		Connection context = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGameConnection();
			ps = context.prepareStatement("DROP TABLE " + table_clear);
			CivLog.debug("deleted " + table_clear);
			ps.execute();
		} catch (SQLException e) {
			CivLog.error("not deleted " + table_clear);
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public static void makeGlobalTable(String table_create) throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;

		try {
			context = SQL.getGlobalConnection();
			ps = context.prepareStatement(table_create);
			ps.execute();
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public static void close(ResultSet rs, PreparedStatement ps, Connection context) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (ps != null) {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (context != null) {
			try {
				context.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
