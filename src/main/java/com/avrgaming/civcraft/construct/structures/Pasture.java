package com.avrgaming.civcraft.construct.structures;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.LoadPastureEntityTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Pasture extends Structure {

	/* Global pasture chunks */
	public static Map<ChunkCoord, Pasture> pastureChunks = new ConcurrentHashMap<>();
	public static Map<UUID, Pasture> pastureEntities = new ConcurrentHashMap<>();
	
	/* Chunks bound to this pasture. */
	public HashSet<ChunkCoord> chunks = new HashSet<>();
	public HashSet<UUID> entities = new HashSet<>();
	public ReentrantLock lock = new ReentrantLock(); 
	
	private int pendingBreeds = 0;
	
	public Pasture(String id, Town town) {
		super(id, town);
	}

	public int getMobCount() {
		return entities.size();
	}

	public int getMobMax() {
		int max;
		try {
			max = CivSettings.getInteger(CivSettings.structureConfig, "pasture.max_mobs");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return 0;
		}
		return max;
	}

	public boolean processMobBreed(Player player, EntityType type) {
				
		if (!this.isActive()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("pasture_destroyed"));
			return false;
		}
		
		if (this.getMobCount() >= this.getMobMax()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("pasture_isFull"));
			return false;
		}
		
		if ((getPendingBreeds() + this.getMobCount()) >= this.getMobMax()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("pasture_TooMuchorIsFull",CivSettings.localize.localizedString("pasture_isFull")));
			return false;
		}
		
		return true;
	}
	
	public void bindPastureChunks() {
		for (BlockCoord bcoord : this.getConstructBlocks().keySet()) {
			ChunkCoord coord = new ChunkCoord(bcoord);
			this.chunks.add(coord);
			pastureChunks.put(coord, this);
		}
	}
	
	public void unbindPastureChunks() {
		for (ChunkCoord coord : this.chunks) {
			pastureChunks.remove(coord);
		}
		
		this.entities.clear();
		this.chunks.clear();
		
		LinkedList<UUID> removeUs = new LinkedList<>();
		for (UUID id : pastureEntities.keySet()) {
			Pasture pasture = pastureEntities.get(id);
			if (pasture == this) {
				removeUs.add(id);
			}
		}
		
		for (UUID id : removeUs) {
			pastureEntities.remove(id);
		}
		
	}
	
	@Override
	public void onComplete() {
		bindPastureChunks();
	}
	
	@Override
	public void onLoad() {
		bindPastureChunks();
		loadEntities();
	}
	
	@Override
	public void delete() {
		super.delete();
		unbindPastureChunks();
		clearEntities();
	}
	
	public void clearEntities() {
		// TODO Clear entities bound to us?
	}

	public void onBreed(LivingEntity entity) {
		saveEntity(entity.getWorld().getName(), entity.getUniqueId());
		setPendingBreeds(getPendingBreeds() - 1);
	}
	
	public String getEntityKey() {
		return "pasture:"+this.getId();
	}
	
	public String getValue(String worldName, UUID id) {
		return worldName+":"+id;
	}
	
	public void saveEntity(String worldName, UUID id) {
		class AsyncTask implements Runnable {
			final Pasture pasture;
			final UUID id;
			final String worldName;
			
			public AsyncTask(Pasture pasture, UUID id, String worldName) {
				this.pasture = pasture;
				this.id = id;
				this.worldName = worldName;
			}
			
			@Override
			public void run() {
				pasture.sessionAdd(getEntityKey(), getValue(worldName, id));
				lock.lock();
				try {
					entities.add(id);
					pastureEntities.put(id, pasture);
				} finally {
					lock.unlock();
				}
			}
		}
		
		TaskMaster.asyncTask(new AsyncTask(this, id, worldName), 0);
	}
	
	public void loadEntities() {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getEntityKey());
		Queue<SessionEntry> entriesToLoad = new LinkedList<>(entries);
		TaskMaster.syncTask(new LoadPastureEntityTask(entriesToLoad, this));
	}
	
	public void onEntityDeath(LivingEntity entity) {
		class AsyncTask implements Runnable {
			final LivingEntity entity;
			
			public AsyncTask(LivingEntity entity) {
				this.entity = entity;
			}
			
			
			@Override
			public void run() {
				lock.lock();
				try {
					entities.remove(entity.getUniqueId());
					pastureEntities.remove(entity.getUniqueId());
				} finally {
					lock.unlock();
				}
			}
			
		}
		
		TaskMaster.asyncTask(new AsyncTask(entity), 0);
	}

	public int getPendingBreeds() {
		return pendingBreeds;
	}

	public void setPendingBreeds(int pendingBreeds) {
		this.pendingBreeds = pendingBreeds;
	}
	
}