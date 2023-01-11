package ua.rozipp.abstractplugin;

/*
 * This plugin needs a default_lang.yml file in the jar file. This file includes the default strings.
 */

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Мастер загрузки и использования файлов локализаций /localize/en_us.yml
 */
public class ALocalizer {
	private static final String FOLDERNAME = "localization/";
	private final APlugin plugin;
	private final ua.rozipp.abstractplugin.ASettingMaster setting;
	public List<String> localizeNames = new ArrayList<>();
	private String serverLanguage;
	private final Map<String, FileConfiguration> languageFiles = new HashMap<>();

	/**
	 * Из файла конфигурации плагина берет код языка сервера и загружает все доступные языковые файлы
	 *
	 * @param plugin  - родительский плагин
	 * @param setting - мастер настроек
	 */
	public ALocalizer(APlugin plugin, ASettingMaster setting) {
		this.plugin = plugin;
		this.setting = setting;
		serverLanguage = setting.getStringOrDefault("", "server_language", "en_us").toLowerCase();
		try {
			loadLanguageFiles();
		} catch (Exception e) {
			plugin.getLogger().severe(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * По CommandSender определяет код языка и возвращает строку из языкового файла
	 *
	 * @param sender       - тот, кому нужно вывести строку из языкового файла.
	 * @param pathToString - название строки
	 * @param args         - возможные параметры замены в строке
	 */
	public String getString(CommandSender sender, String pathToString, Object... args) {
		if (sender instanceof Player)
			return getString(((Player) sender).getLocale(), pathToString, args);
		return getString("", pathToString, args);
	}

	/**
	 * Возвращает строку из языкового файла
	 *
	 * @param language     - код языка (например en_us, ru_ru).
	 * @param pathToString - название строки
	 * @param args         - возможные параметры замены в строке
	 */
	public String getString(String language, String pathToString, Object... args) {
		String localString = pathToString;
		FileConfiguration languageFile = getLanguageFile(language);
		localString = languageFile.getString(pathToString);
		if (localString == null) {
			addNewString(languageFile, language, pathToString);
			return pathToString;
		}
		return compoundedArgs(localString, args);
	}

	private void addNewString(FileConfiguration languageFile, String language, String pathToString) {
		String fileName = FOLDERNAME + language + ".yml";
		try(FileWriter writer = new FileWriter(plugin.getDataFolder().getPath() + "/" + fileName, true)){
			writer.append(pathToString + ": \"" + pathToString + "\"");
			writer.flush();
			plugin.getLogger().info("Added new string \"" + pathToString + "\" to Configuration file: " + fileName);
		} catch (IOException e) {
			plugin.getLogger().severe("Can not added new string \"" + pathToString + "\" to Configuration file: " + fileName);
		}
	}

	private String compoundedArgs(String string, Object... args) {
		if (args.length == 0) return string;
		try {
			for (int arg = 0; arg < args.length; ++arg) {
				string = string.replace("%" + arg, args[arg].toString());
			}
			return string;
		} catch (IllegalFormatException e1) {
			return string + " - [" + getString("", "string_formatting_error") + "]";
		}
	}

	private void loadLanguageFiles() throws Exception {
		File folder = new File(plugin.getDataFolder(), FOLDERNAME);
		if (!folder.exists()) {
			String streamingFileName = serverLanguage;
			try {
				plugin.saveResource(FOLDERNAME + serverLanguage + ".yml", true);
			} catch (Exception e1) {
				e1.printStackTrace();
				streamingFileName = "en_us";
				plugin.saveResource(FOLDERNAME + streamingFileName + ".yml", true);
			}
			plugin.getLogger().severe("Language file: " + FOLDERNAME + streamingFileName + ".yml" + " was missing. Streaming to disk from Jar.");
		}

		for (String fileName : Objects.requireNonNull(folder.list()))
			localizeNames.add(fileName.substring(0, fileName.length() - 4).toLowerCase());
		for (String fileName : localizeNames)
			try {
				FileConfiguration fff = setting.loadFileConfig(FOLDERNAME + fileName + ".yml", false);
				languageFiles.put(fileName, fff);
			} catch (Exception e) {
				plugin.getLogger().severe("Language pack (" + FOLDERNAME + fileName + ".yml" + ") not loaded.");
				e.printStackTrace();
			}

		if (!languageFiles.containsKey(serverLanguage))
			try {
				languageFiles.put(serverLanguage, setting.loadFileConfig(FOLDERNAME + serverLanguage + ".yml", true));
			} catch (Exception e) {
				plugin.getLogger().severe("Server language pack (" + serverLanguage + ") not found. Server language reset to \"en_us\"");
				serverLanguage = "en_us";
				if (!languageFiles.containsKey(serverLanguage))
					languageFiles.put(serverLanguage, setting.loadFileConfig(FOLDERNAME + serverLanguage + ".yml", true));
			}
	}

	private FileConfiguration getLanguageFile(String language) {
		if (language == null || language.isEmpty() || !languageFiles.containsKey(language.toLowerCase()))
			return languageFiles.get(serverLanguage);
		return languageFiles.get(language.toLowerCase());
	}

}