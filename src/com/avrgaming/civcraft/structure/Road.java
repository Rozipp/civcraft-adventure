package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.ConstructBlock;
import com.avrgaming.civcraft.construct.ConstructDamageBlock;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.listener.MarkerPlacementManager;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;

public class Road extends Structure {
	private static double MAX_SEGMENT = 100;
	private static final int WIDTH = 3;
	private static final int RECURSION_LIMIT = 350;
	public static double ROAD_PLAYER_SPEED = 1.5;
	public static double ROAD_HORSE_SPEED = 1.1;
	public static double ROAD_COST_PER_SEGMENT = 1;
	public static final int HEIGHT = 4;
	private Date nextRaidDate;

	private static final int ROAD_MATERIAL = CivData.COBBLESTONE;
	private static int DEBUG_DATA = 0;

	/* A road is a special type of structure that is built like a Wall but is layered on the ground. The road allows faster movement speeds.
	 * Road blocks are attackable for a 2 hour window based on when they are built. */

	public Road(String id, Town town) throws CivException {
		super(id, town);
		/* Set next Raid date 24 hours from now. */
		nextRaidDate = new Date();
		nextRaidDate.setTime(nextRaidDate.getTime() + 24 * 60 * 60 * 1000);
	}

	public Road(ResultSet rs) throws SQLException, CivException {
		super(rs);
		this.loadSessionData();
	}

	public static Road newRoad(Player player, String id, Town town) throws CivException {
		Road road = new Road(id, town);
		if (!road.getTown().hasTechnology(road.getRequiredTechnology())) throw new CivException(CivSettings.localize.localizedString("road_missingTech"));

		if (War.isWarTime()) throw new CivException(CivSettings.localize.localizedString("road_warTime"));

		/* Put the player into a "place mode" which allows them to place down markers */
		MarkerPlacementManager.addToPlacementMode(player, road, CivSettings.localize.localizedString("road_startPlacement"));
		return road;
	}

	@Override
	public void loadSettings() {
		super.loadSettings();

		try {
			ROAD_PLAYER_SPEED = CivSettings.getDouble(CivSettings.structureConfig, "road.player_speed");
			ROAD_HORSE_SPEED = CivSettings.getDouble(CivSettings.structureConfig, "road.horse_speed");
			MAX_SEGMENT = CivSettings.getInteger(CivSettings.structureConfig, "road.max_segment");
			this.raidLength = CivSettings.getInteger(CivSettings.structureConfig, "road.raid_length");
			ROAD_COST_PER_SEGMENT = CivSettings.getDouble(CivSettings.structureConfig, "road.cost_per_segment");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}

	}

	public HashMap<BlockCoord, RoadBlock> roadBlocks = new HashMap<BlockCoord, RoadBlock>();
	private boolean hasOldBlockData = false;
	private HashMap<BlockCoord, SimpleBlock> oldBlockData = new HashMap<BlockCoord, SimpleBlock>();
	private int raidLength = 2;
	private int segmentsBuilt = 0;

	@Override
	public void processUndo() throws CivException {

		if (!this.hasOldBlockData) throw new CivException(CivSettings.localize.localizedString("road_mustDemolish"));

		for (BlockCoord bcoord : oldBlockData.keySet()) {
			SimpleBlock sb = oldBlockData.get(bcoord);

			Material material = ItemManager.getMaterial(sb.getType());
			if (CivSettings.restrictedUndoBlocks.contains(material)) continue;

			Block block = bcoord.getBlock();
			ItemManager.setTypeId(block, sb.getType());
			ItemManager.setData(block, sb.getData());
		}

		LinkedList<RoadBlock> removed = new LinkedList<RoadBlock>();
		for (RoadBlock rb : roadBlocks.values()) {
			removed.add(rb);
		}

		for (RoadBlock rb : removed) {
			rb.delete();
		}

		double totalCost = this.getTotalCost();
		this.getTown().getTreasury().deposit(totalCost);
		CivMessage.sendTown(this.getTown(), CivColor.Yellow + CivSettings.localize.localizedString("var_road_undoComplete", totalCost, CivSettings.CURRENCY_NAME));

		this.delete();
	}

