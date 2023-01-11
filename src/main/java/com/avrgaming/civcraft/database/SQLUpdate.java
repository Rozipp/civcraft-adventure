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
package com.avrgaming.civcraft.database;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.main.CivCraft;

public class SQLUpdate implements Runnable {

	private static ConcurrentLinkedQueue<SQLObject> saveObjects = new ConcurrentLinkedQueue<SQLObject>();

	public static void add(SQLObject obj) {
		if (!saveObjects.contains(obj)) saveObjects.add(obj);
	}

	public static void save() {
		for (SQLObject obj : saveObjects) {
			if (obj != null) {
				try {
					obj.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run() {
		while (!CivCraft.isDisable) {
			try {
				SQLObject obj = saveObjects.poll();
				if (obj == null) {
					if (saveObjects.isEmpty()) {
						Thread.sleep(500);
					}
					continue;
				}
				obj.saveNow();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
