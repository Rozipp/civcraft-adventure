package ua.rozipp.sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;

public class SoundManager {

	public static int MAX_RADIUS_CHUNK = 24;

	private static HashMap<String, ConfigSound> sounds = new HashMap<String, ConfigSound>();

	public static void playSound(String key, Location local) {
		Location loc = local.clone();
		TaskMaster.asyncTask(new Runnable() {
			@Override
			public void run() {
				ConfigSound configSound = sounds.get(key.toLowerCase());
				if (configSound == null) {
					CivLog.error("Sound \"" + key + "\" not found");
					return;
				}
				int radius = configSound.radius;
				if (radius > MAX_RADIUS_CHUNK) radius = MAX_RADIUS_CHUNK;

				if (radius == 0) {
					for (Entity e : loc.getChunk().getEntities()) {
						if (e.getLocation().distance(loc) <= radius) {
							if (e instanceof Player) configSound.playVoices((Player) e, loc);
						}
					}
				} else {
					int x = (int) loc.getX(), y = (int) loc.getY(), z = (int) loc.getZ();
					int radiusSqr = radius * (radius + 1);
					for (int chX = 0 - radius; chX <= radius; chX++) {
						for (int chZ = 0 - radius; chZ <= radius; chZ++) {
							if (chX * chX + chZ * chZ > radiusSqr) continue;
							Location newloc = new Location(loc.getWorld(), x + (chX * 16), y, z + (chZ * 16));
							TaskMaster.syncTask(new Runnable() {
								@Override
								public void run() {
									Chunk chunk = newloc.getChunk();
									for (Entity e : chunk.getEntities()) {
										if (e instanceof Player) configSound.playVoices((Player) e, loc);
									}
								}
							});
						}
					}
				}
			}
		}, 0);
	}

	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg) {
		sounds.clear();
		List<Map<?, ?>> configSounds = cfg.getMapList("elements");
		Object ob;
		for (Map<?, ?> b : configSounds) {
			ConfigSound cs = new ConfigSound();
			try {
				if ((cs.key = (String) b.get("key")) == null) throw new CivException("null елемент в key");
				cs.radius = ((ob = b.get("radius")) == null) ? 0 : (Integer) ob;
				List<Map<?, ?>> csv = (List<Map<?, ?>>) b.get("voice");
				if (csv != null) {
					for (Map<?, ?> c : csv) {
						Voice voice = new Voice();
						voice.type = (String) c.get("type");
						Double dd = (ob = c.get("delay")) == null ? 0 : (Double) ob;
						dd = dd * 20;
						voice.delay = dd.longValue();
						voice.pitch = (ob = c.get("pitch")) == null ? 0 : ((Double) ob).floatValue();
						cs.voices.add(voice);
					}
				}
				sounds.put(cs.key.toLowerCase(), cs);
			} catch (CivException e) {
				CivLog.error("----ConfigSound---" + e.getMessage() + " при чтении sound = ");
			}
		}

		CivLog.info("Loaded " + sounds.size() + " sounds");
	}

}
