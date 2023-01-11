package ua.rozipp.abstractplugin;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.rozipp.abstractplugin.exception.InvalidConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ASettingMaster {

	private static final String FOLDERNAME = "data/";
	private final APlugin plugin;
	private final Logger logger;
	private final FileConfiguration mainConfig;
	private final Map<String, FileConfiguration> configsFiles = new HashMap<>();

	public ASettingMaster(APlugin plugin, Logger logger){
		//save a copy of the default config.yml if one is not there
		this.plugin = plugin;
		this.logger = logger;
		plugin.saveDefaultConfig();
		mainConfig = plugin.getConfig();

		plugin.getLogger().setLevel(Level.parse(getStringOrDefault("", "loglevel", "INFO")));

	}

	public void loadConfig(String name) throws IOException, InvalidConfigurationException {
		configsFiles.put(name, loadFileConfig(FOLDERNAME + name + ".yml", false));
	}

	private FileConfiguration getFileConfig(String fileName){
		if (fileName == null || fileName.isEmpty() || !configsFiles.containsKey(fileName)) return mainConfig;
		return configsFiles.get(fileName);
	}

	public FileConfiguration loadFileConfig(String filepath, boolean replace) throws IOException, InvalidConfigurationException {
		File file = new File(plugin.getDataFolder().getPath() + "/" + filepath);
		if (!file.exists()) {
			logger.warning("Configuration file: " + filepath + " was missing. Streaming to disk from Jar.");
			plugin.saveResource(filepath, replace);
		}

		YamlConfiguration cfg = new YamlConfiguration();
		cfg.load(file);
		logger.fine("Loading Configuration file: " + filepath);
		return cfg;
	}

	public void saveFileConfig(String filename, FileConfiguration fileConfiguration) {
		String filePath = plugin.getDataFolder().getPath() + "/" + filename;
		try {
			FileWriter fw = new FileWriter(filePath);
			logger.info("Save Configuration file: " + filename);
		} catch (Exception e) {
			logger.severe("Can not save Configuration file: " + filename);
		}
	}

	public String getString(String fileName, String path) throws InvalidConfiguration {
		FileConfiguration cfg = getFileConfig(fileName);
		if (!cfg.contains(path)) throw new InvalidConfiguration(FOLDERNAME + fileName + ".yml", path);
		logger.config(path + ": " + cfg.getString(path));
		return cfg.getString(path);
	}

	public int getInteger(String fileName, String path) throws InvalidConfiguration {
		FileConfiguration cfg = getFileConfig(fileName);
		if (!cfg.contains(path)) throw new InvalidConfiguration(FOLDERNAME + fileName + ".yml", path);
		logger.config(path + ": " + cfg.getInt(path));
		return cfg.getInt(path);
	}

	public double getDouble(String fileName, String path) throws InvalidConfiguration {
		FileConfiguration cfg = getFileConfig(fileName);
		if (!cfg.contains(path)) throw new InvalidConfiguration(FOLDERNAME + fileName + ".yml", path);
		logger.config(path + ": " + cfg.getDouble(path));
		return cfg.getDouble(path);
	}

	public boolean getBoolean(String fileName, String path) throws InvalidConfiguration {
		FileConfiguration cfg = getFileConfig(fileName);
		logger.info(cfg.getCurrentPath());
		if (!cfg.contains(path)) throw new InvalidConfiguration(FOLDERNAME + fileName + ".yml", path);
		logger.config(path + ": " + cfg.getBoolean(path));
		return cfg.getBoolean(path);
	}

	public String getStringOrDefault(String fileName, String path, String defaultValue){
		try {
			return getString(fileName, path);
		} catch (InvalidConfiguration e) {
			return defaultValue;
		}
	}

	public int getIntegerOrDefault(String fileName, String path, int defaultValue){
		try {
			return getInteger(fileName, path);
		} catch (InvalidConfiguration e) {
			return defaultValue;
		}
	}

	public double getDoubleOrDefault(String fileName, String path, double defaultValue){
		try {
			return getDouble(fileName, path);
		} catch (InvalidConfiguration e) {
			return defaultValue;
		}
	}

	public boolean getBooleanOrDefault(String fileName, String path, boolean defaultValue){
		try {
			return getBoolean(fileName, path);
		} catch (InvalidConfiguration e) {
			return defaultValue;
		}
	}

}
