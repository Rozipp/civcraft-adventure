/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.components;

import java.util.Collection;
import java.util.HashSet;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.cache.PlayerLocationCache;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;

import net.minecraft.server.v1_12_R1.Vec3D;

@Getter
@Setter
public abstract class ProjectileComponent extends Component {

	protected int damage;
	protected double range;
	protected double min_range;
	protected PlayerProximityComponent proximityComponent;

	private HashSet<BlockCoord> turrets = new HashSet<BlockCoord>();

	public ProjectileComponent(Construct constr) {
		this.setConstruct(constr);
		proximityComponent = new PlayerProximityComponent();
		proximityComponent.createComponent(constr);
		loadSettings();
	}

	@Override
	public void onLoad() {
	}

	@Override
	public void onSave() {
	}

	/* We're overriding the create component class here so that all child-classes will register with this method rather than the default. This
	 * is done so that the key used in components by type will get all instances of this base class rather than having to search for the
	 * children in the componentByType list. */
	@Override
	public void createComponent(Construct constr, boolean async) {
		startRegisterComponentTask(constr, ProjectileComponent.class.getName(), true, async);
	}

	@Override
	public void destroyComponent() {
		startRegisterComponentTask(null, ProjectileComponent.class.getName(), true, true);
	}

	public void setTurretLocation(BlockCoord absCoord) {
		turrets.add(absCoord);
	}

	public void setTurretLocation(Collection<? extends BlockCoord> absCoord) {
		turrets.addAll(absCoord);
	}

	public Location getTurretCenter() {
		return getConstruct().getCenterLocation();
	}

	public Vector getVectorBetween(Location to, Location from) {
		Vector dir = new Vector();

		dir.setX(to.getX() - from.getX());
		dir.setY(to.getY() - from.getY());
		dir.setZ(to.getZ() - from.getZ());

		return dir;
	}

	public int getDamage() {
		double rate = 1;
		rate += this.getConstruct().getTownOwner().getBuffManager().getEffectiveDouble(Buff.FIRE_BOMB);
		return (int) (this.damage * rate);
	}

	public void setDamage(int damage) {
		this.damage = damage;
	}

	private Location getNearestTurret(Location playerLoc) {

		double distance = Double.MAX_VALUE;
		BlockCoord nearest = null;
		for (BlockCoord turretCoord : turrets) {
			Location turretLoc = turretCoord.getLocation();
			if (playerLoc.getWorld() != turretLoc.getWorld()) {
				return null;
			}

			double tmp = turretLoc.distance(playerLoc);
			if (tmp < distance) {
				distance = tmp;
				nearest = turretCoord;
			}

		}
		if (nearest == null) {
			return null;
		}
		return nearest.getLocation();
	}

	private boolean isWithinRange(Location residentLocation, double range) {

		if (residentLocation.getWorld() != getTurretCenter().getWorld()) {
			return false;
		}

		if (residentLocation.distance(getTurretCenter()) <= range) {
			return true;
		}
		return false;
	}

	private boolean isLit(Player player) {
		Location loc1 = player.getLocation();
		return ((loc1.getWorld()).getBlockAt(loc1).getLightFromSky() >= 15);

	}

	private boolean canSee(Player player, Location loc2) {
		Location loc1 = player.getLocation();
		Vec3D vec1 = new Vec3D(loc1.getX(), loc1.getY() + player.getEyeHeight(), loc1.getZ());
		Vec3D vec2 = new Vec3D(loc2.getX(), loc2.getY(), loc2.getZ());
		return ((CraftWorld) loc1.getWorld()).getHandle().rayTrace(vec1, vec2) == null;
	}

	protected Location adjustTurretLocation(Location turretLoc, Location playerLoc) {
		// Keep the y position the same, but advance 1 block in the direction of the player..?
		int diff = 2;

		int xdiff = 0;
		int zdiff = 0;
		if (playerLoc.getBlockX() > turretLoc.getBlockX()) {
			xdiff = diff;
		} else
			if (playerLoc.getBlockX() < turretLoc.getBlockX()) {
				xdiff = -diff;
			}

		if (playerLoc.getBlockZ() > turretLoc.getBlockZ()) {
			zdiff = diff;
		} else
			if (playerLoc.getBlockZ() < turretLoc.getBlockZ()) {
				zdiff = -diff;
			}

		return turretLoc.getBlock().getRelative(xdiff, 0, zdiff).getLocation();
	}

	public void process() {
		if (!getConstruct().isActive()) {
			return;
		}

		Player nearestPlayer = null;
		double nearestDistance = Double.MAX_VALUE;

		Location turretLoc = null;
		HashSet<PlayerLocationCache> getpl = proximityComponent.tryGetNearbyPlayers(false);
		for (PlayerLocationCache pc : getpl) {
			if (pc == null || pc.isDead()) continue;

			if (!getConstruct().getTownOwner().isOutlaw(pc.getName())) {
				Resident resident = pc.getResident();
				// Try to exit early by making sure this resident is at war.
				if (resident == null || (!resident.hasTown())) continue;
				if (!getConstruct().getCivOwner().getDiplomacyManager().isHostileWith(resident)) continue;
			}

			Location playerLoc = pc.getCoord().getLocation();
			turretLoc = getNearestTurret(playerLoc);
			if (turretLoc == null) {
				// No nearest turret, player is probably not in the same world as the turret.
				return;
			}

			Player player;
			try {
				player = CivGlobal.getPlayer(pc.getName());
			} catch (CivException e) {
				return;
			}

			if (player.getGameMode() != GameMode.SURVIVAL) {
				return;
			}

			if (!(this.getConstruct()).getConfigId().equals("s_teslatower")) {
				// XXX todo convert this to not use a player so we can async...
				if (!this.canSee(player, turretLoc)) {
					continue;
				}
			} else {
				if (!this.isLit(player)) {
					continue;
				}
			}

			if (isWithinRange(player.getLocation(), range)) {
				if (isWithinRange(player.getLocation(), min_range)) {
					continue;
				}

				double distance = player.getLocation().distance(this.getTurretCenter());
				if (distance < nearestDistance) {
					nearestPlayer = player;
					nearestDistance = distance;
				}
			}
		}

		if (nearestPlayer == null || turretLoc == null) {
			return;
		}

		fire(turretLoc, nearestPlayer);
	}

	public abstract void fire(Location turretLoc, Entity targetEntity);

	public abstract void loadSettings();

}
