package ua.rozipp.abstractplugin;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AListenerMaster {

	private final List<Listener> listeners = new ArrayList<>();
	private final APlugin plugin;

	public AListenerMaster(APlugin plugin) {
		this.plugin = plugin;
	}

	public int getCountListener(){
		return listeners.size();
	}

	public void unregisterAllListener() {
		Iterator<Listener> iter = listeners.iterator();
		synchronized (listeners) {
			while (iter.hasNext())
				HandlerList.unregisterAll(iter.next());
			listeners.clear();
		}
	}

	public void registerListener(Listener listener) {
		synchronized (listeners) {
			plugin.getServer().getPluginManager().registerEvents(listener, plugin);
			listeners.add(listener);
		}
	}

	public boolean hasPlugin(String name) {
		Plugin p;
		p = plugin.getServer().getPluginManager().getPlugin(name);
		return (p != null);
	}

}
