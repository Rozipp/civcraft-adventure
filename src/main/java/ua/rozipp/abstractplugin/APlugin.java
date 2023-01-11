package ua.rozipp.abstractplugin;

import org.bukkit.plugin.java.JavaPlugin;
import ua.rozipp.abstractplugin.command.ACommander;

public class APlugin extends JavaPlugin {

	private static APlugin instance;
	private ua.rozipp.abstractplugin.ATaskMaster taskMaster;
	private ua.rozipp.abstractplugin.ASettingMaster setting;
	private ua.rozipp.abstractplugin.ALocalizer localizer;
	private ua.rozipp.abstractplugin.AMessenger messenger;
	private ACommander commander;
	private ua.rozipp.abstractplugin.AListenerMaster listenerMaster;


	public static APlugin getInstance() {
		return instance;
	}

	/**
	 * Инициализация всех объектов плагина:
	 *
	 * <li>{@link ua.rozipp.abstractplugin.ATaskMaster} - мастер организации синхронных и асинхронных задач и таймеров,
	 * <li>{@link ua.rozipp.abstractplugin.ASettingMaster} - мастер загрузки файлов настроек /data/*.yml,
	 * <li>{@link ua.rozipp.abstractplugin.ALocalizer} - мастер загрузки и использования файлов локализаций /localize/*.yml,
	 * <li>{@link ua.rozipp.abstractplugin.AMessenger} - мастер формирования сообщений игроку, серверу и глобальные сообщения с учетом локализации,
	 * <li>{@link ACommander} - мастер регистрации команд консоли,
	 * <li>{@link ua.rozipp.abstractplugin.AListenerMaster} - Помощник регистрации Listener-ов плагина
	 */
	@Override
	public void onEnable() {
		instance = this;
		taskMaster = new ua.rozipp.abstractplugin.ATaskMaster(this);
		setting = new ua.rozipp.abstractplugin.ASettingMaster(this, this.getLogger());
		localizer = new ua.rozipp.abstractplugin.ALocalizer(this, setting);
		messenger = new ua.rozipp.abstractplugin.AMessenger(this.getLogger(), localizer);
		commander = new ACommander(this);
		listenerMaster = new ua.rozipp.abstractplugin.AListenerMaster(this);

	}

	@Override
	public void onDisable() {
		getTaskMaster().stopAll();
		getListenerMaster().unregisterAllListener();
	}

	public ATaskMaster getTaskMaster() {
		return this.taskMaster;
	}

	public ASettingMaster getSetting() {
		return this.setting;
	}

	public ALocalizer getLocalizer() {
		return this.localizer;
	}

	public AMessenger getMessenger() {
		return this.messenger;
	}

	public ACommander getCommander() {
		return this.commander;
	}

	public AListenerMaster getListenerMaster() {
		return this.listenerMaster;
	}
}
