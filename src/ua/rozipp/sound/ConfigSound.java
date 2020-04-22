package ua.rozipp.sound;

import java.util.LinkedList;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;

public class ConfigSound {

	public String key;
	public int radius; // количество блоков от источника, на котором будет слышен звук 

	public LinkedList<Voice> voices = new LinkedList<>();

	public void playVoices(Player player, Location loc) {
		
		class AsyncTask extends CivAsyncTask {
			Player player;
			Location loc;
			Voice voi;
			
			AsyncTask(Player player, Location loc, Voice voi) {
				this.player = player;
				this.loc = loc;
				this.voi = voi;
			}
			@Override
			public void run() {
				player.playSound(loc, Sound.valueOf(voi.type), SoundManager.MAX_RADIUS_CHUNK, voi.pitch);
			}
		}
		
		for (Voice voi : voices) {
			TaskMaster.asyncTask(new AsyncTask(player, loc, voi), voi.delay);
		}
	}
}
