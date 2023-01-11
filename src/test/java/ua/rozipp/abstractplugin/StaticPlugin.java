package ua.rozipp.abstractplugin;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mockito.Mockito;
import ua.rozipp.abstractplugin.*;
import ua.rozipp.abstractplugin.command.ACommander;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class StaticPlugin {
	private static final String folderPatch = "/home/rozipp/IdeaProjects/APlugin/src/main/resources/";
	private static final FileConfiguration newConfig = new YamlConfiguration();
	private static APlugin plugin = null;
	private static Player player = null;
	private static CommandSender console = null;

	public static APlugin getPlugin() {
		if (plugin == null) {
			plugin = Mockito.mock(APlugin.class);
			System.out.println("-----APlugin created for test-----");
			Mockito.doNothing().when(plugin).saveDefaultConfig();
			Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("Test"));
			Mockito.when(plugin.getDataFolder()).thenReturn(new File(folderPatch));
			try {
				newConfig.load(new File(folderPatch + "config.yml"));
			} catch (IOException | InvalidConfigurationException e) {
				e.printStackTrace();
			}
			Mockito.when(plugin.getConfig()).thenReturn(newConfig);

			ATaskMaster taskMaster = new ATaskMaster(plugin);
			ASettingMaster setting = new ASettingMaster(plugin, plugin.getLogger());
			ALocalizer localizer = new ALocalizer(plugin, setting);
			AMessenger messenger = new AMessenger(plugin.getLogger(), localizer);
			ACommander commander = new ACommander(plugin);
			AListenerMaster listenerMaster = new AListenerMaster(plugin);

			Mockito.when(plugin.getTaskMaster()).thenReturn(taskMaster);
			Mockito.when(plugin.getSetting()).thenReturn(setting);
			Mockito.when(plugin.getLocalizer()).thenReturn(localizer);
			Mockito.when(plugin.getMessenger()).thenReturn(messenger);
			Mockito.when(plugin.getCommander()).thenReturn(commander);
			Mockito.when(plugin.getListenerMaster()).thenReturn(listenerMaster);
		}
		return plugin;
	}

	public static Player getPlayer_ru_ru() {
		if (player == null) {
			player = Mockito.mock(Player.class);
			Mockito.doAnswer(invocation -> {
						Object[] args = invocation.getArguments();
						Player player = (Player) invocation.getMock();
						System.out.println("[Message to \"" + player.getName() + "\"]: " + args[0].toString());
						return null;
					}
			).when(player).sendMessage(Mockito.anyString());
			Mockito.when(player.getLocale()).thenReturn("ru_ru");
			Mockito.when(player.getName()).thenReturn("TestPlayer");
		}
		return player;
	}

	public static CommandSender getConsole() {
		if (console ==null) {
			console = Mockito.mock(CommandSender.class);
			Mockito.doAnswer(invocation -> {
						Object[] args = invocation.getArguments();
						Object mock = invocation.getMock();
						System.out.println("[MSG to console]: " + args[0].toString());
						return null;
					}
			).when(console).sendMessage(Mockito.anyString());
		}
		return console;
	}
}