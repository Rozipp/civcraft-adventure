package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.construct.ConstructSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;
import com.avrgaming.civcraft.util.SimpleBlock;

public class FishHatchery extends Structure {
	public static final int MAX_CHANCE = CivSettings.getIntegerStructure("fishery.tierMax");
	public static final double FISH_T0_RATE = CivSettings.getDoubleStructure("fishery.t0_rate"); //100%
	public static final double FISH_T1_RATE = CivSettings.getDoubleStructure("fishery.t1_rate"); //100%
	public static final double FISH_T2_RATE = CivSettings.getDoubleStructure("fishery.t2_rate"); //100%
	public static final double FISH_T3_RATE = CivSettings.getDoubleStructure("fishery.t3_rate"); //100%
	public static final double FISH_T4_RATE = CivSettings.getDoubleStructure("fishery.t4_rate"); //100%

	private int level = 1;
	private Biome biome = null;
	public int skippedCounter = 0;
	public ReentrantLock lock = new ReentrantLock();

	public FishHatchery(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		setLevel(town.saved_fish_hatchery_level);
	}

	public FishHatchery(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}
	@Override
	public void onMinuteUpdate() {
		if (!CivGlobal.fisheryEnabled) return;
		Random rand = new Random();
		if (rand.nextInt(5) <= 1) {
			TaskMaster.asyncTask("fishHatchery-" + this.getCorner().toString(), new FisheryAsyncTask(this), 0);
		}
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>" + this.getDisplayName() + "</u></b><br/>";
		out += CivSettings.localize.localizedString("Level") + " " + this.level;
		return out;
	}

	@Override
	public String getMarkerIconName() {
		return "cutlery";
	}

	public double getChance(double chance) {
		return this.modifyTransmuterChance(chance);
	}

	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
		this.level = getTown().saved_fish_hatchery_level;
		this.setBiome(this.getCorner().getBlock().getBiome());
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	private ConstructSign getSignFromSpecialId(int special_id) {
		for (ConstructSign sign : getSigns()) {
			int id = Integer.valueOf(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}

	@Override
	public void updateSignText() {
		int count = 0;

		for (count = 0; count < level; count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:" + count);
				return;
			}
			sign.setText(CivSettings.localize.localizedString("fishery_sign_pool") + "\n" + (count + 1));
			sign.update();
		}

		for (; count < getSigns().size(); count++) {
			ConstructSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:" + count);
				return;
			}
			sign.setText(CivSettings.localize.localizedString("fishery_sign_poolOffline"));
			sign.update();
		}

	}

