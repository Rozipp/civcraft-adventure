package com.avrgaming.civcraft.construct;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.constructs.Cannon;
import com.avrgaming.civcraft.construct.structures.Cityhall;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.EntityProximity;
import com.avrgaming.civcraft.util.ItemManager;

import net.minecraft.server.v1_12_R1.EntityPlayer;

public class CannonProjectile {
	public Cannon cannon;
	public Location loc;
	private Location startLoc;
	public Player whoFired;
	public double speed = 1.0f;

	public static double yield;
	public static double playerDamage;
	public static double maxRange;
	public static int controlBlockHP;
	static {
		try {
			yield = CivSettings.getDouble(CivSettings.warConfig, "cannon.yield");
			playerDamage = CivSettings.getDouble(CivSettings.warConfig, "cannon.player_damage");
			maxRange = CivSettings.getDouble(CivSettings.warConfig, "cannon.max_range");
			controlBlockHP = CivSettings.getInteger(CivSettings.warConfig, "cannon.control_block_hp");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public CannonProjectile(Cannon cannon, Location loc, Player whoFired) {
		this.cannon = cannon;
		this.loc = loc;
		this.startLoc = loc.clone();
		this.whoFired = whoFired;
	}

	private void explodeBlock(Block b) {
		// WarRegen.explodeThisBlock(b, Cannon.RESTORE_NAME);
		launchExplodeFirework(b.getLocation());
	}

	public void onHit() {
		// launchExplodeFirework(loc);
		Resident resident = CivGlobal.getResident(whoFired);
		int radius = (int) yield;
		HashSet<Construct> structuresHit = new HashSet<>();
		
		for (int x = -radius; x < radius; x++) {
			for (int z = -radius; z < radius; z++) {
				for (int y = -radius; y < radius; y++) {
					Block b = loc.getBlock().getRelative(x, y, z);
					if (b.getType() == Material.BEDROCK) continue;

					if (loc.distanceSquared(b.getLocation()) <= yield * yield) {
						BlockCoord bcoord = new BlockCoord(b);
						ConstructBlock cb = CivGlobal.getConstructBlock(bcoord);
						if (cb == null) {
							explodeBlock(b);
							continue;
						}
						if (!cb.isDamageable()) continue;
						if (cb.getOwner() instanceof Cityhall) {
							Cityhall th = (Cityhall) cb.getOwner();
							if (th.getControlPoints().containsKey(bcoord)) continue;
						}
						if (!cb.getOwner().isDestroyed() && !structuresHit.contains(cb.getOwner())) {
							structuresHit.add(cb.getOwner());
							if (cb.getOwner() instanceof Cityhall) {
								Cityhall th = (Cityhall) cb.getOwner();
								if (th.getHitpoints() == 0)
									explodeBlock(b);
								else
									th.onCannonDamage(cannon.getDamage(), this);
							} else {
								if (!cb.getCiv().getDiplomacyManager().atWarWith(resident.getCiv())) {
									CivMessage.sendError(whoFired, CivSettings.localize.localizedString("cannonProjectile_ErrorNotAtWar"));
									return;
								}

								cb.getOwner().onDamage(cannon.getDamage(), whoFired, cb);
								CivMessage.sendCiv(cb.getCiv(),
										CivColor.Yellow
												+ CivSettings.localize.localizedString("cannonProjectile_hitAnnounce", cb.getOwner().getDisplayName(),
														cb.getOwner().getCenterLocation().getX() + "," + cb.getOwner().getCenterLocation().getY() + "," + cb.getOwner().getCenterLocation().getZ())
												+ " (" + cb.getOwner().getHitpoints() + "/" + cb.getOwner().getMaxHitPoints() + ")");
							}

							CivMessage.sendCiv(resident.getCiv(), CivColor.LightGreen + CivSettings.localize.localizedString("var_cannonProjectile_hitSuccess", ((Buildable) cb.getOwner()).getTownOwner().getName(), cb.getOwner().getDisplayName())
									+ " (" + cb.getOwner().getHitpoints() + "/" + cb.getOwner().getMaxHitPoints() + ")");
						}
					}
				}
			}
		}

		/* Instantly kill any players caught in the blast. */
		LinkedList<Entity> players = EntityProximity.getNearbyEntities(null, loc, yield, EntityPlayer.class);
		for (Entity e : players) {
			Player player = (Player) e;
			player.damage(playerDamage);
			if (player.isDead()) {
				CivMessage.global(CivColor.LightGray + CivSettings.localize.localizedString("var_cannonProjectile_userKilled", player.getName(), whoFired.getName()));
			}
		}
	}

	private void launchExplodeFirework(Location loc) {
		Random rand = new Random();
		int rand1 = rand.nextInt(100);

		if (rand1 > 90) {
			FireworkEffect fe = FireworkEffect.builder().withColor(Color.ORANGE).withColor(Color.YELLOW).flicker(true).with(Type.BURST).build();
			TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
		}
	}

	public boolean advance() {
		Vector dir = loc.getDirection();
		dir.add(new Vector(0.0f, -0.008, 0.0f)); // Apply 'gravity'
		loc.setDirection(dir);

		loc.add(dir.multiply(speed));
		loc.getWorld().createExplosion(loc, 0.0f, false);

		if (ItemManager.getTypeId(loc.getBlock()) != CivData.AIR) return true;
		if (loc.distance(startLoc) > maxRange) return true;

		return false;
	}

	public void fire() {
		class SyncTask implements Runnable {
			CannonProjectile proj;

			public SyncTask(CannonProjectile proj) {
				this.proj = proj;
			}

			@Override
			public void run() {
				if (proj.advance()) {
					onHit();
					return;
				}
				TaskMaster.syncTask(this, 1);
			}
		}

		TaskMaster.syncTask(new SyncTask(this));
	}

}
