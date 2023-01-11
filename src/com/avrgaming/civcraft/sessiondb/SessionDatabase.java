/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.sessiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.sessiondb.SessionAsyncRequest.Database;
import com.avrgaming.civcraft.sessiondb.SessionAsyncRequest.Operation;


public class SessionDatabase {
	/* 
	 * This object provides interface functions to allow CivCraft objects
	 * to store temporary information in the SQL database. 
	 */
	private boolean initalized;
	
	String tb_prefix;
	
	private ConcurrentHashMap<String, ArrayList<SessionEntry>> cache = new ConcurrentHashMap<String, ArrayList<SessionEntry>>();
		
	public SessionDatabase() {
		tb_prefix = SQL.tb_prefix;
	}
	
	public static String TABLE_NAME = "SESSIONS";
	public static String GLOBAL_TABLE_NAME = "GLOBAL_SESSIONS";
	public static void init() throws SQLException {
		System.out.println("================= SESSION DB INIT ======================");
		// Check/Build SessionDB tables				
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`request_id` int(11) unsigned NOT NULL auto_increment," +
					"`key` mediumtext," +
					"`value` mediumtext," +
					"`town_id` int(11)," +
					"`civ_id` int(11)," +
					"`struct_id` int(11)," +
					"`time` long," +
					"PRIMARY KEY (`request_id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}			
		System.out.println("==================================================");
		
		System.out.println("================= GLOBAL SESSION DB INIT ======================");
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(GLOBAL_TABLE_NAME)) {
			String table_create = "CREATE TABLE " + GLOBAL_TABLE_NAME+" (" + 
					"`request_id` int(11) unsigned NOT NULL auto_increment," +
					"`key` mediumtext," +
					"`value` mediumtext," +
					"`town_id` int(11)," +
					"`civ_id` int(11)," +
					"`struct_id` int(11)," +
					"`time` long," +
					"PRIMARY KEY (`request_id`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+GLOBAL_TABLE_NAME+" table");
		} else {
			CivLog.info(GLOBAL_TABLE_NAME+" table OK!");
		}			
		System.out.println("==================================================");
	}
	
	public boolean add(String key, String value, int civ_id, int town_id, int struct_id) {
		SessionEntry se = new SessionEntry();
		se.key = key;
		se.value = value;
		se.civ_id = civ_id;
		se.town_id = town_id;
		se.struct_id = struct_id;
		se.time = System.currentTimeMillis();
		se.request_id = -1;
		
		// Add to cache map, then fire async add to DB.
	    ArrayList<SessionEntry> entries = this.cache.get(key);
		if (entries == null) {
			entries = new ArrayList<SessionEntry>();
		}
		entries.add(se);
		
		SessionAsyncRequest request = new SessionAsyncRequest(Operation.ADD, Database.GAME, tb_prefix, se);
		request.queue();
		return true;
	}
	
	public boolean global_add(String key, String value) {
		SessionEntry se = new SessionEntry();
		se.key = key;
		se.value = value;
		se.time = System.currentTimeMillis();
		se.request_id = -1;
		
		//Fire async add to DB.
		SessionAsyncRequest request = new SessionAsyncRequest(Operation.ADD, Database.GLOBAL, "GLOBAL_", se);
		request.queue();
		return true;
	}
		
	public ArrayList<SessionEntry> lookup(String key) {
		Connection cntx = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		String code;
		ArrayList<SessionEntry> retList = null;
		
		try {
			// Lookup in cache first, then look in DB.
			retList = cache.get(key);
			if (retList != null) {
				return retList;
			}
			
			// Couldnt find in cache, attempt DB lookup.
			retList = new ArrayList<SessionEntry>();
			code = "SELECT * FROM `"+ tb_prefix + "SESSIONS` WHERE `key` = ?";
	
			try {
				cntx = SQL.getGameConnection();		
				ps = cntx.prepareStatement(code);
				ps.setString(1, key);
			
				rs = ps.executeQuery();
				
				while (rs.next()) {
					SessionEntry se = new SessionEntry();
					String line;
					
					se.request_id = rs.getInt("request_id");
	
					line = rs.getString("key");
	
					if (line == null)
						break;
					else
						se.key = line;
					
					line = rs.getString("value");
					if (line == null)
						break;
					else
						se.value = line;
					
					se.civ_id = rs.getInt("civ_id");
					se.town_id = rs.getInt("town_id");
					se.struct_id = rs.getInt("struct_id");
					
					long time = rs.getLong("time");
					se.time = time;
					
					retList.add(se);
				}
	
			} catch (SQLException e) {
				CivLog.error("SQL: select sql error " + e.getMessage() + " --> " + code);
			}
			
			// Add what we found to the cache.
			cache.put(key, retList);
			
			return retList;
		} finally {
			SQL.close(rs, ps, cntx);
		}
	}
	
	public ArrayList<SessionEntry> global_lookup(String key) {
		Connection global_context = null;
		ResultSet rs = null;
		PreparedStatement s = null;
		ArrayList<SessionEntry> retList = new ArrayList<SessionEntry>();

		try {
			try {
				global_context = SQL.getGlobalConnection();	
				String code;
				code = "SELECT * FROM `GLOBAL_SESSIONS` WHERE `key` = ?";
				s = global_context.prepareStatement(code);
				s.setString(1, key);
			
				rs = s.executeQuery();
				
				while (rs.next()) {
					SessionEntry se = new SessionEntry();
					String line;
					
					se.request_id = rs.getInt("request_id");
	
					line = rs.getString("key");
	
					if (line == null)
						break;
					else
						se.key = line;
					
					line = rs.getString("value");
					if (line == null)
						break;
					else
						se.value = line;
					
					long time = rs.getLong("time");
					se.time = time;
					
					retList.add(se);
				}
			} catch (SQLException e) {
				CivLog.error("SQL: select sql error " + e.getMessage());
			}
			
			// Add what we found to the cache.
			cache.put(key, retList);
			return retList;
		} finally {
			SQL.close(rs, s, global_context);
		}
	}
		
	/* debug function to test the session DB */
	public void test() {
		add("ThisTestKey", "ThisTestData", 0, 0, 0);		

		for (SessionEntry se : lookup("ThisTestKey")) {
			System.out.println("GOT ME SOME:"+se.value);
		}
	
	}

	public boolean isInitialized() {
		return this.initalized;
	}

	public boolean delete_all(String key) {
		SessionEntry se = new SessionEntry();
		se.key = key;
		
		// Remove all from the cache
		cache.remove(key);
		
		//Fire async delete all from DB.
		SessionAsyncRequest request = new SessionAsyncRequest(Operation.DELETE_ALL, Database.GAME, tb_prefix, se);
		request.queue();
		return true;
	}
	
	public boolean delete(int request_id, String key) {
		SessionEntry se = new SessionEntry();
		se.request_id = request_id;
		
		// Remove it from the cache as well
		ArrayList<SessionEntry> entries = cache.get(key);
		if (entries != null) {
			for (SessionEntry e : entries) {
				if (e.request_id == request_id) {
					entries.remove(e);
					break;
				}
			}
			// Go ahead and remove entire array if empty now
			if (entries.size() == 0) {
				cache.remove(key);
			}
		}
		
		
		//Fire async delete reqid from DB.
		SessionAsyncRequest request = new SessionAsyncRequest(Operation.DELETE, Database.GAME, tb_prefix, se);
		request.queue();
		return true;
	}

	public boolean global_update(int request_id, String key, String newValue) {
		SessionEntry se = new SessionEntry();
		se.request_id = request_id;
		se.value = newValue;
		se.key = key;
		
		//Fire async to update DB.
		SessionAsyncRequest request = new SessionAsyncRequest(Operation.UPDATE, Database.GLOBAL, "GLOBAL_", se);
		request.queue();
		return true;
	}

	
	public boolean update(int request_id, String key, String newValue) {
		SessionEntry se = new SessionEntry();
		se.request_id = request_id;
		se.value = newValue;
		se.key = key;
		
		// Update cache as well.
		ArrayList<SessionEntry> entries = cache.get(key);
		if (entries != null) {
			for (SessionEntry e : entries) {
				if (e.request_id == request_id) {
					e.value = newValue;
				}
			}
		} else {
			entries = new ArrayList<SessionEntry>();
			entries.add(se);
			cache.put(se.key, entries);
		}
		
		//Fire async to update DB.
		SessionAsyncRequest request = new SessionAsyncRequest(Operation.UPDATE, Database.GAME, tb_prefix, se);
		request.queue();
		return true;
	}

}