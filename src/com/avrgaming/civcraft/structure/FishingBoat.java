/************************************************************************* AVRGAMING LLC __________________
 * 
 * [2013] AVRGAMING LLC All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of AVRGAMING LLC and its suppliers, if any. The intellectual and technical concepts
 * contained herein are proprietary to AVRGAMING LLC and its suppliers and may be covered by U.S. and Foreign Patents, patents in process, and are protected by
 * trade secret or copyright law. Dissemination of this information or reproduction of this material is strictly forbidden unless prior written permission is
 * obtained from AVRGAMING LLC. */
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.construct.Template;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.util.ChunkCoord;

public class FishingBoat extends TradeOutpost {

	/* Fishing boats extend trade outposts, so we only need to override methods that are relevant to the construction of the goodie's tower. */
	public static int WATER_LEVEL = 62;
	public static int TOLERANCE = 10;

	public FishingBoat(String id, Town town) throws CivException {
		super(id, town);
	}

	protected FishingBoat(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public String getMarkerIconName() {
		return "anchor";
	}

	@Override
	public void build_trade_outpost(ChunkCoord cChunk) throws CivException {

		/* Add trade good to town. */
		TradeGood good = CivGlobal.getTradeGood(tradeGoodCoord);
		if (good == null) {
			throw new CivException(CivSettings.localize.localizedString("tradeOutpost_notFound") + good);
		}

		if (!good.getInfo().water) {
			throw new CivException(CivSettings.localize.localizedString("fishingBoat_notWater"));
		}

		if (good.getTown() != null) {
			throw new CivException(CivSettings.localize.localizedString("tradeOutpost_alreadyClaimed"));
		}

		good.setStruct(this);
		good.setTown(this.getTown());
		good.setCiv(this.getTown().getCiv());
		/* Save the good *afterwards* so the structure id is properly set. */
		this.setGood(good);
	}

//	@Override
//	public void build_trade_outpost_tower() throws CivException {
//		/* Add trade good to town. */
//		
//		/* this.good is set by the good's load function or by the onBuild function. */
//		TradeGood good = this.good;
//		if (good == null) {
//			throw new CivException("Couldn't find trade good at location:"+good);
//		}
//		
//		/* Build the 'trade good tower' */
//		/* This is always set on post build using the post build sync task. */
//		if (tradeOutpostTower == null) {
//			throw new CivException("Couldn't find trade outpost tower.");
//		}
//		
//		Location centerLoc = tradeOutpostTower.getLocation();
//		
//		/* Build the bedrock tower. */
//		for (int i = 0; i < 3; i++) {
//			Block b = centerLoc.getBlock().getRelative(0, i, 0);
//			ItemManager.setTypeId(b, CivData.BEDROCK); ItemManager.setData(b, 0);
//			
//			StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
//			this.addStructureBlock(sb.getCoord(), false);
//			//CivGlobal.addStructureBlock(sb.getCoord(), this);
//		}
//		
//		/* Place the sign. */
//		Block b = centerLoc.getBlock().getRelative(1, 2, 0);
//		ItemManager.setTypeId(b, CivData.WALL_SIGN); 
//		ItemManager.setData(b, CivData.DATA_SIGN_EAST);
//		Sign s = (Sign)b.getState();
//		s.setLine(0, good.getInfo().name);
//		s.update();
//		StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
//		//CivGlobal.addStructureBlock(sb.getCoord(), this);
//		this.addStructureBlock(sb.getCoord(), false);
//		
//		/* Place the itemframe. */
//		b = centerLoc.getBlock().getRelative(1,1,0);
//		this.addStructureBlock(new BlockCoord(b), false);
//		Block b2 = b.getRelative(0, 0, 0);
//		Entity entity = CivGlobal.getEntityAtLocation(b2.getLocation());
//		
//		if (entity == null || (!(entity instanceof ItemFrame))) {
//			this.frameStore = new ItemFrameStorage(b.getLocation(), BlockFace.EAST);	
//		} else {
//			this.frameStore = new ItemFrameStorage((ItemFrame)entity, b.getLocation());
//		}
//		
//		this.frameStore.setBuildable(this);
//	}

	@Override
	public Location repositionCenter(Location center, Template tpl) {
		Location loc = center.clone();
		String dir = tpl.getDirection();
		double x_size = tpl.getSize_x();
		double z_size = tpl.getSize_z();
		// Reposition tile improvements
		if (this.isTileImprovement()) {
			// just put the center at 0,0 of this chunk?
			loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
			//loc = center.getChunk().getBlock(arg0, arg1, arg2)
		} else {
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
		}

		if (this.getTemplateYShift() != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(WATER_LEVEL + this.getTemplateYShift());
		}

		return loc;
	}

	@Override
	public void onLoad() throws CivException {
		super.createTradeGood();
	}

	@Override
	public void checkBlockPermissionsAndRestrictions(Player player) throws CivException {
		super.checkBlockPermissionsAndRestrictions(player);

		if (Math.abs(this.getCorner().getY() - WATER_LEVEL) > TOLERANCE) {
			throw new CivException(CivSettings.localize.localizedString("fishingBoat_tooDeep"));
		}

	}

}
