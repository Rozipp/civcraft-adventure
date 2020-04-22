package com.avrgaming.civcraft.siege;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Construct;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.BuildableStatic;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;

public class Cannon extends Construct {

	public static HashMap<BlockCoord, Cannon> fireSignLocations = new HashMap<BlockCoord, Cannon>();
	public static HashMap<BlockCoord, Cannon> angleSignLocations = new HashMap<BlockCoord, Cannon>();
	public static HashMap<BlockCoord, Cannon> powerSignLocations = new HashMap<BlockCoord, Cannon>();
	public static HashMap<BlockCoord, Cannon> cannonBlocks = new HashMap<BlockCoord, Cannon>();

	private BlockCoord fireSignLocation;
	private BlockCoord angleSignLocation;
	private BlockCoord powerSignLocation;
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
	private int hitpoints = 0;
	private Resident owner;

	private HashSet<BlockCoord> blocks = new HashSet<BlockCoord>();

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
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public static void newCannon(Player player) throws CivException {
		Cannon cannon = new Cannon();
		cannon.buildCannon(player, player.getLocation());
	}

	public static void cleanupAll() {
		cannonBlocks.clear();
		powerSignLocations.clear();
		angleSignLocations.clear();
		fireSignLocations.clear();
	}

	private static void removeAllValues(Cannon cannon, HashMap<BlockCoord, Cannon> map) {
		LinkedList<BlockCoord> removeUs = new LinkedList<BlockCoord>();
		for (BlockCoord bcoord : map.keySet()) {
			Cannon c = map.get(bcoord);
			if (c == cannon) {
				removeUs.add(bcoord);
			}
		}

		for (BlockCoord bcoord : removeUs) {
			map.remove(bcoord);
		}
	}

	public void cleanup() {
		removeAllValues(this, cannonBlocks);
		removeAllValues(this, powerSignLocations);
		removeAllValues(this, angleSignLocations);
		removeAllValues(this, fireSignLocations);
	}

	public void buildCannon(Player player, Location center) throws CivException {
		String templateName;
		try {
			templateName = CivSettings.getString(CivSettings.warConfig, "cannon.template");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}

		/* Load in the template. */
		Template tpl;
		String templatePath = Template.getTemplateFilePath(templateName, Template.getDirection(center), null);
		tpl = Template.getTemplate(templatePath);
		if (tpl == null) throw new CivException(CivSettings.localize.localizedString("internalCommandException"));

		this.setTemplate(tpl);
		this.setCorner(new BlockCoord(this.repositionCenter(center, tpl)));
		this.setCenterLocation(this.getCorner().getLocation().add(tpl.size_x / 2, tpl.size_y / 2, tpl.size_z / 2));
		
		checkBlockPermissionsAndRestrictions(player, getCorner().getBlock(), tpl.size_x, tpl.size_y, tpl.size_z);
		buildCannonFromTemplate(tpl, getCorner());
		processCommandSigns();
		this.setHitpoints(maxHitpoints);
		this.owner = CivGlobal.getResident(player);

		try {
			this.saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}

	}

