package com.avrgaming.civcraft.construct.template;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTheme {

	public static Set<ConfigTheme> configThemes = new HashSet<>();
	public static Map<String, Set<ConfigTheme>> configThemesforCnstruct = new HashMap<>();

	public String id;
	public String display_name;
	public String simple_name;
	public int item_id = 1;
	public int data = 0;

	public Set<String> constrNames = new HashSet<>();

	public ConfigTheme() {
	}

	public static void loadAllTemplateTheme() throws FileNotFoundException {
		File mainFolder = new File("templates/themes/");
		if (!mainFolder.exists()) throw new FileNotFoundException("Not found templates/themes/ folder");

		for (String theme : mainFolder.list()) {
			try {
				ConfigTheme.load(new File(mainFolder.getPath() + "/" + theme + "/"));
			} catch (IOException | InvalidConfigurationException e) {
				CivLog.error(e.getMessage());
			}
		}
	}

	public static void load(File folder) throws FileNotFoundException, IOException, InvalidConfigurationException {
		File configFile = new File(folder.getPath() + "/" + "config.yml");

		FileConfiguration fconfig = new YamlConfiguration();
		fconfig.load(configFile);

		ConfigTheme configTheme = new ConfigTheme();
		configTheme.id = fconfig.getString("id");
		configTheme.display_name = fconfig.getString("display_name");
		configTheme.simple_name = fconfig.getString("simple_name");
		configTheme.item_id = fconfig.getInt("item_id");
		configTheme.data = fconfig.getInt("data");

		for (String constrName : folder.list()) {
			if (constrName.equals("config.yml")) continue;
			configTheme.constrNames.add(constrName);
		}
		configThemes.add(configTheme);
		
		CivLog.info("Added " + folder.getName() + " theme with " + configTheme.constrNames.size() + " counstructs.");
	}

	public static Set<ConfigTheme> getConfigThemeForConstruct(String constructNames) {
		if (configThemesforCnstruct.containsKey(constructNames)) return configThemesforCnstruct.get(constructNames);

		Set<ConfigTheme> res = new HashSet<>();
		for (ConfigTheme ct : configThemes) {
			if (ct.constrNames.contains(constructNames)) {
				res.add(ct);
			}
		}
		if (!res.isEmpty()) configThemesforCnstruct.put(constructNames, res);
		return res;
	}
}
