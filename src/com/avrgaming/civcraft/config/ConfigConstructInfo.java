/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;

public class ConfigConstructInfo {

	public enum ConstructType {
		Global, Titles, Structure, Wonder, War
	}

	public String id = "";
	public ConstructType type;
	public String template_name;
	public int template_y_shift;
	public String displayName = "";
	public String require_tech;
	public String require_upgrade;
	public String require_structure;
	public int gui_item_id;
	public int gui_slot;
	public int limit;
	public double cost;
	public double upkeep;
	public int hammer_cost;
	public int max_hitpoints;
	public Boolean destroyable;
	public Integer regenRate;
	public Boolean tile_improvement;
	public Integer points;
	public boolean allow_demolish;
	public boolean strategic;
	public boolean ignore_floating;
	public List<HashMap<String, String>> components = new LinkedList<HashMap<String, String>>();

	public ConfigConstructInfo() {
	}

	public boolean isAvailable(Town town) {
		if (town == null) return true;
		if (!town.hasTechnology(require_tech)) return false;
		if (!town.hasUpgrade(require_upgrade)) return false;
		if (!town.BM.hasStructure(require_structure)) return false;
		if (limit != 0 && town.BM.getAllStructuresById(id).size() >= limit) return false;
		return true;
	}

	public static void loadConfig(FileConfiguration cfg, String path, Map<String, ConfigConstructInfo> structureMap) {
		// structureMap.clear();
		List<Map<?, ?>> structures = cfg.getMapList(path);
		for (Map<?, ?> struct : structures) {
			Object obj;
			ConfigConstructInfo sinfo = new ConfigConstructInfo();

			sinfo.id/*               */ = (String) struct.get("id");
			sinfo.type/*             */ = ConstructType.valueOf((String) struct.get("type"));
			sinfo.template_name/*    */ = ((obj = struct.get("template"))/*         */ == null) ? null : (String) obj;
			sinfo.template_y_shift/*   */ = ((obj = struct.get("template_y_shift"))/* */ == null) ? 0 : (Integer) obj;
			sinfo.displayName/*      */ = (String) struct.get("displayName");
			sinfo.require_tech/*     */ = ((obj = struct.get("require_tech"))/*     */ == null) ? null : (String) obj;
			sinfo.require_upgrade/*  */ = ((obj = struct.get("require_upgrade"))/*  */ == null) ? null : (String) obj;
			sinfo.require_structure/**/ = ((obj = struct.get("require_structure"))/**/ == null) ? null : (String) obj;
			sinfo.gui_item_id/*      */ = ((obj = struct.get("gui_item_id"))/*      */ == null) ? 1 : (Integer) obj;
			sinfo.gui_slot/*         */ = ((obj = struct.get("gui_slot"))/*         */ == null) ? 0 : (Integer) obj;
			sinfo.limit/*            */ = ((obj = struct.get("limit"))/*            */ == null) ? 1 : (Integer) obj;
			sinfo.cost/*             */ = ((obj = struct.get("cost"))/*             */ == null) ? 0 : (Double) obj;
			sinfo.upkeep/*           */ = ((obj = struct.get("upkeep"))/*           */ == null) ? 0 : (Double) obj;
			sinfo.hammer_cost/*      */ = ((obj = struct.get("hammer_cost"))/*      */ == null) ? 0 : (Integer) obj;
			sinfo.max_hitpoints/*    */ = ((obj = struct.get("max_hitpoints"))/*    */ == null) ? 0 : (Integer) obj;
			sinfo.destroyable/*      */ = ((obj = struct.get("destroyable"))/*      */ == null) ? false : (Boolean) obj;
			sinfo.regenRate/*        */ = ((obj = struct.get("regen_rate"))/*       */ == null) ? 0 : (Integer) obj;
			sinfo.points/*           */ = ((obj = struct.get("points"))/*           */ == null) ? 0 : (Integer) obj;
			sinfo.tile_improvement/* */ = ((obj = struct.get("tile_improvement"))/* */ == null) ? false : (Boolean) obj;
			sinfo.allow_demolish/*   */ = ((obj = struct.get("allow_demolish"))/*   */ == null) ? true : (Boolean) obj;
			sinfo.strategic/*        */ = ((obj = struct.get("strategic"))/*        */ == null) ? false : (Boolean) obj;
			sinfo.ignore_floating/*  */ = ((obj = struct.get("ignore_floating"))/*  */ == null) ? false : (Boolean) obj;

			@SuppressWarnings("unchecked")
			List<Map<?, ?>> comps = (List<Map<?, ?>>) struct.get("components");
			if (comps != null) {
				for (Map<?, ?> compObj : comps) {
					HashMap<String, String> compMap = new HashMap<String, String>();
					for (Object key : compObj.keySet()) {
						compMap.put((String) key, (String) compObj.get(key));
					}
					sinfo.components.add(compMap);
				}
			}
			structureMap.put(sinfo.id, sinfo);
		}
		CivLog.info("Loaded " + structureMap.size() + " " + path + " construct.");
	}
}
