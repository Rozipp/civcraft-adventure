/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;

public class ConfigBuildableInfo {
	public String id = "";
	public String template_name = "";
	public int templateYShift = 0;
	public String displayName = "";
	public String require_tech = null;
	public String require_upgrade = null;
	public String require_structure = null;
	public int gui_item_id = 1;
	public int gui_slot;
	public int limit = 0;
	public ArrayList<String> signs = new ArrayList<String>();
	public double cost = 0;
	public double upkeep = 0;
	public int hammer_cost = 0;
	public int max_hitpoints = 0;
	public Boolean destroyable = false;
	public Boolean allow_outside_town = false;
	public Boolean isWonder = false;
	public Integer regenRate = 0;
	public Boolean tile_improvement = false;
	public Integer points = 0;
	public boolean allow_demolish = true;
	public boolean strategic = false;
	public boolean ignore_floating = false;
	public List<HashMap<String, String>> components = new LinkedList<HashMap<String, String>>();
	public boolean has_template = true;

	public ConfigBuildableInfo() {
	}

	public ConfigBuildableInfo(String id, String displayName, boolean ignore_floating, String template_name, boolean tile_improvement, int max_hitpoints, int templateYShift) {
		this.id = id;
		this.displayName = displayName;
		this.ignore_floating = ignore_floating;
		this.template_name = template_name;
		this.tile_improvement = tile_improvement;
		this.max_hitpoints = max_hitpoints;
		this.templateYShift = templateYShift;
	}

	public boolean isAvailable(Town town) {
		if (town == null) return true;
		if (!town.hasTechnology(require_tech)) return false;
		if (!town.hasUpgrade(require_upgrade)) return false;
		if (!town.BM.hasStructure(require_structure)) return false;
		if (limit != 0 && town.BM.getAllStructuresById(id).size() >= limit) return false;
		return true;
	}

	public static void loadConfig(FileConfiguration cfg, String path, Map<String, ConfigBuildableInfo> structureMap, boolean isWonder) {
		structureMap.clear();
		List<Map<?, ?>> structures = cfg.getMapList(path);
		for (Map<?, ?> struct : structures) {
			Object obj;
			ConfigBuildableInfo sinfo = new ConfigBuildableInfo();

			sinfo.id = (String) struct.get("id");
			sinfo.template_name = ((obj = struct.get("template")) == null) ? null : (String) obj;
			sinfo.templateYShift = ((obj = struct.get("template_y_shift")) == null) ? 0 : (Integer) obj;
			sinfo.displayName = (String) struct.get("displayName");
			sinfo.require_tech = ((obj = struct.get("require_tech")) == null) ? null : (String) obj;
			sinfo.require_upgrade = ((obj = struct.get("require_upgrade")) == null) ? null : (String) obj;
			sinfo.require_structure = ((obj = struct.get("require_structure")) == null) ? null : (String) obj;
			sinfo.gui_item_id = ((obj = struct.get("gui_item_id")) == null) ? 1 : (Integer) obj;
			sinfo.gui_slot = ((obj = struct.get("gui_slot")) == null) ? 0 : (Integer) obj;
			sinfo.limit = ((obj = struct.get("limit")) == null) ? 1 : (Integer) obj;
			sinfo.cost = (Double) struct.get("cost");
			sinfo.upkeep = (Double) struct.get("upkeep");
			sinfo.hammer_cost = (Integer) struct.get("hammer_cost");
			sinfo.max_hitpoints = (Integer) struct.get("max_hitpoints");
			sinfo.destroyable = (Boolean) struct.get("destroyable");
			sinfo.allow_outside_town = (Boolean) struct.get("allow_outside_town");
			sinfo.regenRate = (Integer) struct.get("regen_rate");
			sinfo.isWonder = isWonder;
			sinfo.points = (Integer) struct.get("points");

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

			sinfo.tile_improvement = ((obj = struct.get("tile_improvement")) == null) ? false : (Boolean) obj;
			sinfo.allow_demolish = ((obj = struct.get("allow_demolish")) == null) ? true : (Boolean) obj;
			sinfo.strategic = ((obj = struct.get("strategic")) == null) ? false : (Boolean) obj;
			sinfo.ignore_floating = ((obj = struct.get("ignore_floating")) == null) ? false : (Boolean) obj;
			sinfo.has_template = ((obj = struct.get("has_template")) == null) ? true : (Boolean) obj;

			if (isWonder) sinfo.strategic = true;

			structureMap.put(sinfo.id, sinfo);
		}
		CivLog.info("Loaded " + structureMap.size() + " structures.");
	}

}