	@Override
	public void onDemolish() throws CivException {
		for (RoadBlock rb : roadBlocks.values()) {
			Block block = rb.getCoord().getBlock();
			ItemManager.setTypeId(block, rb.getOldType());
			ItemManager.setData(block, rb.getOldData());
		}
	}

	@Override
	public void undoFromTemplate() {
	}

	@Override
	public void delete() {
		CivGlobal.getSessionDatabase().delete_all(this.getSessionKey());

		LinkedList<RoadBlock> remove = new LinkedList<RoadBlock>();
		for (RoadBlock rb : this.roadBlocks.values()) {
			remove.add(rb);
		}

		for (RoadBlock rb : remove) {
			rb.delete();
		}

		CivGlobal.removeStructure(this);
		this.getTown().removeStructure(this);
		this.unbindConstructBlocks();

		try {
			SQL.deleteNamedObject(this, TABLE_NAME);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saveNow() throws SQLException {
		super.saveNow();

		for (RoadBlock rb : this.roadBlocks.values()) {
			rb.saveNow();
		}
	}

	// @Override
	// public void build(Player player) throws Exception {
	//// /*
	//// * Put the player into a "place mode" which allows them to place down
	//// * markers
	//// */
	//// MarkerPlacementManager.addToPlacementMode(player, this, "Road Marker");
	// }

	@Override
	public void onMarkerPlacement(Player player, Location next, ArrayList<Location> locs) throws CivException {

		CultureChunk cc = CivGlobal.getCultureChunk(next);
		if (cc != null && cc.getTown().getCiv() != this.getTown().getCiv()) {
			throw new CivException(CivSettings.localize.localizedString("road_wrongCulture"));
		}

		if (locs.size() <= 1) {
			CivMessage.send(player, CivColor.LightGray + CivSettings.localize.localizedString("road_marker1"));
			return;
		}

		/* Validate our locations */

		double distance = locs.get(0).distance(locs.get(1));
		if (distance > Road.MAX_SEGMENT) {
			throw new CivException(CivSettings.localize.localizedString("var_road_tooLong", Road.MAX_SEGMENT));
		}

		MarkerPlacementManager.removeFromPlacementMode(player, false);

		/* Reduce locations by one so that road is built at your feet. */
		locs.get(0).add(0, -1, 0);
		locs.get(1).add(0, -1, 0);

		/* Gather the blocks that changed. */
		HashMap<String, SimpleBlock> simpleBlocks = new HashMap<String, SimpleBlock>();
		this.segmentsBuilt = this.buildRoadSegment(player, locs.get(1), locs.get(0), 0, simpleBlocks, 0);

		/* Validate each of the blocks that are about to change */
		LinkedList<SimpleBlock> removed = new LinkedList<SimpleBlock>();
		for (SimpleBlock sb : simpleBlocks.values()) {
			BlockCoord bcoord = new BlockCoord(sb);
			if (!validateBlockLocation(bcoord)) {
				/* This block cannot be placed here, but it will NOT stop the construction of the road. */
				removed.add(sb);
			}
		}

		/* Remove any invalid blocks. */
		for (SimpleBlock sb : removed) {
			simpleBlocks.remove(sb.getKey());
		}

		double totalCost = this.getTotalCost();
		if (!this.getTown().getTreasury().hasEnough(totalCost)) {
			throw new CivException(CivSettings.localize.localizedString("var_road_tooPoor", totalCost, CivSettings.CURRENCY_NAME));
		}

		for (SimpleBlock sb : simpleBlocks.values()) {
			BlockCoord bcoord = new BlockCoord(sb);

			this.oldBlockData.put(new BlockCoord(bcoord), new SimpleBlock(bcoord.getBlock()));

			/* Set air blocks above road. */
			for (int i = 1; i < Road.HEIGHT; i++) {
				BlockCoord bcoord2 = new BlockCoord(bcoord);
				bcoord2.setY(sb.y + i);
				this.oldBlockData.put(new BlockCoord(bcoord2), new SimpleBlock(bcoord2.getBlock()));
			}
		}

		/* Build the road blocks */
		this.hasOldBlockData = true;
		for (SimpleBlock sb : simpleBlocks.values()) {
			BlockCoord bcoord = new BlockCoord(sb);

			/* Save old block data. */
			// this.oldBlockData.put(new BlockCoord(bcoord), new SimpleBlock(bcoord.getBlock()));
			addRoadBlock(bcoord);

			/* Set new block data. */
			ItemManager.setTypeId(bcoord.getBlock(), sb.getType());
			ItemManager.setData(bcoord.getBlock(), sb.getData());

			/* Set air blocks above road. */
			for (int i = 1; i < Road.HEIGHT; i++) {
				BlockCoord bcoord2 = new BlockCoord(bcoord);
				bcoord2.setY(sb.y + i);
				if (!simpleBlocks.containsKey(SimpleBlock.getKeyFromBlockCoord(bcoord2))) {
					ItemManager.setTypeId(bcoord2.getBlock(), CivData.AIR);
				}
			}

		}

		/* Register structure in global tables. */
		this.getTown().getTreasury().withdraw(totalCost);
		CivMessage.sendTown(this.getTown(), CivColor.LightGreen + CivSettings.localize.localizedString("var_road_success", CivColor.Yellow + totalCost + CivColor.LightGreen, CivSettings.CURRENCY_NAME));
		this.getTown().addStructure(this);
		CivGlobal.addStructure(this);
		this.getTown().lastBuildableBuilt = this;
		this.setComplete(true);
		this.setCorner(locs.get(0));
		locs.clear();
		this.save();
		this.saveSaveSessionData();
	}

	private double getTotalCost() {
		double total;
		total = this.segmentsBuilt * Road.ROAD_COST_PER_SEGMENT;
		return total;
	}

	/* We're going to try something tricky here. In order to make building roads not annoying we can't have our own structures getting in the
	 * way. However if we dont "halt" for structures then an enemy can easily use the road to "slice" through our structures and grief us. So
	 * instead what we'll do is we'll return false if this block is "not allowed" to be built here, but will not halt the entire road
	 * construction. If we hit a structure that doesn't belong to our town, then we know this is a potential griefing situation and we'll cancel
	 * the entire road. If it's one of our structure blocks, we'll assume the player knows what they are doing and allow the constructio to
	 * continue, minus the offending blocks. */

	private boolean validateBlockLocation(BlockCoord startCoord) throws CivException {

		boolean allowedToPlaceHere = true;
		for (int i = 0; i < Road.HEIGHT; i++) {
			BlockCoord bcoord = new BlockCoord(startCoord);
			ChunkCoord coord = new ChunkCoord(bcoord);
			bcoord.setY(startCoord.getY() + i);

			if (ItemManager.getTypeId(bcoord.getBlock()) == CivData.CHEST) throw new CivException(CivSettings.localize.localizedString("var_road_validate_wouldDestroyChest", bcoord.toString()));
			if (CivGlobal.getCampAt(coord) != null) throw new CivException(CivSettings.localize.localizedString("road_validate_camp"));

			ConstructBlock structBlock = CivGlobal.getConstructBlock(bcoord);
			if (structBlock != null) {
				allowedToPlaceHere = false;
				if (structBlock.getCiv() != this.getCiv()) {
					throw new CivException(CivSettings.localize.localizedString("var_road_validate_structure", structBlock.getCiv().getName(), (structBlock.getX() + ", " + structBlock.getY() + ", " + structBlock.getZ())));
				}
			}

			RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
			if (rb != null) {
				allowedToPlaceHere = false;
				if (rb.getRoad().getCiv() != this.getCiv()) {
					throw new CivException(CivSettings.localize.localizedString("var_road_validate_roadInWay", rb.getRoad().getCiv().getName(), (rb.getCoord().getX() + ", " + rb.getCoord().getY() + ", " + rb.getCoord().getZ())));
				}
			}
		}
		return allowedToPlaceHere;
	}

	private String getSessionKey() {
		return "Road:" + this.getCorner().toString();
	}

	private int buildRoadSegment(Player player, Location locFirst, Location locSecond, int blockCount, HashMap<String, SimpleBlock> simpleBlocks, int segments) throws CivException {

		Vector dir = new Vector(locFirst.getX() - locSecond.getX(), //
				locFirst.getY() - locSecond.getY(), //
				locFirst.getZ() - locSecond.getZ());
		dir.normalize();

		dir.multiply(0.5);
		getHorizontalSegment(player, locSecond, simpleBlocks);
		segments++;

		/* Increment towards the first location. */
		double distance = locSecond.distance(locFirst);

		BlockCoord lastBlockCoord = new BlockCoord(locSecond);
		BlockCoord currentBlockCoord = new BlockCoord(locSecond);
		int lastY = locSecond.getBlockY();

		while (locSecond.distance(locFirst) > 1.0) {
			locSecond.add(dir);

			currentBlockCoord.setFromLocation(locSecond);
			if (lastBlockCoord.distance(currentBlockCoord) < Road.WIDTH)
				continue; /* Didn't move far enough, keep going. */
			else
				lastBlockCoord.setFromLocation(locSecond);

			/* Make sure the Y doesnt get too steep. */
			if (Math.abs(lastY - locSecond.getBlockY()) > 1.0) throw new CivException(CivSettings.localize.localizedString("road_validate_tooSteep"));
			if (locSecond.getBlockY() < 5) throw new CivException(CivSettings.localize.localizedString("road_validate_tooLow"));
			lastY = locSecond.getBlockY();
			blockCount++;
			if (blockCount > Road.RECURSION_LIMIT) throw new CivException(CivSettings.localize.localizedString("road_validate_recursionLimit"));

			getHorizontalSegment(player, locSecond, simpleBlocks);

			// Road.DEBUG_DATA++;
			// if (Road.DEBUG_DATA > 15) {
			// Road.DEBUG_DATA = 0;
			// }
			segments++;

			// Distance should always be going down, as a failsave
			// check that it is. Abort if our distance goes up.
			double tmpDist = locSecond.distance(locFirst);
			if (tmpDist > distance) break;
		}

		/* Build last road segment */
		getHorizontalSegment(player, locFirst, simpleBlocks);
		return segments;
	}

	private void getHorizontalSegment(Player player, Location loc, HashMap<String, SimpleBlock> simpleBlocks) {
		Location tmp = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());

		/* Go through each location here and draw a circle around it the circles will represent the line's width. */
		Road.getCircle(tmp.getBlockX(), tmp.getBlockY(), tmp.getBlockZ(), tmp.getWorld().getName(), Road.WIDTH, simpleBlocks);
	}

	public static void getCircle(int blockX, int blockY, int blockZ, String world, int radius, HashMap<String, SimpleBlock> simpleBlocks) {
		int error = -radius;
		int x = radius;
		int z = 0;

		while (x >= z) {
			int lastZ = z;

			error += z;
			++z;
			error += z;

			plotFourPoints(blockX, blockZ, x, lastZ, blockY, world, simpleBlocks);

			if (error >= 0) {
				if (x != lastZ) plotFourPoints(blockX, blockZ, lastZ, x, blockY, world, simpleBlocks);
				error -= x;
				--x;
				error -= x;
			}
		}
	}

	private static void setPixel(int x, int y, int z, String world, HashMap<String, SimpleBlock> simpleBlocks) {
		SimpleBlock sb = new SimpleBlock(Road.ROAD_MATERIAL, Road.DEBUG_DATA);
		sb.worldname = world;
		sb.x = x;
		sb.y = y;
		sb.z = z;

		/* Always defer to 'first' simple block in the map. */
		simpleBlocks.put(sb.getKey(), sb);
	}

	private static void plotFourPoints(int cx, int cz, int x, int z, int baseY, String world, HashMap<String, SimpleBlock> simpleBlocks) {

		/* This bit of code does the outline only. */
		// setPixel(cx + x, baseY, cz + z, world, simpleBlocks);
		// setPixel(cx - x, baseY, cz + z, world, simpleBlocks);
		// setPixel(cx + x, baseY, cz - z, world, simpleBlocks);
		// setPixel(cx - x, baseY, cz - z, world, simpleBlocks);

		horizontalLine(cx - x, baseY, cz + z, cx + x, world, simpleBlocks);
		if (x != 0 && z != 0) {
			horizontalLine(cx - x, baseY, cz - z, cx + x, world, simpleBlocks);
		}
	}

	private static void horizontalLine(int x, int y, int z, int size, String world, HashMap<String, SimpleBlock> simpleBlocks) {
		for (int i = x; i <= size; ++i) {
			setPixel(i, y, z, world, simpleBlocks);
		}
	}

	@Override
	public boolean showOnDynmap() {
		return false;
	}

	@Override
	public String getDynmapDescription() {
		return "";
	}

	@Override
	public String getMarkerIconName() {
		return "";
	}

	@Override
	public void bindBlocks() {
		this.setCenterLocation(this.getCorner().getLocation());
	}

	public void addBlocksAbove(RoadBlock rb) {

		/* Add blocks above the road, but do not save them in the Road structure. This prevents them from being saved but still allows us to look
		 * them up via the bcoord hash so that we can prevent non-owners from placing blocks on the road. */
		for (int i = 1; i < Road.HEIGHT; i++) {
			BlockCoord bcoord = new BlockCoord(rb.getCoord());
			bcoord.setY(rb.getCoord().getY() + i);

			SimpleBlock sb = this.oldBlockData.get(bcoord);
			RoadBlock rb2 = new RoadBlock(sb.getType(), sb.getData());
			rb2.setCoord(bcoord);
			rb2.setRoad(rb.getRoad());
			rb2.setAboveRoadBlock(true);
			roadBlocks.put(bcoord, rb2);
			CivGlobal.addRoadBlock(rb2);
		}
	}

	public void addRoadBlock(BlockCoord coord) {
		SimpleBlock sb = this.oldBlockData.get(coord);
		RoadBlock rb = new RoadBlock(sb.getType(), sb.getData());
		rb.setCoord(coord);
		rb.setRoad(this);

		roadBlocks.put(coord, rb);
		CivGlobal.addRoadBlock(rb);
		this.addBlocksAbove(rb);
	}

	public void addRoadBlock(RoadBlock rb) {
		roadBlocks.put(rb.getCoord(), rb);
		CivGlobal.addRoadBlock(rb);
	}

	public void deleteRoadBlock(RoadBlock roadBlock) {
		roadBlock.delete();

		if (this.roadBlocks.size() == 0) {
			/* We're out of road blocks. This road is no more! */
			CivMessage.sendTown(this.getTown(), CivSettings.localize.localizedString("var_road_destroyed", this.getCorner()));
			this.delete();
		}
	}

	public void removeRoadBlock(RoadBlock roadBlock) {
		roadBlocks.remove(roadBlock.getCoord());
		CivGlobal.removeRoadBlock(roadBlock);
	}

	private void saveSaveSessionData() {
		this.sessionAdd(getSessionKey(), "" + this.nextRaidDate.getTime() + ":" + this.segmentsBuilt);
	}

	private void loadSessionData() {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDatabase().lookup(getSessionKey());
		if (entries == null) {
			saveSaveSessionData();
		}

		String[] split = entries.get(0).value.split(":");

		long time = Long.valueOf(split[0]);
		this.nextRaidDate = new Date(time);
		this.segmentsBuilt = Integer.valueOf(split[1]);
	}

	public Date getNextRaidDate() {
		Date raidEnd = new Date(this.nextRaidDate.getTime());
		raidEnd.setTime(this.nextRaidDate.getTime() + 60 * 60 * 1000 * this.raidLength);

		Date now = new Date();
		if (now.getTime() > raidEnd.getTime()) {
			this.nextRaidDate.setTime(nextRaidDate.getTime() + 60 * 60 * 1000 * 24);
		}

		return this.nextRaidDate;
	}

	public void setNextRaidDate(Date next) {
		this.nextRaidDate = next;
		this.save();
	}

	@Override
	public void onDamage(int amount, World world, Player player, BlockCoord coord, ConstructDamageBlock hit) {
		boolean wasTenPercent = false;

		if (hit.getOwner().isDestroyed()) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_road_alreadyDestroyed", hit.getOwner().getDisplayName()));
			return;
		}

		if (!(hit.getOwner() instanceof Buildable && ((Buildable) hit.getOwner()).isComplete() || (hit.getOwner() instanceof Wonder))) {
			CivMessage.sendError(player, CivSettings.localize.localizedString("var_road_underConstruction", hit.getOwner().getDisplayName()));
			return;
		}

		if ((hit.getOwner().getDamagePercentage() % 10) == 0) {
			wasTenPercent = true;
		}

		this.damage(amount);

		world.playSound(hit.getCoord().getLocation(), Sound.BLOCK_ANVIL_USE, 0.2f, 1);
		world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);