	@Override
	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) {
		int special_id = Integer.valueOf(sign.getAction());
		if (special_id < this.level) {
			CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_fishery_pool_msg_online", (special_id + 1)));

		} else {
			CivMessage.send(player, CivColor.Rose + CivSettings.localize.localizedString("var_fishery_pool_msg_offline", (special_id + 1)));
		}
	}

	public Biome getBiome() {
		if (biome == null) {
			try {
				Chunk chunk = this.getCenterLocation().getChunk();
				ChunkCoord coord = new ChunkCoord(chunk);
				CultureChunk cc = new CultureChunk(this.getTown(), coord);
				biome = cc.getBiome();
				this.setBiome(cc.getBiome());
			} catch (IllegalStateException e) {

			} finally {
				biome = Biome.BIRCH_FOREST_HILLS;
			}
		}
		return biome;
	}

	public void setBiome(Biome biome) {
		this.biome = biome;
	}
	
	
	class FisheryAsyncTask extends CivAsyncTask {

		FishHatchery fishHatchery;
		
		
		public FisheryAsyncTask(Structure fishHatchery) {
			this.fishHatchery = (FishHatchery)fishHatchery;
		}
		
		public void processFisheryUpdate() {
			if (!fishHatchery.isActive()) {
				return;
			}
			
			// Grab each CivChest object we'll require.
			ArrayList<ConstructChest> sources = fishHatchery.getAllChestsById("0");
			sources.addAll(fishHatchery.getAllChestsById("1"));
			sources.addAll(fishHatchery.getAllChestsById("2"));
			sources.addAll(fishHatchery.getAllChestsById("3"));
			ArrayList<ConstructChest> destinations = fishHatchery.getAllChestsById("4");
			
			if (sources.size() != 4 || destinations.size() != 2) {
				CivLog.error("Bad chests for fish hatchery in town:"+fishHatchery.getTown().getName()+" sources:"+sources.size()+" dests:"+destinations.size());
				return;
			}
			
			// Make sure the chunk is loaded before continuing. Also, add get chest and add it to inventory.
			MultiInventory source_inv0 = new MultiInventory();
			MultiInventory source_inv1 = new MultiInventory();
			MultiInventory source_inv2 = new MultiInventory();
			MultiInventory source_inv3 = new MultiInventory();
			MultiInventory dest_inv = new MultiInventory();

			try {
				for (ConstructChest src : sources) {
					//this.syncLoadChunk(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getZ());				
					Inventory tmp;
					try {
						tmp = this.getChestInventory(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getY(), src.getCoord().getZ(), false);
					} catch (CivTaskAbortException e) {
						//e.printStackTrace();
						CivLog.warning("Fish Hatchery:"+e.getMessage());
						return;
					}
					if (tmp == null) {
						fishHatchery.skippedCounter++;
						return;
					}
					switch(src.getChestId()){
					case "0": source_inv0.addInventory(tmp);
				 	break;
					case "1": source_inv1.addInventory(tmp);
				 	break;
					case "2": source_inv2.addInventory(tmp);
				 	break;
					case "3": source_inv3.addInventory(tmp);
				 	break;
					}
				}
				
				boolean full = true;
				for (ConstructChest dst : destinations) {
					//this.syncLoadChunk(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getZ());
					Inventory tmp;
					try {
						tmp = this.getChestInventory(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getY(), dst.getCoord().getZ(), false);
					} catch (CivTaskAbortException e) {
						//e.printStackTrace();
						CivLog.warning("Fish Hatchery:"+e.getMessage());
						return;
					}
					if (tmp == null) {
						fishHatchery.skippedCounter++;
						return;
					}
					dest_inv.addInventory(tmp);
					
					for (ItemStack stack : tmp.getContents()) {
						if (stack == null) {
							full = false;
							break;
						}
					}
				}
				
				if (full) {
					/* Quarry destination chest is full, stop processing. */
					return;
				}
				
			} catch (InterruptedException e) {
				return;
			}

			ItemStack[] contents0 = source_inv0.getContents();
			ItemStack[] contents1 = source_inv1.getContents();
			ItemStack[] contents2 = source_inv2.getContents();
			ItemStack[] contents3 = source_inv3.getContents();
			for (int i = 0; i < fishHatchery.skippedCounter+1; i++) {
			
				for(ItemStack stack : contents0) {
					if (stack == null) {
						continue;
					}
					
					if (ItemManager.getTypeId(stack) == CivData.FISHING_ROD) {
						try {
							short damage = ItemManager.getData(stack);
							this.updateInventory(Action.REMOVE, source_inv0, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
							damage++;
							if (damage < 64) {
							this.updateInventory(Action.ADD, source_inv0, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
							}
						} catch (InterruptedException e) {
							return;
						}
						
						ItemStack newItem;
						
						newItem = this.getFishForBiome();
						
						//Try to add the new item to the dest chest, if we cant, oh well.
						try {
							this.updateInventory(Action.ADD, dest_inv, newItem);
						} catch (InterruptedException e) {
							return;
						}
						break;
					}
				}
				
				if (this.fishHatchery.getLevel() >= 2)
				{
					for(ItemStack stack : contents1) {
						if (stack == null) {
							continue;
						}
						
						if (ItemManager.getTypeId(stack) == CivData.FISHING_ROD) {
							try {
								short damage = ItemManager.getData(stack);
								this.updateInventory(Action.REMOVE, source_inv1, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
								damage++;
								if (damage < 64) {
								this.updateInventory(Action.ADD, source_inv1, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
								}
							} catch (InterruptedException e) {
								return;
							}
							
							ItemStack newItem;
							
							newItem = this.getFishForBiome();
							
							//Try to add the new item to the dest chest, if we cant, oh well.
							try {
								this.updateInventory(Action.ADD, dest_inv, newItem);
							} catch (InterruptedException e) {
								return;
							}
							break;
						}
					}
				}
				
				if (this.fishHatchery.getLevel() >= 3)
				{
					for(ItemStack stack : contents2) {
						if (stack == null) {
							continue;
						}
						
						if (ItemManager.getTypeId(stack) == CivData.FISHING_ROD) {
							try {
								short damage = ItemManager.getData(stack);
								this.updateInventory(Action.REMOVE, source_inv2, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
								damage++;
								if (damage < 64) {
								this.updateInventory(Action.ADD, source_inv2, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
								}
							} catch (InterruptedException e) {
								return;
							}
							
							ItemStack newItem;
							
							newItem = this.getFishForBiome();
							
							//Try to add the new item to the dest chest, if we cant, oh well.
							try {
								this.updateInventory(Action.ADD, dest_inv, newItem);
							} catch (InterruptedException e) {
								return;
							}
							break;
						}
					}
				}
				if (this.fishHatchery.getLevel() >= 4)
				{
					for(ItemStack stack : contents3) {
						if (stack == null) {
							continue;
						}
						
						if (ItemManager.getTypeId(stack) == CivData.FISHING_ROD) {
							try {
								short damage = ItemManager.getData(stack);
								this.updateInventory(Action.REMOVE, source_inv3, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
								damage++;
								if (damage < 64) {
								this.updateInventory(Action.ADD, source_inv3, ItemManager.createItemStack(CivData.FISHING_ROD, damage, 1));
								}
							} catch (InterruptedException e) {
								return;
							}
							
							ItemStack newItem = this.getFishForBiome();
							
							//Try to add the new item to the dest chest, if we cant, oh well.
							try {
								this.updateInventory(Action.ADD, dest_inv, newItem);
							} catch (InterruptedException e) {
								return;
							}
							break;
						}
					}
				}
			}	
			fishHatchery.skippedCounter = 0;
		}
		
		private int getBiome() {
			Biome biome = this.fishHatchery.getBiome();
			
			if (biome.equals(Biome.BIRCH_FOREST_HILLS) ||
					biome.equals(Biome.MUTATED_BIRCH_FOREST) ||
					biome.equals(Biome.MUTATED_BIRCH_FOREST_HILLS) ||
					biome.equals(Biome.MUTATED_TAIGA_COLD) ||
					biome.equals(Biome.MUTATED_EXTREME_HILLS) ||
					biome.equals(Biome.MUTATED_EXTREME_HILLS_WITH_TREES ) ||
					biome.equals(Biome.ICE_MOUNTAINS) ||
					biome.equals(Biome.MUTATED_JUNGLE_EDGE) ||
					biome.equals(Biome.JUNGLE_HILLS) ||
					biome.equals(Biome.MUTATED_JUNGLE) ||
					biome.equals(Biome.MUTATED_MESA) ||
					biome.equals(Biome.MUTATED_MESA_CLEAR_ROCK) ||
					biome.equals(Biome.MUTATED_MESA_CLEAR_ROCK) ||
					biome.equals(Biome.MUTATED_MESA_ROCK) ||
					biome.equals(Biome.MUTATED_SAVANNA) ||
					biome.equals(Biome.MUTATED_SAVANNA_ROCK) ||
					biome.equals(Biome.SMALLER_EXTREME_HILLS) ||
					biome.equals(Biome.MUTATED_SWAMPLAND) ||
					biome.equals(Biome.MUTATED_TAIGA))
			{
				return 1;
			}
			else if (biome.equals(Biome.BIRCH_FOREST) ||
					biome.equals(Biome.EXTREME_HILLS) ||
					biome.equals(Biome.FOREST) ||
					biome.equals(Biome.FOREST_HILLS) ||
					biome.equals(Biome.ICE_FLATS) ||
					biome.equals(Biome.ICE_MOUNTAINS) ||
					biome.equals(Biome.MUTATED_ICE_FLATS) ||
					biome.equals(Biome.JUNGLE) ||
					biome.equals(Biome.JUNGLE_EDGE) ||
					biome.equals(Biome.MUTATED_REDWOOD_TAIGA) ||
					biome.equals(Biome.MUTATED_REDWOOD_TAIGA_HILLS) ||
					biome.equals(Biome.REDWOOD_TAIGA) ||
					biome.equals(Biome.REDWOOD_TAIGA_HILLS) ||
					biome.equals(Biome.ROOFED_FOREST) ||
					biome.equals(Biome.MESA) ||
					biome.equals(Biome.MESA_CLEAR_ROCK) ||
					biome.equals(Biome.MESA_ROCK ) ||
					biome.equals(Biome.EXTREME_HILLS_WITH_TREES) ||
					biome.equals(Biome.ROOFED_FOREST) ||
					biome.equals(Biome.SAVANNA) ||
					biome.equals(Biome.SAVANNA_ROCK) ||
					biome.equals(Biome.TAIGA) ||
					biome.equals(Biome.TAIGA_HILLS))
			{
				return 2;
			}
			else if (biome.equals(Biome.BEACHES) ||
					biome.equals(Biome.COLD_BEACH) ||
					biome.equals(Biome.TAIGA_COLD) ||
					biome.equals(Biome.DEEP_OCEAN) ||
					biome.equals(Biome.DESERT) ||
					biome.equals(Biome.DESERT_HILLS) ||
					biome.equals(Biome.MUTATED_DESERT) ||
					biome.equals(Biome.FROZEN_OCEAN) ||
					biome.equals(Biome.FROZEN_RIVER) ||
					biome.equals(Biome.MUSHROOM_ISLAND) ||
					biome.equals(Biome.MUSHROOM_ISLAND_SHORE) ||
					biome.equals(Biome.OCEAN) ||
					biome.equals(Biome.PLAINS) ||
					biome.equals(Biome.RIVER) ||
					biome.equals(Biome.STONE_BEACH) ||
					biome.equals(Biome.SWAMPLAND) )
			{
				return 3;
			}
			else
			{
				return 0;
			}
		}
		
		private ItemStack getFishForBiome() {
			

			Random rand = new Random();
			int maxRand = FishHatchery.MAX_CHANCE;
			int baseRand = rand.nextInt(maxRand);
			ItemStack newItem = null;
			int fisheryLevel = this.fishHatchery.getLevel();
			int fishTier;
			if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T4_RATE))*maxRand)) && fisheryLevel >= 4) {
				fishTier = 4;
			} else if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T3_RATE))*maxRand)) && fisheryLevel >= 3) {
				fishTier = 3;
			} else if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T2_RATE))*maxRand)) && fisheryLevel >= 2) {
				fishTier = 2;
			} else if (baseRand < ((int)((fishHatchery.getChance(FishHatchery.FISH_T1_RATE))*maxRand))) {
				fishTier = 1;
			} else {
				fishTier = 0;
			}
			int biome = getBiome();

			int randMax = 100;
			int biomeRand = rand.nextInt(randMax);
			
			switch (fishTier) {
				case 0: //Fish Tier 0
					if (biomeRand >= 95) { 
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_pufferfish"));
					} else if (biomeRand > 85) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_clownfish"));
					} else if (biomeRand > 75) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_salmon"));
					} else if (biomeRand > 50) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_fish"));
					} else {
						int junkRand = rand.nextInt(randMax);
						if (junkRand > 90)
						{
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_minnows"));
						}else if (junkRand > 70) {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tadpole"));
						} else if (junkRand > 50) {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_seaweed"));
						} else if (junkRand > 30) {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tangled_string"));
						} else {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_pond_scum"));
						}
					}
					break;
				case 1: //Fish Tier 1
					switch (biome) {
					case 0: //Not ranked
						newItem = ItemManager.createItemStack(CivData.FISH_RAW, 1);
						break;

					case 1: //Mountains
						if (biomeRand < 90) {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_brown_trout"));
						} else {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_gag_grouper"));
						}
						break;

					case 2: //Flatter Lands
						if (biomeRand < 90) {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_atlantic_striped_bass"));
						} else {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_arrowtooth_flounder"));
						}
						break;

					case 3: // Oceans, Mushroom, Swamps, Ice
						if (biomeRand < 90) {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_atlantic_cod"));
						} else {
							newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_atlantic_surfclam"));
						}
						break;
				}
					break;

				case 2: //Fish Tier 2
					switch (biome) {
					case 0: //Not ranked
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_fish"));
						break;

					case 1: //Mountains
						if (biomeRand < 90) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_brook_trout"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_red_grouper"));
						}
						break;

					case 2: //Flatter Lands
						if (biomeRand < 90) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_pacific_ocean_perch"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_summer_flounder"));
						}
						break;

					case 3: // Oceans, Mushroom, Swamps, Ice
						if (biomeRand < 90) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_pacific_cod"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_ocean_quahog"));
						}
						break;
				}
					break;

				case 3: //Fish Tier 3
					switch (biome) {
					case 0: //Not ranked
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_fish"));
						break;

					case 1: //Mountains
						if (biomeRand < 80) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_cutthroat_trout"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_black_sea_bass"));
						}
						break;

					case 2: //Flatter Lands
						if (biomeRand < 80) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_acadian_redfish"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_winter_flounder"));
						}
						break;

					case 3: // Oceans, Mushroom, Swamps, Ice
						if (biomeRand < 80) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_lingcod"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_northern_quahog"));
						}
						break;
				}
					break;

				case 4: //Fish Tier 4
					switch (biome) {
					case 0: //Not ranked
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_fish"));
						break;

					case 1: //Mountains
						if (biomeRand < 80) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_rainbow_trout"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_wreckfish"));
						}
						break;

					case 2: //Flatter Lands
						if (biomeRand < 80) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_widow_rockfish"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_yellowtail_flounder"));
						}
						break;

					case 3: // Oceans, Mushroom, Swamps, Ice
						if (biomeRand < 80) {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_sablefish"));
						} else {
						newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_geoduck"));
						}
						break;
				}
					break;
					
			}
			if (newItem == null)
			{
				CivLog.debug("Fish Hatchery: newItem was null");
				newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_fish_fish"));
			}

			return newItem;
			
		}
		
		@Override
		public void run() {
			if (this.fishHatchery.lock.tryLock()) {
				try {
					try {
						processFisheryUpdate();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} finally {
					this.fishHatchery.lock.unlock();
				}
			} 
		}

	}
}