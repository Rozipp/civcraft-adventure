package ua.rozipp.abstractplugin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ua.rozipp.abstractplugin.APlugin;

@RunWith(MockitoJUnitRunner.class)
public class ALocalizerTest {

	private static APlugin plugin;
	private static Player player;
	private static CommandSender console;

	@BeforeAll
	public static void beforeClass() {
		plugin = StaticPlugin.getPlugin();
		player = StaticPlugin.getPlayer_ru_ru();
		console = StaticPlugin.getConsole();
	}

	@Test
	void getString() {
		plugin.getMessenger().sendMessageLocalized(player, "New_Test_Message");
		plugin.getMessenger().sendMessageLocalized(console, "Finished");
	}
}