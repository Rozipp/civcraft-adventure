package com.avrgaming.civcraft.construct;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.construct.template.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Cannon extends Construct {

	public static ConfigBuildableInfo cannonInfo;

	public static HashMap<BlockCoord, Construct> cannons = new HashMap<BlockCoord, Construct>();

	private ConstructSign fireSignLocation;
	private ConstructSign angleSignLocation;
	private ConstructSign powerSignLocation;
	private Location cannonLocation;
	private Vector direction = new Vector(0, 0, 0);

	public static final String RESTORE_NAME = "special:Cannons";
	public static final double STEP = 1.0f;

	public static final byte WALLSIGN_EAST = 0x5;
	public static final byte WALLSIGN_WEST = 0x4;
	public static final byte WALLSIGN_NORTH = 0x2;
	public static final byte WALLSIGN_SOUTH = 0x3;
	public int signDirection;

	public static final double minAngle = -35.0f;
	public static final double maxAngle = 35.0f;
	private double angle = 0.0f;

	public static final double minPower = 0.0f;
	public static final double maxPower = 50.0f;
	private double power = 0.0f;

	private int tntLoaded = 0;
	private int shotCooldown = 0;

	public static int tntCost;
	public static int maxCooldown;
	public static int maxHitpoints;
	public static int baseStructureDamage;

	private boolean angleFlip = false;

	static {
		try {
			tntCost = CivSettings.getInteger(CivSettings.warConfig, "cannon.tnt_cost");
			maxCooldown = CivSettings.getInteger(CivSettings.warConfig, "cannon.cooldown");
			maxHitpoints = CivSettings.getInteger(CivSettings.warConfig, "cannon.hitpoints");
			baseStructureDamage = CivSettings.getInteger(CivSettings.warConfig, "cannon.structure_damage");
			String templateName = CivSettings.getString(CivSettings.warConfig, "cannon.template");
			cannonInfo = new ConfigBuildableInfo("c_cannon", "Cannon", false, templateName, false, maxHitpoints, 0);
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public static void cleanupAll() {
		for (Construct cannon : cannons.values()) {
			cannon.delete();
		}
	}

	public Cannon(Player player) {
		this.setSQLOwner(CivGlobal.getResident(player).getCiv());
		setInfo(Cannon.cannonInfo);
	}

	public static void newCannon(Player player) throws CivException {
		Resident resident = CivGlobal.getResident(player);
		if (player.isOp())
			CivMessage.send(player, "Поскольку вы оп, то можете строить пушки в мирное время");
		else {
			if (!War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("buildCannon_NotWar"));
			if (!resident.getCiv().getDiplomacyManager().isAtWar()) throw new CivException("Ваша цивилизация не участвует в войне");
		}
		Cannon cannon = new Cannon(player);
		cannon.initDefaultTemplate(player.getLocation());
		cannon.checkBlockPermissionsAndRestrictions(player);

		CivMessage.sendCiv(resident.getCiv(), CivSettings.localize.localizedString("var_buildCannon_Success", cannon.getCorner().toStringNotWorld()));
		cannon.build(player);
	}

	@Override
	public void build(Player player) throws CivException {
		this.checkBlockPermissionsAndRestrictions(player);
		try {
			this.getTemplate().saveUndoTemplate(this.getCorner().toString(), this.getCorner());
		} catch (IOException var8) {
			var8.printStackTrace();
		}
		this.getTemplate().buildTemplate(corner);
		this.bindBlocks();
		Cannon.cannons.put(this.getCorner(), this);
	}

	@Override
	public void delete() {
		try {
			this.undoFromTemplate();
		} catch (IOException | CivException e) {
			this.fancyDestroyConstructBlocks();
		}
		super.delete();
	}

	public void updateSignText() {
		updateAngleSign();
		updatePowerSign();
		updateFireSign();
	}

	private void updateAngleSign() {
		String[] line = { "YAW", "" + this.angle, "", "" };
		if (this.angle > 0)
			line[2] = "-->";
		else
			if (this.angle < 0)
				line[2] = "<--";
			else
				line[2] = "";
		angleSignLocation.setText(line);
		angleSignLocation.update();
	}

	private void updatePowerSign() {
		String[] line = { "PITCH", "" + this.power, "", "" };
		powerSignLocation.setText(line);
		powerSignLocation.update();
	}

	private void updateFireSign() {
		String[] line = { "", "", "", "" };
		line[0] = CivSettings.localize.localizedString("cannon_fire");
		boolean loaded = false;

		if (this.tntLoaded >= tntCost) {
			line[1] = CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("cannon_Loaded");
			loaded = true;
		} else
			line[1] = CivColor.Yellow + "(" + this.tntLoaded + "/" + tntCost + ") TNT";

		if (this.shotCooldown > 0)
			line[2] = CivColor.LightGray + CivSettings.localize.localizedString("cannon_cooldownWait") + " " + this.shotCooldown;
		else {
			if (loaded)
				line[2] = CivColor.LightGray + CivSettings.localize.localizedString("cannon_ready");
			else
				line[2] = CivColor.LightGray + CivSettings.localize.localizedString("cannon_addTNT");
		}
		fireSignLocation.setText(line);
		fireSignLocation.update();
	}

	private ConstructSign createSign(BlockCoord coord, SimpleBlock sb) {
		ConstructSign sign = CivGlobal.getConstructSign(coord);
		if (sign == null) sign = new ConstructSign(coord, this);
		ItemManager.setTypeIdAndData(coord.getBlock(), sb.getType(), sb.getData(), false);
		sign.setDirection(ItemManager.getData(coord.getBlock().getState()));
		sign.setType("id");
		sign.setOwner(this);
		this.addConstructSign(sign);
		CivGlobal.addConstructSign(sign);
		return sign;
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		BlockCoord coord = new BlockCoord(absCoord);
		ConstructSign sign;
		switch (sb.command) {
		case "/fire":
			sign = createSign(coord, sb);
			sign.setAction("fire");
			sign.setAllowRightClick(true);
			setFireSignLocation(sign);
			break;
		case "/angle":
			sign = createSign(coord, sb);
			sign.setAction("angle");
			sign.setAllowRightClick(true);
			this.setAngleSignLocation(sign);
			break;
		case "/power":
			sign = createSign(coord, sb);
			sign.setAction("power");
			sign.setAllowRightClick(true);
			this.setPowerSignLocation(sign);
			break;
		case "/cannon":
			this.cannonLocation = absCoord.getLocation();

			switch (sb.getData()) {
			case WALLSIGN_EAST:
				cannonLocation.add(1, 0, 0);
				direction.setX(1.0f);
				direction.setY(0.0f);
				direction.setZ(0.0f);
				break;
			case WALLSIGN_WEST:
				cannonLocation.add(-1, 0, 0);
				this.angleFlip = true;
				direction.setX(-1.0f);
				direction.setY(0.0f);
				direction.setZ(0.0f);
				break;
			case WALLSIGN_NORTH:
				cannonLocation.add(0, 0, -1);
				direction.setX(0.0f);
				direction.setY(0.0f);
				direction.setZ(-1.0f);
				break;
			case WALLSIGN_SOUTH:
				cannonLocation.add(0, 0, 1);
				this.angleFlip = true;
				direction.setX(0.0f);
				direction.setY(0.0f);
				direction.setZ(1.0f);
				break;
			default:
				CivLog.error("INVALID SIGN DIRECTION..");
				break;
			}
			signDirection = sb.getData();
			break;
		}
	}

	@Override
	public void processUndo() throws CivException {
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return null;
	}

	@Override
	public void onLoad() throws CivException {
	}

	@Override
	public void onUnload() {
	}

	@Override
	public void load(ResultSet rs) throws SQLException, CivException {
	}

	@Override
	public void save() {
	}

	@Override
	public void saveNow() throws SQLException {
	}

	@Override
	public Location repositionCenter(Location center, Template tpl) throws CivException {
		Location loc = center.clone();
		String dir = tpl.getDirection();
		double x_size = tpl.getSize_x();
		double z_size = tpl.getSize_z();
		if (dir.equalsIgnoreCase("east")) {
			loc.setZ(loc.getZ() - (z_size / 2));
			loc.setX(loc.getX());
		} else
			if (dir.equalsIgnoreCase("west")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc.setX(loc.getX() - (x_size));

			} else
				if (dir.equalsIgnoreCase("north")) {
					loc.setX(loc.getX() - (x_size / 2));
					loc.setZ(loc.getZ() - (z_size));
				} else
					if (dir.equalsIgnoreCase("south")) {
						loc.setX(loc.getX() - (x_size / 2));
						loc.setZ(loc.getZ());

					}

		return loc;
	}

	private void validateUse(Player player) throws CivException {
		if (this.getHitpoints() == 0) throw new CivException(CivSettings.localize.localizedString("cannon_destroyed"));

		Resident resident = CivGlobal.getResident(player);

		if (resident.getCiv() != getCiv()) throw new CivException(CivSettings.localize.localizedString("cannon_notMember"));
	}

	public void processSignAction(Player player, ConstructSign sign, PlayerInteractEvent event) throws CivException {
		validateUse(event.getPlayer());

		switch (sign.getAction()) {
		case "fire":
			processFire(event);
			break;
		case "angle":
			processAngle(event);
			break;
		case "power":
			processPower(event);
			break;
		}
	}

	public void processFire(PlayerInteractEvent event) throws CivException {
		if (this.shotCooldown > 0) {
			CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("cannon_waitForCooldown"));
			return;
		}
		if (this.tntLoaded < tntCost) {
			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
				if (stack != null) {
					if (stack.getType() == Material.TNT) {
						if (stack.getAmount() > 1)
							stack.setAmount(stack.getAmount() - 1);
						else
							stack = new ItemStack(Material.AIR);
						event.getPlayer().getInventory().setItemInMainHand(stack);
						this.tntLoaded++;
						CivMessage.sendSuccess(event.getPlayer(), CivSettings.localize.localizedString("cannon_addedTNT"));
						updateFireSign();
						return;
					}
				}
				CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("cannon_notLoaded"));
				return;
			} else {
				event.setCancelled(true);
				event.getPlayer().updateInventory();
				return;
			}
		} else {
			CivMessage.send(event.getPlayer(), CivSettings.localize.localizedString("cannon_fireAway"));
			cannonLocation.setDirection(direction);
			CannonProjectile proj = new CannonProjectile(this, cannonLocation.clone(), event.getPlayer());
			proj.fire();
			this.tntLoaded = 0;
			this.shotCooldown = maxCooldown;

			class SyncTask implements Runnable {
				Cannon cannon;

				public SyncTask(Cannon cannon) {
					this.cannon = cannon;
				}

				@Override
				public void run() {
					if (decrementCooldown()) return;
					TaskMaster.syncTask(new SyncTask(cannon), TimeTools.toTicks(1));
				}

				public boolean decrementCooldown() {
					cannon.shotCooldown--;
					cannon.updateFireSign();
					return cannon.shotCooldown <= 0;
				}
			}
			TaskMaster.syncTask(new SyncTask(this), TimeTools.toTicks(1));
		}
		event.getPlayer().updateInventory();
		updateFireSign();
	}

	public void processAngle(PlayerInteractEvent event) throws CivException {
		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			this.angle -= STEP;
			if (this.angle < minAngle) {
				this.angle = minAngle;
			}
		} else
			if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				this.angle += STEP;
				if (this.angle > maxAngle) {
					this.angle = maxAngle;
				}
			}

		double a = this.angle;
		if (this.angleFlip) {
			a *= -1;
		}

		if (signDirection == WALLSIGN_EAST || signDirection == WALLSIGN_WEST) {
			direction.setZ(a / 100);
		} else {
			// NORTH/SOUTH
			direction.setX(a / 100);
		}

		event.getPlayer().updateInventory();
		updateAngleSign();
	}

	public void processPower(PlayerInteractEvent event) throws CivException {
		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			this.power -= STEP;
			if (this.power < minPower) this.power = minPower;
		} else
			if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				this.power += STEP;
				if (this.power > maxPower) this.power = maxPower;
			}

		direction.setY(this.power / 100);
		event.getPlayer().updateInventory();
		updatePowerSign();
	}

	private void launchExplodeFirework(Location loc) {
		FireworkEffect fe = FireworkEffect.builder().withColor(Color.RED).withColor(Color.ORANGE).flicker(false).with(org.bukkit.FireworkEffect.Type.BALL).build();
		TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
	}

	private void destroy() {
		for (BlockCoord b : this.constructBlocks.keySet()) {
			launchExplodeFirework(b.getCenteredLocation());
			if (b.getBlock().getType().equals(Material.COAL_BLOCK)) {
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.GRAVEL, 0, false);
			} else {
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.AIR, 0, false);
			}
		}
		this.delete();
	}

	public int getDamage() {
		return baseStructureDamage;
	}

	@Override
	public void onDamage(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit) {
		this.setHitpoints(this.getHitpoints() - amount);

		if (getHitpoints() <= 0) {
			destroy();
			return;
		}

	}

	/* public void onHit(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit) { if
	 * (!this.getCiv().getDiplomacyManager().isAtWar()) { return; } boolean wasTenPercent = false; if (hit.getOwner().isDestroyed()) { if
	 * (player != null) { CivMessage.sendError(player, CivSettings.localize.localizedString( "var_buildable_alreadyDestroyed",
	 * hit.getOwner().getDisplayName())); } return; } Construct constrOwner = hit.getOwner(); if (!((constrOwner instanceof Buildable) &&
	 * ((Buildable) constrOwner).isComplete() || (hit.getOwner() instanceof Wonder))) { if (player != null) { CivMessage.sendError(player,
	 * CivSettings.localize.localizedString( "var_buildable_underConstruction", hit.getOwner().getDisplayName())); } return; } if
	 * ((hit.getOwner().getDamagePercentage() % 10) == 0) { wasTenPercent = true; } this.damage(amount);
	 * world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1); world.playEffect(hit.getCoord().getLocation(),
	 * Effect.MOBSPAWNER_FLAMES, 0); if ((hit.getOwner().getDamagePercentage() % 10) == 0 && !wasTenPercent) { if (player != null) {
	 * onDamageNotification(player, hit); } } if (player != null) { Resident resident = CivGlobal.getResident(player); if
	 * (resident.isCombatInfo()) { CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString(
	 * "var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(), (hit.getOwner().getHitpoints() + "/" +
	 * hit.getOwner().getMaxHitPoints()))); } } } */
	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
		CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(), (hit.getOwner().getDamagePercentage() + "%")));

		CivMessage.sendTown(hit.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildable_underAttackAlert", hit.getOwner().getDisplayName(), hit.getOwner().getCorner(), hit.getOwner().getDamagePercentage()));
	}

	@Override
	protected List<HashMap<String, String>> getComponentInfoList() {
		return null;
	}

	@Override
	public void onPostBuild() {
		// TODO Автоматически созданная заглушка метода

	}
}