	public void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ) throws CivException {

		if (!War.isWarTime()) {
			throw new CivException(CivSettings.localize.localizedString("buildCannon_NotWar"));
		}

		if (player.getLocation().getY() >= 128) {
			throw new CivException(CivSettings.localize.localizedString("cannon_build_tooHigh"));
		}

		if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
			throw new CivException(CivSettings.localize.localizedString("cannon_build_overHeightLimit"));
		}

		if (!player.isOp()) {
			BuildableStatic.validateDistanceFromSpawn(centerBlock.getLocation());
		}

		int yTotal = 0;
		int yCount = 0;

		for (int x = 0; x < regionX; x++) {
			for (int y = 0; y < regionY; y++) {
				for (int z = 0; z < regionZ; z++) {
					Block b = centerBlock.getRelative(x, y, z);

					if (ItemManager.getTypeId(b) == CivData.CHEST) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_chestInWay"));
					}

					BlockCoord coord = new BlockCoord(b);

					if (CivGlobal.getProtectedBlock(coord) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_protectedInWay"));
					}

					if (CivGlobal.getConstructBlock(coord) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannotBuild_structureInWay"));
					}

					if (Cannon.cannonBlocks.containsKey(coord)) {
						throw new CivException(CivSettings.localize.localizedString("cannon_build_cannonInWay"));
					}

					yTotal += b.getWorld().getHighestBlockYAt(centerBlock.getX() + x, centerBlock.getZ() + z);
					yCount++;

					if (CivGlobal.getRoadBlock(coord) != null) {
						throw new CivException(CivSettings.localize.localizedString("cannon_build_onRoad"));
					}
				}
			}
		}

		double highestAverageBlock = (double) yTotal / (double) yCount;

		if (((centerBlock.getY() > (highestAverageBlock + 10)) || (centerBlock.getY() < (highestAverageBlock - 10)))) {
			throw new CivException(CivSettings.localize.localizedString("cannotBuild_toofarUnderground"));
		}
	}

	private void updateAngleSign(Block block) {
		Sign sign = (Sign) block.getState();
		sign.setLine(0, "YAW");
		sign.setLine(1, "" + this.angle);

		double a = this.angle;

		if (a > 0) {
			sign.setLine(2, "-->");
		} else
			if (a < 0) {
				sign.setLine(2, "<--");
			} else {
				sign.setLine(2, "");
			}

		sign.setLine(3, "");
		sign.update();
	}

	private void updatePowerSign(Block block) {
		Sign sign = (Sign) block.getState();
		sign.setLine(0, "PITCH");
		sign.setLine(1, "" + this.power);
		sign.setLine(2, "");
		sign.setLine(3, "");
		sign.update();
	}

	private void updateFireSign(Block block) {
		Sign sign = (Sign) block.getState();
		sign.setLine(0, CivSettings.localize.localizedString("cannon_fire"));
		boolean loaded = false;

		if (this.tntLoaded >= tntCost) {
			sign.setLine(1, CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("cannon_Loaded"));
			loaded = true;
		} else {
			sign.setLine(1, CivColor.Yellow + "(" + this.tntLoaded + "/" + tntCost + ") TNT");
		}

		if (this.shotCooldown > 0) {
			sign.setLine(2, CivColor.LightGray + CivSettings.localize.localizedString("cannon_cooldownWait") + " " + this.shotCooldown);
		} else {
			if (loaded) {
				sign.setLine(2, CivColor.LightGray + CivSettings.localize.localizedString("cannon_ready"));
			} else {
				sign.setLine(2, CivColor.LightGray + CivSettings.localize.localizedString("cannon_addTNT"));
			}
		}

		sign.setLine(3, "");
		sign.update();
	}

	@Override
	public void commandBlockRelatives(BlockCoord absCoord, SimpleBlock sb) {
		BlockCoord coord = new BlockCoord(absCoord);
		switch (sb.command) {
			case "/fire" :
				this.setFireSignLocation(coord);

				ItemManager.setTypeIdAndData(absCoord.getBlock(), sb.getType(), sb.getData(), false);
				updateFireSign(absCoord.getBlock());

				Cannon.fireSignLocations.put(coord, this);
				break;
			case "/angle" :
				this.setAngleSignLocation(coord);

				ItemManager.setTypeIdAndData(absCoord.getBlock(), sb.getType(), sb.getData(), false);
				updateAngleSign(absCoord.getBlock());

				Cannon.angleSignLocations.put(coord, this);
				break;
			case "/power" :
				this.setPowerSignLocation(coord);

				ItemManager.setTypeIdAndData(absCoord.getBlock(), sb.getType(), sb.getData(), false);
				updatePowerSign(absCoord.getBlock());

				Cannon.powerSignLocations.put(coord, this);
				break;
			case "/cannon" :
				this.cannonLocation = absCoord.getLocation();

				switch (sb.getData()) {
					case WALLSIGN_EAST :
						cannonLocation.add(1, 0, 0);
						direction.setX(1.0f);
						direction.setY(0.0f);
						direction.setZ(0.0f);
						break;
					case WALLSIGN_WEST :
						cannonLocation.add(-1, 0, 0);
						this.angleFlip = true;
						direction.setX(-1.0f);
						direction.setY(0.0f);
						direction.setZ(0.0f);
						break;
					case WALLSIGN_NORTH :
						cannonLocation.add(0, 0, -1);
						direction.setX(0.0f);
						direction.setY(0.0f);
						direction.setZ(-1.0f);
						break;
					case WALLSIGN_SOUTH :
						cannonLocation.add(0, 0, 1);
						this.angleFlip = true;
						direction.setX(0.0f);
						direction.setY(0.0f);
						direction.setZ(1.0f);
						break;
					default :
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
	public void build(Player player) throws Exception {
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

	private void buildCannonFromTemplate(Template tpl, BlockCoord corner) {
		Block cornerBlock = corner.getBlock();
		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block nextBlock = cornerBlock.getRelative(x, y, z);

					if (tpl.blocks[x][y][z].specialType == Type.COMMAND) {
						continue;
					}

					if (tpl.blocks[x][y][z].specialType == Type.LITERAL) {
						// Adding a command block for literal sign placement
						tpl.blocks[x][y][z].command = "/literal";
						tpl.commandBlockRelativeLocations.add(tpl.blocks[x][y][z]);
						continue;
					}

					try {
						if (ItemManager.getTypeId(nextBlock) != tpl.blocks[x][y][z].getType()) {
							/* Save it as a war block so it's automatically removed when war time ends. */
							WarRegen.saveBlock(nextBlock, Cannon.RESTORE_NAME, false);
							ItemManager.setTypeId(nextBlock, tpl.blocks[x][y][z].getType());
							ItemManager.setData(nextBlock, tpl.blocks[x][y][z].getData());
						}

						if (ItemManager.getTypeId(nextBlock) != CivData.AIR) {
							BlockCoord b = new BlockCoord(nextBlock.getLocation());
							cannonBlocks.put(b, this);
							blocks.add(b);
						}
					} catch (Exception e) {
						CivLog.error(e.getMessage());
					}
				}
			}
		}
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

	public BlockCoord getFireSignLocation() {
		return fireSignLocation;
	}

	public void setFireSignLocation(BlockCoord fireSignLocation) {
		this.fireSignLocation = fireSignLocation;
	}

	public BlockCoord getAngleSignLocation() {
		return angleSignLocation;
	}

	public void setAngleSignLocation(BlockCoord angleSignLocation) {
		this.angleSignLocation = angleSignLocation;
	}

	public BlockCoord getPowerSignLocation() {
		return powerSignLocation;
	}

	public void setPowerSignLocation(BlockCoord powerSignLocation) {
		this.powerSignLocation = powerSignLocation;
	}

	private void validateUse(Player player) throws CivException {
		if (this.hitpoints == 0) {
			throw new CivException(CivSettings.localize.localizedString("cannon_destroyed"));
		}

		Resident resident = CivGlobal.getResident(player);

		if (resident.getCiv() != owner.getCiv()) {
			throw new CivException(CivSettings.localize.localizedString("cannon_notMember"));
		}
	}

	public void processFire(PlayerInteractEvent event) throws CivException {
		validateUse(event.getPlayer());

		if (this.shotCooldown > 0) {
			CivMessage.sendError(event.getPlayer(), CivSettings.localize.localizedString("cannon_waitForCooldown"));
			return;
		}

		if (this.tntLoaded < tntCost) {
			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				ItemStack stack = event.getPlayer().getInventory().getItemInMainHand();
				if (stack != null) {
					if (ItemManager.getTypeId(stack) == CivData.TNT) {
						if (ItemManager.removeItemFromPlayer(event.getPlayer(), Material.TNT, 1)) {
							this.tntLoaded++;
							CivMessage.sendSuccess(event.getPlayer(), CivSettings.localize.localizedString("cannon_addedTNT"));
							updateFireSign(fireSignLocation.getBlock());

							return;
						}
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
//			Random rand = new Random();
//			int randDestroy = rand.nextInt(100);
//			if (randDestroy <= 15)
//			{
//				//destroy cannon
//				CivMessage.send(event.getPlayer(), "Cannon misfired and was destroyed");
//				destroy();
//				CivMessage.sendCiv(owner.getCiv(), CivColor.Yellow+"Our Cannon at "+
//						cannonLocation.getBlockX()+","+cannonLocation.getBlockY()+","+cannonLocation.getBlockZ()+
//						" has been destroyed due to misfire!");
//				return;
//			}

			CivMessage.send(event.getPlayer(), CivSettings.localize.localizedString("cannon_fireAway"));
			cannonLocation.setDirection(direction);
			Resident resident = CivGlobal.getResident(event.getPlayer());
			CannonProjectile proj = new CannonProjectile(this, cannonLocation.clone(), resident);
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
					if (cannon.decrementCooldown()) {
						return;
					}

					TaskMaster.syncTask(new SyncTask(cannon), TimeTools.toTicks(1));
				}
			}
			TaskMaster.syncTask(new SyncTask(this), TimeTools.toTicks(1));
		}

		event.getPlayer().updateInventory();
		updateFireSign(fireSignLocation.getBlock());

	}

	public boolean decrementCooldown() {
		this.shotCooldown--;
		this.updateFireSign(fireSignLocation.getBlock());

		if (this.shotCooldown <= 0) {
			return true;
		}

		return false;
	}

	public void processAngle(PlayerInteractEvent event) throws CivException {
		validateUse(event.getPlayer());

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
		updateAngleSign(this.angleSignLocation.getBlock());
	}

	public void processPower(PlayerInteractEvent event) throws CivException {
		validateUse(event.getPlayer());

		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			this.power -= STEP;
			if (this.power < minPower) {
				this.power = minPower;
			}
		} else
			if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				this.power += STEP;
				if (this.power > maxPower) {
					this.power = maxPower;
				}
			}

		direction.setY(this.power / 100);
		event.getPlayer().updateInventory();
		updatePowerSign(this.powerSignLocation.getBlock());
	}

	public void onHit(BlockBreakEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());

		if (!resident.hasTown()) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_NotAtWar"));
			return;
		}

		if (resident.getCiv() == owner.getCiv()) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_ownCannon"));
			return;
		}

		if (!resident.getCiv().getDiplomacyManager().atWarWith(owner.getCiv())) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_NotWarringCiv") + "(" + owner.getCiv().getName() + ")");
			return;
		}

		if (this.hitpoints == 0) {
			CivMessage.sendError(resident, CivSettings.localize.localizedString("cannon_onHit_alreadyDestroyed"));
			return;
		}

		this.hitpoints--;

		if (hitpoints <= 0) {
			destroy();
			CivMessage.send(event.getPlayer(), CivColor.LightGreen + CivColor.BOLD + CivSettings.localize.localizedString("cannon_onHit_Destroyed"));
			CivMessage.sendCiv(owner.getCiv(), CivColor.Yellow + CivSettings.localize.localizedString("var_cannon_onHit_DestroyAlert",
					cannonLocation.getBlockX() + "," + cannonLocation.getBlockY() + "," + cannonLocation.getBlockZ()));
			return;
		}

		CivMessage.send(event.getPlayer(),
				CivColor.Yellow + CivSettings.localize.localizedString("cannon_onHit_doDamage") + " (" + this.hitpoints + "/" + maxHitpoints + ")");
		CivMessage.sendCiv(owner.getCiv(), CivColor.LightGray + CivSettings.localize.localizedString("var_cannon_onHit_doDamageAlert",
				hitpoints + "/" + maxHitpoints, cannonLocation.getBlockX() + "," + cannonLocation.getBlockY() + "," + cannonLocation.getBlockZ()));
	}

	private void launchExplodeFirework(Location loc) {
		FireworkEffect fe = FireworkEffect.builder().withColor(Color.RED).withColor(Color.ORANGE).flicker(false).with(org.bukkit.FireworkEffect.Type.BALL)
				.build();
		TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
	}

	private void destroy() {
		for (BlockCoord b : blocks) {
			launchExplodeFirework(b.getCenteredLocation());
			if (b.getBlock().getType().equals(Material.COAL_BLOCK)) {
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.GRAVEL, 0, false);
			} else {
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.AIR, 0, false);
			}
		}

		ItemManager.setTypeIdAndData(fireSignLocation.getBlock(), CivData.AIR, 0, false);
		ItemManager.setTypeIdAndData(angleSignLocation.getBlock(), CivData.AIR, 0, false);
		ItemManager.setTypeIdAndData(powerSignLocation.getBlock(), CivData.AIR, 0, false);

		blocks.clear();
		this.cleanup();
	}

	public int getDamage() {
		return baseStructureDamage;
	}

	@Override
	public String getDisplayName() {
		return "Cannon";
	}

	@Override
	public void onDamage(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit) {
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
			return;
		}
		boolean wasTenPercent = false;
		if (hit.getOwner().isDestroyed()) {
			if (player != null) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_alreadyDestroyed", hit.getOwner().getDisplayName()));
			}
			return;
		}

		Construct constrOwner = hit.getOwner();
		if (!((constrOwner instanceof Buildable) && ((Buildable) constrOwner).isComplete() || (hit.getOwner() instanceof Wonder))) {
			if (player != null) {
				CivMessage.sendError(player, CivSettings.localize.localizedString("var_buildable_underConstruction", hit.getOwner().getDisplayName()));
			}
			return;
		}
		if ((hit.getOwner().getDamagePercentage() % 10) == 0) {
			wasTenPercent = true;
		}

		this.damage(amount);

		world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
		world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

		if ((hit.getOwner().getDamagePercentage() % 10) == 0 && !wasTenPercent) {
			if (player != null) {
				onDamageNotification(player, hit);
			}
		}

		if (player != null) {
			Resident resident = CivGlobal.getResident(player);
			if (resident.isCombatInfo()) {
				CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess",
						hit.getOwner().getDisplayName(), (hit.getOwner().getHitpoints() + "/" + hit.getOwner().getMaxHitPoints())));
			}
		}
	}

	@Override
	public void onDamageNotification(Player player, ConstructDamageBlock hit) {
		CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("var_buildable_OnDamageSuccess", hit.getOwner().getDisplayName(),
				(hit.getOwner().getDamagePercentage() + "%")));

		CivMessage.sendTown(hit.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_buildable_underAttackAlert",
				hit.getOwner().getDisplayName(), hit.getOwner().getCorner(), hit.getOwner().getDamagePercentage()));
	}

	@Override
	protected List<HashMap<String, String>> getComponentInfoList() {
		return null;
	}
}
