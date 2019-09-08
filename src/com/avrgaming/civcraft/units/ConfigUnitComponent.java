package com.avrgaming.civcraft.units;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigUnitComponent {

	public String id;
	public String name;
	public String ammunition;
	public String require_tech;
	public HashMap<String, Integer> require_component = new HashMap<>();
	public int require_level;
	public int require_level_upgrade;
	public int max_upgrade;
	public int gui_item_id;
	public int gui_slot;
	public String[] gui_lore;

	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigUnitComponent> components) {
		components.clear();
		List<Map<?, ?>> configUnitComponents = cfg.getMapList("unit_components");
		for (Map<?, ?> b : configUnitComponents) {
			ConfigUnitComponent component = new ConfigUnitComponent();

			Object temp;
			component.id = (String) b.get("id");
			component.name = (String) b.get("name");
			component.ammunition = (String) b.get("ammunition");
			component.require_tech = (String) b.get("require_tech");

			List<?> temp_require_component = (List<?>) b.get("require_component");
			if (temp_require_component != null) {
				for (Object obj : temp_require_component) {
					String[] temp_split = ((String) obj).split(" ");
					if (temp_split.length >= 2) component.require_component.put(temp_split[0], Integer.valueOf(temp_split[1]));
					if (temp_split.length == 1) component.require_component.put(temp_split[0], 1);
				}
			}

			component.require_level = ((temp = b.get("require_level")) != null) ? (Integer) temp : 0;
			component.require_level_upgrade = ((temp = b.get("require_level_upgrade")) != null) ? (Integer) temp : 0;
			component.max_upgrade = ((temp = b.get("max_upgrade")) != null) ? (Integer) temp : 1;

			component.gui_item_id = (Integer) b.get("gui_item_id");
			component.gui_slot = (Integer) b.get("gui_slot");

			List<?> ai = (List<?>) b.get("gui_lore");
			if (ai != null) {
				component.gui_lore = new String[ai.size()];
				int i = 0;
				for (Object obj : ai) {
					component.gui_lore[i] = (String) obj;
					i++;
				}
			}
			components.put(component.id, component);
		}

		CivLog.info("Loaded " + components.size() + " unitComponents.");
	}
}
