package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructChest;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.MultiInventory;

public class MobGrinder extends Structure {
	private static final double T1_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t1_chance"); //1%
	private static final double T2_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t2_chance"); //2%
	private static final double T3_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t3_chance"); //1%
	private static final double T4_CHANCE = CivSettings.getDoubleStructure("mobGrinder.t4_chance"); //0.25%
	private static final double PACK_CHANCE = CivSettings.getDoubleStructure("mobGrinder.pack_chance"); //0.10%
	private static final double BIGPACK_CHANCE = CivSettings.getDoubleStructure("mobGrinder.bigpack_chance");
	private static final double HUGEPACK_CHANCE = CivSettings.getDoubleStructure("mobGrinder.hugepack_chance");

	public int skippedCounter = 0;
	public ReentrantLock lock = new ReentrantLock();

	public enum Crystal {
		T1, T2, T3, T4, PACK, BIGPACK, HUGEPACK
	}

	public MobGrinder(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	public MobGrinder(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void onMinuteUpdate() {
		if (!CivGlobal.mobGrinderEnabled) return;
		TaskMaster.asyncTask("mobGrinder-" + this.getCorner().toString(), new MobGrinderAsyncTask(this), 0);
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return "minecart";
	}

	public double getMineralChance(Crystal crystal) {
		double chance = 0;
		switch (crystal) {
			case T1 :
				chance = T1_CHANCE;
				break;
			case T2 :
				chance = T2_CHANCE;
				break;
			case T3 :
				chance = T3_CHANCE;
				break;
			case T4 :
				chance = T4_CHANCE;
				break;
			case PACK :
				chance = PACK_CHANCE;
				break;
			case BIGPACK :
				chance = BIGPACK_CHANCE;
				break;
			case HUGEPACK :
				chance = HUGEPACK_CHANCE;
		}

		double increase = chance * this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
		chance += increase;

		try {
			if (this.getTown().getGovernment().id.equals("gov_tribalism")) {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "mobGrinder.tribalism_rate");
			} else {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "mobGrinder.penalty_rate");
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}

		return chance;
	}
	class MobGrinderAsyncTask extends CivAsyncTask {

		MobGrinder mobGrinder;

		public MobGrinderAsyncTask(Structure mobGrinder) {
			this.mobGrinder = (MobGrinder) mobGrinder;
		}

		public void processMobGrinderUpdate() {
			if (!mobGrinder.isActive()) {
				return;
			}

			// Grab each CivChest object we'll require.
			ArrayList<ConstructChest> sources = mobGrinder.getAllChestsById("1");
			ArrayList<ConstructChest> destinations = mobGrinder.getAllChestsById("2");

			if (sources.size() != 2 || destinations.size() != 2) {
				CivLog.error(
						"Bad chests for Mob Grinder in town:" + mobGrinder.getTown().getName() + " sources:" + sources.size() + " dests:" + destinations.size());
				return;
			}

			// Make sure the chunk is loaded before continuing. Also, add get chest and add it to inventory.
			MultiInventory source_inv = new MultiInventory();
			MultiInventory dest_inv = new MultiInventory();

			try {
				for (ConstructChest src : sources) {
					//this.syncLoadChunk(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getZ());				
					Inventory tmp;
					try {
						tmp = this.getChestInventory(src.getCoord().getWorldname(), src.getCoord().getX(), src.getCoord().getY(), src.getCoord().getZ(), false);
					} catch (CivTaskAbortException e) {
						//e.printStackTrace();
						CivLog.warning("Mob Grinder:" + e.getMessage());
						return;
					}
					if (tmp == null) {
						mobGrinder.skippedCounter++;
						return;
					}
					source_inv.addInventory(tmp);
				}

				boolean full = true;
				for (ConstructChest dst : destinations) {
					//this.syncLoadChunk(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getZ());
					Inventory tmp;
					try {
						tmp = this.getChestInventory(dst.getCoord().getWorldname(), dst.getCoord().getX(), dst.getCoord().getY(), dst.getCoord().getZ(), false);
					} catch (CivTaskAbortException e) {
						//e.printStackTrace();
						CivLog.warning("Mob Grinder:" + e.getMessage());
						return;
					}
					if (tmp == null) {
						mobGrinder.skippedCounter++;
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
					/* Mob Grinder destination chest is full, stop processing. */
					return;
				}

			} catch (InterruptedException e) {
				return;
			}

			ItemStack[] contents = source_inv.getContents();
			for (int i = 0; i < mobGrinder.skippedCounter + 1; i++) {

				for (ItemStack stack : contents) {
					if (stack == null) continue;
					String itemID = CustomMaterial.getMID(stack);
					if (!itemID.isEmpty()) continue;
					try {
						ItemStack newItem = CustomMaterial.spawn(CustomMaterial.getCustomMaterial(itemID));
						this.updateInventory(Action.REMOVE, source_inv, newItem);
					} catch (InterruptedException e) {
						return;
					}

					Random rand = new Random();
					int rand1 = rand.nextInt(10000);
					ArrayList<ItemStack> newItems = new ArrayList<ItemStack>();
					if (itemID.contains("_egg_4")) {
						if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.HUGEPACK)) * 10000))) {
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_1"), 2));
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_1"), 2));
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_2"), 2));
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2"), 2));
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_3"), 2));
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_3"), 2));
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_4"), 2));
							newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_4"), 2));

						} else
							if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.BIGPACK)) * 10000))) {
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_4")));
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_4")));
								if (itemID.contains("creeper")) {
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tungsten_chestplate")));
								} else
									if (itemID.contains("skeleton")) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tungsten_leggings")));
									} else
										if (itemID.contains("spider")) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tungsten_helmet")));
										} else
											if (itemID.equals("zombie")) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tungsten_boots")));
											} else
												if (itemID.contains("slime")) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tungsten_sword")));
												} else
													if (itemID.contains("enderman")) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_tungsten_sword")));
													} else
														if (itemID.contains("pig")) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_composite_leather_leggings")));
														} else
															if (itemID.contains("cow")) {
																newItems.add(
																		CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_composite_leather_chestplate")));
															} else
																if (itemID.contains("chicken")) {
																	newItems.add(
																			CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_composite_leather_helmet")));
																} else
																	if (itemID.contains("sheep")) {
																		newItems.add(CustomMaterial
																				.spawn(CustomMaterial.getCustomMaterial("mat_composite_leather_boots")));
																	}

							} else
								if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.PACK)) * 10000))) {
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_4"), 3));
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_4"), 3));

								} else
									if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T4)) * 10000))) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_4"), 2));
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_4"), 2));

									} else
										if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T3)) * 10000))) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_4"), 2));

										} else
											if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T2)) * 10000))) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_4"), 5));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_4"), 5));

											} else
												if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T1)) * 10000))) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_4"), 3));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_4"), 3));

												} else {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_4"), 2));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_4"), 2));
												}
					} else
						if (itemID.contains("_egg_3")) {
							if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.HUGEPACK)) * 10000))) {
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_1")));
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_1")));
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_2")));
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2")));
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_3"), 2));
								newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_3"), 2));
								if (itemID.contains("creeper")) {
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_creeper_egg_4"), 2));
								} else
									if (itemID.contains("skeleton")) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_skeleton_egg_4"), 2));
									} else
										if (itemID.contains("spider")) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_spider_egg_4"), 2));
										} else
											if (itemID.equals("zombie")) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_zombie_egg_4"), 2));
											} else
												if (itemID.contains("slime")) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_slime_egg_4"), 2));
												} else
													if (itemID.contains("enderman")) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_enderman_egg_4"), 2));
													} else
														if (itemID.contains("pig")) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_pig_egg_4"), 2));
														} else
															if (itemID.contains("cow")) {
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_cow_egg_4"), 2));
															} else
																if (itemID.contains("chicken")) {
																	newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_chicken_egg_4"), 2));
																} else
																	if (itemID.contains("sheep")) {
																		newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_sheep_egg_4"), 2));
																	}

							} else
								if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.BIGPACK)) * 10000))) {
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_3")));
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_3")));
									if (itemID.contains("creeper")) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_creeper_egg_4")));
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_carbide_steel_chestplate")));
									} else
										if (itemID.contains("skeleton")) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_skeleton_egg_4")));
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_carbide_steel_leggings")));
										} else
											if (itemID.contains("spider")) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_spider_egg_4")));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_carbide_steel_helmet")));
											} else
												if (itemID.equals("zombie")) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_zombie_egg_4")));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_carbide_steel_boots")));
												} else
													if (itemID.contains("slime")) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_slime_egg_4")));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_carbide_steel_sword")));
													} else
														if (itemID.contains("enderman")) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_enderman_egg_4")));
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_carbide_steel_sword")));
														} else
															if (itemID.contains("pig")) {
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_pig_egg_4")));
																newItems.add(
																		CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_hardened_leather_leggings")));
															} else
																if (itemID.contains("cow")) {
																	newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_cow_egg_4")));
																	newItems.add(CustomMaterial
																			.spawn(CustomMaterial.getCustomMaterial("mat_hardened_leather_chestplate")));
																} else
																	if (itemID.contains("chicken")) {
																		newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_chicken_egg_4")));
																		newItems.add(CustomMaterial
																				.spawn(CustomMaterial.getCustomMaterial("mat_hardened_leather_helmet")));
																	} else
																		if (itemID.contains("sheep")) {
																			newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_sheep_egg_4")));
																			newItems.add(CustomMaterial
																					.spawn(CustomMaterial.getCustomMaterial("mat_hardened_leather_boots")));
																		}

								} else
									if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.PACK)) * 10000))) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_3")));
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_3")));
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_4")));
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_4")));

									} else
										if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T4)) * 10000))) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_3")));
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_3")));

										} else
											if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T3)) * 10000))) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_3")));

											} else
												if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T2)) * 10000))) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_4")));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_4")));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_3"), 2));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_3"), 2));

												} else
													if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T1)) * 10000))) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_3"), 3));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_3"), 3));

													} else {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_3")));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_3")));
													}

						} else
							if (itemID.contains("_egg_2")) {

								if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.HUGEPACK)) * 10000))) {
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_2"), 2));
									newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2"), 2));
									if (itemID.equals("creeper")) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_creeper_egg_3"), 2));
									} else
										if (itemID.equals("skeleton")) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_skeleton_egg_3"), 2));
										} else
											if (itemID.equals("spider")) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_spider_egg_3"), 2));
											} else
												if (itemID.equals("zombie")) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_zombie_egg_3"), 2));
												} else
													if (itemID.contains("slime")) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_slime_egg_3"), 2));
													} else
														if (itemID.contains("enderman")) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_enderman_egg_3"), 2));
														} else
															if (itemID.contains("pig")) {
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_pig_egg_3"), 2));
															} else
																if (itemID.contains("cow")) {
																	newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_cow_egg_3"), 2));
																} else
																	if (itemID.contains("chicken")) {
																		newItems.add(
																				CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_chicken_egg_3"), 2));
																	} else
																		if (itemID.contains("sheep")) {
																			newItems.add(
																					CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_sheep_egg_3"), 2));
																		}

								} else
									if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.BIGPACK)) * 10000))) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_2")));
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2")));
										if (itemID.contains("creeper")) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_creeper_egg_3")));
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_steel_chestplate")));
										} else
											if (itemID.contains("skeleton")) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_skeleton_egg_3")));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_steel_leggings")));
											} else
												if (itemID.contains("spider")) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_spider_egg_3")));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_steel_helmet")));
												} else
													if (itemID.contains("zombie")) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_zombie_egg_3")));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_steel_boots")));
													} else
														if (itemID.contains("slime")) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_slime_egg_3")));
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_steel_sword")));
														} else
															if (itemID.contains("enderman")) {
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_enderman_egg_3")));
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_steel_sword")));
															} else
																if (itemID.contains("pig")) {
																	newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_pig_egg_3")));
																	newItems.add(
																			CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_refined_leather_leggings")));
																} else
																	if (itemID.contains("cow")) {
																		newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_cow_egg_3")));
																		newItems.add(CustomMaterial
																				.spawn(CustomMaterial.getCustomMaterial("mat_refined_leather_chestplate")));
																	} else
																		if (itemID.contains("chicken")) {
																			newItems.add(
																					CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_chicken_egg_3")));
																			newItems.add(CustomMaterial
																					.spawn(CustomMaterial.getCustomMaterial("mat_refined_leather_helmet")));
																		} else
																			if (itemID.contains("sheep")) {
																				newItems.add(
																						CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_sheep_egg_3")));
																				newItems.add(CustomMaterial
																						.spawn(CustomMaterial.getCustomMaterial("mat_refined_leather_boots")));
																			}

									} else
										if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.PACK)) * 10000))) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_3")));
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_3")));
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_2")));
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2")));

										} else
											if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T4)) * 10000))) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2")));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_2")));

											} else
												if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T3)) * 10000))) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2")));

												} else
													if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T2)) * 10000))) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_3")));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_3")));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_2"), 2));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_2"), 2));

													} else
														if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T1)) * 10000))) {
															newItems.add(
																	CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_2"), 3));
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_2"), 3));

														} else {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_2")));
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_2")));
														}

							} else
								if (itemID.contains("_egg")) {
									if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.HUGEPACK)) * 10000))) {
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_1"), 2));
										newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_1"), 2));

										if (itemID.contains("creeper")) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_creeper_egg_2"), 2));
										} else
											if (itemID.contains("skeleton")) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_skeleton_egg_2"), 2));
											} else
												if (itemID.contains("spider")) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_spider_egg_2"), 2));
												} else
													if (itemID.contains("zombie")) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_zombie_egg_2"), 2));
													} else
														if (itemID.contains("slime")) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_slime_egg_2"), 2));
														} else
															if (itemID.contains("enderman")) {
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_enderman_egg_2"), 2));
															} else
																if (itemID.contains("pig")) {
																	newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_pig_egg_2"), 2));
																} else
																	if (itemID.contains("cow")) {
																		newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_cow_egg_2"), 2));
																	} else
																		if (itemID.contains("chicken")) {
																			newItems.add(
																					CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_chicken_egg_2"), 2));
																		} else
																			if (itemID.contains("sheep")) {
																				newItems.add(CustomMaterial
																						.spawn(CustomMaterial.getCustomMaterial("mat_sheep_egg_2"), 2));
																			}
									} else
										if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.BIGPACK)) * 10000))) {
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_1")));
											newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_1")));
											if (itemID.contains("creeper")) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_creeper_egg_2")));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_iron_chestplate")));
											} else
												if (itemID.contains("skeleton")) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_skeleton_egg_2")));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_iron_leggings")));
												} else
													if (itemID.contains("spider")) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_spider_egg_2")));
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_iron_helmet")));
													} else
														if (itemID.contains("zombie")) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_zombie_egg_2")));
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_iron_boots")));
														} else
															if (itemID.contains("slime")) {
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_slime_egg_2")));
																newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_iron_sword")));
															} else
																if (itemID.contains("enderman")) {
																	newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_enderman_egg_2")));
																	newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_iron_sword")));
																} else
																	if (itemID.contains("pig")) {
																		newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_pig_egg_2")));
																		newItems.add(
																				CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_leather_leggings")));
																	} else
																		if (itemID.contains("cow")) {
																			newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_cow_egg_2")));
																			newItems.add(CustomMaterial
																					.spawn(CustomMaterial.getCustomMaterial("mat_leather_chestplate")));
																		} else
																			if (itemID.contains("chicken")) {
																				newItems.add(CustomMaterial
																						.spawn(CustomMaterial.getCustomMaterial("mat_chicken_egg_2")));
																				newItems.add(CustomMaterial
																						.spawn(CustomMaterial.getCustomMaterial("mat_leather_helmet")));
																			} else
																				if (itemID.contains("sheep")) {
																					newItems.add(CustomMaterial
																							.spawn(CustomMaterial.getCustomMaterial("mat_sheep_egg_2")));
																					newItems.add(CustomMaterial
																							.spawn(CustomMaterial.getCustomMaterial("mat_leather_boots")));
																				}
										} else
											if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.PACK)) * 10000))) {
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_1")));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_1")));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_2")));
												newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_2")));
											} else
												if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T4)) * 10000))) {
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_1")));
													newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_1")));
												} else
													if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T3)) * 10000))) {
														newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_1")));
													} else
														if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T2)) * 10000))) {
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_2")));
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_2")));
															newItems.add(
																	CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_1"), 2));
															newItems.add(CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_1"), 2));
														} else
															if (rand1 < ((int) ((mobGrinder.getMineralChance(Crystal.T1)) * 10000))) {
																newItems.add(CustomMaterial
																		.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_1"), 3));
																newItems.add(
																		CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_1"), 3));
															} else {
																newItems.add(
																		CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_metallic_crystal_fragment_1")));
																newItems.add(
																		CustomMaterial.spawn(CustomMaterial.getCustomMaterial("mat_ionic_crystal_fragment_1")));
															}
								} else {
									continue;
								}
					if (newItems.size() >= 1) {
						//Try to add the new item to the dest chest, if we cant, oh well.
						try {
							for (ItemStack item : newItems) {
								this.updateInventory(Action.ADD, dest_inv, item);
							}
						} catch (InterruptedException e) {
							return;
						}
					} else {
					}
					break;
				}
			}
			mobGrinder.skippedCounter = 0;
		}

		@Override
		public void run() {
			if (this.mobGrinder.lock.tryLock()) {
				try {
					try {
						processMobGrinderUpdate();
						if (CivData.randChance(this.mobGrinder.getTown().getReturnChance())) {
							processMobGrinderUpdate();
						} // TODO зачем второй запуск здесь???
					} catch (Exception e) {
						e.printStackTrace();
					}
				} finally {
					this.mobGrinder.lock.unlock();
				}
			} 
		}

	}
}
