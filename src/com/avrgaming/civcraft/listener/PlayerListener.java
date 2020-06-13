/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.listener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTechPotion;
import com.avrgaming.civcraft.construct.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.CustomMaterial;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.mythicmob.MobStatic;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.structure.Townhall;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerChunkNotifyAsyncTask;
import com.avrgaming.civcraft.threading.tasks.PlayerLoginAsyncTask;
import com.avrgaming.civcraft.threading.timers.PlayerLocationCacheUpdate;
import com.avrgaming.civcraft.units.UnitCustomMaterial;
import com.avrgaming.civcraft.units.UnitMaterial;
import com.avrgaming.civcraft.units.UnitStatic;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.TagManager;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarStats;

public class PlayerListener implements Listener {
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerPickup(EntityPickupItemEvent event) {
		if (event.isCancelled()) return;
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			String name;
			boolean rare = false;
			ItemStack item = event.getItem().getItemStack();
			if (item.getItemMeta().hasDisplayName()) {
				name = item.getItemMeta().getDisplayName();
				rare = true;
			} else {
				name = item.getType().name().replace("_", " ").toLowerCase();
			}

			Resident resident = CivGlobal.getResident(player);
			if (resident.getItemMode().equals("all")) {
				CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_customItem_Pickup", CivColor.LightPurple + event.getItem().getItemStack().getAmount(), name));
			} else
				if (resident.getItemMode().equals("rare") && rare) {
					CivMessage.send(player, CivColor.LightGreen + CivSettings.localize.localizedString("var_customItem_Pickup", CivColor.LightPurple + event.getItem().getItemStack().getAmount(), name));
				}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		CivLog.info("Scheduling on player login task for player:" + player.getName());
		TaskMaster.asyncTask("onPlayerLogin-" + player.getName(), new PlayerLoginAsyncTask(player.getUniqueId()), 0L);
		Bukkit.getScheduler().runTaskLater((Plugin) CivCraft.getPlugin(), () -> TagManager.editNameTag(player), 4L);
		CivGlobal.playerFirstLoginMap.put(player.getName(), new Date());
		PlayerLocationCacheUpdate.playerQueue.add(player.getName());

		if (player.isOp()) {
			// Bukkit.dispatchCommand(event.getPlayer(), "vanish");
			// Bukkit.dispatchCommand(event.getPlayer(), "dynmap hide");
		}
		event.getPlayer().setFlying(false);
		event.getPlayer().setAllowFlight(false);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void OnPlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Resident resident = CivGlobal.getResident(player);
		if (resident != null) resident.setUnitObjectId(0);
		UnitStatic.removeChildrenItems(player);
		UnitStatic.updateUnitForPlaeyr(player);
		UnitStatic.setModifiedMovementSpeed(player);
		UnitStatic.setModifiedJumping(player);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
		// Handle Teleportation Things here!
		if (event.getCause().equals(TeleportCause.COMMAND) || event.getCause().equals(TeleportCause.PLUGIN)) {
			CivLog.info("[TELEPORT]" + " " + event.getPlayer().getName() + " " + "to:" + event.getTo().getBlockX() + "," + event.getTo().getBlockY() + "," + event.getTo().getBlockZ() + " " + "from:" + event.getFrom().getBlockX() + ","
					+ event.getFrom().getBlockY() + "," + event.getFrom().getBlockZ());
			Player player = event.getPlayer();
			if (!player.isOp() && !(player.hasPermission("civ.admin") || player.hasPermission(CivSettings.TPALL))) {
				CultureChunk cc = CivGlobal.getCultureChunk(new ChunkCoord(event.getTo()));
				Resident resident = CivGlobal.getResident(player);
				if (cc != null && cc.getCiv() != resident.getCiv() && !cc.getCiv().isAdminCiv()) {
					Relation.Status status = cc.getCiv().getDiplomacyManager().getRelationStatus(player);
					if (!(status.equals(Relation.Status.ALLY) && player.hasPermission(CivSettings.TPALLY)) && !(status.equals(Relation.Status.NEUTRAL) && player.hasPermission(CivSettings.TPNEUTRAL))
							&& !(status.equals(Relation.Status.HOSTILE) && player.hasPermission(CivSettings.TPHOSTILE)) && !(status.equals(Relation.Status.PEACE) && player.hasPermission(CivSettings.TPWAR))
							&& !(status.equals(Relation.Status.WAR) && player.hasPermission(CivSettings.TPWAR)) && !player.hasPermission(CivSettings.TPALL)) {
						/* Deny telportation into Civ if not allied. */
						event.setTo(event.getFrom());
						if (!event.isCancelled()) {
							event.setCancelled(true);
							CivMessage.send(resident, CivColor.Red + CivSettings.localize.localizedString("teleportDeniedPrefix") + " " + CivColor.White
									+ CivSettings.localize.localizedString("var_teleportDeniedCiv", CivColor.Green + cc.getCiv().getName() + CivColor.White));
							return;
						}
					}
				}

				Camp tocamp = CivGlobal.getCampAt(new ChunkCoord(event.getTo()));
				if (tocamp != null && !tocamp.equals(resident.getCamp()) && !player.hasPermission(CivSettings.TPCAMP)) {
					/* Deny telportation into Civ if not allied. */
					event.setTo(event.getFrom());
					if (!event.isCancelled()) {
						CivLog.debug("Cancelled Event " + event.getEventName() + " with cause: " + event.getCause());
						event.setCancelled(true);
						CivMessage.send(resident, CivColor.Red + CivSettings.localize.localizedString("teleportDeniedPrefix") + " " + CivColor.White
								+ CivSettings.localize.localizedString("var_teleportDeniedCamp", CivColor.Green + tocamp.getName() + CivColor.White));
						return;
					}

				}

				// if (War.isWarTime()) {
				//
				// if (toCamp != null && toCamp == resident.getCamp()) {
				// return;
				// }
				// if (cc != null && (cc.getCiv() == resident.getCiv() || cc.getCiv().isAdminCiv())) {
				// return;
				// }
				//
				// event.setTo(event.getFrom());
				// if (!event.isCancelled())
				// {
				// event.setCancelled(true);
				// CivMessage.send(resident, CivColor.Red+"[Denied] "+CivColor.White+"You're not allowed to Teleport during War unless you are teleporting
				// to your own Civ or Camp");
				// }
				// }
			}

			if (!event.isCancelled()) {
				TaskMaster.asyncTask(PlayerChunkNotifyAsyncTask.class.getSimpleName(), new PlayerChunkNotifyAsyncTask(event.getFrom(), event.getTo(), event.getPlayer().getName()), 0);

			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerMove(PlayerMoveEvent event) {
		/* Abort if we havn't really moved */
		if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
			return;
		}
		if (!CivGlobal.speedChunks) {
			/* Get the Modified Speed for the player. */
			UnitStatic.setModifiedMovementSpeed(event.getPlayer());
		}

		ChunkCoord fromChunk = new ChunkCoord(event.getFrom());
		ChunkCoord toChunk = new ChunkCoord(event.getTo());

		// Haven't moved chunks.
		if (fromChunk.equals(toChunk)) {
			return;
		}
		if (CivGlobal.speedChunks) {
			/* Get the Modified Speed for the player. */
			UnitStatic.setModifiedMovementSpeed(event.getPlayer());
		}

		TaskMaster.asyncTask(PlayerChunkNotifyAsyncTask.class.getSimpleName(), new PlayerChunkNotifyAsyncTask(event.getFrom(), event.getTo(), event.getPlayer().getName()), 0);

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown()) {
			return;
		}

		if (War.isWarTime()) {
			if (resident.getTown().getCiv().getDiplomacyManager().isAtWar()) {
				Capitol capitol = resident.getCiv().getCapitolStructure();
				if (capitol != null) {
					BlockCoord respawn = capitol.getRandomRespawnPoint();
					if (respawn != null) {
						// PlayerReviveTask reviveTask = new PlayerReviveTask(player, townhall.getRespawnTime(), townhall, event.getRespawnLocation());
						resident.setLastKilledTime(new Date());
						event.setRespawnLocation(respawn.getCenteredLocation());
						CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("playerListen_repawnInWarRoom"));

						// TaskMaster.asyncTask("", reviveTask, 0);
					}
				}
			}
		} else {
			if (resident.hasCamp()) {
				Camp camp = resident.getCamp();
				BlockCoord respawn = camp.getCorner();
				if (respawn != null) {
					event.setRespawnLocation(respawn.getCenteredLocation());
					CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("playerListen_repawnAtName", camp.getName()));
				}
				return;

			}
			if (resident.hasTown()) {
				Townhall townhall = resident.getTown().getTownHall();
				if (townhall != null) {
					BlockCoord respawn = townhall.getRandomRevivePoint();
					if (respawn != null) {
						event.setRespawnLocation(respawn.getCenteredLocation());
						CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("playerListen_repawnAtName", resident.getTown().getName()));
					}
				}
			}
		}

	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (resident != null) {
			if (resident.previewUndo != null) {
				resident.previewUndo.clear();
			}
			resident.clearInteractiveMode();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Boolean keepInventory = Boolean.valueOf(event.getEntity().getWorld().getGameRuleValue("keepInventory"));
		if (!keepInventory) {
			ArrayList<ItemStack> stacksToRemove = new ArrayList<ItemStack>();
			for (ItemStack stack : event.getDrops()) {
				if (stack != null) {
					final CustomMaterial material = CustomMaterial.getCustomMaterial(stack);
					if (material == null) continue;
					material.onPlayerDeath(event, stack);
					if (material instanceof UnitMaterial) {
						stacksToRemove.add(stack);
					} else {
						if (material instanceof UnitCustomMaterial) stacksToRemove.add(stack);
					}
				}
			}

			for (ItemStack stack : stacksToRemove) {
				event.getDrops().remove(stack);
			}
		}

		MobStatic.despawnMobsFromRadius(event.getEntity().getLocation(), 80);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDeathMonitor(PlayerDeathEvent event) {
		Player death = event.getEntity();
		Resident deathRes = CivGlobal.getResident(death);
		Player killer = event.getEntity().getKiller();
		Resident killerRes = null;
		if (deathRes.getLastAttackTime() - System.currentTimeMillis() < 2000) {
			killerRes = deathRes.getLastAttacker();
			if (killerRes == null) return;
			try {
				killer = CivGlobal.getPlayer(killerRes);
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
		if (War.isWarTime() && killer != null) {
			WarStats.incrementPlayerKills(killer.getName());
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPortalCreate(PortalCreateEvent event) {
		event.setCancelled(true);
	}

	// @EventHandler(priority = EventPriority.NORMAL)
	// public void OnCraftItemEvent(CraftItemEvent event) {
	// if (event.getInventory() == null) {
	// return;
	// }
	//
	// ItemStack resultStack = event.getInventory().getResult();
	// if (resultStack == null) {
	// return;
	// }
	//
	// if (CivSettings.techItems == null) {
	// CivLog.error("tech items null???");
	// return;
	// }
	//
	// //XXX Replaced via materials system.
	//// ConfigTechItem item = CivSettings.techItems.get(resultStack.getTypeId());
	//// if (item != null) {
	//// Resident resident = CivGlobal.getResident(event.getWhoClicked().getName());
	//// if (resident != null && resident.hasTown()) {
	//// if (resident.getCiv().hasTechnology(item.require_tech)) {
	//// return;
	//// }
	//// }
	//// event.setCancelled(true);
	//// CivMessage.sendError((Player)event.getWhoClicked(), "You do not have the required technology to craft a "+item.name);
	//// }
	// }

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerPortalEvent(PlayerPortalEvent event) {
		if (event.getCause().equals(TeleportCause.END_PORTAL)) {
			event.setCancelled(true);
			CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("playerListen_endDisabled"));
			return;
		}

		if (event.getCause().equals(TeleportCause.NETHER_PORTAL)) {
			event.setCancelled(true);
			CivMessage.sendErrorNoRepeat(event.getPlayer(), CivSettings.localize.localizedString("playerListen_netherDisabled"));
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
		// THIS EVENT IS NOT RUN IN OFFLINE MODE
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnPlayerBucketEmptyEvent(PlayerBucketEmptyEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());

		if (resident == null) {
			event.setCancelled(true);
			return;
		}

		ChunkCoord coord = new ChunkCoord(event.getBlockClicked().getLocation());
		CultureChunk cc = CivGlobal.getCultureChunk(coord);
		if (cc != null) {
			if (event.getBucket().equals(Material.LAVA_BUCKET) || event.getBucket().equals(Material.LAVA)) {

				if (!resident.hasTown() || (resident.getTown().getCiv() != cc.getCiv())) {
					CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("playerListen_placeLavaDenied"));
					event.setCancelled(true);
					return;
				}

			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void OnBrewEvent(BrewEvent event) {
		event.setCancelled(true);
		
//		/* Hardcoded disables based on ingredients used. */
//		if (event.getContents().contains(Material.BLAZE_POWDER)) {
//			if (event.getContents().getItem(3).getType() == Material.BLAZE_POWDER) {
//				event.setCancelled(true);
//				return;
//			}
//		}
//
//		if (event.getContents().contains(Material.SPIDER_EYE) || event.getContents().contains(Material.GOLDEN_CARROT) || event.getContents().contains(Material.GHAST_TEAR) || event.getContents().contains(Material.FERMENTED_SPIDER_EYE)
//				|| event.getContents().contains(Material.SULPHUR)) {
//			event.setCancelled(true);
//		}
//
//		if (event.getContents().contains(Material.POTION)) {
//			ItemStack potion = event.getContents().getItem(event.getContents().first(Material.POTION));
//
//			if (potion.getDurability() == CivData.MUNDANE_POTION_DATA || potion.getDurability() == CivData.MUNDANE_POTION_EXT_DATA || potion.getDurability() == CivData.THICK_POTION_DATA) {
//				event.setCancelled(true);
//			}
//		}
	}

	private boolean isFrendlyEffect(PotionEffect type) {
		if (type.getType().equals(PotionEffectType.SPEED)) return true;
		if (type.getType().equals(PotionEffectType.FIRE_RESISTANCE)) return true;
		if (type.getType().equals(PotionEffectType.HEAL)) return true;
		if (type.getType().equals(PotionEffectType.INCREASE_DAMAGE)) return false;
		if (type.getType().equals(PotionEffectType.INVISIBILITY)) return true;
		if (type.getType().equals(PotionEffectType.JUMP)) return true;
		if (type.getType().equals(PotionEffectType.POISON)) return false;
		if (type.getType().equals(PotionEffectType.REGENERATION)) return true;
		if (type.getType().equals(PotionEffectType.SLOW)) return false;
		if (type.getType().equals(PotionEffectType.WEAKNESS)) return false;
		return true;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPotionSplash(PotionSplashEvent event) {
		ThrownPotion potion = event.getPotion();

		if ((potion.getShooter() instanceof Player)) {
			Player player = (Player) potion.getShooter();
			Resident res = CivGlobal.getResident(player);
			if ((res.getCiv() == null)) return;
			for (LivingEntity entity : event.getAffectedEntities()) {
				if (!(entity instanceof Player)) continue;
				Player target = (Player) entity;
				Resident targetRes = CivGlobal.getResident(target);
				
				for (PotionEffect effect : event.getPotion().getEffects()) {
					if (isFrendlyEffect(effect)) { //FIXME
//						event.setCancelled(true);
//						return;
					}
				}
			}
			return;
		}
		
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onConsume(PlayerItemConsumeEvent event) {
		if (ItemManager.getTypeId(event.getItem()) == CivData.GOLDEN_APPLE) {
			CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("itemUse_errorGoldenApple"));
			event.setCancelled(true);
			return;
		}

		if (event.getItem().getType().equals(Material.POTION)) {
			PotionMeta meta = (PotionMeta) event.getItem().getItemMeta();
			for (PotionEffect effect : meta.getCustomEffects()) {
				String name = effect.getType().getName();
				Integer amp = effect.getAmplifier();
				ConfigTechPotion pot = CivSettings.techPotions.get("" + name + amp);
				if (pot != null) {
					if (!pot.hasTechnology(event.getPlayer())) {
						CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("var_playerListen_potionNoTech", pot.name));
						event.setCancelled(true);
						return;
					}
					if (pot.hasTechnology(event.getPlayer())) {
						event.setCancelled(false);
						return;
					}
				} else {
					CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("playerListen_denyUse"));
					event.setCancelled(true);
					return;
				}
			}

		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryOpenEvent(InventoryOpenEvent event) {
		if (event.getInventory() instanceof DoubleChestInventory) {
			DoubleChestInventory doubleInv = (DoubleChestInventory) event.getInventory();

			Chest leftChest = (Chest) doubleInv.getHolder().getLeftSide();
			/* Generate a new player 'switch' event for the left and right chests. */
			PlayerInteractEvent interactLeft = new PlayerInteractEvent((Player) event.getPlayer(), Action.RIGHT_CLICK_BLOCK, null, leftChest.getBlock(), null);
			BlockListener.OnPlayerSwitchEvent(interactLeft);

			if (interactLeft.isCancelled()) {
				event.setCancelled(true);
				return;
			}

			Chest rightChest = (Chest) doubleInv.getHolder().getRightSide();
			PlayerInteractEvent interactRight = new PlayerInteractEvent((Player) event.getPlayer(), Action.RIGHT_CLICK_BLOCK, null, rightChest.getBlock(), null);
			BlockListener.OnPlayerSwitchEvent(interactRight);

			if (interactRight.isCancelled()) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamageByEntityMonitor(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) return;

		Player attacker = null;
		Player defender = null;
		String damage;
		Arrow arrow = null;

		if (event.getEntity() instanceof Player) defender = (Player) event.getEntity();

		if (event.getDamager() instanceof Player) {
			attacker = (Player) event.getDamager();
		} else
			if (event.getDamager() instanceof Arrow) {
				arrow = (Arrow) event.getDamager();
				if (arrow.getShooter() instanceof Player) attacker = (Player) arrow.getShooter();
			}

		if (attacker == null && defender == null) return;

		if (attacker != null && defender != null) {
			Resident attackerRes = CivGlobal.getResident(attacker);
			Resident defenderRes = CivGlobal.getResident(defender);
			defenderRes.lastAttacker = attackerRes;
			defenderRes.lastAttackTime = System.currentTimeMillis();
		}

		// TODO Проверка на артифакты. Отключить
		if (attacker != null && attacker.hasPotionEffect(PotionEffectType.WEAKNESS)) {
			event.setCancelled(true);
			CivMessage.sendError(attacker, CivSettings.localize.localizedString("var_artifact_archer_attackForbidden"));
			return;
		}
		if (attacker != null && arrow != null) {
			if (attacker.hasPotionEffect(PotionEffectType.WEAKNESS)) {
				event.getEntity().setFireTicks(60);
				if (defender != null) {
					defender.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
					defender.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 60, 1));
				}
			}
		}

		damage = new DecimalFormat("#.#").format(event.getDamage());

		if (defender != null) {
			Resident defenderResident = CivGlobal.getResident(defender);
			if (defenderResident.isTeleporting) {
				CivMessage.send((Object) defenderResident, CivSettings.localize.localizedString("cmd_camp_teleport_moveAborted2"));
				defenderResident.isTeleporting = false;
			}
			if (defenderResident.isCombatInfo()) {
				if (attacker != null) {
					CivMessage.send(defender, CivColor.LightGray + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " "
							+ CivSettings.localize.localizedString("var_playerListen_combatDefend", CivColor.Rose + attacker.getName() + CivColor.LightGray, CivColor.Rose + damage + CivColor.LightGray));
				} else {
					String entityName = null;
					if (event.getDamager() instanceof LivingEntity) entityName = ((LivingEntity) event.getDamager()).getCustomName();
					if (entityName == null) entityName = event.getDamager().getType().toString();
					CivMessage.send(defender, CivColor.LightGray + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " "
							+ CivSettings.localize.localizedString("var_playerListen_combatDefend", CivColor.LightPurple + entityName + CivColor.LightGray, CivColor.Rose + damage + CivColor.LightGray));
				}
			}
		}

		if (attacker != null) {
			Resident attackerResident = CivGlobal.getResident(attacker);
			if (attackerResident.isCombatInfo()) {
				if (defender != null) {
					CivMessage.send(attacker, CivColor.LightGray + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " "
							+ CivSettings.localize.localizedString("var_playerListen_attack", CivColor.Rose + defender.getName() + CivColor.LightGray, CivColor.LightGreen + damage + CivColor.LightGray));
				} else {
					String entityName = null;
					if (event.getEntity() instanceof LivingEntity) entityName = ((LivingEntity) event.getEntity()).getCustomName();
					if (entityName == null) entityName = event.getEntity().getType().toString();
					CivMessage.send(attacker, CivColor.LightGray + "   " + CivSettings.localize.localizedString("playerListen_combatHeading") + " "
							+ CivSettings.localize.localizedString("var_playerListen_attack", CivColor.LightPurple + entityName + CivColor.LightGray, CivColor.LightGreen + damage + CivColor.LightGray));
				}
			}
		}

		// FIXME Ебанутый фикс урона в мониторе
		// double dmg = event.getDamage();
		// if (!event.isCancelled() && event.getEntity() instanceof LivingEntity) {
		// LivingEntity def = (LivingEntity) event.getEntity();
		// if (def.getHealth() - dmg > 0.0) {
		// def.setHealth(def.getHealth() - dmg);
		// event.setDamage(0.5);
		// } else {
		// def.setHealth(0.1);
		// event.setDamage(1.0);
		// }
		// }
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
		if (event.getMessage().contains("essentials:")) {
			event.setMessage(event.getMessage().replace("essentials:", ""));
		} else
			if (event.getMessage().contains("minecraft:")) {
				event.setMessage(event.getMessage().replace("minecraft:", ""));
			}
		if (event.getMessage().equalsIgnoreCase("/ec")) {
			event.setMessage("/enderchest");
		}
		if (event.getMessage().equalsIgnoreCase("/spawn")) {
			final Player player = event.getPlayer();
			if (player == null) {
				return;
			}
			final Resident resident = CivGlobal.getResident(player);
			if (resident == null) {
				return;
			}
			if (resident.isTeleporting) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("cmd_camp_teleport_teleportingErr"));
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void RestrictModDrops(PlayerDropItemEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Player player = event.getPlayer();
		if (player.hasPermission(CivSettings.MODERATOR) && !player.hasPermission(CivSettings.MINI_ADMIN)) {
			event.setCancelled(true);
			return;
		}
	}
}
