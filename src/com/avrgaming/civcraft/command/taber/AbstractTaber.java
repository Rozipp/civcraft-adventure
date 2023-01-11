package com.avrgaming.civcraft.command.taber;

import java.util.List;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;

/** Интерфейс для класов дополнения по клавише Tab
 * @author rozipp */
public interface AbstractTaber {
	List<String> getTabList(CommandSender sender, String arg) throws CivException;
}