		if ((hit.getOwner().getDamagePercentage() % 10) == 0 && !wasTenPercent) {
			onDamageNotification(player, hit);
		}
	}

	@Override
	public void onDestroy() {
		// can be overriden in subclasses.
		CivMessage.global(CivSettings.localize.localizedString("var_road_destroySuccess", this.getDisplayName(), this.getTown().getName()));
		this.setHitpoints(0);
		this.fancyDestroyConstructBlocks();
		this.delete();
	}

	public void fancyDestroyConstructBlocks() {
		for (BlockCoord coord : this.roadBlocks.keySet()) {

			if (CivGlobal.getConstructChest(coord) != null) {
				continue;
			}

			if (CivGlobal.getConstructSign(coord) != null) {
				continue;
			}

			if (ItemManager.getTypeId(coord.getBlock()) == CivData.AIR) {
				continue;
			}

			if (ItemManager.getTypeId(coord.getBlock()) == CivData.CHEST) {
				continue;
			}

			if (ItemManager.getTypeId(coord.getBlock()) == CivData.SIGN) {
				continue;
			}

			if (ItemManager.getTypeId(coord.getBlock()) == CivData.WALL_SIGN) {
				continue;
			}

			if (CivSettings.alwaysCrumble.contains(ItemManager.getTypeId(coord.getBlock()))) {
				ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
				continue;
			}

			Random rand = new Random();

			// Each block has a 10% chance to turn into gravel
			if (rand.nextInt(100) <= 10) {
				ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
				ItemManager.setData(coord.getBlock(), 0, true);

				continue;
			}

			// Each block has a 50% chance of starting a fire
			if (rand.nextInt(100) <= 50) {
				ItemManager.setTypeId(coord.getBlock(), CivData.FIRE);
				ItemManager.setData(coord.getBlock(), 0, true);
				continue;
			}

			// Each block has a 1% chance of launching an explosion effect
			if (rand.nextInt(100) <= 1) {
				FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED).withTrail().withFlicker().build();
				FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
				for (int i = 0; i < 3; i++) {
					try {
						fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
