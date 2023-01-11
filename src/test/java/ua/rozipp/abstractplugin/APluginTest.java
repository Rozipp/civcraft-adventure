package ua.rozipp.abstractplugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ua.rozipp.abstractplugin.APlugin;

@RunWith(MockitoJUnitRunner.class)
public class APluginTest {

	private static APlugin plugin;

	@BeforeAll
	public static void beforeClass() {
		plugin = StaticPlugin.getPlugin();
	}

	@Test
	void onEnable() {
		Assertions.assertNotNull(plugin.getTaskMaster());
		Assertions.assertNotNull(plugin.getSetting());
		Assertions.assertNotNull(plugin.getLocalizer());
		Assertions.assertNotNull(plugin.getMessenger());
		Assertions.assertNotNull(plugin.getCommander());
	}
}