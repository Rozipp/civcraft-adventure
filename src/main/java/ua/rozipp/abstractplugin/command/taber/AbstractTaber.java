package ua.rozipp.abstractplugin.command.taber;

import org.bukkit.command.CommandSender;
import ua.rozipp.abstractplugin.exception.AException;

import java.util.List;

/** Интерфейс для класов дополнения по клавише Tab
 * @author rozipp */
public interface AbstractTaber {
	List<String> getTabList(CommandSender sender, String arg) throws AException;
}
